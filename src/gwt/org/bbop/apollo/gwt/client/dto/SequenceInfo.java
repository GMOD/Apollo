package org.bbop.apollo.gwt.client.dto;

/**
 * Created by ndunn on 12/18/14.
 */
public class SequenceInfo {

    private String name ;
    private Integer length ;
    private Boolean selected;
    private Boolean aDefault;

    public SequenceInfo(){

    }

    public SequenceInfo(String name){
        this.name = name ;
        this.length = (int) Math.round(Math.random()*1000);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getLength() {
        return length;
    }

    public void setLength(Integer length) {
        this.length = length;
    }

    public Boolean isDefault() {
        return aDefault;
    }

    public void setDefault(Boolean aDefault) {
        this.aDefault = aDefault;
    }

    public Boolean getSelected() {
        return selected;
    }

    public void setSelected(Boolean selected) {
        this.selected = selected;
    }
}
