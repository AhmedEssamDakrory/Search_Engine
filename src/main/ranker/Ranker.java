package main.ranker;

import com.mongodb.client.AggregateIterable;
import main.model.ImageSearchResult;
import main.model.TextSearchResult;
import main.utilities.ConnectToDB;
import org.bson.Document;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class Ranker {
    private Integer totDocs = null;
    private Integer termDocs = null;
    private HashMap<String, Double> userHist;

    public Ranker()
    {
        ConnectToDB.establishConnection();

        PageRank.run(100, 0.85);
    }

    public void setTotDocs()
    {
        totDocs = ConnectToDB.countAllDocs();
    }

    public void setTermDocs(String word)
    {
        termDocs = ConnectToDB.countTermDocs(word);
    }

    public void getUserHistory(String user)
    {
        userHist = new HashMap<>();

        if (user == null)
        {
            return;
        }

        AggregateIterable<Document> result = ConnectToDB.getUserData(user);
        int sum = 0;
        for (Document doc : result)
        {
            String url = doc.get("url").toString();
            String count = doc.get("count").toString();

            double value = Double.parseDouble(count);
            sum += value;
            userHist.put(url, value);
        }
        for (String url : userHist.keySet())
        {
            double value = userHist.get(url)/sum;
            userHist.put(url, value);
        }
    }

    // IDF = log(total no of docs / (1 + docs containing word))
    // allow for extra metrics in the scoring (country, personality, etc.)
    // userBonus = 1 + (prevVisits(url) / totPrevVisits)
    public double calcScore(double score, String url, String word, String country) {

        if (totDocs == null)
        {
            setTotDocs();
        }
        if (termDocs == null)
        {
            setTermDocs(word);
        }

        double docWeight = Math.log(1.0 * totDocs / (1 + termDocs));
        double countryBonus = 1;
        double historyBonus = 1;

        URL u = null;
        try {
            u = new URL(url);
        }
        catch (MalformedURLException e) {
            System.out.println("Malformed URL: " + e.getMessage());
        }
        if (u != null) {
            if (!(country == null) && country.length() > 1 && u.getHost().endsWith(country)) {
                countryBonus = 10;
            }
            if (userHist.get(u.getHost()) != null) {
                historyBonus = 1 + userHist.get(u.getHost());
            }
        }

        return (score * docWeight * PageRank.getPageRank(url) * countryBonus * historyBonus);
    }

    public List<TextSearchResult> rankText(List<String> searchWords, String country, String user)
    {
        HashMap<String, Double> urlScore = new HashMap<>();
        HashMap<String, Document> results = new HashMap<>();

        setTotDocs();

        getUserHistory(user);

        for(String word : searchWords)
        {
            setTermDocs(word);

            AggregateIterable<Document> queryResult = ConnectToDB.findTextMatches(word);
            for (Document doc : queryResult)
            {
                String url = doc.get("url").toString();

                results.put(url, doc);

                double score = Double.parseDouble(doc.get("score").toString());

                Double finalScore = calcScore(score, url, word, country);

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
            String url = doc.get("url").toString();
            TextSearchResult tmp = docToTextSearchResult(doc, urlScore.get(url));
            orderedResults.add(tmp);
        }

        orderedResults.sort(Comparator.comparing(TextSearchResult::getScore).reversed());
        return orderedResults;
    }

    public List<ImageSearchResult> rankImages(List<String> searchWords, String country, String user)
    {
        HashMap<String, Double> imgScore = new HashMap<>();
        HashMap<String, Document> results = new HashMap<>();

        setTotDocs();

        getUserHistory(user);

        for(String word : searchWords)
        {
            setTermDocs(word);

            AggregateIterable<Document> result = ConnectToDB.findImageMatches(word);
            for (Document doc : result)
            {
                String image = doc.get("image").toString();

                results.put(image, doc);

                String url = doc.get("url").toString();
                double score = Double.parseDouble(doc.get("score").toString());

                Double finalScore = calcScore(score, url, word, country);

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
            String image = doc.get("image").toString();
            ImageSearchResult tmp = docToImageSearchResult(doc, imgScore.get(image));
            orderedResults.add(tmp);
        }

        orderedResults.sort(Comparator.comparing(ImageSearchResult::getScore).reversed());
        return orderedResults;
    }

    public List<TextSearchResult> rankPhrase(List<String> searchWords, String country)
    {
        HashMap<String, Double> urlScore = new HashMap<>();
        HashMap<String, Document> results = new HashMap<>();
        HashMap<String, Integer> numFound = new HashMap<>();

        setTotDocs();

        int i = 0;
        for(String word : searchWords)
        {
            setTermDocs(word);

            AggregateIterable<Document> queryResult = ConnectToDB.findTextMatches(word);
            for (Document doc : queryResult)
            {
                String url = doc.get("url").toString();

                if (i != 0 && (numFound.get(url) == null || numFound.get(url) != i))
                {
                    continue;
                }
                else
                {
                    numFound.put(url, i + 1);
                }

                results.put(url, doc);

                double score = Double.parseDouble(doc.get("score").toString());

                Double finalScore = calcScore(score, url, word, country);

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
            ++i;
        }

        List<TextSearchResult> orderedResults = new ArrayList<>();
        for (Document doc : results.values())
        {
            String url = doc.get("url").toString();
            TextSearchResult tmp = docToTextSearchResult(doc, urlScore.get(url));
            orderedResults.add(tmp);
        }

        orderedResults.sort(Comparator.comparing(TextSearchResult::getScore).reversed());
        return orderedResults;
    }

    public static TextSearchResult docToTextSearchResult(Document doc, double score)
    {
        String strID = doc.get("id").toString();
        Integer id = Integer.parseInt(strID.substring(strID.length() - 4), 16);
        String url = doc.get("url").toString();
        String icon = doc.get("iconUrl").toString();
        String title = doc.get("title").toString();
        String description = "Description";

        return new TextSearchResult(id, url, icon, title, description, score);
    }

    public static ImageSearchResult docToImageSearchResult(Document doc, double score)
    {
        String strID = doc.get("id").toString();
        Integer id = Integer.parseInt(strID.substring(strID.length() - 4), 16);
        String pageUrl = doc.get("url").toString();
        String image = doc.get("image").toString();
        String title = doc.get("title").toString();

        return new ImageSearchResult(id, image, pageUrl, title, score);
    }

    public static <E> List<E> page(List<E> list, int pageNumber, int resultsPerPage) {
        int startIndex = (pageNumber - 1) * resultsPerPage;
        int endIndex = pageNumber * resultsPerPage;
        return list.subList(startIndex, Math.min(endIndex, list.size()));
    }

    public static void main(String[] args)
    {
        Ranker ranker = new Ranker();

        List<String> tests = Arrays.asList("gam", "work");

        List<TextSearchResult> text = page(ranker.rankText(tests, null, "5ed885d8a557cdd8ae96aebf"), 1, 10);
        List<TextSearchResult> phrases = page(ranker.rankPhrase(tests, "blog"), 1, 10);
        List<ImageSearchResult> images = page(ranker.rankImages(tests, "uk", "5ed885d8a557cdd8ae96aeff"), 1, 10);

        System.out.println("Text: " + text.size());
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

        System.out.println("\nPhrases: " + phrases.size());
        for(TextSearchResult res : phrases)
        {
            String url = res.getUrl();
            Integer id = res.getID();
            String icon = res.getIconUrl();
            String title = res.getTitle();
            String description = res.getDescription();
            System.out.println(id + " " + url + " " + icon + " " + title + " " + description);
            System.out.println("Score: " + res.getScore());
        }

        System.out.println("\nImages: " + images.size());
        for(ImageSearchResult res : images)
        {
            String image = res.getImageUrl();
            String url = res.getUrl();
            Integer id = res.getID();
            String title = res.getTitle();
            System.out.println(id + " " + url + " " + image + " " + title);
            System.out.println("Score: " + res.getScore());
        }

        ConnectToDB.click("5ed885d8a557cdd8ae96aebf", "abc");
        ConnectToDB.click("5ed885d8a557cdd8ae96aebf", "def");
        ConnectToDB.click("5ed885d8a557cdd8ae96aeff", "abc");
        ConnectToDB.click("5ed885d8a557cdd8ae96aeff", "xyz");
    }
}
