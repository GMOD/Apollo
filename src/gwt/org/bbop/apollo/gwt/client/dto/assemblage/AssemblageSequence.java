package org.bbop.apollo.gwt.client.dto.assemblage;

import com.google.gwt.json.client.*;
import org.bbop.apollo.gwt.client.assemblage.FeatureLocationInfo;
import org.bbop.apollo.gwt.client.assemblage.FeatureLocations;
import org.bbop.apollo.gwt.client.dto.SequenceInfo;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;

/**
 * Created by ndunn on 9/30/15.
 */
public class AssemblageSequence extends JSONObject {

    public AssemblageSequence() {
    }

    public AssemblageSequence(JSONObject jsonObject) {
        this.put(FeatureStringEnum.NAME.getValue(), new JSONString(jsonObject.get(FeatureStringEnum.NAME.getValue()).isString().stringValue()));
        if (jsonObject.containsKey(FeatureStringEnum.START.getValue())) {
            this.put(FeatureStringEnum.START.getValue(), new JSONNumber(jsonObject.get(FeatureStringEnum.START.getValue()).isNumber().doubleValue()));
            this.put(FeatureStringEnum.END.getValue(), new JSONNumber(jsonObject.get(FeatureStringEnum.END.getValue()).isNumber().doubleValue()));
        }
        if (jsonObject.containsKey(FeatureStringEnum.REVERSE.getValue())) {
            this.put(FeatureStringEnum.REVERSE.getValue(), jsonObject.get(FeatureStringEnum.REVERSE.getValue()));
        } else {
            this.put(FeatureStringEnum.REVERSE.getValue(), JSONBoolean.getInstance(false));
        }
        if (jsonObject.containsKey(FeatureStringEnum.FEATURE.getValue())) {
            SequenceFeatureInfo sequenceFeatureInfo = new SequenceFeatureInfo(jsonObject.get(FeatureStringEnum.FEATURE.getValue()).isObject());
            this.put(
                    FeatureStringEnum.FEATURE.getValue()
                    , sequenceFeatureInfo
            );
        }
        if (jsonObject.containsKey(FeatureStringEnum.ORGANISM.getValue())) {
            this.put(FeatureStringEnum.ORGANISM.getValue(), jsonObject.get(FeatureStringEnum.ORGANISM.getValue()));
        }
        if (jsonObject.containsKey(FeatureStringEnum.LOCATION.getValue())) {
            this.put(FeatureStringEnum.LOCATION.getValue(), jsonObject.get(FeatureStringEnum.LOCATION.getValue()));
        }
    }

    public AssemblageSequence(SequenceInfo sequenceInfo) {
        setStart(sequenceInfo.getStart());
        setEnd(sequenceInfo.getEnd());
        setName(sequenceInfo.getName());
    }

    public Boolean getReverse(){
        if(containsKey(FeatureStringEnum.REVERSE.getValue()) && get(FeatureStringEnum.REVERSE.getValue()).isBoolean()!=null){
            return get(FeatureStringEnum.REVERSE.getValue()).isBoolean().booleanValue();
        }
        return false ;
    }

    public void setReverse(Boolean reverse){
        put(FeatureStringEnum.REVERSE.getValue(),JSONBoolean.getInstance(reverse));
    }

    public Long getStart() {
        return containsKey(FeatureStringEnum.START.getValue()) ? (long) get(FeatureStringEnum.START.getValue()).isNumber().doubleValue() : null;
    }

    public void setStart(Long start) {
        put(FeatureStringEnum.START.getValue(), new JSONNumber(start));
    }

    public Long getEnd() {
        return containsKey(FeatureStringEnum.END.getValue()) ? (long) get(FeatureStringEnum.END.getValue()).isNumber().doubleValue() : null;
    }

    public void setEnd(Long end) {
        put(FeatureStringEnum.END.getValue(), new JSONNumber(end));
    }


    public String getName() {
        return get(FeatureStringEnum.NAME.getValue()).isString().stringValue();
    }

    public SequenceFeatureInfo getFeature() {
        if (containsKey(FeatureStringEnum.FEATURE.getValue())) {
            return new SequenceFeatureInfo(get(FeatureStringEnum.FEATURE.getValue()).isObject());
        }
        return null;
    }

    public void setFeatureProperties(SequenceFeatureInfo sequenceFeatureInfo) {
        put(FeatureStringEnum.FEATURE.getValue(),sequenceFeatureInfo);
        put(FeatureStringEnum.START.getValue(),new JSONNumber(sequenceFeatureInfo.getStart()));
        put(FeatureStringEnum.END.getValue(),new JSONNumber(sequenceFeatureInfo.getEnd()));
    }

    public void setName(String groupName) {
        put(FeatureStringEnum.NAME.getValue(), new JSONString(groupName));
    }

    public Long getLength() {
        return getEnd() - getStart();
    }

    public AssemblageSequence deepCopy() {

        AssemblageSequence assemblageSequence = new AssemblageSequence();
        for(String key :this.keySet()){
            assemblageSequence.put(key,this.get(key));
        }

        return assemblageSequence;
    }

    public FeatureLocations getLocation(){
        JSONArray jsonArray = get(FeatureStringEnum.LOCATION.getValue()).isArray();
        if(jsonArray instanceof FeatureLocations){
            return (FeatureLocations) jsonArray;
        }
        FeatureLocations featureLocations = new FeatureLocations();
        for(int i = 0 ; i < jsonArray.size() ; i++){
            featureLocations.set(i,new FeatureLocationInfo(jsonArray.get(i).isObject()));
        }
        return featureLocations;
    }

    public void setLocation(FeatureLocations featureLocations){
        put(FeatureStringEnum.LOCATION.getValue(),featureLocations);
    }

    public boolean hasLocation() {
        return containsKey(FeatureStringEnum.LOCATION.getValue());
    }

    public void flip(){
        boolean original = getReverse()==null ? false : getReverse();
        setReverse(!original);
    }
}
