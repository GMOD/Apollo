package org.bbop.apollo.gwt.client;

import com.google.gwt.core.client.GWT;
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
import org.bbop.apollo.gwt.client.demo.DataGenerator;
import org.bbop.apollo.gwt.client.dto.OrganismInfo;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.constants.IconType;

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

    // debug
    private Boolean showFrame = false ;

    @UiField
    Button dockOpenClose;
    @UiField
    Frame frame;
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

    public MainPanel() {
        initWidget(ourUiBinder.createAndBindUi(this));
        exportStaticMethod();

//        frame.setUrl();
//        AnnotatorWidget3 annotatorWidget3 = new AnnotatorWidget3();
        Dictionary dictionary = Dictionary.getDictionary("Options");
        rootUrl = dictionary.get("rootUrl");
//        showFrame = !dictionary.get("showFrame").contains("false");
        String frameVariable = dictionary.get("showFrame");
        showFrame = dictionary.get("showFrame")!=null && dictionary.get("showFrame").contains("true");
        if(showFrame){
            frame.setUrl(rootUrl + "/jbrowse/?loc=Group1.3%3A14865..15198&tracks=DNA%2CAnnotations%2COfficial%20Gene%20Set%20v3.2%2CGeneID%2CCflo_OGSv3.3&highlight=");
        }
        else{
            frame.setUrl(rootUrl + "/jbrowse/?loc=Group1.3%3A14865..15198&tracks=DNA%2CAnnotations%2COfficial%20Gene%20Set%20v3.2%2CGeneID%2CCflo_OGSv3.3&highlight=&tracklist=0");
        }

//        westPanel.setVisible(true);

        loadOrganisms(organismList);
        DataGenerator.populateSequenceList(sequenceList);

//        detailTabs.selectTab(1);

//        Canvas canvas = Canvas.createIfSupported();
////        canvas.setHeight("300px");
//        westPanel.add(canvas);
//        Context2d context2d = canvas.getContext2d();
//        context2d.beginPath();
////        context2d.moveTo(20,180);
////        context2d.rotate(-Math.PI/2.0);
//
//        context2d.rotate(-Math.PI / 5.0);
//        context2d.setTextAlign("right");
//        context2d.fillRect(40,40, 20, 20);
//        context2d.fillText("adfas",10,20);
//        context2d.closePath();
    }

    public void loadOrganisms(final ListBox trackInfoList) {
        String url = "/apollo/organism/findAllOrganisms";
        RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, URL.encode(url));
        builder.setHeader("Content-type", "application/x-www-form-urlencoded");
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                JSONValue returnValue = JSONParser.parseStrict(response.getText());
                JSONArray array = returnValue.isArray();
//                Window.alert("array size: "+array.size());

                for(int i = 0 ; i < array.size() ; i++){
                    JSONObject object = array.get(i).isObject();
//                    GWT.log(object.toString());
                    OrganismInfo organismInfo = new OrganismInfo();
                    organismInfo.setId(object.get("id").isNumber().toString());
                    organismInfo.setName(object.get("commonName").isString().stringValue());
                    organismInfo.setNumSequences(object.get("sequences").isArray().size());
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
                annotatorPanel.loadAnnotations();
                break;
            case 1:
                trackPanel.dataGrid.redraw();
            case 2:
                sequencePanel.dataGrid.redraw();
                break;
            case 3:
                organismPanel.reloadOrganism();
                break;
            case 4:
                userPanel.dataGrid.redraw();
                break;
            case 5:
                userGroupPanel.dataGrid.redraw();
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
            mainSplitPanel.setWidgetSize(eastDockPanel, 500);
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


    public static void reloadAnnotator(){
        annotatorPanel.loadAnnotations();
    }

//    $entry(@org.bbop.apollo.gwt.client.AnnotatorPanel::loadAnnotations()());
    public static native void exportStaticMethod() /*-{
        $wnd.reloadAnnotations = $entry(@org.bbop.apollo.gwt.client.MainPanel::reloadAnnotator());
    }-*/;

}