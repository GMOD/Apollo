package org.bbop.apollo.gwt.client;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.ModalBody;
import org.gwtbootstrap3.client.ui.constants.ButtonType;
import org.gwtbootstrap3.client.ui.constants.ModalBackdrop;

/**
 * Created by Nathan Dunn on 4/30/15.
 */
public class InfoDialog extends Modal{

//    private Button closeButton = new Button("OK");

    public InfoDialog(String title, String message, Boolean showOnConstruct){
        setTitle(title);
        setClosable(true);
        setFade(true);
        setDataKeyboard(true);
        setDataBackdrop(ModalBackdrop.STATIC);
//        closeButton.setType(ButtonType.PRIMARY);
//        closeButton.addClickHandler(new ClickHandler() {
//            @Override
//            public void onClick(ClickEvent event) {
//                hide();
//            }
//        });
//        setDataBackdrop(ModalBackdrop.STATIC);

        if(message!=null){
//            FlowPanel horizontalPanel = new FlowPanel();
//
            HTML content = new HTML(message);
//            horizontalPanel.add(content);
//            horizontalPanel.add(closeButton);
            ModalBody modalBody = new ModalBody();
            modalBody.add(content);
//            modalBody.add(horizontalPanel);
            add( modalBody );
        }
        if(showOnConstruct){
            show();
        }
    }
}
