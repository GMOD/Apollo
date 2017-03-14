package org.bbop.apollo.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.*;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.view.client.ListDataProvider;
import org.bbop.apollo.gwt.client.dto.TrackInfo;
import org.bbop.apollo.gwt.client.event.OrganismChangeEvent;
import org.bbop.apollo.gwt.client.event.OrganismChangeEventHandler;
import org.bbop.apollo.gwt.client.rest.UserRestService;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;
import org.gwtbootstrap3.client.shared.event.HiddenEvent;
import org.gwtbootstrap3.client.shared.event.HiddenHandler;
import org.gwtbootstrap3.client.shared.event.ShowEvent;
import org.gwtbootstrap3.client.shared.event.ShowHandler;
import org.gwtbootstrap3.client.ui.*;
import org.gwtbootstrap3.client.ui.CheckBox;
import org.gwtbootstrap3.client.ui.Panel;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.constants.HeadingSize;
import org.gwtbootstrap3.client.ui.constants.Pull;
import org.gwtbootstrap3.client.ui.constants.Toggle;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;
import org.gwtbootstrap3.extras.toggleswitch.client.ui.ToggleSwitch;

import java.util.*;


/**
 * Created by ndunn on 12/16/14.
 */
public class TrackPanel extends Composite {

    interface TrackUiBinder extends UiBinder<Widget, TrackPanel> {
    }

    private static TrackUiBinder ourUiBinder = GWT.create(TrackUiBinder.class);

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

    @UiField
    ToggleSwitch trackListToggle;


    //    private static DataGrid.Resources tablecss = GWT.create(TableResources.TableCss.class);
//    @UiField(provided = true)
//    static DataGrid<TrackInfo> dataGrid = new DataGrid<TrackInfo>(1000, tablecss);
    @UiField
    DockLayoutPanel layoutPanel;
    @UiField
    Tree optionTree;
    @UiField
    static PanelGroup dataGrid;


    public static ListDataProvider<TrackInfo> dataProvider = new ListDataProvider<>();
    private static List<TrackInfo> trackInfoList = new ArrayList<>();
    private static List<TrackInfo> filteredTrackInfoList = dataProvider.getList();
//    private SingleSelectionModel<TrackInfo> singleSelectionModel = new SingleSelectionModel<TrackInfo>();
//    private boolean trackSelectionFix; // this fixes the fact that firefox requires two clicks to select a CheckboxCell

    public TrackPanel() {
        exportStaticMethod();
//        trackSelectionFix=true;

        Widget rootElement = ourUiBinder.createAndBindUi(this);
        initWidget(rootElement);

        dataGrid.setWidth("100%");

        // Set the message to display when the table is empty.
        // fix selected style: http://comments.gmane.org/gmane.org.google.gwt/70747
//        dataGrid.setEmptyTableWidget(new Label("No tracks!"));

//        Column<TrackInfo, Boolean> showColumn = new Column<TrackInfo, Boolean>(new CheckboxCell(true, false)) {
//            @Override
//            public Boolean getValue(TrackInfo track) {
//                return track.getVisible();
//            }
//        };
//        dataGrid.addCellPreviewHandler(new CellPreviewEvent.Handler<TrackInfo>() {
//
//            @Override
//            public void onCellPreview(final CellPreviewEvent<TrackInfo> event) {
//
//                if (BrowserEvents.CLICK.equals(event.getNativeEvent().getType())) {
//                    trackSelectionFix=false;
//                    final TrackInfo value = event.getValue();
//                    final Boolean state = !event.getDisplay().getSelectionModel().isSelected(value);
//                    event.getDisplay().getSelectionModel().setSelected(value, state);
//                    event.setCanceled(true);
//                }
//            }
//        });

//        showColumn.setFieldUpdater(new FieldUpdater<TrackInfo, Boolean>() {
//            /**
//             * TODO: emulate . . underTrackList . . Create an external function in Annotrack to then call from here
//             * a good example: http://www.springindepth.com/book/gwt-comet-gwt-dojo-cometd-spring-bayeux-jetty.html
//             * uses DOJO publish mechanism (http://dojotoolkit.org/reference-guide/1.7/dojo/publish.html)
//
//             * @param index
//             * @param trackInfo
//             * @param value
//             */
//            @Override
//            public void update(int index, TrackInfo trackInfo, Boolean value) {
//                JSONObject jsonObject = trackInfo.getPayload();
//                trackInfo.setVisible(value);
//                if (value) {
//                    jsonObject.put("command", new JSONString("show"));
//                } else {
//                    jsonObject.put("command", new JSONString("hide"));
//                }
//
//                MainPanel.getInstance().postMessage("handleTrackVisibility", jsonObject);
//            }
//        });
//        showColumn.setSortable(true);

//        TextColumn<TrackInfo> nameColumn = new TextColumn<TrackInfo>() {
//            @Override
//            public String getValue(TrackInfo track) {
//                return track.getName();
//            }
//        };
//        nameColumn.setSortable(true);
//
//
//        TextColumn<TrackInfo> categoryColumn = new TextColumn<TrackInfo>() {
//            @Override
//            public String getValue(TrackInfo track) {
//                return track.getCategory();
//            }
//        };
//        categoryColumn.setSortable(true);

//        dataGrid.addColumn(showColumn, "Show");
//        dataGrid.addColumn(categoryColumn, "Category");
//        dataGrid.addColumn(nameColumn, "Name");
//        dataGrid.setColumnWidth(0, "10%");
//        dataGrid.setSelectionModel(singleSelectionModel);
//        singleSelectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
//            @Override
//            public void onSelectionChange(SelectionChangeEvent event) {
//                if(!trackSelectionFix) setTrackInfo(singleSelectionModel.getSelectedObject());
//                trackSelectionFix=true;
//            }
//        });

//        dataProvider.addDataDisplay(dataGrid);
//
//
//        ColumnSortEvent.ListHandler<TrackInfo> sortHandler = new ColumnSortEvent.ListHandler<TrackInfo>(filteredTrackInfoList);
//        dataGrid.addColumnSortHandler(sortHandler);

//        sortHandler.setComparator(showColumn, new Comparator<TrackInfo>() {
//            @Override
//            public int compare(TrackInfo o1, TrackInfo o2) {
//                if (o1.getVisible() == o2.getVisible()) {
//                    return 0;
//                }  else if (o1.getVisible()) {
//                    return 1;
//                } else {
//                    return -1;
//                }
//            }
//        });
//        sortHandler.setComparator(nameColumn, new Comparator<TrackInfo>() {
//            @Override
//            public int compare(TrackInfo o1, TrackInfo o2) {
//                return o1.getName().compareTo(o2.getName());
//            }
//        });
//        sortHandler.setComparator(categoryColumn, new Comparator<TrackInfo>() {
//            @Override
//            public int compare(TrackInfo o1, TrackInfo o2) {
//                if(o1.getCategory()==null && o2.getCategory()!=null ) return -1 ;
//                if(o1.getCategory()!=null && o2.getCategory()==null ) return 1 ;
//                if(o1.getCategory()==null && o2.getCategory()==null ) return 0 ;
//                return o1.getCategory().compareTo(o2.getCategory());
//            }
//        });


        Annotator.eventBus.addHandler(OrganismChangeEvent.TYPE, new OrganismChangeEventHandler() {
            @Override
            public void onOrganismChanged(OrganismChangeEvent authenticationEvent) {
                loadTracks(2000);
            }
        });

    }

    public void loadTracks(int delay) {
//        dataGrid.setLoadingIndicator(new Label("Loading..."));
//        dataGrid.setEmptyTableWidget(new Label("Loading..."));
        filteredTrackInfoList.clear();
        trackInfoList.clear();
        Scheduler.get().scheduleFixedDelay(new Scheduler.RepeatingCommand() {
            @Override
            public boolean execute() {
                reload();
                if (trackInfoList.size() == 0) {
//                    dataGrid.setEmptyTableWidget(new Label("Loading..."));
                    return true;
                }
                return false;
            }
        }, delay);
    }

    public void reloadIfEmpty() {
        if (dataProvider.getList().size() == 0) {
            loadTracks(7000);
        }
    }


    private void setTrackInfo(TrackInfo selectedObject) {
        if (selectedObject == null) {
            trackName.setText("");
            trackType.setText("");
            optionTree.clear();
        } else {
            trackName.setText(selectedObject.getName());
            trackType.setText(selectedObject.getType());
            optionTree.clear();
            JSONObject jsonObject = selectedObject.getPayload();
            setOptionDetails(jsonObject);
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


    @UiHandler("nameSearchBox")
    public void doSearch(KeyUpEvent keyUpEvent) {
        filterList();
    }

    static void filterList() {
        String text = nameSearchBox.getText();
        for (TrackInfo trackInfo : trackInfoList) {
            if (trackInfo.getName().toLowerCase().contains(text.toLowerCase()) &&
                    !isReferenceSequence(trackInfo) &&
                    !isAnnotationTrack(trackInfo)) {
                Integer filteredIndex = filteredTrackInfoList.indexOf(trackInfo);
                if (filteredIndex < 0) {
                    filteredTrackInfoList.add(trackInfo);
                } else {
                    filteredTrackInfoList.get(filteredIndex).setVisible(trackInfo.getVisible());
                }
            } else {
                filteredTrackInfoList.remove(trackInfo);
            }
        }
        renderFiltered();
//        dataGrid.redraw();
    }

    private static Map<String, List<TrackInfo>> categoryMap = new TreeMap<>();
    private static Map<String, Boolean> categoryOpen = new TreeMap<>();

    static class TrackBodyPanel extends PanelBody {

        private final TrackInfo trackInfo;

        public TrackBodyPanel(TrackInfo trackInfo) {
            this.trackInfo = trackInfo;
            decorate();
        }

        private void decorate() {
            HTML label = new HTML(trackInfo.getLabel());
            label.addStyleName("track-link");
            final CheckBox selected = new CheckBox();
            selected.setValue(trackInfo.getVisible());
            selected.addStyleName("track-link");

            add(selected);
            add(label);
            setWidth("100%");

            selected.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    JSONObject jsonObject = trackInfo.getPayload();
                    Boolean value = selected.getValue();
                    trackInfo.setVisible(value);
                    if (value) {
                        jsonObject.put("command", new JSONString("show"));
                    } else {
                        jsonObject.put("command", new JSONString("hide"));
                    }
                    MainPanel.getInstance().postMessage("handleTrackVisibility", jsonObject);
                }
            });
            label.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    MainPanel.getTrackPanel().setTrackInfo(trackInfo);
                }
            });
        }
    }

    private static void renderFiltered() {
        dataGrid.clear();
//        categoryMap.clear();
        // populate the map of categories
        for (TrackInfo trackInfo : trackInfoList) {
            categoryMap.put(trackInfo.getStandardCategory(), new ArrayList<TrackInfo>());
            if(!categoryOpen.containsKey(trackInfo.getStandardCategory())){
                categoryOpen.put(trackInfo.getStandardCategory(),false);
            }
        }

        for (TrackInfo trackInfo : filteredTrackInfoList) {
            List<TrackInfo> trackInfoList = categoryMap.get(trackInfo.getStandardCategory());
            trackInfoList.add(trackInfo);
            categoryMap.put(trackInfo.getStandardCategory(), trackInfoList);
        }


        // build up categories, first, processing any / all levels
        int count = 1;
        for (final String key : categoryMap.keySet()) {
            List<TrackInfo> trackInfoList = categoryMap.get(key);
            Collections.sort(trackInfoList, new Comparator<TrackInfo>() {
                @Override
                public int compare(TrackInfo o1, TrackInfo o2) {
                    return o1.getLabel().compareTo(o2.getLabel());
                }
            });

            // if this is a root panel
            Panel panel = new Panel();
            PanelHeader panelHeader = new PanelHeader();
            panelHeader.setDataToggle(Toggle.COLLAPSE);
            Heading heading = new Heading(HeadingSize.H4, key);
            panelHeader.add(heading);
            heading.addStyleName("track-header");
            Badge badge = new Badge(trackInfoList.size() + "");
            badge.setPull(Pull.RIGHT);
            panelHeader.add(badge);
            panel.add(panelHeader);

            final PanelCollapse panelCollapse = new PanelCollapse();
            panelCollapse.setIn(categoryOpen.get(key));
            panelCollapse.setId("collapse" + count++);
            panel.add(panelCollapse);
            panelCollapse.setWidth("100%");
            panelHeader.setDataTarget("#" + panelCollapse.getId());

            panelCollapse.addHiddenHandler(new HiddenHandler() {
                @Override
                public void onHidden(HiddenEvent event) {
                    categoryOpen.put(key,false);
                }
            });

            panelCollapse.addShowHandler(new ShowHandler() {
                @Override
                public void onShow(ShowEvent showEvent) {
                    categoryOpen.put(key,true);
                }
            });

            if (trackInfoList.size() > 0) {
                for (TrackInfo trackInfo : trackInfoList) {
                    TrackBodyPanel panelBody = new TrackBodyPanel(trackInfo);
//                    panelBody.add(new Label(trackInfo.getLabel()));
                    panelCollapse.add(panelBody);
                }
            }

//            PanelBody panelBody = getPanelBodyForCategory(key);
//            if (panelBody == null) {
                dataGrid.add(panel);
//            } else {
//                panelBody.add(panel);
//            }
            // else, find the PanelBody and insert there
        }
    }

    private static PanelBody getPanelBodyForCategory(String key) {
        Element element = Document.get().getElementById("#panelBody" + key);
        if (element != null) {
            com.google.gwt.user.client.EventListener listener = DOM.getEventListener((com.google.gwt.user.client.Element) element);
            return (PanelBody) listener;
        }
        return null;
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
        MainPanel.getInstance().postMessage("handleTrackVisibility", commandObject);
    }

    public static void updateTracks(String jsonString) {
        JSONArray returnValueObject = JSONParser.parseStrict(jsonString).isArray();
        updateTracks(returnValueObject);
    }

    public List<String> getTrackList() {
        if (trackInfoList.size() == 0) {
            reload();
        }
        List<String> trackListArray = new ArrayList<>();
        for (TrackInfo trackInfo : trackInfoList) {
            if (trackInfo.getVisible() &&
                    !isReferenceSequence(trackInfo) &&
                    !isAnnotationTrack(trackInfo)) {
                trackListArray.add(trackInfo.getLabel());
            }
        }
        return trackListArray;
    }

    public static void updateTracks(JSONArray array) {
        trackInfoList.clear();

        for (int i = 0; i < array.size(); i++) {
            JSONObject object = array.get(i).isObject();
            TrackInfo trackInfo = new TrackInfo();
            // track label can never be null, but key can be
            trackInfo.setName(object.get("key") == null ? object.get("label").isString().stringValue() : object.get("key").isString().stringValue());

            if (object.get("label") != null) trackInfo.setLabel(object.get("label").isString().stringValue());
            else Bootbox.alert("Track label should not be null, please check your tracklist");

            if (object.get("type") != null) trackInfo.setType(object.get("type").isString().stringValue());

            if (object.get("urlTemplate") != null)
                trackInfo.setUrlTemplate(object.get("urlTemplate").isString().stringValue());

            if (object.get("visible") != null) trackInfo.setVisible(object.get("visible").isBoolean().booleanValue());
            else trackInfo.setVisible(false);

            if (object.get("category") != null) trackInfo.setCategory(object.get("category").isString().stringValue());

            trackInfo.setPayload(object);
            trackInfoList.add(trackInfo);
        }
        filterList();
    }

    @UiHandler("trackListToggle")
    public void trackListToggle(ValueChangeEvent<Boolean> event) {
        MainPanel.useNativeTracklist = trackListToggle.getValue();
        MainPanel.getInstance().trackListToggle.setActive(MainPanel.useNativeTracklist);
        updateTrackToggle(MainPanel.useNativeTracklist);
    }

    public void updateTrackToggle(Boolean val) {
        trackListToggle.setValue(val);


        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                JSONValue v = JSONParser.parseStrict(response.getText());
                JSONObject o = v.isObject();
                if (o.containsKey(FeatureStringEnum.ERROR.getValue())) {
                    new ErrorDialog("Error Updating User", o.get(FeatureStringEnum.ERROR.getValue()).isString().stringValue(), true, true);
                }

                MainPanel.updateGenomicViewer(true, true);
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Bootbox.alert("Error updating user: " + exception);
            }
        };
        UserRestService.updateUserTrackPanelPreference(requestCallback, trackListToggle.getValue());
    }
}
