package org.bbop.apollo.gwt.client;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.*;

/**
 * Created by ndunn on 12/10/14.
 */
public class TrackDetailPanel extends TabLayoutPanel{

    final FlexTable detailsTable = new FlexTable();
    final FlexTable configurationTable = new FlexTable();
    final ScrollPanel scrollPanel = new ScrollPanel();

    public TrackDetailPanel(){
        super(30,Style.Unit.PX);

        configurationTable.setHTML(0, 0, "maxHeight");;
        configurationTable.setHTML(0, 1, "1000");;
        configurationTable.setHTML(1, 0, "maxFeatureScreenDensity");;
        configurationTable.setHTML(1, 1, "0.5");;
        configurationTable.setHTML(2, 0, "maxDescriptionLength");;
        configurationTable.setHTML(2, 1, "70");;
        configurationTable.setHTML(3, 0, "label");;
        configurationTable.setHTML(3, 1, "Cflo_OGSv3.3");;

        configurationTable.setWidth("100%");


        add(detailsTable, "Details");
//        add(permissionsTable, "Groups");
        scrollPanel.add(configurationTable);
        add(scrollPanel, "Configuration");



        selectTab(0);

        detailsTable.setWidget(0,0,new HTML("Name"));

//        detailsTable.setWidget(0,1,new HTML("Group1.3"));
        detailsTable.setWidget(0,1,new HTML("Track3"));
        detailsTable.setWidget(1,0,new HTML("Track type"));
        detailsTable.setWidget(1,1,new HTML("HTMLFeature"));
        detailsTable.setWidget(2,0,new HTML("Feature Count"));
        detailsTable.setWidget(2,1,new HTML("33"));
        detailsTable.setWidget(3,0,new HTML("Feature Density"));
        detailsTable.setWidget(3,1,new HTML("0.0101"));
        detailsTable.setWidth("100%");


        setWidth("100%");
        setHeight("100%");

    }

    private void generateRow(FlexTable sequenceTable, int i) {

        Anchor link = new Anchor("Group1."+i);
        sequenceTable.setWidget(i, 0, link);
        sequenceTable.setHTML(i, 1, Math.random() > 0.5 ? "X" : "");
        sequenceTable.setHTML(i, 2, Math.random() > 0.5 ? "X" : "");
        sequenceTable.setHTML(i, 3, Math.random() > 0.5 ? "X" : "");
        sequenceTable.setHTML(i, 4, Math.random() > 0.5 ? "X" : "");
    }
}
