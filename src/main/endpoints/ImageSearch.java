package main.endpoints;

import com.google.gson.Gson;
import main.ranker.Ranker;
import main.utilities.ConnectToDB;
import main.utilities.QueryProcessor;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ImageSearch extends HttpServlet {


    public ImageSearch() {
        ConnectToDB.establishConnection();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String query = req.getParameter("query");
        int page_number = Integer.parseInt(req.getParameter("page_number"));
        int per_page = Integer.parseInt(req.getParameter("per_page"));
        QueryProcessor queryProcessor = QueryProcessor.getInstance();
        String message = new Gson().toJson(Ranker.rankImages(queryProcessor.process(query), page_number, per_page));
        resp.setContentType("application/json");
        resp.getWriter().println(message);
    }
}