package org.bbop.apollo.gwt.client.dto;

import java.util.List;

/**
 * Created by ndunn on 12/18/14.
 */
public class UserInfo {
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
}
