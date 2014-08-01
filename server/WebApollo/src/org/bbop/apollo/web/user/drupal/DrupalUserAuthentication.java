package org.bbop.apollo.web.user.drupal;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

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

public class DrupalUserAuthentication implements UserAuthentication {

    private static final int DRUPAL_HASH_LENGTH = 55;
    
    private String DrupalUsername = null;
    private String DrupalPassword = null;
    private String DrupalURL = null;
    
    /**
     * The class constructor.  
     */
    public DrupalUserAuthentication() {
        try {
            // Read in the configuration settings.
            URL resource = getClass().getResource("/");
            String config_path = resource.getPath() + "/../../config/drupal.xml";
            FileInputStream fstream = new FileInputStream(config_path);
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(fstream);
            
            // Parse the elements of the drupal.xml config file.
            Element userNode = (Element)doc.getElementsByTagName("user").item(0);
            if (userNode != null) {
               Element databaseNode = (Element)userNode.getElementsByTagName("database").item(0);
                if (databaseNode != null) {
                   DrupalURL = databaseNode.getElementsByTagName("url").item(0).getTextContent();
                    Node userNameNode = databaseNode.getElementsByTagName("username").item(0);
                    if (userNameNode != null) {
                       DrupalUsername = userNameNode.getTextContent();
                    }
                    Node passwordNode = databaseNode.getElementsByTagName("password").item(0);
                    if (passwordNode != null) {
                       DrupalPassword = passwordNode.getTextContent();
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
    public void generateUserLoginPage(HttpServlet servlet, HttpServletRequest request, HttpServletResponse response) throws ServletException {
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
    /**
     * Overrides the validateUser() function.
     */
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


    /**
     * Overrides the getUserLoginPageURL() function.
     */
    public String getUserLoginPageURL() {
        return "user_interfaces/localdb/login.html";
    }

    /**
     * Overrides the getAddUserURL() function.
     */
    public String getAddUserURL() {
        return "user_interfaces/localdb/addUser.jsp";
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
    private boolean validateUser(String username, String password) throws SQLException {
        
        boolean valid = false;
        
        // Establish the connection with Drupal, and query the database for the given user
        Connection drupalConn = DriverManager.getConnection(DrupalURL, DrupalUsername, DrupalPassword);
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
