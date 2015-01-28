package org.bbop.apollo.gwt.client.dto;

/**
 * Created by ndunn on 1/27/15.
 */
public class AnnotationInfo {
    private String name;
    private String type;
    private Integer min ;
    private Integer max ;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public Integer getLength() {
        if(min!=null && max!=null){
            return max - min ;
        }
        return -1 ;
    }

    public Integer getMin() {
        return min;
    }

    public void setMin(Integer min) {
        this.min = min;
    }

    public Integer getMax() {
        return max;
    }

    public void setMax(Integer max) {
        this.max = max;
    }
}
