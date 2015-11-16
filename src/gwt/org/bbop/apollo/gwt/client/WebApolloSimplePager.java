package org.bbop.apollo.gwt.client;


import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.view.client.Range;


public class WebApolloSimplePager extends SimplePager {

    public WebApolloSimplePager() {
        this.setRangeLimited(true);
    }

    public WebApolloSimplePager(TextLocation location, Resources resources, boolean showFastForwardButton, int fastForwardRows, boolean showLastPageButton) {
        super(location, resources, showFastForwardButton, fastForwardRows, showLastPageButton);
        this.setRangeLimited(true);
    }
    public WebApolloSimplePager(TextLocation location) {
        super(location, false, true);
        this.setRangeLimited(true);
    }

    public void setPageStart(int index) {
        if (this.getDisplay() != null) {
            Range range = getDisplay().getVisibleRange();
            int pageSize = range.getLength();
            if (!isRangeLimited() && getDisplay().isRowCountExact()) {
                index = Math.min(index, getDisplay().getRowCount() - pageSize);
            }
            index = Math.max(0, index);
            if (index != range.getStart()) {
                getDisplay().setVisibleRange(index, pageSize);
            }
        }  
    }
}

