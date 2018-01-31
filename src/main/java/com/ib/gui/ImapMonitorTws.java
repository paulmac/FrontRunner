/* Paulmac Software dev 2017. */

package com.ib.gui;

import java.util.ArrayList;
//import java.util.List;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Rectangle;

import com.ib.util.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
//import javax.swing.SwingWorker;

import com.ib.client.Types.NewsType;
import com.ib.controller.ApiConnection.ILogger;
import com.ib.controller.ApiController;
//import com.ib.controller.ApiController.IBulletinHandler;
import com.ib.controller.ApiController.IConnectionHandler;
//import com.ib.controller.ApiController.ITimeHandler;
import com.ib.controller.Formats;

//HtmlButton;
import com.ib.util.IConnectionConfiguration;
import com.ib.util.IConnectionConfiguration.DefaultConnectionConfiguration;
import com.ib.util.NewLookAndFeel;
import com.ib.util.NewTabbedPanel;
import com.ib.util.VerticalPanel;

//import com.paulmac.trade.ImapMonitor;

import com.ib.client.Contract;
import com.ib.client.Order;
import com.ib.client.OrderType;
import com.ib.client.Types;
import com.ib.contracts.OptContract;
import com.ib.contracts.StkContract;
//import com.ib.controller.ApiConnection;
import com.ib.controller.Position;

import java.io.*;
import javax.mail.*;
import javax.mail.event.*;
import javax.mail.internet.*;
import com.sun.mail.imap.*;
import com.sun.mail.imap.protocol.IMAPProtocol;
import java.util.regex.Pattern;
//import java.util.logging.Level;
//import javax.swing.SwingWorker;
//import org.apache.commons.mail.util.*;
//import org.apache.commons.lang3.StringUtils;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
//import com.mongodb.client.MongoCollection;
//import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.currentDate;
import static com.mongodb.client.model.Updates.set;
import com.mongodb.client.result.UpdateResult;
import java.text.NumberFormat;
import org.apache.commons.lang3.math.NumberUtils;
//import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.time.Month;
import java.time.Year;
import java.util.Arrays;
import org.bson.Document;

//import com.google.common.collect.ImmutableList;

//import com.mongodb.client.model.CreateCollectionOptions;
//import com.mongodb.client.model.ValidationOptions;
//import com.paulmac.trade.ImapMonitor;
import java.util.Calendar;
import java.util.Iterator;
//import java.util.Date;
//import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
//import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
//import java.util.logging.Level;
//import java.util.logging.Level;
//import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
//import org.jsoup.Jsoup;
//import org.jsoup.nodes.TextNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

 public class ImapMonitorTws implements IConnectionHandler {
    
   final static Logger logger = LoggerFactory.getLogger(ImapMonitorTws.class);
   
   static { NewLookAndFeel.register(); }

    public static ImapMonitorTws INSTANCE;

    private final IConnectionConfiguration m_connectionConfiguration;
    private final JTextArea m_inLog = new JTextArea();
    private final JTextArea m_outLog = new JTextArea();
    private final PanelLogger m_inLogger = new PanelLogger( m_inLog);
    private final PanelLogger m_outLogger = new PanelLogger( m_outLog);
    private ApiController m_controller;
//    private ImapMonitor m_imapMonitor;
    private final ArrayList<String> m_acctList = new ArrayList<>();
    private final JFrame m_frame = new JFrame();
    private final NewTabbedPanel m_tabbedPanel = new NewTabbedPanel(true);
    private final ConnectionPanel m_connectionPanel;
    private final MarketDataPanel m_mktDataPanel = new MarketDataPanel();
    private final ContractInfoPanel m_contractInfoPanel = new ContractInfoPanel();
    private final TradingPanel m_tradingPanel = new TradingPanel();
    private final AccountInfoPanel m_acctInfoPanel = new AccountInfoPanel();
    private final AccountPositionsMultiPanel m_acctPosMultiPanel = new AccountPositionsMultiPanel();
    private final OptionsPanel m_optionsPanel = new OptionsPanel();
    private final AdvisorPanel m_advisorPanel = new AdvisorPanel();
    private final ComboPanel m_comboPanel = new ComboPanel(m_mktDataPanel);
    private final StratPanel m_stratPanel = new StratPanel();
    private final NewsPanel m_newsPanel = new NewsPanel();
    private final JTextArea m_msg = new JTextArea();

    private MongoClient mongoClient; // = new MongoClient();
    private MongoDatabase database; // = mongoClient.getDatabase("frontrunner");
    private MongoCollection<Document> m_recommendations;
    private Document m_advisorfirms;

    static boolean verbose = false;
    static boolean debug = false;
    static boolean showStructure = false;
    static boolean showMessage = false;
    static boolean showAlert = false;
    static boolean saveAttachments = false;
    static int attnum = 1;
    static int freq = 1000; // 0.5 second
//    private static final String regex ="[A-Z]{3,4}"; //alpha uppercase
//    private static final String regExUpperCase = "([A-Z]{2,5}.)";
//    private static final Pattern upper2to5 = Pattern.compile(regExUpperCase);
    private static final List<String> m_exchanges_excludes = Arrays.asList("NYSE", "NASDAQ", "OTC", "OTCBB", "TSX", "MSCI", "ETF","sup3"); // "NASDAQ", not included - 6 chars
    private static final List<String> m_expiry_wednesday = Arrays.asList("VIX");

//    private boolean m_connected = false;

    // IMAP config
    static String m_mbox; // = "INBOX"; // Should be something else
    private static IMAPFolder m_folder;
    static private String m_user;
    static private String m_password;
    static private String m_protocol;
    static private String m_host;
    static private String m_advisorfirm_email; // = "paulmac1914@yahoo.com";
    static private int m_port;
    private boolean m_activated;
    static private List<Document> m_subs;
    
    static private Properties m_props = System.getProperties();
       
    // getter methods
    public ArrayList<String> accountList()  { return m_acctList; }
    public JFrame frame()                   { return m_frame; }
    public ILogger getInLogger()            { return m_inLogger; }
    public ILogger getOutLogger()           { return m_outLogger; }

//    public static boolean isUpperCase(String str){
//        return Pattern.compile(regex).matcher(str).find();
//    }

    public static void main(String[] args) {
        ImapMonitorTws appMainThread = new ImapMonitorTws( new DefaultConnectionConfiguration() );
//        appMainThread
        start(appMainThread);
    }
	
    public static void start( ImapMonitorTws apiDemo ) {
        INSTANCE = apiDemo;
        INSTANCE.run();
    }

    public ImapMonitorTws( IConnectionConfiguration connectionConfig ) {
        m_connectionConfiguration = connectionConfig; 
        m_connectionPanel = new ConnectionPanel(); // must be done after connection config is set
    }
	
    public ApiController controller() {
        if ( m_controller == null ) {
            m_controller = new ApiController( this, getInLogger(), getOutLogger() );
        }
        return m_controller;
    }
    
    private void run() {
        try {
            Thread.currentThread().setName("AppMainThread");
            logger.info("In {}", Thread.currentThread().getStackTrace()[1].getMethodName());
            m_tabbedPanel.addTab( "Connection", m_connectionPanel);
            m_tabbedPanel.addTab( "Market Data", m_mktDataPanel);
            m_tabbedPanel.addTab( "Trading", m_tradingPanel);
            m_tabbedPanel.addTab( "Account Info", m_acctInfoPanel);
            m_tabbedPanel.addTab( "Acct/Pos Multi", m_acctPosMultiPanel);
            m_tabbedPanel.addTab( "Options", m_optionsPanel);
            m_tabbedPanel.addTab( "Combos", m_comboPanel);
            m_tabbedPanel.addTab( "Contract Info", m_contractInfoPanel);
            m_tabbedPanel.addTab( "Advisor", m_advisorPanel);
            // m_tabbedPanel.addTab( "Strategy", m_stratPanel); in progress
            m_tabbedPanel.addTab( "News", m_newsPanel);

            m_msg.setEditable( false);
            m_msg.setLineWrap( true);
            JScrollPane msgScroll = new JScrollPane( m_msg);
            msgScroll.setPreferredSize( new Dimension( 10000, 120) );

            JScrollPane outLogScroll = new JScrollPane( m_outLog);
            outLogScroll.setPreferredSize( new Dimension( 10000, 120) );

            JScrollPane inLogScroll = new JScrollPane( m_inLog);
            inLogScroll.setPreferredSize( new Dimension( 10000, 120) );

            NewTabbedPanel bot = new NewTabbedPanel();
            bot.addTab( "Messages", msgScroll);
            bot.addTab( "Log (out)", outLogScroll);
            bot.addTab( "Log (in)", inLogScroll);

            m_frame.add( m_tabbedPanel);
            m_frame.add( bot, BorderLayout.SOUTH);
            m_frame.setSize( 1024, 768);
            m_frame.setVisible( true);
            m_frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

            // Setup MongoClient
            MongoClientURI uri = new MongoClientURI("mongodb://paulmac:acmilan"
                    + "@clusterfr-shard-00-00-orba3.mongodb.net:27017,"
                    + "clusterfr-shard-00-01-orba3.mongodb.net:27017,"
                    + "clusterfr-shard-00-02-orba3.mongodb.net:27017/"
                    + "admin?ssl=true&replicaSet=ClusterFR-shard-0&authSource=admin"
//                    + "clusterfr-shard-00-00-orba3.mongodb.net:27017,"
//                    + "clusterfr-shard-00-01-orba3.mongodb.net:27017,"
//                    + "clusterfr-shard-00-02-orba3.mongodb.net:27017/"
//                    + "frontrunner?replicaSet=ClusterFR-shard-0" --authenticationDatabase admin""
//                    + " --ssl --username paulmac --password acmilan"
            );
            

            mongoClient = new MongoClient(uri);
            database = mongoClient.getDatabase("frontrunner");

            m_advisorfirms = database.getCollection("advisorfirms").find().first();
            logger.info("Sub Provider {}", m_advisorfirms.toJson());
            
            m_advisorfirm_email = m_advisorfirms.getString("email");
            m_mbox = m_advisorfirms.getString("mailbox");
            m_subs = (List<Document>) m_advisorfirms.get("subscriptions");
            m_subs.forEach((sub) -> {
                logger.info("sub : {} ", sub);
            });
                        
            Document emailConfig = database.getCollection("emailconfig").find(eq("_id", m_advisorfirms.getString("emailconfig_id"))).first();
            logger.info("Email Config : {}", emailConfig.toJson());
            m_user = emailConfig.getString("user");
            m_password = emailConfig.getString("password");
            Document imapConfig = (Document)emailConfig.get("imap");
            m_protocol = imapConfig.getString("protocol");
            m_host = imapConfig.getString("host");
            m_port = ((Number)imapConfig.get("port")).intValue();
            
            m_recommendations = database.getCollection("recommendations");

            // setup IMAP listener

            // make initial connection to local host, port 7496, client id 0, no connection options
            controller().connect( "127.0.0.1", 7496, 0, m_connectionConfiguration.getDefaultConnectOptions() != null ? "" : null );
    //                m_acctInfoPanel.activated();
    //                show( "Called activated");

            // Wait at least 1 sec wait, till TWS is fully setup (activated) .......
            do {
                TimeUnit.SECONDS.sleep(1);
            } while (!m_activated); 

            // config imap properties
            m_props = System.getProperties();
            //props.setProperty("mail.store.protocol", "imaps");
            m_props.setProperty("mail.smtp.ssl.trust", "*");
            m_props.setProperty("mail.imaps.ssl.trust", "*");
            m_props.setProperty("mail.imaps.timeout", "1000");

            openFolder();
//        if (m_imapMonitor.isConnected())
//            m_imapMonitor.listen();

//        this.listen();
            //  
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

            // causes the current thread to pause execution until idleThread's thread terminate
            //idleThread.join();
        } catch (Exception e) {
            logger.error("msg {}.", e, e);
        } finally {
            logger.info("Finished ImapMonitorTws Startup");
            show("Finished ApiDemo thread");        
        }        
    }

    private class IdleThread extends Thread {
        
        private volatile boolean running = true;

        public IdleThread() { 
            super();
        }
        
        public synchronized void kill() {
            if (!running)
                return;
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
                    if (!m_folder.isOpen() ) { /* && (m_folder.getType() & Folder.HOLDS_MESSAGES) != 0 */
                        m_folder.open(Folder.READ_WRITE);
                        logger.info("In ensureOpen(). Open Folder {} Successful.", m_folder, " @ ", Calendar.getInstance().getTime());
                        if (!m_folder.isOpen())
                            throw new FolderClosedException(m_folder);
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
                } catch(NumberFormatException pe) {
                    j++;
                }
            }
            
            return 0.0;
        }
        
        String retVal;
        retVal = words[i+2].replaceAll("^\\p{Punct}+|\\p{Punct}+$","");
        
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
        if (m_expiry_wednesday.contains(symbol))
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY);
        else
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
        calendar.set(Calendar.DAY_OF_WEEK_IN_MONTH, 3);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.YEAR, year);
        return calendar;
    }    

    public void insertRecommendation(Contract con, Order order, String subId) {
        
        try {
            Document rec = new Document();
            rec.put("_id", con.localSymbol());
            rec.put("symbol", con.symbol());
            rec.put("secType", con.secType().toString());
            rec.put("lastTradeDateOrContractMonth", con.lastTradeDateOrContractMonth());
            rec.put("strike", con.strike());
            rec.put("right", con.right().toString());
            rec.put("entryPrice", 0.0); // Agora published 'entry' price
            rec.put("limitPrice", order.lmtPrice());
            rec.put("buyUpToPrice", order.auxPrice());
            rec.put("receivedDate", Calendar.getInstance().getTime()); // Tue Sep 22 07:59:07 EDT 2009
            rec.put("exitDate", "");
            rec.put("subscription_id", subId);

            Document posBson = new Document();
            posBson.put("_id", con.localSymbol());  // Composite Key including Account
            posBson.put("recommedation_id", con.localSymbol()); // same for first (default) position
            posBson.put("exchange", "SMART");   // default
            posBson.put("entryDate", "");       // Can be different to the received date of the Recommendation
            posBson.put("currency", "USD");     // default
            posBson.put("position", 0);         // initially, basically a placeholder
            posBson.put("marketPrice", 0);      // initially, basically a placeholder
            posBson.put("marketValue", 0);      // initially, basically a placeholder
            posBson.put("averageCost", 0);      // initially, basically a placeholder
            posBson.put("unrealPnl", 0);        // initially, basically a placeholder
            posBson.put("realPnl", 0);          // initially, basically a placeholder

            rec.append("positions", posBson);
            m_recommendations.insertOne(rec);

            logger.info("Buy Order for : {} Stored in MongoDB", con.localSymbol());

        } catch (com.mongodb.MongoWriteException e) {
            logger.error("msg {}.", e, e);
        }    
    }
    
    public void updateRecommendation1to3(Position pos) {                
        // The Position Collection is usually only 1 element so for efficiency search based on first element of Collection 
        if ( m_recommendations.find(eq("positions._id", pos.contract().localSymbol())).iterator().hasNext()) {
            UpdateResult result = m_recommendations.updateOne(eq("positions._id", pos.contract().localSymbol()), 
            combine(set("positions.position", pos.position()), 
                    set("positions.marketPrice", pos.marketPrice()), 
                    set("positions.marketValue", pos.marketValue()), 
                    set("positions.unrealPnl", pos.unrealPnl()), 
                    set("positions.averageCost", pos.averageCost()) 
                    )
                );
            if (result.wasAcknowledged() && result.isModifiedCountAvailable() && (result.getModifiedCount() == 1))
                logger.info("Updated Recommedation  {}, Position {}, Market Price {}", pos.contract().localSymbol(), pos.position(), pos.marketPrice());
//            else
//                logger.error("Recommedation  {}, Position {}, Market Price {} NOT FOUND in DB?!", pos.contract().localSymbol(), pos.position(), pos.marketPrice());
                
        } else if ( m_recommendations.find(eq("positions.1._id", pos.contract().localSymbol())).iterator().hasNext()) {
            UpdateResult result = m_recommendations.updateOne(eq("positions.1._id", pos.contract().localSymbol()), 
            combine(set("positions.1.position", pos.position()), 
                    set("positions.1.marketPrice", pos.marketPrice()), 
                    set("positions.1.marketValue", pos.marketValue()), 
                    set("positions.1.unrealPnl", pos.unrealPnl()), 
                    set("positions.1.averageCost", pos.averageCost()) 
                    )
                );
            if (result.wasAcknowledged() && result.isModifiedCountAvailable() && (result.getModifiedCount() == 1))
                logger.info("Updated Recommedation  {}, Position {}, Market Price {}", pos.contract().localSymbol(), pos.position(), pos.marketPrice());            
        } else if ( m_recommendations.find(eq("positions.2._id", pos.contract().localSymbol())).iterator().hasNext()) {
            UpdateResult result = m_recommendations.updateOne(eq("positions.2._id", pos.contract().localSymbol()), 
            combine(set("positions.2.position", pos.position()), 
                    set("positions.2.marketPrice", pos.marketPrice()), 
                    set("positions.2.marketValue", pos.marketValue()), 
                    set("positions.2.unrealPnl", pos.unrealPnl()), 
                    set("positions.2.averageCost", pos.averageCost()) 
                    )
                );
            if (result.wasAcknowledged() && result.isModifiedCountAvailable() && (result.getModifiedCount() == 1))
                logger.info("Updated Recommedation {}, Position {}, Market Price {}", pos.contract().localSymbol(), pos.position(), pos.marketPrice());            
        } else {
            logger.info("Recommedation {} Not found in DB, Position {}, Market Price {}", pos.contract().localSymbol(), pos.position(), pos.marketPrice());
        }
    }
    
    /**
     *
     * @param pos
     */
    public void updateRecommendation3toN(Position pos) {                
        
        for (int i = 2; i < 4; i++) {
            String PosPrefix = new StringBuilder("positions.").append(i).toString();
            UpdateResult result = m_recommendations.updateOne(eq(PosPrefix.concat("_id"), pos.contract().localSymbol()), 
                    combine(set(PosPrefix.concat("position"), pos.position()), 
                            set(PosPrefix.concat("marketPrice"), pos.marketPrice()), 
                            set(PosPrefix.concat("marketValue"), pos.marketValue()), 
                            set(PosPrefix.concat("unrealPnl"), pos.unrealPnl()), 
                            set(PosPrefix.concat("averageCost"), pos.averageCost())  
                            )
                        );
            if (result.wasAcknowledged() && result.isModifiedCountAvailable() && (result.getModifiedCount() == 1))
                return;
        }
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

    private void processEmailMsgContent(String content, Document sub) {

        try {
            
            Iterator<String> blockItr;
            
            if (StringUtils.contains(content, sub.getString("orderToken")))
                blockItr = splitBlocks(content, sub.getString("orderToken"));
            else if (StringUtils.contains(content, sub.getString("orderToken2")))
                blockItr = splitBlocks(content, sub.getString("orderToken2"));
            else
                return;

            while(blockItr.hasNext()) {
                String block = blockItr.next().trim(); // remove leading whitespace
                Contract con = null; // Contract created for each Recomendation or "Order Line"

                Order order = new Order();
//                order.account(m_acctInfoPanel.m_selAcct);
                order.action(Types.Action.SSHORT); // Pmac: Should modify also in Ctor?
                String orderStr; // = null;

                logger.info("Block {} ", block);

                String[] lines = block.split("\\r?\\n");
                Iterator<String> linesItr = Arrays.asList(lines).iterator();
                
//                int lnctr = 0;
                StringBuilder sb = new StringBuilder(); // Has action "Sell" or "Buy" but may span another line or 2                                                

                // Identify Order String and setup Order object
		while (linesItr.hasNext()) {
//                for (String line : linesList) {
                    
                    String line = linesItr.next();
//                    logger.info("Line {} : {}", lnctr++, line);

                    if (line.isEmpty() && (sb.length() != 0)) { // ensure not the first (or leading) empty String          

                        orderStr = sb.toString();
                        logger.info("Order: {} ", orderStr);

                        // Check Order String contains keywords; "option", "Put" Or "Call" 
                        if (StringUtils.containsIgnoreCase(orderStr, "option") &&
                             (StringUtils.containsIgnoreCase(orderStr, "put") ||
                              StringUtils.containsIgnoreCase(orderStr, "call"))) 
                        {

                            String[] words = orderStr.split("[^a-zA-Z0-9.']+"); // \\P{Alpha}+ matches any non-alphabetic character '.' is for a possible decimal point
                            String w = words[0];
                            for (int j=0; j < words.length; j++) {
                                w =  words[j];
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
                                            for (int z = j; z < words.length; z++)
                                                if (words[z].equals("up") && words[z+1].equals("to")) {
                                                    order.auxPrice(new Double(words[z+2]));
                                                    break;
                                                }
                                        } else { // Look for the next $amount in the Order String                                         
                                            for (int z = j; z < words.length; z++)
                                                if (NumberUtils.isCreatable(words[z])) {
                                                    order.auxPrice(new Double(words[z]));
                                                    break;
                                                }
                                        }
                                    }
                                        
                                    break; // don't need to process any more words
                                }
                            }
                        } else {
                            String[] words = orderStr.split("[^a-zA-Z\\d*\\.\\d+]+"); // \\P{Alpha}+  matches any non-alphabetic character
                            for (int j=0; j < words.length; j++) {
                                String w =  words[j];
                                w = w.replaceAll("^\\p{Punct}+|\\p{Punct}+$","");
                                if (Pattern.matches("([A-Z]{2,5})", w) && !m_exchanges_excludes.contains(w)) {
                                    con = new StkContract(w);
                                    if (order.action().equals(Types.Action.BUY))
                                        order.auxPrice(getBuyLimit(words, j));
                                    break;
                                }
                            }
                        }                                            

                        Position position = null; // Held in memory stack

                        if (order.action().equals(Types.Action.SELL)) {
                            // Require Position from 'long store' PortfolioMap. Compare the Contract 
                            position = m_acctInfoPanel.findPosition(con);
                            if (position != null) {
                                if (StringUtils.containsIgnoreCase(orderStr, "half")) {
                                    order.totalQuantity((int)position.position()/2);
                                } else if (StringUtils.containsIgnoreCase(orderStr, "third")) {
                                    order.totalQuantity((int)position.position()/3);
                                } else {
                                    order.totalQuantity(position.position());
                                }
                                order.orderType(OrderType.MKT);
                                
                                m_controller.placeOrModifyOrder(con, order, null);
                                                                
//                                updateRecommendation(con, order);
                            } 
                        } else {                            
                            
                            // Rickards' subs talk about an 'assumed' entry price, 
                            // this is our preferred BUY 'limit price' 
                            if (sub.getString("fullname").contains("Rickards") && linesItr.hasNext()) {                                            
                                do {                                                
                                    line = linesItr.next();
                                    logger.info("Line : {}", line);
                                } while (!(StringUtils.containsIgnoreCase(line, "assuming")) && linesItr.hasNext());

                                sb = new StringBuilder(StringUtils.substringAfter(line, "ssuming")); // avoid capital/lowercase issue for 'A' 
                                sb.append(" "); // so last word and first word of next string don't merge
                                if (linesItr.hasNext()) {
                                    line = linesItr.next(); // lines[lnctr++];
                                    sb.append(line); // "assuming a $limitPrice .." may span more than 1 line
                                }
                                String[] words = StringUtils.substringAfter(sb.toString(), "$").split("[^0-9.']+");                                            
                                order.lmtPrice(new Double(words[0].replaceAll("^\\p{Punct}+|\\p{Punct}+$","")));
                            } else {
                                order.lmtPrice(order.auxPrice()); // same thing for non Rickard's subs
                            }

                            if (sub.getBoolean("autoBuy")) {
                                
                                int requiredPos = 0;
                                // Test whether a reaffirm Rec.
                                if (sub.getDouble("affirmBuyLimit") > 0.0) {
                                    position = m_acctInfoPanel.findPosition(con);
                                    if (position != null)
                                        requiredPos = new Double(sub.getDouble("affirmBuyLimit")/order.lmtPrice()).intValue();                                    
                                }
                                
                                if (position == null)
                                    requiredPos = new Double(sub.getDouble("autoBuyLimit")/order.lmtPrice()).intValue();

                                if (requiredPos > 0) {
                                    if (con instanceof OptContract)
                                        requiredPos = requiredPos/100;
                                    if (requiredPos > 1)
                                        order.minQty(requiredPos/2);
                                    else if (requiredPos == 1)
                                        order.minQty(requiredPos);                                        
                                    order.totalQuantity(requiredPos);
                                    order.orderType(OrderType.LMT);

                                    m_controller.placeOrModifyOrder(con, order, null);
                                }
                            } 
                            
                            if (position == null)
                                insertRecommendation(con, order, sub.getString("_id"));
                        }

                        break; // out of loop for this 'Block'

                    } else { // continue building "Order String"
                        if (!line.isEmpty()) {
                            sb.append(line);
                            sb.append(" ");
                            if (order.action().equals(Types.Action.SSHORT))
                                if (StringUtils.containsIgnoreCase(line, "buy"))
                                    order.action(Types.Action.BUY);
                                else if (StringUtils.containsIgnoreCase(line, "sell"))
                                    order.action(Types.Action.SELL);
                                else if (StringUtils.containsIgnoreCase(line, "hold"))
                                    break; // out of this Block
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

    private void processEmailMsg(Message msg) throws IOException, MessagingException {
        
        InternetAddress email = (InternetAddress)msg.getFrom()[0];
        String subject = msg.getSubject();
        String address = email.getAddress();
        String personal = StringUtils.replacePattern(email.getPersonal(), "[^a-zA-Z0-9.\\ ]+", ""); // to remove ambiguous ' or ’ 
        logger.info("Email arrived from : [{}] <{}> with Subject: {}", personal, address, subject);

//                            if (address.equals(m_advisorfirm_email)) {
        for (Document sub: m_subs) {
             // indicative of a Buy or Sell Recommendation(s) embedded in the body
            String displayName = StringUtils.replacePattern(sub.getString("fullname"), "[^a-zA-Z0-9.\\ ]+", "");
            if ((StringUtils.equals(personal, displayName))&& 
                    (StringUtils.containsIgnoreCase(subject,sub.getString("sellSubjectToken")) || 
                    StringUtils.containsIgnoreCase(subject,sub.getString("buySubjectToken")) ||
                    StringUtils.containsIgnoreCase(subject,"%"))) {

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
                        }).start();break;
                    }
                }                                            
                return;
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
            IMAPStore store = (IMAPStore)session.getStore(m_protocol);
            store.connect(m_host, m_port, m_user, m_password);
            
            if (!store.hasCapability("IDLE")) {
                throw new RuntimeException("IDLE not supported");
            }

            m_folder = (IMAPFolder)store.getFolder(m_mbox);
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
                        logger.error("Wait 100ms and try One more time .... {}  ", me,  me);
                        try {
                            TimeUnit.MILLISECONDS.sleep(100);
                            ensureOpen();
                            processEmailMsgs(ev.getMessages()); // Obtain from Folder rather than Event this time
                        } catch (IOException | MessagingException | InterruptedException e) {
                            logger.error("Failed 2nd time so lost this msg {} ", e,  e);                        
                        } 
                    }
                }
            });
        } catch (MessagingException me) {
            logger.error("msg {}.", me,toString(), me);
            ensureOpen(); // recursive
        }        
    }
        
    @Override 
    public void connected() {
        logger.info("Connected to TWS");
        show( "connected");
        m_connectionPanel.m_status.setText( "connected");

        controller().reqCurrentTime((long time) -> {
            show( "Server date/time is " + Formats.fmtDate(time * 1000) );
        });

        controller().reqBulletins(true, (int msgId, NewsType newsType, String message, String exchange) -> {
            String str = String.format( "Received bulletin:  type=%s  exchange=%s", newsType, exchange);
            show( str);
            show( message);
        });
        
        if ((m_acctList.size() == 1)) {
            m_acctInfoPanel.activated();
            m_activated = true;
        }

    }

    @Override public void disconnected() {
        logger.info("Disconnected from TWS");
        show( "disconnected");
        m_connectionPanel.m_status.setText("disconnected");
    }

    @Override // Pmac
    public void storePosition(Position pos) {
        updateRecommendation1to3(pos);
        logger.info("Position Updated: {}", pos.toString());
    }
    

    @Override public void accountList(ArrayList<String> list) {
        show( "Received account list");
        m_acctList.clear();
        m_acctList.addAll( list);
    }

    @Override public void show( final String str) {
        SwingUtilities.invokeLater(() -> {
            if (!StringUtils.isEmpty(str)) {
                logger.info("Show: {}", str);
                m_msg.append(str);
                m_msg.append("\n\n");
                Dimension d = m_msg.getSize();
                m_msg.scrollRectToVisible( new Rectangle( 0, d.height, 1, 1) );
            }
        });
    }

    @Override public void error(Exception e) {
        show( e.toString() );
    }

    @Override public void message(int id, int errorCode, String errorMsg) {
        show( id + " " + errorCode + " " + errorMsg);
    }
	
    private class ConnectionPanel extends JPanel {
        
        private final JTextField m_host = new JTextField( m_connectionConfiguration.getDefaultHost(), 10);
        private final JTextField m_port = new JTextField( m_connectionConfiguration.getDefaultPort(), 7);
        private final JTextField m_connectOptionsTF = new JTextField( m_connectionConfiguration.getDefaultConnectOptions(), 30);
        private final JTextField m_clientId = new JTextField("0", 7);
        private final JLabel m_status = new JLabel("Disconnected");
        private final JLabel m_defaultPortNumberLabel = new JLabel("<html>Live Trading ports:<b> TWS: 7496; IB Gateway: 4001.</b><br>"
                    + "Simulated Trading ports for new installations of "
                        + "version 954.1 or newer: "
                        + "<b>TWS: 7497; IB Gateway: 4002</b></html>");

        public ConnectionPanel() {
                HtmlButton connect = new HtmlButton("Connect") {
                        @Override public void actionPerformed() {
                                onConnect();
                        }
                };

                HtmlButton disconnect = new HtmlButton("Disconnect") {
                        @Override public void actionPerformed() {
                                controller().disconnect();
                        }
                };

                JPanel p1 = new VerticalPanel();
                p1.add( "Host", m_host);
                p1.add( "Port", m_port);
                p1.add( "Client ID", m_clientId);
                if ( m_connectionConfiguration.getDefaultConnectOptions() != null ) {
                        p1.add( "Connect options", m_connectOptionsTF);
                }
                p1.add( "", m_defaultPortNumberLabel);

                JPanel p2 = new VerticalPanel();
                p2.add( connect);
                p2.add( disconnect);
                p2.add( Box.createVerticalStrut(20));

                JPanel p3 = new VerticalPanel();
                p3.setBorder( new EmptyBorder( 20, 0, 0, 0));
                p3.add( "Connection status: ", m_status);

                JPanel p4 = new JPanel( new BorderLayout() );
                p4.add( p1, BorderLayout.WEST);
                p4.add( p2);
                p4.add( p3, BorderLayout.SOUTH);

                setLayout( new BorderLayout() );
                add( p4, BorderLayout.NORTH);
        }

        protected void onConnect() {
            int port = Integer.parseInt( m_port.getText() );
            int clientId = Integer.parseInt( m_clientId.getText() );
            controller().connect( m_host.getText(), port, clientId, m_connectOptionsTF.getText());
        }
    }
	
    private static class PanelLogger implements ILogger {
        final private JTextArea m_area;

        PanelLogger( JTextArea area) {
                m_area = area;
        }

        @Override public void log(final String str) {
                SwingUtilities.invokeLater(() -> {
//					m_area.append(str);
//					
//					Dimension d = m_area.getSize();
//					m_area.scrollRectToVisible( new Rectangle( 0, d.height, 1, 1) );
                });
        }
    }
}

// Soup Stuff .....
//                org.jsoup.nodes.Document jsoupDoc = Jsoup.parse(block);
//                List<org.jsoup.nodes.Node> nodes = jsoupDoc.childNodes();
//                
//                for (org.jsoup.nodes.Node node: nodes) {
//                    String t = node.toString();
//                }
//   for ( Element element : doc.getAllElements() )
//    {
//        for ( TextNode textNode : element.textNodes() )
//        {
//            final String text = textNode.text();
//            builder.append( text );
//            appendWhitespaceAfterTextIfNotThere( builder, text );
//        }
//    }
//
//    return builder.toString();

// do clearing support
// change from "New" to something else
// more dn work, e.g. deltaNeutralValidation
// add a "newAPI" signature
// probably should not send F..A position updates to listeners, at least not to API; also probably not send FX positions; or maybe we should support that?; filter out models or include it 
// finish or remove strat panel
// check all ps
// must allow normal summary and ledger at the same time
// you could create an enum for normal account events and pass segment as a separate field
// check pacing violation
// newticktype should break into price, size, and string?
// give "already subscribed" message if appropriate

// BUGS
// When API sends multiple snapshot requests, TWS sends error "Snapshot exceeds 100/sec" even when it doesn't
// When API requests SSF contracts, TWS sends both dividend protected and non-dividend protected contracts. They are indistinguishable except for having different conids.
// Fundamentals financial summary works from TWS but not from API 
// When requesting fundamental data for IBM, the data is returned but also an error
// The "Request option computation" method seems to have no effect; no data is ever returned
// When an order is submitted with the "End time" field, it seems to be ignored; it is not submitted but also no error is returned to API.
// Most error messages from TWS contain the class name where the error occurred which gets garbled to gibberish during obfuscation; the class names should be removed from the error message 
// If you exercise option from API after 4:30, TWS pops up a message; TWS should never popup a message due to an API request
// TWS did not desubscribe option vol computation after API disconnect
// Several error message are misleading or completely wrong, such as when upperRange field is < lowerRange
// Submit a child stop with no stop price; you get no error, no rejection
// When a child order is transmitted with a different contract than the parent but no hedge params it sort of works but not really, e.g. contract does not display at TWS, but order is working
// Switch between frozen and real-time quotes is broken; e.g.: request frozen quotes, then realtime, then request option market data; you don't get bid/ask; request frozen, then an option; you don't get anything
// TWS pops up mkt data warning message in response to api order

// API/TWS Changes
// we should add underConid for sec def request sent API to TWS so option chains can be requested properly
// reqContractDetails needs primary exchange, currently only takes currency which is wrong; all requests taking Contract should be updated
// reqMktDepth and reqContractDetails does not take primary exchange but it needs to; ideally we could also pass underConid in request
// scanner results should return primary exchange
// the check margin does not give nearly as much info as in TWS
// need clear way to distinguish between order reject and warning

// API Improvements
// add logging support
// we need to add dividendProt field to contract description
// smart live orders should be getting primary exchange sent down

// TWS changes
// TWS sends acct update time after every value; not necessary
// support stop price for trailing stop order (currently only for trailing stop-limit)
// let TWS come with 127.0.0.1 enabled, same is IBG
// we should default to auto-updating client zero with new trades and open orders

// NOTES TO USERS
// you can get all orders and trades by setting "master id" in the TWS API config
// reqManagedAccts() is not needed because managed accounts are sent down on login
// TickType.LAST_TIME comes for all top mkt data requests
// all option ticker requests trigger option model calc and response
// DEV: All Box layouts have max size same as pref size; but center border layout ignores this
// DEV: Box layout grows items proportionally to the difference between max and pref sizes, and never more than max size

//TWS sends error "Snapshot exceeds 100/sec" even when it doesn't; maybe try flush? or maybe send 100 then pause 1 second? this will take forever; i think the limit needs to be increased

//req open orders can only be done by client 0 it seems; give a message
//somehow group is defaulting to EqualQuantity if not set; seems wrong
//i frequently see order canceled - reason: with no text
//Missing or invalid NonGuaranteed value. error should be split into two messages
//Rejected API order is downloaded as Inactive open order; rejected orders should never be sen
//Submitting an initial stop price for trailing stop orders is supported only for trailing stop-limit orders; should be supported for plain trailing stop orders as well 
//EMsgReqOptCalcPrice probably doesn't work since mkt data code was re-enabled
//barrier price for trail stop lmt orders why not for trail stop too?
//All API orders default to "All" for F; that's not good
