package org.bbop.apollo.gwt.client.dto;

import com.google.gwt.core.client.GWT;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;

import java.util.ArrayList;
import java.util.List;

public class CommentInfoConverter {

    public static CommentInfo convertToCommentFromObject(JSONObject jsonObject) {
        CommentInfo commentInfo = new CommentInfo();
        commentInfo.setComment(jsonObject.get(FeatureStringEnum.COMMENT.getValue()).isString().stringValue());
        return commentInfo;
    }

    public static List<CommentInfo> convertToCommentFromArray(JSONArray array) {
        List<CommentInfo> commentInfoList = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            commentInfoList.add(convertToCommentFromObject(array.get(i).isObject()));
        }
        return commentInfoList;
    }

    public static JSONObject convertToJson(CommentInfo commentInfo) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(FeatureStringEnum.COMMENT.getValue(), new JSONString(commentInfo.getComment()));
        return jsonObject;
    }

    public static CommentInfo convertFromJson(JSONObject object) {
        CommentInfo commentInfo = new CommentInfo();

//                    "geneRelationship":"RO:0002326", "goTerm":"GO:0031084", "references":"[\"ref:12312\"]", "gene":
//                    "1743ae6c-9a37-4a41-9b54-345065726d5f", "negate":false, "evidenceCode":"ECO:0000205", "withOrFrom":
//                    "[\"adf:12312\"]"
        GWT.log(object.toString());
        commentInfo.setComment(object.get(FeatureStringEnum.TAG.getValue()).isString().stringValue());
        return commentInfo;

    }
}
