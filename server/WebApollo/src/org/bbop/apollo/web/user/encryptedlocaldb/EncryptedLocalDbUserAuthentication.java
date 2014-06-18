package org.bbop.apollo.web.user.encryptedlocaldb;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.crackstation.PasswordHash;

import org.bbop.apollo.web.user.UserAuthenticationException;
import org.bbop.apollo.web.user.UserAuthentication;
import org.bbop.apollo.web.user.UserManager;
import org.bbop.apollo.web.util.JSONUtil;
import org.json.JSONException;
import org.json.JSONObject;

public class EncryptedLocalDbUserAuthentication implements UserAuthentication {

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
			if (!validateUser(username, password)) {
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
	
	private boolean validateUser(String username, String password) throws SQLException {
		Connection conn = UserManager.getInstance().getConnection();
		PreparedStatement stmt = conn.prepareStatement("SELECT username, password FROM users WHERE username=?");
		stmt.setString(1, username);
		ResultSet rs = stmt.executeQuery();
		boolean valid = false;
		if (rs.next()) {
			try {
				valid = PasswordHash.validatePassword(password, rs.getString(2));
			}
			catch (NoSuchAlgorithmException e) {
				valid = false;
			}
			catch (InvalidKeySpecException e) {
				valid = false;
			}
		}
		conn.close();
		return valid;
	}


}
