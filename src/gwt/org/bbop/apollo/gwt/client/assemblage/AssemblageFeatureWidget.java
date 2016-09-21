package org.bbop.apollo.gwt.client.assemblage;

import com.allen_sauer.gwt.dnd.client.PickupDragController;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HorizontalPanel;
import org.bbop.apollo.gwt.client.Annotator;
import org.bbop.apollo.gwt.client.dto.assemblage.SequenceFeatureInfo;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Image;
import org.gwtbootstrap3.client.ui.constants.ButtonType;
import org.gwtbootstrap3.client.ui.constants.ImageType;
import org.gwtbootstrap3.client.ui.constants.Pull;

/**
 * Created by nathandunn on 9/20/16.
 */
public class AssemblageFeatureWidget extends HorizontalPanel {

    private SequenceFeatureInfo sequenceFeatureInfo;

    private Button featureButton = new Button();
    private Image expandImage = new Image();
    private Image collapseImage = new Image();


    private void handleCollapsed() {
        if (sequenceFeatureInfo.isCollapsed()) {
            collapseImage.setVisible(false);
            expandImage.setVisible(true);
        } else {
            collapseImage.setVisible(true);
            expandImage.setVisible(false);
        }
        Annotator.eventBus.fireEvent(new AssemblageViewEvent());
    }

    public AssemblageFeatureWidget(final SequenceFeatureInfo sequenceFeatureInfo) {
        this.sequenceFeatureInfo = sequenceFeatureInfo;
        featureButton.setType(ButtonType.INFO);
        String name = sequenceFeatureInfo.getName();
        featureButton.setText(name);

        featureButton.addStyleName("assemblage-detail-widget");

        collapseImage.setVisible(sequenceFeatureInfo.isCollapsed());
        collapseImage.setUrl("../images/fold.png");
        collapseImage.setType(ImageType.DEFAULT);
        collapseImage.setSize("20px", "20px");
        collapseImage.setPull(Pull.RIGHT);
        featureButton.add(collapseImage);

        collapseImage.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                sequenceFeatureInfo.setCollapsed(true);
                handleCollapsed();
            }

        });

        expandImage.setVisible(!sequenceFeatureInfo.isCollapsed());
        expandImage.setUrl("../images/unfold.png");
        expandImage.setType(ImageType.DEFAULT);
        expandImage.setSize("20px", "20px");
        expandImage.setPull(Pull.RIGHT);
        featureButton.add(expandImage);

        expandImage.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                sequenceFeatureInfo.setCollapsed(false);
                handleCollapsed();
            }

        });

        add(featureButton);
    }


    public void render(PickupDragController featureDragController) {
        featureDragController.makeDraggable(this, featureButton);
    }

    public SequenceFeatureInfo getSequenceFeatureInfo() {
        return sequenceFeatureInfo;
    }
}
