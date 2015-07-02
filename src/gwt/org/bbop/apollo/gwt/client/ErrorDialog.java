package org.bbop.apollo.gwt.client;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.ui.*;
import org.bbop.apollo.gwt.client.rest.UserRestService;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.ModalBody;
import org.gwtbootstrap3.client.ui.constants.ModalBackdrop;

/**
 * Created by ndunn on 3/17/15.
 */
//  TODO: this needs to be moved into UIBinder into its own class
public class ErrorDialog extends Modal{

    Button logoutButton;

    public ErrorDialog(String title,String message,boolean showOnConstruct, boolean closeModal) {
        this(title,message,showOnConstruct,closeModal,false);
    }

    public ErrorDialog(String title,String message,boolean showOnConstruct, boolean closeModal, boolean showLogoutButton){
        setTitle(title);
        setClosable(closeModal);
        setFade(true);
        setDataBackdrop(ModalBackdrop.STATIC);

        if(message!=null){
            HTML content = new HTML(message);
            ModalBody modalBody = new ModalBody();
            modalBody.add(content);

            if(showLogoutButton) {

                logoutButton=new Button("Logout", new ClickHandler() {
                    public void onClick(ClickEvent event) {
                        UserRestService.logout();
                    }
                });

                modalBody.add(logoutButton);
            }
            add( modalBody );
        }



        if(showOnConstruct){
            show();
        }
    }
}

