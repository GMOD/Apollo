package org.bbop.apollo.gwt.client;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.google.gwt.json.client.JSONBoolean;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import org.bbop.apollo.gwt.client.rest.UserRestService;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;

/**
 * Created by ndunn on 3/17/15.
 */
//  TODO: this needs to be moved into UIBinder into its own class
public class RegisterDialog extends DialogBox {

    // TODO: move to UIBinder
    private VerticalPanel panel = new VerticalPanel();
    private Grid grid = new Grid(6, 2);
    private Button okButton = new Button("Register & Login");
    private TextBox username = new TextBox();
    private TextBox firstNameBox = new TextBox();
    private TextBox lastNameBox = new TextBox();
    private PasswordTextBox passwordTextBox = new PasswordTextBox();
    private PasswordTextBox passwordRepeatTextBox = new PasswordTextBox();
    private HorizontalPanel horizontalPanel = new HorizontalPanel();
    private CheckBox rememberMeCheckBox = new CheckBox("Remember me");
    private HTML errorMessage = new HTML("");

    public RegisterDialog() {
        // Set the dialog box's caption.
        setText("Register First Admin User");
        // Enable animation.
        setAnimationEnabled(true);
        // Enable glass background.
        setGlassEnabled(true);

        grid.setHTML(0, 0, "Username (email)");
        grid.setWidget(0, 1, username);
        grid.setHTML(1, 0, "Password");
        grid.setWidget(1, 1, passwordTextBox);
        grid.setHTML(2, 0, "Repeat Password");
        grid.setWidget(2, 1, passwordRepeatTextBox);
        grid.setHTML(3, 0, "First Name");
        grid.setWidget(3, 1, firstNameBox);
        grid.setHTML(4, 0, "Last Name");
        grid.setWidget(4, 1, lastNameBox);
        grid.setHTML(5, 0, "");
        grid.setWidget(5, 1, errorMessage);
        panel.add(grid);


        horizontalPanel.add(rememberMeCheckBox);
        horizontalPanel.add(new HTML("&nbsp;&nbsp;&nbsp;&nbsp;"));
        horizontalPanel.add(okButton);
        panel.add(horizontalPanel);
        // DialogBox is a SimplePanel, so you have to set its widget property to
        // whatever you want its contents to be.
        okButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                doRegister();
            }
        });
        setWidget(panel);
    }

    @Override
    public void onPreviewNativeEvent(NativePreviewEvent e) {
        NativeEvent nativeEvent = e.getNativeEvent();
        if ("keydown".equals(nativeEvent.getType())) {
            if (nativeEvent.getKeyCode() == KeyCodes.KEY_ENTER) {
                doRegister();
            }
        }
    }

    public void doRegister() {
        clearError();
        if (passwordTextBox.getText().length() < 4) {
            setError("Passwords must be at least 4 characters");
        }

        if (!passwordTextBox.getText().equals(passwordRepeatTextBox.getText())) {
            setError("Passwords do not match");
            return;
        }

        String usernameText = username.getText();
        // TODO: use a better regexp search
        if (!usernameText.contains("@") || !usernameText.contains(".")) {
            setError("Username does not appear to be an email");
            return;
        }
        registerAdmin(username.getText().trim(), passwordTextBox.getText(), rememberMeCheckBox.getValue(),firstNameBox.getText().trim(),lastNameBox.getText().trim());
    }

    public void registerAdmin(String username, String password, Boolean rememberMe, String firstName, String lastName) {
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                if (response.getStatusCode() < 200 || response.getStatusCode() > 299) {
                    setError("Problem during registration");
                }
                else{
                    Window.Location.reload();
                }
            }

            @Override
            public void onError(Request request, Throwable exception) {
                setError("Error registering admin: " + exception.getMessage());
            }
        };
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("operation", new JSONString("register"));
        jsonObject.put("username", new JSONString(username));
        jsonObject.put("password", new JSONString(URL.encodeQueryString(password)));
        jsonObject.put("rememberMe", JSONBoolean.getInstance(rememberMe));
        jsonObject.put("firstName", new JSONString(firstName));
        jsonObject.put("lastName", new JSONString(lastName));
        UserRestService.registerAdmin(requestCallback, jsonObject);
    }

    public void setError(String errroString) {
        errorMessage.setHTML("<font color='red'>" + errroString + "</font>");
    }

    public void clearError(){
        errorMessage.setHTML("");
    }
}

