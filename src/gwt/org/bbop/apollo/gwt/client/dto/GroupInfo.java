package org.bbop.apollo.gwt.client.dto;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ndunn on 12/18/14.
 */
public class GroupInfo implements HasJSON{

    private String name;
    private Integer numberOfUsers;
    private Integer numberOfAdmin;
    private Integer numberOrganisms;
    private Integer numberSequences;
    private Long id;
    private List<UserInfo> userInfoList;
    private List<UserInfo> adminInfoList;
    private Map<String, GroupOrganismPermissionInfo> organismPermissionMap = new HashMap<>();

//    public GroupInfo(String name){
//        this.name = name ;
//        this.numberOfUsers = (int) Math.round(Math.random()*100);
//        this.numberOrganisms = (int) Math.round(Math.random()*100);
//        this.numberSequences = (int) Math.round(Math.random()*100);
//    }

    public Integer getNumberOfUsers() { return numberOfUsers; }
    public Integer getNumberofAdmin() { return numberOfAdmin; }

    public void setNumberOfUsers(Integer numberOfUsers) {
        this.numberOfUsers = numberOfUsers;
    }
    public void setNumberOfAdmin(Integer numberOfAdmin) {
        this.numberOfAdmin = numberOfAdmin;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getNumberOrganisms() {
        return numberOrganisms;
    }

    public void setNumberOrganisms(Integer numberOrganisms) {
        this.numberOrganisms = numberOrganisms;
    }

    public Integer getNumberSequences() {
        return numberSequences;
    }

    public void setNumberSequences(Integer numberSequences) {
        this.numberSequences = numberSequences;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<UserInfo> getUserInfoList() {
        return userInfoList;
    }

    public List<UserInfo> getAdminInfoList() {
        return adminInfoList;
    }

    public void setUserInfoList(List<UserInfo> userInfoList) {
        this.userInfoList = userInfoList;
    }
    public void setAdminInfoList(List<UserInfo> adminInfoList) { this.adminInfoList = adminInfoList; }

    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();

        if (id != null) {
            jsonObject.put("id", new JSONNumber(id));
        }
        jsonObject.put("name", new JSONString(name));

        if (numberOfUsers != null) {
            jsonObject.put("numberOfUsers", new JSONNumber(numberOfUsers));
        }

        if (numberOfAdmin != null) {
            jsonObject.put("numberOfAdmin", new JSONNumber(numberOfAdmin));
        }

        JSONArray userInfoArray = new JSONArray();
        for (int i = 0; userInfoList != null && i < userInfoList.size(); i++) {
            userInfoArray.set(i,userInfoList.get(i).toJSON());
        }
        jsonObject.put("users",userInfoArray);

        JSONArray adminInfoArray = new JSONArray();
        for (int i = 0; adminInfoList != null && i < adminInfoList.size(); i++) {
            adminInfoArray.set(i,adminInfoList.get(i).toJSON());
        }
        jsonObject.put("admin",adminInfoArray);

        JSONArray organismPermissions = new JSONArray();
        int index = 0 ;
        for(String organism : organismPermissionMap.keySet()){
            JSONObject orgPermission = new JSONObject();
            orgPermission.put(organism,organismPermissionMap.get(organism).toJSON());
            organismPermissions.set(index,orgPermission);
            ++index ;
        }
        jsonObject.put("organismPermissions",organismPermissions);

        return jsonObject;
    }

    public void setOrganismPermissionMap(Map<String, GroupOrganismPermissionInfo> organismPermissionMap) {
        this.organismPermissionMap = organismPermissionMap;
    }

    public Map<String, GroupOrganismPermissionInfo> getOrganismPermissionMap() {
        return organismPermissionMap;
    }
}
