package main.endpoints;

import com.google.gson.Gson;
import main.utilities.ConnectToDB;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Trends extends HttpServlet {

    public Trends() {
        ConnectToDB.establishConnection();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String country = req.getParameter("country");
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().println(new Gson().toJson(ConnectToDB.getTrends(country)));
    }
}