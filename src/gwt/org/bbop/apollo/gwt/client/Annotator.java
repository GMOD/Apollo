package org.bbop.apollo.gwt.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;

import java.util.Random;


/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Annotator implements EntryPoint {

    public static EventBus eventBus = GWT.create(SimpleEventBus.class);
    private static Storage preferenceStore = Storage.getSessionStorageIfSupported();
    private final static Random random = new Random(); // or SecureRandom

    // TODO: move
    private static String generateRandomString(int length) {
        Integer value = Math.abs(Integer.valueOf(random.nextInt()));
        return value.toString();
    }

    /**
     * This is the entry point method.
     */
    public void onModuleLoad() {
        MainPanel mainPanel = MainPanel.getInstance();
        RootLayoutPanel rp = RootLayoutPanel.get();
        rp.add(mainPanel);

        Dictionary optionsDictionary = Dictionary.getDictionary("Options");
        Double height = 100d;
        Style.Unit heightUnit = Style.Unit.PCT;
        Double top = 0d;
        Style.Unit topUnit = Style.Unit.PCT;

        if (optionsDictionary.keySet().contains("top")) {
            top = Double.valueOf(optionsDictionary.get("top"));
        }
        if (optionsDictionary.keySet().contains("topUnit")) {
            topUnit = Style.Unit.valueOf(optionsDictionary.get("topUnit").toUpperCase());
        }
        if (optionsDictionary.keySet().contains("height")) {
            height = Double.valueOf(optionsDictionary.get("height"));
        }
        if (optionsDictionary.keySet().contains("heightUnit")) {
            heightUnit = Style.Unit.valueOf(optionsDictionary.get("heightUnit").toUpperCase());
        }
        rp.setWidgetTopHeight(mainPanel, top, topUnit, height, heightUnit);

        exportStaticMethod();
    }

    public static native void exportStaticMethod() /*-{
        $wnd.setPreference = $entry(@org.bbop.apollo.gwt.client.Annotator::setPreference(Ljava/lang/String;Ljava/lang/Object;));
        $wnd.getPreference = $entry(@org.bbop.apollo.gwt.client.Annotator::getPreference(Ljava/lang/String;));
        $wnd.getClientToken = $entry(@org.bbop.apollo.gwt.client.Annotator::getClientToken());
    }-*/;

    public static void setPreference(String key, Object value) {
        if (preferenceStore != null) {
            preferenceStore.setItem(key, value.toString());
        }
    }

    public static String getPreference(String key) {
        if (preferenceStore != null) {
            String returnValue = preferenceStore.getItem(key);
            return returnValue;
        }
        return null;
    }


    public static String getRootUrl(){
        String rootUrl = GWT.getModuleBaseURL().replace("annotator/","");
        return rootUrl ;
    }

    public static String getClientToken() {
        String clientID = getPreference(FeatureStringEnum.CLIENT_TOKEN.getValue());
        if (clientID == null) {
            setPreference(FeatureStringEnum.CLIENT_TOKEN.getValue(), generateRandomString(130));
        }
        return getPreference(FeatureStringEnum.CLIENT_TOKEN.getValue());

    }
}
