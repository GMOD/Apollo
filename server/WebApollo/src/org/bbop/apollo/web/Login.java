package org.bbop.apollo.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.bbop.apollo.web.config.ServerConfiguration;
import org.bbop.apollo.web.sockets.AnnotationEditorSessionManager;
import org.bbop.apollo.web.user.UserAuthenticationException;
import org.bbop.apollo.web.user.UserAuthentication;
import org.bbop.apollo.web.user.UserManager;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Servlet implementation class LoginDynamic
 */
@WebServlet("/Login")
public class Login extends HttpServlet implements HttpSessionListener {
	private static final long serialVersionUID = 1L;

	private UserAuthentication userAuthentication;
	
    /**
     * @see HttpServlet#HttpServlet()
     */
    public Login() {
        super();
    }
    
    @Override
    public void init(ServletConfig config) throws ServletException {
    	super.init(config);
		try {
			ServerConfiguration serverConfig = new ServerConfiguration(getServletContext().getResourceAsStream("/config/config.xml"));
	    	userAuthentication = (UserAuthentication)Class.forName(serverConfig.getUserAuthenticationClass()).newInstance();
			if (!UserManager.getInstance().isInitialized()) {
				ServerConfiguration.UserDatabaseConfiguration userDatabase = serverConfig.getUserDatabase();
				UserManager.getInstance().initialize(userDatabase.getDriver(), userDatabase.getURL(), userDatabase.getUserName(), userDatabase.getPassword());
			}
		}
		catch (Exception e) {
			throw new ServletException(e);
		}
    }

    @Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
//		userAuthentication.generateUserLoginPage(this, request, response);
//    	response.sendRedirect(userAuthentication.getUserLoginPageURL());
    	/*
    	String url = String.format("http://%s:%d%s/%s", request.getServerName(), request.getServerPort(), getServletContext().getContextPath(), userAuthentication.getUserLoginPageURL());
    	HttpURLConnection connection = (HttpURLConnection)new URL(url).openConnection();
		boolean ok = connection.getResponseCode() == HttpURLConnection.HTTP_OK;
		if (ok) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String line;
			while ((line = reader.readLine()) != null) {
				response.getWriter().println(line);
			}
		}
		*/
    	InputStream in = getServletContext().getResourceAsStream(userAuthentication.getUserLoginPageURL());
    	BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		String line;
		while ((line = reader.readLine()) != null) {
			response.getWriter().println(line);
		}
		in.close();
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			String operation = request.getParameter("operation");
			if (operation.equals("login")) {
				String username = userAuthentication.validateUser(request, response);
				if (!UserManager.getInstance().validateUser(username)) {
					throw new UserAuthenticationException("'" + username + "' does not have access");
				}
				HttpSession session = request.getSession();
				session.setAttribute("username", username);
				session.setAttribute("permissions", new HashMap<String, Integer>());
				Map<String, Integer> permissions = UserManager.getInstance().getPermissionsForUser(username);
				int permission = 0;
				if (permissions.values().size() > 0) {
					permission = permissions.values().iterator().next();
				}
				//String url = (permission & Permission.USER_MANAGER) != 0 ? "mainOptions.jsp" : "selectTrack.jsp";
				String url = "selectTrack.jsp";
				JSONObject responseJSON = new JSONObject();
				responseJSON.put("url", url).put("sessionId", session.getId());
				response.getWriter().write(responseJSON.toString());
			}
			else if (operation.equals("logout")) {
				if (request.getSession(false) != null) {
					request.getSession(false).invalidate();
				}
			}
		}
		catch (UserAuthenticationException e) {
			sendError(response, e);
		}
		catch (SQLException e) {
			sendError(response, e);
		}
		catch (JSONException e) {
			sendError(response, e);
		}
	}

	private void sendError(HttpServletResponse response, Exception e) throws IOException {
		try {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, new JSONObject().put("error", e.getMessage()).toString());
		}
		catch (JSONException e2) {
		}
	}

	@Override
	public void sessionCreated(HttpSessionEvent event) {
	}

	@Override
	public void sessionDestroyed(HttpSessionEvent event) {
		AnnotationEditorSessionManager.getInstance().sessionDestroyed(event);
	}

}
