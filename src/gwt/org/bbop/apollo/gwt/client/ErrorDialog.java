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

    private Boolean showOnBuild = true ;

    public ErrorDialog(boolean showOnConstruct){
        this("Error ...",null,showOnConstruct);
    }

    public ErrorDialog(){
        this("Error ...",null,true);
    }


    public ErrorDialog(String title){
        this(title,null,true);

    }
    public ErrorDialog(String title,String message,Boolean showOnConstruct){
        setTitle(title);
        setClosable(false);
        setFade(true);
        setDataBackdrop(ModalBackdrop.STATIC);

        if(message!=null){
            HTML content = new HTML(message);
            ModalBody modalBody = new ModalBody();
            modalBody.add(content);
            add( modalBody );
        }
        if(showOnConstruct){
            show();
        }
    }
}

