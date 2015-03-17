package org.bbop.apollo.gwt.client;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.*;
import org.bbop.apollo.gwt.client.rest.UserRestService;

/**
 * Created by ndunn on 3/17/15.
 */
//  TODO: this needs to be moved into UIBinder into its own class
public class LoginDialog extends DialogBox {

    // TODO: move to UIBinder
    private VerticalPanel panel = new VerticalPanel();
    private Grid grid = new Grid(2,2);
    private Button ok = new Button("Login");
    private Button cancel = new Button("Cancel");
    private TextBox username = new TextBox();
    private PasswordTextBox passwordTextBox = new PasswordTextBox();
    private HorizontalPanel horizontalPanel = new HorizontalPanel();

    //
    public LoginDialog() {
//            // Set the dialog box's caption.
        setText("Login");
//
//            // Enable animation.
        setAnimationEnabled(true);
//
//            // Enable glass background.
        setGlassEnabled(true);

        grid.setHTML(0,0,"Username");
        grid.setWidget(0, 1, username);
        grid.setHTML(1, 0, "Password");
        grid.setWidget(1, 1, passwordTextBox);
        panel.add(grid);
        

        horizontalPanel.add(ok);
        horizontalPanel.add(cancel);
        panel.add(horizontalPanel);
//
//            // DialogBox is a SimplePanel, so you have to set its widget property to
//            // whatever you want its contents to be.
       

        cancel.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                LoginDialog.this.hide();
            }
        });
        ok.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                doLogin(username.getText().trim(),passwordTextBox.getText());
            }
        });
        setWidget(panel);
    }

    public void doLogin(String username,String password){
        UserRestService.login(username, password);
    }
}

