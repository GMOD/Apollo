package org.bbop.apollo.gwt.client;

import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.ModalBody;
import org.gwtbootstrap3.client.ui.constants.ModalBackdrop;

/**
 * Created by ndunn on 4/30/15.
 */
public class LoadingDialog extends Modal{

    public LoadingDialog(boolean showOnConstruct){
        this("Loading ...",null,showOnConstruct);
    }

    public LoadingDialog(){
        this("Loading ...",null,true);
    }


    public LoadingDialog(String title){
        this(title,null,true);

    }
    public LoadingDialog(String title,String message,Boolean showOnConstruct){
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
