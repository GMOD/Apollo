package org.bbop.apollo.web;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bbop.apollo.web.config.ServerConfiguration;
import org.bbop.apollo.web.user.UserManager;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 */
@WebServlet(name = "/version", urlPatterns = {"/version"}, asyncSupported = true)
public class VersionServlet extends HttpServlet {

    final Logger logger = LogManager.getLogger(LogManager.ROOT_LOGGER_NAME);
    private ServerConfiguration serverConfig;
//    private String databaseDir;
//    private Set<String> allStatusList = new TreeSet<String>();
//    private BioObjectConfiguration bioObjectConfiguration;

    private final Integer DEFAULT_LIST_SIZE = 10;


    @Override
    public void init() throws ServletException {
        try {
            serverConfig = new ServerConfiguration(getServletContext());
        } catch (Exception e) {
            throw new ServletException(e);
        }
//        InputStream gbolMappingStream = getServletContext().getResourceAsStream(serverConfig.getGBOLMappingFile());

//        for (ServerConfiguration.AnnotationInfoEditorConfiguration annotationInfoEditorConfiguration : serverConfig.getAnnotationInfoEditor().values()) {
//            allStatusList.addAll(annotationInfoEditorConfiguration.getStatus());
//        }

//        bioObjectConfiguration = new BioObjectConfiguration(gbolMappingStream);
        if (!UserManager.getInstance().isInitialized()) {
            ServerConfiguration.UserDatabaseConfiguration userDatabase = serverConfig.getUserDatabase();
            try {
                UserManager.getInstance().initialize(userDatabase.getDriver(), userDatabase.getURL(), userDatabase.getUserName(), userDatabase.getPassword());
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
//        databaseDir = serverConfig.getDataStoreDirectory();
    }

    /**
     * Generate a record for a feature that includes the name, type, link to browser, and last modified date.
     */

    public synchronized String getVersion() {
        String version = null;

        // try to load from maven properties first
        try {
            Properties p = new Properties();

            InputStream is = getClass().getResourceAsStream("/META-INF/maven/org.bbop.apollo/apollo/pom.properties");
            if (is != null) {
                p.load(is);
                version = p.getProperty("version", "");
            } else {
                System.err.println("is is null");
            }
//            InputStream is = getClass().getResourceAsStream("/META-INF/maven/com.my.group/my-artefact/pom.properties");
//            if (is != null) {
//                p.load(is);
//                version = p.getProperty("version", "");
//            }
        } catch (Exception e) {
            System.err.println(e);
            // ignore
        }

        // fallback to using Java API
        if (version == null) {
            Package aPackage = getClass().getPackage();
            if (aPackage != null) {
                version = aPackage.getImplementationVersion();
                if (version == null) {
                    version = aPackage.getSpecificationVersion();
                }
            }
        }

        if (version == null) {
            // we could not compute the version so use a blank
            version = "";
        }

        return version;
    }


    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String version = getVersion();
        System.out.println(version);
        response.getWriter().write(getVersion());
        response.getWriter().close();
    }

//    @Override
//    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//        super.doGet(req, resp);
//    }
}
