package org.bbop.apollo.gwt.client.assemblage;

import com.allen_sauer.gwt.dnd.client.PickupDragController;
import com.allen_sauer.gwt.dnd.client.drop.VerticalPanelDropController;
import org.bbop.apollo.gwt.client.dto.assemblage.SequenceFeatureInfo;

/**
 * This creates the area that contains feature widgets.
 *
 * Created by nathandunn on 9/20/16.
 */
public class AssemblageFeatureAreaWidget extends VerticalPanelWithSpacer{

    public AssemblageFeatureAreaWidget(){
        addStyleName("assemblage-detail-composite");
    }

    public void registerDropController(PickupDragController widgetDragController) {
        VerticalPanelDropController featureDropController = new VerticalPanelDropController(this);
        widgetDragController.registerDropController(featureDropController);
    }

    public void addSequenceFeature(SequenceFeatureInfo sequenceFeatureInfo,PickupDragController featureDragController) {
        AssemblageFeatureWidget assemblageFeatureWidget = new AssemblageFeatureWidget(sequenceFeatureInfo);
        assemblageFeatureWidget.render(featureDragController);
        add(assemblageFeatureWidget);
    }

}
