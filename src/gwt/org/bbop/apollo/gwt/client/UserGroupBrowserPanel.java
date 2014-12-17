package org.bbop.apollo.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Created by ndunn on 12/17/14.
 */
public class UserGroupBrowserPanel extends Composite {
    interface UserGroupBrowserPanelUiBinder extends UiBinder<Widget, UserGroupBrowserPanel> {
    }

    private static UserGroupBrowserPanelUiBinder ourUiBinder = GWT.create(UserGroupBrowserPanelUiBinder.class);

    public UserGroupBrowserPanel() {
        initWidget(ourUiBinder.createAndBindUi(this));
    }
}