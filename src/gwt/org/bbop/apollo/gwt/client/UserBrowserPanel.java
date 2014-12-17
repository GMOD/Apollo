package org.bbop.apollo.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Created by ndunn on 12/17/14.
 */
public class UserBrowserPanel extends Composite {
    interface UserBrowserPanelUiBinder extends UiBinder<Widget, UserBrowserPanel> {
    }

    private static UserBrowserPanelUiBinder ourUiBinder = GWT.create(UserBrowserPanelUiBinder.class);

    public UserBrowserPanel() {
        initWidget(ourUiBinder.createAndBindUi(this));
    }
}