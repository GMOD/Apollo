package org.bbop.apollo.gwt.client;


import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.*;

/**
 * Created by ndunn on 12/15/14.
 */
public class SequenceBrowserPanel extends SplitLayoutPanel {

    final Panel searchPanel = new VerticalPanel();
    final TextBox nameField = new TextBox();
    final ListBox organismListBox = new ListBox();
    final TextBox minFeatureBox = new TextBox();
    final TextBox maxFeatureBox = new TextBox();
    final SequenceDetailPanel sequenceDetailPanel = new SequenceDetailPanel();
    final VerticalScrollPanel scrollPanel = new VerticalScrollPanel();
    final FlexTable sequenceTable = new FlexTable();
    // make a table


    public SequenceBrowserPanel() {


        nameField.setWidth("100%");
        nameField.setEnabled(true);
        nameField.setReadOnly(false);
        Panel namePanel = new HorizontalPanel();
        namePanel.add(new HTML("Name"));
        namePanel.add(nameField);
        searchPanel.add(namePanel);

        Panel check1Panel = new HorizontalPanel();
        check1Panel.add(new HTML("Organism"));
        check1Panel.add(organismListBox);
        searchPanel.add(check1Panel);

        Panel check2Panel = new HorizontalPanel();
        check2Panel.add(new HTML("# Features"));
        check2Panel.add(minFeatureBox);
        check2Panel.add(new HTML(" - "));
        check2Panel.add(maxFeatureBox);
        searchPanel.add(check2Panel);




        addSouth(sequenceDetailPanel, 200);
        addNorth(searchPanel, 100);
        scrollPanel.add(sequenceTable);
        add(scrollPanel);

        setSize("100%", "100%");

        sequenceTable.setWidth("100%");

        sequenceTable.setHTML(0,0,"<b>Name</b>");
        sequenceTable.setHTML(0,1,"<b># Features</b>");
        sequenceTable.setHTML(0,2,"<b># Genes</b>");
        sequenceTable.setHTML(0,3,"<b>Action</b>");

        for(int i = 1; i < 22 ; i++){
            generateRow(sequenceTable,i);
        }


        sequenceTable.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                setWidgetSize(sequenceDetailPanel, 200);
                animate(400);
            }
        });


//        codonCheckBox.addClickHandler(new ClickHandler() {
//            @Override
//            public void onClick(ClickEvent event) {
//                setWidgetSize(sequenceDetailPanel, 0);
//                animate(400);
//            }
//        });

    }

    private void generateRow(FlexTable sequenceTable, int i) {

        Anchor link = new Anchor("Group1."+i);
        sequenceTable.setWidget(i, 0, link);
        sequenceTable.setHTML(i, 1, Math.rint(Math.random() * 100) + "");
        sequenceTable.setHTML(i, 2, Math.rint(Math.random() * 100) + "");
        Button button = new Button("Annotate");
        Button button2 = new Button("Details");
        HorizontalPanel actionPanel = new HorizontalPanel();
        actionPanel.add(button);
        actionPanel.add(button2);
        sequenceTable.setWidget(i, 3, actionPanel);
    }


}
