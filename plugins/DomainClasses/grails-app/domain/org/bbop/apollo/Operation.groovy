package org.bbop.apollo

import org.bbop.apollo.operation.OperationEnum

/**
 * We store operation attributes as JSON
 */
class Operation {
   
    OperationEnum operationType
    String featureUniqueName
    String attributes;
    String oldFeatures;
    String newFeatures;


    static mapping = {
        attributes type: 'text'
        oldFeatures type: 'text'
        newFeatures type: 'text'
    }

    static constraints = {
        attributes nullable: true, blank: false
        newFeatures nullable: true, blank: false
        oldFeatures nullable: true, blank: false
    }
    
}
