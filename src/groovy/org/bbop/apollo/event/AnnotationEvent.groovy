package org.bbop.apollo.event

import grails.validation.Validateable
import org.bbop.apollo.Bookmark
import org.codehaus.groovy.grails.web.json.JSONObject
import org.bbop.apollo.Sequence

/**
 * Created by Nathan Dunn on 10/29/14.
 */
@Validateable
class AnnotationEvent {

    JSONObject features
    Bookmark bookmark
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
