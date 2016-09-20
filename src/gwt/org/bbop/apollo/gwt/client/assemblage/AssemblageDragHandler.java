package org.bbop.apollo.gwt.client.assemblage;

import com.allen_sauer.gwt.dnd.client.DragEndEvent;
import com.allen_sauer.gwt.dnd.client.DragHandler;
import com.allen_sauer.gwt.dnd.client.DragStartEvent;
import com.allen_sauer.gwt.dnd.client.VetoDragException;
import com.google.gwt.user.client.ui.HTML;

/**
 * Created by nathandunn on 9/19/16.
 */
public class AssemblageDragHandler implements DragHandler{

    /**
     * CSS blue.
     */
    public static final String BLUE = "#4444BB";

    /**
     * CSS green.
     */
    public static final String GREEN = "#44BB44";

    /**
     * CSS red.
     */
    public static final String RED = "#BB4444";


    /**
     * Text area where event messages are shown.
     */
    private final HTML eventTextArea;

    AssemblageDragHandler(HTML dragHandlerHTML) {
        eventTextArea = dragHandlerHTML;
    }

    /**
     * Log the drag end event.
     *
     * @param event the event to log
     */
    @Override
    public void onDragEnd(DragEndEvent event) {
        log("onDragEnd: " + event, RED);
    }

    /**
     * Log the drag start event.
     *
     * @param event the event to log
     */
    @Override
    public void onDragStart(DragStartEvent event) {
        log("onDragStart: " + event, GREEN);
    }

    /**
     * Log the preview drag end event.
     *
     * @param event the event to log
     * @throws VetoDragException exception which may be thrown by any drag handler
     */
    @Override
    public void onPreviewDragEnd(DragEndEvent event) throws VetoDragException {
        log("<br>onPreviewDragEnd: " + event, BLUE);
    }

    /**
     * Log the preview drag start event.
     *
     * @param event the event to log
     * @throws VetoDragException exception which may be thrown by any drag handler
     */
    @Override
    public void onPreviewDragStart(DragStartEvent event) throws VetoDragException {
        log("onPreviewDragStart: " + event, BLUE);
    }

    public void clear() {
        eventTextArea.setHTML("");
    }

    public void log(String text, String color) {
        eventTextArea.setHTML(eventTextArea.getHTML()
                + (eventTextArea.getHTML().length() == 0 ? "" : "<br>") + "<span style='color: " + color
                + "'>" + text + "</span>");
    }
}
