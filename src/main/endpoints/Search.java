package main.endpoints;

import com.google.gson.Gson;
import main.ranker.Ranker;
import main.utilities.ConnectToDB;
import main.utilities.QueryProcessor;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;

public abstract class Search<Result> extends HttpServlet {

    private static final int CACHE_LIMIT = 20;
    private LinkedHashMap<String, List<Result>> cache;

    public Search() {
        ConnectToDB.establishConnection();
        cache = new LinkedHashMap<>();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String query = req.getParameter("query");
        int page_number = Integer.parseInt(req.getParameter("page_number"));
        int per_page = Integer.parseInt(req.getParameter("per_page"));
        String country = req.getParameter("country");
        String user = req.getParameter("user");
        List<Result> allResults = search(query, country, user);
        if (!allResults.isEmpty()) ConnectToDB.addSuggestion(query);
        String message = new Gson().toJson(Ranker.page(allResults, page_number, per_page));
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().println(message);
    }

    // TODO userID
    private List<Result> search(String query, String country, String user) {
        String key = query + country + user;
        if (cache.containsKey(key)) {
            return cache.get(key);
        }
        if (cache.size() >= CACHE_LIMIT)
            cache.remove(cache.entrySet().iterator().next().getKey());
        QueryProcessor queryProcessor = QueryProcessor.getInstance();
        List<Result> allResults = rank(queryProcessor.process(query), country, user);
        cache.put(key, allResults);
        return allResults;
    }

    public abstract List<Result> rank(List<String> words, String country, String user);
}
