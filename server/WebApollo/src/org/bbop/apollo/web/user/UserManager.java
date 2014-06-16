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

import net.crackstation.PasswordHash;

public class UserManager {

	private static UserManager instance;
	private String databaseURL;
	private String databaseUserName;
	private String databasePassword;
	
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
	
	public void addUser(String username, String password) throws SQLException {
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
	
	public void addUser(String username) throws SQLException {
		Connection conn = getConnection();
		PreparedStatement stmt = conn.prepareStatement("INSERT INTO users(username) VALUES(?)");
		stmt.setString(1, username);
		stmt.executeUpdate();
		conn.close();
	}
	
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
