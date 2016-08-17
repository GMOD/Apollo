package org.bbop.apollo.gwt.client.dto;


import com.google.gwt.core.client.GWT;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;

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
    private List<String> alternateBases = new ArrayList<>();
    private Float minorAlleleFrequency;

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

    public String getAlternateBases() {
        String alternateBasesString = "";
        for (String alt : alternateBases) {
            if (alternateBasesString.equals("")) {
                alternateBasesString = alt;
            }
            else {
                alternateBasesString += "," + alt;
            }
        }
        return alternateBasesString;
    }

    public void setAlternateBases(String alternateBasesString) {
        this.alternateBases = Arrays.asList(alternateBasesString.split(","));
    }

    public void setAlternateBases(ArrayList<String> alternateBases) {
        this.alternateBases = alternateBases;
    }

    public void setAlternateBases(JSONArray array) {
        // TODO: a better way of handling this
        for (int i = 0; i < array.size(); i++) {
            this.alternateBases.add(array.get(i).toString().replaceAll("\"", ""));
        }
    }

    public Float getMinorAlleleFrequency() { return minorAlleleFrequency; }

    public void setMinorAlleleFrequency(String minorAlleleFrequency) { this.minorAlleleFrequency = Float.parseFloat(minorAlleleFrequency); }
}
