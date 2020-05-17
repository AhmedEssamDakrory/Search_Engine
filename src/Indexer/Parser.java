import com.mongodb.*;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashMap;

public class Parser {
    public static MongoClient mongoClient = new MongoClient(new MongoClientURI(Constants.DATABASE_ADDRESS));
    public static MongoDatabase database = mongoClient.getDatabase(Constants.DATABASE_NAME);
    public static MongoCollection invertedIndexCollection = database.getCollection("invertedIndex");
    public static MongoCollection forwardIndexCollection = database.getCollection("forwardIndex");

    public static Stemmer s = new Stemmer();

    public static void processElement(Element paragraph, Integer score, HashMap<String, Integer> wordScore){
        String[] words = paragraph.text().toLowerCase().split("\\s");
        for (String word: words){
            s.add(word.toCharArray(), word.length());
            s.stem();
            String stemmedWord = s.toString();
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

    public static void main(String[] args) {
        String path = "data/Codeforces.html";
        String url = "codeforces.com";
        String html = IndexerUtilities.readHtml(path);
        Document document = Jsoup.parse(html);
        HashMap<String, Integer> wordScores = new HashMap<String, Integer>();

        for (String tagName: Constants.SCORES.keySet()) {
            Elements tagsText = document.getElementsByTag(tagName);
            Integer score = Constants.SCORES.get(tagName);
            for (Element tagText : tagsText) {
                processElement(tagText, score, wordScores);
            }
        }
        System.out.println(wordScores.size());
        pushToDatabase(url, wordScores);
//        FindIterable results = invertedIndexCollection.find(new BasicDBObject("_id", "hello"));
//        ArrayList<String> sites = Utilities.pullWebsites(results);
//        if (sites != null) {
//            for (String site : sites) {
//                System.out.println(site);
//            }
//        }
        mongoClient.close();
    }
}
