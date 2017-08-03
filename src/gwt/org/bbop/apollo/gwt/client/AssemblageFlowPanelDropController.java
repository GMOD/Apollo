package org.bbop.apollo.gwt.client;

import com.allen_sauer.gwt.dnd.client.DragContext;
import com.allen_sauer.gwt.dnd.client.drop.FlowPanelDropController;
import com.allen_sauer.gwt.dnd.client.util.DragClientBundle;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;

/**
 * Created by nathandunn on 4/29/16.
 */
public class AssemblageFlowPanelDropController extends FlowPanelDropController{

    public AssemblageFlowPanelDropController(FlowPanel dropTarget){
        super(dropTarget);
    }


    @Override
    protected Widget newPositioner(DragContext context) {
//        HTML positioner = new HTML("&#x203B;");
        HTML positioner = new HTML("&#8660;");
        positioner.addStyleName(DragClientBundle.INSTANCE.css().flowPanelPositioner());
        return positioner;
    }
}
