package org.bbop.apollo.event

/**
 * Created by Nathan Dunn on 10/29/14.
 */
interface AnnotationListener extends EventListener{

    public void handleChangeEvent(AnnotationEvent... event);
}
