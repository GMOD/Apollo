databaseChangeLog = {

    // Phase 1: Add missing columns to existing tables
    // These columns were added in intermediate Apollo releases via dbCreate=update
    // but never had Liquibase changelogs

    changeSet(author: "apollo7-migration", id: "7_0_0-1") {
        preConditions(onFail: "MARK_RAN") {
            not { columnExists(tableName: "organism", columnName: "public_mode") }
        }
        addColumn(tableName: "organism") {
            column(name: "public_mode", type: "boolean", defaultValueBoolean: true)
        }
    }

    changeSet(author: "apollo7-migration", id: "7_0_0-2") {
        preConditions(onFail: "MARK_RAN") {
            not { columnExists(tableName: "organism", columnName: "obsolete") }
        }
        addColumn(tableName: "organism") {
            column(name: "obsolete", type: "boolean", defaultValueBoolean: false)
        }
    }

    changeSet(author: "apollo7-migration", id: "7_0_0-3") {
        preConditions(onFail: "MARK_RAN") {
            not { columnExists(tableName: "organism", columnName: "metadata") }
        }
        addColumn(tableName: "organism") {
            column(name: "metadata", type: "clob")
        }
    }

    changeSet(author: "apollo7-migration", id: "7_0_0-4") {
        preConditions(onFail: "MARK_RAN") {
            not { columnExists(tableName: "organism", columnName: "genome_fasta") }
        }
        addColumn(tableName: "organism") {
            column(name: "genome_fasta", type: "varchar(255)")
        }
    }

    changeSet(author: "apollo7-migration", id: "7_0_0-5") {
        preConditions(onFail: "MARK_RAN") {
            not { columnExists(tableName: "organism", columnName: "genome_fasta_index") }
        }
        addColumn(tableName: "organism") {
            column(name: "genome_fasta_index", type: "varchar(255)")
        }
    }

    changeSet(author: "apollo7-migration", id: "7_0_0-6") {
        preConditions(onFail: "MARK_RAN") {
            not { columnExists(tableName: "organism", columnName: "non_default_translation_table") }
        }
        addColumn(tableName: "organism") {
            column(name: "non_default_translation_table", type: "varchar(255)")
        }
    }

    changeSet(author: "apollo7-migration", id: "7_0_0-7") {
        preConditions(onFail: "MARK_RAN") {
            not { columnExists(tableName: "organism", columnName: "data_added_via_web_services") }
        }
        addColumn(tableName: "organism") {
            column(name: "data_added_via_web_services", type: "boolean")
        }
    }

    changeSet(author: "apollo7-migration", id: "7_0_0-8") {
        preConditions(onFail: "MARK_RAN") {
            not { columnExists(tableName: "organism", columnName: "official_gene_set_track") }
        }
        addColumn(tableName: "organism") {
            column(name: "official_gene_set_track", type: "varchar(255)")
        }
    }

    changeSet(author: "apollo7-migration", id: "7_0_0-9") {
        preConditions(onFail: "MARK_RAN") {
            not { columnExists(tableName: "grails_user", columnName: "inactive") }
        }
        addColumn(tableName: "grails_user") {
            column(name: "inactive", type: "boolean", defaultValueBoolean: false)
        }
    }

    changeSet(author: "apollo7-migration", id: "7_0_0-10") {
        preConditions(onFail: "MARK_RAN") {
            not { columnExists(tableName: "grails_user", columnName: "metadata") }
        }
        addColumn(tableName: "grails_user") {
            column(name: "metadata", type: "varchar(255)")
        }
    }

    changeSet(author: "apollo7-migration", id: "7_0_0-11") {
        preConditions(onFail: "MARK_RAN") {
            not { columnExists(tableName: "feature", columnName: "fmin") }
        }
        addColumn(tableName: "feature") {
            column(name: "fmin", type: "bigint")
            column(name: "fmax", type: "bigint")
            column(name: "reference_allele_id", type: "bigint")
        }
    }

    // Phase 2: Create new tables

    changeSet(author: "apollo7-migration", id: "7_0_0-20") {
        preConditions(onFail: "MARK_RAN") {
            not { tableExists(tableName: "suggested_name") }
        }
        createTable(tableName: "suggested_name") {
            column(autoIncrement: true, name: "id", type: "bigint") {
                constraints(nullable: false, primaryKey: true)
            }
            column(name: "version", type: "bigint") { constraints(nullable: false) }
            column(name: "name", type: "varchar(255)") { constraints(nullable: false) }
            column(name: "metadata", type: "varchar(255)")
        }
    }

    changeSet(author: "apollo7-migration", id: "7_0_0-21") {
        preConditions(onFail: "MARK_RAN") {
            not { tableExists(tableName: "suggested_name_feature_type") }
        }
        createTable(tableName: "suggested_name_feature_type") {
            column(name: "suggested_name_feature_types_id", type: "bigint") { constraints(nullable: false) }
            column(name: "feature_type_id", type: "bigint")
        }
    }

    changeSet(author: "apollo7-migration", id: "7_0_0-22") {
        preConditions(onFail: "MARK_RAN") {
            not { tableExists(tableName: "canned_key") }
        }
        createTable(tableName: "canned_key") {
            column(autoIncrement: true, name: "id", type: "bigint") {
                constraints(nullable: false, primaryKey: true)
            }
            column(name: "version", type: "bigint") { constraints(nullable: false) }
            column(name: "label", type: "varchar(255)") { constraints(nullable: false) }
            column(name: "metadata", type: "varchar(255)")
        }
    }

    changeSet(author: "apollo7-migration", id: "7_0_0-23") {
        preConditions(onFail: "MARK_RAN") {
            not { tableExists(tableName: "canned_key_feature_type") }
        }
        createTable(tableName: "canned_key_feature_type") {
            column(name: "canned_key_feature_types_id", type: "bigint") { constraints(nullable: false) }
            column(name: "feature_type_id", type: "bigint")
        }
    }

    changeSet(author: "apollo7-migration", id: "7_0_0-24") {
        preConditions(onFail: "MARK_RAN") {
            not { tableExists(tableName: "canned_value") }
        }
        createTable(tableName: "canned_value") {
            column(autoIncrement: true, name: "id", type: "bigint") {
                constraints(nullable: false, primaryKey: true)
            }
            column(name: "version", type: "bigint") { constraints(nullable: false) }
            column(name: "label", type: "varchar(255)") { constraints(nullable: false) }
            column(name: "metadata", type: "varchar(255)")
        }
    }

    changeSet(author: "apollo7-migration", id: "7_0_0-25") {
        preConditions(onFail: "MARK_RAN") {
            not { tableExists(tableName: "canned_value_feature_type") }
        }
        createTable(tableName: "canned_value_feature_type") {
            column(name: "canned_value_feature_types_id", type: "bigint") { constraints(nullable: false) }
            column(name: "feature_type_id", type: "bigint")
        }
    }

    changeSet(author: "apollo7-migration", id: "7_0_0-26") {
        preConditions(onFail: "MARK_RAN") {
            not { tableExists(tableName: "gene_product_name") }
        }
        createTable(tableName: "gene_product_name") {
            column(autoIncrement: true, name: "id", type: "bigint") {
                constraints(nullable: false, primaryKey: true)
            }
            column(name: "version", type: "bigint") { constraints(nullable: false) }
            column(name: "name", type: "varchar(255)") { constraints(nullable: false) }
            column(name: "metadata", type: "varchar(255)")
        }
    }

    changeSet(author: "apollo7-migration", id: "7_0_0-27") {
        preConditions(onFail: "MARK_RAN") {
            not { tableExists(tableName: "gene_product_name_feature_type") }
        }
        createTable(tableName: "gene_product_name_feature_type") {
            column(name: "gene_product_name_feature_types_id", type: "bigint") { constraints(nullable: false) }
            column(name: "feature_type_id", type: "bigint")
        }
    }

    changeSet(author: "apollo7-migration", id: "7_0_0-28") {
        preConditions(onFail: "MARK_RAN") {
            not { tableExists(tableName: "available_status_feature_type") }
        }
        createTable(tableName: "available_status_feature_type") {
            column(name: "available_status_feature_types_id", type: "bigint") { constraints(nullable: false) }
            column(name: "feature_type_id", type: "bigint")
        }
    }

    changeSet(author: "apollo7-migration", id: "7_0_0-30") {
        preConditions(onFail: "MARK_RAN") {
            not { tableExists(tableName: "go_annotation") }
        }
        createTable(tableName: "go_annotation") {
            column(autoIncrement: true, name: "id", type: "bigint") {
                constraints(nullable: false, primaryKey: true)
            }
            column(name: "version", type: "bigint") { constraints(nullable: false) }
            column(name: "aspect", type: "varchar(255)") { constraints(nullable: false) }
            column(name: "feature_id", type: "bigint") { constraints(nullable: false) }
            column(name: "go_ref", type: "varchar(255)") { constraints(nullable: false) }
            column(name: "evidence_ref", type: "varchar(255)") { constraints(nullable: false) }
            column(name: "go_ref_label", type: "varchar(255)")
            column(name: "evidence_ref_label", type: "varchar(255)")
            column(name: "gene_product_relationship_ref", type: "varchar(255)")
            column(name: "negate", type: "boolean") { constraints(nullable: false) }
            column(name: "with_or_from_array", type: "varchar(255)")
            column(name: "notes_array", type: "varchar(255)")
            column(name: "reference", type: "varchar(255)") { constraints(nullable: false) }
            column(name: "last_updated", type: "timestamp") { constraints(nullable: false) }
            column(name: "date_created", type: "timestamp") { constraints(nullable: false) }
        }
    }

    changeSet(author: "apollo7-migration", id: "7_0_0-31") {
        preConditions(onFail: "MARK_RAN") {
            not { tableExists(tableName: "go_annotation_grails_user") }
        }
        createTable(tableName: "go_annotation_grails_user") {
            column(name: "go_annotation_owners_id", type: "bigint") { constraints(nullable: false) }
            column(name: "user_id", type: "bigint")
        }
    }

    changeSet(author: "apollo7-migration", id: "7_0_0-32") {
        preConditions(onFail: "MARK_RAN") {
            not { tableExists(tableName: "gene_product") }
        }
        createTable(tableName: "gene_product") {
            column(autoIncrement: true, name: "id", type: "bigint") {
                constraints(nullable: false, primaryKey: true)
            }
            column(name: "version", type: "bigint") { constraints(nullable: false) }
            column(name: "feature_id", type: "bigint") { constraints(nullable: false) }
            column(name: "product_name", type: "varchar(255)") { constraints(nullable: false) }
            column(name: "reference", type: "varchar(255)") { constraints(nullable: false) }
            column(name: "last_updated", type: "timestamp") { constraints(nullable: false) }
            column(name: "date_created", type: "timestamp") { constraints(nullable: false) }
            column(name: "evidence_ref", type: "varchar(255)") { constraints(nullable: false) }
            column(name: "evidence_ref_label", type: "varchar(255)")
            column(name: "notes_array", type: "varchar(255)")
            column(name: "with_or_from_array", type: "varchar(255)")
            column(name: "alternate", type: "boolean") { constraints(nullable: false) }
        }
    }

    changeSet(author: "apollo7-migration", id: "7_0_0-33") {
        preConditions(onFail: "MARK_RAN") {
            not { tableExists(tableName: "gene_product_grails_user") }
        }
        createTable(tableName: "gene_product_grails_user") {
            column(name: "gene_product_owners_id", type: "bigint") { constraints(nullable: false) }
            column(name: "user_id", type: "bigint")
        }
    }

    changeSet(author: "apollo7-migration", id: "7_0_0-34") {
        preConditions(onFail: "MARK_RAN") {
            not { tableExists(tableName: "provenance") }
        }
        createTable(tableName: "provenance") {
            column(autoIncrement: true, name: "id", type: "bigint") {
                constraints(nullable: false, primaryKey: true)
            }
            column(name: "version", type: "bigint") { constraints(nullable: false) }
            column(name: "feature_id", type: "bigint") { constraints(nullable: false) }
            column(name: "field", type: "varchar(255)") { constraints(nullable: false) }
            column(name: "reference", type: "varchar(255)") { constraints(nullable: false) }
            column(name: "last_updated", type: "timestamp") { constraints(nullable: false) }
            column(name: "date_created", type: "timestamp") { constraints(nullable: false) }
            column(name: "evidence_ref", type: "varchar(255)") { constraints(nullable: false) }
            column(name: "evidence_ref_label", type: "varchar(255)")
            column(name: "notes_array", type: "varchar(255)")
            column(name: "with_or_from_array", type: "varchar(255)")
        }
    }

    changeSet(author: "apollo7-migration", id: "7_0_0-35") {
        preConditions(onFail: "MARK_RAN") {
            not { tableExists(tableName: "provenance_grails_user") }
        }
        createTable(tableName: "provenance_grails_user") {
            column(name: "provenance_owners_id", type: "bigint") { constraints(nullable: false) }
            column(name: "user_id", type: "bigint")
        }
    }

    changeSet(author: "apollo7-migration", id: "7_0_0-36") {
        preConditions(onFail: "MARK_RAN") {
            not { tableExists(tableName: "allele") }
        }
        createTable(tableName: "allele") {
            column(autoIncrement: true, name: "id", type: "bigint") {
                constraints(nullable: false, primaryKey: true)
            }
            column(name: "version", type: "bigint") { constraints(nullable: false) }
            column(name: "bases", type: "varchar(255)") { constraints(nullable: false) }
            column(name: "variant_id", type: "bigint")
            column(name: "reference", type: "boolean") { constraints(nullable: false) }
        }
    }

    changeSet(author: "apollo7-migration", id: "7_0_0-37") {
        preConditions(onFail: "MARK_RAN") {
            not { tableExists(tableName: "allele_info") }
        }
        createTable(tableName: "allele_info") {
            column(autoIncrement: true, name: "id", type: "bigint") {
                constraints(nullable: false, primaryKey: true)
            }
            column(name: "version", type: "bigint") { constraints(nullable: false) }
            column(name: "tag", type: "varchar(255)") { constraints(nullable: false) }
            column(name: "value", type: "varchar(255)") { constraints(nullable: false) }
            column(name: "allele_id", type: "bigint") { constraints(nullable: false) }
        }
    }

    changeSet(author: "apollo7-migration", id: "7_0_0-38") {
        preConditions(onFail: "MARK_RAN") {
            not { tableExists(tableName: "variant_info") }
        }
        createTable(tableName: "variant_info") {
            column(autoIncrement: true, name: "id", type: "bigint") {
                constraints(nullable: false, primaryKey: true)
            }
            column(name: "version", type: "bigint") { constraints(nullable: false) }
            column(name: "tag", type: "varchar(255)") { constraints(nullable: false) }
            column(name: "value", type: "varchar(255)")
            column(name: "variant_id", type: "bigint") { constraints(nullable: false) }
        }
    }

    changeSet(author: "apollo7-migration", id: "7_0_0-40") {
        preConditions(onFail: "MARK_RAN") {
            not { tableExists(tableName: "organism_filter") }
        }
        createTable(tableName: "organism_filter") {
            column(autoIncrement: true, name: "id", type: "bigint") {
                constraints(nullable: false, primaryKey: true)
            }
            column(name: "version", type: "bigint") { constraints(nullable: false) }
            column(name: "organism_id", type: "bigint") { constraints(nullable: false) }
            column(name: "class", type: "varchar(255)") { constraints(nullable: false) }
            column(name: "canned_comment_id", type: "bigint")
            column(name: "canned_value_id", type: "bigint")
            column(name: "suggested_name_id", type: "bigint")
            column(name: "gene_product_name_id", type: "bigint")
            column(name: "available_status_id", type: "bigint")
            column(name: "canned_key_id", type: "bigint")
        }
    }

    changeSet(author: "apollo7-migration", id: "7_0_0-41") {
        preConditions(onFail: "MARK_RAN") {
            not { tableExists(tableName: "proxy") }
        }
        createTable(tableName: "proxy") {
            column(autoIncrement: true, name: "id", type: "bigint") {
                constraints(nullable: false, primaryKey: true)
            }
            column(name: "version", type: "bigint") { constraints(nullable: false) }
            column(name: "reference_url", type: "varchar(255)") { constraints(nullable: false) }
            column(name: "target_url", type: "varchar(255)") { constraints(nullable: false) }
            column(name: "active", type: "boolean") { constraints(nullable: false) }
            column(name: "fallback_order", type: "integer")
            column(name: "last_success", type: "timestamp")
            column(name: "last_fail", type: "timestamp")
        }
    }

    changeSet(author: "apollo7-migration", id: "7_0_0-42") {
        preConditions(onFail: "MARK_RAN") {
            not { tableExists(tableName: "sequence_cache") }
        }
        createTable(tableName: "sequence_cache") {
            column(autoIncrement: true, name: "id", type: "bigint") {
                constraints(nullable: false, primaryKey: true)
            }
            column(name: "version", type: "bigint") { constraints(nullable: false) }
            column(name: "sequence_name", type: "clob") { constraints(nullable: false) }
            column(name: "organism_name", type: "varchar(255)") { constraints(nullable: false) }
            column(name: "type", type: "varchar(255)")
            column(name: "fmin", type: "bigint")
            column(name: "fmax", type: "bigint")
            column(name: "feature_name", type: "varchar(255)")
            column(name: "param_map", type: "clob")
            column(name: "response", type: "clob") { constraints(nullable: false) }
        }
    }

    changeSet(author: "apollo7-migration", id: "7_0_0-43") {
        preConditions(onFail: "MARK_RAN") {
            not { tableExists(tableName: "track_cache") }
        }
        createTable(tableName: "track_cache") {
            column(autoIncrement: true, name: "id", type: "bigint") {
                constraints(nullable: false, primaryKey: true)
            }
            column(name: "version", type: "bigint") { constraints(nullable: false) }
            column(name: "track_name", type: "clob") { constraints(nullable: false) }
            column(name: "sequence_name", type: "clob") { constraints(nullable: false) }
            column(name: "organism_name", type: "varchar(255)") { constraints(nullable: false) }
            column(name: "type", type: "varchar(255)")
            column(name: "fmin", type: "bigint")
            column(name: "fmax", type: "bigint")
            column(name: "feature_name", type: "varchar(255)")
            column(name: "param_map", type: "clob")
            column(name: "response", type: "clob") { constraints(nullable: false) }
        }
    }

    changeSet(author: "apollo7-migration", id: "7_0_0-44") {
        preConditions(onFail: "MARK_RAN") {
            not { tableExists(tableName: "server_data") }
        }
        createTable(tableName: "server_data") {
            column(autoIncrement: true, name: "id", type: "bigint") {
                constraints(nullable: false, primaryKey: true)
            }
            column(name: "version", type: "bigint") { constraints(nullable: false) }
            column(name: "name", type: "varchar(255)") { constraints(nullable: false, unique: true) }
            column(name: "date_created", type: "timestamp") { constraints(nullable: false) }
            column(name: "last_updated", type: "timestamp") { constraints(nullable: false) }
        }
    }

    changeSet(author: "apollo7-migration", id: "7_0_0-45") {
        preConditions(onFail: "MARK_RAN") {
            not { tableExists(tableName: "user_group_admin") }
        }
        createTable(tableName: "user_group_admin") {
            column(name: "user_group_id", type: "bigint") { constraints(nullable: false) }
            column(name: "user_id", type: "bigint") { constraints(nullable: false) }
        }
        addPrimaryKey(tableName: "user_group_admin", columnNames: "user_group_id, user_id")
    }

    // Phase 3: Add missing columns for role.rank and user_group.metadata
    // (from changelog-2_0_9 which may not have run on all databases)

    changeSet(author: "apollo7-migration", id: "7_0_0-50") {
        preConditions(onFail: "MARK_RAN") {
            not { columnExists(tableName: "role", columnName: "rank") }
        }
        addColumn(tableName: "role") {
            column(name: "rank", type: "bigint")
        }
    }

    changeSet(author: "apollo7-migration", id: "7_0_0-51") {
        preConditions(onFail: "MARK_RAN") {
            not { columnExists(tableName: "user_group", columnName: "metadata") }
        }
        addColumn(tableName: "user_group") {
            column(name: "metadata", type: "clob")
        }
    }
}
