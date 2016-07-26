package org.bbop.apollo.gwt.client;

import com.allen_sauer.gwt.dnd.client.PickupDragController;
import com.allen_sauer.gwt.dnd.client.drop.FlowPanelDropController;
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
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.MultiSelectionModel;
import com.google.gwt.view.client.SelectionChangeEvent;
import org.bbop.apollo.gwt.client.dto.bookmark.*;
import org.bbop.apollo.gwt.client.event.OrganismChangeEvent;
import org.bbop.apollo.gwt.client.event.OrganismChangeEventHandler;
import org.bbop.apollo.gwt.client.resources.TableResources;
import org.bbop.apollo.gwt.client.rest.BookmarkRestService;
import org.bbop.apollo.gwt.shared.ColorGenerator;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;
import org.gwtbootstrap3.client.ui.*;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.constants.ButtonType;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;

import java.util.*;


/**
 * Created by Nathan Dunn on 12/16/14.
 */
public class BookmarkPanel extends Composite {

    interface BookmarkUiBinder extends UiBinder<Widget, BookmarkPanel> {
    }

    private static BookmarkUiBinder ourUiBinder = GWT.create(BookmarkUiBinder.class);


    private DataGrid.Resources tablecss = GWT.create(TableResources.TableCss.class);
    @UiField(provided = true)
    DataGrid<BookmarkInfo> dataGrid = new DataGrid<BookmarkInfo>(1000, tablecss);
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
//    @UiField
    Input paddingForm;
    @UiField
    Button clearButton;
//    @UiField
//    Button goButton;

    final LoadingDialog loadingDialog;
    private PickupDragController dragController;
    public static ListDataProvider<BookmarkInfo> dataProvider = new ListDataProvider<>();

    // TODO: probably a more clever way to do this
    private static List<BookmarkInfo> bookmarkInfoList = dataProvider.getList();
    private static Map<String,BookmarkInfo> bookmarkInfoMap = new HashMap<>();

    private MultiSelectionModel<BookmarkInfo> selectionModel = new MultiSelectionModel<BookmarkInfo>();

    public BookmarkPanel() {
        Widget rootElement = ourUiBinder.createAndBindUi(this);

        initWidget(rootElement);

        loadingDialog = new LoadingDialog("Processing ...", null, false);

        dragController = new PickupDragController(absolutePanel, true);
        FlowPanelDropController flowPanelDropController = new BookmarkFlowPanelDropController(dragAndDropPanel);
        dragController.registerDropController(flowPanelDropController);
        dataGrid.setWidth("100%");
        // Set the message to display when the table is empty.
        // fix selected style: http://comments.gmane.org/gmane.org.google.gwt/70747
        dataGrid.setEmptyTableWidget(new Label("No bookmarks!"));

        TextColumn<BookmarkInfo> nameColumn = new TextColumn<BookmarkInfo>() {
            @Override
            public String getValue(BookmarkInfo bookmarkInfo) {
                return bookmarkInfo.getName();
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

    @UiHandler("clearButton")
    public void clear(ClickEvent clickEvent){
        BookmarkRestService.clearBookmarkCache();
    }

    @UiHandler("removeButton")
    public void remove(ClickEvent clickEvent) {
        BookmarkRestService.removeBookmarks(new UpdateBookmarksCallback(), selectionModel.getSelectedSet().toArray(new BookmarkInfo[selectionModel.getSelectedSet().size()]));
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
        JSONObject merge1 = getBookmarkPanelAsJson();
        MainPanel.updateGenomicViewerForBookmark(merge1.toString().trim(), -1l, -1l);
    }

    private JSONObject getBookmarkPanelAsJson() {
        JSONArray sequenceList = new JSONArray();
        JSONObject bookmarkObject = new JSONObject();
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
            BookmarkInfo selectedBookmarkInfo = bookmarkInfoMap.get(groupName);
            if(selectedBookmarkInfo!=null){
                sequenceObject.put(FeatureStringEnum.START.getValue(),new JSONNumber(selectedBookmarkInfo.getSequenceList().getSequence(0).getStart()));
                sequenceObject.put(FeatureStringEnum.END.getValue(),new JSONNumber(selectedBookmarkInfo.getSequenceList().getSequence(0).getEnd()));
                sequenceList.set(sequenceList.size(), sequenceObject);
                if(i==0){
                    start = selectedBookmarkInfo.getStart();
                }
                end += selectedBookmarkInfo.getEnd();
            }
            else{
                sequenceObject.put(FeatureStringEnum.NAME.getValue(),new JSONString(groupName));
                sequenceList.set(sequenceList.size(), sequenceObject);
            }
        }


        bookmarkObject.put(FeatureStringEnum.SEQUENCE_LIST.getValue(), sequenceList);
        bookmarkObject.put(FeatureStringEnum.START.getValue(),new JSONNumber(start));
        bookmarkObject.put(FeatureStringEnum.END.getValue(),new JSONNumber(end));
        bookmarkObject.put("label", new JSONString(createLabelFromBookmark(bookmarkObject)));

        // TODO: get fro the UI
//        String paddingValue = paddingForm.getValue();
//        Integer padding = paddingValue !=null ? Integer.parseInt(paddingValue) : 0 ;
//        bookmarkObject.put("padding", new JSONNumber(padding));
        return bookmarkObject;
    }

    private String createLabelFromBookmark(JSONObject genomicObject) {
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
        Set<BookmarkInfo> bookmarkInfoSet = selectionModel.getSelectedSet();
        assert bookmarkInfoSet.size() == 1;

        JSONObject bookmarkObjects = getBookmarkPanelAsJson();
        BookmarkInfo bookmarkInfo = BookmarkInfoConverter.convertJSONObjectToBookmarkInfo(bookmarkObjects);


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
        BookmarkRestService.addBookmark(requestCallback, bookmarkInfo);

    }

    @UiHandler("mergeButton")
    public void merge(ClickEvent clickEvent) {
        BookmarkInfo bookmarkInfo = new BookmarkInfo();
        Set<BookmarkInfo> bookmarkInfoSet = selectionModel.getSelectedSet();
        // merge rule 1 . . . take largest padding
        // merge rule 2 . . . take exon -> transcript -> none
        long start = 0,end = 0 ;
        for (BookmarkInfo bookmarkInfo1 : bookmarkInfoSet) {
            end += bookmarkInfo1.getEnd();
            Integer padding = bookmarkInfo.getPadding();
            String type = bookmarkInfo.getType();
            bookmarkInfo.setPadding(padding == null || bookmarkInfo1.getPadding() > padding ? bookmarkInfo1.getPadding() : padding);
            bookmarkInfo.setType(type == null ? bookmarkInfo1.getType() : type);

            // combine the JSONArray now
            BookmarkSequenceList sequence1 = new BookmarkSequenceList(bookmarkInfo.getSequenceList());
            BookmarkSequenceList sequence2 = new BookmarkSequenceList(bookmarkInfo1.getSequenceList());
            if (sequence1 == null) {
//                sequence1 = sequence2 ;
                bookmarkInfo.setSequenceList(sequence2);
            } else if (sequence2 == null) {
//                sequence2 = sequence1;
                bookmarkInfo.setSequenceList(sequence1);
            } else {
                sequence1 = sequence1.merge(sequence2);
                bookmarkInfo.setSequenceList(sequence1);
            }

        }
        bookmarkInfo.setStart(start);
        bookmarkInfo.setEnd(end);

        addBookmarkLocally(bookmarkInfo);
    }

    private void clearBookmarkLocally(){
        bookmarkInfoMap.clear();
        bookmarkInfoList.clear();
    }

    private void addBookmarkLocally(BookmarkInfo bookmarkInfo) {
        List<BookmarkInfo> bookmarkInfos = new ArrayList<>();
        bookmarkInfos.add(bookmarkInfo);
        addBookmarkLocally(bookmarkInfos);
    }

    private void addBookmarkLocally(List<BookmarkInfo> bookmarkInfos) {
        for(BookmarkInfo bookmarkInfo : bookmarkInfos){
            bookmarkInfoMap.put(bookmarkInfo.getName(),bookmarkInfo);
            bookmarkInfoList.add(bookmarkInfo);
        }
    }


    private void setBookmarkInfo(Set<BookmarkInfo> selectedObjects) {
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

        for (BookmarkInfo bookmarkInfo : selectedObjects) {

            BookmarkSequenceList sequenceArray = bookmarkInfo.getSequenceList();
            for (int i = 0; i < sequenceArray.size(); i++) {
                BookmarkSequence sequenceObject = sequenceArray.getSequence(i);
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
                FocusPanel bookmarkWrapperFocusPanel = new FocusPanel();
                bookmarkWrapperFocusPanel.setStyleName("bookmark-FlowPanel-draggable");
                bookmarkWrapperFocusPanel.getElement().getStyle().setBackgroundColor(ColorGenerator.getColorForIndex(i));

                FlowPanel bookmarkObjectPanel = new FlowPanel();
                bookmarkWrapperFocusPanel.add(bookmarkObjectPanel);

                HTML label = new HTML(name);
                label.setStyleName("bookmark-FlowPanel-label");
//                label.getElement().getStyle().setColor(ColorGenerator.getColorForIndex(i));
                HTML spacer = new HTML(" ");
                bookmarkObjectPanel.add(label);
                bookmarkObjectPanel.add(spacer);

                dragController.makeDraggable(bookmarkWrapperFocusPanel);
                dragAndDropPanel.add(bookmarkWrapperFocusPanel);
            }
        }


    }

    private class UpdateBookmarksCallback implements RequestCallback {
        @Override
        public void onResponseReceived(Request request, Response response) {
            JSONArray jsonValue = JSONParser.parseStrict(response.getText()).isArray();
            clearBookmarkLocally();

            // add to bookmarkInfo list
            for (int i = 0; jsonValue != null && i < jsonValue.size(); i++) {
                JSONObject jsonObject = jsonValue.get(i).isObject();
                BookmarkInfo bookmarkInfo = BookmarkInfoConverter.convertJSONObjectToBookmarkInfo(jsonObject);
                addBookmarkLocally(bookmarkInfo);
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
        BookmarkRestService.loadBookmarks(new UpdateBookmarksCallback());
        dataGrid.setVisibleRangeAndClearData(dataGrid.getVisibleRange(), true);
        dataGrid.redraw();
    }

    public void addBookmark(RequestCallback requestCallback, BookmarkInfo... bookmarkInfoCollection) {
        BookmarkRestService.addBookmark(requestCallback, bookmarkInfoCollection);
    }

    @UiHandler("searchBox")
    public void searchForBookmark(KeyUpEvent keyUpEvent) {
        BookmarkRestService.searchBookmarks(new SearchAndUpdateBookmarksCallback(), searchBox.getText());
        dataGrid.setVisibleRangeAndClearData(dataGrid.getVisibleRange(), true);
        dataGrid.redraw();
    }

    private class SearchAndUpdateBookmarksCallback implements RequestCallback {
        @Override
        public void onResponseReceived(Request request, Response response) {
            JSONArray jsonValue = JSONParser.parseStrict(response.getText()).isArray();
            clearBookmarkLocally();

            // adding bookmarks from response
            addBookmarkLocally(BookmarkInfoConverter.convertFromJsonArray(jsonValue));
            loadingDialog.hide();
        }

        @Override
        public void onError(Request request, Throwable exception) {
            loadingDialog.hide();
            new ErrorDialog("Error", "There was an error: " + exception, true, true);
        }
    }


}
