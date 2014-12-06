package org.bbop.apollo.gwt.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.http.client.*;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import org.bbop.apollo.gwt.shared.FieldVerifier;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Annotator implements EntryPoint {
  /**
   * The message displayed to the user when the server cannot be reached or
   * returns an error.
   */
  private static final String SERVER_ERROR = "An error occurred while "
      + "attempting to contact the server. Please check your network "
      + "connection and try again.";

  /**
   * This is the entry point method.
   */
  public void onModuleLoad() {
    final Button sendButton = new Button("Send");
    final Button doJsButton = new Button("Do JS");
    final TextBox nameField = new TextBox();
    nameField.setText("Jerry the GWT User");
    final Label errorLabel = new Label();
    final Label feedbackLabel = new Label("pre-response");

    // We can add style names to widgets
    sendButton.addStyleName("sendButton");

    // Add the nameField and sendButton to the RootPanel
    // Use RootPanel.get() to get the entire body element
//    RootPanel.get("nameFieldContainer").add(nameField);
//    RootPanel.get("sendButtonContainer").add(sendButton);
//    RootPanel.get("errorLabelContainer").add(errorLabel);
//    RootPanel.get("feedbackLabelContainer").add(feedbackLabel);

    VerticalPanel oldPanel = new VerticalPanel();
    oldPanel.add(nameField);
    oldPanel.add(sendButton);
    oldPanel.add(doJsButton);
    oldPanel.add(errorLabel);
    oldPanel.add(feedbackLabel);

    Frame frame = new Frame("http://localhost:8080/apollo/jbrowse/?loc=Group1.3%3A14865..15198&tracks=DNA%2CAnnotations%2COfficial%20Gene%20Set%20v3.2%2CGeneID%2CCflo_OGSv3.3&highlight=");
    frame.setHeight("100%");
    frame.setWidth("100%");



    SplitLayoutPanel p = new SplitLayoutPanel();
    p.addWest(new HTML("navigation"), 128);
    p.addNorth(oldPanel, 384);
    p.add(frame);
    RootLayoutPanel rp = RootLayoutPanel.get();
    rp.add(p);


    // Focus the cursor on the name field when the app loads
    nameField.setFocus(true);
    nameField.selectAll();

    // Create the popup dialog box
//    final DialogBox dialogBox = new DialogBox();
//    dialogBox.setText("Remote Procedure Call");
//    dialogBox.setAnimationEnabled(true);
//    final Button closeButton = new Button("Close");
//    // We can set the id of a widget by accessing its Element
//    closeButton.getElement().setId("closeButton");
//    final Label textToServerLabel = new Label();
//    final HTML serverResponseLabel = new HTML();
//    VerticalPanel dialogVPanel = new VerticalPanel();
//    dialogVPanel.addStyleName("dialogVPanel");
//    dialogVPanel.add(new HTML("<b>Sending name to the server:</b>"));
//    dialogVPanel.add(textToServerLabel);
//    dialogVPanel.add(new HTML("<br><b>Server replies:</b>"));
//    dialogVPanel.add(serverResponseLabel);
//    dialogVPanel.setHorizontalAlignment(VerticalPanel.ALIGN_RIGHT);
//    dialogVPanel.add(closeButton);
//    dialogBox.setWidget(dialogVPanel);

    // Add a handler to close the DialogBox
//    closeButton.addClickHandler(new ClickHandler() {
//      public void onClick(ClickEvent event) {
//        dialogBox.hide();
//        sendButton.setEnabled(true);
//        sendButton.setFocus(true);
//      }
//    });

    doJsButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
//          Window.alert("doing a click");
      }
    });


    // Add a handler to send the name to the server
//    MyHandler handler = new MyHandler();
    sendButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
//        RequestBuilder requestBuilder  = new RequestBuilder();
        String url = "http://localhost:8080/apollo/annotator/what";
        RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, URL.encode(url));
        JSONObject jsonObject = new JSONObject();
//        jsonObject.put("data", new JSONString("asdasdf"));
        jsonObject.put("thekey", new JSONString("asdasdf"));
        builder.setRequestData("data=" + jsonObject.toString());
//        builder.setHeader("Content-Type", "application/json");
        builder.setHeader("Content-type", "application/x-www-form-urlencoded");
//        builder.setHeader("Accept","application/json");
//        builder.setHeader("Accept","/json");
        RequestCallback requestCallback = new RequestCallback() {
          @Override
          public void onResponseReceived(Request request, Response response) {
            feedbackLabel.setText("success: ["+response.getText()+"]");
          }

          @Override
          public void onError(Request request, Throwable exception) {
            Window.alert("ow");
          }
        };
        try {
          builder.setCallback(requestCallback);
          builder.send();
        } catch (RequestException e) {
          // Couldn't connect to server
          Window.alert(e.getMessage());
        }

      }
    });
//    nameField.addKeyUpHandler(handler);
  }
}
