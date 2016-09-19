package org.bbop.apollo.gwt.client.assemblage;

import com.allen_sauer.gwt.dnd.client.PickupDragController;
import com.allen_sauer.gwt.dnd.client.drop.HorizontalPanelDropController;
import com.allen_sauer.gwt.dnd.client.drop.VerticalPanelDropController;
import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Random;
import com.google.gwt.user.client.ui.*;

/**
 * Created by nathandunn on 9/19/16.
 */
public class AssemblageDetailPanel extends Composite{

    interface AssemblageDetailPanelUiBinder extends UiBinder<Widget, AssemblageDetailPanel> {

    }
    private static AssemblageDetailPanelUiBinder ourUiBinder = GWT.create(AssemblageDetailPanelUiBinder.class);


    @UiField
    AbsolutePanel boundaryPanel;
    @UiField(provided = true)
    HTML logField = new HTML();


    private static final int SPACING = 0;

    private AssemblageDragHandler assemblageDragHandler = new AssemblageDragHandler(logField);

    public AssemblageDetailPanel() {
        Widget rootElement = ourUiBinder.createAndBindUi(this);
        initWidget(rootElement);

        boundaryPanel.setSize("100%","100%");
        PickupDragController columnDragController = new PickupDragController(boundaryPanel,false);
        columnDragController.setBehaviorBoundaryPanelDrop(false);
        columnDragController.addDragHandler(assemblageDragHandler);

        PickupDragController widgetDragController = new PickupDragController(boundaryPanel, false);
        widgetDragController.setBehaviorMultipleSelection(false);
        widgetDragController.addDragHandler(assemblageDragHandler);

        HorizontalPanel horizontalPanel = new HorizontalPanel();
//        horizontalPanel.addStyleName(CSS_DEMO_INSERT_PANEL_EXAMPLE_CONTAINER);
        horizontalPanel.setSpacing(SPACING);
        boundaryPanel.add(horizontalPanel);

        // initialize our column drop controller
        HorizontalPanelDropController columnDropController = new HorizontalPanelDropController(
                horizontalPanel);
        columnDragController.registerDropController(columnDropController);

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
            for (int row = 1; row <= 5 ; row++) {
                // initialize a widget
                HTML widget = new HTML("Draggable&nbsp;#" + ++count);
//                widget.addStyleName(CSS_DEMO_INSERT_PANEL_EXAMPLE_WIDGET);
                widget.setHeight(Random.nextInt(4) + 2 + "em");
                verticalPanel.add(widget);

                // make the widget draggable
                widgetDragController.makeDraggable(widget);
            }
        }
    }
}