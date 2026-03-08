package org.bbop.apollo.gwt.client.dto;

import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;

/**
 * Created by deepak.unni3 on 9/16/16.
 */
public class CommentInfo {

    private String comment;

    public CommentInfo() {
    }

    public CommentInfo(JSONObject variantPropertyInfoJsonObject) {
        String value = null ;
        if(variantPropertyInfoJsonObject.containsKey(FeatureStringEnum.COMMENT.getValue())){
            value = variantPropertyInfoJsonObject.get(FeatureStringEnum.COMMENT.getValue()).isString().stringValue();
        }
        this.comment = value;
    }

    public CommentInfo(String value) {
        this.comment = value ;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public JSONObject convertToJsonObject() {
        JSONObject variantPropertyJsonObject = new JSONObject();
        variantPropertyJsonObject.put(FeatureStringEnum.COMMENT.getValue(), new JSONString(this.comment));
        return variantPropertyJsonObject;
    }
}
