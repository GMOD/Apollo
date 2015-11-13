package org.bbop.apollo.gwt.client;

import com.allen_sauer.gwt.dnd.client.PickupDragController;
import com.allen_sauer.gwt.dnd.client.drop.FlowPanelDropController;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.*;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.view.client.*;
import org.bbop.apollo.gwt.client.dto.bookmark.*;
import org.bbop.apollo.gwt.client.event.OrganismChangeEvent;
import org.bbop.apollo.gwt.client.event.OrganismChangeEventHandler;
import org.bbop.apollo.gwt.client.resources.TableResources;
import org.bbop.apollo.gwt.client.rest.BookmarkRestService;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;

import java.util.*;


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
    org.gwtbootstrap3.client.ui.ListBox referenceTrack;
    @UiField
    Button mergeButton;
    @UiField
    Button removeButton;
//    @UiField
//    Button copyButton;
    @UiField
    Button saveButton;
    @UiField
    FlowPanel dragAndDropPanel;
    @UiField
    AbsolutePanel absolutePanel;
    @UiField
    Button viewButton;

    final LoadingDialog loadingDialog;
    private PickupDragController dragController ;
    public static ListDataProvider<BookmarkInfo> dataProvider = new ListDataProvider<>();
    private static List<BookmarkInfo> bookmarkInfoList = dataProvider.getList();
    private MultiSelectionModel<BookmarkInfo> selectionModel = new MultiSelectionModel<BookmarkInfo>();

    public BookmarkPanel() {

        Widget rootElement = ourUiBinder.createAndBindUi(this);

        initWidget(rootElement);

        loadingDialog = new LoadingDialog("Processing ...",null, false);

        dragController = new PickupDragController(absolutePanel, true);
        FlowPanelDropController flowPanelDropController = new FlowPanelDropController( dragAndDropPanel);
        dragController.registerDropController(flowPanelDropController);
        dataGrid.setWidth("100%");
        foldType.addItem("None");
        foldType.addItem("Exon");
//        foldType.addItem("Transcript");

//        foldPadding.setText("50");
        foldPadding.setText("0");

        referenceTrack.addItem("Official Gene Set v3.2");


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


//        stubBackingData(10);
//        reload();

    }

    @UiHandler("removeButton")
    public void remove(ClickEvent clickEvent) {
        BookmarkRestService.removeBookmarks(new UpdateBookmarksCallback(),selectionModel.getSelectedSet().toArray(new BookmarkInfo[selectionModel.getSelectedSet().size()]));
        resetPanel();
    }

    private void resetPanel() {
        dragAndDropPanel.clear();
        absolutePanel.clear();
        absolutePanel.add(dragAndDropPanel);
    }

    /**
     * This methods views whatever is in the genome locator.
     * @param event
     */
    @UiHandler("viewButton")
    public void view(ClickEvent event){
        JSONObject merge1 = getBookmarksAsJson() ;
        MainPanel.updateGenomicViewerForLocation(merge1.toString().trim(),-1,-1);
    }

    private JSONObject getBookmarksAsJson() {
        JSONArray newArray = new JSONArray();
        for(int i = 0 ; i < dragAndDropPanel.getWidgetCount() ; i++){
            Widget widget = dragAndDropPanel.getWidget(i);
            String groupName = widget.getElement().getChild(1).getChild(0).getChild(0).getNodeValue();
            if(groupName.contains("(")){
                Integer startIndex = groupName.indexOf("(");
                Integer endIndex = groupName.indexOf(")");
                String featureString = groupName.substring(startIndex+1,endIndex-1);
                groupName = groupName.substring(0,startIndex);
                JSONObject featureObject = new JSONObject();
                featureObject.put(FeatureStringEnum.NAME.getValue(),new JSONString(groupName));
                JSONArray featuresArray = new JSONArray() ;
                String[] features = featureString.split(",");
                for(String feature : features){
                    JSONObject fI = new JSONObject();
                    fI.put(FeatureStringEnum.NAME.getValue(),new JSONString(feature));
                    featuresArray.set(featuresArray.size(),fI) ;
                }
                featureObject.put(FeatureStringEnum.FEATURES.getValue(),featuresArray);

                newArray.set(newArray.size(),featureObject);
            }
            else{
                JSONObject featureObject = new JSONObject();
                featureObject.put(FeatureStringEnum.NAME.getValue(),new JSONString(groupName));
                newArray.set(newArray.size(),featureObject);
            }
        }

        JSONObject genomicObject = new JSONObject();
        genomicObject.put("padding",new JSONNumber(Integer.parseInt(foldPadding.getText())));
        genomicObject.put("projection",new JSONString(foldType.getSelectedValue()));
        genomicObject.put("referenceTrack",new JSONString(referenceTrack.getSelectedValue()));
        genomicObject.put(FeatureStringEnum.SEQUENCE_LIST.getValue(),newArray);
        genomicObject.put("label",new JSONString(createLabelFromBookmark(genomicObject)));

        return genomicObject;
    }

    private String createLabelFromBookmark(JSONObject genomicObject) {
        String returnString = "";
        JSONArray sequenceArray = genomicObject.get(FeatureStringEnum.SEQUENCE_LIST.getValue()).isArray() ;
        for(int i = 0 ; i < sequenceArray.size() ; i++){
            returnString += sequenceArray.get(i).isObject().get(FeatureStringEnum.NAME.getValue()).isString().stringValue();
            if(i < sequenceArray.size()-1){
                returnString += "::";
            }
        }
        return returnString ;
    }

    /**
     * Typically just resaves the proper order
     * @param clickEvent
     */
    @UiHandler("saveButton")
    public void save(ClickEvent clickEvent) {
        Set<BookmarkInfo> bookmarkInfoSet = selectionModel.getSelectedSet();
        assert bookmarkInfoSet.size()==1;

        JSONObject bookmarkObjects = getBookmarksAsJson();
        BookmarkInfo bookmarkInfo =  BookmarkInfoConverter.convertJSONObjectToBookmarkInfo(bookmarkObjects);


        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                resetPanel();
                reload();
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Window.alert("error");
                Bootbox.alert("Failed to save: "+exception.getMessage());
            }
        };
        BookmarkRestService.addBookmark(requestCallback,bookmarkInfo);

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
            BookmarkSequenceList sequence1 = bookmarkInfo.getSequenceList();
            BookmarkSequenceList sequence2 = bookmarkInfo1.getSequenceList();
            if(sequence1==null){
//                sequence1 = sequence2 ;
                bookmarkInfo.setSequenceList(sequence2);
            }
            else
            if(sequence2==null){
//                sequence2 = sequence1;
                bookmarkInfo.setSequenceList(sequence1);
            }
            else{
                sequence1 = sequence1.merge(sequence2);
                bookmarkInfo.setSequenceList(sequence1);
            }

        }
//        bookmarkInfoList.removeAll(bookmarkInfoSet);
        bookmarkInfoList.add(bookmarkInfo);
//        reload();
    }

    @UiHandler(value = {"foldType", "foldPadding"})
    public void changeFoldType(ChangeEvent changeEvent){
        view(null);
    }

    private void setBookmarkInfo(Set<BookmarkInfo> selectedObject) {
        if (selectedObject.size() == 0) {
            mergeButton.setText("Combine");
            removeButton.setText("Remove ");
            saveButton.setText("Save");
            mergeButton.setEnabled(false);
//            copyButton.setEnabled(false);
            removeButton.setEnabled(false);
            saveButton.setEnabled(false);
            viewButton.setEnabled(false);
        } else if (selectedObject.size() == 1) {
            mergeButton.setText("Combine");
            removeButton.setText("Remove");
            saveButton.setText("Save");
            mergeButton.setEnabled(false);
//            copyButton.setEnabled(true);
            removeButton.setEnabled(true);
            if(selectedObject.iterator().next().getSequenceList().size()>1){
                saveButton.setEnabled(true);
            }
            else{
                saveButton.setEnabled(false);
            }
            viewButton.setEnabled(true);
        }
        // multiple
        else {
            mergeButton.setText("Combine: " + selectedObject.size());
            removeButton.setText("Remove: " + selectedObject.size());
            saveButton.setText("Save");
            mergeButton.setEnabled(true);
//            copyButton.setEnabled(false);
            removeButton.setEnabled(true);
            saveButton.setEnabled(false);
            viewButton.setEnabled(true);
        }

        dragAndDropPanel.clear();

        for (BookmarkInfo bookmarkInfo : selectedObject) {

            BookmarkSequenceList sequenceArray = bookmarkInfo.getSequenceList();
            for(int i = 0 ; i < sequenceArray.size() ; i++){
                BookmarkSequence sequenceObject = sequenceArray.getSequence(i);
                String name = sequenceObject.getName();
                SequenceFeatureList sequenceFeatureList = sequenceObject.getFeatures();
                for(int j = 0 ;sequenceFeatureList!=null &&  j < sequenceFeatureList.size() ; j++){
                    SequenceFeatureInfo sequenceFeatureInfo = sequenceFeatureList.getFeature(j) ;
                     name += "("+sequenceFeatureInfo.getName()+")";
                }
                FocusPanel focusPanel = new FocusPanel();

                FlowPanel flowPanel = new FlowPanel();
                focusPanel.setStyleName("demo-FlowPanelExample-draggable");
                focusPanel.add(flowPanel);

                HTML label = new HTML(name);
                label.setStyleName("demo-FlowPanelExample-label");
                HTML spacer = new HTML(" ");
                flowPanel.add(label);
                flowPanel.add(spacer);

                dragController.makeDraggable(focusPanel);
                dragAndDropPanel.add(focusPanel);
            }
        }

    }

    private class UpdateBookmarksCallback implements RequestCallback{
        @Override
        public void onResponseReceived(Request request, Response response) {
            GWT.log(response.getText());
            JSONArray jsonValue = JSONParser.parseStrict(response.getText()).isArray();
            bookmarkInfoList.clear();

            // add to bookmarkInfo list
            for(int i = 0 ; jsonValue!=null && i < jsonValue.size() ; i++){
                JSONObject jsonObject = jsonValue.get(i).isObject() ;
                BookmarkInfo bookmarkInfo = BookmarkInfoConverter.convertJSONObjectToBookmarkInfo(jsonObject);
                bookmarkInfoList.add(bookmarkInfo);
            }

            loadingDialog.hide();
        }

        @Override
        public void onError(Request request, Throwable exception) {
            loadingDialog.hide();
            new ErrorDialog("Error","There was an error: "+exception,true,true);
        }
    }

    public void reload() {
        BookmarkRestService.loadBookmarks(new UpdateBookmarksCallback());
        dataGrid.setVisibleRangeAndClearData(dataGrid.getVisibleRange(), true);
        dataGrid.redraw();
    }

    public void addBookmark(RequestCallback requestCallback,BookmarkInfo... bookmarkInfoCollection) {
        BookmarkRestService.addBookmark(requestCallback,bookmarkInfoCollection);
    }

}
