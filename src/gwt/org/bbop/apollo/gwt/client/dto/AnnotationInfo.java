package org.bbop.apollo.gwt.client.dto;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import org.bbop.apollo.gwt.shared.go.GoAnnotation;

import java.util.*;

/**
 * Created by ndunn on 1/27/15.
 */
public class AnnotationInfo {
    private String uniqueName;
    private String name;
    private String type;
    private Integer min;
    private Integer max;
    private Boolean partialMin = false;
    private Boolean partialMax = false;
    private Boolean obsolete = false;
    private Set<AnnotationInfo> childAnnotations = new HashSet<>(); // children
    private List<GoAnnotation> goAnnotations = new ArrayList<>(); // go annotations
    private String symbol;
    private String description;
    private Integer strand;
    private List<String> noteList = new ArrayList<>();
    private List<DbXrefInfo> dbXrefList = new ArrayList<>();
    private String sequence;
    private Integer phase;
    private String owner;
    private String dateLastModified;
    private String dateCreated;
    private String referenceAllele;
    private List<AlternateAlleleInfo> alternateAlleles = new ArrayList<AlternateAlleleInfo>();
    private List<VariantPropertyInfo> variantProperties = new ArrayList<>();
    private List<CommentInfo> commentList = new ArrayList<>();
    private List<AttributeInfo> attributeList= new ArrayList<>();
    private String status;
    private String synonyms;

    public String getOwner() {
        return owner;
    }

    public String getOwnerString() {
        if (owner == null) {
            return "";
        }
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public Integer getLength() {
        if (min != null && max != null) {
            return max - min;
        }
        return -1;
    }

    public void setDateLastModified(String dateLastModified) { this.dateLastModified = dateLastModified; }

    public String getDateLastModified() { return dateLastModified; }

    public Integer getMin() {
        return min;
    }

    public void setMin(Integer min) {
        this.min = min;
    }

    public Integer getMax() {
        return max;
    }

    public void setMax(Integer max) {
        this.max = max;
    }

    public String getUniqueName() {
        return uniqueName;
    }

    public void setUniqueName(String uniqueName) {
        this.uniqueName = uniqueName;
    }

    public void addChildAnnotation(AnnotationInfo annotationInfo) {
        childAnnotations.add(annotationInfo);
    }

    public Set<AnnotationInfo> getChildAnnotations() {
        return childAnnotations;
    }

    public void setChildAnnotations(Set<AnnotationInfo> childAnnotations) {
        this.childAnnotations = childAnnotations;
    }

    public List<GoAnnotation> getGoAnnotations() {
        return goAnnotations;
    }

    public void setGoAnnotations(List<GoAnnotation> goAnnotations) {
        this.goAnnotations = goAnnotations;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public Integer getStrand() {
        return strand;
    }

    public void setStrand(Integer strand) {
        this.strand = strand;
    }

    public List<String> getNoteList() {
        return noteList;
    }

    public void setNoteList(List<String> noteList) {
        this.noteList = noteList;
    }

    public void setSequence(String sequence) {
        this.sequence = sequence;
    }

    public String getSequence() {
        return sequence;
    }

    public Integer getPhase() {
        return phase;
    }

    public void setPhase(Integer phase) {
        this.phase = phase;
    }

    public String getReferenceAllele() { return referenceAllele; }

    public void setReferenceAllele(String referenceAlleleString) { this.referenceAllele = referenceAlleleString; }

    public void setAlternateAlleles(JSONArray alternateAllelesArray) {
        for (int i = 0; i < alternateAllelesArray.size(); i++) {
            JSONObject alternateAlleleJsonObject = alternateAllelesArray.get(i).isObject();
            AlternateAlleleInfo alternateAlleleInfo = new AlternateAlleleInfo(alternateAlleleJsonObject);
            this.alternateAlleles.add(alternateAlleleInfo);
        }
    }

    public List<AlternateAlleleInfo> getAlternateAlleles() {
        return this.alternateAlleles;
    }

    public JSONArray getAlternateAllelesAsJsonArray() {
        JSONArray alternateAllelesJsonArray = new JSONArray();
        int alternateAllelesJsonArrayIndex = 0;
        for (AlternateAlleleInfo alternateAllele : this.alternateAlleles) {
            JSONObject alternateAlleleJsonObject = alternateAllele.convertToJsonObject();
            alternateAllelesJsonArray.set(alternateAllelesJsonArrayIndex, alternateAlleleJsonObject);
            alternateAllelesJsonArrayIndex++;
        }
        return alternateAllelesJsonArray;
    }

    public void setVariantProperties(JSONArray variantPropertiesJsonArray) {
        List<VariantPropertyInfo> variantPropertyInfoArray = new ArrayList<>();
        for (int i = 0; i < variantPropertiesJsonArray.size(); i++) {
            JSONObject variantPropertyJsonObject = variantPropertiesJsonArray.get(i).isObject();
            VariantPropertyInfo variantPropertyInfo = new VariantPropertyInfo(variantPropertyJsonObject);
            variantPropertyInfoArray.add(variantPropertyInfo);
        }
        this.variantProperties = variantPropertyInfoArray;
    }

    public List<VariantPropertyInfo> getVariantProperties() { return this.variantProperties; }

    public JSONArray getVariantPropertiesAsJsonArray() {
        JSONArray variantPropertiesJsonArray = new JSONArray();
        int index = 0;
        for (VariantPropertyInfo variantPropertyInfo : this.variantProperties) {
            JSONObject variantPropertyJsonObject = variantPropertyInfo.convertToJsonObject();
            variantPropertiesJsonArray.set(0, variantPropertyJsonObject);
            index++;
        }
        return variantPropertiesJsonArray;
    }

    public String getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(String dateCreated) {
        this.dateCreated = dateCreated;
    }

    public List<DbXrefInfo> getDbXrefList() {
        return dbXrefList;
    }

    public void setDbXrefList(List<DbXrefInfo> dbXrefList) {
        this.dbXrefList = dbXrefList;
    }

    public List<CommentInfo> getCommentList() {
        return commentList;
    }

    public void setCommentList(List<CommentInfo> commentList) {
        this.commentList = commentList;
    }

    public List<AttributeInfo> getAttributeList() {
        return attributeList;
    }

    public void setAttributeList(List<AttributeInfo> attributeList) {
        this.attributeList = attributeList;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setSynonyms(String synonyms) {
        this.synonyms = synonyms;
    }

    public String getSynonyms() {
        return synonyms;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AnnotationInfo that = (AnnotationInfo) o;
        return uniqueName.equals(that.uniqueName) &&
          name.equals(that.name) &&
          type.equals(that.type) &&
          min.equals(that.min) &&
          max.equals(that.max) &&
          Objects.equals(childAnnotations, that.childAnnotations) &&
          Objects.equals(goAnnotations, that.goAnnotations) &&
          Objects.equals(symbol, that.symbol) &&
          Objects.equals(description, that.description) &&
          strand.equals(that.strand) &&
          Objects.equals(noteList, that.noteList) &&
          Objects.equals(dbXrefList, that.dbXrefList) &&
          sequence.equals(that.sequence) &&
          Objects.equals(phase, that.phase) &&
          Objects.equals(owner, that.owner) &&
          Objects.equals(dateLastModified, that.dateLastModified) &&
          Objects.equals(dateCreated, that.dateCreated) &&
          Objects.equals(referenceAllele, that.referenceAllele) &&
          Objects.equals(alternateAlleles, that.alternateAlleles) &&
          Objects.equals(variantProperties, that.variantProperties) &&
          Objects.equals(commentList, that.commentList) &&
          Objects.equals(attributeList, that.attributeList) &&
          Objects.equals(status, that.status) &&
          Objects.equals(synonyms, that.synonyms);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uniqueName, name, type, min, max, childAnnotations, goAnnotations, symbol, description, strand, noteList, dbXrefList, sequence, phase, owner, dateLastModified, dateCreated, referenceAllele, alternateAlleles, variantProperties, commentList, attributeList, status, synonyms);
    }

    public Boolean getPartialMin() {
        return partialMin;
    }

    public void setPartialMin(Boolean partialMin) {
        this.partialMin = partialMin;
    }

    public Boolean getPartialMax() {
        return partialMax;
    }

    public void setPartialMax(Boolean partialMax) {
        this.partialMax = partialMax;
    }


    public Boolean getObsolete() {
        return obsolete;
    }

    public void setObsolete(Boolean obsolete) {
        this.obsolete = obsolete;
    }

}
