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
    final HorizontalPanel exonPanel = new HorizontalPanel();
    final ScrollPanel exonScrollPanel = new ScrollPanel();
    final FlexTable exonTable = new FlexTable();
    final FlexTable exonDetailTable = new FlexTable();


    public FeatureDetail2Panel(){
        super(30, Style.Unit.PX);

        namePanel.setWidth("100%");
        namePanel.setHeight("100%");

//        add(namePanel);

        exonScrollPanel.add(exonTable) ;
        exonPanel.add(exonScrollPanel);
        exonPanel.add(exonDetailTable);

        exonTable.setHTML(0,0,"Name");
        exonTable.setHTML(1,0,"sox9b-1231");
        exonTable.setHTML(2,0,"pax6a-1231312");
        exonTable.setWidth("100%");

        exonDetailTable.setHTML(0,0,"Start");
        exonDetailTable.setHTML(0,1,"Stop");
        exonDetailTable.setHTML(0,2,"Length");
        exonDetailTable.setHTML(1,0,"12");
        exonDetailTable.setHTML(1,1,"22");
        exonDetailTable.setHTML(1,2,"10");
        exonDetailTable.setHTML(2,0,"33");
        exonDetailTable.setHTML(2,1,"88");
        exonDetailTable.setHTML(2,2,"55");
        exonDetailTable.setWidth("100%");

        exonPanel.setWidth("100%");

        detailsTable.setWidth("100%");

        add(detailsTable,"Details");
        add(exonPanel, "Exon");
        add(dbXrefTable, "DbXref");
        add(pubmedTable, "PubMed");
        add(attributesTable, "Attributes");
        add(goTable, "GO Evidence");
        add(commentTable, "Comments");

        selectTab(0);

        detailsTable.setWidget(0,0,new HTML("Description"));
        detailsTable.setWidget(0,1,new HTML("laksdjf asldjf adslf "));
        detailsTable.setWidget(1,0,new HTML("Created"));
        detailsTable.setWidget(1,1,new HTML("2014-10-07"));
        detailsTable.setWidget(2,0,new HTML("Last Modified"));
        detailsTable.setWidget(2,1,new HTML("2014-10-09"));
//        detailsTable.setWidget(3,0,new HTML("Exons"));
//        detailsTable.setWidget(3,1,new HTML(""));
//        detailsTable.setWidth("100%");


        setWidth("100%");
        setHeight("100%");

    }
}
