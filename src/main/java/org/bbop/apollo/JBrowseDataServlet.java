package org.bbop.apollo;

import org.apache.commons.io.FileUtils;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

//@WebServlet(value="/jbrowse/", name="helloServlet")
//@WebServlet(value="/jbrowse/asdkfjasdlfj", name="JBrowseData")
@WebServlet(urlPatterns = "/jbrowse/data/*", name = "JBrowseData")
public class JBrowseDataServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathTranslated = req.getPathTranslated();
        String rootPath = pathTranslated.substring(0, pathTranslated.length() - req.getPathInfo().length());
        String configPath = rootPath + "/config/config.properties";


        File propertyFile = new File(configPath);
        String filename = null ;

        if(propertyFile.exists()){
            Properties properties = new Properties();
            FileInputStream fileInputStream = new FileInputStream(propertyFile);
            properties.load(fileInputStream);

            filename = properties.getProperty("jbrowse.data") + req.getPathInfo();
            File dataFile = new File(filename);
            if(!dataFile.exists() || !dataFile.canRead()){
                System.out.println("NOT found: "+filename);
                filename = null ;
            }
        }

        if(filename==null){
            filename = rootPath + req.getServletPath() + req.getPathInfo();
            File testFile = new File(filename);
            if(FileUtils.isSymlink(testFile)){
                filename = testFile.getAbsolutePath();
                System.out.println("symlink found so adjusting to absolute path: "+filename);
            }
            System.out.println("not found so using default jbrowse/data path in servlet context "+ filename);
        }


        // Get the absolute path of the file
        ServletContext sc = getServletContext();

        File file = new File(filename);
        if(!file.exists()){
            sc.log("Could not get MIME type of " + filename);
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;

        }

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

    }

}
