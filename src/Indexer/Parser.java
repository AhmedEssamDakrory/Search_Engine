import com.mongodb.*;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

public class Parser implements Runnable{
    public static MongoClient mongoClient = new MongoClient(new MongoClientURI(Constants.DATABASE_ADDRESS));
    public static MongoDatabase database = mongoClient.getDatabase(Constants.DATABASE_NAME);
    public static MongoCollection invertedIndexCollection = database.getCollection("invertedIndex");
//    public static MongoCollection forwardIndexCollection = database.getCollection("forwardIndex");

    public static Stemmer s = new Stemmer();

    public static String readHtml(String path) {
        String content = "";
        try
        {
            content = new String ( Files.readAllBytes( Paths.get(path) ) );
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return content;
    }

    public static void processURL(String path, String url){
        String html = readHtml(path);
        org.jsoup.nodes.Document document = Jsoup.parse(html);
        HashMap<String, Integer> wordScores = new HashMap<String, Integer>();

        for (String tagName: Constants.SCORES.keySet()) {
            Elements tagsText = document.getElementsByTag(tagName);
            Integer score = Constants.SCORES.get(tagName);
            for (Element tagText : tagsText) {
                processElement(tagText, score, wordScores);
            }
        }
        pushToDatabase(url, wordScores);
    }

    public static void processElement(Element paragraph, Integer score, HashMap<String, Integer> wordScore){
        String[] words = paragraph.text().toLowerCase().split("\\s");
        for (String word: words){
            s.add(word.toCharArray(), word.length());
            s.stem();
            String stemmedWord = s.toString();
            if (stemmedWord.isEmpty()) continue;
            if (stemmedWord.charAt(stemmedWord.length()-1) < 'a' || stemmedWord.charAt(stemmedWord.length()-1) > 'z'){
                stemmedWord = stemmedWord.substring(0, stemmedWord.length()-1);
            }
            if (stemmedWord.isEmpty()) continue;
            Integer prevScore = 0;
            prevScore = wordScore.getOrDefault(stemmedWord, 0);
            wordScore.put(stemmedWord, prevScore + score);
        }
    }

    public static void pushToDatabase(String url, HashMap<String, Integer> words){
        for (String word: words.keySet()){
            Integer score = words.get(word);
            invertedIndexCollection.updateOne(Filters.eq("_id", word),

                    new org.bson.Document("$addToSet", new org.bson.Document("urls",
                            new org.bson.Document("url", url).append("score", score))),

                    new UpdateOptions().upsert(true));
        }
    }

    public static void removeSiteFromDatabase(String url){
        invertedIndexCollection.updateMany(new org.bson.Document(),
                Updates.pull("urls", new org.bson.Document("url", url)));
        invertedIndexCollection.deleteMany(Filters.size("urls", 0));
    }

    public void run () {

    }
    public static void main(String[] args) {
        String path = "data/Codeforces.html";
        String url = "codeforces.com";
        processURL(path, url);
//        removeSiteFromDatabase(url);
        mongoClient.close();
    }

}
