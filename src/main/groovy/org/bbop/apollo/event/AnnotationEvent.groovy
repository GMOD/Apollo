package org.bbop.apollo.event

import grails.validation.Validateable
import org.grails.web.json.JSONObject
import org.bbop.apollo.Sequence

// BACKWARDS INCOMPATIBILITY: @Validateable annotation was changed to a trait
// in Grails 3+. Now use `implements Validateable` instead.
class AnnotationEvent implements Validateable {

    JSONObject features
    Sequence sequence
    Operation operation
    boolean sequenceAlterationEvent
    String username

    enum Operation {
        ADD,
        DELETE,
        UPDATE,
        ERROR
    }
}
