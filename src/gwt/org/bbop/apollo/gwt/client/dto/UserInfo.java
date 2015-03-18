package org.bbop.apollo.gwt.client.dto;

import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;

/**
 * Created by ndunn on 12/18/14.
 */
public class UserInfo {
    Long userId ;
    String firstName;
    String lastName;
    String email;
    Integer numberUserGroups ;
    
    public UserInfo(){}
    

    public UserInfo(String firstName){
        this.firstName = firstName ;
        this.email = (firstName.replace(" ","_")+"@place.gov").toLowerCase();
        this.numberUserGroups = (int) Math.round(Math.random()*100);
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
    
    public String getName(){
        return firstName +" " + lastName ;
    }
    
    public JSONObject toJSON(){
        JSONObject jsonObject = new JSONObject();
        if(userId!=null){
            jsonObject.put("userId",new JSONNumber(userId));
        }
        jsonObject.put("firstName",new JSONString(firstName));
        jsonObject.put("lastName",new JSONString(lastName));
        jsonObject.put("email",new JSONString(email));

        return jsonObject;
    }
}
