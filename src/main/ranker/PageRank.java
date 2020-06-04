package main.ranker;

import com.mongodb.client.AggregateIterable;
import main.crawler.UrlNormalizer;
import main.utilities.ConnectToDB;
import main.utilities.Constants;
import org.bson.Document;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PageRank {
    protected static Integer dim;
    protected static double[][] adjList;
    protected static double[] pageRank;
    protected static HashMap<String, Integer> urlID = new HashMap<>();

    public static void setup()
    {
        adjList = new double[dim][dim];
        pageRank = new double[dim];

        AggregateIterable<org.bson.Document> allDocs = ConnectToDB.getAllIndexedData();
        for (org.bson.Document doc : allDocs)
        {
            String url = doc.get("url").toString();

            if (urlID.get(url) == null)
            {
                urlID.put(url, urlID.size());
            }
        }
    }

    private static int[] parseOutgoingLinks(String url)
    {
        int[] outgoing = new int[dim];

        AggregateIterable<Document> outLinks = ConnectToDB.getOutgoingLinks(url);

        for (Document doc : outLinks)
        {
            String to = doc.get("outlink").toString();

            Integer row = urlID.get(to);
            if (row != null && !url.equals(to))
            {
                outgoing[row] = 1;
            }
        }

        return outgoing;
    }

    public static void fillAdjList()
    {
        for (String url : urlID.keySet()) {
            int[] outgoing = parseOutgoingLinks(url);

            int col = urlID.get(url);

            for (int row = 0; row < dim; row++) {
                adjList[row][col] = outgoing[row];
            }
        }
    }

    public static void normalizeAdjList()
    {
        for (int i = 0; i < dim; i++)
        {
            double sum = 0;
            for (int j = 0; j < dim; j++)
            {
                sum += adjList[j][i];
            }
            System.out.println(sum);
            if (sum > 0.5)
            {
                for (int j = 0; j < dim; j++)
                {
                    adjList[j][i] = 1.0 * adjList[j][i] / sum;
                }
            }
        }
    }

    public static void updateRank(Integer maxIter, Double damp) {
        if (maxIter == null) {
            maxIter = 100;
        }
        if (damp == null) {
            damp = 0.85;
        }

        double[][] modifiedAdjList = new double[dim][dim];
        for (int i = 0; i < dim; ++i) {
            for (int j = 0; j < dim; ++j) {
                modifiedAdjList[i][j] = damp * adjList[i][j] + ((1 - damp) / dim);
            }
        }

        for (int i = 0; i < dim; ++i) {
            pageRank[i] = 1.0 / dim;
        }
        double[] tmp = new double[dim];
        for (int k = 0; k < maxIter; ++k) {
            for (int i = 0; i < dim; ++i) {
                tmp[i] = 0;
                for (int j = 0; j < dim; ++j) {
                    tmp[i] += modifiedAdjList[i][j] * pageRank[j];
                }
            }
            System.arraycopy(tmp, 0, pageRank, 0, dim);
        }
    }

    public static void run(int maxIter, double damp)
    {
        ConnectToDB.establishConnection();

        dim = ConnectToDB.countAllDocs();

        setup();

        if (ConnectToDB.isPageRankAvailable(dim))
        {
            AggregateIterable<Document> result = ConnectToDB.readPageRank();
            for (Document doc : result)
            {
                String url = doc.getString("url");
                String pr = doc.get("pageRank").toString();
                double rank = Double.parseDouble(pr);
                pageRank[urlID.get(url)] = rank;
            }
        }
        else
        {
            PageRank.fillAdjList();
            PageRank.normalizeAdjList();
            PageRank.updateRank(1000, 0.85);
        }

        ConnectToDB.savePageRank(urlID, pageRank);
    }

    public static double getPageRank(String url)
    {
        return pageRank[urlID.get(url)];
    }

    public static void print()
    {
        System.out.println("Dimension: " + dim);
        double sum = 0;
        for (int i = 0; i < dim; i++) {
            System.out.println(pageRank[i]);
            sum += pageRank[i];
        }
        System.out.println("Sum= " + sum);
    }

    public static void visualize()
    {
        System.out.println("Dimension: " + dim);
        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                System.out.print(adjList[i][j] + " ");
            }
            System.out.println();
        }
    }

    public static void main(String[] args) throws IOException
    {
        run(100, 0.85);
        print();
    }
}
