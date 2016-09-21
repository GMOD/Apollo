package org.bbop.apollo.gwt.client.assemblage;

import com.allen_sauer.gwt.dnd.client.PickupDragController;
import com.allen_sauer.gwt.dnd.client.drop.VerticalPanelDropController;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Widget;
import org.bbop.apollo.gwt.client.dto.assemblage.SequenceFeatureInfo;

import java.util.ArrayList;
import java.util.List;

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
        if(!containsFeature(sequenceFeatureInfo)){
            AssemblageFeatureWidget assemblageFeatureWidget = new AssemblageFeatureWidget(sequenceFeatureInfo);
            assemblageFeatureWidget.render(featureDragController);
            add(assemblageFeatureWidget);
        }
    }

    private boolean containsFeature(SequenceFeatureInfo sequenceFeatureInfo) {
        for(AssemblageFeatureWidget assemblageFeatureWidget : getAssembledFeatureWidgets()){
            if(sequenceFeatureInfo.getName().equals(assemblageFeatureWidget.getSequenceFeatureInfo().getName())){
//                GWT.log("not adding a duplicate feature "+sequenceFeatureInfo.getName());
                return true ;
            }
        }
        return false ;
    }

    private List<AssemblageFeatureWidget> getAssembledFeatureWidgets() {
        List<AssemblageFeatureWidget> assemblageFeatureWidgets = new ArrayList<>();
        for(int i = 0 ; i < getWidgetCount() ; i++){
            if(getWidget(i) instanceof AssemblageFeatureWidget){
                assemblageFeatureWidgets.add( (AssemblageFeatureWidget) getWidget(i));
            }
        }

        return  assemblageFeatureWidgets ;
    }

}
