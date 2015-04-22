package org.bbop.apollo.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.http.client.*;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.ui.ListBox;
import org.bbop.apollo.gwt.client.dto.*;
import org.bbop.apollo.gwt.client.event.*;
import org.bbop.apollo.gwt.client.rest.OrganismRestService;
import org.bbop.apollo.gwt.client.rest.SequenceRestService;
import org.bbop.apollo.gwt.client.rest.UserRestService;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;
import org.bbop.apollo.gwt.shared.PermissionEnum;
import org.gwtbootstrap3.client.ui.*;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Label;
import org.gwtbootstrap3.client.ui.constants.IconType;

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
//    private String rootUrl;
    public static Map<String, JavaScriptObject> annotrackFunctionMap = new HashMap<>();

    // state info
    // should I use a getter, or is this fine?
    static PermissionEnum highestPermission = PermissionEnum.NONE; // the current logged-in user
//    public static Long currentOrganismId = null;
//    public static String currentSequenceName = null;

    private UserInfo currentUser;
    private OrganismInfo currentOrganism;
    private List<SequenceInfo> currentSequenceList; // sequence list for current organisms
    private SequenceInfo currentSequence;
    private List<OrganismInfo> organismInfoList; // list of organisms for user
    private Annotator rootAnnotator;
    private static MainPanel instance;


    // debug
    private Boolean showFrame = false;
    private int maxUsernameLength = 15;


    @UiField
    Button dockOpenClose;
    @UiField(provided = false)
    NamedFrame frame;
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
    org.gwtbootstrap3.client.ui.Label currentOrganismDisplay;
    @UiField
    Label currentSequenceDisplay;


    public static MainPanel getInstance() {
        if (instance != null) {
            return instance;
        } else {
            Window.alert("No instance available . . initialized?");
            return null;
        }
    }

    public static MainPanel getInstance(Annotator annotator) {
        if (instance == null) {
            instance = new MainPanel(annotator);
        }
        return instance;
    }


    MainPanel(Annotator annotator) {
        this.rootAnnotator = annotator;
        instance = this;
        exportStaticMethod();
//        sequenceList = new SuggestBox(sequenceOracle);

        initWidget(ourUiBinder.createAndBindUi(this));


        GWT.log("name: " + frame.getName());
        frame.getElement().setAttribute("id", frame.getName());

        Dictionary dictionary = Dictionary.getDictionary("Options");
//        rootUrl = dictionary.get("rootUrl");
        showFrame = dictionary.get("showFrame") != null && dictionary.get("showFrame").contains("true");



        Annotator.eventBus.addHandler(AnnotationInfoChangeEvent.TYPE, new AnnotationInfoChangeEventHandler() {
            @Override
            public void onAnnotationChanged(AnnotationInfoChangeEvent annotationInfoChangeEvent) {
                switch (annotationInfoChangeEvent.getAction()) {
                    case SET_FOCUS:
                        AnnotationInfo annotationInfo = annotationInfoChangeEvent.getAnnotationInfo();
                        updateGenomicViewerForLocation(annotationInfo.getSequence(), annotationInfo.getMin(), annotationInfo.getMax());
                        break;
                }
            }
        });


        loginUser();
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
        String url = Annotator.getRootUrl()+"user/checkLogin";
        RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, URL.encode(url));
        builder.setHeader("Content-type", "application/x-www-form-urlencoded");
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                JSONObject returnValue = JSONParser.parseStrict(response.getText()).isObject();
                if (returnValue.containsKey(FeatureStringEnum.USER_ID.getValue())) {
                    getAppState();
//                    loadOrganisms();
                    logoutButton.setVisible(true);
                    currentUser = UserInfoConverter.convertToUserInfoFromJSON(returnValue);

                    String displayName = currentUser.getEmail();

                    userName.setHTML(displayName.length() > maxUsernameLength ?
                            displayName.substring(0, maxUsernameLength - 1) + "..." : displayName);
//                    userName.setHTML(displayName.length()>maxUsernameLength?
//                            displayName.substring(0, maxUsernameLength - 1) + "..." : displayName);
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


    public void updateGenomicViewerForLocation(String selectedSequence, Integer minRegion, Integer maxRegion) {
        Integer buffer = (int) Math.round((maxRegion - minRegion) * 0.5);
        minRegion -= buffer;
        if (minRegion < 0) minRegion = 0;
        maxRegion += buffer;
        String trackListString = Annotator.getRootUrl()+ "jbrowse/?loc=";
        trackListString += selectedSequence;
        trackListString += ":" + minRegion + ".." + maxRegion;
        trackListString += "&";
        for (TrackInfo trackInfo : TrackPanel.dataProvider.getList()) {
            trackListString += trackInfo.getName();
            trackListString += "&";
        }
        trackListString = trackListString.substring(0, trackListString.length() - 1);
        trackListString += "&highlight=&tracklist=0";
        GWT.log("set string: " + trackListString);
        GWT.log("get string: " + frame.getUrl());
        if (!frame.getUrl().contains(trackListString)) {
            frame.setUrl(trackListString);
        }
    }

    public void updateGenomicViewer() {
        String trackListString = Annotator.getRootUrl()+ "jbrowse/?loc=";
        GWT.log("get selected sequence: " + currentSequence.getName());
        trackListString += currentSequence.getName();

        trackListString += "&";
        for (TrackInfo trackInfo : TrackPanel.dataProvider.getList()) {
            trackListString += trackInfo.getName();
            trackListString += "&";
        }
        trackListString = trackListString.substring(0, trackListString.length() - 1);
        trackListString += "&highlight=&tracklist=0";
        GWT.log("set string: " + trackListString);
        frame.setUrl(trackListString);
    }

    public void setAppState(AppStateInfo appStateInfo) {
        organismInfoList = appStateInfo.getOrganismList();
        currentSequenceList = appStateInfo.getCurrentSequenceList();
        currentSequence = appStateInfo.getCurrentSequence();
        currentOrganism = appStateInfo.getCurrentOrganism();

        if(currentSequence!=null){
            currentSequenceDisplay.setHTML(currentSequence.getName());
        }

        if(currentOrganism!=null){
            currentOrganismDisplay.setHTML(currentOrganism.getName());
        }

        updatePermissionsForOrganism();

        updateGenomicViewer();

        Annotator.eventBus.fireEvent(new OrganismChangeEvent(OrganismChangeEvent.Action.LOADED_ORGANISMS));
    }

    public void getAppState() {
        String url = Annotator.getRootUrl()+ "annotator/getAppState";
        RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, URL.encode(url));
        builder.setHeader("Content-type", "application/x-www-form-urlencoded");
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                JSONObject returnValue = JSONParser.parseStrict(response.getText()).isObject();
                AppStateInfo appStateInfo = AppInfoConverter.convertFromJson(returnValue);
                setAppState(appStateInfo);
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

    @UiHandler("dockOpenClose")
    void handleClick(ClickEvent event) {
        toggleOpen();
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
            // close
            closePanel();
        } else {
            // open
            openPanel();
        }

        mainSplitPanel.animate(400);

        toggleOpen = !toggleOpen;
    }


//    public void setRootUrl(String rootUrl) {
//        this.rootUrl = rootUrl;
//    }


    public static void registerFunction(String name, JavaScriptObject javaScriptObject) {
        annotrackFunctionMap.put(name, javaScriptObject);
    }

    @UiHandler("generateLink")
    public void generateLink(ClickEvent clickEvent) {
        UrlDialogBox urlDialogBox = new UrlDialogBox(frame.getUrl());
        urlDialogBox.setWidth("600px");

        urlDialogBox.show();
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
     * @param payload
     */
    public static void handleNavigationEvent(String payload) {
        JSONObject navEvent = JSONParser.parseLenient(payload).isObject();
        GWT.log("event hapened: "+navEvent.toString());

        Integer fmin = (int) navEvent.get("start").isNumber().doubleValue();
        Integer fmax = (int) navEvent.get("end").isNumber().doubleValue();
        String sequenceNameString = navEvent.get("ref").isString().stringValue();


    }

    public static native void exportStaticMethod() /*-{
        $wnd.reloadAnnotations = $entry(@org.bbop.apollo.gwt.client.MainPanel::reloadAnnotator());
        $wnd.reloadSequences = $entry(@org.bbop.apollo.gwt.client.MainPanel::reloadSequences());
        $wnd.reloadOrganisms = $entry(@org.bbop.apollo.gwt.client.MainPanel::reloadOrganisms());
        $wnd.reloadUsers = $entry(@org.bbop.apollo.gwt.client.MainPanel::reloadUsers());
        $wnd.reloadUserGroups = $entry(@org.bbop.apollo.gwt.client.MainPanel::reloadUserGroups());
        $wnd.registerFunction = $entry(@org.bbop.apollo.gwt.client.MainPanel::registerFunction(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;));
        $wnd.handleNavigationEvent = $entry(@org.bbop.apollo.gwt.client.MainPanel::handleNavigationEvent(Ljava/lang/String;));
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

    public List<SequenceInfo> getCurrentSequenceList() {
        return currentSequenceList;
    }

    public void setCurrentSequenceList(List<SequenceInfo> currentSequenceList) {
        this.currentSequenceList = currentSequenceList;
    }

    public SequenceInfo getCurrentSequence() {
        return currentSequence;
    }

    public void setCurrentSequence(SequenceInfo currentSequence) {
        this.currentSequence = currentSequence;
    }

    public List<OrganismInfo> getOrganismInfoList() {
        return organismInfoList;
    }

    public void setOrganismInfoList(List<OrganismInfo> organismInfoList) {
        this.organismInfoList = organismInfoList;
    }

}
