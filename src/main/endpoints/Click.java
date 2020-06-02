package main.endpoints;

import main.utilities.ConnectToDB;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Click extends HttpServlet {

    public Click() {
        ConnectToDB.establishConnection();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String user = req.getParameter("user");
        String link = req.getParameter("link");
        ConnectToDB.click(user, link);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().println();
    }
}