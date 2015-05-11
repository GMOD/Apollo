package org.bbop.apollo

import org.xhtmlrenderer.css.parser.property.PrimitivePropertyBuilders.Left

/**
 * Created by ndunn on 5/9/15.
 */
class LeftPaddingStrategy implements PaddingStrategy{

    String paddingText = "0"
    Integer defaultPaddingCount = 5

    String pad(Integer count){
        return count.toString().padLeft(defaultPaddingCount,paddingText)
    }
}
