package org.bbop.apollo.gwt.client.assemblage;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Created by nathandunn on 9/19/16.
 */
public class VerticalPanelWithSpacer extends VerticalPanel{
    public VerticalPanelWithSpacer() {
        Label spacerLabel = new Label("");
        spacerLabel.setStylePrimaryName("assemblage-detail-verticle-spacer");
        super.add(spacerLabel);
    }

    @Override
    public void add(Widget w) {
        super.insert(w, getWidgetCount() - 1);
    }

    @Override
    public void insert(Widget w, int beforeIndex) {
        if (beforeIndex == getWidgetCount()) {
            beforeIndex--;
        }
        super.insert(w, beforeIndex);
    }
}
