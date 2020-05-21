import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

public class IndexingThread implements Runnable{
    int start, end;

    public IndexingThread(int s, int e){
        start = s;
        end = e;
    }

    public void run() {
        if (end == -1) end = Indexer.crawledURLs.size();
        for (int i = start; i<end; i++){
            processURL(Indexer.crawledURLs.get(i).path, Indexer.crawledURLs.get(i).url);
        }
    }

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
        String html = readHtml(Constants.CRAWLED_WEB_PAGES_FILE_PATH + path + ".html");
        org.jsoup.nodes.Document document = Jsoup.parse(html);
        HashMap<String, Integer> wordScores = new HashMap<String, Integer>();

        for (String tagName: Constants.SCORES.keySet()) {
            Elements tagsText = document.getElementsByTag(tagName);
            Integer score = Constants.SCORES.get(tagName);
            for (Element tagText : tagsText) {
                processElement(tagText, score, wordScores);
            }
        }
        ConnectToDB.pushToDatabase(url, wordScores);
    }

    public static void processElement(Element paragraph, Integer score, HashMap<String, Integer> wordScore){
        String[] words = paragraph.text().toLowerCase().split("\\s");
        for (String word: words){
            try {
                if (word.charAt(word.length() - 1) < 'a' || word.charAt(word.length() - 1) > 'z') {
                    word = word.substring(0, word.length() - 1);
                }
                s.add(word.toCharArray(), word.length());
                s.stem();
            } catch (ArrayIndexOutOfBoundsException | StringIndexOutOfBoundsException e){
                continue;
            }
            String stemmedWord = s.toString();
            if (stemmedWord.isEmpty() || Constants.STOP_WORDS.contains(stemmedWord)) continue;
            Integer prevScore = 0;
            prevScore = wordScore.getOrDefault(stemmedWord, 0);
            wordScore.put(stemmedWord, prevScore + score);
        }
    }
}
