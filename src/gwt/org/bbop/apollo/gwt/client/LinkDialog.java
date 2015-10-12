package org.bbop.apollo.gwt.client;

import com.google.gwt.user.client.ui.HTML;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.ModalBody;
import org.gwtbootstrap3.client.ui.constants.ModalBackdrop;

/**
 * Created by ndunn on 4/30/15.
 */
public class LinkDialog extends Modal{

//    private Boolean showOnBuild = true ;

//    public LinkDialog(boolean showOnConstruct){
//        this("Loading ...",null,showOnConstruct);
//    }
//
//    public LinkDialog(){
//        this("Loading ...",null,true);
//    }

//    public LinkDialog(String title){
//        this(title,null,true);
//
//    }

    public LinkDialog(String title, String message, Boolean showOnConstruct){
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
