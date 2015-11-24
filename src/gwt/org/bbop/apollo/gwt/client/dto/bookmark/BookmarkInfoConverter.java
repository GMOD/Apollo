package org.bbop.apollo.gwt.client.dto.bookmark;

import com.google.gwt.core.client.GWT;
import com.google.gwt.json.client.*;
import com.google.gwt.user.client.Window;
import org.bbop.apollo.gwt.shared.BookmarkKeyEnum;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;

import java.awt.print.Book;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Created by nathandunn on 10/1/15.
 */
public class BookmarkInfoConverter {

    public static JSONObject convertBookmarkInfoToJSONObject(BookmarkInfo bookmarkInfo){
        JSONObject jsonObject = new JSONObject();

        if(bookmarkInfo.getId()!=null){
            jsonObject.put("id",new JSONNumber(bookmarkInfo.getId()));
        }
        jsonObject.put("name",new JSONString(bookmarkInfo.getName()));
        if(bookmarkInfo.getType()!=null) {
            jsonObject.put("type", new JSONString(bookmarkInfo.getType()));
        }
        if(bookmarkInfo.getPadding()!=null) {
            jsonObject.put("padding", new JSONNumber(bookmarkInfo.getPadding()));
        }
        if(bookmarkInfo.getStart()!=null) {
            jsonObject.put("start", new JSONNumber(bookmarkInfo.getStart()));
        }
        if(bookmarkInfo.getEnd()!=null) {
            jsonObject.put("end", new JSONNumber(bookmarkInfo.getEnd()));
        }
        jsonObject.put("sequenceList",bookmarkInfo.getSequenceList());
        if(bookmarkInfo.getPayload()!=null) {
            jsonObject.put("payload", bookmarkInfo.getPayload());
        }

        return jsonObject;
    }



    public static BookmarkInfo convertJSONObjectToBookmarkInfo(JSONObject jsonObject) {
        BookmarkInfo bookmarkInfo = new BookmarkInfo() ;
        if(jsonObject.containsKey(FeatureStringEnum.ID.getValue())){
            bookmarkInfo.setId((long) jsonObject.get(FeatureStringEnum.ID.getValue()).isNumber().doubleValue());
        }
        bookmarkInfo.setPadding( (int) jsonObject.get("padding").isNumber().doubleValue());
        if(jsonObject.containsKey("payload")) {
            bookmarkInfo.setPayload(jsonObject.get("payload").isObject());
        }
        if(jsonObject.containsKey("start")) {
            bookmarkInfo.setStart((int) jsonObject.get("start").isNumber().doubleValue());
            bookmarkInfo.setEnd((int) jsonObject.get("end").isNumber().doubleValue());
        }

        JSONArray sequenceListArray = jsonObject.get("sequenceList").isArray();
        // some weird stuff here
        if(sequenceListArray==null){
            String sequenceArrayString = jsonObject.get("sequenceList").isString().stringValue() ;
            sequenceArrayString = sequenceArrayString.replaceAll("\\\\","");
            sequenceListArray = JSONParser.parseStrict(sequenceArrayString).isArray();
        }
        BookmarkSequenceList bookmarkSequenceList = convertJSONArrayToSequenceList(sequenceListArray);
        bookmarkInfo.setSequenceList(bookmarkSequenceList);

        return bookmarkInfo ;
    }

    private static BookmarkSequenceList convertJSONArrayToSequenceList(JSONArray sequenceListArray) {
        BookmarkSequenceList bookmarkSequenceList = new BookmarkSequenceList();
        for(int i = 0 ; i < sequenceListArray.size() ; i++){
            BookmarkSequence bookmarkSequence = new BookmarkSequence(sequenceListArray.get(i).isObject());
            bookmarkSequenceList.addSequence(bookmarkSequence);
        }
        return bookmarkSequenceList;
    }

    // TODO:
    public static JSONArray convertBookmarkInfoToJSONArray(BookmarkInfo... selectedSet) {
        JSONArray jsonArray = new JSONArray();

        for(BookmarkInfo bookmarkInfo : selectedSet){
            jsonArray.set(jsonArray.size(),convertBookmarkInfoToJSONObject(bookmarkInfo));
        }

        return jsonArray;
    }

    public static List<BookmarkInfo> convertFromJsonArray (JSONArray bookmarkList) {
        List<BookmarkInfo> bookmarkInfoArrayList = new ArrayList<>();
        for (int i = 0; bookmarkList != null && i < bookmarkList.size(); i++) {
            bookmarkInfoArrayList.add(convertJSONObjectToBookmarkInfo(bookmarkList.get(i).isObject()));
        }
        return bookmarkInfoArrayList;
    }
}
