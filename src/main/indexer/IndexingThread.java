package main.indexer;

import main.utilities.ConnectToDB;
import main.utilities.Constants;
import main.utilities.QueryProcessor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;


public class IndexingThread implements Runnable {
    int start, end;

    public IndexingThread(int s, int e) {
        start = s;
        end = e;
    }

    public static String readHtml(String path) {
        String content = "";
        try {
            content = new String(Files.readAllBytes(Paths.get(path)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content;
    }

    public static void processURL(String path, String url) {
        String html = readHtml(Constants.CRAWLED_WEB_PAGES_FILE_PATH + path + ".html");
        org.jsoup.nodes.Document document = Jsoup.parse(html);
        HashMap<String, Integer> wordScores = new HashMap<String, Integer>();

        String title = document.title();
        for (String tagName : Constants.SCORES.keySet()) {
            Elements tagsText = document.getElementsByTag(tagName);
            Integer score = Constants.SCORES.get(tagName);
            for (Element tagText : tagsText) {
                processElement(tagText, score, wordScores);
            }
        }
        Integer totalScore = 0;
        for (Integer score : wordScores.values()) {
            totalScore += score;
        }
        if (title.isEmpty()) title = url;
        ConnectToDB.pushToDatabase(url, title, wordScores, totalScore);

        //---------Process Images----------
        List<Map.Entry<String, Integer>> wordsSorted =
                new LinkedList<Map.Entry<String, Integer>>(wordScores.entrySet());

        // Sort the list
        Collections.sort(wordsSorted, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Map.Entry<String, Integer> o1,
                               Map.Entry<String, Integer> o2) {
                return -(o1.getValue()).compareTo(o2.getValue());
            }
        });

        Elements images = document.getElementsByTag("img");
        for (Element image : images) {
            HashMap<String, Integer> captionScore = new HashMap<String, Integer>();
            String src = image.attr("src");
            if (!(src.startsWith("http") || src.startsWith("//")))
                continue;
            String width = image.attr("width");
            String height = image.attr("height");
            try {
                if ((!width.isEmpty() && Integer.parseInt(width) <= 5) || (!height.isEmpty() && Integer.parseInt(height) <= 5))
                    continue;
            } catch (NumberFormatException ignore) {
            }
            Integer captionTotalScore = processImage(image, captionScore);
            int num = 0;
            for (Map.Entry<String, Integer> word : wordsSorted) {
                Integer prevScore = captionScore.getOrDefault(word, 0);
                captionScore.put(word.getKey(), prevScore + word.getValue());
                captionTotalScore += word.getValue();
                if (num++ == Constants.EXTRA_IMAGE_WORDS) break;
            }
            ConnectToDB.pushImageToDatabase(url, src, title, captionScore, captionTotalScore);
        }

    }

    public static void processElement(Element paragraph, Integer score, HashMap<String, Integer> wordScore) {
        QueryProcessor q = QueryProcessor.getInstance();
        List<String> words = q.process(paragraph.text());
        for (String word : words) {
            Integer prevScore = wordScore.getOrDefault(word, 0);
            wordScore.put(word, prevScore + score);
        }
    }

    public static void main(String[] args) {
        List<Map.Entry<String, Integer>> wordsSorted =
                new LinkedList<Map.Entry<String, Integer>>(Constants.SCORES.entrySet());

        // Sort the list
        Collections.sort(wordsSorted, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Map.Entry<String, Integer> o1,
                               Map.Entry<String, Integer> o2) {
                return -(o1.getValue()).compareTo(o2.getValue());
            }
        });

        for (Map.Entry<String, Integer> word : wordsSorted) {
            System.out.println(word.getKey() + " " + word.getValue());
        }
    }

    public static Integer processImage(Element image, HashMap<String, Integer> imageScore) {
        QueryProcessor q = QueryProcessor.getInstance();
        List<String> words = q.process(image.attr("alt"));
        for (String word : words) {
            Integer prevScore = imageScore.getOrDefault(word, 0);
            imageScore.put(word, prevScore + Constants.CAPTION_SCORE);
        }
        return words.size() * Constants.CAPTION_SCORE;
    }

    public void run() {
        if (end == -1) end = Indexer.crawledURLs.size();
        for (int i = start; i < end; i++) {
            processURL(Indexer.crawledURLs.get(i).path, Indexer.crawledURLs.get(i).url);
        }
    }
}
