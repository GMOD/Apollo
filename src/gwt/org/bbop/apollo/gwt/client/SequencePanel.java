package org.bbop.apollo.gwt.client;


import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;

/**
 * Created by ndunn on 12/15/14.
 */
public class SequencePanel extends SplitLayoutPanel{

//    final HTML label = new HTML("Organism Panel");

    final Panel searchPanel = new VerticalPanel();
    final TextBox nameField = new TextBox();
//    final Label searchResult = new HTML("none");
    final CheckBox cdsCheckBox = new CheckBox();
    final CheckBox codonCheckBox = new CheckBox();
//    final VerticalPanel navigationPanelWrapper = new VerticalPanel();
//    final SplitLayoutPanel navigationPanel = new SplitLayoutPanel();
    final FeatureDetail2Panel featureDetailPanel = new FeatureDetail2Panel();
    final VerticalScrollPanel scrollPanel = new VerticalScrollPanel();


    // make a table
    final Tree tree = new Tree();


    public SequencePanel(){
//        super(Style.Unit.PX);
//        add(label);
        nameField.setWidth("100%");
        nameField.setEnabled(true);
        nameField.setReadOnly(false);
//        searchPanel.add(searchButton);
        searchPanel.add(nameField);

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
//        scrollPanel.setAlwaysShowScrollBars(true);
        scrollPanel.setAlwaysShowVerticalScrollBar(true);
        scrollPanel.add(tree);


        addSouth(featureDetailPanel,200);
        addNorth(searchPanel, 200);
        add(scrollPanel);

        setSize("100%","100%");

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
        sox9b.addTextItem(geneName+"-005");
        sox9b.setState(true);
        return sox9b;
    }


}
