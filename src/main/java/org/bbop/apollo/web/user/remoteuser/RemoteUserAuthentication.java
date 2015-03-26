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
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bbop.apollo.web.user.UserAuthenticationException;
import org.bbop.apollo.web.user.UserAuthentication;
import org.bbop.apollo.web.user.UserManager;
import org.bbop.apollo.web.util.JSONUtil;
import org.bbop.apollo.web.config.ServerConfiguration;
import org.json.JSONException;
import org.json.JSONObject;

public class RemoteUserAuthentication implements UserAuthentication {

    private ServerConfiguration serverConfig;

    public RemoteUserAuthentication(ServerConfiguration serverConfig) {
        this.serverConfig = serverConfig;
    }

    @Override
    public String validateUser(HttpServletRequest request,
            HttpServletResponse response) throws UserAuthenticationException {
        String username = request.getHeader("REMOTE_USER");
        if(username == null){
            // If the header isn't provided, it's actually a server
            // configuration error
            throw new UserAuthenticationException("Invalid login");
        } else {
            UserManager umi = UserManager.getInstance();
            try {
                Set<String> users = umi.getUserNames();
                if(serverConfig.getAutoCreateUsers() && !users.contains(username) ){
                    umi.addUser(username);
                    umi.setDefaultUserTrackPermissions(username, serverConfig.getTracks());
                }
            } catch(SQLException sqle) {
                // Handle this?
            }
            return username;
        }
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
