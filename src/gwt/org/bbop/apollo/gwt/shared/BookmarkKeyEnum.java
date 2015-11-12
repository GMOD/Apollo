package org.bbop.apollo.gwt.shared;


/**
 * Created by nathandunn on 10/5/15.
 */
public enum BookmarkKeyEnum {

    PROJECTION
    ,PADDING
    ,BOOKMARKS
    ,SEQUENCES
    ,REFERENCE_TRACK("referenceTrack")
    ,FEATURES
    ,PROJ_NONE("None")
    ,PROJ_EXON("Exon")
    ,PROJ_TRANSCRIPT("Transcript")
    ;

    private String value ;

    BookmarkKeyEnum(String value){
        this.value = value ;
    }

    BookmarkKeyEnum(){
        this.value = name().toLowerCase();
    }

    public String getValue() {
        return value;
    }
}
