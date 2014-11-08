package org.bbop.apollo

import org.bbop.apollo.event.AnnotationEvent

/**
 * Created by ndunn on 11/7/14.
 */
interface AnnotationEventListener {

    def handleEvent(AnnotationEvent annotationEvent)

}