import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Parser {

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

    public static void processElement(Element paragraph, Integer score){
        String[] words = paragraph.text().toLowerCase().split("\\s");
        for (String word: words){
            s.add(word.toCharArray(), word.length());
            s.stem();
            System.out.println(s.toString() + " " + score.toString());
        }
    }

    public static void main(String[] args) {
        String path = "data/Codeforces.html";
        String html = readHtml(path);
        Document document = Jsoup.parse(html);

        for (String tagName: Constants.SCORES.keySet()) {
            Elements tagsText = document.getElementsByTag(tagName);
            Integer score = Constants.SCORES.get(tagName);
            for (Element tagText : tagsText) {
                processElement(tagText, score);
            }
        }
    }
}
