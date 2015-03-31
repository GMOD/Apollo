package org.bbop.apollo.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsonUtils;
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
import org.gwtbootstrap3.client.ui.Button;
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
    private String rootUrl;
    public static Long currentOrganismId = null;
    public static String currentSequenceId = null;
    public static Map<String, JavaScriptObject> annotrackFunctionMap = new HashMap<>();

    // debug
    private Boolean showFrame = false;

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
    static ListBox organismList;
    @UiField(provided = true)
    static SuggestBox sequenceList;
    @UiField
    FlowPanel westPanel;
    @UiField
    PreferencePanel preferencePanel;
    @UiField
    Button logoutButton;
    @UiField
    HTML userName;
    
    private static UserInfo currentUser ; // the current logged-in user
    private MultiWordSuggestOracle sequenceOracle = new MultiWordSuggestOracle();


    public MainPanel() {
        exportStaticMethod();
        sequenceList = new SuggestBox(sequenceOracle);

        initWidget(ourUiBinder.createAndBindUi(this));


        GWT.log("name: " + frame.getName());
        frame.getElement().setAttribute("id", frame.getName());

        Dictionary dictionary = Dictionary.getDictionary("Options");
        rootUrl = dictionary.get("rootUrl");
        showFrame = dictionary.get("showFrame") != null && dictionary.get("showFrame").contains("true");


        Annotator.eventBus.addHandler(OrganismChangeEvent.TYPE, new OrganismChangeEventHandler() {
            @Override
            public void onOrganismChanged(OrganismChangeEvent organismChangeEvent) {
                List<OrganismInfo> organismInfoList = organismChangeEvent.getOrganismInfoList();
                organismList.clear();
                for (OrganismInfo organismInfo : organismInfoList) {
                    organismList.addItem(organismInfo.getName(), organismInfo.getId());
                }
            }
        });

        loginUser();
    }

    private void loginUser() {
        String url = rootUrl + "/user/checkLogin";
        RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, URL.encode(url));
        builder.setHeader("Content-type", "application/x-www-form-urlencoded");
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                JSONObject returnValue = JSONParser.parseStrict(response.getText()).isObject();
                if (returnValue.isObject().size() > 0) {
                    loadOrganisms(organismList);
                    logoutButton.setVisible(true);
                    currentUser = UserInfoConverter.convertToUserInfoFromJSON(returnValue);
                    String username = currentUser.getEmail();

                    int maxLength = 15 ;
                    if (username.length() > maxLength) {
                        username = username.substring(0, maxLength-1) + "...";
                    }

                    userName.setHTML(username);
                } else {
                    currentUser = null ; 
                    logoutButton.setVisible(false);
                    LoginDialog loginDialog = new LoginDialog();
                    loginDialog.center();
                    loginDialog.show();
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

    public void handleOrganismChange() {
        updateGenomicViewer();
        loadReferenceSequences(true);


    }

    @UiHandler("organismList")
    public void changeOrganism(ChangeEvent event) {
        String selectedValue = organismList.getSelectedValue();
        currentOrganismId = Long.parseLong(selectedValue);
        sequenceList.setText("");
        sequenceOracle.clear();
        OrganismRestService.changeOrganism(this, selectedValue);
    }

    @UiHandler("sequenceList")
    public void changeSequence(SelectionEvent<SuggestOracle.Suggestion> event) {
        updateGenomicViewer();
        SequenceRestService.setDefaultSequence(sequenceList.getText());
    }

    public void updateGenomicViewer() {
        String trackListString = rootUrl + "/jbrowse/?loc=";
        String selectedSequence = sequenceList.getText();
        GWT.log("get selected sequence: " + selectedSequence);
        trackListString += selectedSequence;

        trackListString += "&";
        for (TrackInfo trackInfo : trackPanel.dataProvider.getList()) {
            trackListString += trackInfo.getName();
            trackListString += "&";
        }
        trackListString = trackListString.substring(0, trackListString.length() - 1);
        trackListString += "&highlight=&tracklist=0";
        GWT.log("set string: " + trackListString);
        frame.setUrl(trackListString);
    }

    public void loadReferenceSequences() {
        loadReferenceSequences(false);
    }

    /**
     * could use an sequence callback . . . however, this element needs to use the callback directly.
     */
    public void loadReferenceSequences(final boolean loadFirstSequence) {
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                sequenceOracle.clear();
                sequenceList.setText("");
                JSONValue returnValue = JSONParser.parseStrict(response.getText());
                JSONArray array = returnValue.isArray();


                for (int i = 0; i < array.size(); i++) {
                    JSONObject object = array.get(i).isObject();
                    SequenceInfo sequenceInfo = new SequenceInfo();
                    sequenceInfo.setName(object.get("name").isString().stringValue());
                    sequenceInfo.setStart((int) object.get("start").isNumber().doubleValue());
                    sequenceInfo.setEnd((int) object.get("end").isNumber().doubleValue());
                    if (object.get("default") != null) {
                        sequenceInfo.setDefault(object.get("default").isBoolean().booleanValue());
                    }
                    sequenceOracle.add(sequenceInfo.getName());
                    if (sequenceInfo.isDefault()) {
                        GWT.log("setting name to default: " + sequenceInfo.getName());
                        sequenceList.setText(sequenceInfo.getName());
                        currentSequenceId = sequenceInfo.getName();
                    } else if (sequenceList.getText().isEmpty() && currentSequenceId != null && sequenceInfo.getName().equals(currentSequenceId)) {
                        GWT.log("setting name: " + currentSequenceId);
                        sequenceList.setText(sequenceInfo.getName());
                        currentSequenceId = sequenceInfo.getName();
                    }
//                    else
//                      if(sequenceList.getText().length()==0 && sequenceInfo.isDefault()) {
//                          sequenceList.setText(sequenceInfo.getName());
//                      }
                }

//                updateGenomicViewer();

                if (array.size() > 0) {
                    if (currentSequenceId == null) {
                        currentSequenceId = array.get(0).isObject().get("name").isString().stringValue();
                    }
                    String url = rootUrl + "/jbrowse/?loc=" + currentSequenceId;
                    if (!showFrame) {
                        url += "&tracklist=0";
                    }
                    frame.setUrl(url);
                }
//                if(sequenceList.getText().trim().length()==0){
//                    sequenceList.setText(array.get(0).object.get("name").isString().stringValue());
//                }

//                updateGenomicViewer();

                ContextSwitchEvent contextSwitchEvent = new ContextSwitchEvent(sequenceList.getText(), organismList.getSelectedValue());
                Annotator.eventBus.fireEvent(contextSwitchEvent);
//                reloadTabPerIndex(detailTabs.getSelectedIndex());
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Window.alert("Error loading organisms");
            }
        };
        // TODO: move to a javscript function in iFrame?
        SequenceRestService.loadSequences(requestCallback, MainPanel.currentOrganismId);

    }

    /**
     * could use an organism callback . . . however, this element needs to use the callback directly.
     *
     * @param trackInfoList
     */
    public void loadOrganisms(final ListBox trackInfoList) {
        String url = rootUrl + "/organism/findAllOrganisms";
        RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, URL.encode(url));
        builder.setHeader("Content-type", "application/x-www-form-urlencoded");
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                JSONValue returnValue = JSONParser.parseStrict(response.getText());
                JSONArray array = returnValue.isArray();

                for (int i = 0; i < array.size(); i++) {
                    JSONObject object = array.get(i).isObject();
//                    GWT.log(object.toString());
                    OrganismInfo organismInfo = new OrganismInfo();
                    organismInfo.setId(object.get("id").isNumber().toString());
                    organismInfo.setName(object.get("commonName").isString().stringValue());
                    organismInfo.setNumSequences((int) Math.round(object.get("sequences").isNumber().doubleValue()));
                    organismInfo.setDirectory(object.get("directory").isString().stringValue());
                    organismInfo.setCurrent(object.get("currentOrganism").isBoolean().booleanValue());
                    organismInfo.setNumFeatures(0);
                    organismInfo.setNumTracks(0);
//                    GWT.log(object.toString());
                    trackInfoList.addItem(organismInfo.getName(), organismInfo.getId());
                    if (organismInfo.isCurrent()) {
                        currentOrganismId = Long.parseLong(organismInfo.getId());
                        trackInfoList.setSelectedIndex(i);
                    }
//                    if (currentOrganismId != null) {
//                        if (Long.parseLong(organismInfo.getId()) == currentOrganismId) {
//                            trackInfoList.setSelectedIndex(i);
//                        }
//                    } else if (i == 0) {
//                        currentOrganismId = Long.parseLong(organismInfo.getId());
//                    }
                }

                if (currentOrganismId == null && array.size() > 0) {
                    JSONObject rootObject = array.get(0).isObject();
//                    String name = rootObject.get("commonName").isString().stringValue();
                    currentOrganismId = (long) rootObject.get("id").isNumber().doubleValue();
                    trackInfoList.setSelectedIndex(0);
                }

                loadReferenceSequences(true);
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


    public void setRootUrl(String rootUrl) {
        this.rootUrl = rootUrl;
    }


    public static void registerFunction(String name, JavaScriptObject javaScriptObject) {
        annotrackFunctionMap.put(name, javaScriptObject);
    }

    @UiHandler("logoutButton")
    public void logout(ClickEvent clickEvent) {
        UserRestService.logout();
    }

    public static UserInfo getCurrentUser() {
        return currentUser;
    }
    /*
     * Takes in a JSON String and evals it.
     * @param JSON String that you trust
     * @return JavaScriptObject that you can cast to an Overlay Type
     */
    public static <T extends JavaScriptObject> T parseJson(String jsonStr) {
        return JsonUtils.safeEval(jsonStr);
    }

    public static String executeFunction(String name) {
        return executeFunction(name, JavaScriptObject.createObject());
    }

    public static String executeFunction(String name, JavaScriptObject dataObject) {
        GWT.log("should be executing a function of some sort " + annotrackFunctionMap + " for name: " + name);
        JavaScriptObject targetFunction = annotrackFunctionMap.get(name);
        if (targetFunction == null) {
            return "function " + name + " not found";
        }
        GWT.log("function found!: " + targetFunction);
        return executeFunction(targetFunction, dataObject);
    }


    public static native String executeFunction(JavaScriptObject targetFunction, JavaScriptObject data) /*-{
        console.log('trying to execute a function: ' + targetFunction);
        console.log('with data: ' + data);
        return targetFunction(data);
        //return 'executed';
    }-*/;


    public static void reloadAnnotator() {
        GWT.log("!!! MainPanel::calling annotator relaod ");
        annotatorPanel.reload();
    }

    //    public static void loadTracks(JSONObject trackList){ trackPanel.loadTracks(trackList); }
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
//    public static void sampleFunction(){ Window.alert("sample function"); }


    //    $entry(@org.bbop.apollo.gwt.client.AnnotatorPanel::reload()());
    public static native void exportStaticMethod() /*-{
        $wnd.reloadAnnotations = $entry(@org.bbop.apollo.gwt.client.MainPanel::reloadAnnotator());
        //$wnd.loadTracks = $entry(@org.bbop.apollo.gwt.client.TrackPanel::updateTracks(Lcom/google/gwt/json/client/JSONObject;));
        $wnd.reloadSequences = $entry(@org.bbop.apollo.gwt.client.MainPanel::reloadSequences());
        $wnd.reloadOrganisms = $entry(@org.bbop.apollo.gwt.client.MainPanel::reloadOrganisms());
        $wnd.reloadUsers = $entry(@org.bbop.apollo.gwt.client.MainPanel::reloadUsers());
        $wnd.reloadUserGroups = $entry(@org.bbop.apollo.gwt.client.MainPanel::reloadUserGroups());
        $wnd.registerFunction = $entry(@org.bbop.apollo.gwt.client.MainPanel::registerFunction(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;));
        $wnd.getEmbeddedVersion = $entry(
            function apolloEmbeddedVersion() {
                return 'ApolloGwt-1.0';
            }
        );
        //$wnd.sampleFunction = $entry(@org.bbop.apollo.gwt.client.MainPanel::sampleFunction());
    }-*/;


}
