package main.ranker;

import com.mongodb.client.AggregateIterable;
import org.bson.Document;

import java.util.*;

import main.model.*;
import main.utilities.ConnectToDB;

public class Ranker {

    public static Double calcScore(Document doc)
    {
        String url = doc.getOrDefault("url", null).toString();
        String score = doc.getOrDefault("score", null).toString();
        String popularity = doc.getOrDefault("popularity", null).toString();

        return (Double.parseDouble(score) * Integer.parseInt(popularity));
    }

    // TODO: allow for extra metrics in the scoring (TF-IDF, country, etc.)
    public static List<TextSearchResult> rankText(List<String> searchWords, int pageNum, int resultsPerPage)
    {
        HashMap<String, Double> urlScore = new HashMap<>();
        HashMap<String, Document> results = new HashMap<>();

        for(String word : searchWords)
        {
            AggregateIterable<Document> result = ConnectToDB.findTextMatches(word);
            for (Document doc : result)
            {
                results.put(doc.get("url").toString(), doc);

                String url = doc.getOrDefault("url", null).toString();
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
            String title = "Title";
            String description = "Description";
            Double score = urlScore.get(url);
            TextSearchResult tmp = new TextSearchResult(id, url, icon, title, description, score);
            orderedResults.add(tmp);
        }

        orderedResults.sort(Comparator.comparing(TextSearchResult::getScore).reversed());

        int startIndex = (pageNum - 1) * resultsPerPage;
        int endIndex = pageNum * resultsPerPage;

        return orderedResults.subList(startIndex, Math.min(endIndex, orderedResults.size()));
    }

    public static List<ImageSearchResult> rankImages(List<String> searchWords, int pageNum, int resultsPerPage)
    {
        HashMap<String, Double> imgScore = new HashMap<>();
        HashMap<String, Document> results = new HashMap<>();

        for(String word : searchWords)
        {
            AggregateIterable<Document> result = ConnectToDB.findImageMatches(word);
            for (Document doc : result)
            {
                results.put(doc.get("url").toString(), doc);
                String url = doc.getOrDefault("url", null).toString();
                String score = doc.getOrDefault("score", null).toString();
                String popularity = doc.getOrDefault("popularity", null).toString();

                Double finalScore = Double.parseDouble(score) * Integer.parseInt(popularity);
                if (imgScore.get(url) == null)
                {
                    imgScore.put(url, finalScore);
                }
                else
                {
                    Double oldScore = imgScore.get(url);
                    imgScore.put(url, (oldScore + finalScore));
                }
            }
        }

        List<ImageSearchResult> orderedResults = new ArrayList<>();
        for (Document doc : results.values())
        {
            String strID = doc.get("id").toString();
            Integer id = Integer.parseInt(strID.substring(strID.length() - 4), 16);
            String url = doc.get("url").toString();
            String title = "Title";
            Double score = imgScore.get(url);
            ImageSearchResult tmp = new ImageSearchResult(id, url, title, score);
            orderedResults.add(tmp);
        }

        orderedResults.sort(Comparator.comparing(SearchResult::getScore).reversed());

        int startIndex = (pageNum - 1) * resultsPerPage;
        int endIndex = pageNum * resultsPerPage;

        return orderedResults.subList(startIndex, Math.min(endIndex, orderedResults.size()));
    }

    public static void main(String[] args)
    {
        ConnectToDB.establishConnection();
        List<String> tests = Arrays.asList("comput", "scienc");

        List<TextSearchResult> results = rankText(tests, 1, 10);

        for(TextSearchResult res : results)
        {
            String url = res.getUrl();
            Integer id = res.getID();
            String icon = res.getIconUrl();
            String title = res.getTitle();
            String description = res.getDescription();
            System.out.println(id + " " + url + " " + icon + " " + title + " " + description);
        }
    }
}
