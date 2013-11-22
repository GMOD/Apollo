package org.bbop.apollo.web;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.bbop.apollo.web.config.ServerConfiguration;
import org.bbop.apollo.web.user.Permission;
import org.bbop.apollo.web.user.UserManager;
import org.bbop.apollo.web.util.JSONUtil;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Servlet implementation class UserManagerService
 */
@WebServlet("/UserManagerService")
public class UserManagerService extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public UserManagerService() {
        super();
        // TODO Auto-generated constructor stub
    }

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		try {
			ServerConfiguration serverConfig = new ServerConfiguration(getServletContext().getResourceAsStream("/config/config.xml"));
			if (!UserManager.getInstance().isInitialized()) {
				ServerConfiguration.UserDatabaseConfiguration userDatabase = serverConfig.getUserDatabase();
				UserManager.getInstance().initialize(userDatabase.getDriver(), userDatabase.getURL(), userDatabase.getUserName(), userDatabase.getPassword());
			}
		}
		catch (Exception e) {
			throw new ServletException(e);
		}
	}
	
	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		HttpSession session = request.getSession(false);
		try {
			if (session == null) {
				throw new UserManagerServiceException("You must first login before editing");
			}
			JSONObject json = JSONUtil.convertInputStreamToJSON(request.getInputStream());
			String operation = json.getString("operation");
			if (operation.equals("set_permissions")) {
				setPermissions(json.getJSONObject("permissions"));
			}
			else if (operation.equals("add_user")) {
				addUser(json.getJSONObject("user"));
			}
			else if (operation.equals("delete_user")) {
				deleteUser(json.getJSONObject("user"));
			}
		}
		catch (UserManagerServiceException e) {
			try {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, new JSONObject().put("error", e.getMessage()).toString());
			}
			catch (JSONException e2) {
			}
		} catch (JSONException e) {
			try {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, new JSONObject().put("error", e.getMessage()).toString());
			}
			catch (JSONException e2) {
			}
		} catch (SQLException e) {
			try {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, new JSONObject().put("error", e.getMessage()).toString());
			}
			catch (JSONException e2) {
			}
		}
	}
	
	private void setPermissions(JSONObject permissions) throws JSONException, SQLException {
		Map<String, Integer> permissionsMap = new HashMap<String, Integer>();
		for (String username : JSONObject.getNames(permissions)) {
			int permission = permissions.getInt(username);
			permissionsMap.put(username, permission);
		}
		UserManager.getInstance().updateAllTrackPermissionForUsers(permissionsMap);
	}
	
	private void addUser(JSONObject user) throws JSONException, SQLException {
		String username = user.getString("username");
		if (user.has("password")) {
			String password = user.getString("password");
			UserManager.getInstance().addUser(username, password);
		}
		else {
			UserManager.getInstance().addUser(username);
		}
		JSONObject permissions = new JSONObject();
		permissions.put(username, Permission.NONE);
		setPermissions(permissions);
	}

	private void deleteUser(JSONObject user) throws JSONException, SQLException {
		String username = user.getString("username");
		UserManager.getInstance().deleteUser(username);
	}
	
	private class UserManagerServiceException extends Exception {

		private static final long serialVersionUID = 1L;

		/** Constructor.
		 * 
		 * @param message - String describing the error
		 */
		public UserManagerServiceException(String message) {
			super(message);
		}
		
	}

}
