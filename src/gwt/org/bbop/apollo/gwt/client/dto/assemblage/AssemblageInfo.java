package org.bbop.apollo.gwt.client.dto.assemblage;

import com.google.gwt.json.client.JSONObject;
import org.bbop.apollo.gwt.client.dto.SequenceInfo;
import org.bbop.apollo.gwt.client.projection.ProjectionService;
import org.bbop.apollo.gwt.shared.projection.MultiSequenceProjection;

import java.util.Set;

/**
 * Created by Nathan Dunn on 12/18/14.
 */
public class AssemblageInfo implements Comparable<AssemblageInfo> {

    private AssemblageSequenceList sequenceList;

    private String name;
    private String type;
    private Integer padding;
    private JSONObject payload;
    private Long id;
    private String organismName;
    private Long start;
    private Long end;
    private Long startBp ; // preference values
    private Long endBp ;// preference values

    private MultiSequenceProjection projection; // internal projection

    @Override
    public int compareTo(AssemblageInfo o) {
        return getDescription().compareTo(o.getDescription());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public JSONObject getPayload() {
        return payload;
    }

    public void setPayload(JSONObject payload) {
        this.payload = payload;
    }

    public AssemblageSequenceList getSequenceList() {
        return sequenceList;
    }

    public void setSequenceList(AssemblageSequenceList sequenceList) {
        this.sequenceList = sequenceList;
    }

    public Integer getPadding() {
        return padding;
    }

    public String getOrganismName() {
        return organismName;
    }

    public void setOrganismName(String organismName) {
        this.organismName = organismName;
    }

    public void setPadding(Integer padding) {


        this.padding = padding;
    }

    public AssemblageInfo copy() {
        AssemblageInfo assemblageInfo = new AssemblageInfo();
        assemblageInfo.setPadding(padding);
        assemblageInfo.setPayload(payload);
        assemblageInfo.setSequenceList(sequenceList);
        assemblageInfo.setType(type);
        assemblageInfo.setOrganismName(organismName);
        return assemblageInfo;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public AssemblageInfo merge(AssemblageInfo assemblageInfo) {
        AssemblageInfo assemblageInfoReturn = this.copy();

        assemblageInfoReturn.setPadding(assemblageInfo.getPadding() > assemblageInfoReturn.getPadding() ? assemblageInfo.getPadding() : assemblageInfoReturn.getPadding());
        // TODO: set payload when we have that
        if (assemblageInfoReturn.getPayload() == null) {
            assemblageInfoReturn.setPayload(assemblageInfo.getPayload());
        } else if (assemblageInfo.getPayload() == null) {
//            assemblageInfoReturn.setPayload(assemblageInfo.getPayload());
        }
        // if neither is null
        else {
            // TODO: merge teh payload
        }
        AssemblageSequenceList assemblageSequenceList = assemblageInfo.getSequenceList();
        for (int i = 0; i < assemblageSequenceList.size(); i++) {
            assemblageInfoReturn.getSequenceList().addSequence(assemblageSequenceList.getSequence(i));
        }

        return assemblageInfoReturn;
    }

    public Long getStart() {
        return start;
    }

    public void setStart(Long start) {
        this.start = start;
    }

    public Long getEnd() {
        return end;
    }

    public void setEnd(Long end) {
        this.end = end;
    }

    public Long getStartBp() {
        return startBp;
    }

    public void setStartBp(Long startBp) {
        this.startBp = startBp;
    }

    public void setStartBp(Double startBp ) {
        if(startBp!=null){
            this.startBp = startBp.longValue();
        }
    }

    public Long getEndBp() {
        return endBp;
    }

    public void setEndBp(Long endBp) {
        this.endBp = endBp;
    }

    public void setEndBp(Double endBp) {
        if(endBp!=null){
            this.endBp = endBp.longValue();
        }
    }

    public AssemblageInfo addSequenceInfoSet(Set<SequenceInfo> sequenceInfoSet) {
        if(sequenceList==null){
            sequenceList = new AssemblageSequenceList();
        }
        for(SequenceInfo sequenceInfo: sequenceInfoSet){
            AssemblageSequence assemblageSequence = new AssemblageSequence(sequenceInfo);
            sequenceList.addSequence(assemblageSequence);
        }
        return this;
    }

    /**
     * Basically merge the sequenceInfoSet
     * @param assemblageInfo
     * @return
     */
    public AssemblageInfo addAssemblageToEnd(AssemblageInfo assemblageInfo) {
        AssemblageSequenceList assemblageSequenceList = assemblageInfo.getSequenceList();
        for(int i = 0; i < assemblageSequenceList.size() ; i++){
            this.getSequenceList().addSequence(assemblageSequenceList.getSequence(i));
        }

        return this;
    }

    public Long getLength(){
        return sequenceList == null ? null : sequenceList.getLength();
    }

    public String getSummary() {
        return sequenceList == null ? "None" : sequenceList.getSummary();
    }

    public String getDescription() {
        String name = "";
        for (int i = 0; sequenceList != null && i < sequenceList.size(); i++) {
            AssemblageSequence sequenceObject = sequenceList.getSequence(i);

            SequenceFeatureInfo sequenceFeatureInfo = sequenceObject.getFeature();
            if (sequenceFeatureInfo != null && sequenceFeatureInfo.getName()!=null) {
                name += sequenceFeatureInfo.getName();
                name += " (";
            }
            name += sequenceObject.getName();

            if (sequenceFeatureInfo != null) {
                name += ")";
            }
            if (i < sequenceList.size() - 1) {
                name += "::";
            }
        }
        return name;
    }
}
