
apollo.default_minimum_intron_size = 1
apollo.history_size = 0
apollo.overlapper_class = "org.bbop.apollo.sequence.OrfOverlapper"
apollo.track_name_comparator = "/config/track_name_comparator.js"
apollo.use_cds_for_new_transcripts = true
apollo.user_pure_memory_store = true
apollo.translation_table = "/config/translation_tables/ncbi_1_translation_table.txt"
apollo.is_partial_translation_allowed = false // unused so far
apollo.get_translation_code = 1
apollo.blat_executable = "/usr/local/bin/blat"

// TODO: should come from config or via preferences database
apollo.splice_donor_sites = [ "GT"]
apollo.splice_acceptor_sites = [ "AG"]
apollo.gff3.source= "."
apollo.bootstrap = false

apollo.info_editor = {
    feature_types = "default"
    attributes = true
    dbxrefs = true
    pubmed_ids = true
    go_ids = true
    comments = true
}


