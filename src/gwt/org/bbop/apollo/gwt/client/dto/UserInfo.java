package org.bbop.apollo.gwt.client.dto;

import java.util.List;

/**
 * Created by ndunn on 12/18/14.
 */
public class UserInfo {
    String name ;
    String email;
    Integer numberUserGroups ;

    public UserInfo(String name){
        this.name = name ;
        this.email = (name.replace(" ","_")+"@place.gov").toLowerCase();
        this.numberUserGroups = (int) Math.round(Math.random()*100);
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
}
