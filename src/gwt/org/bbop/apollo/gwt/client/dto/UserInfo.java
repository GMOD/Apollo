package org.bbop.apollo.gwt.client.dto;

import com.google.gwt.http.client.URL;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import org.bbop.apollo.gwt.shared.PermissionEnum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ndunn on 12/18/14.
 */
public class UserInfo implements HasJSON {
    private Long userId;
    private String firstName;
    private String lastName;
    private String email;
    private String role;
    private Integer numberUserGroups;
    private String password;
    private List<String> groupList = new ArrayList<>();
    private List<String> availableGroupList = new ArrayList<>();
    private Map<String, UserOrganismPermissionInfo> organismPermissionMap = new HashMap<>();

    public UserInfo() {
    }


    public UserInfo(String firstName) {
        this.firstName = firstName;
        this.email = (firstName.replace(" ", "_") + "@place.gov").toLowerCase();
        this.numberUserGroups = (int) Math.round(Math.random() * 100);
    }

    public UserInfo(JSONObject userObject) {
        setEmail(userObject.get("email").isString().stringValue());
        setFirstName(userObject.get("firstName").isString().stringValue());
        setLastName(userObject.get("lastName").isString().stringValue());
        if (userObject.get("userId") != null) {
            setUserId((long) userObject.get("userId").isNumber().doubleValue());
        } else if (userObject.get("id") != null) {
            setUserId((long) userObject.get("id").isNumber().doubleValue());
        }
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getNumberUserGroups() {
        return numberUserGroups;
    }

    public void setNumberUserGroups(Integer numberUserGroups) {
        this.numberUserGroups = numberUserGroups;
    }

    public String getName() {
        return firstName + " " + lastName;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public List<String> getGroupList() {
        return groupList;
    }

    public void setGroupList(List<String> groupList) {
        this.groupList = groupList;
    }

    public List<String> getAvailableGroupList() {
        return availableGroupList;
    }

    public void setAvailableGroupList(List<String> availableGroupList) {
        this.availableGroupList = availableGroupList;
    }

    public Map<String, UserOrganismPermissionInfo> getOrganismPermissionMap() {
        return organismPermissionMap;
    }

    public void setOrganismPermissionMap(Map<String, UserOrganismPermissionInfo> organismPermissionMap) {
        this.organismPermissionMap = organismPermissionMap;
    }

    public JSONObject getJSONWithoutPassword() {
        JSONObject returnObject = toJSON();
        returnObject.put("password", new JSONString(""));
        return returnObject;
    }

    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        if (userId != null) {
            jsonObject.put("userId", new JSONNumber(userId));
        }
        jsonObject.put("firstName", new JSONString(firstName));
        jsonObject.put("lastName", new JSONString(lastName));
        jsonObject.put("email", new JSONString(email));
        // TODO: do not use this as it is the "security user" placeholder
//        jsonObject.put("username", new JSONString(email));
        if (role != null) {
            jsonObject.put("role", new JSONString(role));
        }

        JSONArray groupArray = new JSONArray();
        for (int i = 0; i < groupList.size(); i++) {
            groupArray.set(i, new JSONString(groupList.get(i)));
        }
        jsonObject.put("groups", groupArray);

        JSONArray availableGroupArray = new JSONArray();
        for (int i = 0; i < availableGroupList.size(); i++) {
            availableGroupArray.set(i, new JSONString(availableGroupList.get(i)));
        }
        jsonObject.put("availableGroups", availableGroupArray);

        if (password != null) {
            jsonObject.put("newPassword", new JSONString(URL.encodeQueryString(password)));
        }

        JSONArray organismPermissions = new JSONArray();
        int index = 0;
        for (String organism : organismPermissionMap.keySet()) {
            JSONObject orgPermission = new JSONObject();
            orgPermission.put(organism, organismPermissionMap.get(organism).toJSON());
            organismPermissions.set(index, orgPermission);
            ++index;
        }
        jsonObject.put("organismPermissions", organismPermissions);


        return jsonObject;
    }

    PermissionEnum findHighestPermission() {
        if (organismPermissionMap == null) return null;
        PermissionEnum highestPermission = PermissionEnum.NONE;

        for (UserOrganismPermissionInfo userOrganismPermissionInfo : organismPermissionMap.values()) {
            PermissionEnum thisHighestPermission = userOrganismPermissionInfo.getHighestPermission();
            if (thisHighestPermission.getRank() > highestPermission.getRank()) {
                highestPermission = thisHighestPermission;
            }
        }

        return highestPermission;
    }

}
