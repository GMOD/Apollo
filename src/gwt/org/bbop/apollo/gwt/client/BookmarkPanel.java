package org.bbop.apollo.gwt.client;

import com.allen_sauer.gwt.dnd.client.PickupDragController;
import com.allen_sauer.gwt.dnd.client.drop.DropController;
import com.allen_sauer.gwt.dnd.client.drop.FlowPanelDropController;
import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Random;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.view.client.*;
import org.bbop.apollo.gwt.client.dto.BookmarkInfo;
import org.bbop.apollo.gwt.client.event.OrganismChangeEvent;
import org.bbop.apollo.gwt.client.event.OrganismChangeEventHandler;
import org.bbop.apollo.gwt.client.resources.TableResources;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;
import org.gwtbootstrap3.client.ui.*;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.TextBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;


/**
 * Created by Nathan Dunn on 12/16/14.
 */
public class BookmarkPanel extends Composite {
    interface BookmarkUiBinder extends UiBinder<Widget, BookmarkPanel> {
    }

    private static BookmarkUiBinder ourUiBinder = GWT.create(BookmarkUiBinder.class);


//    @UiField
//    static TextBox nameSearchBox;
//    @UiField
//    HTML trackName;
//    @UiField
//    HTML trackType;
//    @UiField
//    HTML trackCount;
//    @UiField
//    HTML trackDensity;

    private DataGrid.Resources tablecss = GWT.create(TableResources.TableCss.class);
    @UiField(provided = true)
    DataGrid<BookmarkInfo> dataGrid = new DataGrid<BookmarkInfo>(1000, tablecss);
    @UiField
    SplitLayoutPanel layoutPanel;
    //    @UiField
//    Tree optionTree;
    @UiField
    ListBox foldType;
    @UiField
    TextBox foldPadding;
    @UiField
    TextBox referenceTrack;
    @UiField
    Button mergeButton;
    @UiField
    Button removeButton;
    @UiField
    Button copyButton;
    @UiField
    Button applyButton;
    @UiField
    FlowPanel dragAndDropPanel;
    @UiField
    AbsolutePanel absolutePanel;


    private PickupDragController dragController ;
    public static ListDataProvider<BookmarkInfo> dataProvider = new ListDataProvider<>();
    private static List<BookmarkInfo> bookmarkInfoList = dataProvider.getList();
    private MultiSelectionModel<BookmarkInfo> selectionModel = new MultiSelectionModel<BookmarkInfo>();

    public BookmarkPanel() {

        Widget rootElement = ourUiBinder.createAndBindUi(this);

        initWidget(rootElement);

        dragController = new PickupDragController(absolutePanel, true);
        FlowPanelDropController flowPanelDropController = new FlowPanelDropController( dragAndDropPanel);
        dragController.registerDropController(flowPanelDropController);
        dataGrid.setWidth("100%");
        foldType.addItem("None");
        foldType.addItem("Exon");
        foldType.addItem("Transcript");

        foldPadding.setText("50");

        referenceTrack.setText("Official Gene Set v3.2");


        // Set the message to display when the table is empty.
        // fix selected style: http://comments.gmane.org/gmane.org.google.gwt/70747
        dataGrid.setEmptyTableWidget(new Label("No bookmarks!"));

        TextColumn<BookmarkInfo> nameColumn = new TextColumn<BookmarkInfo>() {
            @Override
            public String getValue(BookmarkInfo track) {
                return track.getName();
            }
        };
        nameColumn.setSortable(true);


        dataGrid.addColumn(nameColumn, "Name");
        dataGrid.setSelectionModel(selectionModel);
        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                setBookmarkInfo(selectionModel.getSelectedSet());
            }
        });

        dataProvider.addDataDisplay(dataGrid);


        ColumnSortEvent.ListHandler<BookmarkInfo> sortHandler = new ColumnSortEvent.ListHandler<BookmarkInfo>(bookmarkInfoList);
        dataGrid.addColumnSortHandler(sortHandler);

        sortHandler.setComparator(nameColumn, new Comparator<BookmarkInfo>() {
            @Override
            public int compare(BookmarkInfo o1, BookmarkInfo o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });


        Annotator.eventBus.addHandler(OrganismChangeEvent.TYPE, new OrganismChangeEventHandler() {
            @Override
            public void onOrganismChanged(OrganismChangeEvent authenticationEvent) {
                dataGrid.setLoadingIndicator(new Label("Loading..."));
                dataGrid.setEmptyTableWidget(new Label("Loading..."));
//                bookmarkInfoList.clear();
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

        stubBackingData(10);
        reload();

    }

    @UiHandler("removeButton")
    public void remove(ClickEvent clickEvent) {
        bookmarkInfoList.removeAll(selectionModel.getSelectedSet());
        dragAndDropPanel.clear();
    }

    @UiHandler("mergeButton")
    public void merge(ClickEvent clickEvent) {
        BookmarkInfo bookmarkInfo = new BookmarkInfo();
        Set<BookmarkInfo> bookmarkInfoSet = selectionModel.getSelectedSet();
        // merge rule 1 . . . take largest padding
        // merge rule 2 . . . take exon -> transcript -> none
        for (BookmarkInfo bookmarkInfo1 : bookmarkInfoSet) {
            Integer padding = bookmarkInfo.getPadding();
            String type = bookmarkInfo.getType();
            bookmarkInfo.setPadding(padding == null || bookmarkInfo1.getPadding() > padding ? bookmarkInfo1.getPadding() : padding);
            bookmarkInfo.setType(type == null ? bookmarkInfo1.getType() : type);

            // combine the JSONArray now
//            JSONArray jsonArray = bookmarkInfo.getSequenceList();
//            if(jsonArray==null){
//                bookmarkInfo.setSequenceList(jsonArray);
//            }
//            else{
            JSONArray sequence1 = bookmarkInfo.getSequenceList();
            JSONArray sequence2 = bookmarkInfo1.getSequenceList();
            if(sequence1==null){
                sequence1 = sequence2 ;
            }
            else{
                // add all fo the elements between 1 and 2 and put back into 1
                for (int i = 0; i < sequence2.size(); i++) {
                    sequence1.set(sequence1.size(), sequence2.get(i));
                }
            }

            bookmarkInfo.setSequenceList(sequence1);
//            }

        }
        bookmarkInfoList.removeAll(bookmarkInfoSet);
        bookmarkInfoList.add(bookmarkInfo);
//        reload();
    }

    private void setBookmarkInfo(Set<BookmarkInfo> selectedObject) {
        if (selectedObject.size() == 0) {
            mergeButton.setText("Merge");
            removeButton.setText("Remove ");
            applyButton.setText("Apply ");
            mergeButton.setEnabled(false);
            copyButton.setEnabled(false);
            removeButton.setEnabled(false);
            applyButton.setEnabled(false);
        } else if (selectedObject.size() == 1) {
            mergeButton.setText("Merge");
            removeButton.setText("Remove");
            applyButton.setText("Apply");
            mergeButton.setEnabled(false);
            copyButton.setEnabled(true);
            removeButton.setEnabled(true);
            applyButton.setEnabled(true);
        }
        // multiple
        else {
            mergeButton.setText("Merge: " + selectedObject.size());
            removeButton.setText("Remove: " + selectedObject.size());
            applyButton.setText("Apply: " + selectedObject.size());
            mergeButton.setEnabled(true);
            copyButton.setEnabled(false);
            removeButton.setEnabled(true);
            applyButton.setEnabled(true);
        }

        dragAndDropPanel.clear();


//        DropController pickupDropController = new FlowPanelDropController(dragAndDropPanel);
        for (BookmarkInfo bookmarkInfo : selectedObject) {

//            HTML label = new HTML("Draggable&nbsp;#" + i);
            JSONArray sequenceArray = bookmarkInfo.getSequenceList();
            for(int i = 0 ; i < sequenceArray.size() ; i++){
                JSONObject sequenceObject = sequenceArray.get(i).isObject();
                String name = sequenceObject.get(FeatureStringEnum.NAME.getValue()).isString().stringValue();
                if(sequenceObject.containsKey(FeatureStringEnum.FEATURES.getValue())){
                    JSONArray featureArray = sequenceObject.get(FeatureStringEnum.FEATURES.getValue()).isArray();
                    for(int j = 0 ; j < featureArray.size() ; j++){
                        name += "("+featureArray.get(j).isObject().get(FeatureStringEnum.NAME.getValue()).isString().stringValue()+")";
                    }
                }
                FocusPanel focusPanel = new FocusPanel();
//                focusPanel.setStyleName(CSS_DEMO_FLOW_PANEL_EXAMPLE_DRAGGABLE);

                FlowPanel flowPanel = new FlowPanel();
                focusPanel.setStyleName("demo-FlowPanelExample-draggable");
                focusPanel.add(flowPanel);

//                HTML label = new HTML(bookmarkInfo.getName());
                HTML label = new HTML(name);
                label.setStyleName("demo-FlowPanelExample-label");
                HTML spacer = new HTML(" ");
//                label.addStyleName(CSS_DEMO_FLOW_PANEL_EXAMPLE_LABEL);
                flowPanel.add(label);
                flowPanel.add(spacer);

                dragController.makeDraggable(focusPanel);
                dragAndDropPanel.add(focusPanel);
            }
        }

    }

    private void stubBackingData(int number){
//        bookmarkInfoList.clear();
        for (int i = 0; i < number; i++) {
            BookmarkInfo bookmarkInfo = new BookmarkInfo();
            JSONArray jsonArray = new JSONArray();
            JSONObject sequenceObject = new JSONObject();
            sequenceObject.put(FeatureStringEnum.NAME.getValue(), new JSONString("Group" + i % 4));
            if (i % 2 == 0) {
                // add a feature array sometimes
                JSONArray featureArray = new JSONArray();

                JSONObject featureObject = new JSONObject();
                featureObject.put(FeatureStringEnum.NAME.getValue(), new JSONString("GA-1231A" + i));
                featureArray.set(featureArray.size(), featureObject);


                sequenceObject.put(FeatureStringEnum.FEATURES.getValue(), featureArray);
            }
            jsonArray.set(jsonArray.size(), sequenceObject);
            bookmarkInfo.setSequenceList(jsonArray);

            bookmarkInfoList.add(bookmarkInfo);
        }
        reload();
    }

    public void reload() {
        dataGrid.setVisibleRangeAndClearData(dataGrid.getVisibleRange(), true);
        dataGrid.redraw();
    }

}
