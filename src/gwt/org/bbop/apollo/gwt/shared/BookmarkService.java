package org.bbop.apollo.gwt.shared;

import java.util.List;

/**
 * Created by nathandunn on 10/5/15.
 */
public class BookmarkService {

    private static BookmarkService instance = null ;

    private BookmarkService(){}

    public static BookmarkService getInstance(){
        if(instance==null){
            instance = new BookmarkService();
        }
        return instance;
    }

    public String generateSequenceString(List<String> bookmarkList, String foldingType, Integer foldPaddingValue) {
        // merge the bookmark info's into a single one!
        String mergedSequence = "[proj="+foldingType+",padding="+foldPaddingValue;
        if(bookmarkList.size()>0){
            mergedSequence += ",sequences=[";
        }
        for(int i = 0 ; i < bookmarkList.size() ; i++){
            mergedSequence += bookmarkList.get(i);
            if(i < bookmarkList.size()-1){
                mergedSequence += "," ;
            }
        }
        if(bookmarkList.size()>0){
            mergedSequence += "]";
        }
        mergedSequence += "]";
        return mergedSequence;
    }
}
