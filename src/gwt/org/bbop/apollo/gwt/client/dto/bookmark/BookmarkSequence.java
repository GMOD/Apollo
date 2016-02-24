package org.bbop.apollo.gwt.client.dto.bookmark;

import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;

/**
 * Created by ndunn on 9/30/15.
 */
public class BookmarkSequence extends JSONObject {

    public BookmarkSequence() {
    }

    public BookmarkSequence(JSONObject jsonObject) {
        this.put(FeatureStringEnum.NAME.getValue(), new JSONString(jsonObject.get(FeatureStringEnum.NAME.getValue()).isString().stringValue()));
        if(jsonObject.containsKey(FeatureStringEnum.FEATURE.getValue())){
            SequenceFeatureInfo sequenceFeatureInfo = new SequenceFeatureInfo(jsonObject.get(FeatureStringEnum.FEATURE.getValue()).isObject());
            this.put(
                    FeatureStringEnum.FEATURE.getValue()
                    ,sequenceFeatureInfo
            );
        }
//        if (jsonObject.containsKey(FeatureStringEnum.FEATURES.getValue())) {
//            this.put(FeatureStringEnum.FEATURES.getValue(), jsonObject.get(FeatureStringEnum.FEATURES.getValue()).isArray());
//        }
    }

    public String getName() {
        return get(FeatureStringEnum.NAME.getValue()).isString().stringValue();
    }

    public SequenceFeatureInfo getFeature() {
        if (containsKey(FeatureStringEnum.FEATURE.getValue())) {
//            JSONObject featureObject = get(FeatureStringEnum.FEATURE.getValue()).isObject();
//            return (SequenceFeatureInfo) featureObject  ;
            return new SequenceFeatureInfo(get(FeatureStringEnum.FEATURE.getValue()).isObject());
        }
        return null ;
    }


    public void setName(String groupName) {
        put(FeatureStringEnum.NAME.getValue(), new JSONString(groupName));
    }
}
