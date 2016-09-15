package org.bbop.apollo.gwt.client.dto.assemblage;

import com.google.gwt.json.client.JSONArray;

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
        AssemblageSequence assemblageSequence = new AssemblageSequence(get(i).isObject());
        return assemblageSequence;
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
}
