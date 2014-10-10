package org.bbop.apollo.web;

import org.bbop.apollo.web.config.ServerConfiguration;
import org.bbop.apollo.web.datastore.JEDatabase;
import org.bbop.apollo.web.datastore.history.JEHistoryDatabase;
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
@WebServlet(name = "/recentChanges", urlPatterns = {"/recentChanges"}, asyncSupported = true)
public class RecentChangeServlet extends HttpServlet {

    private ServerConfiguration serverConfig;
    private String databaseDir;
    private Set<String> allStatusList = new TreeSet<String>();
    private BioObjectConfiguration bioObjectConfiguration;
    private Integer maxStringLength = 20;
    private final Integer DEFAULT_LIST_SIZE = 10;


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

    /**
     * Generate a record for a feature that includes the name, type, link to browser, and last modified date.
     *
     * @return String representation of the record in JSON format
     */
    private String generateFeatureRecordJSON(AbstractSingleLocationBioFeature feature, ServerConfiguration.TrackConfiguration track, JEHistoryDatabase historyDataStore) {
        String builder = "";
        long flank = Math.round((feature.getFmax() - feature.getFmin()) * 0.5);
        long left = Math.max(feature.getFmin() - flank, 1);
        long right = Math.min(feature.getFmax() + flank, track.getSourceFeature().getSequenceLength() - 1);


//        Transaction t = historyDataStore.getCurrentTransactionForFeature(feature.getUniqueName());


        builder += String.format("['<input type=\"checkbox\" class=\"track_select\" id=\"%s\"/>',", track.getName() + "<=>" + feature.getUniqueName());
        builder += String.format("'%s',", track.getSourceFeature().getUniqueName());
        if (feature.getName() == null || feature.getName().trim().length() == 0) {
            builder += String.format("'---- <a target=\"_blank\" href=\"jbrowse/?loc=%s:%d..%d\">%s</a> ----',",
                    track.getSourceFeature().getUniqueName(), left, right, "unassigned");
        } else {
            builder += String.format("'<a target=\"_blank\" href=\"jbrowse/?loc=%s:%d..%d\">%s</a>',",
                    track.getSourceFeature().getUniqueName(), left, right, feature.getName());
        }
        builder += String.format("'%s',", feature.getType().split(":")[1]);
        builder += String.format("'%s',", feature.getTimeLastModified());
//        String editorString = t != null ? t.getEditor() : feature.getOwner().getOwner();

//        if (editorString.length() > maxStringLength) {
//            editorString = editorString.substring(0, maxStringLength) + "...";
//        }
//        builder += String.format("'%s',", editorString);
        String ownerString = feature.getOwner().getOwner();
        if (ownerString.length() > maxStringLength) {
            ownerString = ownerString.substring(0, maxStringLength) + "...";
        }
        builder += String.format("'%s',", ownerString);
        builder += String.format("'%s']", feature.getStatus() == null ? "" : feature.getStatus().getStatus());
        return builder;
    }

    /**
     * Generate a list of records for a feature that may include subfeatures
     *
     * @return non-empty ArrayList of Strings with records in JSON format
     */
    private List<String> generateFeatureRecord(AbstractSingleLocationBioFeature feature, ServerConfiguration.TrackConfiguration track, JEHistoryDatabase historyDataStore, HttpServletRequest request) {
        List<String> builder = new ArrayList<>();
//        String type=feature.getType().split(":")[1];

        for (AbstractSingleLocationBioFeature subfeature : feature.getChildren()) {
            if (matchesFilter(request, track, subfeature)) {
                builder.add(generateFeatureRecordJSON(subfeature, track, historyDataStore));
            }
        }
        if(matchesFilter(request,track,feature)){
            builder.add(generateFeatureRecordJSON(feature, track, historyDataStore));
        }
        return builder;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String username = (String) request.getSession(true).getAttribute("username");
        Map<String, Integer> permissions;
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

        Object maximumString = request.getParameter("maximum");
//        Object typeString = request.getParameter("type");
//        Object group = request.getParameter("group");
////        Object editor = request.getParameter("editor");
//        Object owner = request.getParameter("owner");
//        Object status = request.getParameter("status");

        int maximum = DEFAULT_LIST_SIZE;
        if (maximumString != null) {
//            if(maximumString.equals("All")){
//                maximum = Integer.MAX_VALUE;
//            }
            maximum = Integer.parseInt(maximumString.toString());
        }
        int count = 0;
        Collection<ServerConfiguration.TrackConfiguration> tracks = serverConfig.getTracks().values();

        System.out.println("# of tracks: " + tracks.size());
        boolean isAdmin = false;
        List<String> changeList = new ArrayList<>();
        List<ServerConfiguration.TrackConfiguration> trackList = new ArrayList<>();
        if (username != null) {

            Iterator<ServerConfiguration.TrackConfiguration> iterator = tracks.iterator();
//            for (ServerConfiguration.TrackConfiguration track : tracks) {
            while (iterator.hasNext() && count < maximum) {

                ServerConfiguration.TrackConfiguration track = iterator.next();


                Integer permission = permissions.get(track.getName());
                Object trackString = request.getParameter("track");
                if (permission == null || count > maximum || (trackString!=null && trackString.toString().length()>0 && !track.getName().substring("Annotations-".length()).equals(trackString.toString()))) {
                    permission = 0;
                }
                if ((permission & Permission.USER_MANAGER) == Permission.USER_MANAGER) {
                    isAdmin = true;
                }

                if ((permission & Permission.READ) == Permission.READ) {
                    trackList.add(track);
                    Collection<Feature> features = new ArrayList<Feature>();
                    Collection<Feature> sequence_alterations = new ArrayList<Feature>();
                    String my_database = databaseDir + "/" + track.getName();

                    //check that database exists
                    File database = new File(my_database);
                    if (!database.exists()) {
                        continue;
                    }
//                    System.out.println("database exists: "+my_database );
//                    File databaseHistory = new File(my_database + "_history");
//                    System.out.println("database histry exists: "+databaseHistory.exists());
                    // load database


                    JEDatabase dataStore = new JEDatabase(my_database, false);
                    JEHistoryDatabase historyDataStore = null;
                    try {

                        dataStore.readFeatures(features);
                        Iterator<Feature> featureIterator = features.iterator();
                        while (featureIterator.hasNext() && count < maximum) {
                            Feature feature = featureIterator.next();
                            // use list of records to get objects that have subfeatures
                            AbstractSingleLocationBioFeature gbolFeature = (AbstractSingleLocationBioFeature) BioObjectUtil.createBioObject(feature, bioObjectConfiguration);

                            if (historyDataStore == null) {
                                historyDataStore = new JEHistoryDatabase(my_database + "_history", false, 0);
                            }
                            List<String> record = generateFeatureRecord(gbolFeature, track, historyDataStore, request);
                            for (String s : record) {
//                                out.println("recent_changes.push(" + s + ");\n");
                                changeList.add(s);
                                ++count;
                            }

                        }
                        dataStore.readSequenceAlterations(sequence_alterations);
                        featureIterator = sequence_alterations.iterator();
                        while (featureIterator.hasNext() && count < maximum) {
                            Feature feature = featureIterator.next();
                            // use list of records to get objects that have subfeatures
                            AbstractSingleLocationBioFeature gbolFeature = (AbstractSingleLocationBioFeature) BioObjectUtil.createBioObject(feature, bioObjectConfiguration);

                            if (historyDataStore == null) {
                                historyDataStore = new JEHistoryDatabase(my_database + "_history", false, 0);
                            }
                            List<String> record = generateFeatureRecord(gbolFeature, track, historyDataStore, request);
                            for (String s : record) {
                                changeList.add(s);
//                                out.println("recent_changes.push(" + s + ");\n");
                                ++count;
                            }

                        }
                    } catch (Exception e) {
                        System.err.println("Unable to read database history: " + my_database + "_history:\n" + e.fillInStackTrace());
                    } finally {
                        dataStore = null;
                        historyDataStore = null;
                    }
                }

            }
        }

        List<String> typeList = new ArrayList<>();
        typeList.add("gene");
        typeList.add("pseudogene");
        typeList.add("transcript");
        typeList.add("mRNA");
        typeList.add("miRNA");
        typeList.add("tRNA");
        typeList.add("snRNA");
        typeList.add("snoRNA");
        typeList.add("ncRNA");
        typeList.add("rRNA");
        typeList.add("repeat region");
        typeList.add("transposable element");


        request.setAttribute("isAdmin", isAdmin);
        request.setAttribute("username", username);
        request.setAttribute("changes", changeList);
        request.setAttribute("tracks", trackList);
        request.setAttribute("types", typeList);
        request.setAttribute("allStatusList", allStatusList);
        request.setAttribute("trackCount", tracks.size());

        Set<String> allTrackNames = new TreeSet<>();
        for (ServerConfiguration.TrackConfiguration aTrack : tracks) {
            Integer permission = permissions.get(aTrack.getName());
            if (permission == null) {
                permission = 0;
            }
            if ((permission & Permission.USER_MANAGER) == Permission.USER_MANAGER) {
                isAdmin = true;
            }

            if ((permission & Permission.READ) == Permission.READ || isAdmin) {
                allTrackNames.add(aTrack.getName());
            }
        }


        // filter attributes
        request.setAttribute("allTrackNames", allTrackNames);
        request.setAttribute("maximum", maximumString);
        request.setAttribute("type", request.getParameter("type"));
        request.setAttribute("track", request.getParameter("track"));
        request.setAttribute("group", request.getParameter("group"));
        request.setAttribute("owner", request.getParameter("owner"));
        request.setAttribute("status", request.getParameter("status"));

//        PrintWriter out = resp.getWriter();
//        out.write("whadup!");
//        out.close();
//        RequestDispatcher view = request.getRequestDispatcher("/changes.jsp");
        RequestDispatcher view = request.getRequestDispatcher("/changes2.jsp");
        view.forward(request, response);
    }

    private boolean matchesFilter(HttpServletRequest request, ServerConfiguration.TrackConfiguration track, AbstractSingleLocationBioFeature gbolFeature) {

        Object trackString = request.getParameter("track");
        Object typeString = request.getParameter("type");
        Object group = request.getParameter("group");
        Object owner = request.getParameter("owner");
        Object status = request.getParameter("status");

        return matchesFilter(trackString, typeString, group, owner, status, track, gbolFeature);
    }

    private boolean matchesFilter(Object trackString, Object typeString, Object group, Object owner, Object status, ServerConfiguration.TrackConfiguration track, AbstractSingleLocationBioFeature gbolFeature) {
        boolean matches = true;

        if (matches && trackString != null && trackString.toString().trim().length() > 0) {
            matches = track.getName().contains(trackString.toString());
        }
        if (matches && typeString != null && typeString.toString().trim().length() > 0) {
            matches = gbolFeature.getType().split(":")[1].toUpperCase().equals(typeString.toString().toUpperCase());
        }
        if (matches && group != null && group.toString().trim().length() > 0) {
            matches = gbolFeature.getName().toUpperCase().contains(group.toString().toUpperCase());
        }
        if (matches && owner != null && owner.toString().trim().length() > 0) {
            matches = gbolFeature.getOwner().getOwner().toUpperCase().contains(owner.toString().toUpperCase());
        }
        if (matches && status != null && status.toString().trim().length() > 0) {
            if (status.equals("None")) {
                matches = gbolFeature.getStatus() == null;
            } else {
                matches = gbolFeature.getStatus().getStatus().toUpperCase().contains(status.toString().toUpperCase());
            }
        }

        return matches;
    }

}
