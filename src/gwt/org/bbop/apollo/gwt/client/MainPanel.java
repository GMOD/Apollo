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
import org.bbop.apollo.gwt.client.dto.*;
import org.bbop.apollo.gwt.client.event.*;
import org.bbop.apollo.gwt.client.rest.OrganismRestService;
import org.bbop.apollo.gwt.client.rest.SequenceRestService;
import org.bbop.apollo.gwt.client.rest.UserRestService;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;
import org.bbop.apollo.gwt.shared.PermissionEnum;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.constants.IconType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private static Integer currentStartBp; // list of organisms for user
    private static Integer currentEndBp; // list of organisms for user
    private static List<OrganismInfo> organismInfoList = new ArrayList<>(); // list of organisms for user

    private static boolean handlingNavEvent = false;


    private static MainPanel instance;
    private int maxUsernameLength = 15;
    private static final double UPDATE_DIFFERENCE_BUFFER = 0.1;
    private static final double GENE_VIEW_BUFFER = 0.4;


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
    @UiField
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
    HTML userName;
    @UiField
    Button generateLink;
    @UiField
    com.google.gwt.user.client.ui.ListBox organismListBox;
    @UiField(provided = true)
    static SuggestBox sequenceSuggestBox;
    @UiField
    HTML linkUrl;
    @UiField
    FlowPanel linkPanel;

    private MultiWordSuggestOracle sequenceOracle = new ReferenceSequenceOracle();


    public static MainPanel getInstance() {
        if (instance == null) {
            instance = new MainPanel();
        }
        return instance;
    }


    MainPanel() {
        instance = this;
        sequenceSuggestBox = new SuggestBox(sequenceOracle);
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
                        start = start < 0 ? 0 : start ;
                        updateGenomicViewerForLocation(annotationInfo.getSequence(), start , end);
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


        loginUser();
    }

    private static void setCurrentSequence(String sequenceNameString, final Integer start, final Integer end) {
        setCurrentSequence(sequenceNameString, start, end, false, false);
    }

    private static void sendCurrentSequenceLocation(String sequenceNameString, final Integer start, final Integer end) {

        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                handlingNavEvent = false;
            }

            @Override
            public void onError(Request request, Throwable exception) {
                handlingNavEvent = false;
                Window.alert("failed to set sequence location: " + exception);
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


                Annotator.eventBus.fireEvent(new OrganismChangeEvent(OrganismChangeEvent.Action.LOADED_ORGANISMS, currentSequence.getName()));

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
                Window.alert("failed to set JBrowse sequence: " + exception);
            }
        };

        handlingNavEvent = true;
        SequenceRestService.setCurrentSequenceAndLocation(requestCallback, sequenceNameString, start, end);

    }


    private void updatePermissionsForOrganism() {
        GWT.log(currentUser.getOrganismPermissionMap().keySet().toString());
        String globalRole = currentUser.getRole();
        GWT.log("global: " + globalRole);
        UserOrganismPermissionInfo userOrganismPermissionInfo = currentUser.getOrganismPermissionMap().get(currentOrganism.getName());
        if (globalRole.equals("admin")) {
            highestPermission = PermissionEnum.ADMINISTRATE;
        } else {
            highestPermission = PermissionEnum.NONE;
        }
        if (userOrganismPermissionInfo != null) {
            GWT.log("organism: " + userOrganismPermissionInfo.toJSON().toString());
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
        RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, URL.encode(url));
        builder.setHeader("Content-type", "application/x-www-form-urlencoded");
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                JSONObject returnValue = JSONParser.parseStrict(response.getText()).isObject();
                if (returnValue.containsKey(FeatureStringEnum.USER_ID.getValue())) {
                    if (returnValue.containsKey(FeatureStringEnum.ERROR.getValue())) {
//                        Window.alert(returnValue.get(FeatureStringEnum.ERROR.getValue()).isString().stringValue());
                        new ErrorDialog("Error", returnValue.get(FeatureStringEnum.ERROR.getValue()).isString().stringValue(), true,false);
                    } else {
                        getAppState();
                        logoutButton.setVisible(true);
                        currentUser = UserInfoConverter.convertToUserInfoFromJSON(returnValue);

                        String displayName = currentUser.getEmail();

                        userName.setHTML(displayName.length() > maxUsernameLength ?
                                displayName.substring(0, maxUsernameLength - 1) + "..." : displayName);
                    }
                } else {
                    boolean hasUsers = returnValue.get(FeatureStringEnum.HAS_USERS.getValue()).isBoolean().booleanValue();
                    if (hasUsers) {
                        currentUser = null;
                        logoutButton.setVisible(false);
                        LoginDialog loginDialog = new LoginDialog();
                        loginDialog.center();
                        loginDialog.show();
                    } else {
                        currentUser = null;
                        logoutButton.setVisible(false);
                        RegisterDialog registerDialog = new RegisterDialog();
                        registerDialog.center();
                        registerDialog.show();
                    }
                }
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Window.alert("User not there: " + exception);
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

    /**
     * @param selectedSequence
     * @param minRegion
     * @param maxRegion
     */
    public static void updateGenomicViewerForLocation(String selectedSequence, Integer minRegion, Integer maxRegion) {

        if (currentStartBp != null && currentEndBp != null && minRegion > 0 && maxRegion > 0 && frame.getUrl().startsWith("http")) {
            int oldLength = maxRegion - minRegion ;
            double diff1 = (Math.abs(currentStartBp - minRegion)) / (float) oldLength;
            double diff2 = (Math.abs(currentEndBp - maxRegion)) / (float) oldLength;
            if (diff1 < UPDATE_DIFFERENCE_BUFFER && diff2 < UPDATE_DIFFERENCE_BUFFER) {
                return;
            }
        }

        currentStartBp = minRegion;
        currentEndBp = maxRegion;


        String trackListString = Annotator.getRootUrl() + "jbrowse/?loc=";
        trackListString += selectedSequence;
        trackListString += URL.encodeQueryString(":") + minRegion + ".." + maxRegion;
        trackListString += "&highlight=&tracklist=0";

        final String finalString = trackListString;

        frame.setUrl(finalString);
    }

    public static void updateGenomicViewer() {
        if (currentStartBp != null && currentEndBp != null) {
            updateGenomicViewerForLocation(currentSequence.getName(), currentStartBp, currentEndBp);
        } else {
            updateGenomicViewerForLocation(currentSequence.getName(), currentSequence.getStart(), currentSequence.getEnd());
        }
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

        if(currentOrganism!=null) {
            updatePermissionsForOrganism();
            updateGenomicViewer();
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
        RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, URL.encode(url));
        builder.setHeader("Content-type", "application/x-www-form-urlencoded");
        final LoadingDialog loadingDialog = new LoadingDialog();
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                JSONValue j = JSONParser.parseStrict(response.getText());
                JSONObject obj = j.isObject();
                if (obj != null && obj.containsKey("error")) {
                    Window.alert(obj.get("error").isString().stringValue());
                    loadingDialog.hide();
                } else {
                    loadingDialog.hide();
                    loadingDialog.hide();
                    AppStateInfo appStateInfo = AppInfoConverter.convertFromJson(obj);
                    setAppState(appStateInfo);
                }
            }

            @Override
            public void onError(Request request, Throwable exception) {
                loadingDialog.hide();
                Window.alert("Error loading organisms");
            }
        };
        try {
            builder.setCallback(requestCallback);
            builder.send();
        } catch (RequestException e) {
            loadingDialog.hide();
            Window.alert(e.getMessage());
        }

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
        reloadTabPerIndex(event.getSelectedItem());
    }

    private void reloadTabPerIndex(Integer selectedItem) {
        switch (selectedItem) {
            case 0:
                annotatorPanel.reload();
                break;
            case 1:
                trackPanel.reload();
                break;
            case 2:
                sequencePanel.reload();
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
            default:
                break;
        }
    }

    private void closePanel() {
        mainSplitPanel.setWidgetSize(eastDockPanel, 20);
        dockOpenClose.setIcon(IconType.CARET_LEFT);
    }

    private void openPanel() {
        mainSplitPanel.setWidgetSize(eastDockPanel, 550);
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
    }


    public static void registerFunction(String name, JavaScriptObject javaScriptObject) {
        annotrackFunctionMap.put(name, javaScriptObject);
    }

    @UiHandler("closeUrlButton")
    public void closeUrl(ClickEvent clickEvent) {
        closeLink();
    }

    public void closeLink() {
//        linkUrl.setHTML("");
        linkPanel.setVisible(false);
        mainSplitPanel.setWidgetSize(linkPanel, 0);
        mainSplitPanel.animate(100);
    }

    @UiHandler("generateLink")
    public void toggleLink(ClickEvent clickEvent) {
        if (linkPanel.isVisible()) {
            closeLink();
        } else {
            generateLink();
        }
    }

    public void generateLink() {
        String url = Annotator.getRootUrl();
        url += "annotator/loadLink";
        if (currentStartBp != null) {
            url += "?loc=" + currentSequence.getName() + ":" + currentStartBp + ".." + currentEndBp;
        } else {
            url += "?loc=" + currentSequence.getName() + ":" + currentSequence.getStart() + ".." + currentSequence.getEnd();
        }
        url += "&organism=" + currentOrganism.getId();
        url += "&highlight=0";
        url += "&tracklist=0";
        url += "&tracks=";

        List<String> trackList = trackPanel.getTrackList();
        for (int i = 0; i < trackList.size(); i++) {
            url += trackList.get(i);
            if (i < trackList.size() - 1) {
                url += ",";
            }
        }
        linkUrl.setText(url);
        linkPanel.setVisible(true);
        mainSplitPanel.setWidgetSize(linkPanel, 50);
        mainSplitPanel.animate(100);
    }

    @UiHandler("logoutButton")
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
        GWT.log("!!! MainPanel::calling annotator relaod ");
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
//                    Window.alert("waiting for this to be false: "+handlingNavEvent);
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
//        if (handlingNavEvent) return;
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
//        if (handlingNavEvent) return;

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
//        if (handlingNavEvent) return;
        if (detailTabs.getSelectedIndex() == 0) {
            annotatorPanel.reload();
        }
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
        $wnd.getEmbeddedVersion = $entry(
            function apolloEmbeddedVersion() {
                return 'ApolloGwt-1.0';
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

//    public SequenceInfo getCurrentSequence() {
//        return currentSequence;
//    }

//    public void setCurrentSequence(SequenceInfo currentSequence) {
//        this.currentSequence = currentSequence;
//    }

    public List<OrganismInfo> getOrganismInfoList() {
        return organismInfoList;
    }

    public void setOrganismInfoList(List<OrganismInfo> organismInfoList) {
        this.organismInfoList = organismInfoList;
    }

}
