package org.bbop.apollo

/**
 * Created by ndunn on 5/9/15.
 */
class LeftPaddingStrategy implements PaddingStrategy{

    String paddingText = "0"
    Integer defaultPaddingCount = 5
    Integer offset = 1

    String pad(Integer count){
        return (count+offset).toString().padLeft(defaultPaddingCount,paddingText)
    }
}
