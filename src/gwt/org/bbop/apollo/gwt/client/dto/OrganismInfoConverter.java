package org.bbop.apollo.gwt.client.dto;

import com.google.gwt.json.client.JSONObject;

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
        organismInfo.setCurrent(object.get("currentOrganism")!=null && object.get("currentOrganism").isBoolean().booleanValue());
        organismInfo.setNumFeatures(0);
        organismInfo.setNumTracks(0);
        return organismInfo ;
    }
}
