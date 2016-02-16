package org.bbop.apollo.gwt.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import org.bbop.apollo.gwt.client.projection.ProjectionService;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Annotator implements EntryPoint {

    public static EventBus eventBus = GWT.create(SimpleEventBus.class);

    private static ProjectionService projectionService = new ProjectionService();

    /**
     * This is the entry point method.
     */
    public void onModuleLoad() {
        exportStaticMethod();
        MainPanel mainPanel = MainPanel.getInstance();
        RootLayoutPanel rp = RootLayoutPanel.get();
        rp.add(mainPanel);
        rp.setWidgetTopHeight(mainPanel, 0, Style.Unit.PX, 100, Style.Unit.PCT);
    }

    public static String getRootUrl() {
        String rootUrl = GWT.getModuleBaseURL().replace("annotator/", "");
        return rootUrl;
    }


    public static native void exportStaticMethod() /*-{
        $wnd.getEmbeddedVersion = $entry(
            function apolloEmbeddedVersion() {
                return 'ApolloGwt-2.0';
            }
        );

    }-*/;

}
