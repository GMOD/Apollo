package org.bbop.apollo.gwt.client.dto;

import java.util.*;

/**
 * Created by Nathan Dunn on 1/27/15.
 *
 * TODO: add a SequenceInfo object instead of string
 */
public class AnnotationInfo {
    private String uniqueName;
    private String name;
    private String type;
    private Long min;
    private Long max;
    private Set<AnnotationInfo> annotationInfoSet = new HashSet<>();
    private String symbol;
    private String description;
    private Integer strand;
    private List<String> noteList = new ArrayList<>();
    private String sequence;
    private Integer phase;
    private String owner;
    private String date;

    public String getOwner() {
        return owner;
    }

    public String getOwnerString() {
        if (owner == null) {
            return "";
        }
        return owner;
    }

    public Long getMin() {
        return min;
    }

    public void setMin(Long min) {
        this.min = min;
    }

    public Long getMax() {
        return max;
    }

    public void setMax(Long max) {
        this.max = max;
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

    public Long getLength() {
        if (min != null && max != null) {
            return max - min;
        }
        return -1l;
    }

    public void setDate(String date) { this.date = date; }

    public String getDate() { return date; }

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
}
