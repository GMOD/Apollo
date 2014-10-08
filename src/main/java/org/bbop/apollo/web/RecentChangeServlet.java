package org.bbop.apollo.web;

import org.bbop.apollo.web.config.ServerConfiguration;
import org.bbop.apollo.web.datastore.JEDatabase;
import org.bbop.apollo.web.datastore.history.JEHistoryDatabase;
import org.bbop.apollo.web.datastore.history.Transaction;
import org.bbop.apollo.web.user.Permission;
import org.bbop.apollo.web.user.UserManager;
import org.gmod.gbol.bioObject.AbstractSingleLocationBioFeature;
import org.gmod.gbol.bioObject.conf.BioObjectConfiguration;
import org.gmod.gbol.bioObject.util.BioObjectUtil;
import org.gmod.gbol.simpleObject.Feature;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by NathanDunn on 10/7/14.
 */
@WebServlet(name="/recentChanges", urlPatterns = {"/recentChanges"}, asyncSupported=true)
public class RecentChangeServlet extends HttpServlet{

    private ServerConfiguration serverConfig;
    private String databaseDir ;
    private Set<String> allStatusList = new TreeSet<String>();
    private BioObjectConfiguration bioObjectConfiguration ;


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
    private String generateFeatureRecordJSON(AbstractSingleLocationBioFeature feature,ServerConfiguration.TrackConfiguration track, JEHistoryDatabase historyDataStore) {
        String builder="";
        long flank=Math.round((feature.getFmax()-feature.getFmin())*0.5);
        long left=Math.max(feature.getFmin()-flank,1);
        long right=Math.min(feature.getFmax()+flank,track.getSourceFeature().getSequenceLength()-1);


        Transaction t=historyDataStore.getCurrentTransactionForFeature(feature.getUniqueName());


        builder+=String.format("['<input type=\"checkbox\" class=\"track_select\" id=\"%s\"/>',", track.getName()+"<=>"+feature.getUniqueName());
        builder+=String.format("'%s',",track.getSourceFeature().getUniqueName());
        if(feature.getName()==null || feature.getName().trim().length()==0){
            builder+=String.format("'---- <a target=\"_blank\" href=\"jbrowse/?loc=%s:%d..%d\">%s</a> ----',",
                    track.getSourceFeature().getUniqueName(), left, right, "unassigned");
        }
        else{
            builder+=String.format("'<a target=\"_blank\" href=\"jbrowse/?loc=%s:%d..%d\">%s</a>',",
                    track.getSourceFeature().getUniqueName(), left, right, feature.getName());
        }
        builder+=String.format("'%s',", feature.getType().split(":")[1]);
        builder+=String.format("'%s',", feature.getTimeLastModified());
        builder+=String.format("'%s',", t!=null?t.getEditor():feature.getOwner().getOwner());
        builder+=String.format("'%s',", feature.getOwner().getOwner());
        builder+=String.format("'%s']", feature.getStatus()==null ? "" : feature.getStatus().getStatus());
        return builder;
    }

    /** Generate a list of records for a feature that may include subfeatures
     *
     * @return non-empty ArrayList of Strings with records in JSON format
     */
    private List<String> generateFeatureRecord(AbstractSingleLocationBioFeature feature, ServerConfiguration.TrackConfiguration track, JEHistoryDatabase historyDataStore) {
        List<String> builder=new ArrayList<>();
//        String type=feature.getType().split(":")[1];

        for (AbstractSingleLocationBioFeature subfeature : feature.getChildren()) {
            builder.add(generateFeatureRecordJSON(subfeature,track, historyDataStore));
        }
        builder.add(generateFeatureRecordJSON(feature,track, historyDataStore));
        return builder;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String username = (String) request.getSession(true).getAttribute("username");
        Map<String, Integer> permissions ;
        try {
            permissions = UserManager.getInstance().getPermissionsForUser(username);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        BufferedReader in = new java.io.BufferedReader(new InputStreamReader(request.getServletContext().getResourceAsStream(serverConfig.getTrackNameComparator())));
        String line;
        String lineString = "";
        while ((line = in.readLine()) != null) {
            lineString += line + "\n";
//            out.println(line);
        }

        int maximum = 100 ;
        int count  =0 ;
        Collection<ServerConfiguration.TrackConfiguration> tracks = serverConfig.getTracks().values();

        System.out.println("# of tracks");
        boolean isAdmin = false;
        List<String> changeList =new ArrayList<>() ;
        List<ServerConfiguration.TrackConfiguration> trackList =new ArrayList<>() ;
        if (username != null) {
            for (ServerConfiguration.TrackConfiguration track : tracks) {
                trackList.add(track);
                Integer permission = permissions.get(track.getName());
                System.out.println("count ["+count+"] / maximum ["+maximum +"]");
                if (permission == null || count > maximum) {
                    permission = 0;
                }
                if ((permission & Permission.USER_MANAGER) == Permission.USER_MANAGER) {
                    isAdmin = true;
                }
                if ((permission & Permission.READ) == Permission.READ) {
                    Collection<Feature> features = new ArrayList<Feature>();
                    Collection<Feature> sequence_alterations = new ArrayList<Feature>();
                    String my_database = databaseDir + "/"+ track.getName();

                    //check that database exists
                    File database = new File(my_database);
                    if (!database.exists()) {
                        continue;
                    }
                    System.out.println("database exists: "+my_database );
                    File databaseHistory = new File(my_database+"_history");
                    System.out.println("database histry exists: "+databaseHistory.exists());
                    // load database
                    JEDatabase dataStore = new JEDatabase(my_database,false);


                    try {
                        JEHistoryDatabase historyDataStore = new JEHistoryDatabase(my_database+"_history",false,0);

                        dataStore.readFeatures(features);
                        Iterator<Feature> featureIterator = features.iterator();
                        while(featureIterator.hasNext() && count < maximum ) {
                            Feature feature = featureIterator.next();
                            // use list of records to get objects that have subfeatures
                            AbstractSingleLocationBioFeature gbolFeature=(AbstractSingleLocationBioFeature) BioObjectUtil.createBioObject(feature, bioObjectConfiguration);
                            List<String> record = generateFeatureRecord(gbolFeature, track, historyDataStore);
                            for (String s : record) {
//                                out.println("recent_changes.push(" + s + ");\n");
                                changeList.add(s);
                                ++count ;
                            }
                        }
                        dataStore.readSequenceAlterations(sequence_alterations);
                        featureIterator = sequence_alterations.iterator();
                        while (featureIterator.hasNext() && count < maximum) {
                            Feature feature = featureIterator.next();
                            // use list of records to get objects that have subfeatures
                            AbstractSingleLocationBioFeature gbolFeature=(AbstractSingleLocationBioFeature)BioObjectUtil.createBioObject(feature, bioObjectConfiguration);
                            List<String> record = generateFeatureRecord(gbolFeature, track, historyDataStore);
                            for (String s : record) {
                                changeList.add(s);
//                                out.println("recent_changes.push(" + s + ");\n");
                                ++count ;
                            }
                        }} catch (Exception e) {
                        System.err.println("Unable to read database history: "+my_database+"_history:\n"+e);
                    }
                }
            }
        }

        request.setAttribute("isAdmin",isAdmin);
        request.setAttribute("username",username);
        request.setAttribute("changes",changeList);
        request.setAttribute("tracks",trackList);
        request.setAttribute("allStatusList",allStatusList);

//        PrintWriter out = resp.getWriter();
//        out.write("whadup!");
//        out.close();
        RequestDispatcher view = request.getRequestDispatcher("/changes.jsp");
        view.forward(request, response);
    }

}
