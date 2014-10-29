package org.bbop.apollo.event

import org.bbop.apollo.Feature

/**
 * Created by ndunn on 10/29/14.
 */
class AnnotationEvent extends EventObject{

    Operation operation
    Feature feature
    // toplevel feature?

    public AnnotationEvent(Object source,Feature feature,Operation operation){
        super(source)
        this.feature= feature
        this.operation = operation
    }

    public enum Operation {
        ADD,
        DELETE,
        UPDATE
    }

}
