package org.bbop.apollo.web.user.oauth;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.bbop.apollo.web.user.UserAuthenticationException;
import org.bbop.apollo.web.user.UserAuthentication;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.util.Arrays;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.auth.openidconnect.IdToken;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;

public class OauthUserAuthentication implements UserAuthentication {
	
	private String Provider_Name = null;
	private String Client_ID = null;
	private String Client_Secret = null;
	private String Auth_URL = null;
	private String Token_URL = null;
	private String Uname_Field = null;
	
	public OauthUserAuthentication() {
		try {
			URL resource = getClass().getResource("/");
			String config_path = resource.getPath() + "/../../config/oauth.xml";
			
			FileInputStream fstream = new FileInputStream(config_path);
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(fstream);
			
			Provider_Name  = doc.getElementsByTagName("provider_name").item(0).getFirstChild().getNodeValue();
			Client_ID = doc.getElementsByTagName("client_id").item(0).getFirstChild().getNodeValue();
			Client_Secret = doc.getElementsByTagName("client_secret").item(0).getFirstChild().getNodeValue();
			Auth_URL = doc.getElementsByTagName("auth_url").item(0).getFirstChild().getNodeValue();
			Token_URL = doc.getElementsByTagName("token_url").item(0).getFirstChild().getNodeValue();
			Uname_Field = doc.getElementsByTagName("uname_field").item(0).getFirstChild().getNodeValue();
			
		} 
		catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//Node hibernateConfigNode = doc.getElementsByTagName("hibernate_config").item(0);
		catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void generateUserLoginPage(HttpServlet servlet, HttpServletRequest request,
			HttpServletResponse response) throws ServletException {
		InputStream in = servlet.getServletContext().getResourceAsStream("/user_interfaces/opauth/login.html");
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
		
		String username = null; 
		
		String redirect_uri = "http://localhost:8080/WebApollo/Login?operation=login&forceRedirect=true";

		
		// get the authorization code
		String code = request.getParameter("code");
		
		HttpTransport http_transport = new NetHttpTransport();
		JsonFactory json_factory = new JacksonFactory();

	    AuthorizationCodeFlow codeFlow = new AuthorizationCodeFlow.Builder(
	        BearerToken.authorizationHeaderAccessMethod(),
	        http_transport,
	        json_factory,
	        new GenericUrl(Token_URL),
	        new ClientParametersAuthentication(Client_ID, Client_Secret), Client_ID, Token_URL).setScopes(Arrays.asList("email")).build();
	    try {
			TokenResponse token_response = codeFlow.newTokenRequest(code)
				.setRedirectUri(redirect_uri).setScopes(Arrays.asList(Uname_Field)).execute();
			IdToken id_token = IdToken.parse(json_factory, token_response.get("id_token").toString());
			username = id_token.getPayload().get(Uname_Field).toString();
		} 
	    catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    return username;

	}

	@Override
	public String getUserLoginPageURL() {
		return "user_interfaces/oauth/login.jsp";
	}

	@Override
	public String getAddUserURL() {
		return "user_interfaces/oauth/addUser.jsp";
	}

}
