package org.bbop.apollo.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ChangeEvent;
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
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.view.client.ListDataProvider;
import org.bbop.apollo.gwt.client.dto.OrganismInfo;
import org.bbop.apollo.gwt.client.dto.TrackInfo;
import org.bbop.apollo.gwt.client.event.OrganismChangeEvent;
import org.bbop.apollo.gwt.client.event.OrganismChangeEventHandler;
import org.bbop.apollo.gwt.client.rest.OrganismRestService;
import org.bbop.apollo.gwt.client.rest.RestService;
import org.bbop.apollo.gwt.client.rest.UserRestService;
import org.bbop.apollo.gwt.client.track.TrackConfigurationTemplate;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;
import org.bbop.apollo.gwt.shared.track.TrackTypeEnum;
import org.gwtbootstrap3.client.shared.event.HiddenEvent;
import org.gwtbootstrap3.client.shared.event.HiddenHandler;
import org.gwtbootstrap3.client.shared.event.ShowEvent;
import org.gwtbootstrap3.client.shared.event.ShowHandler;
import org.gwtbootstrap3.client.ui.*;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Panel;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.constants.HeadingSize;
import org.gwtbootstrap3.client.ui.constants.Pull;
import org.gwtbootstrap3.client.ui.constants.Toggle;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;
import org.gwtbootstrap3.extras.bootbox.client.callback.ConfirmCallback;
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
    Button addTrackButton;

    @UiField
    ToggleSwitch trackListToggle;

    @UiField
    DockLayoutPanel layoutPanel;
    @UiField
    Tree optionTree;
    @UiField
    static PanelGroup dataGrid;
    @UiField
    static Modal addTrackModal;
    @UiField
    com.google.gwt.user.client.ui.Button saveNewTrack;
    @UiField
    Button cancelNewTrack;
    @UiField
    FileUpload uploadTrackFile;
    @UiField
    FileUpload uploadTrackFileIndex;
    @UiField
    FormPanel newTrackForm;
    @UiField
    TextArea configuration;
    @UiField
    Hidden hiddenOrganism;
    @UiField
    FlowPanel flowPanel;
    @UiField
    AnchorListItem selectBam;
    @UiField
    AnchorListItem selectBigWig;
    @UiField
    AnchorListItem selectGFF3;
    @UiField
    AnchorListItem selectGFF3Tabix;
    @UiField
    AnchorListItem selectVCF;
    @UiField
    AnchorListItem selectBamCanvas;
    @UiField
    AnchorListItem selectBigWigXY;
    //    @UiField
//    AnchorListItem selectGFF3Canvas;
    @UiField
    AnchorListItem selectVCFCanvas;
    @UiField
    com.google.gwt.user.client.ui.TextBox trackFileName;
    @UiField
    Button configurationButton;
    @UiField
    TabLayoutPanel southTabs;
    @UiField
    Container northContainer;
    @UiField
    HTML trackNameHTML;
    @UiField
    HTML trackConfigurationHTML;
    @UiField
    HTML trackFileHTML;
    @UiField
    HTML trackFileIndexHTML;
    @UiField
    HTML categoryNameHTML;
    @UiField
    com.google.gwt.user.client.ui.TextBox categoryName;
    @UiField
    Row locationRow;
    @UiField
    HTML locationView;
    @UiField
    HTML topTypeHTML;
    @UiField
    com.google.gwt.user.client.ui.TextBox topTypeName;

    public static ListDataProvider<TrackInfo> dataProvider = new ListDataProvider<>();
    private static List<TrackInfo> trackInfoList = new ArrayList<>();
    private static List<TrackInfo> filteredTrackInfoList = dataProvider.getList();

    private static Map<String, List<TrackInfo>> categoryMap = new TreeMap<>();
    private static Map<String, Boolean> categoryOpen = new TreeMap<>();
    private static Map<TrackInfo, CheckBoxButton> checkBoxMap = new TreeMap<>();
    private static Map<TrackInfo, TrackBodyPanel> trackBodyMap = new TreeMap<>();

    private final int MAX_TIME = 5000 ;
    private final int DELAY_TIME = 400;

    public TrackPanel() {
        exportStaticMethod();

        Widget rootElement = ourUiBinder.createAndBindUi(this);
        initWidget(rootElement);
        dataGrid.setWidth("100%");



        Scheduler.get().scheduleFixedDelay(new Scheduler.RepeatingCommand() {
            @Override
            public boolean execute() {
                if (canAdminTracks()) {
                    handleAdminState();
                    GWT.log("can admin tracks");
                    return false;
                } else {
                    GWT.log("can not admin tracks, retryting");
                }
                return true ;
            }
        }, DELAY_TIME);

        newTrackForm.addSubmitHandler(new FormPanel.SubmitHandler() {
            @Override
            public void onSubmit(FormPanel.SubmitEvent event) {
                addTrackModal.hide();
            }
        });
        newTrackForm.addSubmitCompleteHandler(new FormPanel.SubmitCompleteHandler() {
            @Override
            public void onSubmitComplete(FormPanel.SubmitCompleteEvent event) {
                loadTracks(300);
                Bootbox.confirm("Track '" + trackFileName.getText() + "' added successfully.  Reload to see?", new ConfirmCallback() {
                    @Override
                    public void callback(boolean result) {
                        if (result) {
                            Window.Location.reload();
                        }
                        resetNewTrackModel();
                    }
                });
            }
        });

        Annotator.eventBus.addHandler(OrganismChangeEvent.TYPE, new OrganismChangeEventHandler() {
            @Override
            public void onOrganismChanged(OrganismChangeEvent authenticationEvent) {
                loadTracks(2000);
            }
        });
    }

    private void handleAdminState(){
        addTrackButton.setVisible(true);
        configuration.getElement().setPropertyString("placeholder", "Enter configuration data");
        trackFileName.getElement().setPropertyString("placeholder", "Enter track name");
        categoryName.getElement().setPropertyString("placeholder", "Enter category name");
        newTrackForm.setEncoding(FormPanel.ENCODING_MULTIPART);
        newTrackForm.setMethod(FormPanel.METHOD_POST);
        newTrackForm.setAction(RestService.fixUrl("organism/addTrackToOrganism"));
    }


    private static boolean canAdminTracks() {
        return MainPanel.getInstance().isCurrentUserAdmin();
    }

    private String checkForm() {
        if (configurationButton.getText().startsWith("Choosing")) {
            return "Specify a track type.";
        }
        if (configuration.getText().trim().length() < 10) {
            return "Bad configuration.";
        }
        try {
            JSONParser.parseStrict(configuration.getText().trim());
        } catch (Exception e) {
            return "Invalid JSON:\n" + e.getMessage() + "\n" + configuration.getText().trim();
        }
        if (uploadTrackFile.getFilename().trim().length() == 0) {
            return "Data file needs to be specified.";
        }

        return null;
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
                handleAdminState();
                return false;
            }
        }, delay);
    }

    void reloadIfEmpty() {
        if (dataProvider.getList().isEmpty()) {
            loadTracks(7000);
        }
    }


    private void setTrackInfo(TrackInfo selectedObject) {
        if (selectedObject == null) {
            trackName.setVisible(false);
            trackType.setVisible(false);
            optionTree.setVisible(false);
            trackName.setText("");
            trackType.setText("");
            optionTree.clear();
            locationRow.setVisible(false);
        } else {
            trackName.setHTML(selectedObject.getName());
            trackType.setText(selectedObject.getType());
            optionTree.clear();
            JSONObject jsonObject = selectedObject.getPayload();
            setOptionDetails(jsonObject);
            trackName.setVisible(true);
            trackType.setVisible(true);
            optionTree.setVisible(true);
            if (canAdminTracks()) {
                OrganismInfo currentOrganism = MainPanel.getInstance().getCurrentOrganism();
                if (selectedObject.getApollo() != null) {
                    locationView.setHTML(MainPanel.getInstance().getCommonDataDirectory() + "/" + currentOrganism.getId() + "-" + currentOrganism.getName());
                } else {
                    locationView.setHTML(MainPanel.getInstance().getCurrentOrganism().getDirectory());
                }
                locationRow.setVisible(true);
            }
            else{
                locationRow.setVisible(false);
            }
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

    private void setTrackTypeAndUpdate(TrackTypeEnum trackType) {
        configurationButton.setText(trackType.toString());
        if(topTypeName.getText().length()==0){
            topTypeName.setText("mRNA");
        }
        configuration.setText(TrackConfigurationTemplate.generateForTypeAndKeyAndCategory(trackType, trackFileName.getText(), categoryName.getText(),topTypeName.getText()).toString());
        showFileOptions(trackType);
        if (trackType.isIndexed()) {
            showIndexOptions(trackType);
        } else {
            hideIndexOptions();
        }
    }

    private TrackTypeEnum getTrackType() {
        return TrackTypeEnum.valueOf(configurationButton.getText().replaceAll(" ", "_"));
    }

    @UiHandler("uploadTrackFile")
    public void uploadTrackFile(ChangeEvent event) {
        TrackTypeEnum trackTypeEnum = getTrackType();
        if (!trackTypeEnum.hasSuffix(uploadTrackFile.getFilename())) {
            Bootbox.alert("Filetype suffix for " + uploadTrackFile.getFilename() + " should have the suffix '" + trackTypeEnum.getSuffix() + "' for track type '" + trackTypeEnum.name() + "'");
        }
    }

    @UiHandler("uploadTrackFileIndex")
    public void uploadTrackFileIndex(ChangeEvent event) {
        TrackTypeEnum trackTypeEnum = getTrackType();
        if (!trackTypeEnum.hasSuffixIndex(uploadTrackFileIndex.getFilename())) {
            Bootbox.alert("Filetype suffix for " + uploadTrackFileIndex.getFilename() + " should have the suffix '" + trackTypeEnum.getSuffixIndex() + "' for track type '" + trackTypeEnum.name() + "'");
        }
    }

    @UiHandler({"trackFileName","categoryName","topTypeName"})
    public void updateTrackFileName(KeyUpEvent event) {
        configuration.setText(TrackConfigurationTemplate.generateForTypeAndKeyAndCategory(getTrackType(), trackFileName.getText(), categoryName.getText(),topTypeName.getText()).toString());
    }

    @UiHandler("cancelNewTrack")
    public void cancelNewTrackButtonHandler(ClickEvent clickEvent) {
        addTrackModal.hide();
        resetNewTrackModel();
    }

    @UiHandler("saveNewTrack")
    public void saveNewTrackButtonHandler(ClickEvent clickEvent) {
        String resultMessage = checkForm();
        if (resultMessage == null) {
            newTrackForm.submit();
        } else {
            Bootbox.alert(resultMessage);
        }
    }

    private void showFileOptions(TrackTypeEnum typeEnum) {
        saveNewTrack.setEnabled(true);

        trackNameHTML.setVisible(true);
        trackFileName.setVisible(true);

        if(typeEnum.name().startsWith("GFF")){
            topTypeHTML.setVisible(true);
            topTypeName.setVisible(true);
            topTypeName.setText("mRNA");
        }
        else{
            topTypeHTML.setVisible(false);
            topTypeName.setVisible(false);
        }

        categoryName.setVisible(true);
        categoryNameHTML.setVisible(true);

        trackConfigurationHTML.setVisible(true);
        configuration.setVisible(true);

        trackFileHTML.setVisible(true);
        uploadTrackFile.setVisible(true);

        trackFileHTML.setText(typeEnum.getSuffixString());
    }

    private void hideIndexOptions() {
        trackFileIndexHTML.setVisible(false);
        uploadTrackFileIndex.setVisible(false);
        trackFileIndexHTML.setText("");
    }

    private void showIndexOptions(TrackTypeEnum typeEnum) {
        trackFileIndexHTML.setVisible(true);
        uploadTrackFileIndex.setVisible(true);

        trackFileIndexHTML.setText(typeEnum.getSuffixIndexString());
    }

    private void resetNewTrackModel() {
        configurationButton.setText("Select Track Type");

        saveNewTrack.setEnabled(false);

        trackNameHTML.setVisible(false);
        trackFileName.setVisible(false);

        topTypeHTML.setVisible(false);
        topTypeName.setVisible(false);

        categoryName.setVisible(false);
        categoryNameHTML.setVisible(false);

        trackConfigurationHTML.setVisible(false);
        configuration.setVisible(false);

        trackFileHTML.setVisible(false);
        uploadTrackFile.setVisible(false);
        trackFileIndexHTML.setVisible(false);
        uploadTrackFileIndex.setVisible(false);

        newTrackForm.reset();
    }

    @UiHandler("addTrackButton")
    public void addTrackButtonHandler(ClickEvent clickEvent) {
        hiddenOrganism.setValue(MainPanel.getInstance().getCurrentOrganism().getId());
        addTrackModal.show();
    }

    @UiHandler("selectBam")
    public void selectBam(ClickEvent clickEvent) {
        setTrackTypeAndUpdate(TrackTypeEnum.BAM);
    }

    @UiHandler("selectBamCanvas")
    public void setSelectBamCanvas(ClickEvent clickEvent) {
        setTrackTypeAndUpdate(TrackTypeEnum.BAM_CANVAS);
    }


    @UiHandler("selectBigWig")
    public void selectBigWig(ClickEvent clickEvent) {
        setTrackTypeAndUpdate(TrackTypeEnum.BIGWIG_HEAT_MAP);
    }

    @UiHandler("selectBigWigXY")
    public void selectBigWigXY(ClickEvent clickEvent) {
        setTrackTypeAndUpdate(TrackTypeEnum.BIGWIG_XY);
    }

    @UiHandler("selectGFF3")
    public void selectGFF3(ClickEvent clickEvent) {
        setTrackTypeAndUpdate(TrackTypeEnum.GFF3);
    }

    @UiHandler("selectGFF3Canvas")
    public void selectGFF3Canvas(ClickEvent clickEvent) {
        setTrackTypeAndUpdate(TrackTypeEnum.GFF3_CANVAS);
    }

    @UiHandler("selectGFF3Json")
    public void selectGFF3Json(ClickEvent clickEvent) {
        setTrackTypeAndUpdate(TrackTypeEnum.GFF3_JSON);
    }

    @UiHandler("selectGFF3JsonCanvas")
    public void selectGFF3JsonCanvas(ClickEvent clickEvent) {
        setTrackTypeAndUpdate(TrackTypeEnum.GFF3_JSON_CANVAS);
    }

    @UiHandler("selectGFF3Tabix")
    public void selectGFF3Tabix(ClickEvent clickEvent) {
        setTrackTypeAndUpdate(TrackTypeEnum.GFF3_TABIX);
    }

    @UiHandler("selectGFF3TabixCanvas")
    public void selectGFF3TabixCanvas(ClickEvent clickEvent) {
        setTrackTypeAndUpdate(TrackTypeEnum.GFF3_TABIX_CANVAS);
    }

    @UiHandler("selectVCF")
    public void selectVCF(ClickEvent clickEvent) {
        setTrackTypeAndUpdate(TrackTypeEnum.VCF);
    }

    @UiHandler("selectVCFCanvas")
    public void selectVCFCanvas(ClickEvent clickEvent) {
        setTrackTypeAndUpdate(TrackTypeEnum.VCF_CANVAS);
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
                int filteredIndex = filteredTrackInfoList.indexOf(trackInfo);
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

    static void removeTrack(final String label) {
        Bootbox.confirm("Remove track " + label + "?", new ConfirmCallback() {
            @Override
            public void callback(boolean result) {
                if (result) {
                    OrganismRestService.removeTrack(
                            new RequestCallback() {
                                @Override
                                public void onResponseReceived(Request request, Response response) {
                                    Bootbox.confirm("Track '" + label + "' removed, refresh?", new ConfirmCallback() {
                                        @Override
                                        public void callback(boolean result) {
                                            if (result) {
                                                Window.Location.reload();
                                            }
                                        }
                                    });
                                }

                                @Override
                                public void onError(Request request, Throwable exception) {
                                    Bootbox.alert("Error removing track: " + exception.getMessage());
                                }
                            }
                            , MainPanel.getInstance().getCurrentOrganism(), label);
                }
            }
        });
    }

    static class TrackBodyPanel extends PanelBody {

        private final TrackInfo trackInfo;
        private final InputGroupAddon label = new InputGroupAddon();


        public TrackBodyPanel(TrackInfo trackInfo) {
            this.trackInfo = trackInfo;
            decorate();
        }

        private void decorate() {

            final InputGroup inputGroup = new InputGroup();
            addStyleName("track-entry");
            final CheckBoxButton selected = new CheckBoxButton();
            selected.setValue(trackInfo.getVisible());

            InputGroupButton inputGroupButton = new InputGroupButton();
            inputGroupButton.add(selected);
            inputGroup.add(inputGroupButton);

//            final InputGroupAddon label = new InputGroupAddon();
            HTML trackNameHTML = new HTML(trackInfo.getName());
            trackNameHTML.addStyleName("text-html-left");
            label.add(trackNameHTML);
            label.addStyleName("text-left");
            inputGroup.add(label);
            if (trackInfo.getApollo() != null && canAdminTracks()) {
//                InputGroupAddon editLabel = new InputGroupAddon();
                Button removeButton = new Button("Remove");
                removeButton.setPull(Pull.RIGHT);
                removeButton.addStyleName("track-edit-button");
                removeButton.addClickHandler(new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        removeTrack(trackInfo.getName());
                    }
                });
                label.add(removeButton);
//                Button infoButton = new Button("?");
//                infoButton.setPull(Pull.RIGHT);
//                infoButton.addStyleName("track-edit-button");
//                infoButton.addClickHandler(new ClickHandler() {
//                    @Override
//                    public void onClick(ClickEvent event) {
////                        String extendedDirectoryName = configWrapperService.commonDataDirectory + File.separator + organism.id + "-" + organism.commonName
//                        Bootbox.alert("Track Location: " + File.separator + MainPanel.getInstance().getCurrentOrganism().get);
//                    }
//                });
//                label.add(infoButton);
//                Button editButton = new Button("Edit");
//                editButton.setPull(Pull.RIGHT);
//                editButton.addStyleName("track-edit-button");
//                editButton.addClickHandler(new ClickHandler() {
//                    @Override
//                    public void onClick(ClickEvent event) {
//                        Window.alert("editing");
//                    }
//                });
//                label.add(editButton);
//                Button hideButton = new Button("Hide");
//                hideButton.setPull(Pull.RIGHT);
//                hideButton.addStyleName("track-edit-button");
//                hideButton.addClickHandler(new ClickHandler() {
//                    @Override
//                    public void onClick(ClickEvent event) {
//                        Window.alert("hiding from public");
//                    }
//                });
//                label.add(hideButton);
            }

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
                    // clear previous labels
//                    label.addStyleName("selected-track-link");
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

            if (object.get("apollo") != null) trackInfo.setApollo(object.get("apollo").isObject());

            if (object.get("label") != null) trackInfo.setLabel(object.get("label").isString().stringValue());
            else Bootbox.alert("Track label should not be null, please check your tracklist");

            if (object.get("type") != null) trackInfo.setType(object.get("type").isString().stringValue());

            if (object.get("urlTemplate") != null)
                trackInfo.setUrlTemplate(object.get("urlTemplate").isString().stringValue());

            if (object.get("storeClass") != null)
                trackInfo.setStoreClass(object.get("storeClass").isString().stringValue());

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
                JSONValue v;
                try {
                    v = JSONParser.parseStrict(response.getText());
                } catch (Exception e) {
                    return;
                }
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
