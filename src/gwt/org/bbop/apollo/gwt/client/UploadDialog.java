package org.bbop.apollo.gwt.client;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HTML;
import org.bbop.apollo.gwt.client.dto.AnnotationInfo;
import org.gwtbootstrap3.client.ui.*;
import org.gwtbootstrap3.client.ui.constants.ModalBackdrop;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;

/**
 * Created by ndunn on 4/30/15.
 */
public class UploadDialog extends Modal {

    final TextArea textArea = new TextArea();

    public UploadDialog(AnnotationInfo annotationInfo) {
        setSize(ModalSize.LARGE);
        setHeight("500px");
        setClosable(true);
        setFade(true);
        setDataBackdrop(ModalBackdrop.STATIC);
        setDataKeyboard(true);
        setRemoveOnHide(true);

        ModalBody modalBody = new ModalBody();
        modalBody.setHeight("300px");
        textArea.setStyleName("");
        textArea.setHeight("250px");
        textArea.setWidth("100%");

        ModalHeader modalHeader = new ModalHeader();
        modalHeader.setTitle("Upload annotation for " + annotationInfo.getType() + " named: "+annotationInfo.getName());
        Button exampleLink = new Button("Example Text");
        exampleLink.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                textArea.setText("Example text here\nand here");
            }
        });
        modalHeader.add(exampleLink);


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

        ModalFooter modalFooter = new ModalFooter();
        modalFooter.add(button);

        add(modalHeader);
        add(modalBody);
        add(modalFooter);
//        }
//        if (showOnConstruct) {
//        }
        show();
    }
}
