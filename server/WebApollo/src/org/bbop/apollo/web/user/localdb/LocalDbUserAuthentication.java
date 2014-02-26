package org.bbop.apollo.web.user.localdb;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bbop.apollo.web.user.UserAuthenticationException;
import org.bbop.apollo.web.user.UserAuthentication;
import org.bbop.apollo.web.user.UserManager;
import org.bbop.apollo.web.util.JSONUtil;
import org.json.JSONException;
import org.json.JSONObject;

public class LocalDbUserAuthentication implements UserAuthentication {

	@Override
	public void generateUserLoginPage(HttpServlet servlet, HttpServletRequest request,
			HttpServletResponse response) throws ServletException {
		InputStream in = servlet.getServletContext().getResourceAsStream("/user_interfaces/localdb/login.html");
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		String line;
		try {
			while ((line = reader.readLine()) != null) {
				response.getOutputStream().println(line);
			}
			in.close();
		} catch (IOException e) {
			throw new ServletException(e);
		}
	}

	@Override
	public String validateUser(HttpServletRequest request, HttpServletResponse response) throws UserAuthenticationException {
		try {
			JSONObject requestJSON = JSONUtil.convertInputStreamToJSON(request.getInputStream());
			String username = requestJSON.getString("username");
			String password = requestJSON.getString("password");
			if (!UserManager.getInstance().validateUser(username, password)) {
				throw new UserAuthenticationException("Invalid login");
			}
			return username;
		}
		catch (SQLException e) {
			throw new UserAuthenticationException(e);
		}
		catch (JSONException e) {
			throw new UserAuthenticationException(e);
		}
		catch (IOException e) {
			throw new UserAuthenticationException(e);
		}
	}
	

	@Override
	public String getUserLoginPageURL() {
		return "user_interfaces/localdb/login.html";
	}
	
	@Override
	public String getAddUserURL() {
		return "user_interfaces/localdb/addUser.jsp";
	}

}
