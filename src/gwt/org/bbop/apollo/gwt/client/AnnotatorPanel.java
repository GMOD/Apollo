package org.bbop.apollo.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import org.bbop.apollo.gwt.client.demo.DataGenerator;
import org.gwtbootstrap3.client.ui.CheckBox;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.TextBox;

/**
 * Created by ndunn on 12/17/14.
 */
public class AnnotatorPanel extends Composite {
    interface AnnotatorPanelUiBinder extends UiBinder<com.google.gwt.user.client.ui.Widget, AnnotatorPanel> {
    }

    private static AnnotatorPanelUiBinder ourUiBinder = GWT.create(AnnotatorPanelUiBinder.class);
    @UiField
    TextBox nameSearchBox;
    @UiField
    ListBox sequenceList;
    @UiField
    CheckBox cdsFilter;
    @UiField
    CheckBox stopCodonFilter;
    @UiField
    Tree features;

    @UiField HTML annotationName;
    @UiField HTML annotationDescription;

    TreeItem selectedItem ;

    public AnnotatorPanel() {
//        initWidget(ourUiBinder.createAndBindUi(this));
        Widget rootElement = ourUiBinder.createAndBindUi(this);
        initWidget(rootElement);

        stopCodonFilter.setValue(true);

        features.addItem(DataGenerator.generateTreeItem("sox9a"));
        features.addItem(DataGenerator.generateTreeItem("sox9b"));
        features.addItem(DataGenerator.generateTreeItem("pax6a"));
        features.addItem(DataGenerator.generateTreeItem("pax6b"));

        features.addSelectionHandler(new SelectionHandler<TreeItem>() {
            @Override
            public void onSelection(SelectionEvent<TreeItem> event) {
                selectedItem.removeStyleName("selectedTreeItem");
                selectedItem = event.getSelectedItem();
                selectedItem.addStyleName("selectedTreeItem");
            }
        });
        annotationName.setText("sox9a");
        annotationDescription.setText("SRY (sex determining region Y)-box 9a");

        DataGenerator.populateSequenceList(sequenceList);
    }
}