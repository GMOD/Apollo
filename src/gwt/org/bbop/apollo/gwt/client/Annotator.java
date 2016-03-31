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

        Dictionary optionsDictionary = Dictionary.getDictionary("Options");
        Double height = 100d ;
        Style.Unit heightUnit = Style.Unit.PCT;
        Double top = 0d ;
        Style.Unit topUnit = Style.Unit.PCT;
//        Double bottom = 0d ;
//        Style.Unit bottomUnit = Style.Unit.PCT;

        if(optionsDictionary.keySet().contains("top")){
            top = Double.valueOf(optionsDictionary.get("top"));
        }
        if(optionsDictionary.keySet().contains("topUnit")){
            topUnit = Style.Unit.valueOf(optionsDictionary.get("topUnit").toUpperCase());
        }
        if(optionsDictionary.keySet().contains("height")){
            height = Double.valueOf(optionsDictionary.get("height"));
        }
        if(optionsDictionary.keySet().contains("heightUnit")){
            heightUnit = Style.Unit.valueOf(optionsDictionary.get("heightUnit").toUpperCase());
        }
//        if(optionsDictionary.keySet().contains("bottom")){
//            bottom = Double.valueOf(optionsDictionary.get("bottom"));
//        }
//        if(optionsDictionary.keySet().contains("bottomUnit")){
//            bottomUnit = Style.Unit.valueOf(optionsDictionary.get("bottomUnit").toUpperCase());
//        }

//        rp.setWidgetTopHeight(mainPanel, 0, Style.Unit.PX, 100, Style.Unit.PCT);
        rp.setWidgetTopHeight(mainPanel, top, topUnit, height, heightUnit);
//        rp.setW(mainPanel, 0, Style.Unit.PX, 100, Style.Unit.PCT);


    }

    public static String getRootUrl(){
        String rootUrl = GWT.getModuleBaseURL().replace("annotator/","");
        return rootUrl ;
    }

}
