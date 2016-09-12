package org.bbop.apollo.gwt.client.dto;


import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;
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
    private String referenceBases;
    private ArrayList<HashMap<String, String>> alternateAlleles = new ArrayList<HashMap<String, String>>();

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

    public String getReferenceBases() { return referenceBases; }

    public void setReferenceBases(String referenceBasesString) { this.referenceBases = referenceBasesString; }

    public void setAlternateAlleles(JSONArray alternateAllelesArray) {
        ArrayList<HashMap<String, String>> alternateAlleles = new ArrayList<HashMap<String, String>>();
        for (int i = 0; i < alternateAllelesArray.size(); i++) {
            JSONObject alternateAlleleJsonObject = alternateAllelesArray.get(0).isObject();
            String bases = alternateAlleleJsonObject.get(FeatureStringEnum.BASES.getValue()).toString();
            if (alternateAlleleJsonObject.containsKey(FeatureStringEnum.ALLELE_INFO.getValue())) {
                JSONArray alleleInfoJsonArray = alternateAlleleJsonObject.get(FeatureStringEnum.ALLELE_INFO.getValue()).isArray();
                for (int j = 0; j < alleleInfoJsonArray.size(); j++) {
                    JSONObject alleleInfoJsonObject = alleleInfoJsonArray.get(j).isObject();
                    String tag = alleleInfoJsonObject.get(FeatureStringEnum.TAG.getValue()).toString();
                    String value = alleleInfoJsonObject.get(FeatureStringEnum.VALUE.getValue()).toString();
                    HashMap<String, String> alternateAlleleEntity = new HashMap<String, String>();
                    alternateAlleleEntity.put(FeatureStringEnum.BASES.getValue(), bases);
                    alternateAlleleEntity.put(tag, value);
                    if (tag.equals(FeatureStringEnum.ALLELE_FREQUENCY_TAG.getValue())) {
                        String provenance = alleleInfoJsonObject.get(FeatureStringEnum.PROVENANCE.getValue()).toString();
                        alternateAlleleEntity.put(provenance, value);
                    }
                    alternateAlleles.add(alternateAlleleEntity);
                }
            }
        }
        this.alternateAlleles = alternateAlleles;
    }

    public ArrayList<?> getAlternateAlleles() {
        return this.alternateAlleles;
    }

    public JSONArray getAlternateAllelesAsJsonArray() {
        JSONArray alternateAllelesJsonArray = new JSONArray();
        int alternateAllelesJsonArrayIndex = 0;
        for(HashMap<String, String> alternateAllele : this.alternateAlleles) {
            JSONObject alternateAlleleJsonObject = new JSONObject();
            alternateAlleleJsonObject.put(FeatureStringEnum.BASES.getValue(), new JSONString(alternateAllele.get(FeatureStringEnum.BASES.getValue())));
            if (alternateAllele.containsKey(FeatureStringEnum.ALLELE_FREQUENCY_TAG.getValue())) {
                alternateAlleleJsonObject.put(FeatureStringEnum.ALLELE_FREQUENCY_TAG.getValue(), new JSONString(alternateAllele.get(FeatureStringEnum.ALLELE_FREQUENCY_TAG.getValue())));
                alternateAlleleJsonObject.put(FeatureStringEnum.PROVENANCE.getValue(), new JSONString(alternateAllele.get(FeatureStringEnum.PROVENANCE.getValue())));
            }

            JSONArray alleleInfoJsonArray = new JSONArray();
            int alleleInfoJsonArrayIndex = 0;
            for (String key : alternateAllele.keySet()) {
                if (key.equals(FeatureStringEnum.BASES.getValue()) || key.equals(FeatureStringEnum.ALLELE_FREQUENCY_TAG.getValue())) {
                    continue;
                }
                else {
                    JSONObject alleleInfoJsonObject = new JSONObject();
                    alleleInfoJsonObject.put(FeatureStringEnum.TAG.getValue(), new JSONString(key));
                    alleleInfoJsonObject.put(FeatureStringEnum.VALUE.getValue(), new JSONString(alternateAllele.get(key)));
                    alleleInfoJsonArray.set(alleleInfoJsonArrayIndex, alleleInfoJsonObject);
                }
            }
            alternateAllelesJsonArray.set(alternateAllelesJsonArrayIndex, alternateAlleleJsonObject);

        }

        return alternateAllelesJsonArray;
    }
}
