package org.bbop.apollo.gwt.client;

import com.google.gwt.json.client.JSONObject;
import com.google.gwt.user.client.ui.HTML;

/**
 * Created by ndunn on 1/8/15.
 */
public class AnnotationContainerWidget extends HTML{

    private JSONObject internalData ;

    public AnnotationContainerWidget(String string){
        super(string);
    }

    public AnnotationContainerWidget(JSONObject object) {
        internalData = object ;
        String featureName = object.get("name").isString().stringValue();
        String featureType = object.get("type").isObject().get("name").isString().stringValue();
        int lastFeature = featureType.lastIndexOf(".");
        featureType = featureType.substring(lastFeature + 1);
        HTML html = new HTML(featureName + " <div class='label label-success'>" + featureType + "</div>");
        setHTML(html.getHTML());
    }

    public JSONObject getInternalData() {
        return internalData;
    }
}
