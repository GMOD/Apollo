package org.bbop.apollo.gwt.shared.go;

/**
 * Placeholder for a feature
 */
public class GoGene {

    // points to annotation features
    private Long id;
    private String name ;
    private String uniqueName ;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUniqueName() {
        return uniqueName;
    }

    public void setUniqueName(String uniqueName) {
        this.uniqueName = uniqueName;
    }
}
