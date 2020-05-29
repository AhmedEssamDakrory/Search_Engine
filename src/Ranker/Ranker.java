
import com.mongodb.client.AggregateIterable;
import org.bson.Document;
import java.util.*;

public class Ranker {


    public static List<String> rank(List<String> searchWords)
    {
        HashMap<String, Integer> urlScore = new HashMap<>();

        for(String word : searchWords)
        {
            AggregateIterable<Document> result = ConnectToDB.findMatches(word);
            for (Document doc : result)
            {
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
        List<String> orderedUrls = new ArrayList<>(urlScore.keySet());

        orderedUrls.sort(Comparator.comparing(urlScore::get).reversed());

        return orderedUrls;
    }

    public static void main(String[] args)
    {
        ConnectToDB.establishConnection();
        List<String> tests = Arrays.asList("comput", "scienc");

        List<String> urls = rank(tests);

        for(String s : urls)
        {
            System.out.println(s);
        }
    }
}
