package org.bbop.apollo.gwt.client.assemblage;

import com.allen_sauer.gwt.dnd.client.PickupDragController;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import org.bbop.apollo.gwt.client.Annotator;
import org.bbop.apollo.gwt.client.dto.assemblage.AssemblageSequence;
import org.bbop.apollo.gwt.shared.ColorGenerator;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Icon;
import org.gwtbootstrap3.client.ui.constants.IconType;
import org.gwtbootstrap3.client.ui.constants.Pull;

/**
 * Created by nathandunn on 9/20/16.
 */
public class AssemblageSequenceWidget extends VerticalPanel{

    private AssemblageSequence assemblageSequence;
    private Icon leftIcon = new Icon(IconType.ARROW_LEFT);
    private Icon rightIcon = new Icon(IconType.ARROW_RIGHT);
    private Button labelButton = new Button("");

    public AssemblageSequenceWidget(final AssemblageSequence assemblageSequence){
        this.assemblageSequence = assemblageSequence;

        String sequenceName = assemblageSequence.getName();
        labelButton.setText(sequenceName);
        HorizontalPanel headingPanel = new HorizontalPanel();
        leftIcon.setPull(Pull.LEFT);
        labelButton.add(leftIcon);
        rightIcon.setPull(Pull.RIGHT);
        labelButton.add(rightIcon);
        headingPanel.add(labelButton);
        headingPanel.addStyleName("assemblage-detail-heading");
        addStyleName("assemblage-detail-composite");

        leftIcon.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                assemblageSequence.setReverse(true);
                handleReverseComplement();
            }
        });
        rightIcon.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                assemblageSequence.setReverse(false);
                handleReverseComplement();
            }
        });

        add(headingPanel);
    }

    public AssemblageSequence getAssemblageSequence() {
        return assemblageSequence;
    }


    public void handleReverseComplement(){
        if (assemblageSequence.getReverse()) {
            leftIcon.setColor("green");
            rightIcon.setColor("#DDD");
        } else {
            leftIcon.setColor("#DDD");
            rightIcon.setColor("green");
        }
        Annotator.eventBus.fireEvent(new AssemblageViewEvent());
    }


    public void render(PickupDragController assemblageSequenceDragController,int i) {
        labelButton.setColor(ColorGenerator.getColorForIndex(i));
        if (assemblageSequence.getReverse()) {
            rightIcon.setColor("#DDD");
        } else {
            leftIcon.setColor("#DDD");
        }

        assemblageSequenceDragController.makeDraggable(this, labelButton);
    }

}
