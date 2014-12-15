package org.bbop.apollo.gwt.client;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.*;

/**
 * Created by ndunn on 12/10/14.
 */
public class OrganismDetailPanel extends TabLayoutPanel{

//    final VerticalPanel namePanel = new VerticalPanel();

    final FlexTable detailsTable = new FlexTable();

//    final FlexTable attributesTable = new FlexTable();
//    final FlexTable goTable = new FlexTable();
//    final FlexTable commentTable = new FlexTable();

    public OrganismDetailPanel(){
        super(30,Style.Unit.PX);



        add(detailsTable, "Details");


        selectTab(0);

        detailsTable.setWidget(0,0,new HTML("Name"));
        detailsTable.setWidget(0,1,new HTML("Stickleback"));
        detailsTable.setWidget(1,0,new HTML("# Sequences"));
        detailsTable.setWidget(1,1,new HTML("12"));


        setWidth("100%");
        setHeight("100%");

    }

    private TreeItem generateTreeItem(String geneName) {
        TreeItem sox9b = new TreeItem();
        sox9b.setText(geneName);
        sox9b.addTextItem(geneName + "-001");
        sox9b.addTextItem(geneName + "-002");
        sox9b.addTextItem(geneName + "-003");
        sox9b.addTextItem(geneName + "-004");
        sox9b.addTextItem(geneName + "-005");
        sox9b.setState(true);
        return sox9b;
    }
}
