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

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.bbop.apollo.web.user.UserAuthenticationException;
import org.bbop.apollo.web.user.UserAuthentication;
import org.bbop.apollo.web.util.JSONUtil;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.AuthorizationCodeTokenRequest;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.auth.openidconnect.IdToken;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;

public class OauthUserAuthentication implements UserAuthentication {
	
	// These values are set in the Oauth.xml configuration file 
	private String Provider_Name = null;
	private String Client_ID = null;
	private String Client_Secret = null;
	private String Auth_URL = null;
	private String Token_URL = null;
	private String Profile_URL = null;
	private String Uname_Field = null;
	
	public OauthUserAuthentication() {
		try {
			URL resource = getClass().getResource("/");
			String config_path = resource.getPath() + "/../../config/oauth.xml";
			
			FileInputStream fstream = new FileInputStream(config_path);
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(fstream);
			
			Provider_Name = doc.getElementsByTagName("provider_name").item(0).getFirstChild().getNodeValue();
			Client_ID     = doc.getElementsByTagName("client_id").item(0).getFirstChild().getNodeValue();
			Client_Secret = doc.getElementsByTagName("client_secret").item(0).getFirstChild().getNodeValue();
			Auth_URL      = doc.getElementsByTagName("auth_url").item(0).getFirstChild().getNodeValue();
			Token_URL     = doc.getElementsByTagName("token_url").item(0).getFirstChild().getNodeValue();
			Profile_URL   = doc.getElementsByTagName("profile_url").item(0).getFirstChild().getNodeValue();
			Uname_Field   = doc.getElementsByTagName("uname_field").item(0).getFirstChild().getNodeValue();
			
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
		
		// The WebApollo user name
		String username = null;
		try {
			// ---------------
			// Step1: first, WebApollo redirects the user to the provider 
			// authentication site to log in and approve access for WebApollo. If 
			// we are here, it is because the provider has responded with an 
			// authorization code.  The link that directs the user to the remote 
			// provider is constructed in the WebContent/user_interfaces/oauth/login.jsp file
			
			// get the authorization code
			String auth_code = request.getParameter("code");
			String error = request.getParameter("error");
			String state = request.getParameter("state");
			
			// we have an error and should not continue.  return a message for the
			// user.  If the provider provides an "error_description" then include
			// that in the error message
			if (error != null) {
				String error_description = request.getParameter("error_description");
				throw new UserAuthenticationException("OAuth error: " + error + ". " + error_description + ". ");
			}

			// ---------------
			// Step 2: now that we have an authorization code, we must use it to 
			// request the access token. There are several types of authentication
			// available by the Oauth2 protocal:  Authentication Code Flow, 
			// Implicit Flow, Resource Owner Password Flow, and Client Credentials.
			// We will use Authentication code Flow. Therefore, we begin by
			// creating an AuthroizationCodeFlow object
			JsonFactory json_factory = new JacksonFactory();
			AuthorizationCodeFlow.Builder acfb= new AuthorizationCodeFlow.Builder(
				BearerToken.authorizationHeaderAccessMethod(),
				new NetHttpTransport(),
				json_factory,
				new GenericUrl(Token_URL),
				new ClientParametersAuthentication(Client_ID, Client_Secret), 
				Client_ID, 
				Token_URL
			);
			AuthorizationCodeFlow codeFlow = acfb.setScopes(Arrays.asList(Uname_Field)).build();
			
			// Next we need to construct a token request object to request the access token
			String redirect_uri = "http://localhost:8080/WebApollo/Login?operation=login&forceRedirect=true";
			AuthorizationCodeTokenRequest actr = codeFlow.newTokenRequest(auth_code);
			actr.setRedirectUri(redirect_uri);
			actr.setScopes(Arrays.asList(Uname_Field));
			
			// The executeUnparsed() function of the token request object causes
			// WebApollo to request the access token from the oauth provider. 
			// The Google oauth2 library has an execute() function that will
			// automatically parse the return JSON array but not all providers
			// return values in ways that the Google parser likes, so we must
			// manually parse the response.
			HttpResponse token_unparsed = actr.executeUnparsed();
			if (token_unparsed.getContentEncoding() == "gzip") {
				
			}

			// Parse the response into a TokenResponse object.
			// TokenResponse token_response = token_unparsed.parseAs(TokenResponse.class);
			token_unparsed.getContent();
			InputStream token_is = token_unparsed.getContent();
			JSONObject token_json = JSONUtil.convertInputStreamToJSON(token_is);
			String access_token = token_json.getString("access_token");
			
			// store the details in a TokenResponse object
			TokenResponse token_response = new TokenResponse();
			token_response.setExpiresInSeconds(token_json.getLong("expires_in"));
			token_response.setScope(token_json.getString("scope"));
			token_response.setAccessToken(token_json.getString("access_token"));
			token_response.setTokenType(token_json.getString("token_type"));
			token_response.setRefreshToken(token_json.getString("refresh_token"));
			token_response.setFactory(json_factory);
			
			// ---------------
			// Step 3: now that we have the access token, we can request the profile
			// information which should include the username
			HttpClient http_client = new DefaultHttpClient();
			HttpPost http_post = new HttpPost(Profile_URL);
			
			List <NameValuePair> nvps = new ArrayList <NameValuePair>();
			nvps.add(new BasicNameValuePair("access_token", access_token));
			http_post.setEntity(new UrlEncodedFormEntity(nvps));
			org.apache.http.HttpResponse post_response = http_client.execute(http_post);
			String post_json = EntityUtils.toString(post_response.getEntity());
			JSONObject post_obj = new JSONObject(post_json);
			
			if (post_obj.has("error")) {
				String error_description = "";
				if (post_obj.has("error_description")) {
					error_description = post_obj.getString("error_description");
				}
				throw new UserAuthenticationException("OAuth error: " + error + ". " + error_description + ". ");
			}
			
			//Object obj = token_response.get("id_token");
			
			//TokenResponse token_response = actr.execute();
			IdToken id_token = IdToken.parse(json_factory, token_response.get("id_token").toString());
			username = id_token.getPayload().get(Uname_Field).toString();
			System.out.println("Hi");
		} 
		catch (java.lang.IllegalArgumentException e){
			String error = e.getMessage();
			throw new UserAuthenticationException("OAuth error: Illegal Argument " + error);
		}
		catch (TokenResponseException e) {
			String error = e.getContent();
			throw new UserAuthenticationException("OAuth error: " + error);
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		catch (JSONException e) {
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
