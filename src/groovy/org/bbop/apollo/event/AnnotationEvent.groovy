package org.bbop.apollo.event

import grails.validation.Validateable
import org.bbop.apollo.Sequence

/**
 * Created by ndunn on 10/29/14.
 */
@Validateable
class AnnotationEvent {

    Operation operation
    Sequence sequence
    Object source
    // toplevel feature?

//    public AnnotationEvent(Object source,Sequence sequence,Operation operation){
////        super(source)
//        this.sequence = sequence
//        this.operation = operation
//    }

    public enum Operation {
        ADD,
        DELETE,
        UPDATE
    }

}
