/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pmac.mongo;

import com.ib.client.Contract;
import com.ib.client.Order;
import com.ib.controller.Position;
//import com.ib.gui.FrontRunnerTws;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;
import com.mongodb.client.result.UpdateResult;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Paul
 */
public class MongoController {
    
    final static Logger logger = LoggerFactory.getLogger(MongoController.class);

    public class ImapCoords {
        public String m_mbox; // = "INBOX"; // Should be something else
        public String m_user;
        public String m_password;
        public String m_protocol;
        public String m_host;
        public String m_email_address; // = "paulmac1914@yahoo.com";
        public int m_port;
    }
    
    private MongoClient mongoClient; // = new MongoClient();
    private MongoDatabase database; // = mongoClient.getDatabase("frontrunner");
    private MongoCollection<Document> m_recommendations;
    private Document m_advisorfirm;
    static private List<Document> m_subs;
    public static String DB_NAME = "frontrunner";
    public static ImapCoords IMAP_COORDS;    
    private String m_account = null;

    public List<Document> getSubs() {
        return m_subs;
    }
    
    public void init(String account) {
        
        IMAP_COORDS = new ImapCoords();

        m_account = account;
        
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
        database = mongoClient.getDatabase(DB_NAME);

        m_advisorfirm = database.getCollection("advisorfirms").find().first();
        logger.info("Sub Provider {}", m_advisorfirm.toJson());
//        m_recommendations = database.getCollection("recommendations");
        m_subs = (List<Document>) m_advisorfirm.get("subscriptions");
        m_subs.forEach((sub) -> {
            logger.info("sub : {} ", sub);
        });

        IMAP_COORDS.m_email_address = m_advisorfirm.getString("email");
        IMAP_COORDS.m_mbox = m_advisorfirm.getString("mailbox");
        Document emailConfig = database.getCollection("emailconfig").find(eq("_id", m_advisorfirm.getString("emailconfig_id"))).first();
        logger.info("Email Config : {}", emailConfig.toJson());
        IMAP_COORDS.m_user = emailConfig.getString("user");
        IMAP_COORDS.m_password = emailConfig.getString("password");
        Document imapConfig = (Document)emailConfig.get("imap");
        IMAP_COORDS.m_protocol = imapConfig.getString("protocol");
        IMAP_COORDS.m_host = imapConfig.getString("host");
        IMAP_COORDS.m_port = ((Number)imapConfig.get("port")).intValue();

        logger.info("Imap CoOrds : {} ", IMAP_COORDS.toString());
    }     

    public void createRecommendation(Contract con, Order order, String subId) {
        
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

            // The default Position as quoted from Rec, other Positions may be added as we go along
            Document posBson = new Document();
            posBson.put("_id", con.localSymbol());  // Composite Key including Account
            posBson.put("recommedation_id", con.localSymbol()); // same for first (default) position
            posBson.put("exchange", "SMART");   // default
            posBson.put("entryDate", "");       // Can be different to the received date of the Recommendation
            posBson.put("currency", "USD");     // default
            posBson.put("position", 0);         // initially, basically a placeholder
            posBson.put("previousMarketPrice", 0);      // initially, basically a placeholder
            posBson.put("marketPrice", 0);      // initially, basically a placeholder
            posBson.put("marketValue", 0);      // initially, basically a placeholder
            posBson.put("averageCost", 0);      // initially, basically a placeholder
            posBson.put("unrealPnl", 0);        // initially, basically a placeholder
            posBson.put("realPnl", 0);          // initially, basically a placeholder

//            List<Document> positions = new ArrayList<Document>();
//            positions.add(posBson);
//            rec.put("positions", positions);

//            MongoCollection<Document> positions = new MongoCollection<Document>();
            rec.append("positions", posBson);
            database.getCollection("recommendations").insertOne(rec);

            logger.info("New Rec : {} Stored in MongoDB", con.localSymbol());

        } catch (com.mongodb.MongoWriteException e) {
            logger.error("msg {}.", e, e);
        }    
    }

    public void updatePosition(Position pos) {                
        // The Position Collection is usually only 1 element so for efficiency search based on first element of Coll ection 
//         FindIterable<Document> itr = m_recommendations.find(eq("positions._id", pos.contract().localSymbol()));
//        String account = m_acctList.get(0);
        String localSymbol = pos.contract().localSymbol();
         
        logger.info("Position from TWS {}, Account {}, description, {}", pos, m_account, pos.contract().description());

        m_recommendations = database.getCollection("recommendations");
        MongoCursor<Document> itr = m_recommendations.find(eq("positions._id", localSymbol)).iterator();
//        MongoCursor<Document> itr = database.getCollection("recommendations").find(eq("_id", localSymbol)).iterator();
        if (itr.hasNext()) {
            Document rec = itr.next();

            logger.info("Recomendation {}, Account {}", rec, m_account);
            
            Document position = null;
            UpdateResult result = null;

            if (rec.get("positions").getClass().equals(Document.class)) {
                position = (Document)rec.get("positions");
                // Update Pos Doc directly in Mongo
                result = m_recommendations.updateOne(eq("_id", localSymbol), 
                combine(set("positions.position", pos.position()), 
                        set("positions.previousMarketPrice", position.get("marketPrice")), 
                        set("positions.marketPrice", pos.marketPrice()), 
                        set("positions.marketValue", pos.marketValue()), 
                        set("positions.unrealPnl", pos.unrealPnl()), 
                        set("positions.realPnl", pos.realPnl()), 
                        set("positions.averageCost", pos.averageCost()) 
                        )
                    );
                } else {
                // Locate and replace PosDoc
                List<Document> positions = ((List<Document>)rec.get("positions"));
                Iterator iter = positions.iterator();
                while (iter.hasNext()) {
                    position = (Document)iter.next();
                    if (position.get("_id").equals(localSymbol)) {
                        // update position
                        position.replace("position", pos.position()); 
                        position.replace("previousMarketPrice", position.get("marketPrice")); 
                        position.replace("marketPrice", pos.marketPrice()); 
                        position.replace("marketValue", pos.marketValue()); 
                        position.replace("unrealPnl", pos.unrealPnl()); 
                        position.replace("averageCost", pos.averageCost());
                        
                        result = m_recommendations.replaceOne(eq("positions._id", localSymbol), rec); //updateOne(eq("positions._id", pos.contract().localSymbol()), 
//                        logger.info("Updated Mongo - getMatchedCount : {}, getModifiedCount : {}", result.getMatchedCount(), result.getModifiedCount());

                        break;
                    }
                }                
            }
            
            if (result.wasAcknowledged() && result.isModifiedCountAvailable())
                logger.info("Updated in Mongo Recommedation  {}, Position {}, Current Market Value {}", localSymbol, pos.position(), pos.marketValue());
            else 
                logger.error("Recommedation  {}, Position {},  Current Market Value {} NOT FOUND in DB?!", localSymbol, pos.position(), pos.marketValue());

         } else {
             // Look up cross reference table to find linked symbol. May be for example a Canadian stock.
             // Create a new Position in Mongo attached to default Recomendation
             logger.info("No Recommedation found for this Position {}", pos);
         }
    }

}
