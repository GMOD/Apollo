package org.bbop.apollo.gwt.shared.provenance;


import java.util.ArrayList;
import java.util.List;

public class Provenance {

  private Long id;
  private String feature; // I think this is the gene it refers to? I think the uniquename
  private String field;
  private Reference reference;
  private List<String> noteList;


  private String evidenceCode;
  private String evidenceCodeLabel;
  private List<WithOrFrom> withOrFromList;

  public String getEvidenceCode() {
    return evidenceCode;
  }

  public void setEvidenceCode(String evidenceCode) {
    this.evidenceCode = evidenceCode;
  }

  public String getFeature() {
    return feature;
  }

  public void setFeature(String feature) {
    this.feature = feature;
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

  public String getEvidenceCodeLabel() {
    return evidenceCodeLabel;
  }

  public void setEvidenceCodeLabel(String evidenceCodeLabel) {
    this.evidenceCodeLabel = evidenceCodeLabel;
  }

  public String getField() {
    return field;
  }

  public void setField(String field) {
    this.field = field;
  }

  public List<String> getNoteList() {
    return noteList;
  }

  public void setNoteList(List<String> noteList) {
    this.noteList = noteList;
  }

}
