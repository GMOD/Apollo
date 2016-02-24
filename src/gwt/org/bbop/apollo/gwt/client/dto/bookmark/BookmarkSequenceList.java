package org.bbop.apollo.gwt.client.dto.bookmark;

import com.google.gwt.json.client.JSONArray;

/**
 * Created by ndunn on 9/30/15.
 */
public class BookmarkSequenceList extends JSONArray {

//    public BookmarkSequenceList(JSONArray sequenceList) {
//        for(int i = 0 ; i < sequenceList.size() ; i++){
//            set(size(),sequenceList.get(i).isObject());
//        }
//    }

    public  BookmarkSequenceList(){}

    public BookmarkSequenceList(BookmarkSequenceList sequenceList) {
        for(int i = 0 ; sequenceList!=null &&  i < sequenceList.size() ; i++){
            addSequence(sequenceList.getSequence(i));
        }
    }

    public BookmarkSequence getSequence(int i) {
        BookmarkSequence bookmarkSequence = new BookmarkSequence(get(i).isObject());
        return bookmarkSequence;
    }

    public BookmarkSequenceList merge(BookmarkSequenceList sequence2) {
        // add all fo the elements between 1 and 2 and put back into 1
        for (int i = 0; i < sequence2.size(); i++) {
            set(size(), sequence2.getSequence(i));
        }
        return this;
    }

    public void addSequence(BookmarkSequence bookmarkSequence) {
        set(size(), bookmarkSequence);
    }
}
