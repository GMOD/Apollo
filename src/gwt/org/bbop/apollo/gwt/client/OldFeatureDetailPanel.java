package org.bbop.apollo.gwt.client;

import com.google.gwt.user.client.ui.*;

/**
 * Created by ndunn on 12/10/14.
 * @deprecated
 */
public class OldFeatureDetailPanel extends HorizontalPanel{

    final FlexTable geneTable = new FlexTable();
//    final HTML geneHTML = new HTML("Gene");
//    final HTML geneValue = new HTML("pax6a");
    final TabPanel tabPanel = new TabPanel();
    final StackPanel transcriptPanel = new StackPanel();
    final VerticalPanel namePanel = new VerticalPanel();

    final FlexTable detailsTable = new FlexTable();
    final FlexTable dbXrefTable = new FlexTable();
    final FlexTable pubmedTable = new FlexTable();
    final FlexTable attributesTable = new FlexTable();
    final FlexTable goTable = new FlexTable();
    final FlexTable commentTable = new FlexTable();

    public OldFeatureDetailPanel(){

        geneTable.setHTML(0,0,"Name&nbsp;");
        geneTable.setWidget(0, 1, new HTML("paired box 6a"));
        geneTable.setHTML(1,0,"Symbol&nbsp;");
        geneTable.setWidget(1, 1, new HTML("pax6a"));
        geneTable.setHTML(2,0,"Type&nbsp;");
        geneTable.setWidget(2, 1, new HTML("gene"));
//        geneTable.setWidth("30%");

        FlexTable pax6a001 = new FlexTable();
        pax6a001.setHTML(0,0,"Name&nbsp;");
        pax6a001.setWidget(0, 1, new HTML("paired box 6a-001"));
        pax6a001.setHTML(1,0,"Symbol&nbsp;");
        pax6a001.setWidget(1, 1, new HTML("pax6a-001"));
        pax6a001.setHTML(2,0,"Type&nbsp;");
        pax6a001.setWidget(2, 1, new HTML("mRNA"));

        FlexTable pax6a002 = new FlexTable();
        pax6a002.setHTML(0,0,"Name&nbsp;");
        pax6a002.setWidget(0, 1, new HTML("paired box 6a-002"));
        pax6a002.setHTML(1,0,"Symbol&nbsp;");
        pax6a002.setWidget(1, 1, new HTML("pax6a-002"));
        pax6a002.setHTML(2,0,"Type&nbsp;");
        pax6a002.setWidget(2, 1, new HTML("mRNA"));

        FlexTable pax6a006 = new FlexTable();
        pax6a006.setHTML(0,0,"Name&nbsp;");
        pax6a006.setWidget(0, 1, new HTML("paired box 6a-006"));
        pax6a006.setHTML(1,0,"Symbol&nbsp;");
        pax6a006.setWidget(1, 1, new HTML("pax6a-006"));
        pax6a006.setHTML(2,0,"Type&nbsp;");
        pax6a006.setWidget(2, 1, new HTML("mRNA"));


        transcriptPanel.add(pax6a001, "pax6a-001");
        transcriptPanel.add(pax6a002, "pax6a-002");
        transcriptPanel.add(pax6a006, "pax6a-006");
        transcriptPanel.setWidth("100%");

        namePanel.add(geneTable);
        namePanel.add(transcriptPanel);

        namePanel.setWidth("100%");
        namePanel.setHeight("100%");

        add(namePanel);


        tabPanel.add(detailsTable,"Details");
        tabPanel.add(dbXrefTable,"DbXref");
        tabPanel.add(pubmedTable, "PubMed");
        tabPanel.add(attributesTable,"Attributes");
        tabPanel.add(goTable,"GO Evidence");
        tabPanel.add(commentTable,"Comments");

        tabPanel.selectTab(0);
        add(tabPanel);

        detailsTable.setWidget(0,0,new HTML("Description"));
        detailsTable.setWidget(0,1,new HTML("laksdjf asldjf adslf "));
        detailsTable.setWidget(1,0,new HTML("Created"));
        detailsTable.setWidget(1,1,new HTML("2014-10-07"));
        detailsTable.setWidget(2,0,new HTML("Last Modified"));
        detailsTable.setWidget(2,1,new HTML("2014-10-09"));


//        tabLayoutPanel.setHeight("100%");
//        detailsTable.setHeight("100%");

//        geneTable.setHeight("100%");
        geneTable.setWidth("100%");

        setWidth("100%");
//        setHeight("100%");

    }
}
