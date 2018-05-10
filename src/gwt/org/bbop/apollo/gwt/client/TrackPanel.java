package org.bbop.apollo.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
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
import org.gwtbootstrap3.client.ui.Panel;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.constants.HeadingSize;
import org.gwtbootstrap3.client.ui.constants.Pull;
import org.gwtbootstrap3.client.ui.constants.Toggle;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;
import org.gwtbootstrap3.extras.toggleswitch.client.ui.ToggleSwitch;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


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

    @UiField
    DockLayoutPanel layoutPanel;
    @UiField
    Tree optionTree;
    @UiField
    static PanelGroup dataGrid;


    public static ListDataProvider<TrackInfo> dataProvider = new ListDataProvider<>();
    private static List<TrackInfo> trackInfoList = new ArrayList<>();
    private static List<TrackInfo> filteredTrackInfoList = dataProvider.getList();

    private static Map<String, List<TrackInfo>> categoryMap = new TreeMap<>();
    private static Map<String, Boolean> categoryOpen = new TreeMap<>();
    private static Map<TrackInfo, CheckBoxButton> checkBoxMap = new TreeMap<>();
    private static Map<TrackInfo, TrackBodyPanel> trackBodyMap = new TreeMap<>();

    public TrackPanel() {
        exportStaticMethod();

        Widget rootElement = ourUiBinder.createAndBindUi(this);
        initWidget(rootElement);

        dataGrid.setWidth("100%");


        Annotator.eventBus.addHandler(OrganismChangeEvent.TYPE, new OrganismChangeEventHandler() {
            @Override
            public void onOrganismChanged(OrganismChangeEvent authenticationEvent) {
                loadTracks(2000);
            }
        });

    }

    public void loadTracks(int delay) {
        filteredTrackInfoList.clear();
        trackInfoList.clear();
        Scheduler.get().scheduleFixedDelay(new Scheduler.RepeatingCommand() {
            @Override
            public boolean execute() {
                reload();
                if (trackInfoList.isEmpty()) {
                    return true;
                }
                return false;
            }
        }, delay);
    }

    public void reloadIfEmpty() {
        if (dataProvider.getList().isEmpty()) {
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
    }

    static class TrackBodyPanel extends PanelBody {

        private final TrackInfo trackInfo;


        public TrackBodyPanel(TrackInfo trackInfo) {
            this.trackInfo = trackInfo;
            decorate();
        }

        private void decorate() {

            InputGroup inputGroup = new InputGroup();
            addStyleName("track-entry");
            final CheckBoxButton selected = new CheckBoxButton();
            selected.setValue(trackInfo.getVisible());

            InputGroupButton inputGroupButton = new InputGroupButton();
            inputGroupButton.add(selected);
            inputGroup.add(inputGroupButton);

            InputGroupAddon label = new InputGroupAddon();
            label.add(new HTML(trackInfo.getName()));
            label.addStyleName("text-left");
            inputGroup.add(label);

            add(inputGroup);

            selected.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
                @Override
                public void onValueChange(ValueChangeEvent<Boolean> event) {
                    Boolean value = selected.getValue();
                    handleSelect(value);
                }
            });

            checkBoxMap.put(trackInfo, selected);

            label.addStyleName("track-link");
            label.setWidth("100%");

            label.addDomHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    MainPanel.getTrackPanel().setTrackInfo(trackInfo);
                }
            }, ClickEvent.getType());
        }

        public void handleSelect(Boolean value) {
            JSONObject jsonObject = trackInfo.getPayload();
            trackInfo.setVisible(value);
            if (value) {
                jsonObject.put("command", new JSONString("show"));
            } else {
                jsonObject.put("command", new JSONString("hide"));
            }
            MainPanel.getInstance().postMessage("handleTrackVisibility", jsonObject);
        }
    }

    public void clear() {
        categoryMap.clear();
        categoryOpen.clear();
        checkBoxMap.clear();
        trackBodyMap.clear();
        dataGrid.clear();
        dataGrid.add(new org.gwtbootstrap3.client.ui.Label("Loading..."));
    }

    private static void renderFiltered() {
        dataGrid.clear();
        // populate the map of categories
        for (TrackInfo trackInfo : trackInfoList) {
            if (!isReferenceSequence(trackInfo) && !isAnnotationTrack(trackInfo)) {
                categoryMap.put(trackInfo.getStandardCategory(), new ArrayList<TrackInfo>());
                if (!categoryOpen.containsKey(trackInfo.getStandardCategory())) {
                    categoryOpen.put(trackInfo.getStandardCategory(), false);
                }
            }
        }

        if (categoryOpen.size() == 1) {
            categoryOpen.put(categoryOpen.keySet().iterator().next(), true);
        }

        for (TrackInfo trackInfo : filteredTrackInfoList) {
            if (!isReferenceSequence(trackInfo) && !isAnnotationTrack(trackInfo)) {
                List<TrackInfo> trackInfoList = categoryMap.get(trackInfo.getStandardCategory());
                trackInfoList.add(trackInfo);
                categoryMap.put(trackInfo.getStandardCategory(), trackInfoList);
            }
        }


        // build up categories, first, processing any / all levels
        int count = 1;
        // handle the uncategorized first
        if (categoryMap.containsKey(TrackInfo.TRACK_UNCATEGORIZED)) {
            List<TrackInfo> trackInfoList = categoryMap.get(TrackInfo.TRACK_UNCATEGORIZED);
            for (TrackInfo trackInfo : trackInfoList) {
                TrackBodyPanel panelBody = new TrackBodyPanel(trackInfo);
                dataGrid.add(panelBody);
            }
        }


        for (final String key : categoryMap.keySet()) {
            if (!key.equals(TrackInfo.TRACK_UNCATEGORIZED)) {

                final List<TrackInfo> trackInfoList = categoryMap.get(key);

                // if this is a root panel
                Panel panel = new Panel();

                PanelHeader panelHeader = null;
                final CheckBoxButton panelSelect = new CheckBoxButton();
                panelSelect.addStyleName("panel-select");
                panelSelect.setPull(Pull.RIGHT);
                Badge totalBadge = null;

                // if we only have a single uncategorized category, then do not add a header
                if (categoryOpen.size() != 1 || (!key.equals(TrackInfo.TRACK_UNCATEGORIZED) && categoryOpen.size() == 1)) {
                    panelHeader = new PanelHeader();
                    panelHeader.setPaddingTop(2);
                    panelHeader.setPaddingBottom(2);
                    panelHeader.setDataToggle(Toggle.COLLAPSE);
                    Heading heading = new Heading(HeadingSize.H4, key);
                    panelHeader.add(heading);
                    heading.addStyleName("track-header");
                    totalBadge = new Badge(Integer.toString(trackInfoList.size()));
                    totalBadge.setPull(Pull.RIGHT);
                    panelHeader.add(panelSelect);
                    panelHeader.add(totalBadge);
                    panel.add(panelHeader);
                }


                final PanelCollapse panelCollapse = new PanelCollapse();
                panelCollapse.setIn(categoryOpen.get(key));
                panelCollapse.setId("collapse" + count++);
                panel.add(panelCollapse);
                panelCollapse.setWidth("100%");

                if (panelHeader != null) panelHeader.setDataTarget("#" + panelCollapse.getId());

                panelCollapse.addHiddenHandler(new HiddenHandler() {
                    @Override
                    public void onHidden(HiddenEvent event) {
                        categoryOpen.put(key, false);
                    }
                });

                panelCollapse.addShowHandler(new ShowHandler() {
                    @Override
                    public void onShow(ShowEvent showEvent) {
                        categoryOpen.put(key, true);
                    }
                });

                Integer numVisible = 0;
                if (!trackInfoList.isEmpty()) {
                    for (TrackInfo trackInfo : trackInfoList) {
                        if (trackInfo.getVisible()) ++numVisible;
                        TrackBodyPanel panelBody = new TrackBodyPanel(trackInfo);
                        trackBodyMap.put(trackInfo, panelBody);
                        panelCollapse.add(panelBody);
                    }
                    if (numVisible == 0) {
                        panelSelect.setValue(false);
                    } else if (numVisible == trackInfoList.size()) {
                        panelSelect.setValue(true);
                    }
                    panelSelect.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
                        @Override
                        public void onValueChange(ValueChangeEvent<Boolean> event) {
                            Boolean value = panelSelect.getValue();
                            for (TrackInfo trackInfo : trackInfoList) {
                                checkBoxMap.get(trackInfo).setValue(value);
                                trackBodyMap.get(trackInfo).handleSelect(value);
                            }
                            categoryOpen.put(key, true);
                        }
                    });
                }
                if (totalBadge != null) {
                    totalBadge.setText(numVisible + "/" + trackInfoList.size());
                }

                dataGrid.add(panel);
            }
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
        if (trackInfoList.isEmpty()) {
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
