package org.bbop.apollo.gwt.client.dto;

import com.google.gwt.json.client.JSONBoolean;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;

/**
 * Created by ndunn on 3/24/15.
 */
public class UserOrganismPermissionInfo {

    String organismName;
    Long userId ;
    Long id;
    Boolean admin = false ;
    Boolean write= false ;
    Boolean export= false ;
    Boolean read= false ;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getOrganismName() {
        return organismName;
    }

    public void setOrganismName(String organismName) {
        this.organismName = organismName;
    }

    public Boolean isAdmin() {
        return admin;
    }

    public void setAdmin(Boolean admin) {
        this.admin = admin;
    }

    public Boolean isWrite() {
        return write;
    }

    public void setWrite(Boolean write) {
        this.write = write;
    }

    public Boolean isExport() {
        return export;
    }

    public void setExport(Boolean export) {
        this.export = export;
    }

    public Boolean isRead() {
        return read;
    }

    public void setRead(Boolean read) {
        this.read = read;
    }


    public String toJSON() {
        JSONObject payload = new JSONObject();
        payload.put("organism",new JSONString(organismName));
        payload.put("ADMINISTRATE",JSONBoolean.getInstance(admin));
        payload.put("WRITE",JSONBoolean.getInstance(write));
        payload.put("EXPORT",JSONBoolean.getInstance(export));
        payload.put("READ",JSONBoolean.getInstance(read));
        if(userId!=null){
            payload.put("userId",new JSONNumber(userId));
        }
        if(id!=null){
            payload.put("id",new JSONNumber(id));
        }
        return payload.toString();
    }
}
