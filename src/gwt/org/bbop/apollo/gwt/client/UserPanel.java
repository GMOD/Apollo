package org.bbop.apollo.gwt.client;


import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.*;

/**
 * Created by ndunn on 12/15/14.
 */
public class UserPanel extends SplitLayoutPanel{

//    final HTML label = new HTML("User Panel");

    final Panel searchPanel = new VerticalPanel();
    final TextBox nameField = new TextBox();
    final UserDetailPanel sequenceDetailPanel = new UserDetailPanel();
    final VerticalScrollPanel scrollPanel = new VerticalScrollPanel();
    final FlexTable userTable = new FlexTable();
    // make a table


    public UserPanel(){

        nameField.setWidth("100%");
        nameField.setEnabled(true);
        nameField.setReadOnly(false);
        Panel namePanel = new HorizontalPanel();
        namePanel.add(new HTML("Name"));
        namePanel.add(nameField);
        searchPanel.add(namePanel);


        addSouth(sequenceDetailPanel, 200);
        addNorth(searchPanel, 100);
        scrollPanel.add(userTable);
        add(scrollPanel);

        setSize("100%", "100%");

        userTable.setWidth("100%");

        userTable.setHTML(0, 0, "<b>Name</b>");
        userTable.setHTML(0, 1, "<b>Email</b>");
        userTable.setHTML(0, 2, "<b>Sequences</b>");
        userTable.setHTML(0, 3, "<b>Groups</b>");

        for(int i = 1; i < 22 ; i++){
            generateRow(userTable,i);
        }


        userTable.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                setWidgetSize(sequenceDetailPanel, 200);
                animate(400);
            }
        });


//        add(label);
    }

    private void generateRow(FlexTable sequenceTable, int i) {

        Anchor link = new Anchor("Bob Jones"+i);
        sequenceTable.setWidget(i, 0, link);
        Anchor emailLink = new Anchor("bob@jones.gov");
        sequenceTable.setWidget(i, 1, emailLink);
        sequenceTable.setHTML(i, 2, Math.rint(Math.random() * 100) + "");
//        Button button = new Button("Annotate");
//        Button button2 = new Button("Details");
        HorizontalPanel actionPanel = new HorizontalPanel();



        for(int j =0 ; j < Math.rint(Math.random() * 10); j++){
            if(j< 10){
                actionPanel.add(new HTML("<div class='label label-danger'>Group"+j+"</div>"));
            }

        }


//        actionPanel.add(button);
//        actionPanel.add(button2);
        sequenceTable.setWidget(i, 3, actionPanel);
    }

}
