package org.bbop.apollo.gwt.client;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.ScrollPanel;

/**
 * Created by ndunn on 12/15/14.
 */
public class VerticalScrollPanel extends ScrollPanel{

//    @Override
//    public void setAlwaysShowScrollBars(boolean alwaysShow) {
//    }

    public void setAlwaysShowVerticalScrollBar(boolean alwaysShow) {
        getScrollableElement().getStyle().setOverflowY(Style.Overflow.SCROLL);
        getScrollableElement().getStyle().setOverflowX(Style.Overflow.SCROLL);

//        getScrollableElement().getStyle().setOverflowY(alwaysShow ? Style.Overflow.SCROLL: Style.Overflow.AUTO);
    }
}
