package main.ranker;

import com.mongodb.client.AggregateIterable;
import org.bson.Document;

import java.util.*;

import main.model.*;
import main.utilities.ConnectToDB;

public class Ranker {

    // TODO: allow for text OR image search
    // TODO: allow for extra metrics in the scoring (TF-IDF, country, etc.)
    public static List<TextSearchResult> rank(List<String> searchWords, int pageNum, int resultsPerPage)
    {
        HashMap<String, Integer> urlScore = new HashMap<>();
        Set<Document> results = new HashSet<>();

        for(String word : searchWords)
        {
            AggregateIterable<Document> result = ConnectToDB.findMatches(word);
            for (Document doc : result)
            {
                results.add(doc);
                String url = doc.getOrDefault("url", null).toString();
                String score = doc.getOrDefault("score", null).toString();
                String popularity = doc.getOrDefault("popularity", null).toString();

                Integer finalScore = Integer.parseInt(score) * Integer.parseInt(popularity);
                if (urlScore.get(url) == null)
                {
                    urlScore.put(url, finalScore);
                }
                else
                {
                    Integer oldScore = urlScore.get(url);
                    urlScore.put(url, (oldScore + finalScore));
                }
            }
        }

        List<TextSearchResult> orderedResults = new ArrayList<>();
        for (Document doc : results)
        {
            Integer id = Integer.parseInt(doc.get("id").toString().substring(0, 4), 16);
            String url = doc.get("url").toString();
            String icon = "Icon";
            String title = "Title";
            String description = "Description";
            Integer score = urlScore.get(url);
            TextSearchResult tmp = new TextSearchResult(id, url, icon, title, description, score);
            orderedResults.add(tmp);
        }

        orderedResults.sort(Comparator.comparing(TextSearchResult::getScore).reversed());

        int startIndex = (pageNum - 1) * resultsPerPage;
        int endIndex = pageNum * resultsPerPage;

        return orderedResults.subList(startIndex, Math.min(endIndex, orderedResults.size()));
    }

    public static void main(String[] args)
    {
        ConnectToDB.establishConnection();
        List<String> tests = Arrays.asList("comput", "scienc");

        List<TextSearchResult> results = rank(tests, 1, 10);

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
