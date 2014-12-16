package org.bbop.apollo.gwt.client;


import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.ui.*;

/**
 * Created by ndunn on 12/15/14.
 */
public class FeaturePanel extends SplitLayoutPanel {

    final Panel searchPanel = new VerticalPanel();
    final TextBox nameField = new TextBox();
    final CheckBox cdsCheckBox = new CheckBox();
    final CheckBox codonCheckBox = new CheckBox();
    final FeatureDetail2Panel featureDetailPanel = new FeatureDetail2Panel();
    final VerticalScrollPanel scrollPanel = new VerticalScrollPanel();
    final ListBox sequenceListBox = new ListBox();


    // make a table
    final Tree tree = new Tree();


    public FeaturePanel() {
        nameField.setWidth("100%");
        nameField.setEnabled(true);
        nameField.setReadOnly(false);
        searchPanel.add(nameField);

        HorizontalPanel sequencePanel = new HorizontalPanel();
        sequencePanel.add(new HTML("Sequence"));
        sequencePanel.add(sequenceListBox);
        searchPanel.add(sequencePanel);

        Panel check1Panel = new HorizontalPanel();
        check1Panel.add(cdsCheckBox);
        check1Panel.add(new HTML("&nbsp;Check CDS"));
        cdsCheckBox.setValue(true);
        searchPanel.add(check1Panel);

        Panel check2Panel = new HorizontalPanel();
        check2Panel.add(codonCheckBox);
        codonCheckBox.setValue(true);
        check2Panel.add(new HTML("&nbsp;Check Codons"));
        searchPanel.add(check2Panel);


        tree.addItem(generateTreeItem("abc"));
        tree.addItem(generateTreeItem("def"));
        tree.addItem(generateTreeItem("ghu"));
        tree.addItem(generateTreeItem("lmn"));
        tree.addItem(generateTreeItem("lmn"));
        tree.addItem(generateTreeItem("lmn"));
        scrollPanel.setAlwaysShowVerticalScrollBar(true);
        scrollPanel.add(tree);


        addSouth(featureDetailPanel, 200);
        addNorth(searchPanel, 200);
        add(scrollPanel);

        setSize("100%", "100%");

        tree.addSelectionHandler(new SelectionHandler<TreeItem>() {
            @Override
            public void onSelection(SelectionEvent<TreeItem> event) {
                setWidgetSize(featureDetailPanel, 200);
                animate(400);
            }
        });


        codonCheckBox.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                setWidgetSize(featureDetailPanel, 0);
                animate(400);
            }
        });

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
