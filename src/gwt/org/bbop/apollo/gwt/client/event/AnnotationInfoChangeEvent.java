package org.bbop.apollo.gwt.client.event;

import com.google.gwt.event.shared.GwtEvent;
import org.bbop.apollo.gwt.client.dto.AnnotationInfo;

/**
 * Created by ndunn on 2/2/15.
 */
public class AnnotationInfoChangeEvent extends GwtEvent<AnnotationInfoChangeEventHandler>{

    public static Type<AnnotationInfoChangeEventHandler> TYPE = new Type<AnnotationInfoChangeEventHandler>();

    private AnnotationInfo annotationInfo ;
    private Action action ;

    public enum Action{
        UPDATE,
        INSERT,
        DELETE,
        SET_FOCUS,
    }

    public AnnotationInfoChangeEvent(AnnotationInfo annotationInfo,Action action){
        this.annotationInfo = annotationInfo ;
        this.action = action ;
    }

    @Override
    public Type<AnnotationInfoChangeEventHandler> getAssociatedType() {
        return TYPE ;
    }

    @Override
    protected void dispatch(AnnotationInfoChangeEventHandler handler) {
       handler.onAnnotationChanged(this);
    }

    public AnnotationInfo getAnnotationInfo() {
        return annotationInfo;
    }

    public void setAnnotationInfo(AnnotationInfo annotationInfo) {
        this.annotationInfo = annotationInfo;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }
}
