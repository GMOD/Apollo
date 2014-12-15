package org.bbop.apollo.gwt.client;


import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.SplitLayoutPanel;

/**
 * Created by ndunn on 12/15/14.
 */
public class GroupPanel extends SplitLayoutPanel{

    final HTML label = new HTML("User Panel");


    public GroupPanel(){
        add(label);
    }


}
