package org.bbop.apollo.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.*;
import org.bbop.apollo.gwt.client.demo.DataGenerator;

/**
 * Created by ndunn on 12/17/14.
 */
public class OrganismBrowserPanel extends Composite {
    interface OrganismBrowserPanelUiBinder extends UiBinder<Widget, OrganismBrowserPanel> {
    }

    private static OrganismBrowserPanelUiBinder ourUiBinder = GWT.create(OrganismBrowserPanelUiBinder.class);
    @UiField
    HTML organismName;
    @UiField
    HTML trackCount;
    @UiField
    HTML annotationCount;
    @UiField
    FlexTable organismTable;

    public OrganismBrowserPanel() {
        initWidget(ourUiBinder.createAndBindUi(this));

        organismName.setHTML("Danio rerio");
        trackCount.setHTML("30");
        annotationCount.setHTML("1223");

        DataGenerator.populateOrganismTable(organismTable);

    }
}