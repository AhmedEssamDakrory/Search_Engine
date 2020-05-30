package main.utilities;

import com.mongodb.MongoClientURI;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;

import java.io.IOException;
import java.util.*;

import org.bson.Document;

import com.mongodb.MongoClient;
import com.mongodb.MongoCommandException;
import org.bson.conversions.Bson;

import static com.mongodb.client.model.Aggregates.*;

public class ConnectToDB {
	private static MongoClient mongo;
	private static MongoDatabase database;

	private static MongoCollection imagesIndexCollection;
	private static MongoCollection invertedIndexCollection;
	private static MongoCollection crawlerInfoCollection;

	public static void establishConnection() {
		mongo = new MongoClient(new MongoClientURI(Constants.DATABASE_ADDRESS));
	    System.out.println("Connected to the database successfully");
	    database = mongo.getDatabase(Constants.DATABASE_NAME);
		imagesIndexCollection = database.getCollection("imagesIndex");
		invertedIndexCollection = database.getCollection("invertedIndex");
		crawlerInfoCollection = database.getCollection("crawler_info");
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

	//---------Indexer---------
	public static void pushToDatabase(String url, HashMap<String, Integer> words, Integer totalScore){
		removeUrlFromDatabase(url);
		for (String word: words.keySet()){
			float score = (float)words.get(word) / totalScore;
			invertedIndexCollection.updateOne(Filters.eq("_id", word),

					new org.bson.Document("$push", new org.bson.Document("urls",
							new org.bson.Document("url", url).append("score", score))),

					new UpdateOptions().upsert(true));
		}
		crawlerInfoCollection.updateOne(Filters.eq("url", url),
				Updates.set("visited", false));
	}

	public static void removeUrlFromDatabase(String url){
		invertedIndexCollection.updateMany(new org.bson.Document(),
				Updates.pull("urls", new org.bson.Document("url", url)));
		invertedIndexCollection.deleteMany(Filters.size("urls", 0));
	}

	public static void pushImageToDatabase(String src, HashMap<String, Integer> words, Integer totalScore){
		removeImageFromDatabase(src);
		for (String word: words.keySet()){
			float score = (float)words.get(word) / totalScore;
			imagesIndexCollection.updateOne(Filters.eq("_id", word),

					new org.bson.Document("$push", new org.bson.Document("images",
							new org.bson.Document("image", src).append("score", score))),

					new UpdateOptions().upsert(true));
		}
	}

	public static void removeImageFromDatabase(String src){
		imagesIndexCollection.updateMany(new org.bson.Document(),
				Updates.pull("images", new org.bson.Document("image", src)));
		imagesIndexCollection.deleteMany(Filters.size("images", 0));
	}

	public static FindIterable pullNotVisitedURLs(){
		return crawlerInfoCollection.find(Filters.eq("visited", true));
	}

	// Ranker
	public static AggregateIterable findTextMatches(String word)
	{
		Bson match = match(Filters.eq("_id", word));
		Bson unwind1 = unwind("$urls");
		Bson project1 = project(Projections.fields(
				Projections.computed("url", "$urls.url"),
				Projections.computed("score", "$urls.score")));
		Bson lookup = lookup("crawler_info", "url", "url", "crawled_info");
		Bson unwind2 = unwind("$crawled_info");
		Bson project2 = project(Projections.fields(
				Projections.excludeId(),
				Projections.include("url", "score"),
				Projections.computed("popularity", "$crawled_info.popularity"),
				Projections.computed("id", "$crawled_info._id")));
		// TODO: icon, title, description

		List<Bson> pipeline = Arrays.asList(match, unwind1, project1, lookup, unwind2, project2);
		return invertedIndexCollection.aggregate(pipeline);
	}

	public static AggregateIterable findImageMatches(String word)
	{
		Bson match = match(Filters.eq("_id", word));
		Bson unwind1 = unwind("$images");
		Bson project1 = project(Projections.fields(
				Projections.computed("url", "$images.url"),
				Projections.computed("image", "$images.image"),
				Projections.computed("score", "$images.score")
		));
		Bson lookup = lookup("crawler_info", "url", "url", "crawled_info");
		Bson unwind2 = unwind("$crawled_info");
		Bson project2 = project(Projections.fields(
				Projections.excludeId(),
				Projections.include("url", "image", "score"),
				Projections.computed("popularity", "$crawled_info.popularity"),
				Projections.computed("id", "$crawled_info._id")
		));
		// TODO: title

		List<Bson> pipeline = Arrays.asList(match, unwind1, project1, lookup, unwind2, project2);
		return imagesIndexCollection.aggregate(pipeline);
	}

	public static void clearDB() {
		//***************** drop all collections**********************
		dropCrawlerCollections();
		invertedIndexCollection.drop();
		imagesIndexCollection.drop();
	}

	public static void closeConnection(){
		mongo.close();
	}
	public static void main(String[] args) throws IOException, InterruptedException {
//		ConnectToDB.establishConnection("search_engine", "team", "1234");
//		ConnectToDB.clearDB();
//		ConnectToDB.init();
//		ConnectToDB.insertUrlToBeCrawled("a");
//		System.out.println(ConnectToDB.getCrawledUrlID("a"));
	}

}