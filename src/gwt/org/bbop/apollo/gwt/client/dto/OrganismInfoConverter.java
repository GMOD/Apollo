package org.bbop.apollo.gwt.client.dto;
import com.google.gwt.core.client.GWT;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.user.client.Window;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ndunn on 3/31/15.
 */
public class OrganismInfoConverter {

    public static OrganismInfo convertFromJson(JSONObject object){
        OrganismInfo organismInfo = new OrganismInfo();
        organismInfo.setId(object.get("id").isNumber().toString());
        organismInfo.setName(object.get("commonName").isString().stringValue());
        if(object.get("sequences")!=null){
            organismInfo.setNumSequences((int) Math.round(object.get("sequences").isNumber().doubleValue()));
        }
        organismInfo.setDirectory(object.get("directory").isString().stringValue());
        //GWT.log(object.get("blatdb"));
        if(object.get("blatdb")!=null && object.get("blatdb").isString()!=null){
            organismInfo.setBlatDb(object.get("blatdb").isString().stringValue());
        }
        organismInfo.setCurrent(object.get("currentOrganism")!=null && object.get("currentOrganism").isBoolean().booleanValue());
        organismInfo.setNumFeatures(0);
        organismInfo.setNumTracks(0);
        return organismInfo ;
    }

    public static List<OrganismInfo> convertFromJsonArray(JSONArray organismList) {
        List<OrganismInfo> organismInfoList = new ArrayList<>();

        for(int i = 0 ; i < organismList.size() ; i++){
            organismInfoList.add(convertFromJson( organismList.get(i).isObject()));
        }

        return organismInfoList;
    }
}
