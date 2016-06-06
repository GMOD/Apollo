package org.bbop.apollo.gwt.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.core.java.util.HashMap_CustomFieldSerializer;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import org.bbop.apollo.gwt.shared.ClientTokenGenerator;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;

import java.util.HashMap;
import java.util.Map;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Annotator implements EntryPoint {

    public static EventBus eventBus = GWT.create(SimpleEventBus.class);
    private static Storage preferenceStore = Storage.getSessionStorageIfSupported();
    private static Map<String,String> backupPreferenceStore = new HashMap<>();


    /**
     * This is the entry point method.
     */
    public void onModuleLoad() {
        MainPanel mainPanel = MainPanel.getInstance();
        RootLayoutPanel rp = RootLayoutPanel.get();
        rp.add(mainPanel);

        Dictionary optionsDictionary = Dictionary.getDictionary("Options");
        if(optionsDictionary.keySet().contains(FeatureStringEnum.CLIENT_TOKEN.getValue())){
            String clientToken = optionsDictionary.get(FeatureStringEnum.CLIENT_TOKEN.getValue());
            if(ClientTokenGenerator.isValidToken(clientToken)){
                setPreference(FeatureStringEnum.CLIENT_TOKEN.getValue(),clientToken);
            }
        }
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
        $wnd.getEmbeddedVersion = $entry(
            function apolloEmbeddedVersion() {
                return 'ApolloGwt-2.0';
            }
        );
    }-*/;

    public static void setPreference(String key, Object value) {
        if (preferenceStore != null) {
            preferenceStore.setItem(key, value.toString());
        }
        else{
            backupPreferenceStore.put(key,value.toString());
        }
    }

    public static String getPreference(String key) {
        if (preferenceStore != null) {
            return preferenceStore.getItem(key);
        }
        else{
            return backupPreferenceStore.get(key);
        }
    }


    public static String getRootUrl(){
        return GWT.getModuleBaseURL().replace("annotator/","");
    }


    public static String getClientToken() {
        String token = getPreference(FeatureStringEnum.CLIENT_TOKEN.getValue());
        if (!ClientTokenGenerator.isValidToken(token)) {
            token = ClientTokenGenerator.generateRandomString();
            setPreference(FeatureStringEnum.CLIENT_TOKEN.getValue(), token);
        }
        token = getPreference(FeatureStringEnum.CLIENT_TOKEN.getValue());
        return token ;

    }
}
