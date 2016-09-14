package org.bbop.apollo.gwt.client.dto.bookmark;

import com.google.gwt.json.client.*;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nathandunn on 10/1/15.
 */
public class BookmarkInfoConverter {

    public static JSONObject convertBookmarkInfoToJSONObject(BookmarkInfo bookmarkInfo) {
        JSONObject jsonObject = new JSONObject();

        if (bookmarkInfo.getId() != null) {
            jsonObject.put(FeatureStringEnum.ID.getValue(), new JSONNumber(bookmarkInfo.getId()));
        }
        jsonObject.put(FeatureStringEnum.NAME.getValue(), new JSONString(bookmarkInfo.getName()));
        if (bookmarkInfo.getType() != null) {
            jsonObject.put(FeatureStringEnum.TYPE.getValue(), new JSONString(bookmarkInfo.getType()));
        }
        if (bookmarkInfo.getPadding() != null) {
            jsonObject.put("padding", new JSONNumber(bookmarkInfo.getPadding()));
        }
        if (bookmarkInfo.getStart() != null) {
            jsonObject.put(FeatureStringEnum.START.getValue(), new JSONNumber(bookmarkInfo.getStart()));
        }
        if (bookmarkInfo.getEnd() != null) {
            jsonObject.put(FeatureStringEnum.END.getValue(), new JSONNumber(bookmarkInfo.getEnd()));
        }
        jsonObject.put(FeatureStringEnum.SEQUENCE_LIST.getValue(), bookmarkInfo.getSequenceList());
        if (bookmarkInfo.getPayload() != null) {
            jsonObject.put("payload", bookmarkInfo.getPayload());
        }

        return jsonObject;
    }


    public static BookmarkInfo convertJSONObjectToBookmarkInfo(JSONObject jsonObject) {
        BookmarkInfo bookmarkInfo = new BookmarkInfo();
        if (jsonObject.containsKey(FeatureStringEnum.ID.getValue())) {
            bookmarkInfo.setId((long) jsonObject.get(FeatureStringEnum.ID.getValue()).isNumber().doubleValue());
        }
        if (jsonObject.containsKey("padding")) {
            bookmarkInfo.setPadding((int) jsonObject.get("padding").isNumber().doubleValue());
        }
        else{
            bookmarkInfo.setPadding(0);
        }
        if (jsonObject.containsKey("payload")) {
            bookmarkInfo.setPayload(jsonObject.get("payload").isObject());
        }
        if (jsonObject.containsKey(FeatureStringEnum.START.getValue())) {
            bookmarkInfo.setStart((long) jsonObject.get(FeatureStringEnum.START.getValue()).isNumber().doubleValue());
            bookmarkInfo.setEnd((long) jsonObject.get(FeatureStringEnum.END.getValue()).isNumber().doubleValue());
        }

        JSONArray sequenceListArray = jsonObject.get("sequenceList").isArray();
        // some weird stuff here
        if (sequenceListArray == null) {
            String sequenceArrayString = jsonObject.get("sequenceList").isString().stringValue();
            sequenceArrayString = sequenceArrayString.replaceAll("\\\\", "");
            sequenceListArray = JSONParser.parseStrict(sequenceArrayString).isArray();
        }
        BookmarkSequenceList bookmarkSequenceList = convertJSONArrayToSequenceList(sequenceListArray);
        bookmarkInfo.setSequenceList(bookmarkSequenceList);

        return bookmarkInfo;
    }

    private static BookmarkSequenceList convertJSONArrayToSequenceList(JSONArray sequenceListArray) {
//        BookmarkSequenceList bookmarkSequenceList = new BookmarkSequenceList(sequenceListArray);
        BookmarkSequenceList bookmarkSequenceList = new BookmarkSequenceList();
        for (int i = 0; i < sequenceListArray.size(); i++) {
            BookmarkSequence bookmarkSequence = new BookmarkSequence(sequenceListArray.get(i).isObject());
            bookmarkSequenceList.addSequence(bookmarkSequence);
        }
        return bookmarkSequenceList;
    }

    // TODO:
    public static JSONArray convertBookmarkInfoToJSONArray(BookmarkInfo... selectedSet) {
        JSONArray jsonArray = new JSONArray();

        for (BookmarkInfo bookmarkInfo : selectedSet) {
            jsonArray.set(jsonArray.size(), convertBookmarkInfoToJSONObject(bookmarkInfo));
        }

        return jsonArray;
    }

    public static List<BookmarkInfo> convertFromJsonArray(JSONArray bookmarkList) {
        List<BookmarkInfo> bookmarkInfoArrayList = new ArrayList<>();
        for (int i = 0; bookmarkList != null && i < bookmarkList.size(); i++) {
            bookmarkInfoArrayList.add(convertJSONObjectToBookmarkInfo(bookmarkList.get(i).isObject()));
        }
        return bookmarkInfoArrayList;
    }
}
