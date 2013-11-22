package org.bbop.apollo.web.user.browserid;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bbop.apollo.web.user.UserAuthenticationException;
import org.bbop.apollo.web.user.UserAuthentication;
import org.bbop.apollo.web.util.JSONUtil;
import org.json.JSONException;
import org.json.JSONObject;

public class BrowserIdUserAuthentication implements UserAuthentication {

	@Override
	public void generateUserLoginPage(HttpServlet servlet, HttpServletRequest request,
			HttpServletResponse response) throws ServletException {
		InputStream in = servlet.getServletContext().getResourceAsStream("/user_interfaces/browserid/login.html");
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
	public String validateUser(HttpServletRequest request,
			HttpServletResponse response) throws UserAuthenticationException {
		try {
			JSONObject requestJSON = JSONUtil.convertInputStreamToJSON(request.getInputStream());
			String assertion = requestJSON.getString("assertion");
			URL verifyURL = new URL("https://browserid.org/verify");
			HttpURLConnection connection = (HttpURLConnection)verifyURL.openConnection();
			connection.setRequestMethod("POST");
			connection.setDoInput(true);
			connection.setDoOutput(true);
			OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
			String audience = request.getServerName() + ":" + request.getServerPort();
			out.write(String.format("assertion=%s&audience=%s", assertion, audience));
			out.flush();
			out.close();
			boolean ok = connection.getResponseCode() == HttpURLConnection.HTTP_OK;
			if (ok) {
				JSONObject verificationJSON = JSONUtil.convertInputStreamToJSON(connection.getInputStream());
				if (verificationJSON.getString("status").equals("okay")) {
					return verificationJSON.getString("email");
				}
				else {
					throw new UserAuthenticationException("Error validating user");
				}
			}
			else {
				throw new UserAuthenticationException("Error connecting to verification server");
			}
		}
		catch (JSONException e) {
			throw new UserAuthenticationException(e);
		}
		catch (MalformedURLException e) {
			throw new UserAuthenticationException(e);
		}
		catch (IOException e) {
			throw new UserAuthenticationException(e);
		}
	}

	@Override
	public String getUserLoginPageURL() {
		return "user_interfaces/browserid/login.html";
	}

	@Override
	public String getAddUserURL() {
		return "user_interfaces/browserid/addUser.jsp";
	}

}
