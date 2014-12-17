package org.bbop.apollo.gwt.client.demo;

import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.CheckBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ndunn on 12/17/14.
 */
public class DataGenerator {

    public static String[] organisms = {
            "Zebrafish",
            "Alligator Pipefish",
            "Bloody Stickleback",
            "Brook Stickleback",
            "Three-spined Stickleback",
            "Amur Stickleback",
            "Spinach Stickleback"
    };

    public static List<String> getOrganisms() {
        List<String> organisms = new ArrayList<>();

        return organisms;
    }


    public static List<String> getSequences() {
        List<String> sequences = new ArrayList<>();

        return sequences;
    }

    public static List<String> getAnnotations() {
        List<String> annotations = new ArrayList<>();

        return annotations;
    }

    public static List<String> getUsers() {
        List<String> users = new ArrayList<>();

        return users;
    }

    public static List<String> getGroups() {
        List<String> groups = new ArrayList<>();

        return groups;
    }


    public static IsWidget generateCheckBox(int i) {
        HorizontalPanel panel = new HorizontalPanel();
        CheckBox checkBox = new CheckBox();
        HTML html = new HTML("Track" + i);
        panel.add(checkBox);
        panel.add(html);
        return panel;
    }

    public static void populateOrganismList(ListBox organismList) {
        organismList.clear();
        for(String organism : organisms){
            organismList.addItem(organism);
        }
    }


}
