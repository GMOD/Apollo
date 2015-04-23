package org.bbop.apollo.gwt.client;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.*;

/**
 * Created by ndunn on 4/8/15.
 */
public class UrlDialogBox extends DialogBox{

    private VerticalPanel panel = new VerticalPanel();
    private HorizontalPanel buttonPanel = new HorizontalPanel();
    private TextArea urlView = new TextArea();
    private Button closeButton = new Button("OK");

    public UrlDialogBox(String url){

        buttonPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        buttonPanel.setCellHorizontalAlignment(closeButton,HasHorizontalAlignment.ALIGN_CENTER);
        buttonPanel.setWidth("100%");

        urlView.setCharacterWidth(100);
        urlView.setEnabled(false);
        urlView.setText(url);

        panel.add(urlView);
        panel.add(buttonPanel);
        buttonPanel.add(closeButton);
        panel.add(buttonPanel);

        closeButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                hide();
            }
        });

        setWidget(panel);
    }
}
