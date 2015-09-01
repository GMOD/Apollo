package org.bbop.apollo
/**
 * Created by Nathan Dunn on 5/9/15.
 */
class DefaultPaddingStrategy implements PaddingStrategy{

    String pad(Integer count){
        return count.toString()
    }
}
