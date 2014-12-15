package org.bbop.apollo.gwt.client;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.*;

/**
 * Created by ndunn on 12/10/14.
 */
public class SequenceDetailPanel extends TabLayoutPanel{

//    final VerticalPanel namePanel = new VerticalPanel();

    final FlexTable detailsTable = new FlexTable();
    final FlexTable permissionsTable = new FlexTable();
    final ScrollPanel scrollPanel = new ScrollPanel();
//    final FlexTable sequenceTable = new FlexTable();
    final Tree tree = new Tree();

//    final FlexTable attributesTable = new FlexTable();
//    final FlexTable goTable = new FlexTable();
//    final FlexTable commentTable = new FlexTable();

    public SequenceDetailPanel(){
        super(30,Style.Unit.PX);

        tree.addItem(generateTreeItem("abc"));
        tree.addItem(generateTreeItem("def"));
        tree.addItem(generateTreeItem("ghu"));
        tree.addItem(generateTreeItem("lmn"));
        tree.addItem(generateTreeItem("lmn"));
        tree.addItem(generateTreeItem("lmn"));


        add(detailsTable, "Details");
        add(permissionsTable, "Permissions");
        scrollPanel.add(tree);
        add(scrollPanel, "Features");



        selectTab(0);

        detailsTable.setWidget(0,0,new HTML("Name"));
        detailsTable.setWidget(0,1,new HTML("Group1.3"));
        detailsTable.setWidget(1,0,new HTML("Refseq file"));
        detailsTable.setWidget(1,1,new HTML("/tmp"));
        detailsTable.setWidget(2,0,new HTML("Start/Stop"));
        detailsTable.setWidget(2,1,new HTML("3/123"));


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
