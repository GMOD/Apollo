package org.bbop.apollo.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.*;
import org.bbop.apollo.gwt.client.demo.DataGenerator;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.TextBox;

/**
 * Created by ndunn on 12/17/14.
 */
public class SequencePanel extends Composite {
    interface SequencePanelUiBinder extends UiBinder<Widget, SequencePanel> {
    }

    private static SequencePanelUiBinder ourUiBinder = GWT.create(SequencePanelUiBinder.class);
    @UiField
    TextBox minFeatureLength;
    @UiField
    TextBox maxFeatureLength;
    @UiField
    ListBox organismList;
    @UiField
    FlexTable sequenceTable;
    @UiField
    HTML sequenceName;
    @UiField
    HTML sequenceFile;
    @UiField
    HTML sequenceStart;
    @UiField
    HTML sequenceStop;

    public SequencePanel() {
        initWidget(ourUiBinder.createAndBindUi(this));

        for(int i = 1; i < 22 ; i++){
            DataGenerator.generateSequenceRow(sequenceTable, i);
        }

        sequenceTable.setWidth("100%");

        sequenceName.setHTML("Group1.1");
        sequenceFile.setHTML("/data/apollo/annotations1/");
        sequenceStart.setHTML("100");
        sequenceStop.setHTML("4234");

        DataGenerator.populateOrganismList(organismList);
    }
}