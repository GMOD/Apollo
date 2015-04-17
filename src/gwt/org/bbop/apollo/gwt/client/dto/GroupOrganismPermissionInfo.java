package org.bbop.apollo.gwt.client.dto;

import com.google.gwt.json.client.JSONBoolean;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;

/**
 * Created by ndunn on 3/24/15.
 */
public class GroupOrganismPermissionInfo extends OrganismPermissionInfo{

    Long groupId ;


    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public JSONObject toJSON() {
        JSONObject payload = new JSONObject();
        payload.put("organism",new JSONString(organismName));
        payload.put("ADMINISTRATE",JSONBoolean.getInstance(admin));
        payload.put("WRITE",JSONBoolean.getInstance(write));
        payload.put("EXPORT",JSONBoolean.getInstance(export));
        payload.put("READ",JSONBoolean.getInstance(read));
        if(groupId!=null){
            payload.put("groupId",new JSONNumber(groupId));
        }
        if(id!=null){
            payload.put("id",new JSONNumber(id));
        }
        return payload;
    }
}
