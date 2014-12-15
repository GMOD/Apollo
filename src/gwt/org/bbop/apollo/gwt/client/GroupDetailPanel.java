package org.bbop.apollo.gwt.client;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.*;

/**
 * Created by ndunn on 12/10/14.
 */
public class GroupDetailPanel extends TabLayoutPanel{

//    final VerticalPanel namePanel = new VerticalPanel();

    final FlexTable detailsTable = new FlexTable();
    final FlexTable userTable= new FlexTable();
    final FlexTable sequenceTable = new FlexTable();
    final ScrollPanel scrollPanel = new ScrollPanel();
    final ScrollPanel scrollPanel2 = new ScrollPanel();
//    final FlexTable organismTable = new FlexTable();
//    final Tree tree = new Tree();

//    final FlexTable attributesTable = new FlexTable();
//    final FlexTable goTable = new FlexTable();
//    final FlexTable commentTable = new FlexTable();

    public GroupDetailPanel(){
        super(30,Style.Unit.PX);

        sequenceTable.setHTML(0,0,"Name");;
        sequenceTable.setHTML(0,1,"Admin");;
        sequenceTable.setHTML(0,2,"Write");;
        sequenceTable.setHTML(0,3,"Export");;
        sequenceTable.setHTML(0,4,"Read");;

        for(int i = 1 ; i < 10 ; i++){
            generateSequenceRow(sequenceTable, i);
        }

        userTable.setHTML(0,0,"Name");;
        userTable.setHTML(0,1,"Email");;
        userTable.setHTML(0,2,"Action");;

        for(int i = 1 ; i < 10 ; i++){
            generateUserRow(userTable, i);
        }



        add(detailsTable, "Details");
//        add(permissionsTable, "Groups");
        scrollPanel.add(sequenceTable);
        add(scrollPanel, "Sequences");
        scrollPanel2.add(userTable);
        add(scrollPanel2, "Users");


        sequenceTable.setWidth("100%");
        userTable.setWidth("100%");

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

    private void generateSequenceRow(FlexTable sequenceTable, int i) {

        Anchor link = new Anchor("Group1."+i);
        sequenceTable.setWidget(i, 0, link);
        sequenceTable.setHTML(i, 1, Math.random() > 0.5 ? "X" : "");
        sequenceTable.setHTML(i, 2, Math.random() > 0.5 ? "X" : "");
        sequenceTable.setHTML(i, 3, Math.random() > 0.5 ? "X" : "");
        sequenceTable.setHTML(i, 4, Math.random() > 0.5 ? "X" : "");
    }

    private void generateUserRow(FlexTable sequenceTable, int i) {

        Anchor link = new Anchor("Bob Jones"+i);
        sequenceTable.setWidget(i, 0, link);
        Anchor emailLink= new Anchor("bob@jones.gov");
        sequenceTable.setWidget(i, 1, emailLink);

        Button removeButton = new Button("X");
        sequenceTable.setWidget(i, 2, removeButton);

    }
}
