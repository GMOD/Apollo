package org.bbop.apollo.gwt.client;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.http.client.*;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class AnnotatorWidget2 {

    final Panel searchPanel = new VerticalPanel();
    final TextBox nameField = new TextBox();
    final Label searchResult = new HTML("none");
    final CheckBox cdsCheckBox = new CheckBox();
    final CheckBox codonCheckBox = new CheckBox();

    final SplitLayoutPanel p = new SplitLayoutPanel();
    final SplitLayoutPanel navigationPanel = new SplitLayoutPanel();
    final Button dockOpenClose = new Button("&raquo;");

    /**
     * This is the entry point method.
     */
    public AnnotatorWidget2() {

        Dictionary dictionary = Dictionary.getDictionary("Options");
        String rootUrl = dictionary.get("rootUrl");

        Frame frame = new Frame(rootUrl+"/jbrowse/?loc=Group1.3%3A14865..15198&tracks=DNA%2CAnnotations%2COfficial%20Gene%20Set%20v3.2%2CGeneID%2CCflo_OGSv3.3&highlight=");
        frame.setHeight("100%");
        frame.setWidth("100%");




        nameField.setWidth("100%");
        nameField.setEnabled(true);
        nameField.setReadOnly(false);
//        searchPanel.add(searchButton);
        searchPanel.add(nameField);

        Panel check1Panel = new HorizontalPanel();
        check1Panel.add(cdsCheckBox);
        check1Panel.add(new HTML("&nbsp;Check CDS"));
        cdsCheckBox.setValue(true);
        searchPanel.add(check1Panel);

        Panel check2Panel = new HorizontalPanel();
        check2Panel.add(codonCheckBox);
        codonCheckBox.setValue(true);
        check2Panel.add(new HTML("&nbsp;Check Codons"));
        searchPanel.add(check2Panel);


        VerticalPanel northPanel = new VerticalPanel();

//        LayoutPanel titlePanel = new LayoutPanel();
        FlowPanel titlePanel = new FlowPanel();
//        titlePanel.setWidth("100%");
        titlePanel.add(dockOpenClose);
        HTML detailsTitle = new HTML("<div>Details<div>");
        titlePanel.add(detailsTitle);
//        titlePanel.setWidgetLeftWidth(dockOpenClose,0, Style.Unit.PX,30, Style.Unit.PX);
//        titlePanel.setWidgetRightWidth(detailsTitle,30, Style.Unit.PX,30, Style.Unit.PX);
        detailsTitle.setStyleName("details-header-title");
        titlePanel.setStyleName("details-header");
        dockOpenClose.setStyleName("details-button");
        dockOpenClose.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                toggleOpen();
            }
        });

        northPanel.add(titlePanel);

        TabPanel filterPanel = new TabPanel();
        filterPanel.setWidth("100%");
        filterPanel.add(searchPanel, "Organism");
        filterPanel.add(new HTML("Browse Form"), "Sequence");
        filterPanel.add(new HTML("Flag"), "Features");
        filterPanel.add(new HTML("Flag"), "Users");
        filterPanel.selectTab(0);

        northPanel.add(filterPanel);


        Tree tree = new Tree();
        TreeItem pax6a = new TreeItem();
        pax6a.setText("pax6a");
//        pax6a.addTextItem("pax6a-001");
        pax6a.addItem(new HTML("pax6a-001 <span class='label label-warning'>CDS</span>"));
        pax6a.addTextItem("pax6a-002");
        pax6a.addItem(new HTML("pax6a-006 <span class='label label-danger'>Codon</span>"));
        tree.addItem(pax6a);

        TreeItem sox9b = new TreeItem();
        sox9b.setText("sox9b");
        sox9b.addTextItem("sox9b-001");
        sox9b.addTextItem("sox9b-002");
        sox9b.addTextItem("sox9b-004");
        tree.addItem(sox9b);

        pax6a.setState(true);
        sox9b.setState(true);

        northPanel.setWidth("100%");

        navigationPanel.addNorth(northPanel, 200);
        final FeatureDetail2Panel featureDetailPanel = new FeatureDetail2Panel();
        navigationPanel.addSouth(featureDetailPanel, 200);
        navigationPanel.add(tree);

        tree.addSelectionHandler(new SelectionHandler<TreeItem>() {
            @Override
            public void onSelection(SelectionEvent<TreeItem> event) {
               navigationPanel.setWidgetSize(featureDetailPanel, 200);
               navigationPanel.animate(400);
            }
        });


        codonCheckBox.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                navigationPanel.setWidgetSize(featureDetailPanel, 0);
                navigationPanel.animate(400);
            }
        });




        p.addEast(navigationPanel, 500);
//        p.addSouth(featureDetailPanel, 200);
        p.add(frame);
        RootLayoutPanel rp = RootLayoutPanel.get();

        rp.add(p);
        rp.setWidgetTopHeight(p, 0, Style.Unit.PX, 100, Style.Unit.PCT);


        // Focus the cursor on the name field when the app loads
        nameField.setFocus(true);
        nameField.selectAll();

        nameField.addChangeHandler(new ChangeHandler() {

            @Override
            public void onChange(ChangeEvent event) {
                String url = "/apollo/annotator/search";
                RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, URL.encode(url));
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("query", new JSONString("pax6a"));
                builder.setRequestData("data=" + jsonObject.toString());
                builder.setHeader("Content-type", "application/x-www-form-urlencoded");
                RequestCallback requestCallback = new RequestCallback() {
                    @Override
                    public void onResponseReceived(Request request, Response response) {
                        JSONValue returnValue = JSONParser.parseStrict(response.getText());
                        JSONObject jsonObject = returnValue.isObject();
                        String queryString = jsonObject.get("query").isString().stringValue();

                        // TODO: use proper array parsing
                        String resultString = jsonObject.get("result").isString().stringValue();
                        resultString = resultString.replace("[", "");
                        resultString = resultString.replace("]", "");
                        searchResult.setText(" asdflkj asdflkjdas fsearch for " + queryString + " yields [" + resultString + "]");
                    }

                    @Override
                    public void onError(Request request, Throwable exception) {
                        Window.alert("ow");
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
        });
    }

    boolean toggleOpen = true ;

    private void toggleOpen() {
        if(p.getWidgetSize(navigationPanel)< 100){
            toggleOpen = false ;
        }

        if(toggleOpen){
            p.setWidgetSize(navigationPanel,20);
            dockOpenClose.setHTML("&laquo;");
        }
        else{
            p.setWidgetSize(navigationPanel,500);
            dockOpenClose.setHTML("&raquo;");
        }

        p.animate(400);

        toggleOpen  = !toggleOpen;
    }
}
