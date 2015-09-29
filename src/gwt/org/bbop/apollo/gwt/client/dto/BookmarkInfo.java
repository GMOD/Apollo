package org.bbop.apollo.gwt.client.dto;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.user.client.Window;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;

import java.util.List;

/**
 * Created by Nathan Dunn on 12/18/14.
 */
public class BookmarkInfo implements Comparable<BookmarkInfo> {

//    private String name;
    // should be sequence: All . .. or sequence: Feature . . . order is the sequence order
    // features can not be re-ordered
    private JSONArray sequenceList;
    private String type;
//    private List<String> features;
    private Integer padding ;
    private JSONObject payload ;

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
            JSONObject sequenceObject = sequenceList.get(i).isObject();

            name += sequenceObject.get(FeatureStringEnum.NAME.getValue()).isString().stringValue();
            if(sequenceObject.containsKey(FeatureStringEnum.FEATURES.getValue())){
                name += "(";

                JSONArray featuresArray = sequenceObject.get(FeatureStringEnum.FEATURES.getValue()).isArray();
                for(int j = 0 ; j < featuresArray.size() ; j++){
                    name += featuresArray.get(j).isObject().get(FeatureStringEnum.NAME.getValue()).isString().stringValue();
                    if(j < featuresArray.size()-1){
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

    public JSONArray getSequenceList() {
        return sequenceList;
    }

    public void setSequenceList(JSONArray sequenceList) {
        this.sequenceList = sequenceList;
    }

    public Integer getPadding() {
        return padding;
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
        return bookmarkInfo;
    }
}
