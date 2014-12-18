package org.bbop.apollo.gwt.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.http.client.*;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.json.client.*;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.constants.IconType;
import org.gwtbootstrap3.client.ui.gwt.FlowPanel;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Annotator implements EntryPoint {

    boolean toggleOpen = true ;

    final SplitLayoutPanel mainLayoutPanel = new SplitLayoutPanel();

    final Button dockOpenClose = new Button();
    final TabLayoutPanel tabLayoutPanel = new TabLayoutPanel(30,Style.Unit.PX);
    final DockLayoutPanel dockLayoutPanel = new DockLayoutPanel(Style.Unit.PX);
    final Frame frame = new Frame();
    final FlowPanel titlePanel = new FlowPanel();

    final Track track = new Track();
    final AnnotatorPanel annotatorPanel = new AnnotatorPanel();
    final SequencePanel sequencePanel = new SequencePanel();
    final OrganismBrowserPanel organismBrowserPanel = new OrganismBrowserPanel();
    final UserBrowserPanel userBrowserPanel = new UserBrowserPanel();
    final UserGroupBrowserPanel userGroupBrowserPanel = new UserGroupBrowserPanel();

    /**
     * This is the entry point method.
     */
    public void onModuleLoad() {

//        AnnotatorWidget3 annotatorWidget3 = new AnnotatorWidget3();
        Dictionary dictionary = Dictionary.getDictionary("Options");
        String rootUrl = dictionary.get("rootUrl");

//        frame.setUrl(rootUrl+"/jbrowse/?loc=Group1.3%3A14865..15198&tracklist=0&tracks=DNA%2CAnnotations%2COfficial%20Gene%20Set%20v3.2%2CGeneID%2CCflo_OGSv3.3&highlight=");
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



        tabLayoutPanel.add(track, "Tracks");

        tabLayoutPanel.add(annotatorPanel, "Annotations");
        tabLayoutPanel.add(sequencePanel, "Sequence");
        tabLayoutPanel.add(organismBrowserPanel, "Organism");
        tabLayoutPanel.add(userBrowserPanel, "Users");
        tabLayoutPanel.add(userGroupBrowserPanel, "UserGroups");
        tabLayoutPanel.selectTab(4);

        // need to do this to force them to refresh themselves
        tabLayoutPanel.addSelectionHandler(new SelectionHandler<Integer>() {
            @Override
            public void onSelection(SelectionEvent<Integer> event) {
                switch (event.getSelectedItem()){
                    case 0: track.dataGrid.redraw(); break ;
                    case 2: sequencePanel.dataGrid.redraw(); break ;
                    case 3: organismBrowserPanel.organismTable.redraw(); break ;
                    case 4: userBrowserPanel.dataGrid.redraw(); break ;
                    case 5: userGroupBrowserPanel.dataGrid.redraw(); break ;
                    default: break ;
                }

            }
        });


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
            mainLayoutPanel.setWidgetSize(dockLayoutPanel, 500);
            dockOpenClose.setIcon(IconType.CARET_RIGHT);
        }

        mainLayoutPanel.animate(400);

        toggleOpen  = !toggleOpen;
    }


}
