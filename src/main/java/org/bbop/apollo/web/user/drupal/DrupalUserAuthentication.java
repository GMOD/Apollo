package org.bbop.apollo.web.user.drupal;

import java.io.*;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.bbop.apollo.web.user.UserAuthenticationException;
import org.bbop.apollo.web.user.UserAuthentication;
import org.bbop.apollo.web.user.UserManager;
import org.bbop.apollo.web.util.JSONUtil;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DrupalUserAuthentication implements UserAuthentication {

    private static final int DRUPAL_HASH_LENGTH = 55;

    private final Logger logger = LogManager.getLogger(LogManager.ROOT_LOGGER_NAME);

    /** 
     * Variables with values derived from the drupal.xml config file
     */
    private String drupalUsername = null;
    private String drupalPassword = null;
    private String drupalURL = null;

    /**
     * The class constructor.  
     */
    public DrupalUserAuthentication() {
        try {
            // Read in the configuration settings.

//            URL resource = getClass().getResource("/");
            File currentDirectory= new File(".");
            String[] extensions = new String[]{"xml"};
            Collection<File> files = FileUtils.listFiles(currentDirectory, extensions, true);
            String configPath = null ;
            Iterator<File> fileIterator = files.iterator() ;
            while(fileIterator.hasNext() && configPath ==null ){
                File file = fileIterator.next();
                if(file.getName().contains("drupal.xml")){
                    configPath = file.getAbsolutePath();
                    logger.info("Found the drupal file: "+configPath);
                }
            }
            if(configPath==null ){
                logger.error("could not find the drupal.xml file in path") ;
                return;
            }
//            String configPath = resource.getPath() + "/../../config/drupal.xml";
//            String configPath = resource.getPath() + "/../../config/drupal.xml";
//            InputStream configuration = servletContext.getResourceAsStream("/config/config.xml");
            FileInputStream fstream = new FileInputStream(configPath);
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(fstream);

            // Parse the elements of the drupal.xml config file.
            Element userNode = (Element)doc.getElementsByTagName("user").item(0);
            if (userNode != null) {
               Element databaseNode = (Element)userNode.getElementsByTagName("database").item(0);
                if (databaseNode != null) {
                   drupalURL = databaseNode.getElementsByTagName("url").item(0).getTextContent();
                    Node userNameNode = databaseNode.getElementsByTagName("username").item(0);
                    if (userNameNode != null) {
                       drupalUsername = userNameNode.getTextContent();
                    }
                    Node passwordNode = databaseNode.getElementsByTagName("password").item(0);
                    if (passwordNode != null) {
                       drupalPassword = passwordNode.getTextContent();
                    }
                }
            }
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        catch (ParserConfigurationException e) {
            e.printStackTrace();
        } 
        catch (SAXException e) {
            e.printStackTrace();
        } 
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Overrides the generateUserLoginPage() function.
     */
//    public void generateUserLoginPage(HttpServlet servlet, HttpServletRequest request, HttpServletResponse response) throws ServletException {
//        InputStream in = servlet.getServletContext().getResourceAsStream("/user_interfaces/localdb/login.html");
//        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
//        String line;
//        try {
//            while ((line = reader.readLine()) != null) {
//                response.getOutputStream().println(line);
//            }
//            in.close();
//        } catch (IOException e) {
//            throw new ServletException(e);
//        }
//    }

    /**
     * Overrides the validateUser() function.
     */
    public String validateUser(HttpServletRequest request, HttpServletResponse response) throws UserAuthenticationException {
        String returnName = null;
        try {
            // Get the user name and password supplied by the calling script
            JSONObject requestJSON = JSONUtil.convertInputStreamToJSON(request.getInputStream());
            String username = requestJSON.getString("username");
            String password = requestJSON.getString("password");

            // If both the user name and password are set to '__SESSION__' then
            // this means the caller wants to authenticate using the Drupal
            // Session ID.  If not, then try to authenticate using credentials.
            if (username.contentEquals("__SESSION__") && 
                password.contentEquals("__SESSION__")) {

                // First check the Drupal session cookie and see if it is
                // associated with a valid user. This will only exist if 
                // WebApollo is running on the same domain as the Drupal server.
                // Checking the session ID allows WebApollo to auto login using
                // for the Drupal user that is already logged in.
                String dname = getDrupalSessionUser(request);
                if (dname != null) {
                    returnName = dname;
                }
            }
            // Authenticate using user credentials
            else {
                if (!validateDrupalUser(username, password)) {
                    throw new UserAuthenticationException("Invalid login");
                }
                else {
                  returnName = username;
                }
            }
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
        return returnName;
    }


    /**
     * Overrides the getUserLoginPageURL() function.
     */
    public String getUserLoginPageURL() {
        return "/WEB-INF/jsp/user_interfaces/drupal/login.jsp";
    }

    /**
     * Overrides the getAddUserURL() function.
     */
    public String getAddUserURL() {
        return "/WEB-INF/jsp/user_interfaces/localdb/addUser.jsp";
    }
    
    /**
     * Performs user authentication using the Drupal Session ID
     * 
     *   The request object
     *
     * @return
     *   The name of the Drupal user if the session ID and Host IP match
     */
    private String getDrupalSessionUser(HttpServletRequest request) {
        String dname = null;
        try {
            Cookie[] cookies = request.getCookies();

            for(Cookie cookie : cookies){
                if(cookie.getName().startsWith("SESS")){
                    String session_id = cookie.getValue();
                    String ip_address = request.getRemoteAddr();

                    // Now that we have a session, test to see if this
                    // session exists in the Drupal database. If it does,
                    // then get the user name and return.
                    Connection drupalConn = DriverManager.getConnection(drupalURL, drupalUsername, drupalPassword);
                    String sql = "SELECT name " +
                                 "FROM sessions S " +
                                 "  INNER JOIN users U on U.uid = S.uid " +
                                 "WHERE sid = ? and hostname = ?";
                    PreparedStatement stmt = drupalConn.prepareStatement(sql);
                    stmt.setString(1, session_id);
                    stmt.setString(2, ip_address);
                    ResultSet rs = stmt.executeQuery();

                    // Iterate through the matched records and get the matched name
                    if (rs.next()) {
                         dname = rs.getString(1);    
                    }
                    drupalConn.close();
                }
            }
        } catch(Exception e) {
            System.out.println("Unable to get cookie using CookieHandler");
            e.printStackTrace();
        }
        return dname;
    }

    /**
     * Authenticates a user name and password with the Drupal database.
     * 
     * Users that are blocked by Drupal will not pass validation checks.
     * 
     * @param username
     *   The user name to validate 
     * @param password
     *   The password for the user.
     * 
     * @return boolean
     *   TRUE if the user name and password are valid (and not blocked) FALSE
     *   otherwise.
     * 
     * @throws SQLException
     */
    private boolean validateDrupalUser(String username, String password) throws SQLException {
        
        boolean valid = false;
        
        // Establish the connection with Drupal, and query the database for the given user
        Connection drupalConn = DriverManager.getConnection(drupalURL, drupalUsername, drupalPassword);
        String sql = "SELECT name, pass, status FROM users WHERE name = ?";
        PreparedStatement stmt = drupalConn.prepareStatement(sql);
        stmt.setString(1, username);
        ResultSet rs = stmt.executeQuery();
        
        // Iterate through the matched records and see if the password matches
        if (rs.next()) {

            // Get the response fields
            String  dname = rs.getString(1);
            String  dpass = rs.getString(2);
            Integer dstatus = rs.getInt(3);

            // A Drupal status of 1 for the user indicates the user is blocked
            // we want to maintain that functionality and disallow access for
            // blocked users.
            if (dstatus == 1) {
                try {
                    // Encrypt the the user supplied password then
                    // see if it matches the password in the drupal record.
                    String hash = password_crypt(password, dpass);
                    if (hash.contentEquals(dpass)) {
                        valid = true;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        drupalConn.close();

        return valid;
    }

    /**
     * The proceeding functions were copied from a post on the following site
     * http://docs.oracle.com/javase/tutorial/jdbc/basics/connecting.html 
     **/

    private static String _password_itoa64() {
        return "./0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    }

    private static int password_get_count_log2(String setting) {
        return _password_itoa64().indexOf(setting.charAt(3));
    }

    private static byte[] sha512(String input) {
        try {
            return java.security.MessageDigest.getInstance("SHA-512").digest(input.getBytes());
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        }
        return new byte[0];
    }

    private static byte[] sha512(byte[] input) {
        try {
            return java.security.MessageDigest.getInstance("SHA-512").digest(input);
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        }
        return new byte[0];
    }

    private static String password_crypt(String password, String passwordHash) throws Exception {
        // The first 12 characters of an existing hash are its setting string.
        passwordHash = passwordHash.substring(0, 12);
        int count_log2 = password_get_count_log2(passwordHash);
        String salt = passwordHash.substring(4, 12);
        // Hashes must have an 8 character salt.
        if (salt.length() != 8) {
            return null;
        }

        int count = 1 << count_log2;

        byte[] hash;
        try {
            hash = sha512(salt.concat(password));

            do {
                hash = sha512(joinBytes(hash, password.getBytes("UTF-8")));
            } while (--count > 0);
        } catch (Exception e) {
            System.out.println("error " + e.toString());
            return null;
        }

        String output = passwordHash + _password_base64_encode(hash, hash.length);
        return (output.length() > 0) ? output.substring(0, DRUPAL_HASH_LENGTH) : null;
    }

    private static byte[] joinBytes(byte[] a, byte[] b) {
        byte[] combined = new byte[a.length + b.length];

        System.arraycopy(a, 0, combined, 0, a.length);
        System.arraycopy(b, 0, combined, a.length, b.length);
        return combined;
    }



    private static String _password_base64_encode(byte[] input, int count) throws Exception {

        StringBuffer output = new StringBuffer();
        int i = 0;
        CharSequence itoa64 = _password_itoa64();
        do {
            long value = SignedByteToUnsignedLong(input[i++]);

            output.append(itoa64.charAt((int) value & 0x3f));
            if (i < count) {
                value |= SignedByteToUnsignedLong(input[i]) << 8;
            }
            output.append(itoa64.charAt((int) (value >> 6) & 0x3f));
            if (i++ >= count) {
                break;
            }
            if (i < count) {
                value |=  SignedByteToUnsignedLong(input[i]) << 16;
            }

            output.append(itoa64.charAt((int) (value >> 12) & 0x3f));
            if (i++ >= count) {
                break;
            }
            output.append(itoa64.charAt((int) (value >> 18) & 0x3f));
        } while (i < count);

        return output.toString();
    }


    public static long SignedByteToUnsignedLong(byte b) {
        return b & 0xFF;
    }

}
