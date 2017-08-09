package org.bbop.apollo.gwt.client.dto;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.json.client.JSONBoolean;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.Window;

/**
 * Created by ndunn on 12/18/14.
 */
public class OrganismInfo implements HasJSON{

    // permanent key
    private String id;

    // writeable fields
    private String name ;

    private String genus ;
    private String species ;
    private String directory ;
    private String blatdb ;

    private Integer numFeatures ;
    private Integer numSequences;
    private Boolean valid ;
    private Boolean current;
    private Boolean publicMode;
    private Boolean canEdit ;
    private boolean editable;
    private String nonDefaultTranslationTable ;


    public OrganismInfo(){

    }


    public OrganismInfo(String name) {
        this.name = name;
    }

    public String getBlatDb() {
        return blatdb;
    }

    public void setBlatDb(String blatdb) {
        this.blatdb = blatdb;
    }

    public boolean getPublicMode() {
        return publicMode;
    }

    public void setPublicMode(boolean pm) {
        this.publicMode=pm;
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

    public String getNonDefaultTranslationTable() {
        return nonDefaultTranslationTable;
    }

    public void setNonDefaultTranslationTable(String nonDefaultTranslationTable) {
        this.nonDefaultTranslationTable = nonDefaultTranslationTable;
    }

    public JSONObject toJSON() {
        JSONObject payload = new JSONObject();

        if(id!=null) {
            payload.put("id", new JSONString(id));
        }
        if(name!=null) {
            payload.put("name", new JSONString(name));
        }
        if(directory!=null) {
            payload.put("directory", new JSONString(directory));
        }
        if(current!=null) {
            payload.put("current", JSONBoolean.getInstance(current));
        }
        if(blatdb!=null) {
            payload.put("blatdb", new JSONString(blatdb));
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
        if(nonDefaultTranslationTable!=null){
            payload.put("nonDefaultTranslationTable",new JSONString(nonDefaultTranslationTable));
        }

        payload.put("publicMode",JSONBoolean.getInstance(publicMode != null ? publicMode : false));

        return payload;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public boolean isEditable() {
        return editable;
    }
}
