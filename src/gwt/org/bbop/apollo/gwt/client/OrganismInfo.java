package org.bbop.apollo.gwt.client;

/**
 * Created by ndunn on 12/18/14.
 */
public class OrganismInfo {

    private String name ;
    private Integer numFeatures ;

    public OrganismInfo(String name) {
        this.name = name;
        this.numFeatures = (int) Math.round(Math.random()*200) ;
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
}
