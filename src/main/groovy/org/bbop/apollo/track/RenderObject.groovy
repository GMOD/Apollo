package org.bbop.apollo.track

/**
 * Created by nathandunn on 6/8/17.
 */
class RenderObject {

    int globalFmin
    int globalFmax


    int getGlobalWidth() {
        return globalFmax - globalFmin
    }
}
