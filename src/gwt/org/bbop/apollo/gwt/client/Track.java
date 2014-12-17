package org.bbop.apollo.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiTemplate;
import com.google.gwt.user.client.ui.*;
//import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Label;
import org.bbop.apollo.gwt.client.demo.DataGenerator;
import org.gwtbootstrap3.client.ui.*;
import org.gwtbootstrap3.client.ui.CheckBox;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.TextBox;

/**
 * Created by ndunn on 12/16/14.
 */
public class Track extends Composite{
    interface TrackUiBinder extends UiBinder<Widget, Track> {
    }

    private static TrackUiBinder ourUiBinder = GWT.create(TrackUiBinder.class);

    @UiField FlexTable configurationTable;
    @UiField FlexTable trackTable;
    @UiField ListBox organismList;
    @UiField TextBox nameSearchBox;
    @UiField HTML trackName;
    @UiField HTML trackType;
    @UiField HTML trackCount;
    @UiField HTML trackDensity;

    public Track() {

        Widget rootElement = ourUiBinder.createAndBindUi(this);
        initWidget(rootElement);

        configurationTable.setHTML(0, 0, "maxHeight");;
        configurationTable.setHTML(0, 1, "1000");;
        configurationTable.setHTML(1, 0, "maxFeatureScreenDensity");;
        configurationTable.setHTML(1, 1, "0.5");;
        configurationTable.setHTML(2, 0, "maxDescriptionLength");;
        configurationTable.setHTML(2, 1, "70");;
        configurationTable.setHTML(3, 0, "label");;
        configurationTable.setHTML(3, 1, "Cflo_OGSv3.3");;

        configurationTable.setWidth("100%");

        trackTable.setText(0, 0, "Show");
        trackTable.setHTML(0,1,"Name");
        trackTable.setHTML(0, 2, "Type");

        for(int i = 1 ; i < 20 ; i++){
            trackTable.setWidget(i,0, new CheckBox());
            Hyperlink html = new Hyperlink();
            html.setText("Track"+i);
            trackTable.setWidget(i,1, html);
            HTML typeHTML = new HTML("CanvasFeature");
            trackTable.setWidget(i,2, typeHTML);
        }



        DataGenerator.populateOrganismList(organismList);

        trackName.setHTML("Track3");
        trackType.setHTML("CanvasFeature");
        trackCount.setHTML("34");
        trackDensity.setHTML("0.000123");

    }

    private IsWidget generateCheckBox(int i) {
        HorizontalPanel panel = new HorizontalPanel();
        CheckBox checkBox = new CheckBox();
        HTML html = new HTML("Track"+i);
        panel.add(checkBox);
        panel.add(html);
        return panel;
    }

}