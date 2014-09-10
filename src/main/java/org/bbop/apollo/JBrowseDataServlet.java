package org.bbop.apollo;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

//@WebServlet(value="/jbrowse/", name="helloServlet")
//@WebServlet(value="/jbrowse/asdkfjasdlfj", name="JBrowseData")
@WebServlet(urlPatterns = "/jbrowse/data/*", name = "JBrowseData")
public class JBrowseDataServlet extends HttpServlet {
    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        System.out.println("3 JBrowseDataServlet - IN servlet request filter");

        super.service(req, res);
//        System.out.println("IN servlet request filter");
//        RequestDispatcher rd = getServletContext().getNamedDispatcher("default");
//
//        HttpServletRequest wrapped = new HttpServletRequestWrapper((HttpServletRequest) req) {
//            public String getServletPath() {
//                return "jbrowse/data";
//            }
//        };
//
//        rd.forward(wrapped, res);
//        System.out.println("OUT servlet request filter");

//        res.getWriter().println("JBrowse Data rerouting");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("CP doing get!" + req.getContextPath());
        System.out.println("PT doing get!" + req.getPathTranslated());
        System.out.println("PI doing get!" + req.getPathInfo());
        System.out.println("SP doing get!" + req.getServletPath());

//        CP doing get!/apollo
//        PT doing get!/Users/NathanDunn/git/apollo/src/main/webapp/tracks.conf
//        PI doing get!/tracks.conf
//        SP doing get!/jbrowse/data

        String pathTranslated = req.getPathTranslated();
        String finalPath = "";
        finalPath = pathTranslated.substring(0, pathTranslated.length() - req.getPathInfo().length());
        finalPath += req.getServletPath() + req.getPathInfo();

        // Get the absolute path of the image
        ServletContext sc = getServletContext();
//        String filename = sc.getRealPath(req.getPathInfo());
        String filename = finalPath ;
        System.out.println("filename: " + filename);

        // Get the MIME type of the image
        String mimeType = sc.getMimeType(filename);
        if (mimeType == null) {
            sc.log("Could not get MIME type of " + filename);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        // Set content type
        resp.setContentType(mimeType);

        // Set content size
        File file = new File(filename);
        resp.setContentLength((int) file.length());

        // Open the file and output streams
        FileInputStream in = new FileInputStream(file);
        OutputStream out = resp.getOutputStream();

        // Copy the contents of the file to the output stream
        byte[] buf = new byte[1024];
        int count = 0;
        while ((count = in.read(buf)) >= 0) {
            out.write(buf, 0, count);
        }
        in.close();
        out.close();

//        super.doGet(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.print("doing post!");
//        super.doGet(req, resp);
    }
}
