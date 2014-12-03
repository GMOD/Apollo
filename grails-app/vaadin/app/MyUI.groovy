package app

import com.vaadin.ui.UI
import com.vaadin.ui.VerticalLayout
import com.vaadin.server.VaadinRequest
import com.vaadin.ui.Label
import com.vaadin.grails.Grails

/**
 *
 *
 * @author
 */
class MyUI extends UI {

    @Override
    protected void init(VaadinRequest vaadinRequest) {

		VerticalLayout layout = new VerticalLayout()

        String homeLabel = Grails.i18n("default.home.label")
        Label label = new Label(homeLabel)
        layout.addComponent(label)

        // example of how to get Grails service and call a method
        // List<User> users = Grails.get(UserService).getListOfUsers()
        //    for (User user : users) {
        //    	layout.addComponent(new Label(user.name))
        // }

		setContent(layout)
    }
}
