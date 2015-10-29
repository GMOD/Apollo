package org.bbop.apollo.gwt.client.dto.bookmark;

import com.google.gwt.core.client.GWT;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.Window;

/**
 * Created by Nathan Dunn on 12/18/14.
 */
public class BookmarkInfo implements Comparable<BookmarkInfo> {

//    private String name;
    // should be sequence: All . .. or sequence: Feature . . . order is the sequence order
    // features can not be re-ordered
//    private JSONArray sequenceList;
    private BookmarkSequenceList sequenceList ;

    private String type;
//    private List<String> features;
    private Integer padding ;
    private JSONObject payload ;
    private Long id;
    private String organismName ;
    private Integer start ;
    private Integer end ;

    public BookmarkInfo(){}

    @Override
    public int compareTo(BookmarkInfo o) {
        return getName().compareTo(o.getName());
    }
//
//    public String getName() {
//        return name;
//    }
//
//    public void setName(String name) {
//        this.name = name;
//    }
    public String getName(){
        String name = "" ;
        for(int i = 0 ; i < sequenceList.size() ; i++){
            BookmarkSequence sequenceObject = sequenceList.getSequence(i);

            name += sequenceObject.getName();
            SequenceFeatureList sequenceFeatureList = sequenceObject.getFeatures();

            if(sequenceFeatureList !=null ){
                name += "(";
                for(int j = 0 ; j < sequenceFeatureList.size() ; j++){
                    SequenceFeatureInfo sequenceFeatureInfo = sequenceFeatureList.getFeature(j);
                    name += sequenceFeatureInfo.getName();
                    if(j < sequenceFeatureList.size()-1){
                        name += "," ;
                    }
                }

                name += ")";
            }
            if(i < sequenceList.size()-1){
                name += "::";
            }
        }
        return name ;
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

    public BookmarkSequenceList getSequenceList() {
        return sequenceList;
    }

    public void setSequenceList(BookmarkSequenceList sequenceList) {
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

    public BookmarkInfo copy() {
        BookmarkInfo bookmarkInfo = new BookmarkInfo();
        bookmarkInfo.setPadding(padding);
        bookmarkInfo.setPayload(payload);
        bookmarkInfo.setSequenceList(sequenceList);
        bookmarkInfo.setType(type);
        bookmarkInfo.setOrganismName(organismName);
        return bookmarkInfo;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public BookmarkInfo merge(BookmarkInfo bookmarkInfo) {
        BookmarkInfo bookmarkInfoReturn = this.copy();

        bookmarkInfoReturn.setPadding( bookmarkInfo.getPadding() > bookmarkInfoReturn.getPadding() ? bookmarkInfo.getPadding() : bookmarkInfoReturn.getPadding());
        // TODO: set payload when we have that
        if(bookmarkInfoReturn.getPayload()==null){
            bookmarkInfoReturn.setPayload(bookmarkInfo.getPayload());
        }
        else
        if(bookmarkInfo.getPayload()==null){
//            bookmarkInfoReturn.setPayload(bookmarkInfo.getPayload());
        }
        // if neither is null
        else{
            // TODO: merge teh payload
        }
        // organism should be the same . . .
//        bookmarkInfoReturn.setPayload();
//        bookmarkInfo.setType(); // just take the one I guess
        BookmarkSequenceList bookmarkSequenceList = bookmarkInfo.getSequenceList();
        for(int i = 0 ; i < bookmarkSequenceList.size() ; i++){
            bookmarkInfoReturn.getSequenceList().addSequence(bookmarkSequenceList.getSequence(i));
        }

        return bookmarkInfoReturn;
    }

    public Integer getStart() {
        return start;
    }

    public void setStart(Integer start) {
        this.start = start;
    }

    public Integer getEnd() {
        return end;
    }

    public void setEnd(Integer end) {
        this.end = end;
    }
}
