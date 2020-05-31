package main.ranker;

import com.mongodb.client.AggregateIterable;
import main.model.ImageSearchResult;
import main.model.SearchResult;
import main.model.TextSearchResult;
import main.utilities.ConnectToDB;
import org.bson.Document;

import java.util.*;

public class Ranker {

    public static Double calcScore(Document doc)
    {
        String score = doc.get("score").toString();
        String popularity = doc.get("popularity").toString();

        return (Double.parseDouble(score));
//         * Integer.parseInt(popularity)
    }

    // TODO: calculate IDF = log(total no of docs / (1 + docs containing word)
    // TODO: allow for extra metrics in the scoring (country, personality, etc.)
    public static List<TextSearchResult> rankText(List<String> searchWords)
    {
        HashMap<String, Double> urlScore = new HashMap<>();
        HashMap<String, Document> results = new HashMap<>();

        for(String word : searchWords)
        {
            AggregateIterable<Document> result = ConnectToDB.findTextMatches(word);
            for (Document doc : result)
            {
                results.put(doc.get("url").toString(), doc);

                String url = doc.get("url").toString();
                Double finalScore = calcScore(doc);

                if (urlScore.get(url) == null)
                {
                    urlScore.put(url, finalScore);
                }
                else
                {
                    Double oldScore = urlScore.get(url);
                    urlScore.put(url, (oldScore + finalScore));
                }
            }
        }

        List<TextSearchResult> orderedResults = new ArrayList<>();
        for (Document doc : results.values())
        {
            String strID = doc.get("id").toString();
            Integer id = Integer.parseInt(strID.substring(strID.length() - 4), 16);
            String url = doc.get("url").toString();
            String icon = "Icon";
            String title = doc.get("title").toString();
            String description = "Description";
            Double score = urlScore.get(url);
            TextSearchResult tmp = new TextSearchResult(id, url, icon, title, description, score);
            orderedResults.add(tmp);
        }

        orderedResults.sort(Comparator.comparing(TextSearchResult::getScore).reversed());
        return orderedResults;
    }

    public static <E> List<E> page(List<E> list, int pageNumber, int resultsPerPage) {
        int startIndex = (pageNumber - 1) * resultsPerPage;
        int endIndex = pageNumber * resultsPerPage;
        return list.subList(startIndex, Math.min(endIndex, list.size()));
    }

    public static List<ImageSearchResult> rankImages(List<String> searchWords)
    {
        HashMap<String, Double> imgScore = new HashMap<>();
        HashMap<String, Document> results = new HashMap<>();

        for(String word : searchWords)
        {
            AggregateIterable<Document> result = ConnectToDB.findImageMatches(word);
            for (Document doc : result)
            {
                results.put(doc.get("image").toString(), doc);

                String image = doc.get("image").toString();
                Double finalScore = calcScore(doc);

                if (imgScore.get(image) == null)
                {
                    imgScore.put(image, finalScore);
                }
                else
                {
                    Double oldScore = imgScore.get(image);
                    imgScore.put(image, (oldScore + finalScore));
                }
            }
        }

        List<ImageSearchResult> orderedResults = new ArrayList<>();
        for (Document doc : results.values())
        {
            String strID = doc.get("id").toString();
            Integer id = Integer.parseInt(strID.substring(strID.length() - 4), 16);
            String pageUrl = doc.get("url").toString();
            String image = doc.get("image").toString();
            String title = doc.get("title").toString();
            Double score = imgScore.get(image);
            ImageSearchResult tmp = new ImageSearchResult(id, image, pageUrl, title, score);
            orderedResults.add(tmp);
        }

        orderedResults.sort(Comparator.comparing(ImageSearchResult::getScore).reversed());

        int startIndex = (pageNum - 1) * resultsPerPage;
        int endIndex = pageNum * resultsPerPage;

        return orderedResults.subList(startIndex, Math.min(endIndex, orderedResults.size()));
    }

    public static void main(String[] args)
    {
        ConnectToDB.establishConnection();
        List<String> tests = Arrays.asList("cairo");

        List<TextSearchResult> text = page(rankText(tests), 1, 10);
        List<ImageSearchResult> images = page(rankImages(tests), 1, 10);

        System.out.println("Text:");
        for(TextSearchResult res : text)
        {
            String url = res.getUrl();
            Integer id = res.getID();
            String icon = res.getIconUrl();
            String title = res.getTitle();
            String description = res.getDescription();
            System.out.println(id + " " + url + " " + icon + " " + title + " " + description);
            System.out.println("Score: " + res.getScore());
        }

        System.out.println("\nImages:");
        for(ImageSearchResult res : images)
        {
            String image = res.getUrl();
            String url = res.getPageUrl();
            Integer id = res.getID();
            String title = res.getTitle();
            System.out.println(id + " " + url + " " + image + " " + title);
            System.out.println("Score: " + res.getScore());
        }
    }
}
