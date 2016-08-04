package org.bbop.apollo.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.uibinder.client.UiBinder;

/**
 * Created by deepak.unni3 on 8/2/16.
 */
public class VariantDetailPanel {
    interface VariantDetailPanelUiBinder extends UiBinder<DivElement, VariantDetailPanel> {
    }

    private static VariantDetailPanelUiBinder ourUiBinder = GWT.create(VariantDetailPanelUiBinder.class);

    public VariantDetailPanel() {
        DivElement rootElement = ourUiBinder.createAndBindUi(this);
    }
}