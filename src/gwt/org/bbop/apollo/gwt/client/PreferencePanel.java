package org.bbop.apollo.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.http.client.*;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;

/**
 * Created by ndunn on 1/11/15.
 */
public class PreferencePanel extends Composite {
    interface PreferencePanelUiBinder extends UiBinder<Widget, PreferencePanel> {
    }

    private static PreferencePanelUiBinder ourUiBinder = GWT.create(PreferencePanelUiBinder.class);
    @UiField
    HTML adminPanel;
    @UiField
    Button updateCommonDirectoryButton;

    @UiHandler("updateCommonDirectoryButton")
    public void updateCommonDirectoryButton(ClickEvent clickEvent) {
        MainPanel.getInstance().updateCommonDir(
                "If you update this path, please move pertinent files: " + MainPanel.getInstance().getCommonDataDirectory()
                , MainPanel.getInstance().getCommonDataDirectory());
    }

    public void reload() {
        String url = "annotator/adminPanel";
        String rootUrl = Annotator.getRootUrl();
        if (!url.startsWith(rootUrl)) {
            url = rootUrl + url;
        }
        RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, URL.encode(url));

        try {
            Request request = builder.sendRequest(null, new RequestCallback() {
                public void onResponseReceived(Request request, Response response) {
                    if (200 == response.getStatusCode()) {
                        adminPanel.setHTML(response.getText());
                        // Process the response in response.getText()
                    } else {
                        adminPanel.setHTML("Problem loading admin page");
                        // Handle the error.  Can get the status text from response.getStatusText()
                    }
                }


                public void onError(Request request, Throwable exception) {
                    Bootbox.alert(exception.toString());
                }
            });
        } catch (RequestException e) {
            Bootbox.alert(e.toString());
        }
    }

    public PreferencePanel() {
        initWidget(ourUiBinder.createAndBindUi(this));
        reload();
    }
}