package org.bbop.apollo.gwt.shared.go;


import java.util.ArrayList;
import java.util.List;

public class GoAnnotation {


  private Long id;
  private Aspect aspect;
  private String gene; // I think tis is the gene it refers to? I think the uniquename
  private String goTerm;
  private String goTermLabel;
  private String geneRelationship;
  private String evidenceCode;
  private String evidenceCodeLabel;
  private boolean negate = false;
  private List<WithOrFrom> withOrFromList;
  private List<String> noteList;
  private Reference reference;

  public Aspect getAspect() {
    return aspect;
  }

  public void setAspect(Aspect aspect) {
    this.aspect = aspect;
  }

  public String getEvidenceCode() {
    return evidenceCode;
  }

  public void setEvidenceCode(String evidenceCode) {
    this.evidenceCode = evidenceCode;
  }

  public String getGene() {
    return gene;
  }

  public void setGene(String gene) {
    this.gene = gene;
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

  public List<String> getNoteList() {
    return noteList;
  }

  public void setNoteList(List<String> noteList) {
    this.noteList = noteList;
  }

  public Reference getReference() {
    return reference;
  }

  public void setReference(Reference reference) {
    this.reference = reference;
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

  public void addNote(String note) {
    if (noteList == null) {
      noteList = new ArrayList<>();
    }
    noteList.add(note);
  }

  public String getWithOrFromString() {
    StringBuilder withOrFromStringBuilder = new StringBuilder();
    for (WithOrFrom withOrFrom : getWithOrFromList()) {
      withOrFromStringBuilder.append(withOrFrom.getDisplay());
      withOrFromStringBuilder.append(" ");
    }
    return withOrFromStringBuilder.toString();
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getGoTermLabel() {
    return goTermLabel;
  }

  public void setGoTermLabel(String goTermLabel) {
    this.goTermLabel = goTermLabel;
  }

  public String getEvidenceCodeLabel() {
    return evidenceCodeLabel;
  }

  public void setEvidenceCodeLabel(String evidenceCodeLabel) {
    this.evidenceCodeLabel = evidenceCodeLabel;
  }
}
