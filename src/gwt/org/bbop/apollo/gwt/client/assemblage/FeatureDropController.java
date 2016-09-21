package org.bbop.apollo.gwt.client.assemblage;

import com.allen_sauer.gwt.dnd.client.DragContext;
import com.allen_sauer.gwt.dnd.client.VetoDragException;
import com.allen_sauer.gwt.dnd.client.drop.VerticalPanelDropController;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;

/**
 * Created by nathandunn on 9/21/16.
 */
public class FeatureDropController extends VerticalPanelDropController {

    private AssemblageFeatureAreaWidget assemblageFeatureAreaWidget ;

    public FeatureDropController(AssemblageFeatureAreaWidget verticalPanel) {
        super(verticalPanel);
        this.assemblageFeatureAreaWidget = verticalPanel ;
    }

    @Override
    public void onPreviewDrop(DragContext context) throws VetoDragException {
        super.onPreviewDrop(context);

        // should be An AssembleFeatureWidget
        AssemblageFeatureWidget assemblageFeatureWidget = (AssemblageFeatureWidget) context.draggable ;

        String targetSequenceName =  assemblageFeatureAreaWidget.getAssemblageSequence().getName();
        String sourceSequenceName = assemblageFeatureWidget.getSequenceFeatureInfo().getParentId();
        if(!targetSequenceName.equals(sourceSequenceName)){
            GWT.log("Source ["+sourceSequenceName+"] and target ["+targetSequenceName+"] are not the same sequence name so rejecting");
            throw new VetoDragException();
        }
    }

}
