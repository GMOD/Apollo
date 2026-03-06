databaseChangeLog = {

    changeSet(author: "nathandunn (generated)", id: "1445460972540-1") {
        modifyDataType(columnName: "is_transcript", newDataType: "boolean", tableName: "custom_domain_mapping")
    }

    changeSet(author: "nathandunn (generated)", id: "1445460972540-2") {
        modifyDataType(columnName: "export_source_genomic_sequence", newDataType: "boolean", tableName: "data_adapter")
    }

    changeSet(author: "nathandunn (generated)", id: "1445460972540-3") {
        modifyDataType(columnName: "description", newDataType: "text", tableName: "feature")
    }

    changeSet(author: "nathandunn (generated)", id: "1445460972540-4") {
        modifyDataType(columnName: "is_analysis", newDataType: "boolean", tableName: "feature")
    }

    changeSet(author: "nathandunn (generated)", id: "1445460972540-5") {
        modifyDataType(columnName: "is_obsolete", newDataType: "boolean", tableName: "feature")
    }

    changeSet(author: "nathandunn (generated)", id: "1445460972540-6") {
        modifyDataType(columnName: "name", newDataType: "text", tableName: "feature")
    }

    changeSet(author: "nathandunn (generated)", id: "1445460972540-7") {
        modifyDataType(columnName: "current", newDataType: "boolean", tableName: "feature_event")
    }

    changeSet(author: "nathandunn (generated)", id: "1445460972540-8") {
        modifyDataType(columnName: "name", newDataType: "text", tableName: "feature_event")
    }

    changeSet(author: "nathandunn (generated)", id: "1445460972540-9") {
        modifyDataType(columnName: "is_fmax_partial", newDataType: "boolean", tableName: "feature_location")
    }

    changeSet(author: "nathandunn (generated)", id: "1445460972540-10") {
        modifyDataType(columnName: "is_fmin_partial", newDataType: "boolean", tableName: "feature_location")
    }

    changeSet(author: "nathandunn (generated)", id: "1445460972540-11") {
        modifyDataType(columnName: "is_current", newDataType: "boolean", tableName: "feature_synonym")
    }

    changeSet(author: "nathandunn (generated)", id: "1445460972540-12") {
        modifyDataType(columnName: "is_internal", newDataType: "boolean", tableName: "feature_synonym")
    }

    changeSet(author: "nathandunn (generated)", id: "1445460972540-13") {
        modifyDataType(columnName: "is_not", newDataType: "boolean", tableName: "featurecvterm")
    }

    changeSet(author: "nathandunn (generated)", id: "1445460972540-14") {
        modifyDataType(columnName: "public_mode", newDataType: "boolean", tableName: "organism")
    }

    changeSet(author: "nathandunn (generated)", id: "1445460972540-15") {
        modifyDataType(columnName: "valid", newDataType: "boolean", tableName: "organism")
    }

    changeSet(author: "nathandunn (generated)", id: "1445460972540-16") {
        modifyDataType(columnName: "current_organism", newDataType: "boolean", tableName: "preference")
    }

    changeSet(author: "nathandunn (generated)", id: "1445460972540-17") {
        modifyDataType(columnName: "native_track_list", newDataType: "boolean", tableName: "preference")
    }

    changeSet(author: "nathandunn (generated)", id: "1445460972540-18") {
        modifyDataType(columnName: "active", newDataType: "boolean", tableName: "proxy")
    }

    changeSet(author: "nathandunn (generated)", id: "1445460972540-19") {
        modifyDataType(columnName: "is_obsolete", newDataType: "boolean", tableName: "publication")
    }

    changeSet(author: "nathandunn (generated)", id: "1445460972540-20") {
        modifyDataType(columnName: "editor", newDataType: "boolean", tableName: "publication_author")
    }

    changeSet(author: "nathandunn (generated)", id: "1445460972540-21") {
        modifyDataType(columnName: "is_current", newDataType: "boolean", tableName: "publicationdbxref")
    }

    changeSet(author: "nathandunn (generated)", id: "1445460972540-22") {
        modifyDataType(columnName: "remove_temp_directory", newDataType: "boolean", tableName: "search_tool")
    }

    changeSet(author: "nathandunn (generated)", id: "1445460972540-23") {
        modifyDataType(columnName: "public_group", newDataType: "boolean", tableName: "user_group")
    }

    changeSet(author: "nathandunn (generated)", id: "1445460972540-27") {
        preConditions(onFail: 'MARK_RAN', onError: "HALT") {
            and {
//                tableExists(tableName: "sequence",schemaName:"public")
//                columnExists(columnName: "seq_chunk_prefix", tableName: "sequence",schemaName:"public")
                tableExists(tableName: "sequence")
                columnExists(columnName: "seq_chunk_prefix", tableName: "sequence")
//                columnExists(columnName: "splice_acceptor", tableName: "sequence")
//                columnExists(columnName: "splice_donor", tableName: "sequence")
//                columnExists(columnName: "translation_table_location", tableName: "sequence")
//                columnExists(columnName: "ref_seq_file", tableName: "sequence")
//                columnExists(columnName: "sequence_directory", tableName: "sequence")
            }
        }
        dropColumn(columnName: "ref_seq_file", tableName: "sequence")
        dropColumn(columnName: "splice_acceptor", tableName: "sequence")
        dropColumn(columnName: "splice_donor_site", tableName: "sequence")
        dropColumn(columnName: "translation_table_location", tableName: "sequence")
        dropColumn(columnName: "seq_chunk_prefix", tableName: "sequence")
        dropColumn(columnName: "sequence_directory", tableName: "sequence")
    }

}
