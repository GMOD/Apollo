package org.bbop.apollo.web.dataadapter;

import org.apache.log4j.Logger;
import org.bbop.apollo.web.config.ServerConfiguration;
import org.bbop.apollo.web.user.Permission;
import org.bbop.apollo.web.user.UserManager;
import org.bbop.apollo.web.util.JSONUtil;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.*;

/**
 * Servlet implementation class Servlet
 */
@WebServlet("/IOService")
public class IOService extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private Map<String, DataAdapterValue> dataAdapters;
    private Collection<String> allTracks;
    private final Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * @see HttpServlet#HttpServlet()
     */
    public IOService() {
        super();
        dataAdapters = new HashMap<String, DataAdapterValue>();
        allTracks = new ArrayList<String>();
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            ServerConfiguration serverConfig = new ServerConfiguration(getServletContext());
            if (!UserManager.getInstance().isInitialized()) {
                ServerConfiguration.UserDatabaseConfiguration userDatabase = serverConfig.getUserDatabase();
                UserManager.getInstance().initialize(userDatabase.getDriver(), userDatabase.getURL(), userDatabase.getUserName(), userDatabase.getPassword());
            }
            for (ServerConfiguration.DataAdapterGroupConfiguration confGroup : serverConfig.getDataAdapters().values()) {
                for (ServerConfiguration.DataAdapterConfiguration conf : confGroup.getDataAdapters()) {
                    DataAdapter dataAdapter = (DataAdapter)Class.forName(conf.getClassName()).newInstance();
                    dataAdapter.init(serverConfig, conf.getConfigFileName(), getServletContext().getRealPath(""));
                    dataAdapters.put(conf.getKey().toLowerCase(), new DataAdapterValue(dataAdapter, conf.getPermission()));
                }
            }
            for (ServerConfiguration.TrackConfiguration conf : serverConfig.getTracks().values()) {
                allTracks.add(conf.getName());
            }
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        try {
            if (session == null) {
                throw new ServletException("You must first login before publishing");
            }
            String username = (String)session.getAttribute("username");
            if (username == null) {
                throw new ServletException("You must first login before publishing");
            }
            String tracksParam = request.getParameter("tracks");
            if (tracksParam == null) {
                throw new ServletException("Missing required parameter: tracks");
            }
            String operation = request.getParameter("operation");
            if (operation == null) {
                throw new ServletException("Missing required parameter: operation");
            }
            String dataAdapterKey = request.getParameter("adapter");
            if (dataAdapterKey == null) {
                throw new ServletException("Missing required parameter: adapter");
            }
            DataAdapterValue dataAdapterValue = dataAdapters.get(dataAdapterKey.toLowerCase());
            if (dataAdapterValue == null) {
                throw new ServletException("Cannot access adapter for: " + dataAdapterKey);
            }
            List<String> tracks = new ArrayList<String>();
            String permission = dataAdapterValue.getPermission();
            Map<String, Integer> permissions = UserManager.getInstance().getPermissionsForUser(username);
            if (tracksParam.equals("all_tracks")) {
                for (String t : allTracks) {
                    if (isValidTrack(t, permissions, permission)) {
                        tracks.add(t);
                    }
                }
            }
            else {
                for (String track : tracksParam.split(" ")) {
                    if (!isValidTrack(track, permissions, permission)) {
                        throw new ServletException("You do not have permissions for " + operation + " on " + tracksParam);
                    }
                    tracks.add(track);
                }
            }
            DataAdapter dataAdapter = dataAdapterValue.getDataAdapter();
            Map<String, String[]> parameters = new HashMap<String, String[]>();
            parameters.putAll(request.getParameterMap());
            parameters.put("session_id", new String[]{ session.getId() });

            if(request.getParameterMap().containsKey("options")){
                for (String parameter : request.getParameter("options").split("&")) {
                    String []keyValue = parameter.split("=");
                    parameters.put(keyValue[0], keyValue[1].split("\\+"));
                }
            }

            if (operation.equals("read")) {
                dataAdapter.read(tracks, parameters, response);
            }
            else if (operation.equals("write")) {
                dataAdapter.write(tracks, parameters, response);
            }
            else {
                throw new ServletException("Invalid operation: " + operation);
            }
        }
        catch (ServletException e) {
//            response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(e.getMessage());
        }
        catch (Exception e) {
            StringWriter buf = new StringWriter();
            e.printStackTrace(new PrintWriter(buf));
            e.printStackTrace();
//            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Error processing " + request.getParameter("track") + buf.toString());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Error processing " + request.getParameter("track") + buf.toString());
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        try {
            if (session == null) {
                throw new ServletException("You must first login before publishing");
            }
            String username = (String)session.getAttribute("username");
            if (username == null) {
                throw new ServletException("You must first login before publishing");
            }
            JSONObject json = JSONUtil.convertInputStreamToJSON(request.getInputStream());
            JSONArray tracksParam = json.getJSONArray("tracks");
            if (tracksParam == null) {
                throw new ServletException("Missing required parameter: tracks");
            }
            String operation = json.getString("operation");
            if (operation == null) {
                throw new ServletException("Missing required parameter: operation");
            }
            String dataAdapterKey = json.getString("adapter");
            if (dataAdapterKey == null) {
                throw new ServletException("Missing required parameter: adapter");
            }
            DataAdapterValue dataAdapterValue = dataAdapters.get(dataAdapterKey.toLowerCase());
            if (dataAdapterValue == null) {
                throw new ServletException("Cannot access adapter for: " + dataAdapterKey);
            }
            List<String> tracks = new ArrayList<String>();
            String permission = dataAdapterValue.getPermission();
            Map<String, Integer> permissions = UserManager.getInstance().getPermissionsForUser(username);
            for (int i = 0; i < tracksParam.length(); ++i) {
                String track = tracksParam.getString(i);
                if (!isValidTrack(track, permissions, permission)) {
                    logger.error("You do not have permissions for " + operation + " on " + track);
//                    throw new ServletException("You do not have permissions for " + operation + " on " + tracksParam);
                }
                tracks.add(track);
            }
            DataAdapter dataAdapter = dataAdapterValue.getDataAdapter();
            Map<String, String[]> parameters = new HashMap<String, String[]>();
            for (String parameter : json.getString("options").split("&")) {
                String []keyValue = parameter.split("=");
                parameters.put(keyValue[0], keyValue[1].split("\\+"));
            }
            parameters.put("session_id", new String[]{ session.getId() });
            if (operation.equals("read")) {
                dataAdapter.read(tracks, parameters, response);
            }
            else if (operation.equals("write")) {
                dataAdapter.write(tracks, parameters, response);
            }
            else {
                throw new ServletException("Invalid operation: " + operation);
            }
        }
        catch (ServletException e) {
//            response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(e.getMessage());
        }
        catch (Exception e) {
            StringWriter buf = new StringWriter();
            e.printStackTrace(new PrintWriter(buf));
            e.printStackTrace();
//            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Error processing " + request.getParameter("track") + buf.toString());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Error processing " + request.getParameter("track") + buf.toString());
        }
    }
    
    private boolean isValidTrack(String track, Map<String, Integer> permissions, String permission) throws SQLException {
        Integer trackPermission = permissions.get(track);
        int permissionValue = Permission.getValueForPermission(permission);
        if(trackPermission==null) return false ;

        return (trackPermission & permissionValue) != 0;
    }
    
    private class DataAdapterValue {

        private DataAdapter dataAdapter;
        private String permission;
        
        public DataAdapterValue(DataAdapter dataAdapter, String permission) {
            super();
            this.dataAdapter = dataAdapter;
            this.permission = permission;
        }
        
        public DataAdapter getDataAdapter() {
            return dataAdapter;
        }
        
        public String getPermission() {
            return permission;
        }
        
    }
}
