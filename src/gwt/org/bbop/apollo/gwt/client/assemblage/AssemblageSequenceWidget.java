package org.bbop.apollo.gwt.client.assemblage;

import com.allen_sauer.gwt.dnd.client.PickupDragController;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import org.bbop.apollo.gwt.client.dto.assemblage.AssemblageSequence;
import org.bbop.apollo.gwt.client.dto.assemblage.SequenceFeatureInfo;
import org.bbop.apollo.gwt.shared.ColorGenerator;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Icon;
import org.gwtbootstrap3.client.ui.constants.IconType;

/**
 * Created by nathandunn on 9/20/16.
 */
public class AssemblageSequenceWidget extends VerticalPanel{

    private AssemblageSequence assemblageSequence;

    public AssemblageSequence getAssemblageSequence() {
        return assemblageSequence;
    }

    public void setAssemblageSequence(AssemblageSequence assemblageSequence) {
        this.assemblageSequence = assemblageSequence;
    }

    public void render(PickupDragController assemblageSequenceDragController,int i) {
        String sequenceName = assemblageSequence.getName();
        HorizontalPanel headingPanel = new HorizontalPanel();
        Button labelButton = new Button(sequenceName);
        Icon leftIcon = new Icon(IconType.ARROW_LEFT);
        leftIcon.addStyleName("pull-left");
        labelButton.add(leftIcon);
        Icon rightIcon = new Icon(IconType.ARROW_RIGHT);
        rightIcon.addStyleName("pull-right");
        labelButton.add(rightIcon);
        if (assemblageSequence.getReverse()) {
            rightIcon.setColor("#DDD");
        } else {
            leftIcon.setColor("#DDD");
        }
        labelButton.setColor(ColorGenerator.getColorForIndex(i));
        headingPanel.add(labelButton);
        headingPanel.addStyleName("assemblage-detail-heading");

        HTML headingHtml = new HTML(headingPanel.getElement().getInnerHTML());
        add(headingHtml);

        assemblageSequenceDragController.makeDraggable(this, headingHtml);
    }

}
