package org.bbop.apollo.gwt.client.dto;

import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;

/**
 * Created by ndunn on 12/18/14.
 */
public class GroupInfo {

    private String name ;
    private Integer numberOfUsers;
    private Integer numberOrganisms;
    private Integer numberSequences;
    private Long id;

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();

        if(id!=null){
            jsonObject.put("id",new JSONNumber(id));
        }
        jsonObject.put("name",new JSONString(name));

        if(numberOfUsers!=null){
            jsonObject.put("numberOfUsers",new JSONNumber(numberOfUsers));
        }

        return jsonObject;
    }
}
