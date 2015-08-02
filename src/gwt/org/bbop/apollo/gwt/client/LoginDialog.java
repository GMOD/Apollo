package org.bbop.apollo.gwt.client;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.Event.*;
import com.google.gwt.user.client.Window;
import org.bbop.apollo.gwt.client.rest.UserRestService;

/**
 * Created by ndunn on 3/17/15.
 */
//  TODO: this needs to be moved into UIBinder into its own class
public class LoginDialog extends DialogBox {

    // TODO: move to UIBinder
    private VerticalPanel panel = new VerticalPanel();
    private Grid grid = new Grid(2,2);
    private Button okButton = new Button("Login");
    private TextBox username = new TextBox();
    private PasswordTextBox passwordTextBox = new PasswordTextBox();
    private HorizontalPanel horizontalPanel = new HorizontalPanel();
    private CheckBox rememberMeCheckBox = new CheckBox("Remember me");

    public LoginDialog() {
        // Set the dialog box's caption.
        setText("Login");
        // Enable animation.
        setAnimationEnabled(true);
        // Enable glass background.
        setGlassEnabled(true);

        grid.setHTML(0, 0, "Username");
        grid.setWidget(0, 1, username);
        grid.setHTML(1, 0, "Password");
        grid.setWidget(1, 1, passwordTextBox);
        panel.add(grid);

        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                username.setFocus(true);
            }
        });

        horizontalPanel.add(rememberMeCheckBox);
        horizontalPanel.add(new HTML("&nbsp;&nbsp;&nbsp;&nbsp;"));
        horizontalPanel.add(okButton);
        panel.add(horizontalPanel);
        // DialogBox is a SimplePanel, so you have to set its widget property to
        // whatever you want its contents to be.
        okButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                doLogin(username.getText().trim(),passwordTextBox.getText(),rememberMeCheckBox.getValue());
            }
        });
        setWidget(panel);
    }
    @Override
    public void onPreviewNativeEvent(NativePreviewEvent e) {
        NativeEvent nativeEvent = e.getNativeEvent();
        if ("keydown".equals(nativeEvent.getType())) {
            if (nativeEvent.getKeyCode() == KeyCodes.KEY_ENTER) {
                doLogin(username.getText().trim(),passwordTextBox.getText(),rememberMeCheckBox.getValue());
            }
        }
    }

    public void doLogin(String username,String password,Boolean rememberMe){
        UserRestService.login(username, password,rememberMe);
    }
}

