package org.bbop.apollo.gwt.client;


import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.*;

/**
 * Created by ndunn on 12/15/14.
 */
public class GroupPanel extends SplitLayoutPanel{

//    final HTML label = new HTML("User Panel");
    final Panel searchPanel = new VerticalPanel();
    final TextBox nameField = new TextBox();
    final GroupDetailPanel groupDetailPanel = new GroupDetailPanel();
    final VerticalScrollPanel scrollPanel = new VerticalScrollPanel();
    final FlexTable userTable = new FlexTable();


    public GroupPanel(){
        nameField.setWidth("100%");
        nameField.setEnabled(true);
        nameField.setReadOnly(false);
        Panel namePanel = new HorizontalPanel();
        namePanel.add(new HTML("Name"));
        namePanel.add(nameField);
        searchPanel.add(namePanel);


        addSouth(groupDetailPanel, 200);
        addNorth(searchPanel, 100);
        scrollPanel.add(userTable);
        add(scrollPanel);

        setSize("100%", "100%");

        userTable.setWidth("100%");

        userTable.setHTML(0, 0, "<b>Name</b>");
        userTable.setHTML(0, 1, "<b>Num&nbsp;Users</b>");
        userTable.setHTML(0, 2, "<b>Num&nbsp;Sequences</b>");

        for(int i = 1; i < 22 ; i++){
            generateRow(userTable,i);
        }


        userTable.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                setWidgetSize(groupDetailPanel, 200);
                animate(400);
            }
        });

    }


    private void generateRow(FlexTable sequenceTable, int i) {

        Anchor link = new Anchor("UserGroup"+i);
        sequenceTable.setWidget(i, 0, link);
        sequenceTable.setHTML(i, 1, Math.rint(Math.random() * 100) + "");
        sequenceTable.setHTML(i, 2, Math.rint(Math.random() * 100) + "");

    }
}
