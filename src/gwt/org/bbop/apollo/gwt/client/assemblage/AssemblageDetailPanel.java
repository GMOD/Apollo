package org.bbop.apollo.gwt.client.assemblage;

import com.allen_sauer.gwt.dnd.client.PickupDragController;
import com.allen_sauer.gwt.dnd.client.drop.HorizontalPanelDropController;
import com.allen_sauer.gwt.dnd.client.drop.VerticalPanelDropController;
import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
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
    }

    public AssemblageInfo getAssemblageInfo() {
        return null;
    }

    public void setAssemblageInfo(Set<AssemblageInfo> selectedObjects) {


        widgetDragController.unregisterDropControllers();

        horizontalPanel.clear();

        // we have to munge this to Sequence,List<Feature>
        // assemblageInfo will only have a single feature
//        Map<String, List<AssemblageSequence>> assemblageSequenceListMap = new HashMap<>();
//        TreeMap<Integer, String> orderMap = new TreeMap<>();
//
//        int counter = 0;
//        for (AssemblageInfo assemblageInfo : selectedObjects) {
//            AssemblageSequenceList sequenceArray = assemblageInfo.getSequenceList();
//            for (int i = 0; i < sequenceArray.size(); i++) {
//                AssemblageSequence assemblageSequence = sequenceArray.getSequence(i);
//
//                List<AssemblageSequence> assemblageInfoList = assemblageSequenceListMap.get(assemblageSequence.getName());
//                SequenceFeatureInfo sequenceFeatureInfo = assemblageSequence.getFeature();
////                if (sequenceFeatureInfo != null) {
//                    if (assemblageInfoList == null) {
//                        assemblageInfoList = new ArrayList<>();
//                    }
//                    assemblageInfoList.add(assemblageSequence);
////                }
//                assemblageSequenceListMap.put(assemblageSequence.getName(), assemblageInfoList);
//                if (!orderMap.containsValue(assemblageSequence.getName())) {
//                    orderMap.put(counter, assemblageSequence.getName());
//                }
//                ++counter;
//            }
//        }
//        GWT.log("assemblies: " + assemblageSequenceListMap.size());
//
//
//        // we simplify the ordering to do it all at once
//        for (String sequenceName : orderMap.values()) {
//            GWT.log("trying to "+ sequenceName + " read for: "+orderMap.values());
//            List<AssemblageSequence> assemblageSequences = assemblageSequenceListMap.get(sequenceName);
//            GWT.log("assemblies: " + assemblageSequences.size() + " for "+sequenceName);
//            VerticalPanel sequenceColumnPanel = new VerticalPanel();
//
//            VerticalPanel featurePanel = new VerticalPanelWithSpacer();
//            featurePanel.setSpacing(SPACING);
//            horizontalPanel.add(sequenceColumnPanel);
//
//            VerticalPanelDropController widgetDropController = new VerticalPanelDropController(featurePanel);
//            widgetDragController.registerDropController(widgetDropController);
//
//
////            HorizontalPanel headingPanel = new HorizontalPanel();
//            HTML headingHtml = new HTML(sequenceName);
//
////            if (sequenceObject.getReverse()) {
////                headingPanel.add(new Icon(IconType.LONG_ARROW_LEFT));
////            }
////            headingPanel.add(headingHtml);
////            if (!sequenceObject.getReverse()) {
////                headingPanel.add(new Icon(IconType.LONG_ARROW_RIGHT));
////            }
//
//            sequenceColumnPanel.add(headingHtml);
////                    sequenceColumnPanel.add(headingPanel);
//            sequenceColumnPanel.add(featurePanel);
//
////                    columnDragController.makeDraggable(sequenceColumnPanel, headingPanel);
//            columnDragController.makeDraggable(sequenceColumnPanel, headingHtml);
//
////            sequenceColumnMap.put(sequenceName, sequenceColumnPanel);
////                    sequenceFeatureMap.put(sequenceName, featurePanel);
//
//            // extract the feature
//            for(AssemblageSequence assemblageSequence : assemblageSequences){
//                SequenceFeatureInfo sequenceFeatureInfo = assemblageSequence.getFeature();
//                if (sequenceFeatureInfo != null) {
////                    VerticalPanel thisFeaturePanel = sequenceFeatureMap.get(sequenceName);
//                    String name = sequenceFeatureInfo.getName();
//                    HTML widget = new HTML(name);
//                    featurePanel.add(widget);
//////                // make the widget draggable
//                    widgetDragController.makeDraggable(widget);
//                }
//            }
//        }
        Map<String, VerticalPanel> sequenceColumnMap = new HashMap<>();
        Map<String, VerticalPanel> sequenceFeatureMap = new HashMap<>();

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

                    if (sequenceObject.getReverse()) {
                        headingPanel.add(new Icon(IconType.LONG_ARROW_LEFT));
                    }
                    headingPanel.add(headingHtml);
                    if (!sequenceObject.getReverse()) {
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
                // else, nothing to do, we have to process the feature isntead


            }
        }

    }

}