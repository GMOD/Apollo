package org.bbop.apollo.gwt.client.dto;

/**
 * Created by ndunn on 12/18/14.
 */
public class UserGroupInfo {
    String name ;
    Integer numberOfUsers;
    Integer numberOrganisms;
    Integer numberSequences;

    public UserGroupInfo(String name){
        this.name = name ;
        this.numberOfUsers = (int) Math.round(Math.random()*100);
        this.numberOrganisms = (int) Math.round(Math.random()*100);
        this.numberSequences = (int) Math.round(Math.random()*100);
    }

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
}
