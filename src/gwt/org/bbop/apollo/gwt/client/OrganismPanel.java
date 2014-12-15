package org.bbop.apollo.gwt.client;


import com.google.gwt.user.client.ui.*;

/**
 * Created by ndunn on 12/15/14.
 */
public class OrganismPanel extends SplitLayoutPanel {

    //    final HTML label = new HTML("Organism Panel");
    final ScrollPanel scrollPanel = new ScrollPanel();
    final FlexTable organismTable = new FlexTable();
    final OrganismDetailPanel organismDetailPanel = new OrganismDetailPanel();


    public OrganismPanel() {

        addSouth(organismDetailPanel,300);

        scrollPanel.add(organismTable);
        add(scrollPanel);


        organismTable.setHTML(0,0,"Name");
        organismTable.setHTML(0, 1, "# Sequences");
        organismTable.setHTML(0, 2, "Action");

        organismTable.setWidth("100%");

        for(int i = 1; i < 22 ; i++){
            generateRow(organismTable,i);
        }
    }


    private void generateRow(FlexTable sequenceTable, int i) {

        Anchor link = new Anchor("Group1."+i);
        sequenceTable.setWidget(i, 0, link);
        sequenceTable.setHTML(i, 1, Math.rint(Math.random() * 100) + "");
        Button button = new Button("Set");
//        Button button2 = new Button("Details");
        HorizontalPanel actionPanel = new HorizontalPanel();
        actionPanel.add(button);
//        actionPanel.add(button2);
        sequenceTable.setWidget(i, 2, actionPanel);
    }

}
