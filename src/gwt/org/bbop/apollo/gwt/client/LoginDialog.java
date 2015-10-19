package org.bbop.apollo.gwt.client;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.Event.*;
import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;

import com.google.gwt.user.client.ui.*;
import org.gwtbootstrap3.client.ui.*;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.CheckBox;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.constants.*;
import org.bbop.apollo.gwt.client.rest.UserRestService;

public class LoginDialog extends DialogBox {
    private static final Binder binder = GWT.create(Binder.class);
    interface Binder extends UiBinder<Widget, LoginDialog> {
    }


    private Heading errorHtml = new Heading(HeadingSize.H4);



    @UiField
    Button loginButton;

    @UiField
    TextBox userBox;

    @UiField
    Input passBox;

    @UiField
    CheckBox rememberBox;



    public LoginDialog() {
        setText("Login");
        setAnimationEnabled(true);
        // Enable glass background.
        setGlassEnabled(true);
        errorHtml.setEmphasis(Emphasis.DANGER);
        Icon icon = new Icon(IconType.WARNING);
        errorHtml.add(icon);
        setWidget(binder.createAndBindUi(this));
    }

    public void showLogin() {
        clearErrors();
        center();
        show();
    }

    public void setError(String errorMessage){
        errorHtml.setText(errorMessage);
        errorHtml.setVisible(true);
    }
 
    public void clearErrors(){
        errorHtml.setText("");
        errorHtml.setVisible(false);
    }

    @UiHandler("loginButton")
    public void submitAction(ClickEvent e) {
        String user=userBox.getText();
        String pass=passBox.getText();
        Boolean remember=rememberBox.getValue();
        doLogin(user,pass,remember);
    }

    public void doLogin(String username,String password,Boolean rememberMe){
        UserRestService.login(username, password,rememberMe,this);
    }
}
