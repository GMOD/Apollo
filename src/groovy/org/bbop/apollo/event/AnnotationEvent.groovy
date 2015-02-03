package org.bbop.apollo.event

import grails.validation.Validateable
import org.bbop.apollo.Sequence
import org.codehaus.groovy.grails.web.json.JSONObject

/**
 * Created by ndunn on 10/29/14.
 */
@Validateable
class AnnotationEvent {

    JSONObject features
    Sequence sequence
    Operation operation
    boolean sequenceAlterationEvent
    // toplevel feature?

//    public AnnotationEvent(Object features,Sequence sequence,Operation operation){
////        super(features)
//        this.sequence = sequence
//        this.operation = operation
//    }

    public enum Operation {
        ADD,
        DELETE,
        UPDATE,
        SET_EXON_BOUNDARY,
    }

}
