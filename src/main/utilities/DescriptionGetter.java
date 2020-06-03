package main.utilities;

import com.mongodb.client.AggregateIterable;
import org.bson.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

public class DescriptionGetter {

    public static List<String> getDescriptions(List<String> urls, List<String> words) {
        HashMap<String, String[]> plainText = new HashMap<>();
        AggregateIterable<Document> result = ConnectToDB.getURLsDescriptions(urls, words, plainText);
        HashMap<String, Vector<Integer>> site = new HashMap<>();
        HashMap<String, String> descriptions = new HashMap<>();
        for (Document doc : result) {
            String url = doc.getString("url");
            Integer index = doc.getInteger("index");
            if (!site.containsKey(url)) site.put(url, new Vector<Integer>());
            site.get(url).add(index);
        }
        for (String url : site.keySet()) {
            Vector<Integer> s = site.get(url);
            StringBuilder descript = new StringBuilder();
            int r = -1;
            for (int i = 0; i < s.size(); i++) {
                int idx = s.get(i);
                if (r >= idx) continue;
                int l = Math.max(idx - Constants.DESCRIPTION_RANGE / 2, 0);
                r = Math.min(l + Constants.DESCRIPTION_RANGE, plainText.get(url).length - 1);
                for (int j = l; j <= r; j++) {
                    descript.append(plainText.get(url)[j]).append(" ");
                }
                if (i != s.size() - 1) descript.append("...");
            }
            descriptions.put(url, descript.toString());
        }
        ArrayList<String> descriptionList = new ArrayList<>();
        for (String url: urls){
            descriptionList.add(descriptions.get(url));
        }
        return descriptionList;
    }

    public static void main(String[] args) {
        ConnectToDB.establishConnection();
        List<String> urls = new ArrayList<String>();
        List<String> words = new ArrayList<String>();
        urls.add("https://stackoverflow.com");
        urls.add("https://stackoverflow.com/teams/create");
        words.add("free");
        words.add("code");
        List<String> descriptions = getDescriptions(urls, words);
        for (String d : descriptions) {
            System.out.println(d);
        }
    }
}
