import com.mongodb.*;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

import java.util.ArrayList;

public class Indexer{
    public static MongoClient mongoClient = new MongoClient(new MongoClientURI(Constants.DATABASE_ADDRESS));
    public static MongoDatabase database = mongoClient.getDatabase(Constants.DATABASE_NAME);
    public static MongoCollection invertedIndexCollection = database.getCollection("invertedIndex");
    public static MongoCollection crawlerInfoCollection = database.getCollection("crawler_info");
    public static ArrayList<pathURL> crawledURLs = new ArrayList<pathURL>();
    public static ArrayList<Thread> indexingThreads = new ArrayList<Thread>();

    public static class pathURL{
        String path, url;

        public pathURL(String p, String u) {
            path = p;
            url = u;
        }
    }

    public static void runIndexer(int numThreads){
        FindIterable<org.bson.Document> results = crawlerInfoCollection.find(Filters.eq("visited", true));
        for (org.bson.Document doc: results){
            String path = doc.getOrDefault("_id", null).toString();
            String url = doc.getOrDefault("url", null).toString();
            crawledURLs.add(new pathURL(path, url));
        }
        int segment = crawledURLs.size() / numThreads;
        for (int i=0; i<numThreads-1; i++){
            Thread t = new Thread(new IndexingThread(i * segment, (i+1) * segment));
            t.start();
            indexingThreads.add(t);
        }
        Thread last = new Thread(new IndexingThread((numThreads-1) * segment, -1));
        last.start();
        indexingThreads.add(last);
        for (Thread t: indexingThreads){
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    public static void main(String[] args) {
        double startTime  = (double)System.nanoTime();
        runIndexer(4);
        double endTime  = (double)System.nanoTime();
        double totalTime = (endTime - startTime)* (1e-9);
        System.out.println("finished");
        System.out.println(totalTime);
        mongoClient.close();
    }

}
