package main.utilities;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoCommandException;
import com.mongodb.client.*;
import com.mongodb.client.model.*;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.*;

import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Updates.*;

public class ConnectToDB {
    private static MongoClient mongo;
    private static MongoDatabase database;

    private static MongoCollection<Document> imagesIndexCollection;
    private static MongoCollection<Document> invertedIndexCollection;
    private static MongoCollection<Document> forwardIndexCollection;
    private static MongoCollection<Document> crawlerInfoCollection;
    private static MongoCollection<Document> suggestionsCollection;
    private static MongoCollection<Document> usersCollection;

    public static void establishConnection() {
        if (mongo != null) {
            System.out.println("Already connected to database");
            return;
        }
        mongo = new MongoClient(new MongoClientURI(Constants.DATABASE_ADDRESS));
        System.out.println("Connected to the database successfully");
        database = mongo.getDatabase(Constants.DATABASE_NAME);
        imagesIndexCollection = database.getCollection("imagesIndex");
        invertedIndexCollection = database.getCollection("invertedIndex");
        forwardIndexCollection = database.getCollection("forwardIndex");
        crawlerInfoCollection = database.getCollection("crawler_info");
        suggestionsCollection = database.getCollection("suggestions");
        usersCollection = database.getCollection("users");

    }

    public static void init() {
        createCrawlerCollections();
    }

    public static void createCrawlerCollections() {
        try {
            database.createCollection("crawler_info");
        } catch (MongoCommandException e) {
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
                .append("indexed", false)
//                .append("popularity", 0);
                .append("outgoing", Collections.emptyList());
        collection.insertOne(doc);
    }

    public static boolean checkIfCrawledBefore(String url) {
        MongoCollection<Document> collection = database.getCollection("crawler_info");
        Document doc = collection.find(Filters.eq("url", url)).first();
        return doc != null;
    }

    public static void markUrlAsCrawled(String url) {
        MongoCollection<Document> collection = database.getCollection("crawler_info");
        collection.updateOne(Filters.eq("url", url), new Document("$set", new Document("crawled", true).append("indexed", false)));
    }

    public static void incUrlsPopularity(String url) {
        MongoCollection<Document> collection = database.getCollection("crawler_info");
        FindIterable<Document> iterDoc = collection.find(Filters.eq("url", url));
        synchronized (iterDoc) {
            try {
                int popularity = iterDoc.first().getInteger("popularity");
                collection.updateOne(Filters.eq("url", url), Updates.set("popularity", popularity + 1));
            } catch (NullPointerException e) {
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

    public static List<String> getAllNotCrawledUrls() {
        MongoCollection<Document> collection = database.getCollection("crawler_info");
        FindIterable<Document> iterDoc = collection.find(Filters.eq("crawled", false));
        List<String> urls = new ArrayList<String>();
        Iterator<Document> it = iterDoc.iterator();
        while (it.hasNext()) {
            urls.add(it.next().getString("url"));
        }
        return urls;
    }

    public static void seededUrlsToCrawl(List<String> l) {
        MongoCollection<Document> collection = database.getCollection("crawler_info");
        List<Document> ld = new ArrayList<Document>();
        for (String url : l) {
            Document doc = new Document()
                    .append("url", url)
                    .append("crawled", false)
                    .append("indexed", false)
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
    public static void pushToDatabase(String url, String title, HashMap<String, Integer> words, Integer totalScore) {
        removeUrlFromDatabase(url);
        for (String word : words.keySet()) {
            float score = (float) words.get(word) / totalScore;
            invertedIndexCollection.updateOne(Filters.eq("_id", word),

                    new org.bson.Document("$push", new org.bson.Document("urls",
                            new org.bson.Document("url", url).append("score", score))),

                    new UpdateOptions().upsert(true));
        }

        forwardIndexCollection.updateOne(Filters.eq("_id", url),
                Updates.set("title", title),
                new UpdateOptions().upsert(true));

        crawlerInfoCollection.updateOne(Filters.eq("url", url),
                Updates.set("indexed", true));
    }

    public static void removeUrlFromDatabase(String url) {
        invertedIndexCollection.updateMany(new org.bson.Document(),
                Updates.pull("urls", new org.bson.Document("url", url)));
        invertedIndexCollection.deleteMany(Filters.size("urls", 0));
    }

    public static void pushImageToDatabase(String url, String src, String title, HashMap<String, Integer> words, Integer totalScore) {
        removeImageFromDatabase(src);
        for (String word : words.keySet()) {
            float score = (float) words.get(word) / totalScore;
            imagesIndexCollection.updateOne(Filters.eq("_id", word),

                    new org.bson.Document("$push", new org.bson.Document("images",
                            new org.bson.Document("url", url).append("image", src).append("score", score))),

                    new UpdateOptions().upsert(true));
        }
    }

    public static void removeImageFromDatabase(String src) {
        imagesIndexCollection.updateMany(new org.bson.Document(),
                Updates.pull("images", new org.bson.Document("image", src)));
        imagesIndexCollection.deleteMany(Filters.size("images", 0));
    }

    public static FindIterable<Document> pullNotIndexedURLs() {
        return crawlerInfoCollection.find(new Document("crawled", true).append("indexed", false));
    }

    // Ranker
    public static AggregateIterable<Document> findTextMatches(String word) {
        Bson match = match(Filters.eq("_id", word));
        Bson unwind1 = unwind("$urls");
        Bson project1 = project(Projections.fields(
                Projections.computed("url", "$urls.url"),
                Projections.computed("score", "$urls.score")
        ));
        Bson lookup = lookup("crawler_info", "url", "url", "crawled_info");
        Bson unwind2 = unwind("$crawled_info");
        Bson project2 = project(Projections.fields(
                Projections.excludeId(),
                Projections.include("url", "score"),
                Projections.computed("popularity", "$crawled_info.popularity"),
                Projections.computed("id", "$crawled_info._id")
        ));
        Bson lookup2 = lookup("forwardIndex", "url", "_id", "title_url");
        Bson unwind3 = unwind("$title_url");
        Bson project3 = project(Projections.fields(
                Projections.include("id", "url", "score", "popularity"),
                Projections.computed("title", "$title_url.title")
        ));
        // TODO: icon, description

        List<Bson> pipeline = Arrays.asList(match, unwind1, project1, lookup, unwind2, project2, lookup2, unwind3, project3);
        return invertedIndexCollection.aggregate(pipeline);
    }

    public static AggregateIterable<Document> findImageMatches(String word) {
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
//                Projections.computed("rank", "$crawled_info.rank"),
                Projections.computed("id", "$crawled_info._id")
        ));
        Bson lookup2 = lookup("forwardIndex", "url", "_id", "title_url");
        Bson unwind3 = unwind("$title_url");
        Bson project3 = project(Projections.fields(
                Projections.include("id", "url", "image", "score"),
                Projections.computed("title", "$title_url.title")
        ));

        List<Bson> pipeline = Arrays.asList(match, unwind1, project1, lookup, unwind2, project2, lookup2, unwind3, project3);
        return imagesIndexCollection.aggregate(pipeline);
    }
    
    public static int countAllDocs()
    {
        Bson match = Filters.eq("indexed", true);
        return (int)(crawlerInfoCollection.count(match));
    }

    public static int countTermDocs(String word)
    {
        Bson match = match(Filters.eq("_id", word));
        Bson unwind = unwind("$urls");
        Bson project = project(Projections.fields(
                Projections.computed("url", "$urls.url"),
                Projections.computed("score", "$urls.score")
        ));
        Bson count = count();

        List<Bson> pipeline = Arrays.asList(match, unwind, project, count);
        AggregateIterable<Document> result = invertedIndexCollection.aggregate(pipeline);
        if (result.first() == null)
        {
            return 0;
        }
        return Integer.parseInt(result.first().getOrDefault("count", 0).toString());
    }

    public static AggregateIterable<Document> getAllCrawledData()
    {
        Bson match = match(Filters.eq("indexed", true));
        Bson project = project(Projections.fields(
                Projections.computed("url", "$url"),
                Projections.computed("id", "$_id")
        ));

        List<Bson> pipeline = Arrays.asList(match, project);
        return crawlerInfoCollection.aggregate(pipeline);
    }

    public static boolean isUrlIndexed(String url)
    {
        Bson find = Filters.eq("url", url);
        FindIterable<Document> result = crawlerInfoCollection.find(find);
        if ((result.first() != null) && (!result.first().isEmpty()))
        {
            return Boolean.parseBoolean(result.first().getOrDefault("indexed", "false").toString());
        }
        return false;
    }

    public static void addOutgoingLink(String from, String to)
    {
        Bson filter = Filters.eq("url", from);
        Bson update = addToSet("outgoing", to);

        crawlerInfoCollection.updateOne(filter, update);
    }

    public static AggregateIterable<Document> getOutgoingLinks(String url)
    {
        Bson match = match(Filters.eq("url", url));
        Bson unwind = unwind("outgoing");
        Bson project = project(Projections.fields(
                Projections.computed("outlink", "$outgoing")
        ));

        List<Bson> pipeline = Arrays.asList(match, unwind, project);
        return crawlerInfoCollection.aggregate(pipeline);
    }

    public static void addSuggestion(String suggestion) {
        suggestion = suggestion.trim().toLowerCase();
        Bson filter = Filters.eq("_id", suggestion);
        suggestionsCollection.updateOne(
                filter,
                new Document("$inc", new Document("count", 1)),
                new UpdateOptions().upsert(true));
    }

    public static String retrieveSuggestions(String query) {
        query = query.trim().toLowerCase();
        Bson filter = Filters.regex("_id", "^" + query);
        FindIterable<Document> documents = suggestionsCollection.find(filter).limit(10).sort(Sorts.descending("count"));
        StringBuilder builder = new StringBuilder("[");
        MongoCursor<Document> it = documents.iterator();
        while (it.hasNext()) {
            builder.append("\"").append(it.next().getString("_id")).append("\"");
            if (it.hasNext()) builder.append(",");
        }
        return builder.append("]").toString();
    }

    public static String requestUserID() {
        Document doc = new Document();
        usersCollection.insertOne(doc);
        return "\"" + doc.get("_id").toString() + "\"";
    }

    public static void click(String user, String link) {
        link = link.trim().toLowerCase();
        Bson filter = new Document("_id", new ObjectId(user)).append("urls.url", link);
        if (usersCollection.find(filter).first() == null) {
            usersCollection.updateOne(Filters.eq("_id", new ObjectId(user)),
                    new Document("$push",
                            new Document("urls",
                                    new Document("url", link).append("count", 1)
                            )), new UpdateOptions().upsert(true));
        } else {
            usersCollection.updateOne(filter,
                    new Document("$inc", new Document("urls.$.count", 1)));
        }
    }

    public static void clearDB() {
        //***************** drop all collections**********************
        dropCrawlerCollections();
        invertedIndexCollection.drop();
        imagesIndexCollection.drop();
        suggestionsCollection.drop();
        usersCollection.drop();
    }

    public static void closeConnection() {
        mongo.close();
    }

    public static void main(String[] args) {
    }

}