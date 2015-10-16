package org.bbop.apollo.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.*;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
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
//    @UiField
//    FlexTable statusList;
//    @UiField
//    TextBox newStatusField;
//    @UiField
//    Button newStatusButton;

    public void reload(){
        String url = "annotator/adminPanel";
        String rootUrl = Annotator.getRootUrl();
        if(!url.startsWith(rootUrl)){
            url = rootUrl+url;
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


//        /annotator/adminPanel



    }

//    public PreferencePanel() {
//        initWidget(ourUiBinder.createAndBindUi(this));
//
//
//        statusList.setHTML(0,0,"Status");
//        statusList.setHTML(0,1,"");
//        statusList.setHTML(0,2,"# of Annotations");
//
//        statusList.setHTML(1,0,"Approve");
//        statusList.setHTML(2,0,"Delete");
//        statusList.setHTML(3,0,"Replace");
//        statusList.setHTML(4,0,"Awaiting");
//
////        statusList.setWidget(1, 1, new Button("X"));
////        statusList.setWidget(2, 1, new Button("X"));
//        statusList.setWidget(3, 1, new Button("X"));
////        statusList.setWidget(4, 1, new Button("X"));
//
//        statusList.setHTML(1,2,"3");
//        statusList.setHTML(2,2,"4");
//        statusList.setHTML(3,2,"0");
//        statusList.setHTML(4,2,"10");
//    }
//
//    @UiHandler("newStatusButton")
//    public void newStatusButton(ClickEvent clickEvent){
//        String newText = newStatusField.getText();
//        int rowCount = statusList.getRowCount();
//        statusList.setHTML(rowCount,0,newText);
//        statusList.setWidget(rowCount,1, new Button("X"));
//        newStatusField.setText("");
//    }
}