package org.bbop.apollo.gwt.client;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.*;

/**
 * Created by ndunn on 12/10/14.
 */
public class UserDetailPanel extends TabLayoutPanel{

//    final VerticalPanel namePanel = new VerticalPanel();

    final FlexTable detailsTable = new FlexTable();
//    final FlexTable permissionsTable = new FlexTable();
    final FlexTable sequenceTable = new FlexTable();
    final ScrollPanel scrollPanel = new ScrollPanel();
//    final FlexTable organismTable = new FlexTable();
//    final Tree tree = new Tree();

//    final FlexTable attributesTable = new FlexTable();
//    final FlexTable goTable = new FlexTable();
//    final FlexTable commentTable = new FlexTable();

    public UserDetailPanel(){
        super(30,Style.Unit.PX);

//        tree.addItem(generateTreeItem("abc"));
//        tree.addItem(generateTreeItem("def"));
//        tree.addItem(generateTreeItem("ghu"));
//        tree.addItem(generateTreeItem("lmn"));
//        tree.addItem(generateTreeItem("lmn"));
//        tree.addItem(generateTreeItem("lmn"));

        sequenceTable.setHTML(0,0,"Name");;
        sequenceTable.setHTML(0,1,"Admin");;
        sequenceTable.setHTML(0,2,"Write");;
        sequenceTable.setHTML(0,3,"Export");;
        sequenceTable.setHTML(0,4,"Read");;

        for(int i = 1 ; i < 10 ; i++){
            generateRow(sequenceTable,i);
        }

        sequenceTable.setWidth("100%");


        add(detailsTable, "Details");
//        add(permissionsTable, "Groups");
        scrollPanel.add(sequenceTable);
        add(scrollPanel, "Sequences");



        selectTab(0);

        detailsTable.setWidget(0,0,new HTML("UserGroups"));

        HorizontalPanel horizontalPanel = new HorizontalPanel();

        for(int j =0 ; j < Math.rint(Math.random() * 10); j++){
            if(j< 10){
                horizontalPanel.add(new HTML("<div class='label label-danger'>Group"+j+"</div>"));
            }

        }

//        detailsTable.setWidget(0,1,new HTML("Group1.3"));
        detailsTable.setWidget(0,1,horizontalPanel);
        detailsTable.setWidget(1,0,new HTML("Refseq file"));
        detailsTable.setWidget(1,1,new HTML("/tmp"));
        detailsTable.setWidget(2,0,new HTML("Start/Stop"));
        detailsTable.setWidget(2,1,new HTML("3/123"));


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
