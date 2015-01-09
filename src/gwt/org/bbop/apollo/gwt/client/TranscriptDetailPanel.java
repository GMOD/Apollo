package org.bbop.apollo.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import org.gwtbootstrap3.client.ui.InputGroupAddon;
import org.gwtbootstrap3.client.ui.gwt.CellTable;

/**
 * Created by ndunn on 1/9/15.
 */
public class TranscriptDetailPanel extends Composite {

    interface AnnotationDetailPanelUiBinder extends UiBinder<Widget, TranscriptDetailPanel> {
    }

    private static AnnotationDetailPanelUiBinder ourUiBinder = GWT.create(AnnotationDetailPanelUiBinder.class);

    @UiField
    org.gwtbootstrap3.client.ui.TextBox nameField;
    @UiField
    org.gwtbootstrap3.client.ui.TextBox symbolField;
    @UiField
    org.gwtbootstrap3.client.ui.TextBox descriptionField;
    @UiField
    InputGroupAddon locationField;


    public TranscriptDetailPanel() {
        initWidget(ourUiBinder.createAndBindUi(this));
    }

    public void updateData(JSONObject internalData) {
        GWT.log("updating transcript detail panel");
        GWT.log(internalData.toString());
        nameField.setText(internalData.get("name").isString().stringValue());
        symbolField.setText(internalData.containsKey("symbol") ? internalData.get("symbol").isString().stringValue(): "");
        descriptionField.setText(internalData.containsKey("description") ? internalData.get("description").isString().stringValue() : "");

        JSONObject locationObject = internalData.get("location").isObject();
        String locationText = locationObject.get("fmin").isNumber().toString();
        locationText += " - ";
        locationText += locationObject.get("fmax").isNumber().toString();
        locationText += " strand(";
        locationText += locationObject.get("strand").isNumber().doubleValue() > 0 ? "+" : "-";
        locationText += ")";

        locationField.setText(locationText);

        setVisible(true);
    }
}