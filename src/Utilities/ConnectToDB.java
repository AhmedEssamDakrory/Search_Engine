import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bson.Document;

import com.mongodb.MongoClient;
import com.mongodb.MongoCommandException;
import com.mongodb.MongoCredential;

public class ConnectToDB { 
	private static MongoClient mongo;
	private static MongoCredential credential;
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
	
	private static void createCrawlerCollections() {
		try {
			database.createCollection("crawler_info");
		} catch(MongoCommandException e) {
			//
		}
		 
		
	}
	
	private static void dropCrawlerCollections() {
		MongoCollection<Document> collection = database.getCollection("crawler_info");
		collection.drop();
	}
	
	public static void insertUrlToBeCrawled(String url) {
		MongoCollection<Document> collection = database.getCollection("crawler_info");
		Document doc = new Document()
				.append("url", url)
				.append("crawled", false)
				.append("popularity", 0);
		collection.insertOne(doc);
	}
	
	public static void markUrlAsCrawled(String url) {
		MongoCollection<Document> collection = database.getCollection("crawler_info");
		collection.updateOne(Filters.eq("url", url), Updates.set("crawled", true));
	}
	
	public static void incUrlPopularity(String url) {
		MongoCollection<Document> collection = database.getCollection("crawler_info");
		FindIterable<Document> iterDoc = collection.find(Filters.eq("url", url));
		int popularity = iterDoc.first().getInteger("popularity");
		System.out.println(popularity);
		collection.updateOne(Filters.eq("url", url), Updates.set("popularity", popularity+1));
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
	
	public static void clearDB() {
		//***************** drop all collections**********************
		dropCrawlerCollections();
		
	}
	
	
	
	

	
	public static void main( String args[] ) {  
//		ConnectToDB.clearDB();
		ConnectToDB.establishConnection("search_engine", "team", "12345");
		ConnectToDB.clearDB();
		ConnectToDB.init();
//		ConnectToDB.createCrawlerCollections();
		ConnectToDB.insertUrlToBeCrawled("https://google");
		ConnectToDB.insertUrlToBeCrawled("https://googlegggggggggggg");
		ConnectToDB.insertUrlToBeCrawled("https://googlehhhhhhhhhhh");
		ConnectToDB.markUrlAsCrawled("https://googlehhhhhhhhhhh");
		List<String> l = ConnectToDB.getAllNotCrawledUrls();
		for(String url : l) {
			System.out.println(url);
		}
//		ConnectToDB.incUrlPopularity("https://google");
		
	} 
}