package org.bbop.apollo.gwt.client.projection;

import org.bbop.apollo.projection.*;

/**
 * This class is responsible for generating projection libraries.
 *
 * Created by nathandunn on 10/10/16.
 */
public class ProjectionService {


    MultiSequenceProjection getProjectionForString(String projectionString){
        return null ;
    }

    Double projectValue(Double input,String referenceString){
        return input;
    }

    public static native void exportStaticMethod() /*-{
        @wnd.projectValue = $entry(@org.bbop.apollo.gwt.client.projection.ProjectionService::projectValue(L/java/lang/Double;Ljava/lang/String;));
    }-*/;
}
