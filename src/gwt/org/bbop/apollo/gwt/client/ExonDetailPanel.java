package org.bbop.apollo.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.Widget;
import org.gwtbootstrap3.client.ui.InputGroupAddon;
import org.gwtbootstrap3.client.ui.TextBox;

/**
 * Created by ndunn on 1/9/15.
 */
public class ExonDetailPanel extends Composite {

    interface ExonDetailPanelUiBinder extends UiBinder<Widget, ExonDetailPanel> {
    }

    private static ExonDetailPanelUiBinder ourUiBinder = GWT.create(ExonDetailPanelUiBinder.class);
    @UiField
    TextBox maxField;
    @UiField
    TextBox minField;
    @UiField
    org.gwtbootstrap3.client.ui.RadioButton positiveStrandValue;
    @UiField
    org.gwtbootstrap3.client.ui.RadioButton negativeStrandValue;

//    @UiField
//    org.gwtbootstrap3.client.ui.TextBox nameField;
//    @UiField
//    org.gwtbootstrap3.client.ui.TextBox symbolField;
//    @UiField
//    org.gwtbootstrap3.client.ui.TextBox descriptionField;
//    @UiField
//    InputGroupAddon locationField;
//    @UiField
//    RadioButton strandButton;


    public ExonDetailPanel() {
        initWidget(ourUiBinder.createAndBindUi(this));
    }

    public void updateData(JSONObject internalData) {
        GWT.log("updating transcript detail panel");
        GWT.log(internalData.toString());
//        nameField.setText(internalData.get("name").isString().stringValue());

        JSONObject locationObject = internalData.get("location").isObject();
        minField.setText(locationObject.get("fmin").isNumber().toString());
        maxField.setText(locationObject.get("fmax").isNumber().toString());

        if(locationObject.get("strand").isNumber().doubleValue() > 0){
            positiveStrandValue.setActive(true);
            negativeStrandValue.setActive(false);
        }
        else{
            positiveStrandValue.setActive(false);
            negativeStrandValue.setActive(true);
        }



        setVisible(true);
    }
}