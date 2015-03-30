package org.bbop.apollo.gwt.client.dto;

import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;

/**
 * Created by ndunn on 12/18/14.
 */
public class GroupInfo {

    String name ;
    Integer numberOfUsers;
    Integer numberOrganisms;
    Integer numberSequences;
    private long id;

//    public GroupInfo(String name){
//        this.name = name ;
//        this.numberOfUsers = (int) Math.round(Math.random()*100);
//        this.numberOrganisms = (int) Math.round(Math.random()*100);
//        this.numberSequences = (int) Math.round(Math.random()*100);
//    }

    public Integer getNumberOfUsers() {
        return numberOfUsers;
    }

    public void setNumberOfUsers(Integer numberOfUsers) {
        this.numberOfUsers = numberOfUsers;
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

    public void setId(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();

        jsonObject.put("id",new JSONNumber(id));
        jsonObject.put("name",new JSONString(name));
        jsonObject.put("numberOfUsers",new JSONNumber(numberOfUsers));

        return jsonObject;
    }
}
