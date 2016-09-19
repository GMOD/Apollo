package org.bbop.apollo.gwt.client;

import com.allen_sauer.gwt.dnd.client.PickupDragController;
import com.allen_sauer.gwt.dnd.client.drop.FlowPanelDropController;
import com.google.gwt.cell.client.*;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.*;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.MultiSelectionModel;
import com.google.gwt.view.client.SelectionChangeEvent;
import org.bbop.apollo.gwt.client.dto.assemblage.*;
import org.bbop.apollo.gwt.client.event.OrganismChangeEvent;
import org.bbop.apollo.gwt.client.event.OrganismChangeEventHandler;
import org.bbop.apollo.gwt.client.resources.TableResources;
import org.bbop.apollo.gwt.client.rest.AssemblageRestService;
import org.bbop.apollo.gwt.shared.ColorGenerator;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;
import org.gwtbootstrap3.client.ui.*;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.RadioButton;
import org.gwtbootstrap3.client.ui.constants.ButtonType;
import org.gwtbootstrap3.client.ui.constants.IconType;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;

import java.util.*;

import static org.bbop.apollo.gwt.client.AssemblageInfoService.buildDescriptionWidget;


/**
 * Created by Nathan Dunn on 12/16/14.
 */
public class AssemblagePanel extends Composite {

    interface AssemblageUiBinder extends UiBinder<Widget, AssemblagePanel> {
    }

    private static AssemblageUiBinder ourUiBinder = GWT.create(AssemblageUiBinder.class);


    private DataGrid.Resources tablecss = GWT.create(TableResources.TableCss.class);
    @UiField(provided = true)
    DataGrid<AssemblageInfo> dataGrid = new DataGrid<AssemblageInfo>(1000, tablecss);
    @UiField
    DockLayoutPanel layoutPanel;
    @UiField
    Button mergeButton;
    @UiField
    Button removeButton;
    @UiField
    Button saveButton;
    @UiField
    FlowPanel dragAndDropPanel;
    @UiField
    AbsolutePanel absolutePanel;
    @UiField
    Button viewButton;
    @UiField
    org.gwtbootstrap3.client.ui.TextBox searchBox;
    @UiField
    Button deleteButton;
    @UiField
    RadioButton showAllAssemblagesButton;
    @UiField
    RadioButton showOnlyFeatureButton;
    @UiField
    RadioButton showOnlyCombinedButton;
    @UiField
    RadioButton showOnlyScaffoldButton;

    final LoadingDialog loadingDialog;
    private PickupDragController dragController;
    public static ListDataProvider<AssemblageInfo> dataProvider = new ListDataProvider<>();

    // TODO: probably a more clever way to do this
    private static List<AssemblageInfo> assemblageInfoList = dataProvider.getList();
    private static Map<String,AssemblageInfo> assemblageInfoMap = new HashMap<>();

    private MultiSelectionModel<AssemblageInfo> selectionModel = new MultiSelectionModel<AssemblageInfo>();

    public AssemblagePanel() {
        Widget rootElement = ourUiBinder.createAndBindUi(this);

        initWidget(rootElement);


        loadingDialog = new LoadingDialog("Processing ...", null, false);

        dragController = new PickupDragController(absolutePanel, true);
        FlowPanelDropController flowPanelDropController = new AssemblageFlowPanelDropController(dragAndDropPanel);
        dragController.registerDropController(flowPanelDropController);
        dataGrid.setWidth("100%");
        // Set the message to display when the table is empty.
        // fix selected style: http://comments.gmane.org/gmane.org.google.gwt/70747
        dataGrid.setEmptyTableWidget(new Label("No assemblages!"));

        Column<AssemblageInfo,String> nameColumn = new Column<AssemblageInfo,String>(new EditTextCell()) {
            @Override
            public String getValue(AssemblageInfo assemblageInfo) {
                String name = assemblageInfo.getName();
                if(name == null || name.startsWith("Unnamed")){
                    return "Unnamed";
                }
                return  name ;
            }
        };
        nameColumn.setFieldUpdater(new FieldUpdater<AssemblageInfo, String>() {
            @Override
            public void update(int index, AssemblageInfo object, String value) {
                // Called when the user changes the value.
                if(!value.equals(object.getName())){
                    object.setName(value);
                    AssemblageRestService.saveAssemblage(object);
                }
            }
        });

        nameColumn.setSortable(true);
        TextColumn<AssemblageInfo> lengthColumn = new TextColumn<AssemblageInfo>() {
            @Override
            public String getValue(AssemblageInfo assemblageInfo) {
                Long length = assemblageInfo.getLength();
                return length == null ? "N/A" : length.toString() ;
            }
        };
        lengthColumn.setSortable(true);

        final Column<AssemblageInfo, SafeHtml> descriptionColumn = new Column<AssemblageInfo, SafeHtml>(new SafeHtmlCell()) {
            @Override
            public SafeHtml getValue(AssemblageInfo assemblageInfo) {
                Widget widget = buildDescriptionWidget(assemblageInfo);
                return SafeHtmlUtils.fromTrustedString(widget.getElement().getInnerHTML());
            }
        } ;

        descriptionColumn.setSortable(false);


        dataGrid.addColumn(nameColumn, "Name");
        dataGrid.addColumn(lengthColumn, "Length");
        dataGrid.addColumn(descriptionColumn, "Description");

        dataGrid.setSelectionModel(selectionModel);
        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                setAssemblageInfo(selectionModel.getSelectedSet());
            }
        });

        dataProvider.addDataDisplay(dataGrid);


        ColumnSortEvent.ListHandler<AssemblageInfo> sortHandler = new ColumnSortEvent.ListHandler<AssemblageInfo>(assemblageInfoList);
        dataGrid.addColumnSortHandler(sortHandler);

        sortHandler.setComparator(nameColumn, new Comparator<AssemblageInfo>() {
            @Override
            public int compare(AssemblageInfo o1, AssemblageInfo o2) {
                return o1.getDescription().compareTo(o2.getDescription());
            }
        });
        sortHandler.setComparator(lengthColumn, new Comparator<AssemblageInfo>() {
            @Override
            public int compare(AssemblageInfo o1, AssemblageInfo o2) {
                return o1.getLength().compareTo(o2.getLength());
            }
        });


        dataGrid.addDomHandler(new DoubleClickHandler() {
            @Override
            public void onDoubleClick(DoubleClickEvent event) {
                view(null);
            }
        }, DoubleClickEvent.getType());


        Annotator.eventBus.addHandler(OrganismChangeEvent.TYPE, new OrganismChangeEventHandler() {
            @Override
            public void onOrganismChanged(OrganismChangeEvent authenticationEvent) {
                dataGrid.setLoadingIndicator(new Label("Loading..."));
                dataGrid.setEmptyTableWidget(new Label("Loading..."));
                Scheduler.get().scheduleFixedDelay(new Scheduler.RepeatingCommand() {
                    @Override
                    public boolean execute() {
                        reload();
                        dataGrid.setEmptyTableWidget(new Label("No tracks found!"));
                        return false;
                    }
                }, 2000);
            }
        });

    }



    @UiHandler("deleteButton")
    public void delete(ClickEvent clickEvent){
        AssemblageRestService.removeAssemblage(new UpdateAssemblagesCallback(),dataProvider.getList().toArray(new AssemblageInfo[dataProvider.getList().size()]) );
        resetPanel();
    }


        @UiHandler("removeButton")
    public void remove(ClickEvent clickEvent) {
        AssemblageRestService.removeAssemblage(new UpdateAssemblagesCallback(), selectionModel.getSelectedSet().toArray(new AssemblageInfo[selectionModel.getSelectedSet().size()]));
        resetPanel();
    }

    private void resetPanel() {
        dragAndDropPanel.clear();
        absolutePanel.clear();
        absolutePanel.add(dragAndDropPanel);
    }

    /**
     * This methods views whatever is in the genome locator.
     *
     * @param event
     */
    @UiHandler("viewButton")
    public void view(ClickEvent event) {
        JSONObject merge1 = getAssemblagePanelAsJson();
        MainPanel.updateGenomicViewerForAssemblage(merge1.toString().trim(), -1l, -1l);
    }

    private JSONObject getAssemblagePanelAsJson() {
        JSONArray sequenceList = new JSONArray();
        JSONObject assemblageObject = new JSONObject();
        long start= 0,end = 0 ;
        for (int i = 0; i < dragAndDropPanel.getWidgetCount(); i++) {
            Widget widget = dragAndDropPanel.getWidget(i);
            String groupName = widget.getElement().getChild(1).getChild(0).getChild(0).getNodeValue();
            JSONObject sequenceObject = new JSONObject();
            // map the specific genes
            if (groupName.contains(" (")) {
                Integer startIndex = groupName.indexOf(" (");
                Integer endIndex = groupName.indexOf(")");
                String sequenceString= groupName.substring(startIndex + 2, endIndex );
                String featureString = groupName.substring(0, startIndex);
                JSONObject featureObject = new JSONObject();
                featureObject.put(FeatureStringEnum.NAME.getValue(), new JSONString(featureString));
                sequenceObject.put(FeatureStringEnum.NAME.getValue(), new JSONString(sequenceString));
                sequenceObject.put(FeatureStringEnum.FEATURE.getValue(),featureObject);
            } else {
                // map the entire scaffold
                sequenceObject.put(FeatureStringEnum.NAME.getValue(), new JSONString(groupName));
            }
            AssemblageInfo selectedAssemblageInfo = assemblageInfoMap.get(groupName);
            if(selectedAssemblageInfo !=null){
                sequenceObject.put(FeatureStringEnum.START.getValue(),new JSONNumber(selectedAssemblageInfo.getSequenceList().getSequence(0).getStart()));
                sequenceObject.put(FeatureStringEnum.END.getValue(),new JSONNumber(selectedAssemblageInfo.getSequenceList().getSequence(0).getEnd()));
                sequenceList.set(sequenceList.size(), sequenceObject);
                if(i==0){
                    start = selectedAssemblageInfo.getStart();
                }
                end += selectedAssemblageInfo.getEnd();
            }
            else{
                sequenceObject.put(FeatureStringEnum.NAME.getValue(),new JSONString(groupName));
                sequenceList.set(sequenceList.size(), sequenceObject);
            }
        }


        assemblageObject.put(FeatureStringEnum.SEQUENCE_LIST.getValue(), sequenceList);
        assemblageObject.put(FeatureStringEnum.START.getValue(),new JSONNumber(start));
        assemblageObject.put(FeatureStringEnum.END.getValue(),new JSONNumber(end));
        assemblageObject.put("label", new JSONString(createLabelFromAssemblage(assemblageObject)));

        return assemblageObject;
    }

    private String createLabelFromAssemblage(JSONObject genomicObject) {
        String returnString = "";
        JSONArray sequenceArray = genomicObject.get(FeatureStringEnum.SEQUENCE_LIST.getValue()).isArray();
        for (int i = 0; i < sequenceArray.size(); i++) {
            JSONObject sequenceObject = sequenceArray.get(i).isObject();
            if (sequenceObject.containsKey(FeatureStringEnum.FEATURE.getValue())) {
                JSONObject featureObject = sequenceObject.get(FeatureStringEnum.FEATURE.getValue()).isObject();
                returnString += featureObject.get(FeatureStringEnum.NAME.getValue()).isString().stringValue() ;
                returnString += "(";
            }
            returnString += sequenceObject.get(FeatureStringEnum.NAME.getValue()).isString().stringValue();
            if (sequenceObject.containsKey(FeatureStringEnum.FEATURE.getValue())) {
                returnString += ")";
            }

            if (i < sequenceArray.size() - 1) {
                returnString += "::";
            }
        }
        return returnString;
    }

    /**
     * Typically just resaves the proper order
     *
     * @param clickEvent
     */
    @UiHandler("saveButton")
    public void save(ClickEvent clickEvent) {
        Set<AssemblageInfo> assemblageInfoSet = selectionModel.getSelectedSet();
        assert assemblageInfoSet.size() == 1;

        JSONObject panelAsJson = getAssemblagePanelAsJson();
        AssemblageInfo assemblageInfo = AssemblageInfoConverter.convertJSONObjectToAssemblageInfo(panelAsJson);


        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                resetPanel();
                reload();
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Bootbox.alert("Failed to save: " + exception.getMessage());
            }
        };
        AssemblageRestService.addAssemblage(requestCallback, assemblageInfo);

    }

    @UiHandler("mergeButton")
    public void merge(ClickEvent clickEvent) {
        AssemblageInfo assemblageInfo = new AssemblageInfo();
        Set<AssemblageInfo> assemblageInfoSet = selectionModel.getSelectedSet();
        // merge rule 1 . . . take largest padding
        // merge rule 2 . . . take exon -> transcript -> none
        long start = 0,end = 0 ;
        for (AssemblageInfo assemblageInfo1 : assemblageInfoSet) {
            end += assemblageInfo1.getEnd();
            Integer padding = assemblageInfo.getPadding();
            String type = assemblageInfo.getType();
            assemblageInfo.setPadding(padding == null || assemblageInfo1.getPadding() > padding ? assemblageInfo1.getPadding() : padding);
            assemblageInfo.setType(type == null ? assemblageInfo1.getType() : type);

            // combine the JSONArray now
            AssemblageSequenceList sequence1 = new AssemblageSequenceList(assemblageInfo.getSequenceList());
            AssemblageSequenceList sequence2 = new AssemblageSequenceList(assemblageInfo1.getSequenceList());
            if (sequence1 == null) {
//                sequence1 = sequence2 ;
                assemblageInfo.setSequenceList(sequence2);
            } else if (sequence2 == null) {
//                sequence2 = sequence1;
                assemblageInfo.setSequenceList(sequence1);
            } else {
                sequence1 = sequence1.merge(sequence2);
                assemblageInfo.setSequenceList(sequence1);
            }

        }
        assemblageInfo.setStart(start);
        assemblageInfo.setEnd(end);

        addAssemblageLocally(assemblageInfo);
    }

    private void clearAssemblageLocally(){
        assemblageInfoMap.clear();
        assemblageInfoList.clear();
    }

    private void addAssemblageLocally(AssemblageInfo assemblageInfo) {
        List<AssemblageInfo> assemblageInfos = new ArrayList<>();
        assemblageInfos.add(assemblageInfo);
        addAssemblageLocally(assemblageInfos);
    }

    private void addAssemblageLocally(List<AssemblageInfo> assemblageInfos) {
        for(AssemblageInfo assemblageInfo : assemblageInfos){
            assemblageInfoMap.put(assemblageInfo.getDescription(), assemblageInfo);
            assemblageInfoList.add(assemblageInfo);
        }
    }

    public void setAssemableInfo(AssemblageInfo currentAssemblage) {
        selectionModel.clear();
        selectionModel.setSelected(currentAssemblage,true);
        Set<AssemblageInfo> assemblageInfos = new HashSet<>();
        assemblageInfos.add(currentAssemblage);
        setAssemblageInfo(assemblageInfos);
    }


    private void setAssemblageInfo(Set<AssemblageInfo> selectedObjects) {
        if (selectedObjects.size() == 0) {
            mergeButton.setText("Combine");
            removeButton.setText("Remove ");
            saveButton.setText("Save");
            mergeButton.setEnabled(false);
            removeButton.setEnabled(false);
            saveButton.setEnabled(false);
            viewButton.setEnabled(false);
        } else if (selectedObjects.size() == 1) {
            mergeButton.setText("Combine");
            removeButton.setText("Remove");
            saveButton.setText("Save");
            mergeButton.setEnabled(false);
            removeButton.setEnabled(true);
            if (selectedObjects.iterator().next().getSequenceList().size() > 1) {
                saveButton.setEnabled(true);
            } else {
                saveButton.setEnabled(false);
            }
            viewButton.setEnabled(true);
        }
        // multiple
        else {
            mergeButton.setText("Combine: " + selectedObjects.size());
            removeButton.setText("Remove: " + selectedObjects.size());
            saveButton.setText("Save");
            mergeButton.setEnabled(true);
            removeButton.setEnabled(true);
            saveButton.setEnabled(false);
            viewButton.setEnabled(true);
        }

        saveButton.setType(saveButton.isEnabled() ? ButtonType.PRIMARY : ButtonType.DEFAULT);

        dragAndDropPanel.clear();

        for (AssemblageInfo assemblageInfo : selectedObjects) {

            AssemblageSequenceList sequenceArray = assemblageInfo.getSequenceList();
            for (int i = 0; i < sequenceArray.size(); i++) {
                AssemblageSequence sequenceObject = sequenceArray.getSequence(i);
                String name = "";
                SequenceFeatureInfo sequenceFeatureInfo = sequenceObject.getFeature();
                if(sequenceFeatureInfo!=null){
                    name += sequenceFeatureInfo.getName();
                    name += " (";
                }
                name += sequenceObject.getName();

                if(sequenceFeatureInfo!=null){
                    name += ")";
                }
                FocusPanel focusPanel = new FocusPanel();
                focusPanel.setStyleName("assemblage-FlowPanel-draggable");
                focusPanel.getElement().getStyle().setBackgroundColor(ColorGenerator.getColorForIndex(i));

                FlowPanel assemblageObjectPanel = new FlowPanel();
                focusPanel.add(assemblageObjectPanel);

                HTML label = new HTML(name);
                label.setStyleName("assemblage-FlowPanel-label");
//                label.getElement().getStyle().setColor(ColorGenerator.getColorForIndex(i));
                HTML spacer = new HTML(" ");
                assemblageObjectPanel.add(label);
                assemblageObjectPanel.add(spacer);

                dragController.makeDraggable(focusPanel);
                dragAndDropPanel.add(focusPanel);
            }
        }


    }

    private class UpdateAssemblagesCallback implements RequestCallback {
        @Override
        public void onResponseReceived(Request request, Response response) {
            JSONArray jsonValue = JSONParser.parseStrict(response.getText()).isArray();
            clearAssemblageLocally();

            for (int i = 0; jsonValue != null && i < jsonValue.size(); i++) {
                JSONObject jsonObject = jsonValue.get(i).isObject();
                AssemblageInfo assemblageInfo = AssemblageInfoConverter.convertJSONObjectToAssemblageInfo(jsonObject);
                addAssemblageLocally(assemblageInfo);
            }

            loadingDialog.hide();
        }

        @Override
        public void onError(Request request, Throwable exception) {
            loadingDialog.hide();
            new ErrorDialog("Error", "There was an error: " + exception, true, true);
        }
    }

    public void reload() {
        AssemblageRestService.loadAssemblage(new UpdateAssemblagesCallback());
        dataGrid.setVisibleRangeAndClearData(dataGrid.getVisibleRange(), true);
        dataGrid.redraw();
    }

    public void addAssemblage(RequestCallback requestCallback, AssemblageInfo... assemblageInfoCollection) {
        AssemblageRestService.addAssemblage(requestCallback, assemblageInfoCollection);
    }

    @UiHandler("searchBox")
    public void searchForAssemblage(KeyUpEvent keyUpEvent) {
        AssemblageRestService.searchAssemblage(new SearchAndUpdateAssemblagesCallback(), searchBox.getText(),getFilter());
        dataGrid.setVisibleRangeAndClearData(dataGrid.getVisibleRange(), true);
        dataGrid.redraw();
    }

    private String getFilter() {
        if(showOnlyScaffoldButton.isActive()){
            return "Scaffold";
        }
        if(showOnlyCombinedButton.isActive()){
            return "Combined";
        }
        if(showOnlyFeatureButton.isActive()){
            return "Feature";
        }
        return "";
    }

    @UiHandler({"showOnlyScaffoldButton","showOnlyCombinedButton","showOnlyFeatureButton","showAllAssemblagesButton"})
    public void showOnlyScaffoldButtonClick(ClickEvent event) {
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                searchForAssemblage(null);
            }
        });
    }

    private class SearchAndUpdateAssemblagesCallback implements RequestCallback {
        @Override
        public void onResponseReceived(Request request, Response response) {
            JSONArray jsonValue = JSONParser.parseStrict(response.getText()).isArray();
            clearAssemblageLocally();

            // adding assemblages from response
            addAssemblageLocally(AssemblageInfoConverter.convertFromJsonArray(jsonValue));
            loadingDialog.hide();
        }

        @Override
        public void onError(Request request, Throwable exception) {
            loadingDialog.hide();
            new ErrorDialog("Error", "There was an error: " + exception, true, true);
        }
    }


}
