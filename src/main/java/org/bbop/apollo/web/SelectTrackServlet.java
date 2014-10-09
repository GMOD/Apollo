package org.bbop.apollo.web;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bbop.apollo.web.config.ServerConfiguration;
import org.bbop.apollo.web.data.DataAdapterGroupView;
import org.bbop.apollo.web.user.Permission;
import org.bbop.apollo.web.user.UserManager;
import org.gmod.gbol.bioObject.conf.BioObjectConfiguration;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by NathanDunn on 10/7/14.
 */
@WebServlet(name="/selectTrack", urlPatterns = {"/selectTrack"}, asyncSupported=true)
public class SelectTrackServlet extends HttpServlet{

    final Logger logger = LogManager.getLogger(LogManager.ROOT_LOGGER_NAME);
    private ServerConfiguration serverConfig;
    private String databaseDir ;
    private Set<String> allStatusList = new TreeSet<String>();
    private BioObjectConfiguration bioObjectConfiguration ;
    private Integer maxLength = 20 ;


    @Override
    public void init() throws ServletException {
        try {
            serverConfig = new ServerConfiguration(getServletContext());
        } catch (Exception e) {
            throw new ServletException(e);
        }
        InputStream gbolMappingStream = getServletContext().getResourceAsStream(serverConfig.getGBOLMappingFile());

        for (ServerConfiguration.AnnotationInfoEditorConfiguration annotationInfoEditorConfiguration : serverConfig.getAnnotationInfoEditor().values()) {
            allStatusList.addAll(annotationInfoEditorConfiguration.getStatus());
        }

        bioObjectConfiguration = new BioObjectConfiguration(gbolMappingStream);
        if (!UserManager.getInstance().isInitialized()) {
            ServerConfiguration.UserDatabaseConfiguration userDatabase = serverConfig.getUserDatabase();
            try {
                UserManager.getInstance().initialize(userDatabase.getDriver(), userDatabase.getURL(), userDatabase.getUserName(), userDatabase.getPassword());
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        databaseDir = serverConfig.getDataStoreDirectory();
    }

    /** Generate a record for a feature that includes the name, type, link to browser, and last modified date.
     *
     * @return String representation of the record in JSON format
     */


    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String username = (String) request.getSession(true).getAttribute("username");
        Map<String, Integer> permissions ;
        try {
            permissions = UserManager.getInstance().getPermissionsForUser(username);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(request.getServletContext().getResourceAsStream(serverConfig.getTrackNameComparator())));
        String line;
        String lineString = "";
        while ((line = in.readLine()) != null) {
            lineString += line + "\n";
//            out.println(line);
        }

        int maximum = 100 ;
        int count  =0 ;
        Collection<ServerConfiguration.TrackConfiguration> tracks = serverConfig.getTracks().values();

        System.out.println("# of tracks: "+ tracks.size());
        boolean isAdmin = false;
        List<List<String>> trackTableList =new ArrayList<>() ;
        List<ServerConfiguration.TrackConfiguration> trackList =new ArrayList<>() ;
        if (username != null) {
            for (ServerConfiguration.TrackConfiguration track : tracks) {
                trackList.add(track);
                Integer permission = permissions.get(track.getName());
//                System.out.println("count ["+count+"] / maximum ["+maximum +"]");
                if (permission == null || count > maximum) {
                    permission = 0;
                }
                if ((permission & Permission.USER_MANAGER) == Permission.USER_MANAGER) {
                    isAdmin = true;
                }
                if ((permission & Permission.READ) == Permission.READ) {
                    List<String> trackRow = new ArrayList<>();
                    trackRow.add(String.format("<input type=\"checkbox\" class=\"track_select\" id=\"%s\"/>",track.getName()));
                    trackRow.add(track.getOrganism());
                    trackRow.add(String.format("<a target=\"_blank\" href=\"jbrowse/?loc=%s\">%s</a>",track.getSourceFeature().getUniqueName(), track.getSourceFeature().getUniqueName()));
                    trackRow.add(String.format("%d",track.getSourceFeature().getSequenceLength()));
                    trackTableList.add(trackRow);
                }
            }
        }

        int permission = !permissions.isEmpty() ? permissions.values().iterator().next() : 0;
        List<DataAdapterGroupView> dataAdapterConfigurationList = new ArrayList<>();
        for (ServerConfiguration.DataAdapterGroupConfiguration groupConf : serverConfig.getDataAdapters().values()) {
                if ((permission & Permission.getValueForPermission(groupConf.getPermission())) >= 1) {
                    DataAdapterGroupView dataAdapterGroupView = new DataAdapterGroupView(groupConf);
                    dataAdapterConfigurationList.add(dataAdapterGroupView);
                }
//            }
        }


        request.setAttribute("isAdmin",isAdmin);
        request.setAttribute("username",username);
        request.setAttribute("dataAdapters",dataAdapterConfigurationList);
        request.setAttribute("tracks",trackList);
        request.setAttribute("trackViews",trackTableList);

        RequestDispatcher view = request.getRequestDispatcher("/tracks.jsp");
        view.forward(request, response);
    }

}
