package org.bbop.apollo.gwt.shared.go;

/**
 * Placeholder for a feature with a single annotaiton
 */
public class GoTerm {

    //    Long id;
    private String name;
    private String prefix;
    private String lookupId;

    public GoTerm(String lookup) {
        if(lookup.contains(":")){
            this.prefix = lookup.split(":")[0];
            this.lookupId = lookup.split(":")[1];
        }
        else{
            this.name = lookup  ;
        }
    }

    public GoTerm(String lookup,String name ) {
        this.prefix = lookup.split(":")[0];
        this.lookupId = lookup.split(":")[1];
        this.name = name ;
    }
//    GoGene goGene ;

//    public Long getId() {
//        return id;
//    }
//
//    public void setId(Long id) {
//        this.id = id;
//    }


    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getLookupId() {
        return lookupId;
    }

    public void setLookupId(String lookupId) {
        this.lookupId = lookupId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

//    public GoGene getGoGene() {
//        return goGene;
//    }
//
//    public void setGoGene(GoGene goGene) {
//        this.goGene = goGene;
//    }

    public String getLinkDisplay() {
        return prefix + ":" + lookupId;
    }
}
