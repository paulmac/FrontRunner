/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pmac.imap;

import com.ib.client.Contract;
import com.ib.client.Order;
import com.ib.client.OrderType;
import com.ib.client.Types;
import com.ib.contracts.OptContract;
import com.ib.contracts.StkContract;
import com.ib.controller.Position;
import com.ib.gui.FrontRunnerTws;
import com.pmac.mongo.MongoController;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPStore;
import com.sun.mail.imap.protocol.IMAPProtocol;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Month;
import java.time.Year;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import javax.mail.Folder;
import javax.mail.FolderClosedException;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.event.MessageCountAdapter;
import javax.mail.event.MessageCountEvent;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Paul
 */
public class ImapController {

    final static Logger logger = LoggerFactory.getLogger(ImapController.class);

    // IMAP config
    static String m_mbox; // = "INBOX"; // Should be something else
    private static IMAPFolder m_folder;
//    static private String m_user;
//    static private String m_password;
//    static private String m_protocol;
//    static private String m_host;
//    static private String m_advisorfirm_email; // = "paulmac1914@yahoo.com";
//    static private int m_port;
//    private boolean m_activated;
//    static private List<Document> m_subs;

    static boolean debug = false;

    static final int TWS_PORT = 7496;
    static final int TWS_PORT_PAPER = 7497;

    private static final List<String> m_exchanges_excludes = Arrays.asList("NYSE", "NASDAQ", "OTC", "OTCBB", "TSX", "MSCI", "ETF", "sup3"); // "NASDAQ", not included - 6 chars
    private static final List<String> m_expiry_wednesday = Arrays.asList("VIX");

    static private Properties m_props = System.getProperties();

    private class IdleThread extends Thread {

        private volatile boolean running = true;

        public IdleThread() {
            super();
        }

        public synchronized void kill() {
            if (!running) {
                return;
            }
            this.running = false;
        }

        @Override
        public void run() {
            while (running) {
                try {
                    ensureOpen();
                    m_folder.getMessageCount(); // Prod before entering Idle()
                    ((IMAPFolder) m_folder).idle();
                } catch (MessagingException me) {
                    logger.error(me.toString(), me);
                    try {
                        TimeUnit.MILLISECONDS.sleep(100);
                    } catch (InterruptedException ie) {
                        logger.error(ie.toString(), ie);
                    } catch (Exception e) {                        // Something went wrong, wait and try again
                        logger.error(e.toString(), e);
                    }
                } finally {
                    logger.info("Folder.idle() start @ {}", Calendar.getInstance().getTime());
                }
            }
        }
    }

    public void ensureOpen() {
        do {
            try {
                if (m_folder != null) {
                    if (!m_folder.isOpen()) {
                        /* && (m_folder.getType() & Folder.HOLDS_MESSAGES) != 0 */
                        m_folder.open(Folder.READ_WRITE);
                        logger.info("In ensureOpen(). Open Folder {} Successful.", m_folder, " @ ", Calendar.getInstance().getTime());
                        if (!m_folder.isOpen()) {
                            throw new FolderClosedException(m_folder);
                        }
                    }

                    logger.info("In ensureOpen(). Folder {} is still Open.", m_folder, " @ ", Calendar.getInstance().getTime());
                    return;
                }
            } catch (MessagingException e) {
                logger.error("In ensureOpen() msg {}.", e.toString(), e);
            }
            // If all else fails .....
            openFolder();

        } while (!m_folder.isOpen());
    }

    public void closeFolder() {
        try {
            Store store = m_folder.getStore();
            if (m_folder != null && m_folder.isOpen()) {
                m_folder.close(false);
            }
            if (store != null && store.isConnected()) {
                store.close();
            }

        } catch (MessagingException me) {
            logger.error("msg {}.", me, me);
        }
    }

    public void init() {
        // config imap properties
        m_props = System.getProperties();
        //props.setProperty("mail.store.protocol", "imaps");
        m_props.setProperty("mail.smtp.ssl.trust", "*");
        m_props.setProperty("mail.imaps.ssl.trust", "*");
        m_props.setProperty("mail.imaps.timeout", "1000");

        openFolder();

        IdleThread idleThread = new IdleThread();
        idleThread.setDaemon(false);
        idleThread.setName("ImapIdleThread");
        idleThread.start();

        new Thread(() -> {
            Thread.currentThread().setName("IMapKeepAwakeThread");
            while (true) {
                try {
//                        logger.info("Current Time {}", Calendar.getInstance().getTime(), " messageCount() {}", m_folder.getMessageCount());
                    TimeUnit.MINUTES.sleep(1);
                    // If you want to abuse the IMAP connection by keeping an unused connection open, 
                    // Simple method is call Folder.getMessageCount every 9 minutes. 
                    // If that still fails, reconnect.
                    ensureOpen(); // Just to make sure!

                    // Choose 1 of these methods, no need for both
                    m_folder.getMessageCount();
                    m_folder.doCommand((IMAPProtocol p) -> {
                        p.simpleCommand("NOOP", null);
                        return null;
                    });
                    logger.info("Prodded Imap Server @ {}", Calendar.getInstance().getTime());
                } catch (InterruptedException | MessagingException e) {
                    logger.error("msg {}.", e, e);
                }
            }
        }).start();
    }

    private Double getBuyLimit(String[] words, int j) {
        int i = j;
        try {
            while (!(words[i].equals("up") && words[i + 1].equals("to"))) {
                i++;
            }
        } catch (Exception e) {
            //couldn't find the 'up to' expression so just look for a double
//            Number number = null;
            while (j < words.length) {
                try {
//                    number = NumberFormat.getCurrencyInstance(Locale.US).format(words[j]);
                    return new Double(words[j]);
                } catch (NumberFormatException pe) {
                    j++;
                }
            }

            return 0.0;
        }

        String retVal;
        retVal = words[i + 2].replaceAll("^\\p{Punct}+|\\p{Punct}+$", "");

        return new Double(retVal);
    }

//    Saturday 1st -> Friday 21st.
//    Sunday 1st -> Friday 20th
//    Monday 1st -> Friday 19th
//    Tuesday 1st -> Friday 18th
//    Wednesday 1st -> Friday 17th
//    Thursday 1st -> Friday 16th
//    Friday 1st -> Friday 15th
    public static Calendar expiryDayInMonth(int month, int year, String symbol) {
        Calendar calendar = Calendar.getInstance();
        if (m_expiry_wednesday.contains(symbol)) {
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY);
        } else {
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
        }
        calendar.set(Calendar.DAY_OF_WEEK_IN_MONTH, 3);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.YEAR, year);
        return calendar;
    }

    public Iterator<String> splitBlocks(String content, String orderToken) {

        if (StringUtils.containsIgnoreCase(content, orderToken)) {
            while (StringUtils.countMatches(content, orderToken) == 0) {
                // Problem with capitalization perhaps ....?
                if (StringUtils.contains(orderToken, "take")) {
                    orderToken = StringUtils.replace(orderToken, "take", "Take");
                } else {
                    orderToken = StringUtils.substring(orderToken, 1, 7);
                }
            }

            Iterator<String> itr = Arrays.asList(StringUtils.splitByWholeSeparator(content, orderToken)).iterator();
            itr.next(); // not interested in first Block
            return itr;
        }

        return null;
    }

    private Double getAmountAfter(String orderStr, String prefix) {
        String sb = StringUtils.substringAfter(orderStr, prefix);
        String[] words = StringUtils.substringAfter(sb, "$").split("[^0-9.']+");
        return new Double(words[0].replaceAll("^\\p{Punct}+|\\p{Punct}+$", ""));
    }

    private void processEmailMsgContent(String content, Document sub) {

        try {

            Iterator<String> blockItr;

            if (StringUtils.contains(content, sub.getString("orderToken"))) {
                blockItr = splitBlocks(content, sub.getString("orderToken"));
            } else if (StringUtils.contains(content, sub.getString("orderToken2"))) {
                blockItr = splitBlocks(content, sub.getString("orderToken2"));
            } else {
                return;
            }

            while (blockItr.hasNext()) {
                String block = blockItr.next().trim(); // remove leading whitespace
                Contract con = null; // Contract created for each Recomendation or "Order Line"

                Order order = new Order();
//                order.account(m_acctInfoPanel.m_selAcct);
                order.action(Types.Action.SSHORT); // Pmac: Should modify also in Ctor?
                String orderStr; // = null;

                logger.info("Block {} ", block);

                String[] lines = block.split("\\r?\\n");
                Iterator<String> linesItr = Arrays.asList(lines).iterator();

                StringBuilder sb = new StringBuilder(); // Has action "Sell" or "Buy" but may span another line or 2                                                

                // Identify Order String and setup Order object
                while (linesItr.hasNext()) {
                    String line = linesItr.next();
                    if (line.isEmpty() && (sb.length() != 0)) { // ensure not the first (or leading) empty String          
                        orderStr = sb.toString();
                        logger.info("Order: {} ", orderStr);
                        // Check Order String contains keywords; "option", "Put" Or "Call" 
                        if (StringUtils.containsIgnoreCase(orderStr, "option")
                                && (StringUtils.containsIgnoreCase(orderStr, "put")
                                || StringUtils.containsIgnoreCase(orderStr, "call"))) {
                            String[] words = orderStr.split("[^a-zA-Z0-9.']+"); // \\P{Alpha}+ matches any non-alphabetic character '.' is for a possible decimal point
                            String w = words[0];
                            for (int j = 0; j < words.length; j++) {
                                w = words[j];
                                if (Pattern.matches("([A-Z]{2,5})", w) && !m_exchanges_excludes.contains(w)) {
                                    con = new OptContract(words[j++]); // ticker
                                    // Obtain : lastTradeDateOrContractMonth, double strike, String right) {
                                    String m = words[j++];
                                    Month month = Month.valueOf(m.toUpperCase());
                                    String y = words[j++];
                                    Year year = Year.of(new Integer(y));
                                    // Find first Friday on or after the 15th of the month, (equates to 3rd friday of the month)
                                    Calendar c = expiryDayInMonth(month.ordinal(), year.getValue(), con.symbol());
                                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
                                    con.lastTradeDateOrContractMonth(sdf.format(c.getTime()));
                                    con.strike(new Double(words[j++]));
                                    con.right(words[j++].toUpperCase());
                                    if (order.action().equals(Types.Action.BUY)) {
                                        if (StringUtils.containsIgnoreCase(orderStr, "up to")) {
                                            order.auxPrice(getAmountAfter(orderStr, "up to"));
                                        } else { // Look for the next $amount in the Order String                                         
                                            for (int z = j; z < words.length; z++) {
                                                if (NumberUtils.isCreatable(words[z])) {
                                                    order.auxPrice(new Double(words[z]));
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                    break; // don't need to process any more words
                                }
                            }
                        } else {
                            String[] words = orderStr.split("[^a-zA-Z\\d*\\.\\d+]+"); // \\P{Alpha}+  matches any non-alphabetic character
                            for (int j = 0; j < words.length; j++) {
                                String w = words[j];
                                w = w.replaceAll("^\\p{Punct}+|\\p{Punct}+$", "");
                                if (Pattern.matches("([A-Z]{2,5})", w) && !m_exchanges_excludes.contains(w)) {
                                    con = new StkContract(w);
                                    if (order.action().equals(Types.Action.BUY)) {
                                        order.auxPrice(getBuyLimit(words, j));
                                    }
                                    break;
                                }
                            }
                        }

                        Position position = null; // Held in memory stack

                        if (order.action().equals(Types.Action.SELL)) {
                            // Require Position from 'long store' PortfolioMap. Compare the Contract 
                            position = FrontRunnerTws.INSTANCE.findPosition(con);
                            if (position != null) {
                                if (StringUtils.containsIgnoreCase(orderStr, "half")) {
                                    order.totalQuantity((int) position.position() / 2);
                                } else if (StringUtils.containsIgnoreCase(orderStr, "third")) {
                                    order.totalQuantity((int) position.position() / 3);
                                } else {
                                    order.totalQuantity(position.position());
                                }
                                order.orderType(OrderType.MKT);

                                FrontRunnerTws.INSTANCE.twsController().placeOrModifyOrder(con, order, null);

//                                updateRecommendation(con, order);
                            }
                        } else {  // Buy Order                          

                            // Rickards' subs talk about an 'assumed' entry price, 
                            // this is our preferred BUY 'limit price' 
                            if (sub.getString("fullname").contains("Rickards") && linesItr.hasNext()) {

                                if (StringUtils.containsIgnoreCase(block, "assuming")) {
                                    order.lmtPrice(getAmountAfter(block, "ssuming"));
                                } else {
                                    Double askPrice = getAmountAfter(orderStr, "ask price");
                                    Double bidPrice = getAmountAfter(orderStr, "bid price");
                                    order.lmtPrice(bidPrice + (askPrice - bidPrice) / 2);
                                }
                            } else {
                                order.lmtPrice(order.auxPrice()); // same thing for non Rickard's subs
                            }

                            if (sub.getBoolean("autoBuy")) {

                                int requiredPos = 0;
                                // Test whether a reaffirm Rec.
                                if (sub.getDouble("affirmBuyLimit") > 0.0) {
                                    position = FrontRunnerTws.INSTANCE.findPosition(con);
                                    if (position != null) {
                                        requiredPos = new Double(sub.getDouble("affirmBuyLimit") / order.lmtPrice()).intValue();
                                    }
                                }

                                if (position == null) {
                                    requiredPos = new Double(sub.getDouble("autoBuyLimit") / order.lmtPrice()).intValue();
                                }

                                if (requiredPos > 0) {
                                    if (con instanceof OptContract) {
                                        requiredPos = requiredPos / 100;
                                    }
                                    if (requiredPos > 1) {
                                        order.minQty(requiredPos / 2);
                                    } else if (requiredPos == 1) {
                                        order.minQty(requiredPos);
                                    }

                                    if (requiredPos > 0) { // need to check again as rounding may have caused it to go to 0
                                        order.totalQuantity(requiredPos);
                                        order.orderType(OrderType.LMT);

                                        FrontRunnerTws.INSTANCE.twsController().placeOrModifyOrder(con, order, null);
                                    }
                                }
                            }

                            if (position == null) {
                                FrontRunnerTws.INSTANCE.mongoController().createRecommendation(con, order, sub.getString("_id"));
                            }
                        }

                        break; // out of loop for this 'Block'

                    } else { // continue building "Order String"
                        if (!line.isEmpty()) {
                            sb.append(line);
                            sb.append(" ");
                            if (order.action().equals(Types.Action.SSHORT)) {
                                if (StringUtils.containsIgnoreCase(line, "buy")) {
                                    order.action(Types.Action.BUY);
                                } else if (StringUtils.containsIgnoreCase(line, "sell")) {
                                    order.action(Types.Action.SELL);
                                } else if (StringUtils.containsIgnoreCase(line, "hold")) {
                                    break; // out of this Block
                                }
                            }
                        }
                    }
                } // for (String l : lines) Process each line of a "Block" (i.e a reccommendation)
            } // for (int k = 1; k < blockCnt; k++) Process loop of each "Block" (i.e a reccommendation)        
        } catch (NumberFormatException nfe) {
            logger.error("msg {}.", nfe, nfe);
        } catch (Exception e) {
            logger.error("msg {}.", e, e);
        } finally {
            logger.info("Finished Processing Msg from {}", sub.getString("fullname"));
        }
    }

    // Continually adjust an option order price by the product of a user-define delta and the change of the option's 
    // underlying stock price. 
    // Option Starting Price (By default, this is set to the Option Midpoint at the time the order is submitted.)
    // Stock Reference Price (By default, this is set to the Stock NBBO midpoint at the time the order is submitted.)
    private void sellPegStock(String ticker) throws IOException, MessagingException {
        Contract con = new OptContract(ticker); // ticker
        con.exchange("SMART"); // not all exchanges can do it, let IB route Order 
        Position position = FrontRunnerTws.INSTANCE.findPosition(con);
        if (position != null) {
            Order order = new Order();
            order.action(Types.Action.SELL); // Pmac: Should modify also in Ctor?
            order.orderType(OrderType.PEG_STK);
            order.totalQuantity(position.position());
            order.delta(50);
            FrontRunnerTws.INSTANCE.twsController().placeOrModifyOrder(con, order, null);
        }        
    }

    // For when all necessary info is in the subject i.e TV events
    private void processEmailMsgSubject(String subject) throws IOException, MessagingException {
        logger.info("TradingView Alert : {}", subject);
        if (StringUtils.containsIgnoreCase(subject, "Crossing Fisher Transform") || 
                StringUtils.containsIgnoreCase(subject, "Moving Average")) {
            String ticker = StringUtils.substringBetween(subject, "on", ",").trim(); // .split("[^0-9.']+");
            sellPegStock(ticker);
        } else
            logger.info("TradingView Alert Type not recognised");            
    }

    private void processEmailMsgMultipart(Message msg, Document sub) throws IOException, MessagingException {

        Multipart multiPart = (Multipart) msg.getContent();

        for (int i = 0; i < multiPart.getCount(); i++) {
            MimeBodyPart part = (MimeBodyPart) multiPart.getBodyPart(i);
            if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
                logger.info("Part is attachment : {}", part.getDisposition());
            } else {
                String content = (String) part.getContent();
                new Thread(() -> {
                    Thread.currentThread().setName("EmailContentProcessThread");
                    processEmailMsgContent(content, sub);
                }).start();
                break;
            }
        }
    }

    private void processEmailMsg(Message msg) throws IOException, MessagingException {

        InternetAddress email = (InternetAddress) msg.getFrom()[0];
        String subject = msg.getSubject();
        String address = email.getAddress();
        String personal = StringUtils.replacePattern(email.getPersonal(), "[^a-zA-Z0-9.\\ ]+", ""); // to remove ambiguous ' or ’ 
        logger.info("Email arrived from : [{}] <{}> with Subject: {}", personal, address, subject);

//      Subject for TradingView email
        if (subject.startsWith("TradingView Alert")) {
            processEmailMsgSubject(subject);
        } else { // if (address.equals(m_advisorfirm_email)) {
            List<Document> subs = FrontRunnerTws.INSTANCE.mongoController().getSubs();
            for (Document sub : subs) {
                // indicative of a Buy or Sell Recommendation(s) embedded in the body
                String displayName = StringUtils.replacePattern(sub.getString("fullname"), "[^a-zA-Z0-9.\\ ]+", "");
                if ((StringUtils.equals(personal, displayName))
                        && (StringUtils.containsIgnoreCase(subject, sub.getString("sellSubjectToken"))
                        || StringUtils.containsIgnoreCase(subject, sub.getString("buySubjectToken"))
                        || StringUtils.containsIgnoreCase(subject, "%"))) {

                    processEmailMsgMultipart(msg, sub);
                    return;
                }
            }
        }
    }

    private void processEmailMsgs(Message[] msgs) throws IOException, MessagingException {
        for (Message msg : msgs) {
            processEmailMsg(msg);
        }
    }

//    Stuff to do with actually processing of incoming mail
    public void openFolder() {

        try {
            logger.info("Entering openFolder() @ {}", Calendar.getInstance().getTime());
            // Get a Session object
            Session session = Session.getInstance(m_props, null);
            session.setDebug(debug);

            // Get a Store object
            IMAPStore store = (IMAPStore) session.getStore(MongoController.IMAP_COORDS.m_protocol);
            store.connect(MongoController.IMAP_COORDS.m_host,
                    MongoController.IMAP_COORDS.m_port,
                    MongoController.IMAP_COORDS.m_user,
                    MongoController.IMAP_COORDS.m_password);

            if (!store.hasCapability("IDLE")) {
                throw new RuntimeException("IDLE not supported");
            }

            m_folder = (IMAPFolder) store.getFolder(MongoController.IMAP_COORDS.m_mbox);
            if (m_folder == null || !m_folder.exists()) {
                logger.error("Invalid folder {}.", m_folder);
                System.exit(1);
            }

            m_folder.open(Folder.READ_WRITE);

            logger.info("Folder open for business!");

            // Add messageCountListener to listen for new messages
            m_folder.addMessageCountListener(new MessageCountAdapter() {
                @Override
                public void messagesAdded(MessageCountEvent ev) {
                    try {
                        processEmailMsgs(ev.getMessages());
                    } catch (IOException | MessagingException me) {
                        logger.error("Wait 100ms and try One more time .... {}  ", me, me);
                        try {
                            TimeUnit.MILLISECONDS.sleep(100);
                            ensureOpen();
                            processEmailMsgs(ev.getMessages());
                        } catch (IOException | MessagingException | InterruptedException e) {
                            logger.error("Failed 2nd time so lost this msg {} ", e, e);
                        }
                    }
                }
            });
        } catch (MessagingException me) {
            logger.error("msg {}.", me, toString(), me);
            ensureOpen(); // recursive
        }
    }

}
