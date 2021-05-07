package org.bbop.apollo.gwt.client;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HTML;
import org.bbop.apollo.gwt.client.dto.AnnotationInfo;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.ModalBody;
import org.gwtbootstrap3.client.ui.TextArea;
import org.gwtbootstrap3.client.ui.constants.ModalBackdrop;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;

/**
 * Created by ndunn on 4/30/15.
 */
public class UploadDialog extends Modal {


    public UploadDialog(AnnotationInfo annotationInfo) {
        setTitle("Upload: "+annotationInfo.getUniqueName());
        setClosable(true);
        setFade(true);
        setDataBackdrop(ModalBackdrop.STATIC);
        setDataKeyboard(true);

//        if (message != null) {
        TextArea textArea = new TextArea();
        ModalBody modalBody = new ModalBody();



        Button button = new Button("Apply Annotations");
        button.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                // TODO: convert and put in REST services with a nice return message.
                Bootbox.alert("adding");
                hide();
            }
        });

        modalBody.add(textArea);
        modalBody.add(button);

        add(modalBody);
//        }
//        if (showOnConstruct) {
//        }
        show();
    }
}
