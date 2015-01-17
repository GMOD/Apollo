package org.bbop.apollo.gwt.client.dto;

import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;

/**
 * Created by ndunn on 12/18/14.
 */
public class OrganismInfo {

    // permanent key
    private String id;

    // writeable fields
    private String name ;
    private String directory ;

    private Integer numFeatures ;
    private Integer numSequences;
    private Integer numTracks;

    public OrganismInfo(){

    }

    public OrganismInfo(String name) {
        this.name = name;

        this.numFeatures = (int) Math.round(Math.random()*200) ;
        this.numSequences = (int) Math.round(Math.random()*200) ;
        this.numTracks = (int) Math.round(Math.random()*200) ;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getNumFeatures() {
        return numFeatures;
    }

    public void setNumFeatures(Integer numFeatures) {
        this.numFeatures = numFeatures;
    }

    public Integer getNumSequences() {
        return numSequences;
    }

    public void setNumSequences(Integer numSequences) {
        this.numSequences = numSequences;
    }

    public Integer getNumTracks() {
        return numTracks;
    }

    public void setNumTracks(Integer numTracks) {
        this.numTracks = numTracks;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public String toJSON() {
        JSONObject payload = new JSONObject();
        payload.put("id",new JSONString(id));
        payload.put("name",new JSONString(name));
        payload.put("directory",new JSONString(directory));

//        AutoBean<OrganismInfo> organismInfoAutoBean = AutoBeanUtils.getAutoBean():
        return payload.toString();
    }
}
