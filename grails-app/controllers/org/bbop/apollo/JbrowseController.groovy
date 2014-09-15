package org.bbop.apollo

class JbrowseController {

    def index() { }

    /**
     * Has to handle a number of routes based on selected genome or just use the default otherwise.
     *
     * trackList.json
     * tracks.conf . . .  should be a .json
     * names/meta.json
     * . .  pass-through for css . . . good to change
     * refSeq.json  (good to store in database)
     * 7.json  ??
     *
     * .. .  and original:
//     * data/tracks/Hsal_OGSv3.3/Group1.1/trackData.json
     * data/tracks/<track>/<annotation>/trackData.json
     *
     * data/tracks/Amel_4.5_brain_ovary.gff/Group1.1/lf-1.json  ?? a GFF to json manipulation
     *
     * data/bigwig/<filename>.bw
     *
     *
     */
    def data(){

    }
}
