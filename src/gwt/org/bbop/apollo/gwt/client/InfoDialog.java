package org.bbop.apollo.gwt.client;

import com.google.gwt.user.client.ui.HTML;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.ModalBody;
import org.gwtbootstrap3.client.ui.constants.ModalBackdrop;

/**
 * Created by Nathan Dunn on 4/30/15.
 */
public class InfoDialog extends Modal{

//    private Boolean showOnBuild = true ;

//    public InfoDialog(boolean showOnConstruct){
//        this("Loading ...",null,showOnConstruct);
//    }
//
//    public InfoDialog(){
//        this("Loading ...",null,true);
//    }


//    public InfoDialog(String title,String message){
//        this(title,message,true);
//    }

    public InfoDialog(String title, String message, Boolean showOnConstruct){
        setTitle(title);
        setClosable(true);
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
