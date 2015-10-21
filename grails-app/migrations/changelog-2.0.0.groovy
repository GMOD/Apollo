databaseChangeLog = {

	changeSet(author: "nathandunn (generated)", id: "1445456090131-1") {
		createTable(tableName: "analysis") {
			column(autoIncrement: "true", name: "id", type: "bigint") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "analysisPK")
			}

			column(name: "version", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "algorithm", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "description", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "name", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "program", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "program_version", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "source_name", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "sourceuri", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "source_version", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "time_executed", type: "timestamp") {
				constraints(nullable: "false")
			}
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-2") {
		createTable(tableName: "analysis_feature") {
			column(autoIncrement: "true", name: "id", type: "bigint") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "analysis_featPK")
			}

			column(name: "version", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "analysis_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "feature_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "identity", type: "double") {
				constraints(nullable: "false")
			}

			column(name: "normalized_score", type: "double") {
				constraints(nullable: "false")
			}

			column(name: "raw_score", type: "double") {
				constraints(nullable: "false")
			}

			column(name: "significance", type: "double") {
				constraints(nullable: "false")
			}
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-3") {
		createTable(tableName: "analysis_property") {
			column(autoIncrement: "true", name: "id", type: "bigint") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "analysis_propPK")
			}

			column(name: "version", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "analysis_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "type_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "value", type: "varchar(255)") {
				constraints(nullable: "false")
			}
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-4") {
		createTable(tableName: "audit_log") {
			column(autoIncrement: "true", name: "id", type: "bigint") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "audit_logPK")
			}

			column(name: "actor", type: "varchar(255)")

			column(name: "class_name", type: "varchar(255)")

			column(name: "date_created", type: "timestamp") {
				constraints(nullable: "false")
			}

			column(name: "event_name", type: "varchar(255)")

			column(name: "last_updated", type: "timestamp") {
				constraints(nullable: "false")
			}

			column(name: "new_value", type: "varchar(255)")

			column(name: "old_value", type: "varchar(255)")

			column(name: "persisted_object_id", type: "varchar(255)")

			column(name: "persisted_object_version", type: "bigint")

			column(name: "property_name", type: "varchar(255)")

			column(name: "uri", type: "varchar(255)")
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-5") {
		createTable(tableName: "available_status") {
			column(autoIncrement: "true", name: "id", type: "bigint") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "available_staPK")
			}

			column(name: "version", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "value", type: "varchar(255)") {
				constraints(nullable: "false")
			}
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-6") {
		createTable(tableName: "canned_comment") {
			column(autoIncrement: "true", name: "id", type: "bigint") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "canned_commenPK")
			}

			column(name: "version", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "comment", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "metadata", type: "varchar(255)")
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-7") {
		createTable(tableName: "canned_comment_feature_type") {
			column(name: "canned_comment_feature_types_id", type: "bigint")

			column(name: "feature_type_id", type: "bigint")
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-8") {
		createTable(tableName: "custom_domain_mapping") {
			column(autoIncrement: "true", name: "id", type: "bigint") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "custom_domainPK")
			}

			column(name: "version", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "alternate_cv_term", type: "varchar(255)")

			column(name: "cv_term", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "is_transcript", type: "boolean") {
				constraints(nullable: "false")
			}

			column(name: "ontology_id", type: "varchar(255)") {
				constraints(nullable: "false")
			}
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-9") {
		createTable(tableName: "cv") {
			column(autoIncrement: "true", name: "id", type: "bigint") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "cvPK")
			}

			column(name: "version", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "definition", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "name", type: "varchar(255)") {
				constraints(nullable: "false")
			}
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-10") {
		createTable(tableName: "cvterm") {
			column(autoIncrement: "true", name: "id", type: "bigint") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "cvtermPK")
			}

			column(name: "version", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "cv_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "dbxref_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "definition", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "is_obsolete", type: "integer") {
				constraints(nullable: "false")
			}

			column(name: "is_relationship_type", type: "integer") {
				constraints(nullable: "false")
			}

			column(name: "name", type: "varchar(255)") {
				constraints(nullable: "false")
			}
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-11") {
		createTable(tableName: "cvterm_path") {
			column(autoIncrement: "true", name: "id", type: "bigint") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "cvterm_pathPK")
			}

			column(name: "version", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "cv_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "cvterm_path_id", type: "integer") {
				constraints(nullable: "false")
			}

			column(name: "objectcvterm_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "path_distance", type: "integer") {
				constraints(nullable: "false")
			}

			column(name: "subjectcvterm_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "type_id", type: "bigint") {
				constraints(nullable: "false")
			}
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-12") {
		createTable(tableName: "cvterm_relationship") {
			column(autoIncrement: "true", name: "id", type: "bigint") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "cvterm_relatiPK")
			}

			column(name: "version", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "cvterm_relationship_id", type: "integer") {
				constraints(nullable: "false")
			}

			column(name: "objectcvterm_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "subjectcvterm_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "type_id", type: "bigint") {
				constraints(nullable: "false")
			}
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-13") {
		createTable(tableName: "data_adapter") {
			column(autoIncrement: "true", name: "id", type: "bigint") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "data_adapterPK")
			}

			column(name: "version", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "export_source_genomic_sequence", type: "boolean")

			column(name: "feature_type_string", type: "varchar(255)")

			column(name: "implementation_class", type: "varchar(255)")

			column(name: "data_adapter_key", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "options", type: "varchar(255)")

			column(name: "permission", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "source", type: "varchar(255)")

			column(name: "temp_directory", type: "varchar(255)")
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-14") {
		createTable(tableName: "data_adapter_data_adapter") {
			column(name: "data_adapter_data_adapters_id", type: "bigint")

			column(name: "data_adapter_id", type: "bigint")
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-15") {
		createTable(tableName: "db") {
			column(autoIncrement: "true", name: "id", type: "bigint") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "dbPK")
			}

			column(name: "version", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "description", type: "varchar(255)")

			column(name: "name", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "url", type: "varchar(255)")

			column(name: "url_prefix", type: "varchar(255)")
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-16") {
		createTable(tableName: "dbxref") {
			column(autoIncrement: "true", name: "id", type: "bigint") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "dbxrefPK")
			}

			column(name: "version", type: "varchar(255)")

			column(name: "accession", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "db_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "description", type: "varchar(255)")
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-17") {
		createTable(tableName: "dbxref_property") {
			column(autoIncrement: "true", name: "id", type: "bigint") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "dbxref_properPK")
			}

			column(name: "version", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "dbxref_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "dbxref_property_id", type: "integer") {
				constraints(nullable: "false")
			}

			column(name: "rank", type: "integer") {
				constraints(nullable: "false")
			}

			column(name: "type_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "value", type: "varchar(255)") {
				constraints(nullable: "false")
			}
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-18") {
		createTable(tableName: "environment") {
			column(autoIncrement: "true", name: "id", type: "bigint") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "environmentPK")
			}

			column(name: "version", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "description", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "environment_id", type: "integer") {
				constraints(nullable: "false")
			}

			column(name: "uniquename", type: "varchar(255)") {
				constraints(nullable: "false")
			}
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-19") {
		createTable(tableName: "environmentcvterm") {
			column(autoIncrement: "true", name: "id", type: "bigint") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "environmentcvPK")
			}

			column(name: "version", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "cvterm_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "environment_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "environment_cvterm_id", type: "integer") {
				constraints(nullable: "false")
			}
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-20") {
		createTable(tableName: "feature") {
			column(autoIncrement: "true", name: "id", type: "bigint") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "featurePK")
			}

			column(name: "version", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "date_created", type: "timestamp")

			column(name: "dbxref_id", type: "bigint")

			column(name: "description", type: "varchar(255)")

			column(name: "is_analysis", type: "boolean") {
				constraints(nullable: "false")
			}

			column(name: "is_obsolete", type: "boolean") {
				constraints(nullable: "false")
			}

			column(name: "last_updated", type: "timestamp")

			column(name: "md5checksum", type: "varchar(255)")

			column(name: "name", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "sequence_length", type: "integer")

			column(name: "status_id", type: "bigint")

			column(name: "symbol", type: "varchar(255)")

			column(name: "unique_name", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "class", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "alteration_residue", type: "varchar(255)")

			column(name: "deletion_length", type: "integer")

			column(name: "alternate_cv_term", type: "varchar(255)")

			column(name: "class_name", type: "varchar(255)")

			column(name: "custom_alternate_cv_term", type: "varchar(255)")

			column(name: "custom_class_name", type: "varchar(255)")

			column(name: "custom_cv_term", type: "varchar(255)")

			column(name: "custom_ontology_id", type: "varchar(255)")

			column(name: "cv_term", type: "varchar(255)")

			column(name: "meta_data", type: "longvarchar")

			column(name: "ontology_id", type: "varchar(255)")

			column(name: "analysis_feature_id", type: "bigint")
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-21") {
		createTable(tableName: "feature_dbxref") {
			column(name: "feature_featuredbxrefs_id", type: "bigint")

			column(name: "dbxref_id", type: "bigint")
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-22") {
		createTable(tableName: "feature_event") {
			column(autoIncrement: "true", name: "id", type: "bigint") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "feature_eventPK")
			}

			column(name: "version", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "child_id", type: "bigint")

			column(name: "child_split_id", type: "bigint")

			column(name: "current", type: "boolean") {
				constraints(nullable: "false")
			}

			column(name: "date_created", type: "timestamp") {
				constraints(nullable: "false")
			}

			column(name: "editor_id", type: "bigint")

			column(name: "last_updated", type: "timestamp") {
				constraints(nullable: "false")
			}

			column(name: "name", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "new_features_json_array", type: "longvarchar")

			column(name: "old_features_json_array", type: "longvarchar")

			column(name: "operation", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "original_json_command", type: "longvarchar")

			column(name: "parent_id", type: "bigint")

			column(name: "parent_merge_id", type: "bigint")

			column(name: "unique_name", type: "varchar(255)") {
				constraints(nullable: "false")
			}
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-23") {
		createTable(tableName: "feature_feature_phenotypes") {
			column(name: "feature_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "phenotype_id", type: "bigint") {
				constraints(nullable: "false")
			}
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-24") {
		createTable(tableName: "feature_genotype") {
			column(autoIncrement: "true", name: "id", type: "bigint") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "feature_genotPK")
			}

			column(name: "version", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "cgroup", type: "integer") {
				constraints(nullable: "false")
			}

			column(name: "chromosome_feature_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "cvterm_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "feature_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "feature_genotype_id", type: "integer") {
				constraints(nullable: "false")
			}

			column(name: "genotype_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "rank", type: "integer") {
				constraints(nullable: "false")
			}
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-25") {
		createTable(tableName: "feature_grails_user") {
			column(name: "feature_owners_id", type: "bigint")

			column(name: "user_id", type: "bigint")
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-26") {
		createTable(tableName: "feature_location") {
			column(autoIncrement: "true", name: "id", type: "bigint") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "feature_locatPK")
			}

			column(name: "version", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "feature_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "fmax", type: "integer") {
				constraints(nullable: "false")
			}

			column(name: "fmin", type: "integer") {
				constraints(nullable: "false")
			}

			column(name: "is_fmax_partial", type: "boolean") {
				constraints(nullable: "false")
			}

			column(name: "is_fmin_partial", type: "boolean") {
				constraints(nullable: "false")
			}

			column(name: "locgroup", type: "integer") {
				constraints(nullable: "false")
			}

			column(name: "phase", type: "integer")

			column(name: "rank", type: "integer") {
				constraints(nullable: "false")
			}

			column(name: "residue_info", type: "varchar(255)")

			column(name: "sequence_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "strand", type: "integer")
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-27") {
		createTable(tableName: "feature_location_publication") {
			column(name: "feature_location_feature_location_publications_id", type: "bigint")

			column(name: "publication_id", type: "bigint")
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-28") {
		createTable(tableName: "feature_property") {
			column(autoIncrement: "true", name: "id", type: "bigint") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "feature_propePK")
			}

			column(name: "version", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "feature_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "rank", type: "integer") {
				constraints(nullable: "false")
			}

			column(name: "tag", type: "varchar(255)")

			column(name: "type_id", type: "bigint")

			column(name: "value", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "class", type: "varchar(255)") {
				constraints(nullable: "false")
			}
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-29") {
		createTable(tableName: "feature_property_publication") {
			column(name: "feature_property_feature_property_publications_id", type: "bigint")

			column(name: "publication_id", type: "bigint")
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-30") {
		createTable(tableName: "feature_publication") {
			column(name: "feature_feature_publications_id", type: "bigint")

			column(name: "publication_id", type: "bigint")
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-31") {
		createTable(tableName: "feature_relationship") {
			column(autoIncrement: "true", name: "id", type: "bigint") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "feature_relatPK")
			}

			column(name: "version", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "child_feature_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "parent_feature_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "rank", type: "integer") {
				constraints(nullable: "false")
			}

			column(name: "value", type: "varchar(255)")
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-32") {
		createTable(tableName: "feature_relationship_feature_property") {
			column(name: "feature_relationship_feature_relationship_properties_id", type: "bigint")

			column(name: "feature_property_id", type: "bigint")
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-33") {
		createTable(tableName: "feature_relationship_publication") {
			column(name: "feature_relationship_feature_relationship_publications_id", type: "bigint")

			column(name: "publication_id", type: "bigint")
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-34") {
		createTable(tableName: "feature_synonym") {
			column(autoIncrement: "true", name: "id", type: "bigint") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "feature_synonPK")
			}

			column(name: "version", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "feature_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "is_current", type: "boolean") {
				constraints(nullable: "false")
			}

			column(name: "is_internal", type: "boolean") {
				constraints(nullable: "false")
			}

			column(name: "publication_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "synonym_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "feature_synonyms_id", type: "bigint")
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-35") {
		createTable(tableName: "feature_type") {
			column(autoIncrement: "true", name: "id", type: "bigint") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "feature_typePK")
			}

			column(name: "version", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "display", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "name", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "ontology_id", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "type", type: "varchar(255)") {
				constraints(nullable: "false")
			}
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-36") {
		createTable(tableName: "featurecvterm") {
			column(autoIncrement: "true", name: "id", type: "bigint") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "featurecvtermPK")
			}

			column(name: "version", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "cvterm_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "feature_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "is_not", type: "boolean") {
				constraints(nullable: "false")
			}

			column(name: "publication_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "rank", type: "integer") {
				constraints(nullable: "false")
			}
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-37") {
		createTable(tableName: "featurecvterm_dbxref") {
			column(name: "featurecvterm_featurecvtermdbxrefs_id", type: "bigint")

			column(name: "dbxref_id", type: "bigint")
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-38") {
		createTable(tableName: "featurecvterm_publication") {
			column(name: "featurecvterm_featurecvterm_publications_id", type: "bigint")

			column(name: "publication_id", type: "bigint")
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-39") {
		createTable(tableName: "genome") {
			column(autoIncrement: "true", name: "id", type: "bigint") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "genomePK")
			}

			column(name: "version", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "directory", type: "varchar(255)")

			column(name: "name", type: "varchar(255)") {
				constraints(nullable: "false")
			}
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-40") {
		createTable(tableName: "genotype") {
			column(autoIncrement: "true", name: "id", type: "bigint") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "genotypePK")
			}

			column(name: "version", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "description", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "genotype_id", type: "integer") {
				constraints(nullable: "false")
			}

			column(name: "name", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "unique_name", type: "varchar(255)") {
				constraints(nullable: "false")
			}
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-41") {
		createTable(tableName: "grails_user") {
			column(autoIncrement: "true", name: "id", type: "bigint") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "grails_userPK")
			}

			column(name: "version", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "first_name", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "last_name", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "password_hash", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "username", type: "varchar(255)") {
				constraints(nullable: "false")
			}
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-42") {
		createTable(tableName: "grails_user_roles") {
			column(name: "user_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "role_id", type: "bigint") {
				constraints(nullable: "false")
			}
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-43") {
		createTable(tableName: "operation") {
			column(autoIncrement: "true", name: "id", type: "bigint") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "operationPK")
			}

			column(name: "version", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "attributes", type: "longvarchar")

			column(name: "feature_unique_name", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "new_features", type: "longvarchar")

			column(name: "old_features", type: "longvarchar")

			column(name: "operation_type", type: "varchar(255)") {
				constraints(nullable: "false")
			}
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-44") {
		createTable(tableName: "organism") {
			column(autoIncrement: "true", name: "id", type: "bigint") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "organismPK")
			}

			column(name: "version", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "abbreviation", type: "varchar(255)")

			column(name: "blatdb", type: "varchar(255)")

			column(name: "comment", type: "varchar(255)")

			column(name: "common_name", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "directory", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "genus", type: "varchar(255)")

			column(name: "species", type: "varchar(255)")

			column(name: "valid", type: "boolean")
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-45") {
		createTable(tableName: "organism_organism_property") {
			column(name: "organism_organism_properties_id", type: "bigint")

			column(name: "organism_property_id", type: "bigint")
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-46") {
		createTable(tableName: "organism_property") {
			column(autoIncrement: "true", name: "id", type: "bigint") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "organism_propPK")
			}

			column(name: "version", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "abbreviation", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "comment", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "common_name", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "genus", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "organism_id", type: "integer") {
				constraints(nullable: "false")
			}

			column(name: "species", type: "varchar(255)") {
				constraints(nullable: "false")
			}
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-47") {
		createTable(tableName: "organism_property_organism_property") {
			column(name: "organism_property_organism_properties_id", type: "bigint")

			column(name: "organism_property_id", type: "bigint")
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-48") {
		createTable(tableName: "organism_property_organismdbxref") {
			column(name: "organism_property_organismdbxrefs_id", type: "bigint")

			column(name: "organismdbxref_id", type: "bigint")
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-49") {
		createTable(tableName: "organismdbxref") {
			column(autoIncrement: "true", name: "id", type: "bigint") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "organismdbxrePK")
			}

			column(name: "version", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "dbxref_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "organism_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "organism_dbxref_id", type: "integer") {
				constraints(nullable: "false")
			}
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-50") {
		createTable(tableName: "part_of") {
			column(autoIncrement: "true", name: "id", type: "bigint") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "part_ofPK")
			}

			column(name: "version", type: "bigint") {
				constraints(nullable: "false")
			}
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-51") {
		createTable(tableName: "permission") {
			column(autoIncrement: "true", name: "id", type: "bigint") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "permissionPK")
			}

			column(name: "version", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "organism_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "class", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "group_id", type: "bigint")

			column(name: "permissions", type: "varchar(255)")

			column(name: "track_visibilities", type: "varchar(255)")

			column(name: "user_id", type: "bigint")
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-52") {
		createTable(tableName: "phenotype") {
			column(autoIncrement: "true", name: "id", type: "bigint") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "phenotypePK")
			}

			column(name: "version", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "assay_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "attribute_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "cvalue_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "observable_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "unique_name", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "value", type: "varchar(255)") {
				constraints(nullable: "false")
			}
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-53") {
		createTable(tableName: "phenotype_cvterm") {
			column(name: "phenotype_phenotypecvterms_id", type: "bigint")

			column(name: "cvterm_id", type: "bigint")
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-54") {
		createTable(tableName: "phenotype_description") {
			column(autoIncrement: "true", name: "id", type: "bigint") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "phenotype_desPK")
			}

			column(name: "version", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "description", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "environment_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "genotype_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "phenotype_description_id", type: "integer") {
				constraints(nullable: "false")
			}

			column(name: "publication_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "type_id", type: "bigint") {
				constraints(nullable: "false")
			}
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-55") {
		createTable(tableName: "phenotype_statement") {
			column(autoIncrement: "true", name: "id", type: "bigint") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "phenotype_staPK")
			}

			column(name: "version", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "environment_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "genotype_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "phenotype_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "publication_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "type_id", type: "bigint") {
				constraints(nullable: "false")
			}
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-56") {
		createTable(tableName: "preference") {
			column(autoIncrement: "true", name: "id", type: "bigint") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "preferencePK")
			}

			column(name: "version", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "domain", type: "varchar(255)")

			column(name: "name", type: "varchar(255)")

			column(name: "preferences_string", type: "varchar(255)")

			column(name: "class", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "user_id", type: "bigint")

			column(name: "current_organism", type: "boolean")

			column(name: "endbp", type: "integer")

			column(name: "organism_id", type: "bigint")

			column(name: "sequence_id", type: "bigint")

			column(name: "startbp", type: "integer")
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-57") {
		createTable(tableName: "publication") {
			column(autoIncrement: "true", name: "id", type: "bigint") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "publicationPK")
			}

			column(name: "version", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "is_obsolete", type: "boolean") {
				constraints(nullable: "false")
			}

			column(name: "issue", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "mini_reference", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "pages", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "publication_id", type: "integer") {
				constraints(nullable: "false")
			}

			column(name: "publication_place", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "publication_year", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "publisher", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "series_name", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "title", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "type_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "unique_name", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "volume", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "volume_title", type: "varchar(255)") {
				constraints(nullable: "false")
			}
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-58") {
		createTable(tableName: "publication_author") {
			column(autoIncrement: "true", name: "id", type: "bigint") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "publication_aPK")
			}

			column(name: "version", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "editor", type: "boolean") {
				constraints(nullable: "false")
			}

			column(name: "given_names", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "publication_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "publication_author_id", type: "integer") {
				constraints(nullable: "false")
			}

			column(name: "rank", type: "integer") {
				constraints(nullable: "false")
			}

			column(name: "suffix", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "surname", type: "varchar(255)") {
				constraints(nullable: "false")
			}
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-59") {
		createTable(tableName: "publication_relationship") {
			column(autoIncrement: "true", name: "id", type: "bigint") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "publication_rPK")
			}

			column(name: "version", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "object_publication_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "publication_relationship_id", type: "integer") {
				constraints(nullable: "false")
			}

			column(name: "subject_publication_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "type_id", type: "bigint") {
				constraints(nullable: "false")
			}
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-60") {
		createTable(tableName: "publicationdbxref") {
			column(autoIncrement: "true", name: "id", type: "bigint") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "publicationdbPK")
			}

			column(name: "version", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "dbxref_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "is_current", type: "boolean") {
				constraints(nullable: "false")
			}

			column(name: "publication_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "publicationdbxref_id", type: "integer") {
				constraints(nullable: "false")
			}
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-61") {
		createTable(tableName: "role") {
			column(autoIncrement: "true", name: "id", type: "bigint") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "rolePK")
			}

			column(name: "version", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "name", type: "varchar(255)") {
				constraints(nullable: "false")
			}
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-62") {
		createTable(tableName: "role_permissions") {
			column(name: "role_id", type: "bigint")

			column(name: "permissions_string", type: "varchar(255)")
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-63") {
		createTable(tableName: "search_tool") {
			column(autoIncrement: "true", name: "id", type: "bigint") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "search_toolPK")
			}

			column(name: "version", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "binary_path", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "database_path", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "implementation_class", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "search_key", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "options", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "remove_temp_directory", type: "boolean") {
				constraints(nullable: "false")
			}

			column(name: "tmp_dir", type: "varchar(255)") {
				constraints(nullable: "false")
			}
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-64") {
		createTable(tableName: "sequence") {
			column(autoIncrement: "true", name: "id", type: "bigint") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "sequencePK")
			}

			column(name: "version", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "sequence_end", type: "integer") {
				constraints(nullable: "false")
			}

			column(name: "length", type: "integer") {
				constraints(nullable: "false")
			}

			column(name: "name", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "organism_id", type: "bigint")

			column(name: "ref_seq_file", type: "varchar(255)")

			column(name: "seq_chunk_prefix", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "seq_chunk_size", type: "integer") {
				constraints(nullable: "false")
			}

			column(name: "sequence_directory", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "splice_acceptor", type: "varchar(255)")

			column(name: "splice_donor_site", type: "varchar(255)")

			column(name: "sequence_start", type: "integer") {
				constraints(nullable: "false")
			}

			column(name: "translation_table_location", type: "varchar(255)")
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-65") {
		createTable(tableName: "sequence_chunk") {
			column(autoIncrement: "true", name: "id", type: "bigint") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "sequence_chunPK")
			}

			column(name: "version", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "chunk_number", type: "integer") {
				constraints(nullable: "false")
			}

			column(name: "residue", type: "longvarchar") {
				constraints(nullable: "false")
			}

			column(name: "sequence_id", type: "bigint") {
				constraints(nullable: "false")
			}
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-66") {
		createTable(tableName: "synonym") {
			column(autoIncrement: "true", name: "id", type: "bigint") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "synonymPK")
			}

			column(name: "version", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "name", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "synonymsgml", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "type_id", type: "bigint") {
				constraints(nullable: "false")
			}
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-67") {
		createTable(tableName: "user_group") {
			column(autoIncrement: "true", name: "id", type: "bigint") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "user_groupPK")
			}

			column(name: "version", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "name", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "public_group", type: "boolean") {
				constraints(nullable: "false")
			}
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-68") {
		createTable(tableName: "user_group_users") {
			column(name: "user_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "user_group_id", type: "bigint") {
				constraints(nullable: "false")
			}
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-69") {
		addPrimaryKey(columnNames: "feature_id, phenotype_id", tableName: "feature_feature_phenotypes")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-70") {
		addPrimaryKey(columnNames: "user_id, role_id", tableName: "grails_user_roles")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-71") {
		addPrimaryKey(columnNames: "user_group_id, user_id", tableName: "user_group_users")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-179") {
		createIndex(indexName: "UK_3mkho1p232es23vtaqp0obydb", tableName: "db", unique: "true") {
			column(name: "name")
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-180") {
		createIndex(indexName: "name_uniq_1445456090031", tableName: "db", unique: "true") {
			column(name: "name")
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-181") {
		createIndex(indexName: "UK_rdmcxcj6i53kfjmb9j811ta1m", tableName: "grails_user", unique: "true") {
			column(name: "username")
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-182") {
		createIndex(indexName: "username_uniq_1445456090054", tableName: "grails_user", unique: "true") {
			column(name: "username")
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-183") {
		createIndex(indexName: "UK_8sewwnpamngi6b1dwaa88askk", tableName: "role", unique: "true") {
			column(name: "name")
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-184") {
		createIndex(indexName: "name_uniq_1445456090064", tableName: "role", unique: "true") {
			column(name: "name")
		}
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-72") {
		addForeignKeyConstraint(baseColumnNames: "analysis_id", baseTableName: "analysis_feature", constraintName: "FK_8m30ycwh545b4aoxor9sbk1oq", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "analysis", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-73") {
		addForeignKeyConstraint(baseColumnNames: "feature_id", baseTableName: "analysis_feature", constraintName: "FK_l94xl424xp988f06gr2b3t5tw", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "feature", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-74") {
		addForeignKeyConstraint(baseColumnNames: "analysis_id", baseTableName: "analysis_property", constraintName: "FK_38g8n4bitmdwkrcs217uexrwx", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "analysis", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-75") {
		addForeignKeyConstraint(baseColumnNames: "type_id", baseTableName: "analysis_property", constraintName: "FK_9o7xs7saygim8y0sm4ostvpc1", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "cvterm", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-76") {
		addForeignKeyConstraint(baseColumnNames: "canned_comment_feature_types_id", baseTableName: "canned_comment_feature_type", constraintName: "FK_8l290fdei9m707s7ngn712sts", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "canned_comment", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-77") {
		addForeignKeyConstraint(baseColumnNames: "feature_type_id", baseTableName: "canned_comment_feature_type", constraintName: "FK_es9vpf57b7a14sv803xy64k8h", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "feature_type", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-78") {
		addForeignKeyConstraint(baseColumnNames: "cv_id", baseTableName: "cvterm", constraintName: "FK_oksfqluv12ktmut9s6o9jla7a", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "cv", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-79") {
		addForeignKeyConstraint(baseColumnNames: "dbxref_id", baseTableName: "cvterm", constraintName: "FK_6d097oy44230tuoo8lb8dkkcp", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "dbxref", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-80") {
		addForeignKeyConstraint(baseColumnNames: "cv_id", baseTableName: "cvterm_path", constraintName: "FK_ke2nrw91sxil8mv7osgv83pw1", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "cv", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-81") {
		addForeignKeyConstraint(baseColumnNames: "objectcvterm_id", baseTableName: "cvterm_path", constraintName: "FK_tlfh10092i00g6rlv589naqy5", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "cvterm", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-82") {
		addForeignKeyConstraint(baseColumnNames: "subjectcvterm_id", baseTableName: "cvterm_path", constraintName: "FK_nq02ir0qeydr5tj3071k9gl7b", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "cvterm", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-83") {
		addForeignKeyConstraint(baseColumnNames: "type_id", baseTableName: "cvterm_path", constraintName: "FK_jaqi1bk3t2c0m3pybmparp856", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "cvterm", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-84") {
		addForeignKeyConstraint(baseColumnNames: "objectcvterm_id", baseTableName: "cvterm_relationship", constraintName: "FK_r1o1rnfnsf7oipuv1h7h1fln7", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "cvterm", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-85") {
		addForeignKeyConstraint(baseColumnNames: "subjectcvterm_id", baseTableName: "cvterm_relationship", constraintName: "FK_pwxrfyx6rqu5krq4nj5wa3u4f", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "cvterm", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-86") {
		addForeignKeyConstraint(baseColumnNames: "type_id", baseTableName: "cvterm_relationship", constraintName: "FK_ob1d0vrfaix8b28j4tvilqnyv", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "cvterm", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-87") {
		addForeignKeyConstraint(baseColumnNames: "data_adapter_data_adapters_id", baseTableName: "data_adapter_data_adapter", constraintName: "FK_321276juoco9ijc32gxeo7mi9", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "data_adapter", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-88") {
		addForeignKeyConstraint(baseColumnNames: "data_adapter_id", baseTableName: "data_adapter_data_adapter", constraintName: "FK_c5a2cdstwj0ydibnu567urh7q", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "data_adapter", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-89") {
		addForeignKeyConstraint(baseColumnNames: "db_id", baseTableName: "dbxref", constraintName: "FK_np3tfcu9g867to3qux6raf9y8", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "db", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-90") {
		addForeignKeyConstraint(baseColumnNames: "dbxref_id", baseTableName: "dbxref_property", constraintName: "FK_3p1ssctww083s0tt65mmm64uo", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "dbxref", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-91") {
		addForeignKeyConstraint(baseColumnNames: "type_id", baseTableName: "dbxref_property", constraintName: "FK_t6ojbvugx8kou45oklsie3rt5", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "cvterm", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-92") {
		addForeignKeyConstraint(baseColumnNames: "cvterm_id", baseTableName: "environmentcvterm", constraintName: "FK_tql9djnqw1d7migfndoj3lrph", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "cvterm", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-93") {
		addForeignKeyConstraint(baseColumnNames: "environment_id", baseTableName: "environmentcvterm", constraintName: "FK_rrwb96jjqgtg077yv8pbim3jj", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "environment", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-94") {
		addForeignKeyConstraint(baseColumnNames: "analysis_feature_id", baseTableName: "feature", constraintName: "FK_7huaou2aj3ac3oa49c1e0nhlm", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "analysis_feature", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-95") {
		addForeignKeyConstraint(baseColumnNames: "dbxref_id", baseTableName: "feature", constraintName: "FK_hc4vrafs0ws7ugdkp0n6u3xdo", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "dbxref", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-96") {
		addForeignKeyConstraint(baseColumnNames: "status_id", baseTableName: "feature", constraintName: "FK_kfq8esgv3in8wxml2x36f2md", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "feature_property", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-97") {
		addForeignKeyConstraint(baseColumnNames: "dbxref_id", baseTableName: "feature_dbxref", constraintName: "FK_n6n7lheb1qkmlde8u6gvvjxne", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "dbxref", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-98") {
		addForeignKeyConstraint(baseColumnNames: "feature_featuredbxrefs_id", baseTableName: "feature_dbxref", constraintName: "FK_1mrfkxbb3n7fhjxcrkxappdn8", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "feature", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-99") {
		addForeignKeyConstraint(baseColumnNames: "editor_id", baseTableName: "feature_event", constraintName: "FK_35nc3xd2axx6fwyap4bjkt09u", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "grails_user", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-100") {
		addForeignKeyConstraint(baseColumnNames: "feature_id", baseTableName: "feature_feature_phenotypes", constraintName: "FK_aqr7eiyx6puju6elciwubbwmo", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "feature", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-101") {
		addForeignKeyConstraint(baseColumnNames: "phenotype_id", baseTableName: "feature_feature_phenotypes", constraintName: "FK_dy5g29heir5ic3d36okyuihho", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "phenotype", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-102") {
		addForeignKeyConstraint(baseColumnNames: "chromosome_feature_id", baseTableName: "feature_genotype", constraintName: "FK_hak8r429shmpho06rbyvwnmt0", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "feature", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-103") {
		addForeignKeyConstraint(baseColumnNames: "cvterm_id", baseTableName: "feature_genotype", constraintName: "FK_b42u9iq4kuqe5ay544do81n32", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "cvterm", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-104") {
		addForeignKeyConstraint(baseColumnNames: "feature_id", baseTableName: "feature_genotype", constraintName: "FK_cm3gqs38fa2lpllgoum8n4kgn", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "feature", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-105") {
		addForeignKeyConstraint(baseColumnNames: "genotype_id", baseTableName: "feature_genotype", constraintName: "FK_736wxgjs6pip5212ash5i68p", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "genotype", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-106") {
		addForeignKeyConstraint(baseColumnNames: "feature_owners_id", baseTableName: "feature_grails_user", constraintName: "FK_4dgbhgiw0vb9hqy2k5fqg3neh", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "feature", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-107") {
		addForeignKeyConstraint(baseColumnNames: "user_id", baseTableName: "feature_grails_user", constraintName: "FK_lflwbgxduee8ljjwe5rfbdil2", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "grails_user", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-108") {
		addForeignKeyConstraint(baseColumnNames: "feature_id", baseTableName: "feature_location", constraintName: "FK_qml7xp9f5uojcw7jwdxcb35le", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "feature", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-109") {
		addForeignKeyConstraint(baseColumnNames: "sequence_id", baseTableName: "feature_location", constraintName: "FK_dhnrehn3tj85m2j9c0m4md3f4", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "sequence", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-110") {
		addForeignKeyConstraint(baseColumnNames: "feature_location_feature_location_publications_id", baseTableName: "feature_location_publication", constraintName: "FK_jquf8fftudekrwgx1e870jy43", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "feature_location", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-111") {
		addForeignKeyConstraint(baseColumnNames: "publication_id", baseTableName: "feature_location_publication", constraintName: "FK_n4lr2f61atuxmm8cb90qtkojq", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "publication", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-112") {
		addForeignKeyConstraint(baseColumnNames: "feature_id", baseTableName: "feature_property", constraintName: "FK_jpvdxc57abfiridcr57x8130", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "feature", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-113") {
		addForeignKeyConstraint(baseColumnNames: "type_id", baseTableName: "feature_property", constraintName: "FK_36e638geg9tew42b1mp2ehff", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "cvterm", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-114") {
		addForeignKeyConstraint(baseColumnNames: "feature_property_feature_property_publications_id", baseTableName: "feature_property_publication", constraintName: "FK_86law9p6s1pbt02n3mltkcqwh", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "feature_property", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-115") {
		addForeignKeyConstraint(baseColumnNames: "publication_id", baseTableName: "feature_property_publication", constraintName: "FK_j4dnb11fi9vcvrdjo5m352pyq", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "publication", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-116") {
		addForeignKeyConstraint(baseColumnNames: "feature_feature_publications_id", baseTableName: "feature_publication", constraintName: "FK_qolh5l4blkx8vfmwcl7f3woan", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "feature", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-117") {
		addForeignKeyConstraint(baseColumnNames: "publication_id", baseTableName: "feature_publication", constraintName: "FK_580odgbjowisfshvk82rfjri2", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "publication", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-118") {
		addForeignKeyConstraint(baseColumnNames: "child_feature_id", baseTableName: "feature_relationship", constraintName: "FK_8jm56covt0m7m0m191bc5jseh", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "feature", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-119") {
		addForeignKeyConstraint(baseColumnNames: "parent_feature_id", baseTableName: "feature_relationship", constraintName: "FK_72kmd92rdc6gne0nrh026o1j0", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "feature", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-120") {
		addForeignKeyConstraint(baseColumnNames: "feature_property_id", baseTableName: "feature_relationship_feature_property", constraintName: "FK_scm5rx2kuhgkhdfvskyo924cy", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "feature_property", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-121") {
		addForeignKeyConstraint(baseColumnNames: "feature_relationship_feature_relationship_properties_id", baseTableName: "feature_relationship_feature_property", constraintName: "FK_ebgnfbogf1lwdxd8jc17511o7", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "feature_relationship", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-122") {
		addForeignKeyConstraint(baseColumnNames: "feature_relationship_feature_relationship_publications_id", baseTableName: "feature_relationship_publication", constraintName: "FK_bdd324e5jb0lpuhs7biy2kacm", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "feature_relationship", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-123") {
		addForeignKeyConstraint(baseColumnNames: "publication_id", baseTableName: "feature_relationship_publication", constraintName: "FK_4j4u29xis9bhr65slfaimgjye", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "publication", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-124") {
		addForeignKeyConstraint(baseColumnNames: "feature_id", baseTableName: "feature_synonym", constraintName: "FK_nf9qbuay984ixqd2k1425rnyo", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "feature", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-125") {
		addForeignKeyConstraint(baseColumnNames: "feature_synonyms_id", baseTableName: "feature_synonym", constraintName: "FK_gsol4u8wrfwbkh1qrx18i3u6", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "feature", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-126") {
		addForeignKeyConstraint(baseColumnNames: "publication_id", baseTableName: "feature_synonym", constraintName: "FK_82wsc3bv9i01t9851xv4xekis", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "publication", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-127") {
		addForeignKeyConstraint(baseColumnNames: "synonym_id", baseTableName: "feature_synonym", constraintName: "FK_ll4cqdh994s6x8n7vku1q7iwd", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "synonym", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-128") {
		addForeignKeyConstraint(baseColumnNames: "cvterm_id", baseTableName: "featurecvterm", constraintName: "FK_cuwo3ernssd0t0wjceb7lmm11", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "cvterm", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-129") {
		addForeignKeyConstraint(baseColumnNames: "feature_id", baseTableName: "featurecvterm", constraintName: "FK_iy7bbt67s7jaemiajsrqalv5o", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "feature", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-130") {
		addForeignKeyConstraint(baseColumnNames: "publication_id", baseTableName: "featurecvterm", constraintName: "FK_q3wop7ii25dgiofnp2l3yj9v0", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "publication", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-131") {
		addForeignKeyConstraint(baseColumnNames: "dbxref_id", baseTableName: "featurecvterm_dbxref", constraintName: "FK_r9xhefcekikp1od79ectkb22b", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "dbxref", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-132") {
		addForeignKeyConstraint(baseColumnNames: "featurecvterm_featurecvtermdbxrefs_id", baseTableName: "featurecvterm_dbxref", constraintName: "FK_pniehpb3pk1rqe95ejk0od6vg", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "featurecvterm", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-133") {
		addForeignKeyConstraint(baseColumnNames: "featurecvterm_featurecvterm_publications_id", baseTableName: "featurecvterm_publication", constraintName: "FK_3a9j1mryggb0bcaguvkhw8hjm", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "featurecvterm", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-134") {
		addForeignKeyConstraint(baseColumnNames: "publication_id", baseTableName: "featurecvterm_publication", constraintName: "FK_g6l9cr99p5dhb0kvs9y0tjwnv", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "publication", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-135") {
		addForeignKeyConstraint(baseColumnNames: "role_id", baseTableName: "grails_user_roles", constraintName: "FK_4mxkyj2itw9wyvcn6d8d4mta2", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "role", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-136") {
		addForeignKeyConstraint(baseColumnNames: "user_id", baseTableName: "grails_user_roles", constraintName: "FK_jsuq1rc9mb07tg4kubnqn8yw6", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "grails_user", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-137") {
		addForeignKeyConstraint(baseColumnNames: "organism_organism_properties_id", baseTableName: "organism_organism_property", constraintName: "FK_qyxdgqthtlgixvtdkkhc8g3pu", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "organism", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-138") {
		addForeignKeyConstraint(baseColumnNames: "organism_property_id", baseTableName: "organism_organism_property", constraintName: "FK_f1e1d91q04mqaij1ep3s36ujl", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "organism_property", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-139") {
		addForeignKeyConstraint(baseColumnNames: "organism_property_id", baseTableName: "organism_property_organism_property", constraintName: "FK_8jxbx51qysqlm07orah5xg45y", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "organism_property", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-140") {
		addForeignKeyConstraint(baseColumnNames: "organism_property_organism_properties_id", baseTableName: "organism_property_organism_property", constraintName: "FK_3rtkicqpr3ca8dwivye3yajaa", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "organism_property", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-141") {
		addForeignKeyConstraint(baseColumnNames: "organism_property_organismdbxrefs_id", baseTableName: "organism_property_organismdbxref", constraintName: "FK_kaayriabr4k4b3aomk46dmc77", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "organism_property", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-142") {
		addForeignKeyConstraint(baseColumnNames: "organismdbxref_id", baseTableName: "organism_property_organismdbxref", constraintName: "FK_jbw15sttun6yrcrxchi0lwtam", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "organismdbxref", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-143") {
		addForeignKeyConstraint(baseColumnNames: "dbxref_id", baseTableName: "organismdbxref", constraintName: "FK_s3vk7onqrk0n4c86xnvqmm3ho", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "dbxref", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-144") {
		addForeignKeyConstraint(baseColumnNames: "organism_id", baseTableName: "organismdbxref", constraintName: "FK_l1jfi0wpnyooutd820p5gskr", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "organism", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-145") {
		addForeignKeyConstraint(baseColumnNames: "group_id", baseTableName: "permission", constraintName: "FK_hycx5el5itt1lqidt532shkpj", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "user_group", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-146") {
		addForeignKeyConstraint(baseColumnNames: "organism_id", baseTableName: "permission", constraintName: "FK_4nvoxx3htem6jseb4rmu0aqfp", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "organism", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-147") {
		addForeignKeyConstraint(baseColumnNames: "user_id", baseTableName: "permission", constraintName: "FK_6p3mx8al2w4f7ltqiwf1j88fm", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "grails_user", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-148") {
		addForeignKeyConstraint(baseColumnNames: "assay_id", baseTableName: "phenotype", constraintName: "FK_cwgh6naf9gackae2ei11v6p41", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "cvterm", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-149") {
		addForeignKeyConstraint(baseColumnNames: "attribute_id", baseTableName: "phenotype", constraintName: "FK_phmfgylejydjqyrvo3imc97go", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "cvterm", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-150") {
		addForeignKeyConstraint(baseColumnNames: "cvalue_id", baseTableName: "phenotype", constraintName: "FK_jh0fc3orduigl8s7ymentbtrs", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "cvterm", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-151") {
		addForeignKeyConstraint(baseColumnNames: "observable_id", baseTableName: "phenotype", constraintName: "FK_gb4wy9qesx6vnekxekm18k9xa", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "cvterm", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-152") {
		addForeignKeyConstraint(baseColumnNames: "cvterm_id", baseTableName: "phenotype_cvterm", constraintName: "FK_9e2v7goj5w6nds5jo0x1va1nm", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "cvterm", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-153") {
		addForeignKeyConstraint(baseColumnNames: "phenotype_phenotypecvterms_id", baseTableName: "phenotype_cvterm", constraintName: "FK_aicsmj1kn20ikm14292g9r2j9", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "phenotype", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-154") {
		addForeignKeyConstraint(baseColumnNames: "environment_id", baseTableName: "phenotype_description", constraintName: "FK_t52r166gd8710vffy3aompe7d", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "environment", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-155") {
		addForeignKeyConstraint(baseColumnNames: "genotype_id", baseTableName: "phenotype_description", constraintName: "FK_bf1bstadamyw0gsarkb933l5b", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "genotype", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-156") {
		addForeignKeyConstraint(baseColumnNames: "publication_id", baseTableName: "phenotype_description", constraintName: "FK_1kkkx1uxs6li0r72qhvke6o77", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "publication", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-157") {
		addForeignKeyConstraint(baseColumnNames: "type_id", baseTableName: "phenotype_description", constraintName: "FK_8pbyj05khavdl5a648c7pmcil", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "cvterm", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-158") {
		addForeignKeyConstraint(baseColumnNames: "environment_id", baseTableName: "phenotype_statement", constraintName: "FK_q6jvhi3l7ty0m9tpbn09d8pxj", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "environment", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-159") {
		addForeignKeyConstraint(baseColumnNames: "genotype_id", baseTableName: "phenotype_statement", constraintName: "FK_8rbhsxxdf669tyed8jrr747hv", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "genotype", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-160") {
		addForeignKeyConstraint(baseColumnNames: "phenotype_id", baseTableName: "phenotype_statement", constraintName: "FK_gskh1e7b6qa2du48ayu49lr3s", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "phenotype", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-161") {
		addForeignKeyConstraint(baseColumnNames: "publication_id", baseTableName: "phenotype_statement", constraintName: "FK_hffc43aavltp8t5dtwactux5f", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "publication", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-162") {
		addForeignKeyConstraint(baseColumnNames: "type_id", baseTableName: "phenotype_statement", constraintName: "FK_tk1pgifvuhurefn0y3myfyyt4", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "cvterm", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-163") {
		addForeignKeyConstraint(baseColumnNames: "organism_id", baseTableName: "preference", constraintName: "FK_42b0lk4rcfjcagw84jugd1sgj", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "organism", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-164") {
		addForeignKeyConstraint(baseColumnNames: "sequence_id", baseTableName: "preference", constraintName: "FK_i94ksmdxi88hcqnuycebgkdvs", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "sequence", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-165") {
		addForeignKeyConstraint(baseColumnNames: "user_id", baseTableName: "preference", constraintName: "FK_ro87rogww8hoobbwya2nn16xk", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "grails_user", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-166") {
		addForeignKeyConstraint(baseColumnNames: "type_id", baseTableName: "publication", constraintName: "FK_h3g8f3q2krcnwmq2nasbanlay", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "cvterm", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-167") {
		addForeignKeyConstraint(baseColumnNames: "publication_id", baseTableName: "publication_author", constraintName: "FK_9eou8yof43krmrsdvfhfuisln", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "publication", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-168") {
		addForeignKeyConstraint(baseColumnNames: "object_publication_id", baseTableName: "publication_relationship", constraintName: "FK_97qgrmdull1avkfqpfq1mc1wt", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "publication", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-169") {
		addForeignKeyConstraint(baseColumnNames: "subject_publication_id", baseTableName: "publication_relationship", constraintName: "FK_euu6xx78omdvver8lij1ys2oq", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "publication", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-170") {
		addForeignKeyConstraint(baseColumnNames: "type_id", baseTableName: "publication_relationship", constraintName: "FK_q6hf14oiq9pomkjrhtndonmeh", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "cvterm", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-171") {
		addForeignKeyConstraint(baseColumnNames: "dbxref_id", baseTableName: "publicationdbxref", constraintName: "FK_oh81hma8qx88fhvcmfugx836b", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "dbxref", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-172") {
		addForeignKeyConstraint(baseColumnNames: "publication_id", baseTableName: "publicationdbxref", constraintName: "FK_pgoyqd75q47r6ycwowcppbhk6", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "publication", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-173") {
		addForeignKeyConstraint(baseColumnNames: "role_id", baseTableName: "role_permissions", constraintName: "FK_d4atqq8ege1sij0316vh2mxfu", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "role", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-174") {
		addForeignKeyConstraint(baseColumnNames: "organism_id", baseTableName: "sequence", constraintName: "FK_rux0954nxr4lwvj2qgyjibua7", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "organism", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-175") {
		addForeignKeyConstraint(baseColumnNames: "sequence_id", baseTableName: "sequence_chunk", constraintName: "FK_4tsu0cp2dh2avbxifp1h1c9vd", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "sequence", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-176") {
		addForeignKeyConstraint(baseColumnNames: "type_id", baseTableName: "synonym", constraintName: "FK_4ylco1irefvydmsnedglqqdfu", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "cvterm", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-177") {
		addForeignKeyConstraint(baseColumnNames: "user_group_id", baseTableName: "user_group_users", constraintName: "FK_9jib0g899h0gy3dypo7datfm9", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "user_group", referencesUniqueColumn: "false")
	}

	changeSet(author: "nathandunn (generated)", id: "1445456090131-178") {
		addForeignKeyConstraint(baseColumnNames: "user_id", baseTableName: "user_group_users", constraintName: "FK_jppito7humh6e3v5mjjtutd7h", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "grails_user", referencesUniqueColumn: "false")
	}
}
