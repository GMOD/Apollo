package org.bbop.apollo.gwt.client;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import org.bbop.apollo.gwt.client.rest.UserRestService;

/**
 * Created by ndunn on 3/17/15.
 */
//  TODO: this needs to be moved into UIBinder into its own class
public class RegisterDialog extends DialogBox {

    // TODO: move to UIBinder
    private VerticalPanel panel = new VerticalPanel();
    private Grid grid = new Grid(5,2);
    private Button okButton = new Button("Register & Login");
    private TextBox username = new TextBox();
    private TextBox firstNameBox = new TextBox();
    private TextBox lastNameBox = new TextBox();
    private PasswordTextBox passwordTextBox = new PasswordTextBox();
    private PasswordTextBox passwordRepeatTextBox = new PasswordTextBox();
    private HorizontalPanel horizontalPanel = new HorizontalPanel();
    private CheckBox rememberMeCheckBox = new CheckBox("Remember me");

    public RegisterDialog() {
        // Set the dialog box's caption.
        setText("Register First Admin User");
        // Enable animation.
        setAnimationEnabled(true);
        // Enable glass background.
        setGlassEnabled(true);

        grid.setHTML(0, 0, "Username");
        grid.setWidget(0, 1, username);
        grid.setHTML(1, 0, "Password");
        grid.setWidget(1, 1, passwordTextBox);
        grid.setHTML(2, 0, "Repeat Password");
        grid.setWidget(2, 1, passwordRepeatTextBox);
        grid.setHTML(3, 0, "First Name");
        grid.setWidget(3, 1, firstNameBox);
        grid.setHTML(4, 0, "Last Name");
        grid.setWidget(4, 1, lastNameBox);
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

    public void doRegister(){
        if(passwordTextBox.getText().length()<4){
            Window.alert("Passwords must be at least 4 characters");
        }

        if(!passwordTextBox.getText().equals(passwordRepeatTextBox.getText())){
            Window.alert("Passwords do not match");
            return ;
        }

        String usernameText = username.getText() ;
        // TODO: use a better regexp search
        if(!usernameText.contains("@")&&!usernameText.contains(".")){
            Window.alert("Username does not appear to be an email");
            return ;
        }


        UserRestService.registerAdmin(username.getText().trim(), passwordTextBox.getText(), rememberMeCheckBox.getValue(),firstNameBox.getText().trim(),lastNameBox.getText().trim());
    }
}

