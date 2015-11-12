package org.bbop.apollo.gwt.client.dto;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import org.bbop.apollo.gwt.client.dto.bookmark.BookmarkInfo;
import org.bbop.apollo.gwt.client.dto.bookmark.BookmarkInfoConverter;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;

import java.util.List;

/**
 * Created by Nathan Dunn on 4/17/15.
 */
public class AppStateInfo implements HasJSON{

    private OrganismInfo currentOrganism ;
    private List<OrganismInfo> organismList ;
//    private SequenceInfo currentSequence ;
    private Integer currentStartBp;
    private Integer currentEndBp;
    private BookmarkInfo currentBookmark;

    public OrganismInfo getCurrentOrganism() {
        return currentOrganism;
    }

    public void setCurrentOrganism(OrganismInfo currentOrganism) {
        this.currentOrganism = currentOrganism;
    }

    public List<OrganismInfo> getOrganismList() {
        return organismList;
    }

    public void setOrganismList(List<OrganismInfo> organismList) {
        this.organismList = organismList;
    }

//    public SequenceInfo getCurrentSequence() {
//        return currentSequence;
//    }

//    public void setCurrentSequence(SequenceInfo currentSequence) {
//        this.currentSequence = currentSequence;
//    }


    @Override
    public JSONObject toJSON() {
        JSONObject returnObject = new JSONObject();

        if(currentOrganism!=null){
            returnObject.put(FeatureStringEnum.CURRENT_ORGANISM.getValue(),currentOrganism.toJSON());
        }

        if(currentBookmark!=null){
            returnObject.put(FeatureStringEnum.CURRENT_BOOKMARK.getValue(), BookmarkInfoConverter.convertBookmarkInfoToJSONObject(currentBookmark));
        }
//        if(currentSequence!=null){
//            returnObject.put("currentSequence",currentSequence.toJSON());
//        }
//        if(currentSequenceList!=null){
//            JSONArray sequenceListArray = new JSONArray();
//            for(SequenceInfo sequenceInfo : currentSequenceList){
//                sequenceListArray.set(sequenceListArray.size(),sequenceInfo.toJSON());
//            }
//            returnObject.put("currentSequenceList",sequenceListArray);
//        }
        if(organismList!=null){
            JSONArray organismListArray = new JSONArray();
            for(OrganismInfo organismInfo : organismList){
                organismListArray.set(organismListArray.size(),organismInfo.toJSON());
            }
            returnObject.put("organismList",organismListArray);
        }
        if(currentStartBp!=null){
            returnObject.put("currentStartBp", new JSONNumber(currentStartBp));
        }
        if(currentEndBp!=null){
            returnObject.put("currentEndBp", new JSONNumber(currentEndBp));
        }


        return returnObject ;
    }

    public Integer getCurrentStartBp() {
        return currentStartBp;
    }

    public void setCurrentStartBp(Integer currentStartBp) {
        this.currentStartBp = currentStartBp;
    }

    public Integer getCurrentEndBp() {
        return currentEndBp;
    }

    public void setCurrentEndBp(Integer currentEndBp) {
        this.currentEndBp = currentEndBp;
    }

    public void setCurrentBookmark(BookmarkInfo bookmark) {
        this.currentBookmark = bookmark;
    }

    public BookmarkInfo getCurrentBookmark() {
        return currentBookmark;
    }
}
