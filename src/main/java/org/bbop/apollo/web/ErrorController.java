package org.bbop.apollo.web;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by ndunn on 12/9/14.
 */
@WebServlet("/notFoundJSON")
public class ErrorController extends HttpServlet{

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletExcept
        String errorString = resp.getOutputStream().toString();
        System.out.println("do post error: "+errorString);
        super.doPost(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletExcepti
        System.out.println("do get error");
        super.doGet(req, resp);
    }
}
