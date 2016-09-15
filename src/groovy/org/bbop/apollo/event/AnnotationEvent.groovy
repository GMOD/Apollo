package org.bbop.apollo.event

import grails.validation.Validateable
import org.bbop.apollo.Assemblage
import org.codehaus.groovy.grails.web.json.JSONObject

/**
 * Created by Nathan Dunn on 10/29/14.
 */
@Validateable
class AnnotationEvent {

    JSONObject features
    Assemblage assemblage
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
    }

}
