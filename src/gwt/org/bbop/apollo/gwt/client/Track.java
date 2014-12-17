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

//    @UiTemplate("Track.ui.xml")
    private static TrackUiBinder ourUiBinder = GWT.create(TrackUiBinder.class);

    @UiField FlexTable trackDetailTable;
    @UiField FlexTable configurationTable;
    @UiField FlexTable trackTable;
    @UiField ListBox organismList;
    @UiField TextBox nameSearchBox;

    public Track() {
//        DivElement rootElement = ourUiBinder.createAndBindUi(this);
//        setElement(rootElement);

        Widget rootElement = ourUiBinder.createAndBindUi(this);
        initWidget(rootElement);

        trackDetailTable.setWidget(0,0,new HTML("Name"));
//        trackDetailTable.setWidget(0,1,new HTML("Group1.3"));
        trackDetailTable.setWidget(0,1,new HTML("Track3"));
        trackDetailTable.setWidget(1,0,new HTML("Track type"));
        trackDetailTable.setWidget(1,1,new HTML("HTMLFeature"));
        trackDetailTable.setWidget(2,0,new HTML("Feature Count"));
        trackDetailTable.setWidget(2, 1, new HTML("33"));
        trackDetailTable.setWidget(3, 0, new HTML("Feature Density"));
        trackDetailTable.setWidget(3, 1, new HTML("0.0101"));
        trackDetailTable.setWidth("100%");

        configurationTable.setHTML(0, 0, "maxHeight");;
        configurationTable.setHTML(0, 1, "1000");;
        configurationTable.setHTML(1, 0, "maxFeatureScreenDensity");;
        configurationTable.setHTML(1, 1, "0.5");;
        configurationTable.setHTML(2, 0, "maxDescriptionLength");;
        configurationTable.setHTML(2, 1, "70");;
        configurationTable.setHTML(3, 0, "label");;
        configurationTable.setHTML(3, 1, "Cflo_OGSv3.3");;

        configurationTable.setWidth("100%");

        for(int i = 0 ; i < 20 ; i++){
            trackTable.setWidget(i,0, new CheckBox());
            HTML html = new HTML("Track"+i);
            trackTable.setWidget(i,1, html);
        }

        DataGenerator.populateOrganismList(organismList);


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