package org.bbop.apollo.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Created by ndunn on 1/11/15.
 */
public class PreferencePanel extends Composite {
    interface PreferencePanelUiBinder extends UiBinder<Widget, PreferencePanel> {
    }

    private static PreferencePanelUiBinder ourUiBinder = GWT.create(PreferencePanelUiBinder.class);

    public PreferencePanel() {
        initWidget(ourUiBinder.createAndBindUi(this));
    }
}