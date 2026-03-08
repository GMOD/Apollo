package org.bbop.apollo.gwt.client.dto;

import com.google.gwt.json.client.JSONBoolean;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;

/**
 * Created by ndunn on 3/24/15.
 */
public class UserOrganismPermissionInfo extends OrganismPermissionInfo{

    Long userId ;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public JSONObject toJSON() {
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
        return payload;
    }
}
