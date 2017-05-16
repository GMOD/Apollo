package org.bbop.apollo.gwt.client.assemblage;


import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;


/**
 * Created by nathandunn on 10/5/16.
 *
 * Represents a DiscontinuousProjection, which holds a number of FeatureLocation objects
 *
 */
public class FeatureLocations extends JSONArray {

    public void addFeatureLocation(FeatureLocationInfo featureLocationInfo){
        set(size(),featureLocationInfo);
    }


    public FeatureLocationInfo getFeatureLocationInfo(int i) {
        JSONObject jsonObject = get(i).isObject();
//        return (FeatureLocationInfo) jsonObject;
        if(jsonObject instanceof FeatureLocationInfo){
            return (FeatureLocationInfo) jsonObject;
        }

        return new FeatureLocationInfo(jsonObject);
    }
}
