package org.bbop.apollo.gwt.shared.go;


import java.util.ArrayList;
import java.util.List;

public class GoAnnotation {


    private Long id ;
    private String goGene; // I think tis is the gene it refers to?
    private String goTerm;
    private String geneRelationship;
    private String evidenceCode;
    private boolean negate = false;
    private List<WithOrFrom> withOrFromList;
    private List<Reference> referenceList;

    public String getEvidenceCode() {
        return evidenceCode;
    }

    public void setEvidenceCode(String evidenceCode) {
        this.evidenceCode = evidenceCode;
    }

    public String getGoGene() {
        return goGene;
    }

    public void setGoGene(String goGene) {
        this.goGene = goGene;
    }

    public String getGoTerm() {
        return goTerm;
    }

    public void setGoTerm(String goTerm) {
        this.goTerm = goTerm;
    }

    public String getGeneRelationship() {
        return geneRelationship;
    }

    public void setGeneRelationship(String geneRelationship) {
        this.geneRelationship = geneRelationship;
    }

    public boolean isNegate() {
        return negate;
    }

    public void setNegate(boolean negate) {
        this.negate = negate;
    }

    public List<Reference> getReferenceList() {
        return referenceList;
    }

    public void setReferenceList(List<Reference> referenceList) {
        this.referenceList = referenceList;
    }

    public List<WithOrFrom> getWithOrFromList() {
        return withOrFromList;
    }

    public void setWithOrFromList(List<WithOrFrom> withOrFromList) {
        this.withOrFromList = withOrFromList;
    }

    public void addWithOrFrom(WithOrFrom withOrFrom) {
        if (withOrFromList == null) {
            withOrFromList = new ArrayList<>();
        }
        withOrFromList.add(withOrFrom);
    }

    public void addReference(Reference reference) {
        if (referenceList == null) {
            referenceList = new ArrayList<>();
        }
        referenceList.add(reference);
    }

    public String getWithOrFromString() {
        StringBuilder withOrFromStringBuilder = new StringBuilder();
        for (WithOrFrom withOrFrom : getWithOrFromList()) {
            withOrFromStringBuilder.append(withOrFrom.getDisplay());
            withOrFromStringBuilder.append(" ");
        }
        return withOrFromStringBuilder.toString();
    }

    public String getReferenceString() {
        StringBuilder referenceStringBuilder = new StringBuilder();
        for (Reference reference : getReferenceList()) {
            referenceStringBuilder.append(reference.getReferenceString());
            referenceStringBuilder.append(" ");
        }
        return referenceStringBuilder.toString();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
