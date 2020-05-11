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
        String[] words = paragraph.text().toLowerCase().split("\\s");;
        for (String word: words){
            s.add(word.toCharArray(), word.length());
            s.stem();
            System.out.println(s.toString());
        }
    }

    public static void main(String[] args) {
        String path = "data/Codeforces.html";
        String html = readHtml(path);
        Document document = Jsoup.parse(html);

        processElement(document.getElementsByTag("title").get(0), 100);
        System.out.println("Title: " + document.title());
        System.out.println("Headers:\n");
        Elements headers = document.getElementsByTag("h1");
        for (Element header : headers) {
            System.out.println(header.text());
            processElement(header, 5);
        }
        System.out.println("Paragraphs:\n");
        Elements paragraphs = document.getElementsByTag("p");
        for (Element paragraph : paragraphs) {
            System.out.println(paragraph.text());
            processElement(paragraph, 0);
        }
    }
}
