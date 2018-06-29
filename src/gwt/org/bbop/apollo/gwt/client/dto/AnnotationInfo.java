package org.bbop.apollo.gwt.client.dto;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;

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
    private Set<AnnotationInfo> annotationInfoSet = new HashSet<>();
    private String symbol;
    private String description;
    private Integer strand;
    private List<String> noteList = new ArrayList<>();
    private String sequence;
    private Integer phase;
    private String owner;
    private String date;
    private String referenceAllele;
    private ArrayList<AlternateAlleleInfo> alternateAlleles = new ArrayList<AlternateAlleleInfo>();
    private ArrayList<VariantPropertyInfo> variantProperties = new ArrayList<>();

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

    public void setDate(String date) { this.date = date; }

    public String getDate() { return date; }

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
        annotationInfoSet.add(annotationInfo);
    }

    public Set<AnnotationInfo> getAnnotationInfoSet() {
        return annotationInfoSet;
    }

    public void setAnnotationInfoSet(Set<AnnotationInfo> annotationInfoSet) {
        this.annotationInfoSet = annotationInfoSet;
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

    public ArrayList<AlternateAlleleInfo> getAlternateAlleles() {
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
        ArrayList<VariantPropertyInfo> variantPropertyInfoArray = new ArrayList<>();
        int index = 0;
        for (int i = 0; i < variantPropertiesJsonArray.size(); i++) {
            JSONObject variantPropertyJsonObject = variantPropertiesJsonArray.get(i).isObject();
            VariantPropertyInfo variantPropertyInfo = new VariantPropertyInfo(variantPropertyJsonObject);
            variantPropertyInfoArray.add(variantPropertyInfo);
        }
        this.variantProperties = variantPropertyInfoArray;
    }

    public ArrayList<VariantPropertyInfo> getVariantProperties() { return this.variantProperties; }

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
}
