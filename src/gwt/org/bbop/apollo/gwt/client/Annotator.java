package org.bbop.apollo.gwt.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.user.client.ui.*;
import org.bbop.apollo.gwt.client.dto.OrganismInfo;
import org.bbop.apollo.gwt.client.dto.SequenceInfo;
import org.bbop.apollo.gwt.client.dto.UserInfo;
import org.bbop.apollo.gwt.client.event.ExportEvent;
import org.bbop.apollo.gwt.client.event.ExportEventHandler;
//import org.realityforge.gwt.websockets.client.WebSocket;
//import org.realityforge.gwt.websockets.client.WebSocketListenerAdapter;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Annotator implements EntryPoint {

    public static EventBus eventBus = GWT.create(SimpleEventBus.class);
    /**
     * This is the entry point method.
     */
    public void onModuleLoad() {
        MainPanel mainPanel = MainPanel.getInstance();
        RootLayoutPanel rp = RootLayoutPanel.get();
        rp.add(mainPanel);
        rp.setWidgetTopHeight(mainPanel, 0, Style.Unit.PX, 100, Style.Unit.PCT);


    }

    public static String getRootUrl(){
        String rootUrl = GWT.getModuleBaseURL().replace("annotator/","");
        return rootUrl ;
    }

}
