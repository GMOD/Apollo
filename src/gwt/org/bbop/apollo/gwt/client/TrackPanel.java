package org.bbop.apollo.gwt.client;


import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.ui.*;

/**
 * Created by ndunn on 12/15/14.
 */
public class TrackPanel extends SplitLayoutPanel {

    final Panel searchPanel = new VerticalPanel();
    final TextBox nameField = new TextBox();
    final TrackDetailPanel trackDetailPanel = new TrackDetailPanel();
    final VerticalScrollPanel scrollPanel = new VerticalScrollPanel();
    final ListBox organismListBox = new ListBox();


    // make a table
    final FlexTable trackTable = new FlexTable();


    public TrackPanel() {
        nameField.setWidth("100%");
        nameField.setEnabled(true);
        nameField.setReadOnly(false);
        searchPanel.add(nameField);

        HorizontalPanel sequencePanel = new HorizontalPanel();
        sequencePanel.add(new HTML("Organism"));
        sequencePanel.add(organismListBox);
        searchPanel.add(sequencePanel);



        for(int i = 0 ; i < 20 ; i++){
            trackTable.setWidget(i,0,generateCheckBox(i));
        }

        trackTable.setWidth("100%");
//        trackTable.addItem(generateTreeItem("abc"));
//        trackTable.addItem(generateTreeItem("def"));
//        trackTable.addItem(generateTreeItem("ghu"));
//        trackTable.addItem(generateTreeItem("lmn"));
//        trackTable.addItem(generateTreeItem("lmn"));
//        trackTable.addItem(generateTreeItem("lmn"));
//        scrollPanel.setAlwaysShowVerticalScrollBar(true);
        scrollPanel.add(trackTable);


        addSouth(trackDetailPanel, 200);
        addNorth(searchPanel, 100);
        add(scrollPanel);

        setSize("100%", "100%");

//        trackTable.addSelectionHandler(new SelectionHandler<TreeItem>() {
//            @Override
//            public void onSelection(SelectionEvent<TreeItem> event) {
//                setWidgetSize(trackDetailPanel, 200);
//                animate(400);
//            }
//        });



    }

    private IsWidget generateCheckBox(int i) {
        HorizontalPanel panel = new HorizontalPanel();
        CheckBox checkBox = new CheckBox();
        HTML html = new HTML("Track"+i);
        panel.add(checkBox);
        panel.add(html);
        return panel;
    }

    HTML createHTML(String geneName,int index){
        HTML html = new HTML(geneName + "-00"+index+ " <div class='pull-right badge'>12</div>");

        return html;
    }

    private TreeItem generateTreeItem(String geneName) {
        TreeItem sox9b = new TreeItem();
        int i =0  ;
//        sox9b.setText(geneName);
        sox9b.addItem(new HTML(geneName+"<div class='pull-right label label-warning'>32</div>"));
        sox9b.addItem(createHTML(geneName, i++));
        sox9b.addItem(createHTML(geneName, i++));
        sox9b.addItem(createHTML(geneName, i++));
        sox9b.addItem(createHTML(geneName, i++));
        sox9b.addItem(createHTML(geneName, i++));
        sox9b.setState(true);
        return sox9b;
    }


}
