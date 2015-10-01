package org.bbop.apollo.gwt.client.dto.bookmark;

import com.google.gwt.json.client.JSONArray;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;

/**
 * Created by ndunn on 9/30/15.
 */
public class SequenceFeatureList extends JSONArray{

    public SequenceFeatureList(){}

    public SequenceFeatureList(JSONArray array) {
        for(int i = 0 ; i < array.size() ; i++){
            set(size(), array.get(i));
        }
    }

    public SequenceFeatureInfo getFeature(int j) {
        SequenceFeatureInfo sequenceFeatureInfo = new SequenceFeatureInfo();
        sequenceFeatureInfo.setName(get(j).isObject().get(FeatureStringEnum.NAME.getValue()).isString().stringValue());
        return sequenceFeatureInfo;
    }

    public void addFeature(SequenceFeatureInfo featuresObject) {
        set(size(),featuresObject);
    }
}
