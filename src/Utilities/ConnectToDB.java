import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bson.Document;

import com.mongodb.MongoClient;
import com.mongodb.MongoCommandException;

public class ConnectToDB { 
	private static MongoClient mongo;
//	private static MongoCredential credential;
	private static MongoDatabase database;
	
	public static void establishConnection(String dbName, String userName, String password) {
		mongo = new MongoClient( "localhost" , 27017 );
//	    credential = MongoCredential.createCredential(userName, dbName,
//	       password.toCharArray());
	    System.out.println("Connected to the database successfully");  
	    database = mongo.getDatabase(dbName);
//	    System.out.println("Credentials ::"+ credential);

	}
	
	public static void init() {
	    createCrawlerCollections();
	}
	
	public static void createCrawlerCollections() {
		try {
			database.createCollection("crawler_info");
		} catch(MongoCommandException e) {
			//
		}
	}
	
	public static void dropCrawlerCollections() {
		MongoCollection<Document> collection = database.getCollection("crawler_info");
		collection.drop();
	}
	
	public static void insertUrlToBeCrawled(String url) {
		MongoCollection<Document> collection = database.getCollection("crawler_info");
		Document doc = new Document()
				.append("url", url)
				.append("crawled", false)
				.append("visited", false)
				.append("popularity", 0);
		collection.insertOne(doc);
	}
	
	public static boolean checkIfCrawledBefore(String url) {
		MongoCollection<Document> collection = database.getCollection("crawler_info");
		Document doc = collection.find(Filters.eq("url", url)).first();
		if(doc == null) return false;
		return true;
	}
	
	public static void markAsVisited(String url) {
		MongoCollection<Document> collection = database.getCollection("crawler_info");
		collection.updateOne(Filters.eq("url", url), Updates.set("visited", true));
	}
	
	public static void markUrlAsCrawled(String url) {
		MongoCollection<Document> collection = database.getCollection("crawler_info");
		collection.updateOne(Filters.eq("url", url), Updates.set("crawled", true));
	}
	
	public static void incUrlsPopularity(String url) {
		MongoCollection<Document> collection = database.getCollection("crawler_info");
		FindIterable<Document> iterDoc = collection.find(Filters.eq("url", url));
		synchronized(iterDoc) {
			try {
				int popularity = iterDoc.first().getInteger("popularity");
				collection.updateOne(Filters.eq("url", url), Updates.set("popularity", popularity+1));
			} catch(NullPointerException e){
				//
			}
		}
	}
	
	public static String getCrawledUrlID(String url) {
		MongoCollection<Document> collection = database.getCollection("crawler_info");
		Document doc = collection.find(Filters.eq("url", url)).first();
		String id = doc.get("_id").toString();
		return id;
	}
	
	public static List<String> getAllNotCrawledUrls(){
		MongoCollection<Document> collection = database.getCollection("crawler_info");
		FindIterable<Document> iterDoc = collection.find(Filters.eq("crawled", false));
		List<String> urls = new ArrayList<String>();
		Iterator<Document> it = iterDoc.iterator();
		while (it.hasNext()) {
			urls.add(((Document)it.next()).getString("url"));
		}
		return urls;
	}
	
	public static void seededUrlsToCrawl(List<String> l) {
		MongoCollection<Document> collection = database.getCollection("crawler_info");
		List<Document> ld = new ArrayList<Document>();
		for(String url : l) {
			Document doc = new Document()
					.append("url", url)
					.append("crawled", false)
					.append("visited", false)
					.append("popularity", 0);
			ld.add(doc);
		}
		collection.insertMany(ld);
	}
	public static void deleteUrl(String url) {
		MongoCollection<Document> collection = database.getCollection("crawler_info");
		collection.deleteOne(Filters.eq("url", url));
	}
	
	public static void clearDB() {
		//***************** drop all collections**********************
		dropCrawlerCollections();
		
	}
	
	public static void main(String[] args) throws IOException, InterruptedException {
//		ConnectToDB.establishConnection("search_engine", "team", "1234");
//		ConnectToDB.clearDB();
//		ConnectToDB.init();
//		ConnectToDB.insertUrlToBeCrawled("a");
//		System.out.println(ConnectToDB.getCrawledUrlID("a"));
	}
	 
}