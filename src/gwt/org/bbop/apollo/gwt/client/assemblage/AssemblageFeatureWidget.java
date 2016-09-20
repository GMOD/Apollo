package org.bbop.apollo.gwt.client.assemblage;

import com.allen_sauer.gwt.dnd.client.PickupDragController;
import com.allen_sauer.gwt.dnd.client.drop.VerticalPanelDropController;
import com.google.gwt.user.client.Random;
import com.google.gwt.user.client.ui.HTML;
import org.bbop.apollo.gwt.client.dto.assemblage.SequenceFeatureInfo;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Icon;
import org.gwtbootstrap3.client.ui.constants.ButtonType;
import org.gwtbootstrap3.client.ui.constants.IconType;

/**
 * Created by nathandunn on 9/20/16.
 */
public class AssemblageFeatureWidget extends VerticalPanelWithSpacer{

    public AssemblageFeatureWidget(){
        addStyleName("assemblage-detail-composite");
    }

    private SequenceFeatureInfo sequenceFeatureInfo ;

    public SequenceFeatureInfo getSequenceFeatureInfo() {
        return sequenceFeatureInfo;
    }

    public void setSequenceFeatureInfo(SequenceFeatureInfo sequenceFeatureInfo) {
        this.sequenceFeatureInfo = sequenceFeatureInfo;
    }

    public void registerDropController(PickupDragController widgetDragController) {
        VerticalPanelDropController featureDropController = new VerticalPanelDropController(this);
        widgetDragController.registerDropController(featureDropController);
    }

    public void setSequenceFeature(SequenceFeatureInfo sequenceFeatureInfo) {
        this.sequenceFeatureInfo = sequenceFeatureInfo ;

    }

    public void render(PickupDragController featureDragController) {
        String name = sequenceFeatureInfo.getName();
        Button featureButton = new Button(name);
        featureButton.setType(ButtonType.DANGER);

        IconType iconType = Random.nextBoolean() ? IconType.EXPAND : IconType.COMPRESS;
        Icon expandIcon = new Icon(iconType);
        expandIcon.addStyleName("rotate-icon-45");
        expandIcon.addStyleName("pull-right");
        featureButton.add(expandIcon);

//                    HTML widget = new HTML(name);
        HTML widget = new HTML(featureButton.getElement().getInnerHTML());
        add(widget);
        widget.addStyleName("assemblage-detail-widget");

        featureDragController.makeDraggable(widget);
    }
}
