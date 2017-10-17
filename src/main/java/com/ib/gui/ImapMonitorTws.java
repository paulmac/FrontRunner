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
import com.mongodb.client.MongoCollection;
//import com.mongodb.client.MongoCollection;
//import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import static com.mongodb.client.model.Filters.*;
//import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.time.Month;
import java.time.Year;
import java.util.Arrays;
import org.bson.Document;

//import com.mongodb.client.model.CreateCollectionOptions;
//import com.mongodb.client.model.ValidationOptions;
//import com.paulmac.trade.ImapMonitor;
import java.util.Calendar;
//import java.util.Date;
//import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
//import java.util.Vector;
import java.util.concurrent.TimeUnit;
//import java.util.logging.Level;
//import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;

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
    private static final List<String> m_exchanges = Arrays.asList("NYSE", "NASDAQ", "OTC", "OTCBB", "TSX", "sup3"); // "NASDAQ", not included - 6 chars

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
                    logger.info("Folder.idle() start @ {}", Calendar.getInstance().getTime());
                    ((IMAPFolder) m_folder).idle();
                } catch (MessagingException me) {
                    logger.error(me.toString(), me);
                    try {
                        // Something went wrong, wait and try again
                        TimeUnit.MILLISECONDS.sleep(100);
                    } catch (InterruptedException ie) {
                        logger.error(ie.toString(), ie);
                    } catch (Exception e) {
                        logger.error(e.toString(), e);
                    }
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
                        logger.info("In ensureOpen(). Open Folder {} Successful.", m_folder);
                        if (!m_folder.isOpen())
                            throw new FolderClosedException(m_folder);
                    }

                    logger.info("In ensureOpen(). Folder {} is still Open.", m_folder, " @ ", Calendar.getInstance().getTime());                    
                    return;
                }
            } catch (MessagingException e) {
                logger.error("msg {}.", e.toString(), e);
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

//    Stuff to do with actually processing of incoming mail
    public void openFolder() {
        try {              
            logger.info("Entering openFolder()");
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
                /* Attempting to block the Folder Closed exceptyion with Synchronized*/ 
                public synchronized void messagesAdded(MessageCountEvent ev) { // pmac : not sure if I can use synchronized here?
                    
                    // ensureOpen();  // pmac : not sure if necessary?

                    Message[] msgs = ev.getMessages();
                    for (Message msg : msgs) {
                        do { // keep trying to process same msg until finished i.e until folder is re-opened                                
                            try {
                                InternetAddress email = (InternetAddress)msg.getFrom()[0];
                                String subject = msg.getSubject();
                                String address = email.getAddress();
                                String personal = StringUtils.replacePattern(email.getPersonal(), "[^a-zA-Z0-9.\\ ]+", ""); // to remove ambiguous ' or ’ 
                                logger.info("Email arrived from : [{}] <{}> with Subject: {}", personal, address, subject);
                                if (address.equals(m_advisorfirm_email)) {
                                    for (Document sub: m_subs) {
                                         // indicative of a Buy or Sell Recommendation(s) embedded in the body
                                        String displayName = StringUtils.replacePattern(sub.getString("fullname"), "[^a-zA-Z0-9.\\ ]+", "");
                                        if ((StringUtils.equals(personal, displayName))&& 
                                                (StringUtils.containsIgnoreCase(subject,sub.getString("sellSubjectToken")) || 
                                                StringUtils.containsIgnoreCase(subject,sub.getString("buySubjectToken")))) {
                                            // find and process the individual Recommendation(s) in the msg
                                            process((MimeMessage)msg, sub);
                                            return;
                                        }
                                    }
                                }
                            } catch (MessagingException me) {
                                logger.error("msg {}.", me,toString(), me);
                            }
                        } while (!m_folder.isOpen());
                    }
                }
            });

        } catch (MessagingException me) {
            logger.error("msg {}.", me,toString(), me);
            ensureOpen(); // recursive
        }        
    }

    public /* synchronized */ void process(Message msg, Document sub) {        
        try {            
            int lnctr = 0;

            Multipart multiPart = (Multipart) msg.getContent();
            
            for (int i = 0; i < multiPart.getCount(); i++) {
                MimeBodyPart part = (MimeBodyPart) multiPart.getBodyPart(i);
                if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
                    // this part is attachment
                    // code to save attachment...
                    logger.info("Part : {}", part.getDisposition());
                } else {
//                    boolean inSection = false;
//                    boolean tickerFound = false;
                    // this part may be the message content
//                    String plainContent = part.getContent().toString();
                    Contract con = null; // Contract created for each Recomendation or "Order Line"
                    Order order = null;
                    String line = null;
                    String orderStr = null;
                    String orderToken = sub.getString("orderToken");
                    String eomToken = sub.getString("eomToken");
                    BufferedReader reader = new BufferedReader(new InputStreamReader(part.getInputStream()));

                    do {
                        line = reader.readLine();
                        logger.info("Line {} : {}", lnctr++, line);

                        // Keep reading until line contains an Order Token
                        if  (StringUtils.containsIgnoreCase(line, orderToken)) { // containsorderStr.toLowerCase().contains("option")) {

                            con = null; // Contract created for each Recomendation or "Order Line"
                            
                            order = new Order();
                            order.action(Types.Action.SSHORT); // Pmac: Should modify also in Ctor?

                            // SB initialised with rest of line ( omitting Order Token itself)
                            StringBuilder sb = new StringBuilder(line.substring(orderToken.length())); 
                            // Read until Order line is found, can be same line, next line or line after a space
                             while (order.action().equals(Types.Action.SSHORT)) {
                                if (StringUtils.containsIgnoreCase(line, "buy"))
                                    order.action(Types.Action.BUY);
                                else if (StringUtils.containsIgnoreCase(line, "sell"))
                                    order.action(Types.Action.SELL);
                                
                                // Do this anyway as the 'option' keyword might be on the next line
                                line = reader.readLine();
                                if (StringUtils.isNotBlank(line)) {
                                    sb.append(line);
                                    sb.append(" "); // so last word and first word of next string don't merge
                                }
                            }

                            orderStr = sb.toString();
                            logger.info("{} Order: {} ", order.action(), orderStr);

                            if  (StringUtils.containsIgnoreCase(orderStr, "option")) { // containsorderStr.toLowerCase().contains("option")) {
                                
                                String[] words = orderStr.split("[^a-zA-Z0-9.']+"); // \\P{Alpha}+ matches any non-alphabetic character '.' is for a possible decimal point
                                String w = words[0];
                                for (int j=0; j < words.length; j++) {
                                    w =  words[j];
                                    if (Pattern.matches("([A-Z]{2,5})", w) && !m_exchanges.contains(w)) {
                                        
                                        con = new OptContract(words[j++]); // ticker
                                        // Obtain : lastTradeDateOrContractMonth, double strike, String right) {
                                        String m = words[j++];
                                        Month month = Month.valueOf(m.toUpperCase());
                                        String y = words[j++];
                                        Year year = Year.of(new Integer(y));
                                        Calendar c = Calendar.getInstance();
                                        c.set(year.getValue(), month.ordinal(), 15, 0, 0);  
                                        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
                                        con.lastTradeDateOrContractMonth(sdf.format(c.getTime()));
                                        con.strike(new Double(words[j++]));
                                        con.right(words[j++].toUpperCase());
                                        
                                        if (order.action().equals(Types.Action.BUY)) {
                                            while (!(words[j].equals("up") && words[j+1].equals("to")))
                                                j++;
                                            
                                            order.auxPrice(new Double(words[j+2]));
                                            
                                            // Rickard's subs offer an assumed entry price, somewhere after "assuming ..."
                                            // We use this as our 'limitPrice'
                                            if (sub.getString("fullname").contains("Rickards")) {                                            
                                                do {                                                
                                                    line = reader.readLine();
                                                    logger.info("Line {} : {}", lnctr++, line);
                                                } while (!StringUtils.containsIgnoreCase(line, "assuming"));

                                                sb = new StringBuilder(StringUtils.substringAfter(line, "ssuming")); // avoid capital/lowercase issue for 'A' 
                                                sb.append(" "); // so last word and first word of next string don't merge
                                                sb.append(reader.readLine()); // "assuming a $limitPrice .." may span more than 1 line
                                                words = StringUtils.substringAfter(sb.toString(), "$").split("[^0-9.']+");                                            
                                                order.lmtPrice(new Double(words[0]));
                                            } else {
                                                order.lmtPrice(order.auxPrice()); // same thing for non Rickard's subs
                                            }
                                        }

                                        logger.info("Option Contract: {} Order: {}", con.localSymbol(), order.toString());
                                    }
                                }
                            } else {
                                String[] words = orderStr.split("\\P{Alpha}+"); // matches any non-alphabetic character
                                for (String w:words) {                                    
                                    if (Pattern.matches("([A-Z]{2,5})", w) && !m_exchanges.contains(w)) {
                                        con = new StkContract(w);
                                        break;
                                    }
                                }
                            }                                            

                            if (order.action().equals(Types.Action.SELL)) {
                                // Require Position from 'long store' PortfolioMap. Compare the Contract 
                                Position position = m_acctInfoPanel.findPosition(con);
                                if (position != null) {
                                    order.totalQuantity(position.position());
                                    order.orderType(OrderType.MKT);
                                    logger.info("Sell {} of {}", order.totalQuantity(), con.localSymbol());
                                } else {                                  
                                    logger.info("No Position for {}", con.description());
                                    continue;
                                }
                            } else {
                                logger.info("Buy Order Stored in MongoDB");
                                
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
//                                rec.put("autoBuy", sub.getBoolean("autoBuy"));
//                                rec.put("autoBuyLimit", sub.getDouble("autoBuyLimit"));
                                rec.put("subscription_id", sub.get("_id"));
                                
                                Document pos = new Document();
                                pos.put("_id", con.localSymbol());
                                pos.put("recommedation_id", con.localSymbol()); // same for first (default) position
                                pos.put("exchange", "SMART");   // default
                                pos.put("currency", "USD");     // default
                                pos.put("position", 0);         // initially, basically a placeholder
                                pos.put("averagePrice", 0.0);   // initially, basically a placeholder
                                pos.put("currentPrice", 0.0);   // initially, basically a placeholder
                                pos.put("percentGain", 0.0);    // initially, basically a placeholder
                                
                                rec.append("positions", pos);
//                                List<Document> positions = new ArrayList<Document>();
//                                MongoCollection<Document> positions = (MongoCollection<Document>)pos.get("positions");
//                                MongoCollection<Document> pos = new MongoCollection<Document>();
//                                positions.add(pos);
//                                rec.put("positions", positions);                                
                                m_recommendations.insertOne(rec);
//                                sub.replace("recommendations", recs);
//                                m_advisorfirms.replace(line, line)

                                if (sub.getBoolean("autoBuy")) {
                                    // Use upToLimit position requested.
                                    int requiredPos = new Double(sub.getDouble("autoBuyLimit")/order.auxPrice()).intValue();
                                    order.minQty(requiredPos);
                                    order.lmtPrice(i);
                                    order.orderType(OrderType.LMT);
                                } else {
                                    continue;
                                }
                            }
                            
                           m_controller.placeOrModifyOrder(con, order, null);

                        } // if (line.contains(orderToken))                        
                    // Continue until "end of mail"
                    } while (!StringUtils.containsIgnoreCase(line, eomToken) && (line != null));
                    
                    break; // out of for loop around Body Parts
                }
            }
        } catch (IOException | NumberFormatException | MessagingException e) {
            logger.error("msg {}.", e, e);
        } catch (Exception e) {
            logger.error("msg {}.", e, e);            
        } finally { 
            logger.info("Finisned processing msg from : {}", sub.get("fullname"));
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
            m_connectionPanel.m_status.setText( "disconnected");
    }

    @Override // Pmac
    public void storePosition(Position pos) {
//        logger.info("Position Updated: {} pos={}", pos.contract().localSymbol(), pos.position());
        logger.info("Position Updated: {} ", pos.toString());
    }
    

    @Override public void accountList(ArrayList<String> list) {
            show( "Received account list");
            m_acctList.clear();
            m_acctList.addAll( list);
    }

    @Override public void show( final String str) {
        logger.info("Show: {}", str);
        SwingUtilities.invokeLater(() -> {
            m_msg.append(str);
            m_msg.append( "\n\n");

            Dimension d = m_msg.getSize();
            m_msg.scrollRectToVisible( new Rectangle( 0, d.height, 1, 1) );
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
