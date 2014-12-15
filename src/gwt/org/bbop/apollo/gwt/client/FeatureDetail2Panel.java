package org.bbop.apollo.gwt.client;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.*;

/**
 * Created by ndunn on 12/10/14.
 */
public class FeatureDetail2Panel extends TabLayoutPanel{

    final VerticalPanel namePanel = new VerticalPanel();

    final FlexTable detailsTable = new FlexTable();
    final FlexTable dbXrefTable = new FlexTable();
    final FlexTable pubmedTable = new FlexTable();
    final FlexTable attributesTable = new FlexTable();
    final FlexTable goTable = new FlexTable();
    final FlexTable commentTable = new FlexTable();

    public FeatureDetail2Panel(){
        super(30, Style.Unit.PX);

        namePanel.setWidth("100%");
        namePanel.setHeight("100%");

//        add(namePanel);


        add(detailsTable,"Details");
        add(dbXrefTable,"DbXref");
        add(pubmedTable, "PubMed");
        add(attributesTable,"Attributes");
        add(goTable,"GO Evidence");
        add(commentTable,"Comments");

        selectTab(0);

        detailsTable.setWidget(0,0,new HTML("Description"));
        detailsTable.setWidget(0,1,new HTML("laksdjf asldjf adslf "));
        detailsTable.setWidget(1,0,new HTML("Created"));
        detailsTable.setWidget(1,1,new HTML("2014-10-07"));
        detailsTable.setWidget(2,0,new HTML("Last Modified"));
        detailsTable.setWidget(2,1,new HTML("2014-10-09"));


        setWidth("100%");
        setHeight("100%");

    }
}
