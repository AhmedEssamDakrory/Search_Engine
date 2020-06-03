package main.ranker;

import com.mongodb.client.AggregateIterable;
import org.bson.Document;

import main.model.SearchResult;
import main.model.TextSearchResult;
import main.model.ImageSearchResult;

import main.utilities.ConnectToDB;

import static main.ranker.PageRank.*;

import java.util.*;

public class Ranker {
    private static Integer totDocs = null;
    private static Integer termDocs = null;

    public static void setTotDocs()
    {
        totDocs = ConnectToDB.countAllDocs();
    }

    public static void setTermDocs(String word)
    {
        termDocs = ConnectToDB.countTermDocs(word);
//        termDocs = 0;
    }

    // IDF = log(total no of docs / (1 + docs containing word))
    // TODO: allow for extra metrics in the scoring (country, personality, etc.)
    public static double calcScore(double score, String url, String word)
    {
//        String score = doc.get("score").toString();

        if (totDocs == null)
        {
            setTotDocs();
        }
        if (termDocs == null)
        {
            setTermDocs(word);
        }

        double docWeight = Math.log(1.0 * totDocs / (1 + termDocs));

        return (score * docWeight * getPageRank(url));
    }

    public static List<TextSearchResult> rankText(List<String> searchWords)
    {
        HashMap<String, Double> urlScore = new HashMap<>();
        HashMap<String, Document> results = new HashMap<>();

        setTotDocs();

        for(String word : searchWords)
        {
            setTermDocs(word);

            AggregateIterable<Document> queryResult = ConnectToDB.findTextMatches(word);
            for (Document doc : queryResult)
            {
                results.put(doc.get("url").toString(), doc);

                String url = doc.get("url").toString();
                double score = Double.parseDouble(doc.get("score").toString());

                Double finalScore = calcScore(score, url, word);

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

    public static List<ImageSearchResult> rankImages(List<String> searchWords)
    {
        HashMap<String, Double> imgScore = new HashMap<>();
        HashMap<String, Document> results = new HashMap<>();

        setTotDocs();

        for(String word : searchWords)
        {
            setTermDocs(word);

            AggregateIterable<Document> result = ConnectToDB.findImageMatches(word);
            for (Document doc : result)
            {
                results.put(doc.get("image").toString(), doc);

                String image = doc.get("image").toString();
                String url = doc.get("url").toString();
                double score = Double.parseDouble(doc.get("score").toString());

                Double finalScore = calcScore(score, url, word);

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

        orderedResults.sort(Comparator.comparing(SearchResult::getScore).reversed());
        return orderedResults;
    }

    public static <E> List<E> page(List<E> list, int pageNumber, int resultsPerPage) {
        int startIndex = (pageNumber - 1) * resultsPerPage;
        int endIndex = pageNumber * resultsPerPage;
        return list.subList(startIndex, Math.min(endIndex, list.size()));
    }

    public static void main(String[] args)
    {
        ConnectToDB.establishConnection();

        PageRank.fillAdjList();
        PageRank.normalizeAdjList();
        PageRank.updateRank(1000, 0.85);

        PageRank.print();

        List<String> tests = Arrays.asList("comput", "scienc");

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
            String image = res.getImageUrl();
            String url = res.getUrl();
            Integer id = res.getID();
            String title = res.getTitle();
            System.out.println(id + " " + url + " " + image + " " + title);
            System.out.println("Score: " + res.getScore());
        }
    }
}
