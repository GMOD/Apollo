package org.bbop.apollo.gwt.client.dto;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.json.client.JSONBoolean;
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

    private String genus ;
    private String species ;
    private String directory ;
    private String blatDb ;

    private Integer numFeatures ;
    private Integer numSequences;
    private Integer numTracks;
    private Boolean valid ;
    private boolean current;

    public OrganismInfo(){

    }


    public OrganismInfo(String name) {
        this.name = name;
    }

    public String getBlatDb() {
        return blatDb;
    }

    public void setBlatDb(String blatdb) {
        this.blatDb = blatdb;
    }

    public String getGenus() {
        return genus;
    }

    public void setGenus(String genus) {
        this.genus = genus;
    }

    public String getSpecies() {
        return species;
    }

    public void setSpecies(String species) {
        this.species = species;
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

    public Boolean getValid() {
        return valid;
    }

    public void setValid(Boolean valid) {
        this.valid = valid;
    }

    public void setCurrent(boolean current) {
        this.current = current;
    }

    public boolean isCurrent() {
        return current;
    }

    public JSONObject toJSON() {
        JSONObject payload = new JSONObject();
        payload.put("id",new JSONString(id));
        payload.put("name",new JSONString(name));
        payload.put("directory",new JSONString(directory));
        payload.put("current",JSONBoolean.getInstance(current));
        if(blatDb!=null) {
            payload.put("blatdb", new JSONString(blatDb));
        }
        if(genus!=null){
            payload.put("genus",new JSONString(genus));
        }
        if(species!=null){
            payload.put("species",new JSONString(species));
        }
        if(valid!=null){
            payload.put("valid",JSONBoolean.getInstance(valid));
        }

        return payload;
    }

}
