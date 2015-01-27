package org.bbop.apollo.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Created by ndunn on 1/27/15.
 */
public class ExportPanel extends DialogBox{
    interface ExportPanelUiBinder extends UiBinder<Widget, ExportPanel> {
    }

    private static ExportPanelUiBinder ourUiBinder = GWT.create(ExportPanelUiBinder.class);

    public ExportPanel() {
        setWidget(ourUiBinder.createAndBindUi(this));
        setAutoHideEnabled(true);
        setText("Export");
        setGlassEnabled(true);
        center();
//        initWidget(ourUiBinder.createAndBindUi(this));
    }
}