package org.bbop.apollo.gwt.client.assemblage;

import com.allen_sauer.gwt.dnd.client.PickupDragController;
import com.allen_sauer.gwt.dnd.client.drop.HorizontalPanelDropController;
import com.allen_sauer.gwt.dnd.client.drop.VerticalPanelDropController;
import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Random;
import com.google.gwt.user.client.ui.*;
import org.bbop.apollo.gwt.client.dto.assemblage.AssemblageInfo;
import org.bbop.apollo.gwt.client.dto.assemblage.AssemblageSequence;
import org.bbop.apollo.gwt.client.dto.assemblage.AssemblageSequenceList;
import org.bbop.apollo.gwt.client.dto.assemblage.SequenceFeatureInfo;
import org.gwtbootstrap3.client.ui.Icon;
import org.gwtbootstrap3.client.ui.constants.IconType;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by nathandunn on 9/19/16.
 */
public class AssemblageDetailPanel extends Composite {


    interface AssemblageDetailPanelUiBinder extends UiBinder<Widget, AssemblageDetailPanel> {

    }

    private static AssemblageDetailPanelUiBinder ourUiBinder = GWT.create(AssemblageDetailPanelUiBinder.class);


    @UiField(provided = true)
    AbsolutePanel boundaryPanel = new AbsolutePanel();
    @UiField(provided = true)
    HTML logField = new HTML();


    private static final int SPACING = 0;

    private AssemblageDragHandler assemblageDragHandler = new AssemblageDragHandler(logField);
    private HorizontalPanel horizontalPanel = new HorizontalPanel();
    private PickupDragController widgetDragController = new PickupDragController(boundaryPanel, false);
    private PickupDragController columnDragController = new PickupDragController(boundaryPanel, false);

    public AssemblageDetailPanel() {
        Widget rootElement = ourUiBinder.createAndBindUi(this);
        initWidget(rootElement);

        boundaryPanel.setSize("100%", "100%");
        columnDragController.setBehaviorBoundaryPanelDrop(false);
        columnDragController.addDragHandler(assemblageDragHandler);

        widgetDragController.setBehaviorMultipleSelection(false);
        widgetDragController.addDragHandler(assemblageDragHandler);

//        horizontalPanel.addStyleName(CSS_DEMO_INSERT_PANEL_EXAMPLE_CONTAINER);
        horizontalPanel.setSpacing(SPACING);
        boundaryPanel.add(horizontalPanel);

        // initialize our column drop controller
        HorizontalPanelDropController columnDropController = new HorizontalPanelDropController(
                horizontalPanel);
        columnDragController.registerDropController(columnDropController);

        if(true) return ;
        for (int col = 1; col <= 3 ; col++) {
            // initialize a vertical panel to hold the heading and a second vertical
            // panel
            VerticalPanel columnCompositePanel = new VerticalPanel();
//            columnCompositePanel.addStyleName(CSS_DEMO_INSERT_PANEL_EXAMPLE_COLUMN_COMPOSITE);

            // initialize inner vertical panel to hold individual widgets
            VerticalPanel verticalPanel = new VerticalPanelWithSpacer();
//            verticalPanel.addStyleName(CSS_DEMO_INSERT_PANEL_EXAMPLE_CONTAINER);
            verticalPanel.setSpacing(SPACING);
            horizontalPanel.add(columnCompositePanel);

            // initialize a widget drop controller for the current column
            VerticalPanelDropController widgetDropController = new VerticalPanelDropController(
                    verticalPanel);
            widgetDragController.registerDropController(widgetDropController);

            // Put together the column pieces
            Label heading = new Label("Column " + col);
//            heading.addStyleName(CSS_DEMO_INSERT_PANEL_EXAMPLE_HEADING);
            columnCompositePanel.add(heading);
            columnCompositePanel.add(verticalPanel);

            // make the column draggable by its heading
            columnDragController.makeDraggable(columnCompositePanel, heading);

            int count = 0 ;
//            for (int row = 1; row <= 5 ; row++) {
//                // initialize a widget
//                HTML widget = new HTML("Draggable&nbsp;#" + ++count);
////                widget.addStyleName(CSS_DEMO_INSERT_PANEL_EXAMPLE_WIDGET);
//                widget.setHeight(Random.nextInt(4) + 2 + "em");
//                verticalPanel.add(widget);
//
//                // make the widget draggable
//                widgetDragController.makeDraggable(widget);
//            }
        }
    }

    public AssemblageInfo getAssemblageInfo() {
        return null;
    }

    public void setAssemblageInfo(Set<AssemblageInfo> selectedObjects) {
//        if(true) return ;

        Map<String, VerticalPanel> sequenceColumnMap = new HashMap<>();

        // a map of the features?
        Map<String, VerticalPanel> sequenceFeatureMap = new HashMap<>();

        horizontalPanel.clear();
        for (AssemblageInfo assemblageInfo : selectedObjects) {

            AssemblageSequenceList sequenceArray = assemblageInfo.getSequenceList();
            for (int i = 0; i < sequenceArray.size(); i++) {
                AssemblageSequence sequenceObject = sequenceArray.getSequence(i);
                String sequenceName = sequenceObject.getName();
                VerticalPanel sequenceColumnPanel = sequenceColumnMap.get(sequenceName);

                // add a new sequence column
                if (sequenceColumnPanel == null) {
                    sequenceColumnPanel = new VerticalPanel();

                    VerticalPanel featurePanel = new VerticalPanelWithSpacer();
                    featurePanel.setSpacing(SPACING);
                    horizontalPanel.add(sequenceColumnPanel);

                    VerticalPanelDropController widgetDropController = new VerticalPanelDropController(featurePanel);
                    widgetDragController.registerDropController(widgetDropController);


                    HorizontalPanel headingPanel = new HorizontalPanel();
                    HTML headingHtml = new HTML(sequenceName);

                    if(sequenceObject.getReverse()){
                        headingPanel.add(new Icon(IconType.LONG_ARROW_LEFT));
                    }
                    headingPanel.add(headingHtml);
                    if(!sequenceObject.getReverse()){
                        headingPanel.add(new Icon(IconType.LONG_ARROW_RIGHT));
                    }

                    sequenceColumnPanel.add(headingHtml);
//                    sequenceColumnPanel.add(headingPanel);
                    sequenceColumnPanel.add(featurePanel);

//                    columnDragController.makeDraggable(sequenceColumnPanel, headingPanel);
                    columnDragController.makeDraggable(sequenceColumnPanel, headingHtml);

                    sequenceColumnMap.put(sequenceName, sequenceColumnPanel);
                    sequenceFeatureMap.put(sequenceName, featurePanel);
                }
                // else, nothing to do, we have to process the feature isntead

                // extract the feature
                SequenceFeatureInfo sequenceFeatureInfo = sequenceObject.getFeature();
                if (sequenceFeatureInfo != null) {
                    VerticalPanel thisFeaturePanel = sequenceFeatureMap.get(sequenceName);
                    String name = sequenceFeatureInfo.getName();
                    HTML widget = new HTML(name);
                    thisFeaturePanel.add(widget);
////                // make the widget draggable
                    widgetDragController.makeDraggable(widget);
                }

            }
        }

    }

}