package org.bbop.apollo.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
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
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.ui.ListBox;
import org.bbop.apollo.gwt.client.demo.DataGenerator;
import org.bbop.apollo.gwt.client.dto.OrganismInfo;
import org.bbop.apollo.gwt.client.dto.SequenceInfo;
import org.bbop.apollo.gwt.client.dto.TrackInfo;
import org.bbop.apollo.gwt.client.rest.OrganismRestService;
import org.bbop.apollo.gwt.client.rest.SequenceRestService;
import org.gwtbootstrap3.client.ui.*;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.constants.IconType;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ndunn on 12/18/14.
 */
public class MainPanel extends Composite {


    interface MainPanelUiBinder extends UiBinder<Widget, MainPanel> {
    }

    private static MainPanelUiBinder ourUiBinder = GWT.create(MainPanelUiBinder.class);

    private boolean toggleOpen = true;
    private String rootUrl;
    private String userId;
    private Integer currentOrganismId = 0 ;
    public static List<JavaScriptObject> functionList = new ArrayList<>();

    // debug
    private Boolean showFrame = false ;

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
    static UserGroupPanel userGroupPanel;
    @UiField
    static DockLayoutPanel eastDockPanel;
    @UiField
    static SplitLayoutPanel mainSplitPanel;
    @UiField
    static TabLayoutPanel detailTabs;
    @UiField
    static ListBox organismList;
    @UiField
    static ListBox sequenceList;
    @UiField
    FlowPanel westPanel;
    @UiField
    PreferencePanel preferencePanel;


    public MainPanel() {
        exportStaticMethod();
        initWidget(ourUiBinder.createAndBindUi(this));
//        frame = new NamedFrame("genomeViewer");
        GWT.log("name: "+ frame.getName());
        frame.getElement().setAttribute("id",frame.getName());
//        DOM.setElementAttribute(frame.getElement(), "id", frame.getName());
        // set ID for Name
//        DOM.setElementAttribute(frame.getElement(), "id", frame.getName());

        Dictionary dictionary = Dictionary.getDictionary("Options");
        rootUrl = dictionary.get("rootUrl");
        userId = dictionary.get("userId");
        showFrame = dictionary.get("showFrame")!=null && dictionary.get("showFrame").contains("true");
        if(showFrame){
            frame.setUrl(rootUrl + "/jbrowse/?loc=Group1.3%3A14865..15198&tracks=DNA%2CAnnotations%2COfficial%20Gene%20Set%20v3.2%2CGeneID%2CCflo_OGSv3.3&highlight=&userId="+userId);
        }
        else{
            frame.setUrl(rootUrl + "/jbrowse/?loc=Group1.3%3A14865..15198&tracks=DNA%2CAnnotations%2COfficial%20Gene%20Set%20v3.2%2CGeneID%2CCflo_OGSv3.3&highlight=&tracklist=0&userId="+userId);
        }

        loadOrganisms(organismList);
        loadReferenceSequences(sequenceList);

////        GWT.log("genome1: " + Document.get().getDocumentElement().getE("genomeViewer").getLength());
//        GWT.log("genome2: " + Document.get().getElementById("genomeViewer"));
//
//        Scheduler.get().scheduleDeferred(new Command() {
//            public void execute() {
////                dataEntry.setFocus();
//                execCommand(frame, "whatev", true, "yo");
//            }
//        });
    }

//    public static native void execCommand(NamedFrame frame, String
//            command, boolean ui, String value)/*-{
//        var frameName =
//            frame.@com.google.gwt.user.client.ui.NamedFrame::getName()();
//        var oIframe = $wnd.document.getElementsByName(frameName)[0];
//        console.log(oIframe.contentWindow.doFancy2());
//        //var oDoc = oIframe.contentWindow || oIframe.contentDocument;
//        var oDoc = oIframe.contentWindow || oIframe.contentDocument;
//        //console.log(oDoc);
//        //if (oDoc.document) {
//        //    oDoc = oDoc.document;
//        //}
//        //oIframe.contentWindow.doFancy();
//        //oDoc.doFancy1();
//        //oDoc.doFancy2();
//        //oDoc.doFancy3();
//        //oDoc.execCommand(command, ui, value);
//    }-*/;

    @UiHandler("sequenceList")
    public void changeSequence(ChangeEvent event){
        String trackListString = rootUrl + "/jbrowse/?loc=";
        String selectedSequence = sequenceList.getSelectedValue();
        GWT.log("get selected sequence: "+selectedSequence);
        trackListString += selectedSequence;

        trackListString += "&";
        for(TrackInfo trackInfo : trackPanel.dataProvider.getList()){
            trackListString += trackInfo.getName();
            trackListString += "&";
        }
        trackListString = trackListString.substring(0,trackListString.length()-1);
        trackListString += "&highlight=&tracklist=0" ;
        GWT.log("set string: "+trackListString);
//        frame.setUrl(rootUrl + "/jbrowse/?loc=Group1.3%3A14865..15198&tracks=DNA%2CAnnotations%2COfficial%20Gene%20Set%20v3.2%2CGeneID%2CCflo_OGSv3.3&highlight=&tracklist=0");
        frame.setUrl(trackListString);
    }

    /**
     * could use an sequence callback . . . however, this element needs to use the callback directly.
     * @param sequenceInfoList
     */
    public void loadReferenceSequences(final ListBox sequenceInfoList) {
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                JSONValue returnValue = JSONParser.parseStrict(response.getText());
                JSONArray array = returnValue.isArray();

                for(int i = 0 ; i < array.size() ; i++){
                    JSONObject object = array.get(i).isObject();
                    SequenceInfo sequenceInfo = new SequenceInfo();
                    sequenceInfo.setName(object.get("name").isString().stringValue());
                    sequenceInfo.setLength((int) object.get("length").isNumber().isNumber().doubleValue());
                    sequenceInfoList.addItem(sequenceInfo.getName());
                }
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Window.alert("Error loading organisms");
            }
        };
        SequenceRestService.loadSequences(requestCallback);

    }

    /**
     * could use an organism callback . . . however, this element needs to use the callback directly.
     * @param trackInfoList
     */
    public void loadOrganisms(final ListBox trackInfoList) {
        String url = rootUrl+"/organism/findAllOrganisms";
//        String url = "/apollo/organism/findAllOrganisms";
        RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, URL.encode(url));
        builder.setHeader("Content-type", "application/x-www-form-urlencoded");
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                JSONValue returnValue = JSONParser.parseStrict(response.getText());
                JSONArray array = returnValue.isArray();

                for(int i = 0 ; i < array.size() ; i++){
                    JSONObject object = array.get(i).isObject();
//                    GWT.log(object.toString());
                    OrganismInfo organismInfo = new OrganismInfo();
                    organismInfo.setId(object.get("id").isNumber().toString());
                    organismInfo.setName(object.get("commonName").isString().stringValue());
                    organismInfo.setNumSequences(object.get("sequences").isArray().size());
                    organismInfo.setDirectory(object.get("directory").isString().stringValue());
                    organismInfo.setNumFeatures(0);
                    organismInfo.setNumTracks(0);
//                    GWT.log(object.toString());
                    trackInfoList.addItem(organismInfo.getName(), organismInfo.getId());
                }
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
        switch (event.getSelectedItem()) {
            case 0:
                annotatorPanel.reload();
                break;
            case 1:
                GWT.log(executeFunction(functionList.get(0)));
                GWT.log(executeFunction(functionList.get(1),"{data:'something'}"));
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



    private void toggleOpen() {
        if (mainSplitPanel.getWidgetSize(eastDockPanel) < 100) {
            toggleOpen = false;
        }

        if (toggleOpen) {
            // close
            mainSplitPanel.setWidgetSize(eastDockPanel, 20);
            dockOpenClose.setIcon(IconType.CARET_LEFT);
//            detailTabs.setVisible(false);
//            westPanel.setVisible(true);
//            westPanel.setWidth("20px");
//            eastDockPanel.addWest(westPanel,20);
//            westPanel.setPixelSize(20,1000);
//            eastDockPanel.forceLayout();
        } else {
            // open
            mainSplitPanel.setWidgetSize(eastDockPanel, 550);
            dockOpenClose.setIcon(IconType.CARET_RIGHT);
//            detailTabs.setVisible(true);
//            westPanel.setVisible(false);
//            westPanel.setPixelSize(0,1000);
//            eastDockPanel.remove(westPanel);
//            westPanel.setWidth("1px");
//            eastDockPanel.forceLayout();
        }

        mainSplitPanel.animate(400);

        toggleOpen = !toggleOpen;
    }


    public void setRootUrl(String rootUrl) {
        this.rootUrl = rootUrl;
    }


    public static void registerFunction(JavaScriptObject javaScriptObject){
        functionList.add(javaScriptObject);
    }


    /*
     * Takes in a JSON String and evals it.
     * @param JSON String that you trust
     * @return JavaScriptObject that you can cast to an Overlay Type
     */
    public static <T extends JavaScriptObject> T parseJson(String jsonStr)
    {
//        return JsonUtils.safeEval(jsonStr);
        return JsonUtils.unsafeEval(jsonStr);
    }

    public static String executeFunction(JavaScriptObject targetFunction){
        return executeFunction(targetFunction,JavaScriptObject.createObject());
    }

    public static String executeFunction(JavaScriptObject targetFunction,String data){
//        JavaScriptObject dataObject = parseJson(data);
        String jsonString = "{'data':'asdf'}";
        JavaScriptObject dataObject = parseJson(jsonString);
//        JavaScriptObject dataObject = JavaScriptObject.createObject();
        return executeFunction(targetFunction,dataObject);
    }

    public static native String executeFunction(JavaScriptObject targetFunction,JavaScriptObject data) /*-{
        return targetFunction(data);
        //return 'executed';
    }-*/;


    public static void reloadAnnotator(){
        GWT.log("!!! MainPanel::calling annotator relaod ");
        annotatorPanel.reload();
    }
//    public static void loadTracks(JSONObject trackList){ trackPanel.loadTracks(trackList); }
    public static void reloadSequences(){ sequencePanel.reload(); }
    public static void reloadOrganisms(){ organismPanel.reload(); }
    public static void reloadUsers(){ userPanel.reload(); }
    public static void reloadUserGroups(){ userGroupPanel.reload(); }
//    public static void sampleFunction(){ Window.alert("sample function"); }


//    $entry(@org.bbop.apollo.gwt.client.AnnotatorPanel::reload()());
    public static native void exportStaticMethod() /*-{
        $wnd.reloadAnnotations = $entry(@org.bbop.apollo.gwt.client.MainPanel::reloadAnnotator());
        //$wnd.loadTracks = $entry(@org.bbop.apollo.gwt.client.TrackPanel::updateTracks(Lcom/google/gwt/json/client/JSONObject;));
        $wnd.reloadSequences = $entry(@org.bbop.apollo.gwt.client.MainPanel::reloadSequences());
        $wnd.reloadOrganisms = $entry(@org.bbop.apollo.gwt.client.MainPanel::reloadOrganisms());
        $wnd.reloadUsers = $entry(@org.bbop.apollo.gwt.client.MainPanel::reloadUsers());
        $wnd.reloadUserGroups= $entry(@org.bbop.apollo.gwt.client.MainPanel::reloadUserGroups());
        $wnd.registerFunction = $entry(@org.bbop.apollo.gwt.client.MainPanel::registerFunction(Lcom/google/gwt/core/client/JavaScriptObject;));
        //$wnd.sampleFunction = $entry(@org.bbop.apollo.gwt.client.MainPanel::sampleFunction());
    }-*/;

}