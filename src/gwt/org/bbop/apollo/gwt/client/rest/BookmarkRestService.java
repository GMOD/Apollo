package org.bbop.apollo.gwt.client.rest;

import com.google.gwt.http.client.RequestCallback;
import org.bbop.apollo.gwt.client.dto.bookmark.BookmarkInfo;
import org.bbop.apollo.gwt.client.dto.bookmark.BookmarkInfoConverter;

import java.util.Set;

/**
 * This class stores Boorkmars
 * Created by nathandunn on 10/1/15.
 */
public class BookmarkRestService {

    public static void loadBookmarks(RequestCallback requestCallback) {
        RestService.sendRequest(requestCallback, "bookmark/list");
    }


    public static void addBookmark(RequestCallback requestCallback,BookmarkInfo bookmarkInfo) {
        RestService.sendRequest(requestCallback, "bookmark/addBookmark", BookmarkInfoConverter.convertBookmarkInfoToJSONObject(bookmarkInfo));
    }

    // TODO:
    public static void removeBookmarks(RequestCallback requestCallback,Set<BookmarkInfo> selectedSet) {
        RestService.sendRequest(requestCallback, "bookmark/deleteBookmark","data="+BookmarkInfoConverter.convertBookmarkInfoToJSONArray(selectedSet).toString());
    }
}
