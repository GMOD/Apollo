package org.bbop.apollo.gwt.client.dto.bookmark;

import com.google.gwt.json.client.JSONArray;

/**
 * Created by ndunn on 9/30/15.
 */
public class SequenceFeatureList extends JSONArray{
    public SequenceFeatureInfo getFeature(int j) {
        return (SequenceFeatureInfo) get(j).isObject();
    }

    public void addFeature(SequenceFeatureInfo featuresObject) {
        set(size(),featuresObject);
    }
}
