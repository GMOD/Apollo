package org.bbop.apollo.web.user;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.bbop.apollo.web.config.ServerConfiguration.TrackConfiguration;
import org.bbop.apollo.web.config.ServerConfiguration.TrackAutoPermissionsGroup;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.crackstation.PasswordHash;

public class UserManager {

    private static UserManager instance;
    private String databaseURL;
    private String databaseUserName;
    private String databasePassword;
    private final Logger logger = LogManager.getLogger(LogManager.ROOT_LOGGER_NAME);

    private UserManager() {
    }

    public static UserManager getInstance() {
        if (instance == null) {
            instance = new UserManager();
        }
        return instance;
    }

    public String getDatabaseURL() {
        return databaseURL;
    }

    public void setDatabaseURL(String dbURL) {
        this.databaseURL = dbURL;
    }

    public String getDatabaseUserName() {
        return databaseUserName;
    }

    public void setDatabaseUserName(String databaseUserName) {
        this.databaseUserName = databaseUserName;
    }

    public String getDatabasePassword() {
        return databasePassword;
    }

    public void setDatabasePassword(String databasePassword) {
        this.databasePassword = databasePassword;
    }

    public boolean isInitialized() {
        return databaseURL != null;
    }

    public void initialize(String dbDriver, String dbURL) throws ClassNotFoundException {
        initialize(dbDriver, dbURL, null, null);
    }

    public void initialize(String dbDriver, String dbURL, String userName, String password) throws ClassNotFoundException {
        Class.forName(dbDriver);
        setDatabaseURL(dbURL);
        setDatabaseUserName(userName);
        setDatabasePassword(password);
    }

    public int getTrackPermissionForUser(String track, String username) throws SQLException {
        Connection conn = getConnection();
        PreparedStatement stmt = conn.prepareStatement("SELECT track_id, user_id, permission FROM permissions INNER JOIN tracks USING (track_id) INNER JOIN users USING (user_id) WHERE track_name=? AND username=?");
        stmt.setString(1, track);
        stmt.setString(2, username);
        ResultSet rs = stmt.executeQuery();
        int permission = Permission.NONE;
        if (rs.next()) {
            permission = rs.getInt(3);
        }
        conn.close();
        return permission;

    }

    public Map<String, Integer> getPermissionsForUser(String username) throws SQLException {
        Map<String, Integer> permissions = new HashMap<String, Integer>();
        Connection conn = getConnection();
        PreparedStatement stmt = conn.prepareStatement("SELECT track_name, permission FROM permissions INNER JOIN tracks USING (track_id) INNER JOIN users USING (user_id) WHERE username=?");
        stmt.setString(1, username);
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            permissions.put(rs.getString(1), rs.getInt(2));
        }
        conn.close();
        return permissions;
    }

    public Set<String> getTrackNames() throws SQLException {
        Connection conn = getConnection();
        Set<String> trackNames = getTrackNamesWithPrimaryKey(conn).keySet();
        conn.close();
        return trackNames;
    }

    public boolean validateUser(String username) throws SQLException {
        Connection conn = getConnection();
        PreparedStatement stmt = conn.prepareStatement("SELECT username FROM users WHERE username=?");
        stmt.setString(1, username);
        ResultSet rs = stmt.executeQuery();
        boolean valid = rs.next();
        conn.close();
        return valid;
    }

    public Set<String> getUserNames() throws SQLException {
        Connection conn = getConnection();
        Set<String> userNames = getUserNamesWithPrimaryKey(conn).keySet();
        conn.close();
        return userNames;
    }

    public void updateAllTrackPermissionForUsers(Map<String, Integer> permissions) throws SQLException {
        Connection conn = getConnection();
        conn.setAutoCommit(false);
        Map<String, Integer> tracks = getTrackNamesWithPrimaryKey(conn);
        Map<String, Integer> users = getUserNamesWithPrimaryKey(conn);
        for (Map.Entry<String, Integer> permissionsEntrySet : permissions.entrySet()) {
            String username = permissionsEntrySet.getKey();
            int userId = users.get(username);
            int permission = permissionsEntrySet.getValue();
            Map<String, Integer> permissionsForUser = UserManager.getInstance().getPermissionsForUser(username);
            PreparedStatement stmt = conn.prepareStatement("INSERT INTO permissions VALUES(?, ?, ?)");
            for (Map.Entry<String, Integer> track : tracks.entrySet()) {
                int trackId = track.getValue();
                // If the user doesn't have pre-existing permissions on this track, insert them
                if (!permissionsForUser.containsKey(track.getKey())) {
                    stmt.setInt(1, trackId);
                    stmt.setInt(2, userId);
                    stmt.setInt(3, permission);
                    stmt.addBatch();
                }
            }
            stmt.executeBatch();
            stmt = conn.prepareStatement("UPDATE permissions SET permission=? WHERE user_id=?");
            stmt.setInt(1, permission);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        }
        conn.commit();
        conn.close();
    }

    /**
     * Written to be a track specific version of updateAllTrackPermissionForUsers.
     * There seems to be hints of work towards more configurable track specific permissions
     *
     * @param permissions a username to permission map
     * @param track_name track name
     *
     */
    public void updateTrackSpecificPermissionForUsers(Map<String, Integer> permissions, String track_name) throws SQLException {
        Connection conn = getConnection();
        conn.setAutoCommit(false);
        logger.debug("Updating permissions for " + track_name);
        Map<String, Integer> tracks = getTrackNamesWithPrimaryKey(conn);
        Map<String, Integer> users = getUserNamesWithPrimaryKey(conn);
        for (Map.Entry<String, Integer> permissionsEntrySet : permissions.entrySet()) {
            String username = permissionsEntrySet.getKey();
            int userId = users.get(username);
            int permission = permissionsEntrySet.getValue();
            Map<String, Integer> permissionsForUser = UserManager.getInstance().getPermissionsForUser(username);
            PreparedStatement stmt = conn.prepareStatement("INSERT INTO permissions VALUES(?, ?, ?)");
            int trackId = tracks.get(track_name);
            // If the user doesn't have pre-existing permissions on this track, insert them
            if (!permissionsForUser.containsKey(track_name)) {
                stmt.setInt(1, trackId);
                stmt.setInt(2, userId);
                stmt.setInt(3, permission);
                stmt.addBatch();
            }
            stmt.executeBatch();
            stmt = conn.prepareStatement("UPDATE permissions SET permission=? WHERE user_id=?");
            stmt.setInt(1, permission);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        }
        conn.commit();
        conn.close();
    }

    /**
     * Add user to Apollo user database.
     *
     * @param username New username for the user.
     * @param password password for the user which will automatically be encrypted.
     */
    public void addUser(String username, String password) throws SQLException {
        logger.debug("Adding user " + username);
        String encrypted = null;
        try {
            encrypted = PasswordHash.createHash(password);
        }
        catch (NoSuchAlgorithmException e) {
        }
        catch (InvalidKeySpecException e) {
        }
        Connection conn = getConnection();
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO users(username, password) VALUES(?, ?)");
        stmt.setString(1, username);
        stmt.setString(2, password);
        stmt.executeUpdate();
        conn.close();
    }

    /**
     * Set track specific default permissions for specific user.
     * Only applicable to auth methods with upstream authentication sources (e.g. RemoteUser/OAuth/BrowserID).
     * Only applicable when auto-creation of users is on
     *
     * @param username Username to apply automatic permissions to
     * @param tracks Map of track name/track configurations which hold default permission data.
     *
     **/
    public void setDefaultUserTrackPermissions(String username, Map<String, TrackConfiguration> tracks) throws SQLException {
        System.out.println("Applying default track permissions for " + username);
        Map<String, Integer> new_permissions = new HashMap<String, Integer>();
        // Loop across all known tracks
        for (Map.Entry<String, TrackConfiguration> trackEntry : tracks.entrySet()) {
            // There are multiple permission groups per track e.g.:
            //  - one listing read/write users,
            //  - one listing read-only users
            new_permissions.clear();
            TrackConfiguration track = trackEntry.getValue();
            System.out.println("Track " + track.getName());
            for (TrackAutoPermissionsGroup trackPermissions : track.getAutoPermissions()) {
                // If this trackPermissions applies to username
                //
                // Precedence from first read from config file; exit early as
                // soon as we have a match
                if (trackPermissions.validateUser(username)) {
                    System.out.println("Applying permissions " + trackPermissions.getPermissionValue() + " for " + username +" to " + track.getName());
                    new_permissions.put(username, trackPermissions.getPermissionValue());
                    break;
                }
            }
            updateTrackSpecificPermissionForUsers(new_permissions, track.getName());
        }
    }

    /**
     * Add a user without a password.
     * Some authentication methods are passwordless, especially those storing
     * passwords "upstream", e.g. OAuth/BrowserID/RemoteUser
     *
     * @param username username for the new user
     **/
    public void addUser(String username) throws SQLException {
        Connection conn = getConnection();
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO users(username) VALUES(?)");
        stmt.setString(1, username);
        stmt.executeUpdate();
        conn.close();
    }

    /**
     * Remove user.
     *
     * @param username username of the user to be deleted.
     */
    public void deleteUser(String username) throws SQLException {
        Connection conn = getConnection();
        PreparedStatement stmt = conn.prepareStatement("DELETE FROM permissions WHERE user_id=(SELECT user_id FROM users where username=?)");
        stmt.setString(1, username);
        stmt.executeUpdate();
        stmt = conn.prepareStatement("DELETE FROM users WHERE username=?");
        stmt.setString(1, username);
        stmt.executeUpdate();
        conn.close();
    }

    private Map<String, Integer> getUserNamesWithPrimaryKey(Connection conn) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT username, user_id FROM users");
        ResultSet rs = stmt.executeQuery();
        Map<String, Integer> userNames = new HashMap<String, Integer>();
        while (rs.next()) {
            userNames.put(rs.getString(1), rs.getInt(2));
        }
        return userNames;
    }

    private Map<String, Integer> getTrackNamesWithPrimaryKey(Connection conn) throws SQLException {
        Map<String, Integer> trackNames = new HashMap<String, Integer>();
        PreparedStatement stmt = conn.prepareStatement("SELECT track_name, track_id FROM tracks");
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            trackNames.put(rs.getString(1), rs.getInt(2));
        }
        return trackNames;
    }

    public Connection getConnection() throws SQLException {
        if (getDatabaseUserName() != null) {
            return DriverManager.getConnection(databaseURL, getDatabaseUserName(), getDatabasePassword());
        }
        return DriverManager.getConnection(databaseURL);
    }
}
