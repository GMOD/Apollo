package org.bbop.apollo.gwt.client;

import com.google.gwt.user.client.ui.HTML;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.ModalBody;
import org.gwtbootstrap3.client.ui.constants.ModalBackdrop;

/**
 * Created by ndunn on 4/30/15.
 */
public class LinkDialog extends Modal {


    public LinkDialog(String title, String message, Boolean showOnConstruct) {
        setTitle(title);
        setClosable(true);
        setFade(true);
        setDataBackdrop(ModalBackdrop.STATIC);
        setDataKeyboard(true);

        if (message != null) {
            HTML content = new HTML(message);
            ModalBody modalBody = new ModalBody();
            modalBody.add(content);
            add(modalBody);
        }
        if (showOnConstruct) {
            show();
        }
    }
}
