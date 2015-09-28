package org.bbop.apollo.gwt.client.dto;

import com.google.gwt.json.client.JSONObject;

/**
 * Created by Nathan Dunn on 12/18/14.
 */
public class BookmarkInfo implements Comparable<BookmarkInfo> {

    private String name;
    private String label;
    private String type;
    private Boolean visible;
    private String urlTemplate ;

    private JSONObject payload ;

    public BookmarkInfo(){}


    public BookmarkInfo(String name, String type, Boolean visible) {
        this.name = name;
        this.type = type;
        this.visible = visible;
    }

    public BookmarkInfo(String name) {
        this.name = name;
        this.type = Math.random() > 0.5 ? "CanvasFeature" : "HTMLFeature";
        this.visible = Math.random() > 0.5 ;

    }

    @Override
    public int compareTo(BookmarkInfo o) {
        return name.compareTo(o.name);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Boolean getVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getUrlTemplate() {
        return urlTemplate;
    }

    public void setUrlTemplate(String urlTemplate) {
        this.urlTemplate = urlTemplate;
    }

    public JSONObject getPayload() {
        return payload;
    }

    public void setPayload(JSONObject payload) {
        this.payload = payload;
    }
}
