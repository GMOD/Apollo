package org.bbop.apollo.web.user.remoteuser;

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

import org.bbop.apollo.web.user.UserAuthenticationException;
import org.bbop.apollo.web.user.UserAuthentication;
import org.bbop.apollo.web.user.UserManager;
import org.bbop.apollo.web.util.JSONUtil;
import org.json.JSONException;
import org.json.JSONObject;

public class RemoteUserAuthentication implements UserAuthentication {

//    @Override
//    public void generateUserLoginPage(HttpServlet servlet, HttpServletRequest request,
//            HttpServletResponse response) throws ServletException {
//        InputStream in = servlet.getServletContext().getResourceAsStream("/user_interfaces/remoteuser/login.html");
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

    @Override
    public String validateUser(HttpServletRequest request,
            HttpServletResponse response) throws UserAuthenticationException {
        String username = request.getHeader("REMOTE_USER");
        if(username == null){
            throw new UserAuthenticationException("Invalid login");
        }
        return username;
    }


    @Override
    public String getUserLoginPageURL() {
        return "/WEB-INF/jsp/user_interfaces/remoteuser/login.jsp";
    }

    @Override
    public String getAddUserURL() {
        return "/WEB-INF/jsp/user_interfaces/remoteuser/addUser.jsp";
    }
}
