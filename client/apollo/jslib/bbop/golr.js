/* 
 * Package: golr.js
 * 
 * Namespace: amigo.data.golr
 * 
 * This package was automatically created during an AmiGO 2 installation
 * from the YAML configuration files that AmiGO pulls in.
 *
 * Useful information about GOlr. See the package <golr_conf.js>
 * for the API to interact with this data file.
 *
 * NOTE: This file is generated dynamically at installation time.
 * Hard to work with unit tests--hope it's not too bad. You have to
 * occasionally copy back to keep the unit tests sane.
 *
 * NOTE: This file has a slightly different latout from the YAML
 * configurations files--in addition instead of the fields
 * being in lists (fields), they are in hashes keyed by the
 * field id (fields_hash).
 */


// All of the server/instance-specific meta-data.
bbop.core.require('bbop', 'core');
bbop.core.namespace('amigo', 'data', 'golr');

/*
 * Variable: golr
 * 
 * The configuration for the data.
 * Essentially a JSONification of the OWLTools YAML files.
 * This should be consumed directly by <bbop.golr.conf>.
 */
amigo.data.golr = {
   "general" : {
      "searchable_extension" : "_searchable",
      "result_weights" : "entity^3.0 category^1.0",
      "filter_weights" : "category^4.0",
      "_infile" : "/home/sjcarbon/local/src/git/amigo/metadata//general-config.yaml",
      "display_name" : "General",
      "description" : "A generic search document to get a general overview of everything.",
      "schema_generating" : "true",
      "boost_weights" : "entity^3.0 entity_label^3.0 general_blob^3.0",
      "fields" : [
         {
            "transform" : [],
            "description" : "The mangled internal ID for this entity.",
            "display_name" : "Internal ID",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "id",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "The ID/label for this entity.",
            "display_name" : "Entity",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "entity",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "The label for this entity.",
            "display_name" : "Enity label",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "entity_label",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "The document category that this enitity belongs to.",
            "display_name" : "Document category",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "category",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "A hidden searchable blob document to access this item. It should contain all the goodies that we want to search for, like species(?), synonyms, etc.",
            "display_name" : "Generic blob",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "general_blob",
            "property" : []
         }
      ],
      "fields_hash" : {
         "entity_label" : {
            "transform" : [],
            "description" : "The label for this entity.",
            "display_name" : "Enity label",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "entity_label",
            "property" : []
         },
         "entity" : {
            "transform" : [],
            "description" : "The ID/label for this entity.",
            "display_name" : "Entity",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "entity",
            "property" : []
         },
         "general_blob" : {
            "transform" : [],
            "description" : "A hidden searchable blob document to access this item. It should contain all the goodies that we want to search for, like species(?), synonyms, etc.",
            "display_name" : "Generic blob",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "general_blob",
            "property" : []
         },
         "category" : {
            "transform" : [],
            "description" : "The document category that this enitity belongs to.",
            "display_name" : "Document category",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "category",
            "property" : []
         },
         "id" : {
            "transform" : [],
            "description" : "The mangled internal ID for this entity.",
            "display_name" : "Internal ID",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "id",
            "property" : []
         }
      },
      "document_category" : "general",
      "weight" : "0",
      "_strict" : 0,
      "id" : "general",
      "_outfile" : "/home/sjcarbon/local/src/git/amigo/metadata//general-config.yaml"
   },
   "complex_annotation" : {
      "searchable_extension" : "_searchable",
      "result_weights" : "function_class^5.0 enabled_by^4.0 location_list^3.0 process_class^2.0 annotation_group^1.0",
      "filter_weights" : "annotation_group_label^5.0 enabled_by_label^4.5 location_list_closure_label^4.0 process_class_closure_label^3.0 function_class_closure_label^2.0",
      "_infile" : "/home/sjcarbon/local/src/git/amigo/metadata//complex-ann-config.yaml",
      "display_name" : "Complex annotations (ALPHA)",
      "description" : "An individual unit within LEGO. This is <strong>ALPHA</strong> software.",
      "schema_generating" : "true",
      "boost_weights" : "annotation_group_label^1.0 annotation_unit_label^1.0 enabled_by^1.0 enabled_by_label^1.0 location_list_closure^1.0 location_list_closure_label^1.0 process_class_closure_label^1.0 function_class_closure_label^1.0",
      "fields" : [
         {
            "transform" : [],
            "description" : "A unique (and internal) thing.",
            "display_name" : "ID",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "id",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "???.",
            "display_name" : "Annotation unit",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "annotation_unit",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "???.",
            "display_name" : "Annotation unit",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "annotation_unit_label",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "???.",
            "display_name" : "Annotation group",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "annotation_group",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "???.",
            "display_name" : "Annotation group",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "annotation_group_label",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "???",
            "display_name" : "Enabled by",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "enabled_by",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "???",
            "display_name" : "Enabled by",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "enabled_by_label",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "PANTHER family IDs that are associated with this entity.",
            "display_name" : "PANTHER family",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "panther_family",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "PANTHER families that are associated with this entity.",
            "display_name" : "PANTHER family",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "panther_family_label",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "GAF column 13 (taxon).",
            "display_name" : "Taxon",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "taxon",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "Taxon derived from GAF column 13 and ncbi_taxonomy.obo.",
            "display_name" : "Taxon",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "taxon_label",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "Taxon IDs derived from GAF column 13 and ncbi_taxonomy.obo.",
            "display_name" : "Taxon (IDs)",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "taxon_closure",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "Taxon label closure derived from GAF column 13 and ncbi_taxonomy.obo.",
            "display_name" : "Taxon",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "taxon_closure_label",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "Function acc/ID.",
            "display_name" : "Function",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "function_class",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "Common function name.",
            "display_name" : "Function",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "function_class_label",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "???",
            "display_name" : "Function",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "function_class_closure",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "???",
            "display_name" : "Function",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "function_class_closure_label",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "Process acc/ID.",
            "display_name" : "Process",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "process_class",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "Common process name.",
            "display_name" : "Process",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "process_class_label",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "???",
            "display_name" : "Process",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "process_class_closure",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "???",
            "display_name" : "Process",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "process_class_closure_label",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "",
            "display_name" : "Location",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "location_list",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "",
            "display_name" : "Location",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "location_list_label",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "",
            "display_name" : "Location",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "location_list_closure",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "",
            "display_name" : "Location",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "location_list_closure_label",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "???",
            "display_name" : "???",
            "indexed" : "false",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "owl_blob_json",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "JSON blob form of the local stepwise topology graph.",
            "display_name" : "Topology graph (JSON)",
            "indexed" : "false",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "topology_graph_json",
            "property" : []
         }
      ],
      "fields_hash" : {
         "annotation_group" : {
            "transform" : [],
            "description" : "???.",
            "display_name" : "Annotation group",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "annotation_group",
            "property" : []
         },
         "process_class_closure_label" : {
            "transform" : [],
            "description" : "???",
            "display_name" : "Process",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "process_class_closure_label",
            "property" : []
         },
         "function_class_label" : {
            "transform" : [],
            "description" : "Common function name.",
            "display_name" : "Function",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "function_class_label",
            "property" : []
         },
         "panther_family_label" : {
            "transform" : [],
            "description" : "PANTHER families that are associated with this entity.",
            "display_name" : "PANTHER family",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "panther_family_label",
            "property" : []
         },
         "panther_family" : {
            "transform" : [],
            "description" : "PANTHER family IDs that are associated with this entity.",
            "display_name" : "PANTHER family",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "panther_family",
            "property" : []
         },
         "process_class_closure" : {
            "transform" : [],
            "description" : "???",
            "display_name" : "Process",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "process_class_closure",
            "property" : []
         },
         "taxon_closure_label" : {
            "transform" : [],
            "description" : "Taxon label closure derived from GAF column 13 and ncbi_taxonomy.obo.",
            "display_name" : "Taxon",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "taxon_closure_label",
            "property" : []
         },
         "enabled_by" : {
            "transform" : [],
            "description" : "???",
            "display_name" : "Enabled by",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "enabled_by",
            "property" : []
         },
         "function_class_closure" : {
            "transform" : [],
            "description" : "???",
            "display_name" : "Function",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "function_class_closure",
            "property" : []
         },
         "location_list_label" : {
            "transform" : [],
            "description" : "",
            "display_name" : "Location",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "location_list_label",
            "property" : []
         },
         "annotation_unit" : {
            "transform" : [],
            "description" : "???.",
            "display_name" : "Annotation unit",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "annotation_unit",
            "property" : []
         },
         "location_list" : {
            "transform" : [],
            "description" : "",
            "display_name" : "Location",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "location_list",
            "property" : []
         },
         "topology_graph_json" : {
            "transform" : [],
            "description" : "JSON blob form of the local stepwise topology graph.",
            "display_name" : "Topology graph (JSON)",
            "indexed" : "false",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "topology_graph_json",
            "property" : []
         },
         "process_class_label" : {
            "transform" : [],
            "description" : "Common process name.",
            "display_name" : "Process",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "process_class_label",
            "property" : []
         },
         "function_class_closure_label" : {
            "transform" : [],
            "description" : "???",
            "display_name" : "Function",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "function_class_closure_label",
            "property" : []
         },
         "id" : {
            "transform" : [],
            "description" : "A unique (and internal) thing.",
            "display_name" : "ID",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "id",
            "property" : []
         },
         "function_class" : {
            "transform" : [],
            "description" : "Function acc/ID.",
            "display_name" : "Function",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "function_class",
            "property" : []
         },
         "taxon_closure" : {
            "transform" : [],
            "description" : "Taxon IDs derived from GAF column 13 and ncbi_taxonomy.obo.",
            "display_name" : "Taxon (IDs)",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "taxon_closure",
            "property" : []
         },
         "annotation_group_label" : {
            "transform" : [],
            "description" : "???.",
            "display_name" : "Annotation group",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "annotation_group_label",
            "property" : []
         },
         "location_list_closure_label" : {
            "transform" : [],
            "description" : "",
            "display_name" : "Location",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "location_list_closure_label",
            "property" : []
         },
         "location_list_closure" : {
            "transform" : [],
            "description" : "",
            "display_name" : "Location",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "location_list_closure",
            "property" : []
         },
         "annotation_unit_label" : {
            "transform" : [],
            "description" : "???.",
            "display_name" : "Annotation unit",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "annotation_unit_label",
            "property" : []
         },
         "enabled_by_label" : {
            "transform" : [],
            "description" : "???",
            "display_name" : "Enabled by",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "enabled_by_label",
            "property" : []
         },
         "taxon" : {
            "transform" : [],
            "description" : "GAF column 13 (taxon).",
            "display_name" : "Taxon",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "taxon",
            "property" : []
         },
         "taxon_label" : {
            "transform" : [],
            "description" : "Taxon derived from GAF column 13 and ncbi_taxonomy.obo.",
            "display_name" : "Taxon",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "taxon_label",
            "property" : []
         },
         "owl_blob_json" : {
            "transform" : [],
            "description" : "???",
            "display_name" : "???",
            "indexed" : "false",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "owl_blob_json",
            "property" : []
         },
         "process_class" : {
            "transform" : [],
            "description" : "Process acc/ID.",
            "display_name" : "Process",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "process_class",
            "property" : []
         }
      },
      "document_category" : "complex_annotation",
      "weight" : "-5",
      "_strict" : 0,
      "id" : "complex_annotation",
      "_outfile" : "/home/sjcarbon/local/src/git/amigo/metadata//complex-ann-config.yaml"
   },
   "ontology" : {
      "searchable_extension" : "_searchable",
      "result_weights" : "annotation_class^8.0 description^6.0 source^4.0 synonym^3.0 alternate_id^2.0",
      "filter_weights" : "source^4.0 subset^3.0 regulates_closure_label^1.0 is_obsolete^0.0",
      "_infile" : "/home/sjcarbon/local/src/git/amigo/metadata//ont-config.yaml",
      "display_name" : "Ontology",
      "description" : "Ontology classes for GO.",
      "schema_generating" : "true",
      "boost_weights" : "annotation_class^3.0 annotation_class_label^5.5 description^1.0 comment^0.5 synonym^1.0 alternate_id^1.0",
      "fields" : [
         {
            "transform" : [],
            "description" : "Term acc/ID.",
            "display_name" : "Acc",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "id",
            "property" : [
               "getIdentifier"
            ]
         },
         {
            "transform" : [],
            "description" : "Term acc/ID.",
            "display_name" : "Term",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "annotation_class",
            "property" : [
               "getIdentifier"
            ]
         },
         {
            "transform" : [],
            "description" : "Common term name.",
            "display_name" : "Term",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "annotation_class_label",
            "property" : [
               "getLabel"
            ]
         },
         {
            "transform" : [],
            "description" : "Term definition.",
            "display_name" : "Definition",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "description",
            "property" : [
               "getDef"
            ]
         },
         {
            "transform" : [],
            "description" : "Term namespace.",
            "display_name" : "Ontology source",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "source",
            "property" : [
               "getNamespace"
            ]
         },
         {
            "transform" : [],
            "description" : "Is the term obsolete?",
            "display_name" : "Obsoletion",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "boolean",
            "id" : "is_obsolete",
            "property" : [
               "getIsObsoleteBinaryString"
            ]
         },
         {
            "transform" : [],
            "description" : "Term comment.",
            "display_name" : "Comment",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "comment",
            "property" : [
               "getComment"
            ]
         },
         {
            "transform" : [],
            "description" : "Term synonyms.",
            "display_name" : "Synonyms",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "synonym",
            "property" : [
               "getOBOSynonymStrings"
            ]
         },
         {
            "transform" : [],
            "description" : "Alternate term id.",
            "display_name" : "Alt ID",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "alternate_id",
            "property" : [
               "getAnnotationPropertyValues",
               "alt_id"
            ]
         },
         {
            "transform" : [],
            "description" : "Term that replaces this term.",
            "display_name" : "Replaced By",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "replaced_by",
            "property" : [
               "getAnnotationPropertyValues",
               "replaced_by"
            ]
         },
         {
            "transform" : [],
            "description" : "Others terms you might want to look at.",
            "display_name" : "Consider",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "consider",
            "property" : [
               "getAnnotationPropertyValues",
               "consider"
            ]
         },
         {
            "transform" : [],
            "description" : "Term subset.",
            "display_name" : "Subset",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "subset",
            "property" : [
               "getSubsets"
            ]
         },
         {
            "transform" : [],
            "description" : "Definition cross-reference.",
            "display_name" : "Def xref",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "definition_xref",
            "property" : [
               "getDefXref"
            ]
         },
         {
            "transform" : [],
            "description" : "Database cross-reference.",
            "display_name" : "DB xref",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "database_xref",
            "property" : [
               "getXref"
            ]
         },
         {
            "transform" : [],
            "description" : "Closure of ids/accs over isa and partof.",
            "display_name" : "Is-a/part-of",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "isa_partof_closure",
            "property" : [
               "getRelationIDClosure",
               "BFO:0000050"
            ]
         },
         {
            "transform" : [],
            "description" : "Closure of labels over isa and partof.",
            "display_name" : "Is-a/part-of",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "isa_partof_closure_label",
            "property" : [
               "getRelationLabelClosure",
               "BFO:0000050"
            ]
         },
         {
            "transform" : [],
            "description" : "Closure of ids/accs over regulates.",
            "display_name" : "Ancestor",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "regulates_closure",
            "property" : [
               "getRelationIDClosure",
               "BFO:0000050",
               "RO:0002211",
               "RO:0002212",
               "RO:0002213"
            ]
         },
         {
            "transform" : [],
            "description" : "Closure of labels over regulates.",
            "display_name" : "Ancestor",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "regulates_closure_label",
            "property" : [
               "getRelationLabelClosure",
               "BFO:0000050",
               "RO:0002211",
               "RO:0002212",
               "RO:0002213"
            ]
         },
         {
            "transform" : [],
            "description" : "Closure of ids/accs over regulates.",
            "display_name" : "Ancestor",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "regulates_closure",
            "property" : [
               "getRelationIDClosure",
               "BFO:0000050",
               "RO:0002211",
               "RO:0002212",
               "RO:0002213"
            ]
         },
         {
            "transform" : [],
            "description" : "Closure of labels over regulates.",
            "display_name" : "Ancestor",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "regulates_closure_label",
            "property" : [
               "getRelationLabelClosure",
               "BFO:0000050",
               "RO:0002211",
               "RO:0002212",
               "RO:0002213"
            ]
         },
         {
            "transform" : [],
            "description" : "JSON blob form of the local stepwise topology graph.",
            "display_name" : "Topology graph (JSON)",
            "indexed" : "false",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "topology_graph_json",
            "property" : [
               "getSegmentShuntGraphJSON"
            ]
         },
         {
            "transform" : [],
            "description" : "JSON blob form of the local relation transitivity graph.",
            "display_name" : "Regulates transitivity graph (JSON)",
            "indexed" : "false",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "regulates_transitivity_graph_json",
            "property" : [
               "getLineageShuntGraphJSON"
            ]
         },
         {
            "transform" : [],
            "description" : "Only in taxon.",
            "display_name" : "Only in taxon",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "only_in_taxon",
            "property" : [
               "getIdentifier"
            ]
         },
         {
            "transform" : [],
            "description" : "Only in taxon label.",
            "display_name" : "Only in taxon",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "only_in_taxon_label",
            "property" : [
               "getLabel"
            ]
         },
         {
            "transform" : [],
            "description" : "Only in taxon closure.",
            "display_name" : "Only in taxon (IDs)",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "only_in_taxon_closure",
            "property" : [
               "getRelationLabelClosure",
               "RO:0002160"
            ]
         },
         {
            "transform" : [],
            "description" : "Only in taxon label closure.",
            "display_name" : "Only in taxon",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "only_in_taxon_closure_label",
            "property" : [
               "getRelationLabelClosure",
               "RO:0002160"
            ]
         }
      ],
      "fields_hash" : {
         "only_in_taxon_closure" : {
            "transform" : [],
            "description" : "Only in taxon closure.",
            "display_name" : "Only in taxon (IDs)",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "only_in_taxon_closure",
            "property" : [
               "getRelationLabelClosure",
               "RO:0002160"
            ]
         },
         "source" : {
            "transform" : [],
            "description" : "Term namespace.",
            "display_name" : "Ontology source",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "source",
            "property" : [
               "getNamespace"
            ]
         },
         "definition_xref" : {
            "transform" : [],
            "description" : "Definition cross-reference.",
            "display_name" : "Def xref",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "definition_xref",
            "property" : [
               "getDefXref"
            ]
         },
         "regulates_transitivity_graph_json" : {
            "transform" : [],
            "description" : "JSON blob form of the local relation transitivity graph.",
            "display_name" : "Regulates transitivity graph (JSON)",
            "indexed" : "false",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "regulates_transitivity_graph_json",
            "property" : [
               "getLineageShuntGraphJSON"
            ]
         },
         "database_xref" : {
            "transform" : [],
            "description" : "Database cross-reference.",
            "display_name" : "DB xref",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "database_xref",
            "property" : [
               "getXref"
            ]
         },
         "alternate_id" : {
            "transform" : [],
            "description" : "Alternate term id.",
            "display_name" : "Alt ID",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "alternate_id",
            "property" : [
               "getAnnotationPropertyValues",
               "alt_id"
            ]
         },
         "only_in_taxon_closure_label" : {
            "transform" : [],
            "description" : "Only in taxon label closure.",
            "display_name" : "Only in taxon",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "only_in_taxon_closure_label",
            "property" : [
               "getRelationLabelClosure",
               "RO:0002160"
            ]
         },
         "consider" : {
            "transform" : [],
            "description" : "Others terms you might want to look at.",
            "display_name" : "Consider",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "consider",
            "property" : [
               "getAnnotationPropertyValues",
               "consider"
            ]
         },
         "topology_graph_json" : {
            "transform" : [],
            "description" : "JSON blob form of the local stepwise topology graph.",
            "display_name" : "Topology graph (JSON)",
            "indexed" : "false",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "topology_graph_json",
            "property" : [
               "getSegmentShuntGraphJSON"
            ]
         },
         "subset" : {
            "transform" : [],
            "description" : "Term subset.",
            "display_name" : "Subset",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "subset",
            "property" : [
               "getSubsets"
            ]
         },
         "only_in_taxon_label" : {
            "transform" : [],
            "description" : "Only in taxon label.",
            "display_name" : "Only in taxon",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "only_in_taxon_label",
            "property" : [
               "getLabel"
            ]
         },
         "only_in_taxon" : {
            "transform" : [],
            "description" : "Only in taxon.",
            "display_name" : "Only in taxon",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "only_in_taxon",
            "property" : [
               "getIdentifier"
            ]
         },
         "id" : {
            "transform" : [],
            "description" : "Term acc/ID.",
            "display_name" : "Acc",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "id",
            "property" : [
               "getIdentifier"
            ]
         },
         "is_obsolete" : {
            "transform" : [],
            "description" : "Is the term obsolete?",
            "display_name" : "Obsoletion",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "boolean",
            "id" : "is_obsolete",
            "property" : [
               "getIsObsoleteBinaryString"
            ]
         },
         "isa_partof_closure_label" : {
            "transform" : [],
            "description" : "Closure of labels over isa and partof.",
            "display_name" : "Is-a/part-of",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "isa_partof_closure_label",
            "property" : [
               "getRelationLabelClosure",
               "BFO:0000050"
            ]
         },
         "replaced_by" : {
            "transform" : [],
            "description" : "Term that replaces this term.",
            "display_name" : "Replaced By",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "replaced_by",
            "property" : [
               "getAnnotationPropertyValues",
               "replaced_by"
            ]
         },
         "annotation_class" : {
            "transform" : [],
            "description" : "Term acc/ID.",
            "display_name" : "Term",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "annotation_class",
            "property" : [
               "getIdentifier"
            ]
         },
         "regulates_closure_label" : {
            "transform" : [],
            "description" : "Closure of labels over regulates.",
            "display_name" : "Ancestor",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "regulates_closure_label",
            "property" : [
               "getRelationLabelClosure",
               "BFO:0000050",
               "RO:0002211",
               "RO:0002212",
               "RO:0002213"
            ]
         },
         "description" : {
            "transform" : [],
            "description" : "Term definition.",
            "display_name" : "Definition",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "description",
            "property" : [
               "getDef"
            ]
         },
         "regulates_closure" : {
            "transform" : [],
            "description" : "Closure of ids/accs over regulates.",
            "display_name" : "Ancestor",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "regulates_closure",
            "property" : [
               "getRelationIDClosure",
               "BFO:0000050",
               "RO:0002211",
               "RO:0002212",
               "RO:0002213"
            ]
         },
         "isa_partof_closure" : {
            "transform" : [],
            "description" : "Closure of ids/accs over isa and partof.",
            "display_name" : "Is-a/part-of",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "isa_partof_closure",
            "property" : [
               "getRelationIDClosure",
               "BFO:0000050"
            ]
         },
         "synonym" : {
            "transform" : [],
            "description" : "Term synonyms.",
            "display_name" : "Synonyms",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "synonym",
            "property" : [
               "getOBOSynonymStrings"
            ]
         },
         "comment" : {
            "transform" : [],
            "description" : "Term comment.",
            "display_name" : "Comment",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "comment",
            "property" : [
               "getComment"
            ]
         },
         "annotation_class_label" : {
            "transform" : [],
            "description" : "Common term name.",
            "display_name" : "Term",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "annotation_class_label",
            "property" : [
               "getLabel"
            ]
         }
      },
      "document_category" : "ontology_class",
      "weight" : "40",
      "_strict" : 0,
      "id" : "ontology",
      "_outfile" : "/home/sjcarbon/local/src/git/amigo/metadata//ont-config.yaml"
   },
   "bbop_ann_ev_agg" : {
      "searchable_extension" : "_searchable",
      "result_weights" : "bioentity^4.0 annotation_class^3.0 taxon^2.0",
      "filter_weights" : "evidence_type_closure^4.0 evidence_with^3.0 taxon_closure_label^2.0",
      "_infile" : "/home/sjcarbon/local/src/git/amigo/metadata//ann_ev_agg-config.yaml",
      "display_name" : "Advanced",
      "description" : "A description of annotation evidence aggregate for GOlr and AmiGO.",
      "schema_generating" : "true",
      "boost_weights" : "annotation_class^2.0 annotation_class_label^1.0 bioentity^2.0 bioentity_label^1.0 panther_family^1.0 panther_family_label^1.0 taxon_closure_label^1.0",
      "fields" : [
         {
            "transform" : [],
            "description" : "Gene/product ID.",
            "display_name" : "Acc",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "id",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "Column 1 + columns 2.",
            "display_name" : "Gene/product ID",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "bioentity",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "Column 3.",
            "display_name" : "Gene/product label",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "bioentity_label",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "Column 5.",
            "display_name" : "Annotation class",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "annotation_class",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "Column 5 + ontology.",
            "display_name" : "Annotation class label",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "annotation_class_label",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "All evidence for this term/gene product pair",
            "display_name" : "Evidence type",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "evidence_type_closure",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "All column 8s for this term/gene product pair",
            "display_name" : "Evidence with",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "evidence_with",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "Column 13: taxon.",
            "display_name" : "Taxon",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "taxon",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "Derived from C13 + ncbi_taxonomy.obo.",
            "display_name" : "Taxon",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "taxon_label",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "IDs derived from C13 + ncbi_taxonomy.obo.",
            "display_name" : "Taxon (IDs)",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "taxon_closure",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "Labels derived from C13 + ncbi_taxonomy.obo.",
            "display_name" : "Taxon",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "taxon_closure_label",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "Family IDs that are associated with this entity.",
            "display_name" : "Protein family",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "panther_family",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "Families that are associated with this entity.",
            "display_name" : "Family",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "panther_family_label",
            "property" : []
         }
      ],
      "fields_hash" : {
         "panther_family_label" : {
            "transform" : [],
            "description" : "Families that are associated with this entity.",
            "display_name" : "Family",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "panther_family_label",
            "property" : []
         },
         "panther_family" : {
            "transform" : [],
            "description" : "Family IDs that are associated with this entity.",
            "display_name" : "Protein family",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "panther_family",
            "property" : []
         },
         "bioentity_label" : {
            "transform" : [],
            "description" : "Column 3.",
            "display_name" : "Gene/product label",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "bioentity_label",
            "property" : []
         },
         "taxon_closure_label" : {
            "transform" : [],
            "description" : "Labels derived from C13 + ncbi_taxonomy.obo.",
            "display_name" : "Taxon",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "taxon_closure_label",
            "property" : []
         },
         "annotation_class" : {
            "transform" : [],
            "description" : "Column 5.",
            "display_name" : "Annotation class",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "annotation_class",
            "property" : []
         },
         "taxon" : {
            "transform" : [],
            "description" : "Column 13: taxon.",
            "display_name" : "Taxon",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "taxon",
            "property" : []
         },
         "bioentity" : {
            "transform" : [],
            "description" : "Column 1 + columns 2.",
            "display_name" : "Gene/product ID",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "bioentity",
            "property" : []
         },
         "taxon_label" : {
            "transform" : [],
            "description" : "Derived from C13 + ncbi_taxonomy.obo.",
            "display_name" : "Taxon",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "taxon_label",
            "property" : []
         },
         "annotation_class_label" : {
            "transform" : [],
            "description" : "Column 5 + ontology.",
            "display_name" : "Annotation class label",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "annotation_class_label",
            "property" : []
         },
         "evidence_type_closure" : {
            "transform" : [],
            "description" : "All evidence for this term/gene product pair",
            "display_name" : "Evidence type",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "evidence_type_closure",
            "property" : []
         },
         "id" : {
            "transform" : [],
            "description" : "Gene/product ID.",
            "display_name" : "Acc",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "id",
            "property" : []
         },
         "evidence_with" : {
            "transform" : [],
            "description" : "All column 8s for this term/gene product pair",
            "display_name" : "Evidence with",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "evidence_with",
            "property" : []
         },
         "taxon_closure" : {
            "transform" : [],
            "description" : "IDs derived from C13 + ncbi_taxonomy.obo.",
            "display_name" : "Taxon (IDs)",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "taxon_closure",
            "property" : []
         }
      },
      "document_category" : "annotation_evidence_aggregate",
      "weight" : "-10",
      "_strict" : 0,
      "id" : "bbop_ann_ev_agg",
      "_outfile" : "/home/sjcarbon/local/src/git/amigo/metadata//ann_ev_agg-config.yaml"
   },
   "bioentity" : {
      "searchable_extension" : "_searchable",
      "result_weights" : "bioentity^8.0 bioentity_name^7.0 taxon^6.0 panther_family^5.0 type^4.0 source^3.0 annotation_class_list^2.0",
      "filter_weights" : "source^7.0 type^6.0 panther_family_label^5.0 annotation_class_list_label^3.5 taxon_closure_label^4.0 regulates_closure_label^2.0",
      "_infile" : "/home/sjcarbon/local/src/git/amigo/metadata//bio-config.yaml",
      "display_name" : "Gene/products",
      "description" : "A description of bioentities file for GOlr.",
      "schema_generating" : "true",
      "boost_weights" : "bioentity^2.0 bioentity_label^2.0 bioentity_name^1.0 bioentity_internal_id^1.0 isa_partof_closure_label^1.0 regulates_closure_label^1.0 panther_family^1.0 panther_family_label^1.0 taxon_closure_label^1.0",
      "fields" : [
         {
            "transform" : [],
            "description" : "Gene/product ID.",
            "display_name" : "Acc",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "id",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "Gene/product ID.",
            "display_name" : "Acc",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "bioentity",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "Symbol or name.",
            "display_name" : "Label",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "bioentity_label",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "The full name of the gene product.",
            "display_name" : "Name",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "bioentity_name",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "The bioentity ID used at the database of origin.",
            "display_name" : "This should not be displayed",
            "indexed" : "false",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "bioentity_internal_id",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "Type class (GAF column 12).",
            "display_name" : "Type",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "type",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "GAF column 13 (taxon).",
            "display_name" : "Taxon",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "taxon",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "Taxon derived from GAF column 13 and ncbi_taxonomy.obo.",
            "display_name" : "Taxon",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "taxon_label",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "Taxon IDs derived from GAF column 13 and ncbi_taxonomy.obo.",
            "display_name" : "Taxon",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "taxon_closure",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "Taxon label closure derived from GAF column 13 and ncbi_taxonomy.obo.",
            "display_name" : "Taxon",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "taxon_closure_label",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "Closure of ids/accs over isa and partof.",
            "display_name" : "Involved in",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "isa_partof_closure",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "Closure of labels over isa and partof.",
            "display_name" : "Involved in",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "isa_partof_closure_label",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "Closure of ids/accs over regulates.",
            "display_name" : "Inferred annotation (IDs)",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "regulates_closure",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "Closure of labels over regulates.",
            "display_name" : "Inferred annotation",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "regulates_closure_label",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "GAF column 1 (database source).",
            "display_name" : "Source",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "source",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "Direct annotations (GAF column 5).",
            "display_name" : "Direct annotation",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "annotation_class_list",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "Direct annotations (GAF column 5).",
            "display_name" : "Direct annotation",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "annotation_class_list_label",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "Gene product synonyms.",
            "display_name" : "Synonyms",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "synonym",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "PANTHER family IDs that are associated with this entity.",
            "display_name" : "PANTHER family",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "panther_family",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "PANTHER families that are associated with this entity.",
            "display_name" : "PANTHER family",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "panther_family_label",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "JSON blob form of the phylogenic tree.",
            "display_name" : "This should not be displayed",
            "indexed" : "false",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "phylo_graph_json",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "Database cross-reference.",
            "display_name" : "DB xref",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "database_xref",
            "property" : []
         }
      ],
      "fields_hash" : {
         "source" : {
            "transform" : [],
            "description" : "GAF column 1 (database source).",
            "display_name" : "Source",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "source",
            "property" : []
         },
         "phylo_graph_json" : {
            "transform" : [],
            "description" : "JSON blob form of the phylogenic tree.",
            "display_name" : "This should not be displayed",
            "indexed" : "false",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "phylo_graph_json",
            "property" : []
         },
         "panther_family_label" : {
            "transform" : [],
            "description" : "PANTHER families that are associated with this entity.",
            "display_name" : "PANTHER family",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "panther_family_label",
            "property" : []
         },
         "panther_family" : {
            "transform" : [],
            "description" : "PANTHER family IDs that are associated with this entity.",
            "display_name" : "PANTHER family",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "panther_family",
            "property" : []
         },
         "bioentity_label" : {
            "transform" : [],
            "description" : "Symbol or name.",
            "display_name" : "Label",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "bioentity_label",
            "property" : []
         },
         "database_xref" : {
            "transform" : [],
            "description" : "Database cross-reference.",
            "display_name" : "DB xref",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "database_xref",
            "property" : []
         },
         "taxon_closure_label" : {
            "transform" : [],
            "description" : "Taxon label closure derived from GAF column 13 and ncbi_taxonomy.obo.",
            "display_name" : "Taxon",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "taxon_closure_label",
            "property" : []
         },
         "annotation_class_list_label" : {
            "transform" : [],
            "description" : "Direct annotations (GAF column 5).",
            "display_name" : "Direct annotation",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "annotation_class_list_label",
            "property" : []
         },
         "bioentity_name" : {
            "transform" : [],
            "description" : "The full name of the gene product.",
            "display_name" : "Name",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "bioentity_name",
            "property" : []
         },
         "bioentity_internal_id" : {
            "transform" : [],
            "description" : "The bioentity ID used at the database of origin.",
            "display_name" : "This should not be displayed",
            "indexed" : "false",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "bioentity_internal_id",
            "property" : []
         },
         "id" : {
            "transform" : [],
            "description" : "Gene/product ID.",
            "display_name" : "Acc",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "id",
            "property" : []
         },
         "taxon_closure" : {
            "transform" : [],
            "description" : "Taxon IDs derived from GAF column 13 and ncbi_taxonomy.obo.",
            "display_name" : "Taxon",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "taxon_closure",
            "property" : []
         },
         "isa_partof_closure_label" : {
            "transform" : [],
            "description" : "Closure of labels over isa and partof.",
            "display_name" : "Involved in",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "isa_partof_closure_label",
            "property" : []
         },
         "annotation_class_list" : {
            "transform" : [],
            "description" : "Direct annotations (GAF column 5).",
            "display_name" : "Direct annotation",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "annotation_class_list",
            "property" : []
         },
         "taxon" : {
            "transform" : [],
            "description" : "GAF column 13 (taxon).",
            "display_name" : "Taxon",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "taxon",
            "property" : []
         },
         "regulates_closure_label" : {
            "transform" : [],
            "description" : "Closure of labels over regulates.",
            "display_name" : "Inferred annotation",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "regulates_closure_label",
            "property" : []
         },
         "regulates_closure" : {
            "transform" : [],
            "description" : "Closure of ids/accs over regulates.",
            "display_name" : "Inferred annotation (IDs)",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "regulates_closure",
            "property" : []
         },
         "bioentity" : {
            "transform" : [],
            "description" : "Gene/product ID.",
            "display_name" : "Acc",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "bioentity",
            "property" : []
         },
         "isa_partof_closure" : {
            "transform" : [],
            "description" : "Closure of ids/accs over isa and partof.",
            "display_name" : "Involved in",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "isa_partof_closure",
            "property" : []
         },
         "synonym" : {
            "transform" : [],
            "description" : "Gene product synonyms.",
            "display_name" : "Synonyms",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "synonym",
            "property" : []
         },
         "taxon_label" : {
            "transform" : [],
            "description" : "Taxon derived from GAF column 13 and ncbi_taxonomy.obo.",
            "display_name" : "Taxon",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "taxon_label",
            "property" : []
         },
         "type" : {
            "transform" : [],
            "description" : "Type class (GAF column 12).",
            "display_name" : "Type",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "type",
            "property" : []
         }
      },
      "document_category" : "bioentity",
      "weight" : "30",
      "_strict" : 0,
      "id" : "bioentity",
      "_outfile" : "/home/sjcarbon/local/src/git/amigo/metadata//bio-config.yaml"
   },
   "bbop_term_ac" : {
      "searchable_extension" : "_searchable",
      "result_weights" : "annotation_class^8.0 synonym^3.0 alternate_id^2.0",
      "filter_weights" : "annotation_class^8.0 synonym^3.0 alternate_id^2.0",
      "_infile" : "/home/sjcarbon/local/src/git/amigo/metadata//term-autocomplete-config.yaml",
      "display_name" : "Term autocomplete",
      "description" : "Easily find ontology classes in GO. For personality only--not a schema configuration.",
      "schema_generating" : "false",
      "boost_weights" : "annotation_class^5.0 annotation_class_label^5.0 synonym^1.0 alternate_id^1.0",
      "fields" : [
         {
            "transform" : [],
            "description" : "Term acc/ID.",
            "display_name" : "Acc",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "id",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "Term acc/ID.",
            "display_name" : "Term",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "annotation_class",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "Common term name.",
            "display_name" : "Term",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "annotation_class_label",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "Term synonyms.",
            "display_name" : "Synonyms",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "synonym",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "Alternate term id.",
            "display_name" : "Alt ID",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "alternate_id",
            "property" : []
         }
      ],
      "fields_hash" : {
         "synonym" : {
            "transform" : [],
            "description" : "Term synonyms.",
            "display_name" : "Synonyms",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "synonym",
            "property" : []
         },
         "annotation_class_label" : {
            "transform" : [],
            "description" : "Common term name.",
            "display_name" : "Term",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "annotation_class_label",
            "property" : []
         },
         "alternate_id" : {
            "transform" : [],
            "description" : "Alternate term id.",
            "display_name" : "Alt ID",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "alternate_id",
            "property" : []
         },
         "annotation_class" : {
            "transform" : [],
            "description" : "Term acc/ID.",
            "display_name" : "Term",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "annotation_class",
            "property" : []
         },
         "id" : {
            "transform" : [],
            "description" : "Term acc/ID.",
            "display_name" : "Acc",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "id",
            "property" : []
         }
      },
      "document_category" : "ontology_class",
      "weight" : "-20",
      "_strict" : 0,
      "id" : "bbop_term_ac",
      "_outfile" : "/home/sjcarbon/local/src/git/amigo/metadata//term-autocomplete-config.yaml"
   },
   "annotation" : {
      "searchable_extension" : "_searchable",
      "result_weights" : "bioentity^7.0 bioentity_name^6.0 qualifier^5.0 annotation_class^4.7 annotation_extension_json^4.5 source^4.0 taxon^3.0 evidence_type^2.5 evidence_with^2.0 panther_family^1.5 bioentity_isoform^0.5 reference^0.25",
      "filter_weights" : "source^7.0 assigned_by^6.5 aspect^6.25 evidence_type_closure^6.0 panther_family_label^5.5 qualifier^5.25 taxon_closure_label^5.0 annotation_class_label^4.5 regulates_closure_label^3.0 annotation_extension_class_closure_label^2.0",
      "_infile" : "/home/sjcarbon/local/src/git/amigo/metadata//ann-config.yaml",
      "display_name" : "Annotations",
      "description" : "A description of annotations for GOlr and AmiGO.",
      "schema_generating" : "true",
      "boost_weights" : "annotation_class^2.0 annotation_class_label^1.0 bioentity^2.0 bioentity_label^1.0 bioentity_name^1.0 annotation_extension_class^2.0 annotation_extension_class_label^1.0 reference^1.0 panther_family^1.0 panther_family_label^1.0 bioentity_isoform^1.0",
      "fields" : [
         {
            "transform" : [],
            "description" : "A unique (and internal) combination of bioentity and ontology class.",
            "display_name" : "Acc",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "id",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "GAF column 1 (database source).",
            "display_name" : "Source",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "source",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "GAF column 12 (type class id).",
            "display_name" : "Type class id",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "type",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "GAF column 14 (date of assignment).",
            "display_name" : "Date",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "date",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "GAF column 15 (assigned by).",
            "display_name" : "Assigned by",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "assigned_by",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "Rational for redundancy of annotation.",
            "display_name" : "Redundant for",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "is_redundant_for",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "GAF column 13 (taxon).",
            "display_name" : "Taxon",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "taxon",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "Taxon derived from GAF column 13 and ncbi_taxonomy.obo.",
            "display_name" : "Taxon",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "taxon_label",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "Taxon IDs derived from GAF column 13 and ncbi_taxonomy.obo.",
            "display_name" : "Taxon (IDs)",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "taxon_closure",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "Taxon label closure derived from GAF column 13 and ncbi_taxonomy.obo.",
            "display_name" : "Taxon",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "taxon_closure_label",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "Secondary taxon.",
            "display_name" : "Secondary taxon",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "secondary_taxon",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "Secondary taxon.",
            "display_name" : "Secondary taxon",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "secondary_taxon_label",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "Secondary taxon closure.",
            "display_name" : "Secondary taxon (IDs)",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "secondary_taxon_closure",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "Secondary taxon closure.",
            "display_name" : "Secondary taxon",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "secondary_taxon_closure_label",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "Closure of ids/accs over isa and partof.",
            "display_name" : "Involved in (IDs)",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "isa_partof_closure",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "Closure of labels over isa and partof.",
            "display_name" : "Involved in",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "isa_partof_closure_label",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "Closure of ids/accs over regulates.",
            "display_name" : "Inferred annotation (IDs)",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "regulates_closure",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "Closure of labels over regulates.",
            "display_name" : "Inferred annotation",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "regulates_closure_label",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "Closure of ids/accs over has_participant.",
            "display_name" : "Has participant (IDs)",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "has_participant_closure",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "Closure of labels over has_participant.",
            "display_name" : "Has participant",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "has_participant_closure_label",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "GAF column 11: gene product synonyms.",
            "display_name" : "Synonym",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "synonym",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "GAF column 1 + columns 2.",
            "display_name" : "Gene/Product",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "bioentity",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "GAF column 3: bioentity label.",
            "display_name" : "Gene/product label",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "bioentity_label",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "The full name of the gene product.",
            "display_name" : "Gene/Product name",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "bioentity_name",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "The bioentity ID used at the database of origin.",
            "display_name" : "This should not be displayed",
            "indexed" : "false",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "bioentity_internal_id",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "Annotation qualifier (GAF column 4).",
            "display_name" : "Qualifier",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "qualifier",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "Direct annotations (GAF column 5).",
            "display_name" : "Direct annotation",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "annotation_class",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "Direct annotations (GAF column 5).",
            "display_name" : "Direct annotation",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "annotation_class_label",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "GAF column 9: Ontology aspect.",
            "display_name" : "Ontology (aspect)",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "aspect",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "GAF column 17: Bioentity isoform.",
            "display_name" : "Isoform",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "bioentity_isoform",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "GAF column 7: evidence type.",
            "display_name" : "Evidence",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "evidence_type",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "All evidence (evidence closure) for this annotation",
            "display_name" : "Evidence type",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "evidence_type_closure",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "GAF column 8: with/from.",
            "display_name" : "Evidence with",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "evidence_with",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "GAF column 6: database reference.",
            "display_name" : "Reference",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "reference",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "GAF column 16: extension class for the annotation.",
            "display_name" : "Annotation extension",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "annotation_extension_class",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "Annotation extension.",
            "display_name" : "Annotation extension (labels)",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "annotation_extension_class_label",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "Annotation extension.",
            "display_name" : "Annotation extension (IDs)",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "annotation_extension_class_closure",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "Annotation extension.",
            "display_name" : "Annotation extension",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "annotation_extension_class_closure_label",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "A special JSON blob for GAF column 16.",
            "display_name" : "Annotation extension",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "annotation_extension_json",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "PANTHER family IDs that are associated with this entity.",
            "display_name" : "PANTHER family",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "panther_family",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "PANTHER families that are associated with this entity.",
            "display_name" : "PANTHER family",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "panther_family_label",
            "property" : []
         }
      ],
      "fields_hash" : {
         "panther_family_label" : {
            "transform" : [],
            "description" : "PANTHER families that are associated with this entity.",
            "display_name" : "PANTHER family",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "panther_family_label",
            "property" : []
         },
         "annotation_extension_class_closure_label" : {
            "transform" : [],
            "description" : "Annotation extension.",
            "display_name" : "Annotation extension",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "annotation_extension_class_closure_label",
            "property" : []
         },
         "bioentity_label" : {
            "transform" : [],
            "description" : "GAF column 3: bioentity label.",
            "display_name" : "Gene/product label",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "bioentity_label",
            "property" : []
         },
         "date" : {
            "transform" : [],
            "description" : "GAF column 14 (date of assignment).",
            "display_name" : "Date",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "date",
            "property" : []
         },
         "has_participant_closure" : {
            "transform" : [],
            "description" : "Closure of ids/accs over has_participant.",
            "display_name" : "Has participant (IDs)",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "has_participant_closure",
            "property" : []
         },
         "secondary_taxon_label" : {
            "transform" : [],
            "description" : "Secondary taxon.",
            "display_name" : "Secondary taxon",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "secondary_taxon_label",
            "property" : []
         },
         "bioentity_internal_id" : {
            "transform" : [],
            "description" : "The bioentity ID used at the database of origin.",
            "display_name" : "This should not be displayed",
            "indexed" : "false",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "bioentity_internal_id",
            "property" : []
         },
         "bioentity_name" : {
            "transform" : [],
            "description" : "The full name of the gene product.",
            "display_name" : "Gene/Product name",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "bioentity_name",
            "property" : []
         },
         "evidence_type" : {
            "transform" : [],
            "description" : "GAF column 7: evidence type.",
            "display_name" : "Evidence",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "evidence_type",
            "property" : []
         },
         "id" : {
            "transform" : [],
            "description" : "A unique (and internal) combination of bioentity and ontology class.",
            "display_name" : "Acc",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "id",
            "property" : []
         },
         "annotation_extension_class_label" : {
            "transform" : [],
            "description" : "Annotation extension.",
            "display_name" : "Annotation extension (labels)",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "annotation_extension_class_label",
            "property" : []
         },
         "isa_partof_closure_label" : {
            "transform" : [],
            "description" : "Closure of labels over isa and partof.",
            "display_name" : "Involved in",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "isa_partof_closure_label",
            "property" : []
         },
         "annotation_class" : {
            "transform" : [],
            "description" : "Direct annotations (GAF column 5).",
            "display_name" : "Direct annotation",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "annotation_class",
            "property" : []
         },
         "annotation_extension_json" : {
            "transform" : [],
            "description" : "A special JSON blob for GAF column 16.",
            "display_name" : "Annotation extension",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "annotation_extension_json",
            "property" : []
         },
         "has_participant_closure_label" : {
            "transform" : [],
            "description" : "Closure of labels over has_participant.",
            "display_name" : "Has participant",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "has_participant_closure_label",
            "property" : []
         },
         "synonym" : {
            "transform" : [],
            "description" : "GAF column 11: gene product synonyms.",
            "display_name" : "Synonym",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "synonym",
            "property" : []
         },
         "assigned_by" : {
            "transform" : [],
            "description" : "GAF column 15 (assigned by).",
            "display_name" : "Assigned by",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "assigned_by",
            "property" : []
         },
         "type" : {
            "transform" : [],
            "description" : "GAF column 12 (type class id).",
            "display_name" : "Type class id",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "type",
            "property" : []
         },
         "annotation_extension_class" : {
            "transform" : [],
            "description" : "GAF column 16: extension class for the annotation.",
            "display_name" : "Annotation extension",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "annotation_extension_class",
            "property" : []
         },
         "source" : {
            "transform" : [],
            "description" : "GAF column 1 (database source).",
            "display_name" : "Source",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "source",
            "property" : []
         },
         "panther_family" : {
            "transform" : [],
            "description" : "PANTHER family IDs that are associated with this entity.",
            "display_name" : "PANTHER family",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "panther_family",
            "property" : []
         },
         "taxon_closure_label" : {
            "transform" : [],
            "description" : "Taxon label closure derived from GAF column 13 and ncbi_taxonomy.obo.",
            "display_name" : "Taxon",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "taxon_closure_label",
            "property" : []
         },
         "qualifier" : {
            "transform" : [],
            "description" : "Annotation qualifier (GAF column 4).",
            "display_name" : "Qualifier",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "qualifier",
            "property" : []
         },
         "reference" : {
            "transform" : [],
            "description" : "GAF column 6: database reference.",
            "display_name" : "Reference",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "reference",
            "property" : []
         },
         "secondary_taxon_closure_label" : {
            "transform" : [],
            "description" : "Secondary taxon closure.",
            "display_name" : "Secondary taxon",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "secondary_taxon_closure_label",
            "property" : []
         },
         "taxon_closure" : {
            "transform" : [],
            "description" : "Taxon IDs derived from GAF column 13 and ncbi_taxonomy.obo.",
            "display_name" : "Taxon (IDs)",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "taxon_closure",
            "property" : []
         },
         "bioentity_isoform" : {
            "transform" : [],
            "description" : "GAF column 17: Bioentity isoform.",
            "display_name" : "Isoform",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "bioentity_isoform",
            "property" : []
         },
         "secondary_taxon_closure" : {
            "transform" : [],
            "description" : "Secondary taxon closure.",
            "display_name" : "Secondary taxon (IDs)",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "secondary_taxon_closure",
            "property" : []
         },
         "aspect" : {
            "transform" : [],
            "description" : "GAF column 9: Ontology aspect.",
            "display_name" : "Ontology (aspect)",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "aspect",
            "property" : []
         },
         "taxon" : {
            "transform" : [],
            "description" : "GAF column 13 (taxon).",
            "display_name" : "Taxon",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "taxon",
            "property" : []
         },
         "regulates_closure_label" : {
            "transform" : [],
            "description" : "Closure of labels over regulates.",
            "display_name" : "Inferred annotation",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "regulates_closure_label",
            "property" : []
         },
         "regulates_closure" : {
            "transform" : [],
            "description" : "Closure of ids/accs over regulates.",
            "display_name" : "Inferred annotation (IDs)",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "regulates_closure",
            "property" : []
         },
         "secondary_taxon" : {
            "transform" : [],
            "description" : "Secondary taxon.",
            "display_name" : "Secondary taxon",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "secondary_taxon",
            "property" : []
         },
         "isa_partof_closure" : {
            "transform" : [],
            "description" : "Closure of ids/accs over isa and partof.",
            "display_name" : "Involved in (IDs)",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "isa_partof_closure",
            "property" : []
         },
         "bioentity" : {
            "transform" : [],
            "description" : "GAF column 1 + columns 2.",
            "display_name" : "Gene/Product",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "bioentity",
            "property" : []
         },
         "taxon_label" : {
            "transform" : [],
            "description" : "Taxon derived from GAF column 13 and ncbi_taxonomy.obo.",
            "display_name" : "Taxon",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "taxon_label",
            "property" : []
         },
         "annotation_class_label" : {
            "transform" : [],
            "description" : "Direct annotations (GAF column 5).",
            "display_name" : "Direct annotation",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "annotation_class_label",
            "property" : []
         },
         "evidence_type_closure" : {
            "transform" : [],
            "description" : "All evidence (evidence closure) for this annotation",
            "display_name" : "Evidence type",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "evidence_type_closure",
            "property" : []
         },
         "evidence_with" : {
            "transform" : [],
            "description" : "GAF column 8: with/from.",
            "display_name" : "Evidence with",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "evidence_with",
            "property" : []
         },
         "is_redundant_for" : {
            "transform" : [],
            "description" : "Rational for redundancy of annotation.",
            "display_name" : "Redundant for",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "is_redundant_for",
            "property" : []
         },
         "annotation_extension_class_closure" : {
            "transform" : [],
            "description" : "Annotation extension.",
            "display_name" : "Annotation extension (IDs)",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "annotation_extension_class_closure",
            "property" : []
         }
      },
      "document_category" : "annotation",
      "weight" : "20",
      "_strict" : 0,
      "id" : "annotation",
      "_outfile" : "/home/sjcarbon/local/src/git/amigo/metadata//ann-config.yaml"
   },
   "family" : {
      "searchable_extension" : "_searchable",
      "result_weights" : "panther_family^5.0 bioentity_list^4.0",
      "filter_weights" : "bioentity_list_label^1.0",
      "_infile" : "/home/sjcarbon/local/src/git/amigo/metadata//protein-family-config.yaml",
      "display_name" : "Protein families",
      "description" : "Information about protein (PANTHER) families.",
      "schema_generating" : "true",
      "boost_weights" : "panther_family^2.0 panther_family_label^2.0 bioentity_list^1.0 bioentity_list_label^1.0",
      "fields" : [
         {
            "transform" : [],
            "description" : "Family ID.",
            "display_name" : "Acc",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "id",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "PANTHER family IDs that are associated with this entity.",
            "display_name" : "PANTHER family",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "panther_family",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "PANTHER families that are associated with this entity.",
            "display_name" : "PANTHER family",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "panther_family_label",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "JSON blob form of the phylogenic tree.",
            "display_name" : "This should not be displayed",
            "indexed" : "false",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "phylo_graph_json",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "Gene/products annotated with this protein family.",
            "display_name" : "Gene/products",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "bioentity_list",
            "property" : []
         },
         {
            "transform" : [],
            "description" : "Gene/products annotated with this protein family.",
            "display_name" : "Gene/products",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "bioentity_list_label",
            "property" : []
         }
      ],
      "fields_hash" : {
         "phylo_graph_json" : {
            "transform" : [],
            "description" : "JSON blob form of the phylogenic tree.",
            "display_name" : "This should not be displayed",
            "indexed" : "false",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "phylo_graph_json",
            "property" : []
         },
         "panther_family_label" : {
            "transform" : [],
            "description" : "PANTHER families that are associated with this entity.",
            "display_name" : "PANTHER family",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "panther_family_label",
            "property" : []
         },
         "panther_family" : {
            "transform" : [],
            "description" : "PANTHER family IDs that are associated with this entity.",
            "display_name" : "PANTHER family",
            "indexed" : "true",
            "searchable" : "true",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "panther_family",
            "property" : []
         },
         "bioentity_list" : {
            "transform" : [],
            "description" : "Gene/products annotated with this protein family.",
            "display_name" : "Gene/products",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "bioentity_list",
            "property" : []
         },
         "bioentity_list_label" : {
            "transform" : [],
            "description" : "Gene/products annotated with this protein family.",
            "display_name" : "Gene/products",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "multi",
            "type" : "string",
            "id" : "bioentity_list_label",
            "property" : []
         },
         "id" : {
            "transform" : [],
            "description" : "Family ID.",
            "display_name" : "Acc",
            "indexed" : "true",
            "searchable" : "false",
            "required" : "false",
            "cardinality" : "single",
            "type" : "string",
            "id" : "id",
            "property" : []
         }
      },
      "document_category" : "family",
      "weight" : "5",
      "_strict" : 0,
      "id" : "family",
      "_outfile" : "/home/sjcarbon/local/src/git/amigo/metadata//protein-family-config.yaml"
   }
};

