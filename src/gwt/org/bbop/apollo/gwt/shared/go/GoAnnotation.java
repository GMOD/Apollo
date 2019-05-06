package org.bbop.apollo.gwt.shared.go;


import java.util.ArrayList;
import java.util.List;

public class GoAnnotation {


    private Long id ;
    private GoGene goGene; // I think tis is the gene it refers to?
    private GoTerm goTerm;
    private EvidenceCode evidenceCode;
    private List<Qualifier> qualifierList;
    private List<WithOrFrom> withOrFromList;
    private List<Reference> referenceList;

    public GoGene getGoGene() {
        return goGene;
    }

    public void setGoGene(GoGene goGene) {
        this.goGene = goGene;
    }

    public GoTerm getGoTerm() {
        return goTerm;
    }

    public void setGoTerm(GoTerm goTerm) {
        this.goTerm = goTerm;
    }

    public EvidenceCode getEvidenceCode() {
        return evidenceCode;
    }

    public void setEvidenceCode(EvidenceCode evidenceCode) {
        this.evidenceCode = evidenceCode;
    }

    public List<Qualifier> getQualifierList() {
        return qualifierList;
    }

    public void setQualifierList(List<Qualifier> qualifierList) {
        this.qualifierList = qualifierList;
    }

    public List<Reference> getReferenceList() {
        return referenceList;
    }

    public void setReferenceList(List<Reference> referenceList) {
        this.referenceList = referenceList;
    }

    public void addQualifier(Qualifier qualifier) {
        if (qualifierList == null) {
            qualifierList = new ArrayList<Qualifier>();
        }
        qualifierList.add(qualifier);
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
