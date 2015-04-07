package org.bbop.apollo.web;

import org.bbop.apollo.web.config.ServerConfiguration;
import org.bbop.apollo.web.user.UserAuthentication;
import org.bbop.apollo.web.user.UserAuthenticationException;
import org.bbop.apollo.web.user.UserManager;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.lang.reflect.Constructor;

/**
 * Servlet implementation class LoginDynamic
 */
@WebServlet("/Login")
public class Login extends HttpServlet {
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
            ServerConfiguration serverConfig = new ServerConfiguration(getServletContext());
            Constructor ctor = Class.forName(serverConfig.getUserAuthenticationClass())
                .getDeclaredConstructor(ServerConfiguration.class);
            ctor.setAccessible(true);
            userAuthentication = (UserAuthentication)ctor.newInstance(serverConfig);
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
//        userAuthentication.generateUserLoginPage(this, request, response);
//        response.sendRedirect(userAuthentication.getUserLoginPageURL());
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
        if (request.getParameter("operation") != null && request.getParameter("operation").equals("login")) {
            boolean forceRedirect = request.getParameter("forceRedirect") != null ? Boolean.parseBoolean(request.getParameter("forceRedirect")) : false;
            login(request, response, forceRedirect);
        }
        else {
            RequestDispatcher view = request.getRequestDispatcher(userAuthentication.getUserLoginPageURL());
            view.forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String operation = request.getParameter("operation");
        if (operation.equals("login")) {
            login(request, response);
        }
        else if (operation.equals("logout")) {
            if (request.getSession(false) != null) {
                request.getSession(false).invalidate();
            }
        }
    }

    private void sendError(HttpServletResponse response, Exception e) throws IOException {
        try {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
//            response.sendError(HttpServletResponse.SC_BAD_REQUEST, new JSONObject().put("error", e.getMessage()).toString());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("error", e.getMessage()).toString());
        }
        catch (JSONException e2) {
        }
    }

    private void login(HttpServletRequest request, HttpServletResponse response) throws IOException {
        login(request, response, false);
    }

    private void login(HttpServletRequest request, HttpServletResponse response, boolean forceRedirect) throws IOException {
        try {
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
            String url = "sequences";
            if (forceRedirect) {
                response.sendRedirect(url);
            }
            else {
                JSONObject responseJSON = new JSONObject();
                responseJSON.put("url", url).put("sessionId", session.getId());
                response.getWriter().write(responseJSON.toString());
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

}
