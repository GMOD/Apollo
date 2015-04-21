package org.bbop.apollo.gwt.client;

import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.http.client.*;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.json.client.*;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.*;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
//import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.bbop.apollo.gwt.client.demo.DataGenerator;
import org.bbop.apollo.gwt.client.dto.TrackInfo;
import org.bbop.apollo.gwt.client.event.ContextSwitchEvent;
import org.bbop.apollo.gwt.client.event.ContextSwitchEventHandler;
import org.bbop.apollo.gwt.client.event.OrganismChangeEvent;
import org.bbop.apollo.gwt.client.event.OrganismChangeEventHandler;
import org.bbop.apollo.gwt.client.resources.TableResources;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.TextBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
//import org.gwtbootstrap3.client.ui.gwt.DataGrid;

/**
 * Created by ndunn on 12/16/14.
 */
public class TrackPanel extends Composite {
    interface TrackUiBinder extends UiBinder<Widget, TrackPanel> {
    }

    private static TrackUiBinder ourUiBinder = GWT.create(TrackUiBinder.class);

    private String rootUrl;

    @UiField
    static TextBox nameSearchBox;
    @UiField
    HTML trackName;
    @UiField
    HTML trackType;
    @UiField
    HTML trackCount;
    @UiField
    HTML trackDensity;

    private DataGrid.Resources tablecss = GWT.create(TableResources.TableCss.class);
    @UiField(provided = true)
    DataGrid<TrackInfo> dataGrid = new DataGrid<TrackInfo>(100, tablecss);
    @UiField
    SplitLayoutPanel layoutPanel;
    @UiField
    Tree optionTree;


    public static ListDataProvider<TrackInfo> dataProvider = new ListDataProvider<>();
    private static List<TrackInfo> trackInfoList = new ArrayList<>();
    private static List<TrackInfo> filteredTrackInfoList = dataProvider.getList();

    private SingleSelectionModel<TrackInfo> singleSelectionModel = new SingleSelectionModel<TrackInfo>();

    private TrackInfo selectedTrackInfo = null;


    public TrackPanel() {
        Dictionary dictionary = Dictionary.getDictionary("Options");
        rootUrl = dictionary.get("rootUrl");
        exportStaticMethod();

        Widget rootElement = ourUiBinder.createAndBindUi(this);
        initWidget(rootElement);

        dataGrid.setWidth("100%");

        // Set the message to display when the table is empty.
        // fix selected style: http://comments.gmane.org/gmane.org.google.gwt/70747
        dataGrid.setEmptyTableWidget(new Label("No tracks!"));

        // TODO: on-click . . . if not Clicked
        Column<TrackInfo, Boolean> showColumn = new Column<TrackInfo, Boolean>(new CheckboxCell(true, false)) {


            @Override
            public Boolean getValue(TrackInfo employee) {
                return employee.getVisible();
            }
        };

        showColumn.setFieldUpdater(new FieldUpdater<TrackInfo, Boolean>() {
            /**
             * TODO: emulate . . underTrackList . . Create an external function in Annotrack to then call from here
             * a good example: http://www.springindepth.com/book/gwt-comet-gwt-dojo-cometd-spring-bayeux-jetty.html
             * uses DOJO publish mechanism (http://dojotoolkit.org/reference-guide/1.7/dojo/publish.html)

             * @param index
             * @param trackInfo
             * @param value
             */
            @Override
            public void update(int index, TrackInfo trackInfo, Boolean value) {
                JSONObject jsonObject = trackInfo.getPayload();
                trackInfo.setVisible(value);
                if (value) {
                    jsonObject.put("command", new JSONString("show"));
                } else {
                    jsonObject.put("command", new JSONString("hide"));
                }

                MainPanel.executeFunction("handleTrackVisibility", jsonObject.getJavaScriptObject());
            }
        });
        showColumn.setSortable(true);

        TextColumn<TrackInfo> nameColumn = new TextColumn<TrackInfo>() {
            @Override
            public String getValue(TrackInfo employee) {
                return employee.getName();
            }
        };
        nameColumn.setSortable(true);


//        TextColumn<TrackInfo> typeColumn = new TextColumn<TrackInfo>() {
//            @Override
//            public String getValue(TrackInfo employee) {
//                return employee.getType();
//            }
//        };
//        typeColumn.setSortable(true);


        dataGrid.addColumn(showColumn, "Show");
        dataGrid.addColumn(nameColumn, "Name");
//        dataGrid.addColumn(typeColumn, "Type");
        dataGrid.setColumnWidth(0, "10%");
        dataGrid.setSelectionModel(singleSelectionModel);
        singleSelectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                setTrackInfo(singleSelectionModel.getSelectedObject());
            }
        });


        dataProvider.addDataDisplay(dataGrid);

        Scheduler.get().scheduleDeferred(new Command() {
            public void execute() {
                reload();
            }
        });

        ColumnSortEvent.ListHandler<TrackInfo> sortHandler = new ColumnSortEvent.ListHandler<TrackInfo>(filteredTrackInfoList);
        dataGrid.addColumnSortHandler(sortHandler);

        sortHandler.setComparator(showColumn, new Comparator<TrackInfo>() {
            @Override
            public int compare(TrackInfo o1, TrackInfo o2) {
                if (o1.getVisible() == o2.getVisible()) return 0;
                if (o1.getVisible() && !o2.getVisible()) {
                    return 1;
                } else {
                    return -1;
                }
            }
        });
        sortHandler.setComparator(nameColumn, new Comparator<TrackInfo>() {
            @Override
            public int compare(TrackInfo o1, TrackInfo o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

    }

    private void setTrackInfo(TrackInfo selectedObject) {
        selectedTrackInfo = selectedObject;
        if (selectedTrackInfo == null) {
            trackName.setText("");
            trackType.setText("");
            optionTree.clear();
//            trackCount.setText("");
//            trackDensity.setText("");
        } else {
            trackName.setText(selectedTrackInfo.getName());
            trackType.setText(selectedTrackInfo.getType());
            optionTree.clear();
            JSONObject jsonObject = selectedTrackInfo.getPayload();
            setOptionDetails(jsonObject);
//            trackCount.setText(selectedTrackInfo.get);
//            trackDensity.setText("");
        }
    }

    private void setOptionDetails(JSONObject jsonObject) {
        for (String key : jsonObject.keySet()) {
            TreeItem treeItem = new TreeItem();
            treeItem.setHTML(generateHtmlFromObject(jsonObject, key));
            if (jsonObject.get(key).isObject() != null) {
                treeItem.addItem(generateTreeItem(jsonObject.get(key).isObject()));
            }
            optionTree.addItem(treeItem);
        }
    }

    private String generateHtmlFromObject(JSONObject jsonObject, String key) {
        if (jsonObject.get(key) == null) {
            return key;
        } else if (jsonObject.get(key).isObject() != null) {
            return key;
        } else {
            return "<b>" + key + "</b>: " + jsonObject.get(key).toString().replace("\\", "");
        }
    }

    private TreeItem generateTreeItem(JSONObject jsonObject) {
        TreeItem treeItem = new TreeItem();

        for (String key : jsonObject.keySet()) {
            treeItem.setHTML(generateHtmlFromObject(jsonObject, key));
            if (jsonObject.get(key).isObject() != null) {
                treeItem.addItem(generateTreeItem(jsonObject.get(key).isObject()));
            }
        }


        return treeItem;
    }

//    private native void publishUpdate(JSONObject jsonObject) /*-{
//        $wnd.sendTrackUpdate(jsonObject);
//    }-*/;

//    private static native JavaScriptObject loadTracks() /*-{
//        console.log('into loadtracks returned');
//        var allTracks = $wnd.getAllTracks();
//        console.log('all tracks returned');
//        console.log(allTracks);
//        if(allTracks==undefined){
//            return null ;
//        }
//        return allTracks;
//    }-*/;

    @UiHandler("nameSearchBox")
    public void doSearch(KeyUpEvent keyUpEvent) {
        filterList();
    }

    static void filterList() {
        String text = nameSearchBox.getText();
        GWT.log("input list: " + trackInfoList.size());
        filteredTrackInfoList.clear();
        TrackInfo trackInfo;
        GWT.log(trackInfoList.get(2).toString());
        for (int i = 0; i < trackInfoList.size(); i++) {
            trackInfo = trackInfoList.get(i);
            if (trackInfo.getName().toLowerCase().contains(text.toLowerCase()) && !isReferenceSequence(trackInfo) && !isAnnotationTrack(trackInfo)) {
                filteredTrackInfoList.add(trackInfo);
            }
        }
        GWT.log("filtered list: " + filteredTrackInfoList.size());
    }

    private static boolean isAnnotationTrack(TrackInfo trackInfo) {
        return trackInfo.getName().equalsIgnoreCase("User-created Annotations");
    }

    private static boolean isReferenceSequence(TrackInfo trackInfo) {
        return trackInfo.getName().equalsIgnoreCase("Reference sequence");
    }

    public static native void exportStaticMethod() /*-{
        $wnd.loadTracks = $entry(@org.bbop.apollo.gwt.client.TrackPanel::updateTracks(Ljava/lang/String;));
    }-*/;

    public void reload() {
        JSONObject commandObject = new JSONObject();
        commandObject.put("command", new JSONString("list"));
        MainPanel.executeFunction("handleTrackVisibility", commandObject.getJavaScriptObject());
    }

    public static void updateTracks(String jsonString) {
        JSONArray returnValueObject = JSONParser.parseStrict(jsonString).isArray();
        updateTracks(returnValueObject);
    }

    public static void updateTracks(JSONArray array) {
        trackInfoList.clear();

        for (int i = 0; i < array.size(); i++) {
            JSONObject object = array.get(i).isObject();
            TrackInfo trackInfo = new TrackInfo();
            trackInfo.setName(object.get("key").isString().stringValue());
            trackInfo.setLabel(object.get("label").isString().stringValue());
            trackInfo.setType(object.get("type").isString().stringValue());
            if (object.get("visible") != null) {
                trackInfo.setVisible(object.get("visible").isBoolean().booleanValue());
            } else {
                trackInfo.setVisible(false);
            }
            // todo, don't need all of this really, for now . .
            trackInfo.setPayload(object);
            if (object.get("urlTemplate") != null) {
                trackInfo.setUrlTemplate(object.get("urlTemplate").isString().stringValue());
            }
            trackInfoList.add(trackInfo);
        }
        GWT.log("info list: " + trackInfoList.size());
        filterList();
    }


    public void loadTracks(final List<TrackInfo> trackInfoList) {
        String url = rootUrl + "/jbrowse/data/trackList.json";
        RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, URL.encode(url));
        builder.setHeader("Content-type", "application/x-www-form-urlencoded");
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                JSONValue returnValue = JSONParser.parseLenient(response.getText());
                JSONObject returnValueObject = returnValue.isObject();
                updateTracks(returnValueObject.get("tracks").isArray());
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Window.alert("Error loading organisms");
            }
        };
        try {
            builder.setCallback(requestCallback);
            builder.send();
        } catch (RequestException e) {
            // Couldn't connect to server
            Window.alert(e.getMessage());
        }

    }

}
