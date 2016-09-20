package org.bbop.apollo.gwt.client.assemblage;

import com.allen_sauer.gwt.dnd.client.PickupDragController;
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
public class AssemblageFeatureWidget extends HTML {

    private SequenceFeatureInfo sequenceFeatureInfo ;

    public AssemblageFeatureWidget(SequenceFeatureInfo sequenceFeatureInfo){
        this.sequenceFeatureInfo = sequenceFeatureInfo ;
    }

    public void render(PickupDragController featureDragController){
        String name = sequenceFeatureInfo.getName();
        Button featureButton = new Button(name);
        featureButton.setType(ButtonType.DANGER);

        IconType iconType = Random.nextBoolean() ? IconType.EXPAND : IconType.COMPRESS;
        Icon expandIcon = new Icon(iconType);
        expandIcon.addStyleName("rotate-icon-45");
        expandIcon.addStyleName("pull-right");
        featureButton.add(expandIcon);

        setHTML(featureButton.getElement().getInnerHTML());
        addStyleName("assemblage-detail-widget");

        featureDragController.makeDraggable(this);
    }

    public SequenceFeatureInfo getSequenceFeatureInfo() {
        return sequenceFeatureInfo;
    }
}
