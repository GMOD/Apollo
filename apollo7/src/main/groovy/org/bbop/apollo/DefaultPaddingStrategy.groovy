package org.bbop.apollo
/**
 * Created by ndunn on 5/9/15.
 */
class DefaultPaddingStrategy implements PaddingStrategy{

    String pad(Integer count){
        return count.toString()
    }
}
