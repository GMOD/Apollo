package org.bbop.apollo.gwt.client.assemblage;

import com.allen_sauer.gwt.dnd.client.PickupDragController;
import com.allen_sauer.gwt.dnd.client.drop.HorizontalPanelDropController;
import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import org.bbop.apollo.gwt.client.Annotator;
import org.bbop.apollo.gwt.client.MainPanel;
import org.bbop.apollo.gwt.client.dto.assemblage.AssemblageInfo;
import org.bbop.apollo.gwt.client.dto.assemblage.AssemblageSequence;
import org.bbop.apollo.gwt.client.dto.assemblage.AssemblageSequenceList;
import org.bbop.apollo.gwt.client.dto.assemblage.SequenceFeatureInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by nathandunn on 9/19/16.
 */
public class AssemblageDetailPanel extends Composite {


    interface AssemblageDetailPanelUiBinder extends UiBinder<Widget, AssemblageDetailPanel> { }

    private static AssemblageDetailPanelUiBinder ourUiBinder = GWT.create(AssemblageDetailPanelUiBinder.class);

    @UiField(provided = true)
    protected AbsolutePanel boundaryPanel = new AbsolutePanel();

    private static final int SPACING = 0;

    private AssemblageWidget assemblageWidget = new AssemblageWidget();
    private PickupDragController featureDragController = new PickupDragController(boundaryPanel, false);
    private PickupDragController assemblageSequenceDragController = new PickupDragController(boundaryPanel, false);

    public AssemblageDetailPanel() {
        Widget rootElement = ourUiBinder.createAndBindUi(this);
        initWidget(rootElement);
        AssemblageDragHandler assemblageDragHandler = new AssemblageDragHandler();

        boundaryPanel.setSize("100%", "100%");
        assemblageSequenceDragController.setBehaviorBoundaryPanelDrop(false);
        assemblageSequenceDragController.setBehaviorDragStartSensitivity(5);
        assemblageSequenceDragController.addDragHandler(assemblageDragHandler);

        featureDragController.setBehaviorMultipleSelection(false);
        featureDragController.setBehaviorDragStartSensitivity(5);
        featureDragController.addDragHandler(assemblageDragHandler);

        assemblageWidget.addStyleName("assemblage-detail-container");
        assemblageWidget.setSpacing(SPACING);
        boundaryPanel.add(assemblageWidget);
        boundaryPanel.addStyleName("assemblage-detail-root-absolute");

        // initialize our column drop controller
        HorizontalPanelDropController columnDropController = new HorizontalPanelDropController(
                assemblageWidget);
        assemblageSequenceDragController.registerDropController(columnDropController);

        Annotator.eventBus.addHandler(AssemblageViewEvent.TYPE, new AssemblageViewEventHandler() {
            @Override
            public void onAssemblageView(AssemblageViewEvent event) {
                GWT.log("HANDLING an assemblage event");
                AssemblageInfo assemblageInfo = getAssemblageInfo();
                GWT.log("ASSEMBLAGE event");
                MainPanel.updateGenomicViewerForAssemblage(assemblageInfo);
                GWT.log("HANDLED an assemblage event");
            }
        });
    }

    /**
     * Generate a sequence for each SequenceColumnPanel
     *
     * @return
     */
    public AssemblageInfo getAssemblageInfo() {
        AssemblageInfo assemblageInfo = new AssemblageInfo();
        AssemblageSequenceList assemblageSequenceList = new AssemblageSequenceList();
        assemblageInfo.setSequenceList(assemblageSequenceList);


        for (int sequenceIndex = 0; sequenceIndex < assemblageWidget.getWidgetCount(); ++sequenceIndex) {

            Widget sequenceWidget = assemblageWidget.getWidget(sequenceIndex);
            if (sequenceWidget instanceof AssemblageSequenceWidget) {
                AssemblageSequenceWidget assemblageSequenceWidget = ((AssemblageSequenceWidget) sequenceWidget);
                AssemblageSequence assemblageSequence = assemblageSequenceWidget.getAssemblageSequence();
                assemblageSequenceList.addSequence(assemblageSequence);

                int addedFeatureCount = 0;
                for (int featureAreaIndex = 0; featureAreaIndex < assemblageSequenceWidget.getWidgetCount(); ++featureAreaIndex) {
                    Widget featureWidget = assemblageSequenceWidget.getWidget(featureAreaIndex);
                    if (featureWidget instanceof AssemblageFeatureAreaWidget) {
                        AssemblageFeatureAreaWidget assemblageFeatureAreaWidget = (AssemblageFeatureAreaWidget) featureWidget;

                        for (int featureIndex = 0; featureIndex < assemblageFeatureAreaWidget.getWidgetCount(); ++featureIndex) {
                            // these would be sequences, but with a single feature
                            Widget innerFeatureWidget = assemblageFeatureAreaWidget.getWidget(featureIndex);
                            if (innerFeatureWidget instanceof AssemblageFeatureWidget) {
                                AssemblageFeatureWidget assemblageFeatureWidget = (AssemblageFeatureWidget) innerFeatureWidget;
                                SequenceFeatureInfo sequenceFeatureInfo = assemblageFeatureWidget.getSequenceFeatureInfo();
                                if (addedFeatureCount == 0) {
                                    assemblageSequence.setFeatureProperties(sequenceFeatureInfo);
                                    ++addedFeatureCount;
                                }
                                // we have to add a duplicate feature because of the way they are rendered
                                else {
                                    AssemblageSequence duplicateAssemblageSequence = assemblageSequence.deepCopy();
                                    duplicateAssemblageSequence.setFeatureProperties(sequenceFeatureInfo);
                                    ++addedFeatureCount;
                                    assemblageSequenceList.addSequence(duplicateAssemblageSequence);
                                }
                            }
                        }
                    }


                }
            }
        }

        // set name now
        String assemblageName = generateAssemblageName(assemblageInfo);
        assemblageInfo.setName(assemblageName);

        return assemblageInfo;
    }

    private String generateAssemblageName(AssemblageInfo assemblageInfo) {
        return assemblageInfo.getSequenceList().toString();
    }

    public void setAssemblageInfo(Set<AssemblageInfo> selectedObjects) {


        // we have to deregister before we clear the components, otherwise an error.
        featureDragController.unregisterDropControllers();
        assemblageWidget.clear();

        Map<String, AssemblageSequenceWidget> assemblageSequenceMap = new HashMap<>();
        Map<String, AssemblageFeatureAreaWidget> assemblageSequenceFeatureMap = new HashMap<>();

        for (AssemblageInfo assemblageInfo : selectedObjects) {

            AssemblageSequenceList sequenceArray = assemblageInfo.getSequenceList();
            for (int i = 0; i < sequenceArray.size(); i++) {
                AssemblageSequence assemblageSequence = sequenceArray.getSequence(i);
                Long start = assemblageSequence.getStart();
                Long end = assemblageSequence.getEnd();
                String sequenceName = assemblageSequence.getName();
                AssemblageSequenceWidget assemblageSequenceWidget = assemblageSequenceMap.get(sequenceName);

                // add a new sequence column
                if (assemblageSequenceWidget == null) {
                    assemblageSequenceWidget = new AssemblageSequenceWidget(assemblageSequence);
                    assemblageSequenceWidget.render(assemblageSequenceDragController, i);
                    assemblageWidget.add(assemblageSequenceWidget);


                    // stub the feature panel for adding features
                    AssemblageFeatureAreaWidget featurePanel = new AssemblageFeatureAreaWidget(assemblageSequence);
                    featurePanel.registerDropController(featureDragController);
                    assemblageSequenceWidget.add(featurePanel);


                    assemblageSequenceMap.put(sequenceName, assemblageSequenceWidget);
                    assemblageSequenceFeatureMap.put(sequenceName, featurePanel);
                }

                // extract the feature and add if it exists
                SequenceFeatureInfo sequenceFeatureInfo = assemblageSequence.getFeature();
                if (sequenceFeatureInfo != null) {
                    sequenceFeatureInfo.setStart(start);
                    sequenceFeatureInfo.setEnd(end);
                    sequenceFeatureInfo.setParentId(assemblageSequence.getName());
                    AssemblageFeatureAreaWidget thisFeaturePanel = assemblageSequenceFeatureMap.get(sequenceName);
                    thisFeaturePanel.addSequenceFeature(sequenceFeatureInfo, featureDragController);
                }

            }
        }

    }

}