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
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.uibinder.client.UiBinder;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Input;
import org.gwtbootstrap3.client.ui.Icon;
import org.gwtbootstrap3.client.ui.CheckBox;
import org.gwtbootstrap3.client.ui.html.Div;
import org.gwtbootstrap3.client.ui.html.Paragraph;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.constants.*;
import org.bbop.apollo.gwt.client.rest.UserRestService;

public class LoginDialog extends DialogBox {
    private static final Binder binder = GWT.create(Binder.class);
    interface Binder extends UiBinder<Widget, LoginDialog> {
    }


    @UiField
    Div errorHtml;

    @UiField
    Paragraph errorText;




    @UiField
    Button loginButton;

    @UiField
    TextBox userBox;

    @UiField
    Input passBox;

    @UiField
    CheckBox rememberBox;



    public LoginDialog() {
        getElement().setId("loginDialogId");
        setText("Login");
        setAnimationEnabled(true);
        // Enable glass background.
        setGlassEnabled(true);
        setWidget(binder.createAndBindUi(this));
    }

    public void showLogin() {
        Icon icon = new Icon(IconType.WARNING);
        errorHtml.add(icon);
        errorText.setEmphasis(Emphasis.DANGER);
        clearErrors();
        center();
        show();
        userBox.setFocus(true);
    }

    public void setError(String errorMessage){
        errorText.setText(errorMessage);
        errorHtml.setVisible(true);
    }
 
    public void clearErrors(){
        errorText.setText("");
        errorHtml.setVisible(false);
    }

    @Override
    public void onPreviewNativeEvent(NativePreviewEvent e) {
        NativeEvent nativeEvent = e.getNativeEvent();
        if ("keydown".equals(nativeEvent.getType())) {
            if (nativeEvent.getKeyCode() == KeyCodes.KEY_ENTER) {
                doLogin(userBox.getText().trim(), passBox.getText(), rememberBox.getValue());
            }
        }
    }

    @UiHandler("loginButton")
    public void submitAction(ClickEvent e) {
        doLogin(userBox.getText().trim(),passBox.getText(),rememberBox.getValue());
    }

    public void doLogin(String username,String password,Boolean rememberMe){
        UserRestService.login(username, password,rememberMe,this);
    }
}
