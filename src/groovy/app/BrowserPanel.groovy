package app

import com.vaadin.ui.Button
import com.vaadin.ui.Label
import com.vaadin.ui.TreeTable
import com.vaadin.ui.VerticalLayout
import com.vaadin.ui.VerticalSplitPanel
//import groovyx.net.http.RESTClient

/**
 * Created by ndunn on 12/3/14.
 */
//@CompileStatic
class BrowserPanel extends VerticalSplitPanel {


    int count = 0


    BrowserPanel() {

        final TreeTable ttable = new TreeTable("My TreeTable");
        ttable.addContainerProperty("Name", String.class, "");
//        ttable.setWidth("100%");
//        ttable.setWidth("20em");
//        ttable.setWidth("100px");
//        ttable.setHeight("300px");

// Create the tree nodes
        ttable.addItem(["Root"] as Object[], 0);
        ttable.addItem(["Branch 1"] as Object[], 1);
        ttable.addItem(["Branch 2"] as Object[], 2);
        ttable.addItem(["Leaf 3"] as Object[], 3);
        ttable.addItem(["Leaf 4"] as Object[], 4);
        ttable.addItem(["Leaf 5"] as Object[], 5);
        ttable.addItem(["Leaf 6"] as Object[], 6);

// Set the hierarchy
        ttable.setParent(1, 0);
        ttable.setParent(2, 0);
        ttable.setParent(3, 1);
        ttable.setParent(4, 1);
        ttable.setParent(5, 2);
        ttable.setParent(6, 2);

        ttable.setCollapsed(0, false)
        ttable.setCollapsed(1, false)


        Label label = new Label("filter holder")

        Button button = new Button("ouch")
        button.addClickListener(new Button.ClickListener() {
            public void buttonClick(Button.ClickEvent event) {
                ++count
                button.setCaption("click count: " + count);
            }
        });

        VerticalLayout verticalLayout = new VerticalLayout()
        verticalLayout.addComponent(label)
        verticalLayout.addComponent(button)

        Button button2 = new Button("testnet")
        button2.addClickListener(new Button.ClickListener() {
            public void buttonClick(Button.ClickEvent event) {
//                RESTClient restClient = new RESTClient()
//                restClient.get("/sequence/test")
//                def twitter = new RESTClient( '../' )


//                def twitter = new RESTClient( 'https://api.twitter.com/1.1/statuses/' )

            }
        });

        verticalLayout.addComponent(button2)

        setFirstComponent(verticalLayout)


        setSecondComponent(ttable)

        ttable.setSizeFull()
//        setCompositionRoot(verticalSplitPanel)

    }

}
