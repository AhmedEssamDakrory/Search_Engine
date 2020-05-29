package main.indexer;

import com.mongodb.client.FindIterable;
import main.utilities.ConnectToDB;

import java.util.ArrayList;

public class Indexer{
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
        FindIterable<org.bson.Document> results = ConnectToDB.pullNotVisitedURLs();
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
        ConnectToDB.establishConnection();
        double startTime  = (double)System.nanoTime();
        runIndexer(8);
        double endTime  = (double)System.nanoTime();
        double totalTime = (endTime - startTime)* (1e-9);
        System.out.println("finished");
        System.out.println(totalTime);
        ConnectToDB.closeConnection();
    }

}
