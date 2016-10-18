package org.bbop.apollo.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.http.client.*;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.ui.ListBox;
import org.bbop.apollo.gwt.client.dto.*;
import org.bbop.apollo.gwt.client.event.AnnotationInfoChangeEvent;
import org.bbop.apollo.gwt.client.event.AnnotationInfoChangeEventHandler;
import org.bbop.apollo.gwt.client.event.OrganismChangeEvent;
import org.bbop.apollo.gwt.client.event.UserChangeEvent;
import org.bbop.apollo.gwt.client.rest.OrganismRestService;
import org.bbop.apollo.gwt.client.rest.SequenceRestService;
import org.bbop.apollo.gwt.client.rest.UserRestService;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;
import org.bbop.apollo.gwt.shared.PermissionEnum;
import org.gwtbootstrap3.client.ui.*;
import org.gwtbootstrap3.client.ui.Anchor;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.SuggestBox;
import org.gwtbootstrap3.client.ui.constants.AlertType;
import org.gwtbootstrap3.client.ui.constants.IconType;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;

import java.util.*;

/**
 * Created by ndunn on 12/18/14.
 */
public class MainPanel extends Composite {



    interface MainPanelUiBinder extends UiBinder<Widget, MainPanel> {
    }

    private static MainPanelUiBinder ourUiBinder = GWT.create(MainPanelUiBinder.class);

    private boolean toggleOpen = true;
    public static Map<String, JavaScriptObject> annotrackFunctionMap = new HashMap<>();

    // state info
    static PermissionEnum highestPermission = PermissionEnum.NONE; // the current logged-in user
    private static UserInfo currentUser;
    private static OrganismInfo currentOrganism;
    private static SequenceInfo currentSequence;
    private static Integer currentStartBp; // start base pair
    private static Integer currentEndBp; // end base pair
    private static Map<String,List<String>> currentQueryParams ; // list of organisms for user
    public static boolean useNativeTracklist; // list native tracks
    private static List<OrganismInfo> organismInfoList = new ArrayList<>(); // list of organisms for user
    private static final String trackListViewString = "&tracklist=";

    private static boolean handlingNavEvent = false;


    private static MainPanel instance;
    private int maxUsernameLength = 15;
    private static final double UPDATE_DIFFERENCE_BUFFER = 0.3;
    private static final double GENE_VIEW_BUFFER = 0.4;
    private static List<String> reservedList = new ArrayList<>();


    @UiField
    Button dockOpenClose;
    @UiField(provided = false)
    static NamedFrame frame;
    @UiField
    static AnnotatorPanel annotatorPanel;
    @UiField
    static TrackPanel trackPanel;
    @UiField
    static SequencePanel sequencePanel;
    @UiField
    static OrganismPanel organismPanel;
    @UiField
    static UserPanel userPanel;
    @UiField
    static GroupPanel userGroupPanel;
    @UiField
    static DockLayoutPanel eastDockPanel;
    @UiField(provided = true)
    static SplitLayoutPanel mainSplitPanel;
    @UiField
    static TabLayoutPanel detailTabs;
    @UiField
    FlowPanel westPanel;
    @UiField
    PreferencePanel preferencePanel;
    @UiField
    Button logoutButton;
    @UiField
    Button userName;
    @UiField
    Button generateLink;
    @UiField
    ListBox organismListBox;
    @UiField(provided = true)
    static SuggestBox sequenceSuggestBox;
    @UiField
    Modal notificationModal;
    @UiField
    Alert alertText;
    @UiField
    Button logoutButton2;
    @UiField
    Anchor logoutAndBrowsePublicGenomes;
    @UiField
    Modal editUserModal;
    @UiField
    Input editMyPasswordInput;
    @UiField
    Button savePasswordButton;
    @UiField
    Button cancelPasswordButton;
    @UiField
    Input editMyPasswordInputRepeat;
    @UiField
    Alert editUserAlertText;
    @UiField
    HTML editUserHeader;


    private LoginDialog loginDialog = new LoginDialog();
    private RegisterDialog registerDialog = new RegisterDialog();


    public static MainPanel getInstance() {
        if (instance == null) {
            instance = new MainPanel();
        }
        return instance;
    }


    MainPanel() {
        instance = this;
        sequenceSuggestBox = new SuggestBox(new ReferenceSequenceOracle());

        mainSplitPanel = new SplitLayoutPanel() {
            @Override
            public void onResize() {
                super.onResize();
                Annotator.setPreference(FeatureStringEnum.DOCK_WIDTH.getValue(), mainSplitPanel.getWidgetSize(eastDockPanel));
            }
        };

        exportStaticMethod();

        initWidget(ourUiBinder.createAndBindUi(this));
        frame.getElement().setAttribute("id", frame.getName());
        Annotator.eventBus.addHandler(AnnotationInfoChangeEvent.TYPE, new AnnotationInfoChangeEventHandler() {
            @Override
            public void onAnnotationChanged(AnnotationInfoChangeEvent annotationInfoChangeEvent) {
                switch (annotationInfoChangeEvent.getAction()) {
                    case SET_FOCUS:
                        AnnotationInfo annotationInfo = annotationInfoChangeEvent.getAnnotationInfo();
                        int start = annotationInfo.getMin();
                        int end = annotationInfo.getMax();
                        int newLength = end - start;
                        start -= newLength * GENE_VIEW_BUFFER;
                        end += newLength * GENE_VIEW_BUFFER;
                        start = start < 0 ? 0 : start;
                        updateGenomicViewerForLocation(annotationInfo.getSequence(), start, end);
                        break;
                }
            }
        });

        sequenceSuggestBox.addSelectionHandler(new SelectionHandler<SuggestOracle.Suggestion>() {
            @Override
            public void onSelection(SelectionEvent<SuggestOracle.Suggestion> event) {
                setCurrentSequence(sequenceSuggestBox.getText().trim(), null, null, true, false);
            }
        });


        try {
            String dockOpen = Annotator.getPreference(FeatureStringEnum.DOCK_OPEN.getValue());
            if (dockOpen != null) {
                Boolean setDockOpen = Boolean.valueOf(dockOpen);
                toggleOpen = !setDockOpen;
                toggleOpen();
            }
        } catch (Exception e) {
            GWT.log("Error setting preference: " + e.fillInStackTrace().toString());
            Annotator.setPreference(FeatureStringEnum.DOCK_OPEN.getValue(), true);
        }


        try {
            String dockWidth = Annotator.getPreference(FeatureStringEnum.DOCK_WIDTH.getValue());
            if (dockWidth != null && toggleOpen) {
                Integer dockWidthInt = Integer.parseInt(dockWidth);
                mainSplitPanel.setWidgetSize(eastDockPanel, dockWidthInt);
            }
        } catch (NumberFormatException e) {
            GWT.log("Error setting preference: " + e.fillInStackTrace().toString());
            Annotator.setPreference(FeatureStringEnum.DOCK_WIDTH.getValue(), 600);
        }

        setUserNameForCurrentUser();

        String tabPreferenceString = Annotator.getPreference(FeatureStringEnum.CURRENT_TAB.getValue());
        try {
            int selectedTab = Integer.parseInt(tabPreferenceString);
            detailTabs.selectTab(selectedTab);
            if(selectedTab==TabPanelIndex.TRACKS.index){
                trackPanel.reloadIfEmpty();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        currentQueryParams = Window.Location.getParameterMap();

        reservedList.add("loc");
        reservedList.add("trackList");

        loginUser();
    }

    private static void setCurrentSequence(String sequenceNameString, final Integer start, final Integer end) {
        setCurrentSequence(sequenceNameString, start, end, false, false);
    }

    private static void sendCurrentSequenceLocation(String sequenceNameString, final Integer start, final Integer end) {

        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                currentStartBp = start;
                currentEndBp = end;
                handlingNavEvent = false;
            }

            @Override
            public void onError(Request request, Throwable exception) {
                handlingNavEvent = false;
                Bootbox.alert("failed to set sequence location: " + exception);
            }
        };

        handlingNavEvent = true;
        SequenceRestService.setCurrentSequenceAndLocation(requestCallback, sequenceNameString, start, end, true);

    }

    private static void setCurrentSequence(String sequenceNameString, final Integer start, final Integer end, final boolean updateViewer, final boolean blocking) {

        final LoadingDialog loadingDialog = new LoadingDialog(false);
        if (blocking) {
            loadingDialog.show();
        }

        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                handlingNavEvent = false;
                JSONObject sequenceInfoJson = JSONParser.parseStrict(response.getText()).isObject();
                currentSequence = SequenceInfoConverter.convertFromJson(sequenceInfoJson);
                currentStartBp = start != null ? start : 0;
                currentEndBp = end != null ? end : currentSequence.getEnd();
                sequenceSuggestBox.setText(currentSequence.getName());


                Annotator.eventBus.fireEvent(new OrganismChangeEvent(OrganismChangeEvent.Action.LOADED_ORGANISMS, currentSequence.getName(),currentOrganism.getName()));

                if (updateViewer) {
                    updateGenomicViewer();
                }
                if (blocking) {
                    loadingDialog.hide();
                }
            }

            @Override
            public void onError(Request request, Throwable exception) {
                handlingNavEvent = false;
                if (blocking) {
                    loadingDialog.hide();
                }
                Bootbox.alert("failed to set JBrowse sequence: " + exception);
            }
        };

        handlingNavEvent = true;
        SequenceRestService.setCurrentSequenceAndLocation(requestCallback, sequenceNameString, start, end);

    }


    private void updatePermissionsForOrganism() {
        String globalRole = currentUser.getRole();
        UserOrganismPermissionInfo userOrganismPermissionInfo = currentUser.getOrganismPermissionMap().get(currentOrganism.getName());
        if (globalRole.equals("admin")) {
            highestPermission = PermissionEnum.ADMINISTRATE;
        } else {
            highestPermission = PermissionEnum.NONE;
        }
        if (userOrganismPermissionInfo != null && highestPermission != PermissionEnum.ADMINISTRATE) {
            highestPermission = userOrganismPermissionInfo.getHighestPermission();
        }

        switch (highestPermission) {
            case ADMINISTRATE:
                GWT.log("setting to ADMINISTRATE permissions");
                detailTabs.getTabWidget(TabPanelIndex.USERS.index).getParent().setVisible(true);
                detailTabs.getTabWidget(TabPanelIndex.GROUPS.index).getParent().setVisible(true);
                detailTabs.getTabWidget(TabPanelIndex.ORGANISM.index).getParent().setVisible(true);
                detailTabs.getTabWidget(TabPanelIndex.PREFERENCES.index).getParent().setVisible(true);
                break;
            case WRITE:
                GWT.log("setting to WRITE permissions");
            case EXPORT:
                GWT.log("setting to EXPORT permissions");
            case READ:
                GWT.log("setting to READ permissions");
                //break; <-- uncomment if want non-admin users to view panels
            case NONE:
            default:
                GWT.log("setting to no permissions");
                // let's set the view
                detailTabs.getTabWidget(TabPanelIndex.USERS.index).getParent().setVisible(false);
                detailTabs.getTabWidget(TabPanelIndex.GROUPS.index).getParent().setVisible(false);
                detailTabs.getTabWidget(TabPanelIndex.ORGANISM.index).getParent().setVisible(false);
                detailTabs.getTabWidget(TabPanelIndex.PREFERENCES.index).getParent().setVisible(false);

                break;
        }

        UserChangeEvent userChangeEvent = new UserChangeEvent(UserChangeEvent.Action.PERMISSION_CHANGED, highestPermission);
        Annotator.eventBus.fireEvent(userChangeEvent);
    }

    private void loginUser() {
        String url = Annotator.getRootUrl() + "user/checkLogin";
        url += "?clientToken=" + Annotator.getClientToken();
        RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, URL.encode(url));
        builder.setHeader("Content-type", "application/x-www-form-urlencoded");
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                JSONObject returnValue = JSONParser.parseStrict(response.getText()).isObject();
                if (returnValue.containsKey(FeatureStringEnum.USER_ID.getValue())) {
                    if (returnValue.containsKey(FeatureStringEnum.ERROR.getValue())) {
                        String errorText = returnValue.get(FeatureStringEnum.ERROR.getValue()).isString().stringValue();
                        alertText.setText(errorText);
                        detailTabs.setVisible(false);
                        notificationModal.show();
                    } else {
                        detailTabs.setVisible(true);
                        getAppState();
                        logoutButton.setVisible(true);
                        currentUser = UserInfoConverter.convertToUserInfoFromJSON(returnValue);
                        Annotator.startSessionTimer();
                        if (returnValue.containsKey("tracklist")) {
                            MainPanel.useNativeTracklist = returnValue.get("tracklist").isBoolean().booleanValue();
                        } else {
                            MainPanel.useNativeTracklist = false;
                        }
                        trackPanel.updateTrackToggle(MainPanel.useNativeTracklist);


                        setUserNameForCurrentUser();
                    }


                } else {
                    boolean hasUsers = returnValue.get(FeatureStringEnum.HAS_USERS.getValue()).isBoolean().booleanValue();
                    if (hasUsers) {
                        currentUser = null;
                        logoutButton.setVisible(false);
                        loginDialog.showLogin();
                    } else {
                        currentUser = null;
                        logoutButton.setVisible(false);
                        registerDialog.center();
                        registerDialog.show();
                    }
                }
            }

            @Override
            public void onError(Request request, Throwable exception) {
                loginDialog.setError(exception.getMessage());
            }
        };
        try {
            builder.setCallback(requestCallback);
            builder.send();
        } catch (RequestException e) {
            loginDialog.setError(e.getMessage());
        }

    }

    private void setUserNameForCurrentUser() {
        if (currentUser == null) return;
        String displayName = currentUser.getEmail();
        userName.setText(displayName.length() > maxUsernameLength ?
                displayName.substring(0, maxUsernameLength - 1) + "..." : displayName);
    }

    public static void updateGenomicViewerForLocation(String selectedSequence, Integer minRegion, Integer maxRegion) {
        updateGenomicViewerForLocation(selectedSequence, minRegion, maxRegion, false);
    }

    /**
     * @param selectedSequence
     * @param minRegion
     * @param maxRegion
     */
    public static void updateGenomicViewerForLocation(String selectedSequence, Integer minRegion, Integer maxRegion, boolean forceReload) {

        if (!forceReload && currentSequence != null && currentSequence.getName().equals(selectedSequence) && currentStartBp != null && currentEndBp != null && minRegion > 0 && maxRegion > 0 && frame.getUrl().startsWith("http")) {
            int oldLength = maxRegion - minRegion;
            double diff1 = (Math.abs(currentStartBp - minRegion)) / (float) oldLength;
            double diff2 = (Math.abs(currentEndBp - maxRegion)) / (float) oldLength;
            if (diff1 < UPDATE_DIFFERENCE_BUFFER && diff2 < UPDATE_DIFFERENCE_BUFFER) {
                return;
            }
        }

        currentStartBp = minRegion;
        currentEndBp = maxRegion;


        String trackListString = Annotator.getRootUrl() ;
        trackListString +=  Annotator.getClientToken() +"/";
        trackListString += "jbrowse/index.html?loc=";
        trackListString += selectedSequence;
        trackListString += URL.encodeQueryString(":") + minRegion + ".." + maxRegion;

        trackListString += getCurrentQueryParamsAsString();


        // if the trackList contains a string, it should over-ride and set?
        if(trackListString.contains(trackListViewString)){
            // replace with whatever is in the toggle ? ? ?
            Boolean showTrackValue = trackPanel.trackListToggle.getValue();

            String positiveString = trackListViewString+"1";
            String negativeString = trackListViewString+"0";
            if(trackListString.contains(positiveString) && !showTrackValue){
                trackListString = trackListString.replace(positiveString,negativeString);
            }
            else
            if(trackListString.contains(negativeString) && showTrackValue){
                trackListString = trackListString.replace(negativeString,positiveString);
            }

            MainPanel.useNativeTracklist = showTrackValue ;
        }
        // otherwise we use the nativeTrackList
        else{
            trackListString += "&tracklist=" + (MainPanel.useNativeTracklist ? "1" : "0");
        }

        frame.setUrl(trackListString);
    }


    private static String getCurrentQueryParamsAsString() {
        String returnString = "";
        if(currentQueryParams==null){
            return returnString;
        }

        for(String key : currentQueryParams.keySet()){
            if(!reservedList.contains(key)){
                for(String value : currentQueryParams.get(key)){
                    returnString += "&"+key + "=" + value;
                }
            }
        }
        return returnString;
    }

    public static void updateGenomicViewer(boolean forceReload) {
        if (currentStartBp != null && currentEndBp != null) {
            updateGenomicViewerForLocation(currentSequence.getName(), currentStartBp, currentEndBp, forceReload);
        } else {
            updateGenomicViewerForLocation(currentSequence.getName(), currentSequence.getStart(), currentSequence.getEnd(), forceReload);
        }
    }

    public static void updateGenomicViewer() {
        updateGenomicViewer(false);
    }

    public void setAppState(AppStateInfo appStateInfo) {
        organismInfoList = appStateInfo.getOrganismList();
        currentSequence = appStateInfo.getCurrentSequence();
        currentOrganism = appStateInfo.getCurrentOrganism();
        currentStartBp = appStateInfo.getCurrentStartBp();
        currentEndBp = appStateInfo.getCurrentEndBp();

        if (currentSequence != null) {
            sequenceSuggestBox.setText(currentSequence.getName());
        }


        organismListBox.clear();
        for (OrganismInfo organismInfo : organismInfoList) {
            organismListBox.addItem(organismInfo.getName(), organismInfo.getId());
            if (currentOrganism.getId().equals(organismInfo.getId())) {
                organismListBox.setSelectedIndex(organismListBox.getItemCount() - 1);
            }
        }

        if (currentOrganism != null) {
            updatePermissionsForOrganism();
            updateGenomicViewer(true);
        }


        Scheduler.get().scheduleFixedDelay(new Scheduler.RepeatingCommand() {
            @Override
            public boolean execute() {
                Annotator.eventBus.fireEvent(new OrganismChangeEvent(OrganismChangeEvent.Action.LOADED_ORGANISMS));
                return false;
            }
        }, 500);
    }

    public void getAppState() {
        String url = Annotator.getRootUrl() + "annotator/getAppState";
        url += "?"+FeatureStringEnum.CLIENT_TOKEN.getValue() + "=" + Annotator.getClientToken();
        RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, URL.encode(url));
        builder.setHeader("Content-type", "application/x-www-form-urlencoded");
        final LoadingDialog loadingDialog = new LoadingDialog();
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                JSONValue j = JSONParser.parseStrict(response.getText());
                JSONObject obj = j.isObject();
                if (obj != null && obj.containsKey("error")) {
//                    Bootbox.alert(obj.get("error").isString().stringValue());
                    loadingDialog.hide();
                } else {
                    loadingDialog.hide();
                    AppStateInfo appStateInfo = AppInfoConverter.convertFromJson(obj);
                    setAppState(appStateInfo);
                }
            }

            @Override
            public void onError(Request request, Throwable exception) {
                loadingDialog.hide();
                Bootbox.alert("Error loading organisms");
            }
        };
        try {
            builder.setCallback(requestCallback);
            builder.send();
        } catch (RequestException e) {
            loadingDialog.hide();
            Bootbox.alert(e.getMessage());
        }
    }

    @UiHandler("cancelPasswordButton")
    void cancelEditUserPassword(ClickEvent event) {
        editUserModal.hide();
    }


    @UiHandler("savePasswordButton")
    void saveEditUserPassword(ClickEvent event) {
        UserInfo currentUser = MainPanel.getInstance().getCurrentUser();
        if (editMyPasswordInput.getText().equals(editMyPasswordInputRepeat.getText())) {
            currentUser.setPassword(editMyPasswordInput.getText());
        } else {
            editUserAlertText.setVisible(true);
            editUserAlertText.setText("Passwords do not match");
            return;
        }
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
//                {"error":"Failed to update the user You have insufficient permissions [write < administrate] to perform this operation"}
                if (response.getText().startsWith("{\"error\":")) {
                    JSONObject errorJsonObject = JSONParser.parseStrict(response.getText()).isObject();
                    String errorMessage = errorJsonObject.get("error").isString().stringValue();

                    editUserAlertText.setType(AlertType.DANGER);
                    editUserAlertText.setVisible(true);
                    editUserAlertText.setText(errorMessage);
                    return;
                }
                savePasswordButton.setEnabled(false);
                cancelPasswordButton.setEnabled(false);
                editUserAlertText.setType(AlertType.SUCCESS);
                editUserAlertText.setVisible(true);
                editUserAlertText.setText("Saved!");
                Scheduler.get().scheduleFixedDelay(new Scheduler.RepeatingCommand() {
                    @Override
                    public boolean execute() {
                        editUserModal.setFade(true);
                        editUserModal.hide();
                        return false;
                    }
                }, 1000);
            }

            @Override
            public void onError(Request request, Throwable exception) {
                editUserAlertText.setVisible(true);
                editUserAlertText.setText("Error setting user password: " + exception.getMessage());
                editUserModal.hide();
            }
        };
        UserRestService.updateUser(requestCallback, currentUser);
    }

    @UiHandler("userName")
    void editUserPassword(ClickEvent event) {
        editUserHeader.setHTML("Edit password for " + currentUser.getName() + "(" + currentUser.getEmail() + ")");
        editUserAlertText.setText("");
        editUserAlertText.setVisible(false);
        editMyPasswordInput.setText("");
        editMyPasswordInputRepeat.setText("");
        editUserModal.show();
        savePasswordButton.setEnabled(true);
        cancelPasswordButton.setEnabled(true);
    }

    @UiHandler("dockOpenClose")
    void handleClick(ClickEvent event) {
        toggleOpen();
    }

    @UiHandler("organismListBox")
    void handleOrganismChange(ChangeEvent changeEvent) {
        OrganismRestService.switchOrganismById(organismListBox.getSelectedValue());
    }


    @UiHandler("detailTabs")
    public void onSelection(SelectionEvent<Integer> event) {
        Annotator.setPreference(FeatureStringEnum.CURRENT_TAB.getValue(), event.getSelectedItem());
        reloadTabPerIndex(event.getSelectedItem());
    }

    private void reloadTabPerIndex(Integer selectedItem) {
        switch (selectedItem) {
            case 0:
                annotatorPanel.reload(true);
                break;
            case 1:
                trackPanel.reload();
                break;
            case 2:
                sequencePanel.reload(true);
                break;
            case 3:
                organismPanel.reload();
                break;
            case 4:
                userPanel.reload();
                break;
            case 5:
                userGroupPanel.reload();
                break;
            case 6:
                preferencePanel.reload();
                break;
            default:
                break;
        }
    }

    private void closePanel() {
        mainSplitPanel.setWidgetSize(eastDockPanel, 20);
        dockOpenClose.setIcon(IconType.CARET_LEFT);
    }

    private void openPanel() {
        String dockWidth = Annotator.getPreference(FeatureStringEnum.DOCK_WIDTH.getValue());
        if (dockWidth != null) {
            Integer dockWidthInt = Integer.parseInt(dockWidth);
            mainSplitPanel.setWidgetSize(eastDockPanel, dockWidthInt);
        } else {
            mainSplitPanel.setWidgetSize(eastDockPanel, 550);
        }
        dockOpenClose.setIcon(IconType.CARET_RIGHT);
    }

    private void toggleOpen() {
        if (mainSplitPanel.getWidgetSize(eastDockPanel) < 100) {
            toggleOpen = false;
        }

        if (toggleOpen) {
            closePanel();
        } else {
            openPanel();
        }

        mainSplitPanel.animate(400);

        toggleOpen = !toggleOpen;
        Annotator.setPreference(FeatureStringEnum.DOCK_OPEN.getValue(), toggleOpen);
    }


    public static void registerFunction(String name, JavaScriptObject javaScriptObject) {
        annotrackFunctionMap.put(name, javaScriptObject);
    }


    @UiHandler("generateLink")
    public void toggleLink(ClickEvent clickEvent) {
        String text = "";
        String publicUrl = URL.encode(generatePublicUrl());
        String apolloUrl = URL.encode(generateApolloUrl());
        text += "<div style='margin-left: 10px;'>";
        text += "<ul>";
        text += "<li>";
        text += "Public URL: <a href='" + publicUrl + "'>" + publicUrl + "</a>";
        text += "</li>";
        text += "<li>";
        text += "Apollo URL: <a href='" + apolloUrl + "'>" + apolloUrl + "</a>";
        text += "</li>";
        text += "</ul>";
        text += "</div>";
        new LinkDialog("Links to this Location", text, true);
    }

    public String generatePublicUrl() {
        String url2 = Annotator.getRootUrl();
        url2 += currentOrganism.getId()+"/";
        url2 += "jbrowse/index.html";
        if (currentStartBp != null) {
            url2 += "?loc=" + currentSequence.getName() + ":" + currentStartBp + ".." + currentEndBp;
        } else {
            url2 += "?loc=" + currentSequence.getName() + ":" + currentSequence.getStart() + ".." + currentSequence.getEnd();
        }
//        url2 += "&organism=" + currentOrganism.getId();
        url2 += "&tracks=";

        List<String> trackList = trackPanel.getTrackList();
        for (int i = 0; i < trackList.size(); i++) {
            url2 += trackList.get(i);
            if (i < trackList.size() - 1) {
                url2 += ",";
            }
        }
        return url2;
    }

    public String generateApolloUrl() {
        String url = Annotator.getRootUrl();
        url += "annotator/loadLink";
        if (currentStartBp != null) {
            url += "?loc=" + currentSequence.getName() + ":" + currentStartBp + ".." + currentEndBp;
        } else {
            url += "?loc=" + currentSequence.getName() + ":" + currentSequence.getStart() + ".." + currentSequence.getEnd();
        }
        url += "&organism=" + currentOrganism.getId();
        url += "&tracks=";

        List<String> trackList = trackPanel.getTrackList();
        for (int i = 0; i < trackList.size(); i++) {
            url += trackList.get(i);
            if (i < trackList.size() - 1) {
                url += ",";
            }
        }
        return url;
    }

    @UiHandler(value = {"logoutAndBrowsePublicGenomes"})
    public void logoutAndBrowse(ClickEvent clickEvent) {
        UserRestService.logout("../jbrowse");
    }


    @UiHandler(value = {"logoutButton", "logoutButton2"})
    public void logout(ClickEvent clickEvent) {
        UserRestService.logout();
    }


    public static String executeFunction(String name) {
        return executeFunction(name, JavaScriptObject.createObject());
    }

    public static String executeFunction(String name, JavaScriptObject dataObject) {
        JavaScriptObject targetFunction = annotrackFunctionMap.get(name);
        if (targetFunction == null) {
            return "function " + name + " not found";
        }
        return executeFunction(targetFunction, dataObject);
    }


    public static native String executeFunction(JavaScriptObject targetFunction, JavaScriptObject data) /*-{
        return targetFunction(data);
    }-*/;


    public static void reloadAnnotator() {
        GWT.log("MainPanel reloadAnnotator");
        annotatorPanel.reload();
    }

    public static void reloadSequences() {
        sequencePanel.reload();
    }

    public static void reloadOrganisms() {
        organismPanel.reload();
    }

    public static void reloadUsers() {
        userPanel.reload();
    }

    public static void reloadUserGroups() {
        userGroupPanel.reload();
    }

    /**
     * currRegion:{"start":6000,"end":107200,"ref":"chrI"}
     *
     * @param payload
     */
    public static void handleNavigationEvent(String payload) {
        if (handlingNavEvent) return;

        handlingNavEvent = true;
        JSONObject navEvent = JSONParser.parseLenient(payload).isObject();

        final Integer start = (int) navEvent.get("start").isNumber().doubleValue();
        final Integer end = (int) navEvent.get("end").isNumber().doubleValue();
        String sequenceNameString = navEvent.get("ref").isString().stringValue();

        if (!sequenceNameString.equals(currentSequence.getName())) {
            setCurrentSequence(sequenceNameString, start, end, false, true);
            Scheduler.get().scheduleFixedPeriod(new Scheduler.RepeatingCommand() {
                @Override
                public boolean execute() {
                    return handlingNavEvent;
                }
            }, 200);

        } else {
            sendCurrentSequenceLocation(sequenceNameString, start, end);
        }

    }

    /**
     * Features array handed in
     *
     * @param payload
     */
    public static void handleFeatureAdded(String payload) {
        if (detailTabs.getSelectedIndex() == 0) {
            annotatorPanel.reload();
        }
    }

    /**
     * Features array handed in
     *
     * @param payload
     */
    public static void handleFeatureDeleted(String payload) {
        if (detailTabs.getSelectedIndex() == 0) {
            Scheduler.get().scheduleDeferred(new Command() {
                @Override
                public void execute() {
                    annotatorPanel.reload();
                }
            });
        }

    }

    /**
     * Features array handed in
     *
     * @param payload
     */
    public static void handleFeatureUpdated(String payload) {
        if (detailTabs.getSelectedIndex() == 0) {
            annotatorPanel.reload();
        }
    }


    public static String getCurrentSequenceAsJson() {
        if (currentSequence == null) {
            return "{}";
        }
        return currentSequence.toJSON().toString();
    }

    public static boolean hasCurrentUser() {
        return currentUser!=null ;
    }

    public static String getCurrentUserAsJson() {
        if (currentUser == null) {
            return "{}";
        }
        return currentUser.getJSONWithoutPassword().toString();
    }

    public static String getCurrentOrganismAsJson() {
        if (currentOrganism == null) {
            return "{}";
        }
        return currentOrganism.toJSON().toString();
    }

    public static native void exportStaticMethod() /*-{
        $wnd.reloadAnnotations = $entry(@org.bbop.apollo.gwt.client.MainPanel::reloadAnnotator());
        $wnd.reloadSequences = $entry(@org.bbop.apollo.gwt.client.MainPanel::reloadSequences());
        $wnd.reloadOrganisms = $entry(@org.bbop.apollo.gwt.client.MainPanel::reloadOrganisms());
        $wnd.reloadUsers = $entry(@org.bbop.apollo.gwt.client.MainPanel::reloadUsers());
        $wnd.reloadUserGroups = $entry(@org.bbop.apollo.gwt.client.MainPanel::reloadUserGroups());
        $wnd.registerFunction = $entry(@org.bbop.apollo.gwt.client.MainPanel::registerFunction(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;));
        $wnd.handleNavigationEvent = $entry(@org.bbop.apollo.gwt.client.MainPanel::handleNavigationEvent(Ljava/lang/String;));
        $wnd.handleFeatureAdded = $entry(@org.bbop.apollo.gwt.client.MainPanel::handleFeatureAdded(Ljava/lang/String;));
        $wnd.handleFeatureDeleted = $entry(@org.bbop.apollo.gwt.client.MainPanel::handleFeatureDeleted(Ljava/lang/String;));
        $wnd.handleFeatureUpdated = $entry(@org.bbop.apollo.gwt.client.MainPanel::handleFeatureUpdated(Ljava/lang/String;));
        $wnd.getCurrentOrganism = $entry(@org.bbop.apollo.gwt.client.MainPanel::getCurrentOrganismAsJson());
        $wnd.getCurrentUser = $entry(@org.bbop.apollo.gwt.client.MainPanel::getCurrentUserAsJson());
        $wnd.getCurrentSequence = $entry(@org.bbop.apollo.gwt.client.MainPanel::getCurrentSequenceAsJson());
        $wnd.getEmbeddedVersion = $entry(
            function apolloEmbeddedVersion() {
                return 'ApolloGwt-2.0';
            }
        );
    }-*/;

    private enum TabPanelIndex {
        ANNOTATIONS(0),
        TRACKS(1),
        SEQUENCES(2),
        ORGANISM(3),
        USERS(4),
        GROUPS(5),
        PREFERENCES(6),;

        private int index;

        TabPanelIndex(int index) {
            this.index = index;
        }

    }

    public boolean isCurrentUserAdmin() {
        return (currentUser != null && currentUser.getRole().equals("admin"));
    }

    public UserInfo getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(UserInfo currentUser) {
        this.currentUser = currentUser;
    }

    public OrganismInfo getCurrentOrganism() {
        return currentOrganism;
    }

    public void setCurrentOrganism(OrganismInfo currentOrganism) {
        this.currentOrganism = currentOrganism;
    }

    public List<OrganismInfo> getOrganismInfoList() {
        return organismInfoList;
    }

    public void setOrganismInfoList(List<OrganismInfo> organismInfoList) {
        this.organismInfoList = organismInfoList;
    }

    public static SequencePanel getSequencePanel() {
        return sequencePanel;
    }
}
