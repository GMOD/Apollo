package org.bbop.apollo.gwt.client;

import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.http.client.*;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.json.client.*;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.*;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
//import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.view.client.ListDataProvider;
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

//    @UiField
//    FlexTable configurationTable;
//    @UiField FlexTable trackTable;
//    @UiField
//    ListBox organismList;
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
//    @UiField(provided = false)
//    DataGrid<TrackInfo> dataGrid;

    private DataGrid.Resources tablecss = GWT.create(TableResources.TableCss.class);
//    @UiField(provided=true) DataGrid<TrackInfo> dataGrid = new DataGrid<TrackInfo>( 10, tablecss );
    @UiField(provided=true) DataGrid<TrackInfo> dataGrid = new DataGrid<TrackInfo>( 100, tablecss );
    public static ListDataProvider<TrackInfo> dataProvider = new ListDataProvider<>();
    private static List<TrackInfo> trackInfoList = new ArrayList<>();
    private static List<TrackInfo> filteredTrackInfoList = dataProvider.getList();


    //    @UiField(provided = true) org.gwtbootstrap3.client.ui.gwt.DataGrid<TrackInfo> dataGrid;
//    @UiField
//    DataGrid dataGrid;
//    @UiField(provided = true)
//    org.gwtbootstrap3.client.ui.gwt.DataGrid<TrackInfo> dataGrid;

//    private ListDataProvider<TrackInfo> dataProvider = new ListDataProvider<TrackInfo>();



    public TrackPanel() {
        Dictionary dictionary = Dictionary.getDictionary("Options");
        rootUrl = dictionary.get("rootUrl");
        exportStaticMethod();

        Widget rootElement = ourUiBinder.createAndBindUi(this);
        initWidget(rootElement);

//        configurationTable.setHTML(0, 0, "maxHeight"); ;
//        configurationTable.setHTML(0, 1, "1000");
//        configurationTable.setHTML(1, 0, "maxFeatureScreenDensity");
//        configurationTable.setHTML(1, 1, "0.5");
//        configurationTable.setHTML(2, 0, "maxDescriptionLength");
//        configurationTable.setHTML(2, 1, "70");
//        configurationTable.setHTML(3, 0, "label");
//        configurationTable.setHTML(3, 1, "Cflo_OGSv3.3");
//
//        configurationTable.setWidth("100%");



//        dataGrid = new CellTable<>();
        dataGrid.setWidth("100%");
//        dataGrid.setAutoHeaderRefreshDisabled(true);

        // Set the message to display when the table is empty.
        // fix selected style: http://comments.gmane.org/gmane.org.google.gwt/70747
        dataGrid.setEmptyTableWidget(new Label("No tracks!"));

        // TODO: on-click . . . if not Clicked
        Column<TrackInfo,Boolean> showColumn = new Column<TrackInfo,Boolean>(new CheckboxCell(true,false)) {


            @Override
            public Boolean getValue(TrackInfo employee) {
                return employee.getVisible();
            }
        };

        showColumn.setFieldUpdater(new FieldUpdater<TrackInfo, Boolean>() {
            /**
             * TODO: emulate . . underTrackList . . Create an external function n Annotrackto then call from here
             * a good example: http://www.springindepth.com/book/gwt-comet-gwt-dojo-cometd-spring-bayeux-jetty.html
             * uses DOJO publish mechanism (http://dojotoolkit.org/reference-guide/1.7/dojo/publish.html)
             });
             });
             });

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
                    GWT.log("selected . .  do something");
                } else {
                    jsonObject.put("command", new JSONString("hide"));
                    GWT.log("UN selected . .  do something");
                }

                MainPanel.executeFunction("handleTrackVisibility", jsonObject.getJavaScriptObject());
            }
        });
        showColumn.setSortable(true);

        TextColumn<TrackInfo> secondNameColumn = new TextColumn<TrackInfo>() {
            @Override
            public String getValue(TrackInfo employee) {
                return employee.getName();
            }
        };
        secondNameColumn.setSortable(true);



        TextColumn<TrackInfo> thirdNameColumn = new TextColumn<TrackInfo>() {
            @Override
            public String getValue(TrackInfo employee) {
                return employee.getType();
            }
        };
        thirdNameColumn.setSortable(true);



        dataGrid.addColumn(showColumn, "Show");
        dataGrid.addColumn(secondNameColumn, "Name");
        dataGrid.addColumn(thirdNameColumn, "Type");
        dataGrid.setColumnWidth(0,"10%");


        dataProvider.addDataDisplay(dataGrid);

        loadTracks(trackInfoList);
//        DataGenerator.populateTrackList(trackInfoList);


        ColumnSortEvent.ListHandler<TrackInfo> sortHandler = new ColumnSortEvent.ListHandler<TrackInfo>(trackInfoList);
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
        sortHandler.setComparator(secondNameColumn, new Comparator<TrackInfo>() {
            @Override
            public int compare(TrackInfo o1, TrackInfo o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        sortHandler.setComparator(thirdNameColumn, new Comparator<TrackInfo>() {
            @Override
            public int compare(TrackInfo o1, TrackInfo o2) {
                return o1.getType().compareTo(o2.getType());
            }
        });


        Annotator.eventBus.addHandler(ContextSwitchEvent.TYPE, new ContextSwitchEventHandler() {
            @Override
            public void onContextSwitched(ContextSwitchEvent contextSwitchEvent) {
                loadTracks(trackInfoList);
            }

        });

        trackName.setHTML("GeneID");
        trackType.setHTML("HTMLFeature");
        trackCount.setHTML("34");
        trackDensity.setHTML("0.000123");

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
    public void doSearch(KeyUpEvent keyUpEvent){
        filterList(trackInfoList);
    }

    static void filterList(List<TrackInfo> trackInfoList) {
        String text = nameSearchBox.getText();
        filteredTrackInfoList.clear();
        for(TrackInfo trackInfo : trackInfoList ){
            if(trackInfo.getName().toLowerCase().contains(text) && !isReferenceSequence(trackInfo) && !isAnnotationTrack(trackInfo)){
                filteredTrackInfoList.add(trackInfo);
            }
        }
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
//        JSONObject commandObject = new JSONObject();
//        commandObject.put("command", new JSONString("list"));
//        MainPanel.executeFunction("handleTrackVisibility", commandObject.getJavaScriptObject());
        loadTracks(trackInfoList);
    }

    public static void updateTracks(String jsonString){
//        Window.alert("updating tracks: "+jsonString);
        JSONArray returnValueObject = JSONParser.parseStrict(jsonString).isArray();


        updateTracks(returnValueObject,dataProvider.getList());

    }

    public static void updateTracks(JSONArray array, List<TrackInfo> trackInfoList){
        trackInfoList.clear();

        for(int i = 0 ; i < array.size() ; i++){
            JSONObject object = array.get(i).isObject();
            TrackInfo trackInfo = new TrackInfo();
            trackInfo.setName(object.get("key").isString().stringValue());
            trackInfo.setLabel(object.get("label").isString().stringValue());
            trackInfo.setType(object.get("type").isString().stringValue());
            if(object.get("visible")!=null){
                trackInfo.setVisible(object.get("visible").isBoolean().booleanValue());
            }
            else{
                trackInfo.setVisible(false);
            }
            // todo, don't need all of this really, for now . .
            trackInfo.setPayload(object);
            if(object.get("urlTemplate")!=null){
                trackInfo.setUrlTemplate(object.get("urlTemplate").isString().stringValue());
            }
            trackInfoList.add(trackInfo);
        }
        filterList(trackInfoList);
    }


    public void loadTracks(final List<TrackInfo> trackInfoList) {

//        String url = rootUrl+"/jbrowse/allTracks";
        String url = rootUrl+"/jbrowse/data/trackList.json";
        RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, URL.encode(url));
        builder.setHeader("Content-type", "application/x-www-form-urlencoded");
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                GWT.log("response: "+response.getText());
                JSONValue returnValue = JSONParser.parseLenient(response.getText());
                JSONObject returnValueObject = returnValue.isObject();
                updateTracks(returnValueObject.get("tracks").isArray(),trackInfoList);
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