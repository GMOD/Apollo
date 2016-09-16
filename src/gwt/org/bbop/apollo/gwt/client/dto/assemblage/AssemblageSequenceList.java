package org.bbop.apollo.gwt.client.dto.assemblage;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.user.client.Window;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by ndunn on 9/30/15.
 */
public class AssemblageSequenceList extends JSONArray {


    public AssemblageSequenceList(){}

    public AssemblageSequenceList(AssemblageSequenceList sequenceList) {
        for(int i = 0 ; sequenceList!=null &&  i < sequenceList.size() ; i++){
            addSequence(sequenceList.getSequence(i));
        }
    }

    public AssemblageSequence getSequence(int i) {
        return new AssemblageSequence(get(i).isObject());
    }

    public AssemblageSequenceList merge(AssemblageSequenceList sequence2) {
        // add all fo the elements between 1 and 2 and put back into 1
        for (int i = 0; i < sequence2.size(); i++) {
            set(size(), sequence2.getSequence(i));
        }
        return this;
    }

    public void addSequence(AssemblageSequence assemblageSequence) {
        set(size(), assemblageSequence);
    }

    public Long getLength() {
        Long finalLength = 0l ;
        for(int i = 0 ; i < size() ; i++){
            finalLength += getSequence(i).getLength() ;
        }
        return finalLength ;
    }

    public String getSummary() {
        String description = "";

        //
        Map<String,Integer> scaffoldFeatureMap = new HashMap<>();

        for(int i = 0 ; i < size() ; i++){
            AssemblageSequence assemblageSequence = getSequence(i);
            String scaffoldName = assemblageSequence.getName();
            Integer featureCount = scaffoldFeatureMap.get(scaffoldName);

            SequenceFeatureInfo sequenceFeatureInfo = assemblageSequence.getFeature();

            featureCount = featureCount==null ? 0 : featureCount ;
            featureCount = sequenceFeatureInfo!=null ? featureCount + 1 : featureCount ;
            scaffoldFeatureMap.put(scaffoldName,featureCount);
        }

        Iterator<String> scaffoldIterator = scaffoldFeatureMap.keySet().iterator();
        while (scaffoldIterator.hasNext()){
            String scaffoldName = scaffoldIterator.next();
            description += scaffoldName;
            Integer featureCount = scaffoldFeatureMap.get(scaffoldName) ;
            if(featureCount>0){
                description += " ("+featureCount + ") ";
            }

            description += scaffoldIterator.hasNext() ? " " : "";
        }

        return description;
    }
}
