package org.bbop.apollo.gwt.client.dto;

/**
 * Created by ndunn on 12/18/14.
 */
public class OrganismInfo {

    private String id;
    private String name ;
    private Integer numFeatures ;
    private Integer numSequences;
    private Integer numTracks;
    private String directory ;

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
}
