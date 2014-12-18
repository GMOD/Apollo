package org.bbop.apollo.gwt.client;

/**
 * Created by ndunn on 12/18/14.
 */
public class TrackInfo implements Comparable<TrackInfo> {

    private String name;
    private String type;
    private Boolean visible;

    public TrackInfo(String name, String type, Boolean visible) {
        this.name = name;
        this.type = type;
        this.visible = visible;
    }

    public TrackInfo(String name) {
        this.name = name;
        this.type = Math.random() > 0.5 ? "CanvasFeature" : "HTMLFeature";
        this.visible = Math.random() > 0.5 ;

    }

    @Override
    public int compareTo(TrackInfo o) {
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
}
