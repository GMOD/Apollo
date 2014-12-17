package org.bbop.apollo.gwt.client;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.user.client.ui.*;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.constants.IconType;
import org.gwtbootstrap3.client.ui.gwt.FlowPanel;
//import com.google.gwt.user.client.ui.*;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class AnnotatorWidget3 {

    boolean toggleOpen = true ;

    final SplitLayoutPanel mainLayoutPanel = new SplitLayoutPanel();

    final Button dockOpenClose = new Button();
    final TabLayoutPanel tabLayoutPanel = new TabLayoutPanel(30,Style.Unit.PX);
    final DockLayoutPanel dockLayoutPanel = new DockLayoutPanel(Style.Unit.PX);
    final Frame frame = new Frame();
    final FlowPanel titlePanel = new FlowPanel();


    /**
     * This is the entry point method.
     */
    public AnnotatorWidget3() {
        Dictionary dictionary = Dictionary.getDictionary("Options");
        String rootUrl = dictionary.get("rootUrl");

        frame.setUrl(rootUrl+"/jbrowse/?loc=Group1.3%3A14865..15198&tracklist=0&tracks=DNA%2CAnnotations%2COfficial%20Gene%20Set%20v3.2%2CGeneID%2CCflo_OGSv3.3&highlight=");
        frame.setHeight("100%");
        frame.setWidth("100%");


        dockOpenClose.setIcon(IconType.CARET_RIGHT);

        titlePanel.add(dockOpenClose);
        HTML detailsTitle = new HTML("<div>Details<div>");
        titlePanel.add(detailsTitle);
        detailsTitle.setStyleName("details-header-title");
        titlePanel.setStyleName("details-header");
        dockOpenClose.setStyleName("details-button");
        dockOpenClose.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                toggleOpen();
            }
        });




        SequenceBrowserPanel sequenceBrowserPanel = new SequenceBrowserPanel();
        FeaturePanel featurePanel = new FeaturePanel();
        OrganismPanel organismPanel = new OrganismPanel();
        UserPanel userPanel = new UserPanel();
        GroupPanel groupPanel = new GroupPanel();
        TrackPanel trackPanel = new TrackPanel();
        Track track = new Track();
//        tabLayoutPanel.add(new HTML("this"),"[this]");
        tabLayoutPanel.add(track, "Tracks");
//        tabLayoutPanel.add(trackPanel, "Tracks");
        tabLayoutPanel.add(featurePanel, "Annotations");
        tabLayoutPanel.add(sequenceBrowserPanel, "Sequence");
        tabLayoutPanel.add(organismPanel, "Organism");
        tabLayoutPanel.add(userPanel, "Users");
        tabLayoutPanel.add(groupPanel, "UserGroups");
        tabLayoutPanel.selectTab(1);



        dockLayoutPanel.addNorth(titlePanel,30);
        dockLayoutPanel.add(tabLayoutPanel);

        tabLayoutPanel.setWidth("100%");
        tabLayoutPanel.setHeight("100%");

        dockLayoutPanel.setWidth("100%");
//        sequencePanel.setHeight("800px");

        mainLayoutPanel.addEast(dockLayoutPanel, 500);
        mainLayoutPanel.add(frame);
        RootLayoutPanel rp = RootLayoutPanel.get();

        rp.add(mainLayoutPanel);
        rp.setWidgetTopHeight(mainLayoutPanel, 0, Style.Unit.PX, 100, Style.Unit.PCT);


        // Focus the cursor on the name field when the app loads
//        nameField.setFocus(true);
//        nameField.selectAll();
//
//        nameField.addChangeHandler(new ChangeHandler() {
//
//            @Override
//            public void onChange(ChangeEvent event) {
//                String url = "/apollo/annotator/search";
//                RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, URL.encode(url));
//                JSONObject jsonObject = new JSONObject();
//                jsonObject.put("query", new JSONString("pax6a"));
//                builder.setRequestData("data=" + jsonObject.toString());
//                builder.setHeader("Content-type", "application/x-www-form-urlencoded");
//                RequestCallback requestCallback = new RequestCallback() {
//                    @Override
//                    public void onResponseReceived(Request request, Response response) {
//                        JSONValue returnValue = JSONParser.parseStrict(response.getText());
//                        JSONObject jsonObject = returnValue.isObject();
//                        String queryString = jsonObject.get("query").isString().stringValue();
//
//                        // TODO: use proper array parsing
//                        String resultString = jsonObject.get("result").isString().stringValue();
//                        resultString = resultString.replace("[", "");
//                        resultString = resultString.replace("]", "");
//                        searchResult.setText(" asdflkj asdflkjdas fsearch for " + queryString + " yields [" + resultString + "]");
//                    }
//
//                    @Override
//                    public void onError(Request request, Throwable exception) {
//                        Window.alert("ow");
//                    }
//                };
//                try {
//                    builder.setCallback(requestCallback);
//                    builder.send();
//                } catch (RequestException e) {
//                    // Couldn't connect to server
//                    Window.alert(e.getMessage());
//                }
//
//            }
//        });
    }


    private void toggleOpen() {
        if(mainLayoutPanel.getWidgetSize(dockLayoutPanel)< 100){
            toggleOpen = false ;
        }

        if(toggleOpen){
            mainLayoutPanel.setWidgetSize(dockLayoutPanel,20);
            dockOpenClose.setIcon(IconType.CARET_LEFT);
        }
        else{
            mainLayoutPanel.setWidgetSize(dockLayoutPanel,500);
            dockOpenClose.setIcon(IconType.CARET_RIGHT);
        }

        mainLayoutPanel.animate(400);

        toggleOpen  = !toggleOpen;
    }
}
