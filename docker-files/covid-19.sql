--
-- PostgreSQL database dump
--

-- Dumped from database version 9.6.17
-- Dumped by pg_dump version 9.6.17

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: allele; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.allele (
    id bigint NOT NULL,
    version bigint NOT NULL,
    bases character varying(255) NOT NULL,
    reference boolean NOT NULL,
    variant_id bigint
);


ALTER TABLE public.allele OWNER TO apollo;

--
-- Name: allele_info; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.allele_info (
    id bigint NOT NULL,
    version bigint NOT NULL,
    allele_id bigint NOT NULL,
    tag character varying(255) NOT NULL,
    value text NOT NULL
);


ALTER TABLE public.allele_info OWNER TO apollo;

--
-- Name: analysis; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.analysis (
    id bigint NOT NULL,
    version bigint NOT NULL,
    algorithm character varying(255) NOT NULL,
    description character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    program character varying(255) NOT NULL,
    program_version character varying(255) NOT NULL,
    source_name character varying(255) NOT NULL,
    sourceuri character varying(255) NOT NULL,
    source_version character varying(255) NOT NULL,
    time_executed timestamp without time zone NOT NULL
);


ALTER TABLE public.analysis OWNER TO apollo;

--
-- Name: analysis_feature; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.analysis_feature (
    id bigint NOT NULL,
    version bigint NOT NULL,
    analysis_id bigint NOT NULL,
    feature_id bigint NOT NULL,
    identity double precision NOT NULL,
    normalized_score double precision NOT NULL,
    raw_score double precision NOT NULL,
    significance double precision NOT NULL
);


ALTER TABLE public.analysis_feature OWNER TO apollo;

--
-- Name: analysis_property; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.analysis_property (
    id bigint NOT NULL,
    version bigint NOT NULL,
    analysis_id bigint NOT NULL,
    type_id bigint NOT NULL,
    value character varying(255) NOT NULL
);


ALTER TABLE public.analysis_property OWNER TO apollo;

--
-- Name: application_preference; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.application_preference (
    id bigint NOT NULL,
    version bigint NOT NULL,
    name character varying(255) NOT NULL,
    value character varying(255)
);


ALTER TABLE public.application_preference OWNER TO apollo;

--
-- Name: audit_log; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.audit_log (
    id bigint NOT NULL,
    actor character varying(255),
    class_name character varying(255),
    date_created timestamp without time zone NOT NULL,
    event_name character varying(255),
    last_updated timestamp without time zone NOT NULL,
    new_value character varying(255),
    old_value character varying(255),
    persisted_object_id character varying(255),
    persisted_object_version bigint,
    property_name character varying(255),
    uri character varying(255)
);


ALTER TABLE public.audit_log OWNER TO apollo;

--
-- Name: available_status; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.available_status (
    id bigint NOT NULL,
    version bigint NOT NULL,
    value character varying(255) NOT NULL
);


ALTER TABLE public.available_status OWNER TO apollo;

--
-- Name: available_status_feature_type; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.available_status_feature_type (
    available_status_feature_types_id bigint,
    feature_type_id bigint
);


ALTER TABLE public.available_status_feature_type OWNER TO apollo;

--
-- Name: canned_comment; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.canned_comment (
    id bigint NOT NULL,
    version bigint NOT NULL,
    comment character varying(255) NOT NULL,
    metadata character varying(255)
);


ALTER TABLE public.canned_comment OWNER TO apollo;

--
-- Name: canned_comment_feature_type; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.canned_comment_feature_type (
    canned_comment_feature_types_id bigint,
    feature_type_id bigint
);


ALTER TABLE public.canned_comment_feature_type OWNER TO apollo;

--
-- Name: canned_key; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.canned_key (
    id bigint NOT NULL,
    version bigint NOT NULL,
    label character varying(255) NOT NULL,
    metadata character varying(255)
);


ALTER TABLE public.canned_key OWNER TO apollo;

--
-- Name: canned_key_feature_type; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.canned_key_feature_type (
    canned_key_feature_types_id bigint,
    feature_type_id bigint
);


ALTER TABLE public.canned_key_feature_type OWNER TO apollo;

--
-- Name: canned_value; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.canned_value (
    id bigint NOT NULL,
    version bigint NOT NULL,
    label character varying(255) NOT NULL,
    metadata character varying(255)
);


ALTER TABLE public.canned_value OWNER TO apollo;

--
-- Name: canned_value_feature_type; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.canned_value_feature_type (
    canned_value_feature_types_id bigint,
    feature_type_id bigint
);


ALTER TABLE public.canned_value_feature_type OWNER TO apollo;

--
-- Name: custom_domain_mapping; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.custom_domain_mapping (
    id bigint NOT NULL,
    version bigint NOT NULL,
    alternate_cv_term character varying(255),
    cv_term character varying(255) NOT NULL,
    is_transcript boolean NOT NULL,
    ontology_id character varying(255) NOT NULL
);


ALTER TABLE public.custom_domain_mapping OWNER TO apollo;

--
-- Name: cv; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.cv (
    id bigint NOT NULL,
    version bigint NOT NULL,
    definition character varying(255) NOT NULL,
    name character varying(255) NOT NULL
);


ALTER TABLE public.cv OWNER TO apollo;

--
-- Name: cvterm; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.cvterm (
    id bigint NOT NULL,
    version bigint NOT NULL,
    cv_id bigint NOT NULL,
    dbxref_id bigint NOT NULL,
    definition character varying(255) NOT NULL,
    is_obsolete integer NOT NULL,
    is_relationship_type integer NOT NULL,
    name character varying(255) NOT NULL
);


ALTER TABLE public.cvterm OWNER TO apollo;

--
-- Name: cvterm_path; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.cvterm_path (
    id bigint NOT NULL,
    version bigint NOT NULL,
    cv_id bigint NOT NULL,
    objectcvterm_id bigint NOT NULL,
    path_distance integer NOT NULL,
    subjectcvterm_id bigint NOT NULL,
    type_id bigint NOT NULL
);


ALTER TABLE public.cvterm_path OWNER TO apollo;

--
-- Name: cvterm_relationship; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.cvterm_relationship (
    id bigint NOT NULL,
    version bigint NOT NULL,
    objectcvterm_id bigint NOT NULL,
    subjectcvterm_id bigint NOT NULL,
    type_id bigint NOT NULL
);


ALTER TABLE public.cvterm_relationship OWNER TO apollo;

--
-- Name: data_adapter; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.data_adapter (
    id bigint NOT NULL,
    version bigint NOT NULL,
    export_source_genomic_sequence boolean,
    feature_type_string character varying(255),
    implementation_class character varying(255),
    data_adapter_key character varying(255) NOT NULL,
    options character varying(255),
    permission character varying(255) NOT NULL,
    source character varying(255),
    temp_directory character varying(255)
);


ALTER TABLE public.data_adapter OWNER TO apollo;

--
-- Name: data_adapter_data_adapter; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.data_adapter_data_adapter (
    data_adapter_data_adapters_id bigint,
    data_adapter_id bigint
);


ALTER TABLE public.data_adapter_data_adapter OWNER TO apollo;

--
-- Name: databasechangelog; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.databasechangelog (
    id character varying(63) NOT NULL,
    author character varying(63) NOT NULL,
    filename character varying(200) NOT NULL,
    dateexecuted timestamp with time zone NOT NULL,
    orderexecuted integer NOT NULL,
    exectype character varying(10) NOT NULL,
    md5sum character varying(35),
    description character varying(255),
    comments character varying(255),
    tag character varying(255),
    liquibase character varying(20)
);


ALTER TABLE public.databasechangelog OWNER TO apollo;

--
-- Name: databasechangeloglock; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.databasechangeloglock (
    id integer NOT NULL,
    locked boolean NOT NULL,
    lockgranted timestamp with time zone,
    lockedby character varying(255)
);


ALTER TABLE public.databasechangeloglock OWNER TO apollo;

--
-- Name: db; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.db (
    id bigint NOT NULL,
    version bigint NOT NULL,
    description character varying(255),
    name character varying(255) NOT NULL,
    url character varying(255),
    url_prefix character varying(255)
);


ALTER TABLE public.db OWNER TO apollo;

--
-- Name: dbxref; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.dbxref (
    id bigint NOT NULL,
    version character varying(255),
    accession character varying(255) NOT NULL,
    db_id bigint NOT NULL,
    description character varying(255)
);


ALTER TABLE public.dbxref OWNER TO apollo;

--
-- Name: dbxref_property; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.dbxref_property (
    id bigint NOT NULL,
    version bigint NOT NULL,
    dbxref_id bigint NOT NULL,
    rank integer NOT NULL,
    type_id bigint NOT NULL,
    value character varying(255) NOT NULL
);


ALTER TABLE public.dbxref_property OWNER TO apollo;

--
-- Name: environment; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.environment (
    id bigint NOT NULL,
    version bigint NOT NULL,
    description character varying(255) NOT NULL,
    uniquename character varying(255) NOT NULL
);


ALTER TABLE public.environment OWNER TO apollo;

--
-- Name: environmentcvterm; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.environmentcvterm (
    id bigint NOT NULL,
    version bigint NOT NULL,
    cvterm_id bigint NOT NULL,
    environment_id bigint NOT NULL
);


ALTER TABLE public.environmentcvterm OWNER TO apollo;

--
-- Name: feature; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.feature (
    id bigint NOT NULL,
    version bigint NOT NULL,
    date_created timestamp without time zone,
    dbxref_id bigint,
    description text,
    is_analysis boolean NOT NULL,
    is_obsolete boolean NOT NULL,
    last_updated timestamp without time zone,
    md5checksum character varying(255),
    name text NOT NULL,
    sequence_length integer,
    status_id bigint,
    symbol character varying(255),
    unique_name character varying(255) NOT NULL,
    class character varying(255) NOT NULL,
    analysis_feature_id bigint,
    alternate_cv_term character varying(255),
    class_name character varying(255),
    custom_alternate_cv_term character varying(255),
    custom_class_name character varying(255),
    custom_cv_term character varying(255),
    custom_ontology_id character varying(255),
    cv_term character varying(255),
    meta_data text,
    ontology_id character varying(255),
    alteration_residue character varying(255),
    deletion_length integer,
    reference_allele_id bigint
);


ALTER TABLE public.feature OWNER TO apollo;

--
-- Name: feature_dbxref; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.feature_dbxref (
    feature_featuredbxrefs_id bigint,
    dbxref_id bigint
);


ALTER TABLE public.feature_dbxref OWNER TO apollo;

--
-- Name: feature_event; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.feature_event (
    id bigint NOT NULL,
    version bigint NOT NULL,
    child_id bigint,
    child_split_id bigint,
    current boolean NOT NULL,
    date_created timestamp without time zone NOT NULL,
    editor_id bigint,
    last_updated timestamp without time zone NOT NULL,
    name text NOT NULL,
    new_features_json_array text,
    old_features_json_array text,
    operation character varying(255) NOT NULL,
    original_json_command text,
    parent_id bigint,
    parent_merge_id bigint,
    unique_name character varying(255) NOT NULL
);


ALTER TABLE public.feature_event OWNER TO apollo;

--
-- Name: feature_feature_phenotypes; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.feature_feature_phenotypes (
    feature_id bigint NOT NULL,
    phenotype_id bigint NOT NULL
);


ALTER TABLE public.feature_feature_phenotypes OWNER TO apollo;

--
-- Name: feature_genotype; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.feature_genotype (
    id bigint NOT NULL,
    version bigint NOT NULL,
    cgroup integer NOT NULL,
    chromosome_feature_id bigint NOT NULL,
    cvterm_id bigint NOT NULL,
    feature_id bigint NOT NULL,
    feature_genotype_id integer NOT NULL,
    genotype_id bigint NOT NULL,
    rank integer NOT NULL
);


ALTER TABLE public.feature_genotype OWNER TO apollo;

--
-- Name: feature_grails_user; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.feature_grails_user (
    feature_owners_id bigint,
    user_id bigint
);


ALTER TABLE public.feature_grails_user OWNER TO apollo;

--
-- Name: feature_location; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.feature_location (
    id bigint NOT NULL,
    version bigint NOT NULL,
    feature_id bigint NOT NULL,
    fmax integer NOT NULL,
    fmin integer NOT NULL,
    is_fmax_partial boolean NOT NULL,
    is_fmin_partial boolean NOT NULL,
    locgroup integer NOT NULL,
    phase integer,
    rank integer NOT NULL,
    residue_info character varying(255),
    sequence_id bigint NOT NULL,
    strand integer
);


ALTER TABLE public.feature_location OWNER TO apollo;

--
-- Name: feature_location_publication; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.feature_location_publication (
    feature_location_feature_location_publications_id bigint,
    publication_id bigint
);


ALTER TABLE public.feature_location_publication OWNER TO apollo;

--
-- Name: feature_property; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.feature_property (
    id bigint NOT NULL,
    version bigint NOT NULL,
    feature_id bigint NOT NULL,
    rank integer NOT NULL,
    tag character varying(255),
    type_id bigint,
    value text NOT NULL,
    class character varying(255) NOT NULL
);


ALTER TABLE public.feature_property OWNER TO apollo;

--
-- Name: feature_property_publication; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.feature_property_publication (
    feature_property_feature_property_publications_id bigint,
    publication_id bigint
);


ALTER TABLE public.feature_property_publication OWNER TO apollo;

--
-- Name: feature_publication; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.feature_publication (
    feature_feature_publications_id bigint,
    publication_id bigint
);


ALTER TABLE public.feature_publication OWNER TO apollo;

--
-- Name: feature_relationship; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.feature_relationship (
    id bigint NOT NULL,
    version bigint NOT NULL,
    child_feature_id bigint NOT NULL,
    parent_feature_id bigint NOT NULL,
    rank integer NOT NULL,
    value character varying(255)
);


ALTER TABLE public.feature_relationship OWNER TO apollo;

--
-- Name: feature_relationship_feature_property; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.feature_relationship_feature_property (
    feature_relationship_feature_relationship_properties_id bigint,
    feature_property_id bigint
);


ALTER TABLE public.feature_relationship_feature_property OWNER TO apollo;

--
-- Name: feature_relationship_publication; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.feature_relationship_publication (
    feature_relationship_feature_relationship_publications_id bigint,
    publication_id bigint
);


ALTER TABLE public.feature_relationship_publication OWNER TO apollo;

--
-- Name: feature_synonym; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.feature_synonym (
    id bigint NOT NULL,
    version bigint NOT NULL,
    feature_id bigint NOT NULL,
    is_current boolean,
    is_internal boolean,
    publication_id bigint,
    synonym_id bigint NOT NULL
);


ALTER TABLE public.feature_synonym OWNER TO apollo;

--
-- Name: feature_type; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.feature_type (
    id bigint NOT NULL,
    version bigint NOT NULL,
    display character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    ontology_id character varying(255) NOT NULL,
    type character varying(255) NOT NULL
);


ALTER TABLE public.feature_type OWNER TO apollo;

--
-- Name: featurecvterm; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.featurecvterm (
    id bigint NOT NULL,
    version bigint NOT NULL,
    cvterm_id bigint NOT NULL,
    feature_id bigint NOT NULL,
    is_not boolean NOT NULL,
    publication_id bigint NOT NULL,
    rank integer NOT NULL
);


ALTER TABLE public.featurecvterm OWNER TO apollo;

--
-- Name: featurecvterm_dbxref; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.featurecvterm_dbxref (
    featurecvterm_featurecvtermdbxrefs_id bigint,
    dbxref_id bigint
);


ALTER TABLE public.featurecvterm_dbxref OWNER TO apollo;

--
-- Name: featurecvterm_publication; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.featurecvterm_publication (
    featurecvterm_featurecvterm_publications_id bigint,
    publication_id bigint
);


ALTER TABLE public.featurecvterm_publication OWNER TO apollo;

--
-- Name: gene_product; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.gene_product (
    id bigint NOT NULL,
    version bigint NOT NULL,
    alternate boolean NOT NULL,
    date_created timestamp without time zone NOT NULL,
    evidence_ref character varying(255) NOT NULL,
    evidence_ref_label character varying(255),
    feature_id bigint NOT NULL,
    last_updated timestamp without time zone NOT NULL,
    notes_array character varying(255),
    product_name character varying(255) NOT NULL,
    reference character varying(255) NOT NULL,
    with_or_from_array character varying(255)
);


ALTER TABLE public.gene_product OWNER TO apollo;

--
-- Name: gene_product_grails_user; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.gene_product_grails_user (
    gene_product_owners_id bigint,
    user_id bigint
);


ALTER TABLE public.gene_product_grails_user OWNER TO apollo;

--
-- Name: genotype; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.genotype (
    id bigint NOT NULL,
    version bigint NOT NULL,
    description character varying(255) NOT NULL,
    genotype_id integer NOT NULL,
    name character varying(255) NOT NULL,
    unique_name character varying(255) NOT NULL
);


ALTER TABLE public.genotype OWNER TO apollo;

--
-- Name: go_annotation; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.go_annotation (
    id bigint NOT NULL,
    version bigint NOT NULL,
    aspect character varying(255) NOT NULL,
    date_created timestamp without time zone NOT NULL,
    evidence_ref character varying(255) NOT NULL,
    evidence_ref_label character varying(255),
    feature_id bigint NOT NULL,
    gene_product_relationship_ref character varying(255),
    go_ref character varying(255) NOT NULL,
    go_ref_label character varying(255),
    last_updated timestamp without time zone NOT NULL,
    negate boolean NOT NULL,
    notes_array character varying(255),
    reference character varying(255) NOT NULL,
    with_or_from_array character varying(255)
);


ALTER TABLE public.go_annotation OWNER TO apollo;

--
-- Name: go_annotation_grails_user; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.go_annotation_grails_user (
    go_annotation_owners_id bigint,
    user_id bigint
);


ALTER TABLE public.go_annotation_grails_user OWNER TO apollo;

--
-- Name: grails_user; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.grails_user (
    id bigint NOT NULL,
    version bigint NOT NULL,
    first_name character varying(255) NOT NULL,
    inactive boolean NOT NULL,
    last_name character varying(255) NOT NULL,
    metadata character varying(255),
    password_hash character varying(255) NOT NULL,
    username character varying(255) NOT NULL
);


ALTER TABLE public.grails_user OWNER TO apollo;

--
-- Name: grails_user_roles; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.grails_user_roles (
    role_id bigint NOT NULL,
    user_id bigint NOT NULL
);


ALTER TABLE public.grails_user_roles OWNER TO apollo;

--
-- Name: hibernate_sequence; Type: SEQUENCE; Schema: public; Owner: apollo
--

CREATE SEQUENCE public.hibernate_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.hibernate_sequence OWNER TO apollo;

--
-- Name: operation; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.operation (
    id bigint NOT NULL,
    version bigint NOT NULL,
    attributes text,
    feature_unique_name character varying(255) NOT NULL,
    new_features text,
    old_features text,
    operation_type character varying(255) NOT NULL
);


ALTER TABLE public.operation OWNER TO apollo;

--
-- Name: organism; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.organism (
    id bigint NOT NULL,
    version bigint NOT NULL,
    abbreviation character varying(255),
    blatdb character varying(255),
    comment character varying(255),
    common_name character varying(255) NOT NULL,
    data_added_via_web_services boolean,
    directory character varying(255) NOT NULL,
    genome_fasta character varying(255),
    genome_fasta_index character varying(255),
    genus character varying(255),
    metadata text,
    non_default_translation_table character varying(255),
    obsolete boolean NOT NULL,
    official_gene_set_track character varying(255),
    public_mode boolean DEFAULT true NOT NULL,
    species character varying(255),
    valid boolean
);


ALTER TABLE public.organism OWNER TO apollo;

--
-- Name: organism_filter; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.organism_filter (
    id bigint NOT NULL,
    version bigint NOT NULL,
    organism_id bigint NOT NULL,
    class character varying(255) NOT NULL,
    canned_value_id bigint,
    canned_comment_id bigint,
    suggested_name_id bigint,
    canned_key_id bigint,
    available_status_id bigint
);


ALTER TABLE public.organism_filter OWNER TO apollo;

--
-- Name: organism_organism_property; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.organism_organism_property (
    organism_organism_properties_id bigint,
    organism_property_id bigint
);


ALTER TABLE public.organism_organism_property OWNER TO apollo;

--
-- Name: organism_property; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.organism_property (
    id bigint NOT NULL,
    version bigint NOT NULL,
    abbreviation character varying(255) NOT NULL,
    comment character varying(255) NOT NULL,
    common_name character varying(255) NOT NULL,
    genus character varying(255) NOT NULL,
    organism_id integer NOT NULL,
    species character varying(255) NOT NULL
);


ALTER TABLE public.organism_property OWNER TO apollo;

--
-- Name: organism_property_organism_property; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.organism_property_organism_property (
    organism_property_organism_properties_id bigint,
    organism_property_id bigint
);


ALTER TABLE public.organism_property_organism_property OWNER TO apollo;

--
-- Name: organism_property_organismdbxref; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.organism_property_organismdbxref (
    organism_property_organismdbxrefs_id bigint,
    organismdbxref_id bigint
);


ALTER TABLE public.organism_property_organismdbxref OWNER TO apollo;

--
-- Name: organismdbxref; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.organismdbxref (
    id bigint NOT NULL,
    version bigint NOT NULL,
    dbxref_id bigint NOT NULL,
    organism_id bigint NOT NULL
);


ALTER TABLE public.organismdbxref OWNER TO apollo;

--
-- Name: part_of; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.part_of (
    id bigint NOT NULL,
    version bigint NOT NULL
);


ALTER TABLE public.part_of OWNER TO apollo;

--
-- Name: permission; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.permission (
    id bigint NOT NULL,
    version bigint NOT NULL,
    organism_id bigint NOT NULL,
    class character varying(255) NOT NULL,
    group_id bigint,
    permissions character varying(255),
    track_visibilities character varying(255),
    user_id bigint
);


ALTER TABLE public.permission OWNER TO apollo;

--
-- Name: phenotype; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.phenotype (
    id bigint NOT NULL,
    version bigint NOT NULL,
    assay_id bigint NOT NULL,
    attribute_id bigint NOT NULL,
    cvalue_id bigint NOT NULL,
    observable_id bigint NOT NULL,
    unique_name character varying(255) NOT NULL,
    value character varying(255) NOT NULL
);


ALTER TABLE public.phenotype OWNER TO apollo;

--
-- Name: phenotype_cvterm; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.phenotype_cvterm (
    phenotype_phenotypecvterms_id bigint,
    cvterm_id bigint
);


ALTER TABLE public.phenotype_cvterm OWNER TO apollo;

--
-- Name: phenotype_description; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.phenotype_description (
    id bigint NOT NULL,
    version bigint NOT NULL,
    description character varying(255) NOT NULL,
    environment_id bigint NOT NULL,
    genotype_id bigint NOT NULL,
    publication_id bigint NOT NULL,
    type_id bigint NOT NULL
);


ALTER TABLE public.phenotype_description OWNER TO apollo;

--
-- Name: phenotype_statement; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.phenotype_statement (
    id bigint NOT NULL,
    version bigint NOT NULL,
    environment_id bigint NOT NULL,
    genotype_id bigint NOT NULL,
    phenotype_id bigint NOT NULL,
    publication_id bigint NOT NULL,
    type_id bigint NOT NULL
);


ALTER TABLE public.phenotype_statement OWNER TO apollo;

--
-- Name: preference; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.preference (
    id bigint NOT NULL,
    version bigint NOT NULL,
    client_token character varying(255) NOT NULL,
    date_created timestamp without time zone NOT NULL,
    domain character varying(255),
    last_updated timestamp without time zone NOT NULL,
    name character varying(255),
    preferences_string character varying(255),
    class character varying(255) NOT NULL,
    user_id bigint,
    current_organism boolean,
    endbp integer,
    native_track_list boolean,
    organism_id bigint,
    sequence_id bigint,
    startbp integer
);


ALTER TABLE public.preference OWNER TO apollo;

--
-- Name: provenance; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.provenance (
    id bigint NOT NULL,
    version bigint NOT NULL,
    date_created timestamp without time zone NOT NULL,
    evidence_ref character varying(255) NOT NULL,
    evidence_ref_label character varying(255),
    feature_id bigint NOT NULL,
    field character varying(255) NOT NULL,
    last_updated timestamp without time zone NOT NULL,
    notes_array character varying(255),
    reference character varying(255) NOT NULL,
    with_or_from_array character varying(255)
);


ALTER TABLE public.provenance OWNER TO apollo;

--
-- Name: provenance_grails_user; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.provenance_grails_user (
    provenance_owners_id bigint,
    user_id bigint
);


ALTER TABLE public.provenance_grails_user OWNER TO apollo;

--
-- Name: proxy; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.proxy (
    id bigint NOT NULL,
    version bigint NOT NULL,
    active boolean DEFAULT true NOT NULL,
    fallback_order integer,
    last_fail timestamp without time zone,
    last_success timestamp without time zone,
    reference_url character varying(255) NOT NULL,
    target_url character varying(255) NOT NULL
);


ALTER TABLE public.proxy OWNER TO apollo;

--
-- Name: publication; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.publication (
    id bigint NOT NULL,
    version bigint NOT NULL,
    is_obsolete boolean NOT NULL,
    issue character varying(255) NOT NULL,
    mini_reference character varying(255) NOT NULL,
    pages character varying(255) NOT NULL,
    publication_place character varying(255) NOT NULL,
    publication_year character varying(255) NOT NULL,
    publisher character varying(255) NOT NULL,
    series_name character varying(255) NOT NULL,
    title character varying(255) NOT NULL,
    type_id bigint NOT NULL,
    unique_name character varying(255) NOT NULL,
    volume character varying(255) NOT NULL,
    volume_title character varying(255) NOT NULL
);


ALTER TABLE public.publication OWNER TO apollo;

--
-- Name: publication_author; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.publication_author (
    id bigint NOT NULL,
    version bigint NOT NULL,
    editor boolean NOT NULL,
    given_names character varying(255) NOT NULL,
    publication_id bigint NOT NULL,
    rank integer NOT NULL,
    suffix character varying(255) NOT NULL,
    surname character varying(255) NOT NULL
);


ALTER TABLE public.publication_author OWNER TO apollo;

--
-- Name: publication_relationship; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.publication_relationship (
    id bigint NOT NULL,
    version bigint NOT NULL,
    object_publication_id bigint NOT NULL,
    subject_publication_id bigint NOT NULL,
    type_id bigint NOT NULL
);


ALTER TABLE public.publication_relationship OWNER TO apollo;

--
-- Name: publicationdbxref; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.publicationdbxref (
    id bigint NOT NULL,
    version bigint NOT NULL,
    dbxref_id bigint NOT NULL,
    is_current boolean NOT NULL,
    publication_id bigint NOT NULL
);


ALTER TABLE public.publicationdbxref OWNER TO apollo;

--
-- Name: role; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.role (
    id bigint NOT NULL,
    version bigint NOT NULL,
    name character varying(255) NOT NULL,
    rank integer
);


ALTER TABLE public.role OWNER TO apollo;

--
-- Name: role_permissions; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.role_permissions (
    role_id bigint,
    permissions_string character varying(255)
);


ALTER TABLE public.role_permissions OWNER TO apollo;

--
-- Name: search_tool; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.search_tool (
    id bigint NOT NULL,
    version bigint NOT NULL,
    binary_path character varying(255) NOT NULL,
    database_path character varying(255) NOT NULL,
    implementation_class character varying(255) NOT NULL,
    search_key character varying(255) NOT NULL,
    options character varying(255) NOT NULL,
    remove_temp_directory boolean NOT NULL,
    tmp_dir character varying(255) NOT NULL
);


ALTER TABLE public.search_tool OWNER TO apollo;

--
-- Name: sequence; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.sequence (
    id bigint NOT NULL,
    version bigint NOT NULL,
    sequence_end integer NOT NULL,
    length integer NOT NULL,
    name character varying(255) NOT NULL,
    organism_id bigint,
    seq_chunk_size integer,
    sequence_start integer NOT NULL
);


ALTER TABLE public.sequence OWNER TO apollo;

--
-- Name: sequence_cache; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.sequence_cache (
    id bigint NOT NULL,
    version bigint NOT NULL,
    feature_name character varying(255),
    fmax bigint,
    fmin bigint,
    organism_name character varying(255) NOT NULL,
    param_map text,
    response text NOT NULL,
    sequence_name text NOT NULL,
    type character varying(255)
);


ALTER TABLE public.sequence_cache OWNER TO apollo;

--
-- Name: sequence_chunk; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.sequence_chunk (
    id bigint NOT NULL,
    version bigint NOT NULL,
    chunk_number integer NOT NULL,
    residue text NOT NULL,
    sequence_id bigint NOT NULL
);


ALTER TABLE public.sequence_chunk OWNER TO apollo;

--
-- Name: server_data; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.server_data (
    id bigint NOT NULL,
    version bigint NOT NULL,
    date_created timestamp without time zone NOT NULL,
    last_updated timestamp without time zone NOT NULL,
    name character varying(255) NOT NULL
);


ALTER TABLE public.server_data OWNER TO apollo;

--
-- Name: suggested_name; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.suggested_name (
    id bigint NOT NULL,
    version bigint NOT NULL,
    metadata character varying(255),
    name character varying(255) NOT NULL
);


ALTER TABLE public.suggested_name OWNER TO apollo;

--
-- Name: suggested_name_feature_type; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.suggested_name_feature_type (
    suggested_name_feature_types_id bigint,
    feature_type_id bigint
);


ALTER TABLE public.suggested_name_feature_type OWNER TO apollo;

--
-- Name: synonym; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.synonym (
    id bigint NOT NULL,
    version bigint NOT NULL,
    name character varying(255) NOT NULL
);


ALTER TABLE public.synonym OWNER TO apollo;

--
-- Name: track_cache; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.track_cache (
    id bigint NOT NULL,
    version bigint NOT NULL,
    feature_name character varying(255),
    fmax bigint,
    fmin bigint,
    organism_name character varying(255) NOT NULL,
    param_map text,
    response text NOT NULL,
    sequence_name text NOT NULL,
    track_name text NOT NULL,
    type character varying(255)
);


ALTER TABLE public.track_cache OWNER TO apollo;

--
-- Name: user_group; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.user_group (
    id bigint NOT NULL,
    version bigint NOT NULL,
    metadata text,
    name character varying(255) NOT NULL,
    public_group boolean NOT NULL
);


ALTER TABLE public.user_group OWNER TO apollo;

--
-- Name: user_group_admin; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.user_group_admin (
    user_id bigint NOT NULL,
    user_group_id bigint NOT NULL
);


ALTER TABLE public.user_group_admin OWNER TO apollo;

--
-- Name: user_group_users; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.user_group_users (
    user_id bigint NOT NULL,
    user_group_id bigint NOT NULL
);


ALTER TABLE public.user_group_users OWNER TO apollo;

--
-- Name: variant_info; Type: TABLE; Schema: public; Owner: apollo
--

CREATE TABLE public.variant_info (
    id bigint NOT NULL,
    version bigint NOT NULL,
    tag character varying(255) NOT NULL,
    value text,
    variant_id bigint NOT NULL
);


ALTER TABLE public.variant_info OWNER TO apollo;

--
-- Data for Name: allele; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.allele (id, version, bases, reference, variant_id) FROM stdin;
\.


--
-- Data for Name: allele_info; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.allele_info (id, version, allele_id, tag, value) FROM stdin;
\.


--
-- Data for Name: analysis; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.analysis (id, version, algorithm, description, name, program, program_version, source_name, sourceuri, source_version, time_executed) FROM stdin;
\.


--
-- Data for Name: analysis_feature; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.analysis_feature (id, version, analysis_id, feature_id, identity, normalized_score, raw_score, significance) FROM stdin;
\.


--
-- Data for Name: analysis_property; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.analysis_property (id, version, analysis_id, type_id, value) FROM stdin;
\.


--
-- Data for Name: application_preference; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.application_preference (id, version, name, value) FROM stdin;
21	0	common_data_directory	/data/temporary/apollo_data
\.


--
-- Data for Name: audit_log; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.audit_log (id, actor, class_name, date_created, event_name, last_updated, new_value, old_value, persisted_object_id, persisted_object_version, property_name, uri) FROM stdin;
\.


--
-- Data for Name: available_status; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.available_status (id, version, value) FROM stdin;
\.


--
-- Data for Name: available_status_feature_type; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.available_status_feature_type (available_status_feature_types_id, feature_type_id) FROM stdin;
\.


--
-- Data for Name: canned_comment; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.canned_comment (id, version, comment, metadata) FROM stdin;
\.


--
-- Data for Name: canned_comment_feature_type; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.canned_comment_feature_type (canned_comment_feature_types_id, feature_type_id) FROM stdin;
\.


--
-- Data for Name: canned_key; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.canned_key (id, version, label, metadata) FROM stdin;
\.


--
-- Data for Name: canned_key_feature_type; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.canned_key_feature_type (canned_key_feature_types_id, feature_type_id) FROM stdin;
\.


--
-- Data for Name: canned_value; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.canned_value (id, version, label, metadata) FROM stdin;
\.


--
-- Data for Name: canned_value_feature_type; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.canned_value_feature_type (canned_value_feature_types_id, feature_type_id) FROM stdin;
\.


--
-- Data for Name: custom_domain_mapping; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.custom_domain_mapping (id, version, alternate_cv_term, cv_term, is_transcript, ontology_id) FROM stdin;
\.


--
-- Data for Name: cv; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.cv (id, version, definition, name) FROM stdin;
\.


--
-- Data for Name: cvterm; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.cvterm (id, version, cv_id, dbxref_id, definition, is_obsolete, is_relationship_type, name) FROM stdin;
\.


--
-- Data for Name: cvterm_path; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.cvterm_path (id, version, cv_id, objectcvterm_id, path_distance, subjectcvterm_id, type_id) FROM stdin;
\.


--
-- Data for Name: cvterm_relationship; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.cvterm_relationship (id, version, objectcvterm_id, subjectcvterm_id, type_id) FROM stdin;
\.


--
-- Data for Name: data_adapter; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.data_adapter (id, version, export_source_genomic_sequence, feature_type_string, implementation_class, data_adapter_key, options, permission, source, temp_directory) FROM stdin;
\.


--
-- Data for Name: data_adapter_data_adapter; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.data_adapter_data_adapter (data_adapter_data_adapters_id, data_adapter_id) FROM stdin;
\.


--
-- Data for Name: databasechangelog; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase) FROM stdin;
1445460972540-1	nathandunn (generated)	changelog-2_0_1.groovy	2020-03-17 03:51:02.666394+00	1	EXECUTED	3:22af752b59c8a786804379d0ef9a7c96	Modify data type		\N	2.0.5
1445460972540-2	nathandunn (generated)	changelog-2_0_1.groovy	2020-03-17 03:51:02.676458+00	2	EXECUTED	3:0c7241c67fe6fdead173495f8135667c	Modify data type		\N	2.0.5
1445460972540-3	nathandunn (generated)	changelog-2_0_1.groovy	2020-03-17 03:51:02.680912+00	3	EXECUTED	3:7da023939d667924b90829879f6e3f90	Modify data type		\N	2.0.5
1445460972540-4	nathandunn (generated)	changelog-2_0_1.groovy	2020-03-17 03:51:02.684926+00	4	EXECUTED	3:32568584e0e7e9d168249fc6aa1fdc78	Modify data type		\N	2.0.5
1445460972540-5	nathandunn (generated)	changelog-2_0_1.groovy	2020-03-17 03:51:02.689681+00	5	EXECUTED	3:af4137cce92f904c0b58b72062282fe6	Modify data type		\N	2.0.5
1445460972540-6	nathandunn (generated)	changelog-2_0_1.groovy	2020-03-17 03:51:02.694164+00	6	EXECUTED	3:958e7f48d6e001950e630907bf2eb43b	Modify data type		\N	2.0.5
1445460972540-7	nathandunn (generated)	changelog-2_0_1.groovy	2020-03-17 03:51:02.698467+00	7	EXECUTED	3:4892312d16728d0a9f7c4ab0224601ce	Modify data type		\N	2.0.5
1445460972540-8	nathandunn (generated)	changelog-2_0_1.groovy	2020-03-17 03:51:02.702766+00	8	EXECUTED	3:b0f760c644ea0a7757cd8c891e1bd8d2	Modify data type		\N	2.0.5
1445460972540-9	nathandunn (generated)	changelog-2_0_1.groovy	2020-03-17 03:51:02.706534+00	9	EXECUTED	3:e11a299c9debc2f5ea21621af7bb5527	Modify data type		\N	2.0.5
1445460972540-10	nathandunn (generated)	changelog-2_0_1.groovy	2020-03-17 03:51:02.710184+00	10	EXECUTED	3:933ae34f8a303eac4b8782571189be9b	Modify data type		\N	2.0.5
1445460972540-11	nathandunn (generated)	changelog-2_0_1.groovy	2020-03-17 03:51:02.714082+00	11	EXECUTED	3:54ae0b1800d2fc5e7878b9d23502c22c	Modify data type		\N	2.0.5
1445460972540-12	nathandunn (generated)	changelog-2_0_1.groovy	2020-03-17 03:51:02.718355+00	12	EXECUTED	3:ea5b0a298adaa4319bceaf219247a097	Modify data type		\N	2.0.5
1445460972540-13	nathandunn (generated)	changelog-2_0_1.groovy	2020-03-17 03:51:02.722552+00	13	EXECUTED	3:0d1c48bf959b14cd33e8e00556da1951	Modify data type		\N	2.0.5
1445460972540-14	nathandunn (generated)	changelog-2_0_1.groovy	2020-03-17 03:51:02.728266+00	14	EXECUTED	3:7880baa527eb89987242a09f500ecf7d	Modify data type		\N	2.0.5
1445460972540-15	nathandunn (generated)	changelog-2_0_1.groovy	2020-03-17 03:51:02.732875+00	15	EXECUTED	3:3f86e5aab029173432a1a0ab4c43e23f	Modify data type		\N	2.0.5
1445460972540-16	nathandunn (generated)	changelog-2_0_1.groovy	2020-03-17 03:51:02.736498+00	16	EXECUTED	3:ae5beb5f944d939d89688395c8bc3f69	Modify data type		\N	2.0.5
1445460972540-17	nathandunn (generated)	changelog-2_0_1.groovy	2020-03-17 03:51:02.740519+00	17	EXECUTED	3:0e70a7f5d594868996bd218338579fe5	Modify data type		\N	2.0.5
1445460972540-18	nathandunn (generated)	changelog-2_0_1.groovy	2020-03-17 03:51:02.744445+00	18	EXECUTED	3:dc35fffeb59fdd0314d3162ce8830d86	Modify data type		\N	2.0.5
1445460972540-19	nathandunn (generated)	changelog-2_0_1.groovy	2020-03-17 03:51:02.748519+00	19	EXECUTED	3:cde6cb0dae81b9ea366edc0ded82a1be	Modify data type		\N	2.0.5
1445460972540-20	nathandunn (generated)	changelog-2_0_1.groovy	2020-03-17 03:51:02.752583+00	20	EXECUTED	3:46a0b00d358c19c65d803b4af4c4d84f	Modify data type		\N	2.0.5
1445460972540-21	nathandunn (generated)	changelog-2_0_1.groovy	2020-03-17 03:51:02.756968+00	21	EXECUTED	3:2125d5aa7efa02682e38d2d337d9afff	Modify data type		\N	2.0.5
1445460972540-22	nathandunn (generated)	changelog-2_0_1.groovy	2020-03-17 03:51:02.760922+00	22	EXECUTED	3:d409f1edac038a1264d030bdb314e7a9	Modify data type		\N	2.0.5
1445460972540-23	nathandunn (generated)	changelog-2_0_1.groovy	2020-03-17 03:51:02.764839+00	23	EXECUTED	3:72eaf1247036c2e73ac28bd75a094e2a	Modify data type		\N	2.0.5
1445460972540-27	nathandunn (generated)	changelog-2_0_1.groovy	2020-03-17 03:51:02.776152+00	24	MARK_RAN	3:0b265d4077016a9e0c76e2e4ee85eac3	Drop Column (x6)		\N	2.0.5
1454711582784-1	cmdcolin (generated)	changelog-2_0_2.groovy	2020-03-17 03:51:05.087599+00	25	MARK_RAN	3:068703dfd6e640d4acb0bb4582c94ece	Create Index		\N	2.0.5
1459788030174-1	deepak.unni3	changelog-2_0_3.groovy	2020-03-17 03:51:05.146226+00	26	EXECUTED	3:78cbe7c731bcba5bf30fbc5ea4cc4380	Modify data type		\N	2.0.5
1459788030174-2	nathandunn	changelog-2_0_3.groovy	2020-03-17 03:51:05.151283+00	27	MARK_RAN	3:b3ba6222b0538395d928e653026856ce	Add Column (x3), Add Not-Null Constraint		\N	2.0.5
1459788030175-1	nathandunn	changelog-2_0_7.groovy	2020-03-17 03:51:05.155936+00	28	EXECUTED	3:7254ad4b135e6906f3b56fddf436853e	Delete Data		\N	2.0.5
1459788030176-1	deepak.unni3	changelog-2_0_8.groovy	2020-03-17 03:51:05.158833+00	29	EXECUTED	3:355d86cb6d6791df4f13ee62b4c49139	Drop Not-Null Constraint		\N	2.0.5
1459788030177-1	nathandunn	changelog-2_0_9.groovy	2020-03-17 03:51:05.167525+00	30	MARK_RAN	3:14e435a1ca31685020b9aecb9dd2cdfa	Add Column (x2)		\N	2.0.5
1459788030178-1	nathandunn	changelog-2_3_1.groovy	2020-03-17 03:51:05.172416+00	31	MARK_RAN	3:bd0d5968dcc35876cec708b578bb6a03	Add Column		\N	2.0.5
1459788030178-2	nathandunn	changelog-2_3_1.groovy	2020-03-17 03:51:05.178394+00	32	MARK_RAN	3:8e672295dc34be575819e54b5dbc5049	Add Column		\N	2.0.5
1459788030178-1	nathandunn	changelog-2_4_0.groovy	2020-03-17 03:51:05.183143+00	33	MARK_RAN	3:78c5ca1179a59d7bdb5a0aaad84028b6	Create Table		\N	2.0.5
1459788030178-2	nathandunn	changelog-2_4_0.groovy	2020-03-17 03:51:05.18576+00	34	EXECUTED	3:b16c9f27ec3323d6ab7a38eba10591df	Drop Not-Null Constraint		\N	2.0.5
1459788030180-1	nathandunn	changelog-2_6_0.groovy	2020-03-17 03:51:05.189863+00	35	MARK_RAN	3:cd71fa86f08a4f42f669ef4ca07e54c2	Drop Column (x2)		\N	2.0.5
1459788030180-2	nathandunn	changelog-2_6_0.groovy	2020-03-17 03:51:05.192926+00	36	EXECUTED	3:51553c12e24018628253f4fbe51ac401	Drop Not-Null Constraint (x3)		\N	2.0.5
\.


--
-- Data for Name: databasechangeloglock; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.databasechangeloglock (id, locked, lockgranted, lockedby) FROM stdin;
1	f	\N	\N
\.


--
-- Data for Name: db; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.db (id, version, description, name, url, url_prefix) FROM stdin;
\.


--
-- Data for Name: dbxref; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.dbxref (id, version, accession, db_id, description) FROM stdin;
\.


--
-- Data for Name: dbxref_property; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.dbxref_property (id, version, dbxref_id, rank, type_id, value) FROM stdin;
\.


--
-- Data for Name: environment; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.environment (id, version, description, uniquename) FROM stdin;
\.


--
-- Data for Name: environmentcvterm; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.environmentcvterm (id, version, cvterm_id, environment_id) FROM stdin;
\.


--
-- Data for Name: feature; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.feature (id, version, date_created, dbxref_id, description, is_analysis, is_obsolete, last_updated, md5checksum, name, sequence_length, status_id, symbol, unique_name, class, analysis_feature_id, alternate_cv_term, class_name, custom_alternate_cv_term, custom_class_name, custom_cv_term, custom_ontology_id, cv_term, meta_data, ontology_id, alteration_residue, deletion_length, reference_allele_id) FROM stdin;
\.


--
-- Data for Name: feature_dbxref; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.feature_dbxref (feature_featuredbxrefs_id, dbxref_id) FROM stdin;
\.


--
-- Data for Name: feature_event; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.feature_event (id, version, child_id, child_split_id, current, date_created, editor_id, last_updated, name, new_features_json_array, old_features_json_array, operation, original_json_command, parent_id, parent_merge_id, unique_name) FROM stdin;
\.


--
-- Data for Name: feature_feature_phenotypes; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.feature_feature_phenotypes (feature_id, phenotype_id) FROM stdin;
\.


--
-- Data for Name: feature_genotype; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.feature_genotype (id, version, cgroup, chromosome_feature_id, cvterm_id, feature_id, feature_genotype_id, genotype_id, rank) FROM stdin;
\.


--
-- Data for Name: feature_grails_user; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.feature_grails_user (feature_owners_id, user_id) FROM stdin;
\.


--
-- Data for Name: feature_location; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.feature_location (id, version, feature_id, fmax, fmin, is_fmax_partial, is_fmin_partial, locgroup, phase, rank, residue_info, sequence_id, strand) FROM stdin;
\.


--
-- Data for Name: feature_location_publication; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.feature_location_publication (feature_location_feature_location_publications_id, publication_id) FROM stdin;
\.


--
-- Data for Name: feature_property; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.feature_property (id, version, feature_id, rank, tag, type_id, value, class) FROM stdin;
\.


--
-- Data for Name: feature_property_publication; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.feature_property_publication (feature_property_feature_property_publications_id, publication_id) FROM stdin;
\.


--
-- Data for Name: feature_publication; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.feature_publication (feature_feature_publications_id, publication_id) FROM stdin;
\.


--
-- Data for Name: feature_relationship; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.feature_relationship (id, version, child_feature_id, parent_feature_id, rank, value) FROM stdin;
\.


--
-- Data for Name: feature_relationship_feature_property; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.feature_relationship_feature_property (feature_relationship_feature_relationship_properties_id, feature_property_id) FROM stdin;
\.


--
-- Data for Name: feature_relationship_publication; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.feature_relationship_publication (feature_relationship_feature_relationship_publications_id, publication_id) FROM stdin;
\.


--
-- Data for Name: feature_synonym; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.feature_synonym (id, version, feature_id, is_current, is_internal, publication_id, synonym_id) FROM stdin;
\.


--
-- Data for Name: feature_type; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.feature_type (id, version, display, name, ontology_id, type) FROM stdin;
4	0	gene	gene	SO:0000704	sequence
5	0	pseudogene	pseudogene	SO:0000336	sequence
6	0	transcript	transcript	SO:0000673	sequence
7	0	mRNA	mRNA	SO:0000234	sequence
8	0	snRNA	snRNA	SO:0000274	sequence
9	0	snoRNA	snoRNA	SO:0000275	sequence
10	0	miRNA	miRNA	SO:0000276	sequence
11	0	tRNA	tRNA	SO:0000253	sequence
12	0	ncRNA	ncRNA	SO:0000655	sequence
13	0	rRNA	rRNA	SO:0000252	sequence
14	0	repeat_region	repeat_region	SO:0000657	sequence
15	0	Terminator	terminator	SO:0000141	sequence
16	0	transposable_element	transposable_element	SO:0000101	sequence
\.


--
-- Data for Name: featurecvterm; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.featurecvterm (id, version, cvterm_id, feature_id, is_not, publication_id, rank) FROM stdin;
\.


--
-- Data for Name: featurecvterm_dbxref; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.featurecvterm_dbxref (featurecvterm_featurecvtermdbxrefs_id, dbxref_id) FROM stdin;
\.


--
-- Data for Name: featurecvterm_publication; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.featurecvterm_publication (featurecvterm_featurecvterm_publications_id, publication_id) FROM stdin;
\.


--
-- Data for Name: gene_product; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.gene_product (id, version, alternate, date_created, evidence_ref, evidence_ref_label, feature_id, last_updated, notes_array, product_name, reference, with_or_from_array) FROM stdin;
\.


--
-- Data for Name: gene_product_grails_user; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.gene_product_grails_user (gene_product_owners_id, user_id) FROM stdin;
\.


--
-- Data for Name: genotype; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.genotype (id, version, description, genotype_id, name, unique_name) FROM stdin;
\.


--
-- Data for Name: go_annotation; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.go_annotation (id, version, aspect, date_created, evidence_ref, evidence_ref_label, feature_id, gene_product_relationship_ref, go_ref, go_ref_label, last_updated, negate, notes_array, reference, with_or_from_array) FROM stdin;
\.


--
-- Data for Name: go_annotation_grails_user; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.go_annotation_grails_user (go_annotation_owners_id, user_id) FROM stdin;
\.


--
-- Data for Name: grails_user; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.grails_user (id, version, first_name, inactive, last_name, metadata, password_hash, username) FROM stdin;
20	4	Apollo	f	Admin	{}	0ead2060b65992dca4769af601a1b3a35ef38cfad2c2c465bb160ea764157c5d	administrator
28	2	Demonstration	f	User	{"creator":"20"}	0ead2060b65992dca4769af601a1b3a35ef38cfad2c2c465bb160ea764157c5d	demouser
\.


--
-- Data for Name: grails_user_roles; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.grails_user_roles (role_id, user_id) FROM stdin;
19	20
\.


--
-- Name: hibernate_sequence; Type: SEQUENCE SET; Schema: public; Owner: apollo
--

SELECT pg_catalog.setval('public.hibernate_sequence', 25, true);


--
-- Data for Name: operation; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.operation (id, version, attributes, feature_unique_name, new_features, old_features, operation_type) FROM stdin;
\.


--
-- Data for Name: organism; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.organism (id, version, abbreviation, blatdb, comment, common_name, data_added_via_web_services, directory, genome_fasta, genome_fasta_index, genus, metadata, non_default_translation_table, obsolete, official_gene_set_track, public_mode, species, valid) FROM stdin;
22	2	\N	\N	\N	Covid-19	\N	/jbrowse/jbrowse/data/SARS-CoV-2	\N	\N	Betacoronavirus	{"creator":"20"}	\N	f	\N	t	Sarbecovirus	t
\.


--
-- Data for Name: organism_filter; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.organism_filter (id, version, organism_id, class, canned_value_id, canned_comment_id, suggested_name_id, canned_key_id, available_status_id) FROM stdin;
\.


--
-- Data for Name: organism_organism_property; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.organism_organism_property (organism_organism_properties_id, organism_property_id) FROM stdin;
\.


--
-- Data for Name: organism_property; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.organism_property (id, version, abbreviation, comment, common_name, genus, organism_id, species) FROM stdin;
\.


--
-- Data for Name: organism_property_organism_property; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.organism_property_organism_property (organism_property_organism_properties_id, organism_property_id) FROM stdin;
\.


--
-- Data for Name: organism_property_organismdbxref; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.organism_property_organismdbxref (organism_property_organismdbxrefs_id, organismdbxref_id) FROM stdin;
\.


--
-- Data for Name: organismdbxref; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.organismdbxref (id, version, dbxref_id, organism_id) FROM stdin;
\.


--
-- Data for Name: part_of; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.part_of (id, version) FROM stdin;
\.


--
-- Data for Name: permission; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.permission (id, version, organism_id, class, group_id, permissions, track_visibilities, user_id) FROM stdin;
23	1	22	org.bbop.apollo.UserOrganismPermission	\N	["ADMINISTRATE"]	\N	20
29	1	22	org.bbop.apollo.UserOrganismPermission	\N	["WRITE"]	\N	28
\.


--
-- Data for Name: phenotype; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.phenotype (id, version, assay_id, attribute_id, cvalue_id, observable_id, unique_name, value) FROM stdin;
\.


--
-- Data for Name: phenotype_cvterm; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.phenotype_cvterm (phenotype_phenotypecvterms_id, cvterm_id) FROM stdin;
\.


--
-- Data for Name: phenotype_description; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.phenotype_description (id, version, description, environment_id, genotype_id, publication_id, type_id) FROM stdin;
\.


--
-- Data for Name: phenotype_statement; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.phenotype_statement (id, version, environment_id, genotype_id, phenotype_id, publication_id, type_id) FROM stdin;
\.


--
-- Data for Name: preference; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.preference (id, version, client_token, date_created, domain, last_updated, name, preferences_string, class, user_id, current_organism, endbp, native_track_list, organism_id, sequence_id, startbp) FROM stdin;
\.


--
-- Data for Name: provenance; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.provenance (id, version, date_created, evidence_ref, evidence_ref_label, feature_id, field, last_updated, notes_array, reference, with_or_from_array) FROM stdin;
\.


--
-- Data for Name: provenance_grails_user; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.provenance_grails_user (provenance_owners_id, user_id) FROM stdin;
\.


--
-- Data for Name: proxy; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.proxy (id, version, active, fallback_order, last_fail, last_success, reference_url, target_url) FROM stdin;
2	0	t	0	\N	\N	http://golr.geneontology.org/select	http://golr.geneontology.org/solr/select
3	0	f	1	\N	\N	http://golr.geneontology.org/select	http://golr.berkeleybop.org/solr/select
\.


--
-- Data for Name: publication; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.publication (id, version, is_obsolete, issue, mini_reference, pages, publication_place, publication_year, publisher, series_name, title, type_id, unique_name, volume, volume_title) FROM stdin;
\.


--
-- Data for Name: publication_author; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.publication_author (id, version, editor, given_names, publication_id, rank, suffix, surname) FROM stdin;
\.


--
-- Data for Name: publication_relationship; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.publication_relationship (id, version, object_publication_id, subject_publication_id, type_id) FROM stdin;
\.


--
-- Data for Name: publicationdbxref; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.publicationdbxref (id, version, dbxref_id, is_current, publication_id) FROM stdin;
\.


--
-- Data for Name: role; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.role (id, version, name, rank) FROM stdin;
17	1	USER	10
18	1	INSTRUCTOR	50
19	2	ADMIN	100
\.


--
-- Data for Name: role_permissions; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.role_permissions (role_id, permissions_string) FROM stdin;
17	*:*
18	*:*
19	*:*
\.


--
-- Data for Name: search_tool; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.search_tool (id, version, binary_path, database_path, implementation_class, search_key, options, remove_temp_directory, tmp_dir) FROM stdin;
\.


--
-- Data for Name: sequence; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.sequence (id, version, sequence_end, length, name, organism_id, seq_chunk_size, sequence_start) FROM stdin;
24	0	29903	29903	NC_045512.2	22	80000	0
\.


--
-- Data for Name: sequence_cache; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.sequence_cache (id, version, feature_name, fmax, fmin, organism_name, param_map, response, sequence_name, type) FROM stdin;
\.


--
-- Data for Name: sequence_chunk; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.sequence_chunk (id, version, chunk_number, residue, sequence_id) FROM stdin;
\.


--
-- Data for Name: server_data; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.server_data (id, version, date_created, last_updated, name) FROM stdin;
1	0	2020-03-17 03:51:05.631	2020-03-17 03:51:05.631	ApolloSever-639066273837765105796952876
\.


--
-- Data for Name: suggested_name; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.suggested_name (id, version, metadata, name) FROM stdin;
\.


--
-- Data for Name: suggested_name_feature_type; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.suggested_name_feature_type (suggested_name_feature_types_id, feature_type_id) FROM stdin;
\.


--
-- Data for Name: synonym; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.synonym (id, version, name) FROM stdin;
\.


--
-- Data for Name: track_cache; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.track_cache (id, version, feature_name, fmax, fmin, organism_name, param_map, response, sequence_name, track_name, type) FROM stdin;
\.


--
-- Data for Name: user_group; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.user_group (id, version, metadata, name, public_group) FROM stdin;
\.


--
-- Data for Name: user_group_admin; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.user_group_admin (user_id, user_group_id) FROM stdin;
\.


--
-- Data for Name: user_group_users; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.user_group_users (user_id, user_group_id) FROM stdin;
\.


--
-- Data for Name: variant_info; Type: TABLE DATA; Schema: public; Owner: apollo
--

COPY public.variant_info (id, version, tag, value, variant_id) FROM stdin;
\.


--
-- Name: allele_info allele_info_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.allele_info
    ADD CONSTRAINT allele_info_pkey PRIMARY KEY (id);


--
-- Name: allele allele_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.allele
    ADD CONSTRAINT allele_pkey PRIMARY KEY (id);


--
-- Name: analysis_feature analysis_feature_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.analysis_feature
    ADD CONSTRAINT analysis_feature_pkey PRIMARY KEY (id);


--
-- Name: analysis analysis_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.analysis
    ADD CONSTRAINT analysis_pkey PRIMARY KEY (id);


--
-- Name: analysis_property analysis_property_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.analysis_property
    ADD CONSTRAINT analysis_property_pkey PRIMARY KEY (id);


--
-- Name: application_preference application_preference_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.application_preference
    ADD CONSTRAINT application_preference_pkey PRIMARY KEY (id);


--
-- Name: audit_log audit_log_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.audit_log
    ADD CONSTRAINT audit_log_pkey PRIMARY KEY (id);


--
-- Name: available_status available_status_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.available_status
    ADD CONSTRAINT available_status_pkey PRIMARY KEY (id);


--
-- Name: canned_comment canned_comment_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.canned_comment
    ADD CONSTRAINT canned_comment_pkey PRIMARY KEY (id);


--
-- Name: canned_key canned_key_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.canned_key
    ADD CONSTRAINT canned_key_pkey PRIMARY KEY (id);


--
-- Name: canned_value canned_value_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.canned_value
    ADD CONSTRAINT canned_value_pkey PRIMARY KEY (id);


--
-- Name: custom_domain_mapping custom_domain_mapping_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.custom_domain_mapping
    ADD CONSTRAINT custom_domain_mapping_pkey PRIMARY KEY (id);


--
-- Name: cv cv_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.cv
    ADD CONSTRAINT cv_pkey PRIMARY KEY (id);


--
-- Name: cvterm_path cvterm_path_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.cvterm_path
    ADD CONSTRAINT cvterm_path_pkey PRIMARY KEY (id);


--
-- Name: cvterm cvterm_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.cvterm
    ADD CONSTRAINT cvterm_pkey PRIMARY KEY (id);


--
-- Name: cvterm_relationship cvterm_relationship_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.cvterm_relationship
    ADD CONSTRAINT cvterm_relationship_pkey PRIMARY KEY (id);


--
-- Name: data_adapter data_adapter_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.data_adapter
    ADD CONSTRAINT data_adapter_pkey PRIMARY KEY (id);


--
-- Name: db db_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.db
    ADD CONSTRAINT db_pkey PRIMARY KEY (id);


--
-- Name: dbxref dbxref_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.dbxref
    ADD CONSTRAINT dbxref_pkey PRIMARY KEY (id);


--
-- Name: dbxref_property dbxref_property_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.dbxref_property
    ADD CONSTRAINT dbxref_property_pkey PRIMARY KEY (id);


--
-- Name: environment environment_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.environment
    ADD CONSTRAINT environment_pkey PRIMARY KEY (id);


--
-- Name: environmentcvterm environmentcvterm_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.environmentcvterm
    ADD CONSTRAINT environmentcvterm_pkey PRIMARY KEY (id);


--
-- Name: feature_event feature_event_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.feature_event
    ADD CONSTRAINT feature_event_pkey PRIMARY KEY (id);


--
-- Name: feature_feature_phenotypes feature_feature_phenotypes_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.feature_feature_phenotypes
    ADD CONSTRAINT feature_feature_phenotypes_pkey PRIMARY KEY (feature_id, phenotype_id);


--
-- Name: feature_genotype feature_genotype_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.feature_genotype
    ADD CONSTRAINT feature_genotype_pkey PRIMARY KEY (id);


--
-- Name: feature_location feature_location_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.feature_location
    ADD CONSTRAINT feature_location_pkey PRIMARY KEY (id);


--
-- Name: feature feature_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.feature
    ADD CONSTRAINT feature_pkey PRIMARY KEY (id);


--
-- Name: feature_property feature_property_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.feature_property
    ADD CONSTRAINT feature_property_pkey PRIMARY KEY (id);


--
-- Name: feature_relationship feature_relationship_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.feature_relationship
    ADD CONSTRAINT feature_relationship_pkey PRIMARY KEY (id);


--
-- Name: feature_synonym feature_synonym_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.feature_synonym
    ADD CONSTRAINT feature_synonym_pkey PRIMARY KEY (id);


--
-- Name: feature_type feature_type_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.feature_type
    ADD CONSTRAINT feature_type_pkey PRIMARY KEY (id);


--
-- Name: featurecvterm featurecvterm_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.featurecvterm
    ADD CONSTRAINT featurecvterm_pkey PRIMARY KEY (id);


--
-- Name: gene_product gene_product_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.gene_product
    ADD CONSTRAINT gene_product_pkey PRIMARY KEY (id);


--
-- Name: genotype genotype_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.genotype
    ADD CONSTRAINT genotype_pkey PRIMARY KEY (id);


--
-- Name: go_annotation go_annotation_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.go_annotation
    ADD CONSTRAINT go_annotation_pkey PRIMARY KEY (id);


--
-- Name: grails_user grails_user_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.grails_user
    ADD CONSTRAINT grails_user_pkey PRIMARY KEY (id);


--
-- Name: grails_user_roles grails_user_roles_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.grails_user_roles
    ADD CONSTRAINT grails_user_roles_pkey PRIMARY KEY (user_id, role_id);


--
-- Name: operation operation_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.operation
    ADD CONSTRAINT operation_pkey PRIMARY KEY (id);


--
-- Name: organism_filter organism_filter_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.organism_filter
    ADD CONSTRAINT organism_filter_pkey PRIMARY KEY (id);


--
-- Name: organism organism_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.organism
    ADD CONSTRAINT organism_pkey PRIMARY KEY (id);


--
-- Name: organism_property organism_property_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.organism_property
    ADD CONSTRAINT organism_property_pkey PRIMARY KEY (id);


--
-- Name: organismdbxref organismdbxref_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.organismdbxref
    ADD CONSTRAINT organismdbxref_pkey PRIMARY KEY (id);


--
-- Name: part_of part_of_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.part_of
    ADD CONSTRAINT part_of_pkey PRIMARY KEY (id);


--
-- Name: permission permission_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.permission
    ADD CONSTRAINT permission_pkey PRIMARY KEY (id);


--
-- Name: phenotype_description phenotype_description_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.phenotype_description
    ADD CONSTRAINT phenotype_description_pkey PRIMARY KEY (id);


--
-- Name: phenotype phenotype_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.phenotype
    ADD CONSTRAINT phenotype_pkey PRIMARY KEY (id);


--
-- Name: phenotype_statement phenotype_statement_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.phenotype_statement
    ADD CONSTRAINT phenotype_statement_pkey PRIMARY KEY (id);


--
-- Name: databasechangelog pk_databasechangelog; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.databasechangelog
    ADD CONSTRAINT pk_databasechangelog PRIMARY KEY (id, author, filename);


--
-- Name: databasechangeloglock pk_databasechangeloglock; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.databasechangeloglock
    ADD CONSTRAINT pk_databasechangeloglock PRIMARY KEY (id);


--
-- Name: preference preference_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.preference
    ADD CONSTRAINT preference_pkey PRIMARY KEY (id);


--
-- Name: provenance provenance_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.provenance
    ADD CONSTRAINT provenance_pkey PRIMARY KEY (id);


--
-- Name: proxy proxy_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.proxy
    ADD CONSTRAINT proxy_pkey PRIMARY KEY (id);


--
-- Name: publication_author publication_author_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.publication_author
    ADD CONSTRAINT publication_author_pkey PRIMARY KEY (id);


--
-- Name: publication publication_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.publication
    ADD CONSTRAINT publication_pkey PRIMARY KEY (id);


--
-- Name: publication_relationship publication_relationship_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.publication_relationship
    ADD CONSTRAINT publication_relationship_pkey PRIMARY KEY (id);


--
-- Name: publicationdbxref publicationdbxref_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.publicationdbxref
    ADD CONSTRAINT publicationdbxref_pkey PRIMARY KEY (id);


--
-- Name: role role_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.role
    ADD CONSTRAINT role_pkey PRIMARY KEY (id);


--
-- Name: search_tool search_tool_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.search_tool
    ADD CONSTRAINT search_tool_pkey PRIMARY KEY (id);


--
-- Name: sequence_cache sequence_cache_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.sequence_cache
    ADD CONSTRAINT sequence_cache_pkey PRIMARY KEY (id);


--
-- Name: sequence_chunk sequence_chunk_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.sequence_chunk
    ADD CONSTRAINT sequence_chunk_pkey PRIMARY KEY (id);


--
-- Name: sequence sequence_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.sequence
    ADD CONSTRAINT sequence_pkey PRIMARY KEY (id);


--
-- Name: server_data server_data_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.server_data
    ADD CONSTRAINT server_data_pkey PRIMARY KEY (id);


--
-- Name: suggested_name suggested_name_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.suggested_name
    ADD CONSTRAINT suggested_name_pkey PRIMARY KEY (id);


--
-- Name: synonym synonym_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.synonym
    ADD CONSTRAINT synonym_pkey PRIMARY KEY (id);


--
-- Name: track_cache track_cache_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.track_cache
    ADD CONSTRAINT track_cache_pkey PRIMARY KEY (id);


--
-- Name: role uk_1uxpq87pyp6d4vp86es3ew5lf; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.role
    ADD CONSTRAINT uk_1uxpq87pyp6d4vp86es3ew5lf UNIQUE (rank);


--
-- Name: db uk_3mkho1p232es23vtaqp0obydb; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.db
    ADD CONSTRAINT uk_3mkho1p232es23vtaqp0obydb UNIQUE (name);


--
-- Name: server_data uk_75vf8prrwqnurcllgi892v8qg; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.server_data
    ADD CONSTRAINT uk_75vf8prrwqnurcllgi892v8qg UNIQUE (name);


--
-- Name: application_preference uk_8mry59fk655ec8pilksknkjxd; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.application_preference
    ADD CONSTRAINT uk_8mry59fk655ec8pilksknkjxd UNIQUE (name);


--
-- Name: role uk_8sewwnpamngi6b1dwaa88askk; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.role
    ADD CONSTRAINT uk_8sewwnpamngi6b1dwaa88askk UNIQUE (name);


--
-- Name: grails_user uk_rdmcxcj6i53kfjmb9j811ta1m; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.grails_user
    ADD CONSTRAINT uk_rdmcxcj6i53kfjmb9j811ta1m UNIQUE (username);


--
-- Name: user_group_admin user_group_admin_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.user_group_admin
    ADD CONSTRAINT user_group_admin_pkey PRIMARY KEY (user_group_id, user_id);


--
-- Name: user_group user_group_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.user_group
    ADD CONSTRAINT user_group_pkey PRIMARY KEY (id);


--
-- Name: user_group_users user_group_users_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.user_group_users
    ADD CONSTRAINT user_group_users_pkey PRIMARY KEY (user_group_id, user_id);


--
-- Name: variant_info variant_info_pkey; Type: CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.variant_info
    ADD CONSTRAINT variant_info_pkey PRIMARY KEY (id);


--
-- Name: feature_uniquename; Type: INDEX; Schema: public; Owner: apollo
--

CREATE INDEX feature_uniquename ON public.feature_event USING btree (unique_name);


--
-- Name: phenotype_description fk_1kkkx1uxs6li0r72qhvke6o77; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.phenotype_description
    ADD CONSTRAINT fk_1kkkx1uxs6li0r72qhvke6o77 FOREIGN KEY (publication_id) REFERENCES public.publication(id);


--
-- Name: feature_dbxref fk_1mrfkxbb3n7fhjxcrkxappdn8; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.feature_dbxref
    ADD CONSTRAINT fk_1mrfkxbb3n7fhjxcrkxappdn8 FOREIGN KEY (feature_featuredbxrefs_id) REFERENCES public.feature(id);


--
-- Name: suggested_name_feature_type fk_1p7u7j0his3pnmk95eataomue; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.suggested_name_feature_type
    ADD CONSTRAINT fk_1p7u7j0his3pnmk95eataomue FOREIGN KEY (feature_type_id) REFERENCES public.feature_type(id);


--
-- Name: organism_filter fk_1ykkgglhecw7pina70hxhrhkf; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.organism_filter
    ADD CONSTRAINT fk_1ykkgglhecw7pina70hxhrhkf FOREIGN KEY (canned_comment_id) REFERENCES public.canned_comment(id);


--
-- Name: data_adapter_data_adapter fk_321276juoco9ijc32gxeo7mi9; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.data_adapter_data_adapter
    ADD CONSTRAINT fk_321276juoco9ijc32gxeo7mi9 FOREIGN KEY (data_adapter_data_adapters_id) REFERENCES public.data_adapter(id);


--
-- Name: feature_event fk_35nc3xd2axx6fwyap4bjkt09u; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.feature_event
    ADD CONSTRAINT fk_35nc3xd2axx6fwyap4bjkt09u FOREIGN KEY (editor_id) REFERENCES public.grails_user(id);


--
-- Name: feature_property fk_36e638geg9tew42b1mp2ehff; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.feature_property
    ADD CONSTRAINT fk_36e638geg9tew42b1mp2ehff FOREIGN KEY (type_id) REFERENCES public.cvterm(id);


--
-- Name: analysis_property fk_38g8n4bitmdwkrcs217uexrwx; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.analysis_property
    ADD CONSTRAINT fk_38g8n4bitmdwkrcs217uexrwx FOREIGN KEY (analysis_id) REFERENCES public.analysis(id);


--
-- Name: organism_filter fk_39svd38qq78gxs0idnu5yiorc; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.organism_filter
    ADD CONSTRAINT fk_39svd38qq78gxs0idnu5yiorc FOREIGN KEY (available_status_id) REFERENCES public.available_status(id);


--
-- Name: featurecvterm_publication fk_3a9j1mryggb0bcaguvkhw8hjm; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.featurecvterm_publication
    ADD CONSTRAINT fk_3a9j1mryggb0bcaguvkhw8hjm FOREIGN KEY (featurecvterm_featurecvterm_publications_id) REFERENCES public.featurecvterm(id);


--
-- Name: dbxref_property fk_3p1ssctww083s0tt65mmm64uo; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.dbxref_property
    ADD CONSTRAINT fk_3p1ssctww083s0tt65mmm64uo FOREIGN KEY (dbxref_id) REFERENCES public.dbxref(id);


--
-- Name: canned_value_feature_type fk_3r7k0qnsbmp0hbe94kpnexb8u; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.canned_value_feature_type
    ADD CONSTRAINT fk_3r7k0qnsbmp0hbe94kpnexb8u FOREIGN KEY (canned_value_feature_types_id) REFERENCES public.canned_value(id);


--
-- Name: organism_property_organism_property fk_3rtkicqpr3ca8dwivye3yajaa; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.organism_property_organism_property
    ADD CONSTRAINT fk_3rtkicqpr3ca8dwivye3yajaa FOREIGN KEY (organism_property_organism_properties_id) REFERENCES public.organism_property(id);


--
-- Name: preference fk_42b0lk4rcfjcagw84jugd1sgj; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.preference
    ADD CONSTRAINT fk_42b0lk4rcfjcagw84jugd1sgj FOREIGN KEY (organism_id) REFERENCES public.organism(id);


--
-- Name: feature_grails_user fk_4dgbhgiw0vb9hqy2k5fqg3neh; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.feature_grails_user
    ADD CONSTRAINT fk_4dgbhgiw0vb9hqy2k5fqg3neh FOREIGN KEY (feature_owners_id) REFERENCES public.feature(id);


--
-- Name: feature_relationship_publication fk_4j4u29xis9bhr65slfaimgjye; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.feature_relationship_publication
    ADD CONSTRAINT fk_4j4u29xis9bhr65slfaimgjye FOREIGN KEY (publication_id) REFERENCES public.publication(id);


--
-- Name: grails_user_roles fk_4mxkyj2itw9wyvcn6d8d4mta2; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.grails_user_roles
    ADD CONSTRAINT fk_4mxkyj2itw9wyvcn6d8d4mta2 FOREIGN KEY (role_id) REFERENCES public.role(id);


--
-- Name: permission fk_4nvoxx3htem6jseb4rmu0aqfp; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.permission
    ADD CONSTRAINT fk_4nvoxx3htem6jseb4rmu0aqfp FOREIGN KEY (organism_id) REFERENCES public.organism(id);


--
-- Name: provenance fk_4pokg8nm0gqw5inh5qyje5vtw; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.provenance
    ADD CONSTRAINT fk_4pokg8nm0gqw5inh5qyje5vtw FOREIGN KEY (feature_id) REFERENCES public.feature(id);


--
-- Name: sequence_chunk fk_4tsu0cp2dh2avbxifp1h1c9vd; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.sequence_chunk
    ADD CONSTRAINT fk_4tsu0cp2dh2avbxifp1h1c9vd FOREIGN KEY (sequence_id) REFERENCES public.sequence(id);


--
-- Name: feature_publication fk_580odgbjowisfshvk82rfjri2; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.feature_publication
    ADD CONSTRAINT fk_580odgbjowisfshvk82rfjri2 FOREIGN KEY (publication_id) REFERENCES public.publication(id);


--
-- Name: cvterm fk_6d097oy44230tuoo8lb8dkkcp; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.cvterm
    ADD CONSTRAINT fk_6d097oy44230tuoo8lb8dkkcp FOREIGN KEY (dbxref_id) REFERENCES public.dbxref(id);


--
-- Name: permission fk_6p3mx8al2w4f7ltqiwf1j88fm; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.permission
    ADD CONSTRAINT fk_6p3mx8al2w4f7ltqiwf1j88fm FOREIGN KEY (user_id) REFERENCES public.grails_user(id);


--
-- Name: feature_relationship fk_72kmd92rdc6gne0nrh026o1j0; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.feature_relationship
    ADD CONSTRAINT fk_72kmd92rdc6gne0nrh026o1j0 FOREIGN KEY (parent_feature_id) REFERENCES public.feature(id);


--
-- Name: feature_genotype fk_736wxgjs6pip5212ash5i68p; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.feature_genotype
    ADD CONSTRAINT fk_736wxgjs6pip5212ash5i68p FOREIGN KEY (genotype_id) REFERENCES public.genotype(id);


--
-- Name: feature fk_7huaou2aj3ac3oa49c1e0nhlm; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.feature
    ADD CONSTRAINT fk_7huaou2aj3ac3oa49c1e0nhlm FOREIGN KEY (analysis_feature_id) REFERENCES public.analysis_feature(id);


--
-- Name: feature_synonym fk_82wsc3bv9i01t9851xv4xekis; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.feature_synonym
    ADD CONSTRAINT fk_82wsc3bv9i01t9851xv4xekis FOREIGN KEY (publication_id) REFERENCES public.publication(id);


--
-- Name: feature_property_publication fk_86law9p6s1pbt02n3mltkcqwh; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.feature_property_publication
    ADD CONSTRAINT fk_86law9p6s1pbt02n3mltkcqwh FOREIGN KEY (feature_property_feature_property_publications_id) REFERENCES public.feature_property(id);


--
-- Name: feature_relationship fk_8jm56covt0m7m0m191bc5jseh; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.feature_relationship
    ADD CONSTRAINT fk_8jm56covt0m7m0m191bc5jseh FOREIGN KEY (child_feature_id) REFERENCES public.feature(id);


--
-- Name: organism_property_organism_property fk_8jxbx51qysqlm07orah5xg45y; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.organism_property_organism_property
    ADD CONSTRAINT fk_8jxbx51qysqlm07orah5xg45y FOREIGN KEY (organism_property_id) REFERENCES public.organism_property(id);


--
-- Name: canned_comment_feature_type fk_8l290fdei9m707s7ngn712sts; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.canned_comment_feature_type
    ADD CONSTRAINT fk_8l290fdei9m707s7ngn712sts FOREIGN KEY (canned_comment_feature_types_id) REFERENCES public.canned_comment(id);


--
-- Name: analysis_feature fk_8m30ycwh545b4aoxor9sbk1oq; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.analysis_feature
    ADD CONSTRAINT fk_8m30ycwh545b4aoxor9sbk1oq FOREIGN KEY (analysis_id) REFERENCES public.analysis(id);


--
-- Name: phenotype_description fk_8pbyj05khavdl5a648c7pmcil; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.phenotype_description
    ADD CONSTRAINT fk_8pbyj05khavdl5a648c7pmcil FOREIGN KEY (type_id) REFERENCES public.cvterm(id);


--
-- Name: phenotype_statement fk_8rbhsxxdf669tyed8jrr747hv; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.phenotype_statement
    ADD CONSTRAINT fk_8rbhsxxdf669tyed8jrr747hv FOREIGN KEY (genotype_id) REFERENCES public.genotype(id);


--
-- Name: publication_relationship fk_97qgrmdull1avkfqpfq1mc1wt; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.publication_relationship
    ADD CONSTRAINT fk_97qgrmdull1avkfqpfq1mc1wt FOREIGN KEY (object_publication_id) REFERENCES public.publication(id);


--
-- Name: phenotype_cvterm fk_9e2v7goj5w6nds5jo0x1va1nm; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.phenotype_cvterm
    ADD CONSTRAINT fk_9e2v7goj5w6nds5jo0x1va1nm FOREIGN KEY (cvterm_id) REFERENCES public.cvterm(id);


--
-- Name: publication_author fk_9eou8yof43krmrsdvfhfuisln; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.publication_author
    ADD CONSTRAINT fk_9eou8yof43krmrsdvfhfuisln FOREIGN KEY (publication_id) REFERENCES public.publication(id);


--
-- Name: user_group_users fk_9jib0g899h0gy3dypo7datfm9; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.user_group_users
    ADD CONSTRAINT fk_9jib0g899h0gy3dypo7datfm9 FOREIGN KEY (user_group_id) REFERENCES public.user_group(id);


--
-- Name: analysis_property fk_9o7xs7saygim8y0sm4ostvpc1; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.analysis_property
    ADD CONSTRAINT fk_9o7xs7saygim8y0sm4ostvpc1 FOREIGN KEY (type_id) REFERENCES public.cvterm(id);


--
-- Name: provenance_grails_user fk_a9jxfyd75d96pq57i34uhs8vk; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.provenance_grails_user
    ADD CONSTRAINT fk_a9jxfyd75d96pq57i34uhs8vk FOREIGN KEY (provenance_owners_id) REFERENCES public.provenance(id);


--
-- Name: phenotype_cvterm fk_aicsmj1kn20ikm14292g9r2j9; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.phenotype_cvterm
    ADD CONSTRAINT fk_aicsmj1kn20ikm14292g9r2j9 FOREIGN KEY (phenotype_phenotypecvterms_id) REFERENCES public.phenotype(id);


--
-- Name: feature_feature_phenotypes fk_aqr7eiyx6puju6elciwubbwmo; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.feature_feature_phenotypes
    ADD CONSTRAINT fk_aqr7eiyx6puju6elciwubbwmo FOREIGN KEY (feature_id) REFERENCES public.feature(id);


--
-- Name: go_annotation_grails_user fk_avbwcwpfh7xi4wxms35t7cbti; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.go_annotation_grails_user
    ADD CONSTRAINT fk_avbwcwpfh7xi4wxms35t7cbti FOREIGN KEY (go_annotation_owners_id) REFERENCES public.go_annotation(id);


--
-- Name: feature_genotype fk_b42u9iq4kuqe5ay544do81n32; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.feature_genotype
    ADD CONSTRAINT fk_b42u9iq4kuqe5ay544do81n32 FOREIGN KEY (cvterm_id) REFERENCES public.cvterm(id);


--
-- Name: feature_relationship_publication fk_bdd324e5jb0lpuhs7biy2kacm; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.feature_relationship_publication
    ADD CONSTRAINT fk_bdd324e5jb0lpuhs7biy2kacm FOREIGN KEY (feature_relationship_feature_relationship_publications_id) REFERENCES public.feature_relationship(id);


--
-- Name: phenotype_description fk_bf1bstadamyw0gsarkb933l5b; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.phenotype_description
    ADD CONSTRAINT fk_bf1bstadamyw0gsarkb933l5b FOREIGN KEY (genotype_id) REFERENCES public.genotype(id);


--
-- Name: data_adapter_data_adapter fk_c5a2cdstwj0ydibnu567urh7q; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.data_adapter_data_adapter
    ADD CONSTRAINT fk_c5a2cdstwj0ydibnu567urh7q FOREIGN KEY (data_adapter_id) REFERENCES public.data_adapter(id);


--
-- Name: feature_genotype fk_cm3gqs38fa2lpllgoum8n4kgn; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.feature_genotype
    ADD CONSTRAINT fk_cm3gqs38fa2lpllgoum8n4kgn FOREIGN KEY (feature_id) REFERENCES public.feature(id);


--
-- Name: featurecvterm fk_cuwo3ernssd0t0wjceb7lmm11; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.featurecvterm
    ADD CONSTRAINT fk_cuwo3ernssd0t0wjceb7lmm11 FOREIGN KEY (cvterm_id) REFERENCES public.cvterm(id);


--
-- Name: phenotype fk_cwgh6naf9gackae2ei11v6p41; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.phenotype
    ADD CONSTRAINT fk_cwgh6naf9gackae2ei11v6p41 FOREIGN KEY (assay_id) REFERENCES public.cvterm(id);


--
-- Name: role_permissions fk_d4atqq8ege1sij0316vh2mxfu; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.role_permissions
    ADD CONSTRAINT fk_d4atqq8ege1sij0316vh2mxfu FOREIGN KEY (role_id) REFERENCES public.role(id);


--
-- Name: feature_location fk_dhnrehn3tj85m2j9c0m4md3f4; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.feature_location
    ADD CONSTRAINT fk_dhnrehn3tj85m2j9c0m4md3f4 FOREIGN KEY (sequence_id) REFERENCES public.sequence(id);


--
-- Name: available_status_feature_type fk_dnofis69fbieijg6f562lv4f2; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.available_status_feature_type
    ADD CONSTRAINT fk_dnofis69fbieijg6f562lv4f2 FOREIGN KEY (feature_type_id) REFERENCES public.feature_type(id);


--
-- Name: feature_feature_phenotypes fk_dy5g29heir5ic3d36okyuihho; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.feature_feature_phenotypes
    ADD CONSTRAINT fk_dy5g29heir5ic3d36okyuihho FOREIGN KEY (phenotype_id) REFERENCES public.phenotype(id);


--
-- Name: variant_info fk_e1wsvc8s3bf4rpq0v664hpw9x; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.variant_info
    ADD CONSTRAINT fk_e1wsvc8s3bf4rpq0v664hpw9x FOREIGN KEY (variant_id) REFERENCES public.feature(id);


--
-- Name: canned_value_feature_type fk_e85bethukbu93d3whx6shl3xi; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.canned_value_feature_type
    ADD CONSTRAINT fk_e85bethukbu93d3whx6shl3xi FOREIGN KEY (feature_type_id) REFERENCES public.feature_type(id);


--
-- Name: feature_relationship_feature_property fk_ebgnfbogf1lwdxd8jc17511o7; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.feature_relationship_feature_property
    ADD CONSTRAINT fk_ebgnfbogf1lwdxd8jc17511o7 FOREIGN KEY (feature_relationship_feature_relationship_properties_id) REFERENCES public.feature_relationship(id);


--
-- Name: canned_comment_feature_type fk_es9vpf57b7a14sv803xy64k8h; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.canned_comment_feature_type
    ADD CONSTRAINT fk_es9vpf57b7a14sv803xy64k8h FOREIGN KEY (feature_type_id) REFERENCES public.feature_type(id);


--
-- Name: organism_filter fk_etqcuqd8bk3atmmxit0gul1q; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.organism_filter
    ADD CONSTRAINT fk_etqcuqd8bk3atmmxit0gul1q FOREIGN KEY (suggested_name_id) REFERENCES public.suggested_name(id);


--
-- Name: publication_relationship fk_euu6xx78omdvver8lij1ys2oq; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.publication_relationship
    ADD CONSTRAINT fk_euu6xx78omdvver8lij1ys2oq FOREIGN KEY (subject_publication_id) REFERENCES public.publication(id);


--
-- Name: organism_organism_property fk_f1e1d91q04mqaij1ep3s36ujl; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.organism_organism_property
    ADD CONSTRAINT fk_f1e1d91q04mqaij1ep3s36ujl FOREIGN KEY (organism_property_id) REFERENCES public.organism_property(id);


--
-- Name: gene_product fk_fd4ng8epgxe03opravdgr1k8o; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.gene_product
    ADD CONSTRAINT fk_fd4ng8epgxe03opravdgr1k8o FOREIGN KEY (feature_id) REFERENCES public.feature(id);


--
-- Name: canned_key_feature_type fk_g48bxstocv037qt6sxvc3bda6; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.canned_key_feature_type
    ADD CONSTRAINT fk_g48bxstocv037qt6sxvc3bda6 FOREIGN KEY (feature_type_id) REFERENCES public.feature_type(id);


--
-- Name: featurecvterm_publication fk_g6l9cr99p5dhb0kvs9y0tjwnv; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.featurecvterm_publication
    ADD CONSTRAINT fk_g6l9cr99p5dhb0kvs9y0tjwnv FOREIGN KEY (publication_id) REFERENCES public.publication(id);


--
-- Name: phenotype fk_gb4wy9qesx6vnekxekm18k9xa; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.phenotype
    ADD CONSTRAINT fk_gb4wy9qesx6vnekxekm18k9xa FOREIGN KEY (observable_id) REFERENCES public.cvterm(id);


--
-- Name: phenotype_statement fk_gskh1e7b6qa2du48ayu49lr3s; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.phenotype_statement
    ADD CONSTRAINT fk_gskh1e7b6qa2du48ayu49lr3s FOREIGN KEY (phenotype_id) REFERENCES public.phenotype(id);


--
-- Name: publication fk_h3g8f3q2krcnwmq2nasbanlay; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.publication
    ADD CONSTRAINT fk_h3g8f3q2krcnwmq2nasbanlay FOREIGN KEY (type_id) REFERENCES public.cvterm(id);


--
-- Name: feature_genotype fk_hak8r429shmpho06rbyvwnmt0; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.feature_genotype
    ADD CONSTRAINT fk_hak8r429shmpho06rbyvwnmt0 FOREIGN KEY (chromosome_feature_id) REFERENCES public.feature(id);


--
-- Name: feature fk_hc4vrafs0ws7ugdkp0n6u3xdo; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.feature
    ADD CONSTRAINT fk_hc4vrafs0ws7ugdkp0n6u3xdo FOREIGN KEY (dbxref_id) REFERENCES public.dbxref(id);


--
-- Name: phenotype_statement fk_hffc43aavltp8t5dtwactux5f; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.phenotype_statement
    ADD CONSTRAINT fk_hffc43aavltp8t5dtwactux5f FOREIGN KEY (publication_id) REFERENCES public.publication(id);


--
-- Name: permission fk_hycx5el5itt1lqidt532shkpj; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.permission
    ADD CONSTRAINT fk_hycx5el5itt1lqidt532shkpj FOREIGN KEY (group_id) REFERENCES public.user_group(id);


--
-- Name: preference fk_i94ksmdxi88hcqnuycebgkdvs; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.preference
    ADD CONSTRAINT fk_i94ksmdxi88hcqnuycebgkdvs FOREIGN KEY (sequence_id) REFERENCES public.sequence(id);


--
-- Name: featurecvterm fk_iy7bbt67s7jaemiajsrqalv5o; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.featurecvterm
    ADD CONSTRAINT fk_iy7bbt67s7jaemiajsrqalv5o FOREIGN KEY (feature_id) REFERENCES public.feature(id);


--
-- Name: feature_property_publication fk_j4dnb11fi9vcvrdjo5m352pyq; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.feature_property_publication
    ADD CONSTRAINT fk_j4dnb11fi9vcvrdjo5m352pyq FOREIGN KEY (publication_id) REFERENCES public.publication(id);


--
-- Name: cvterm_path fk_jaqi1bk3t2c0m3pybmparp856; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.cvterm_path
    ADD CONSTRAINT fk_jaqi1bk3t2c0m3pybmparp856 FOREIGN KEY (type_id) REFERENCES public.cvterm(id);


--
-- Name: organism_property_organismdbxref fk_jbw15sttun6yrcrxchi0lwtam; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.organism_property_organismdbxref
    ADD CONSTRAINT fk_jbw15sttun6yrcrxchi0lwtam FOREIGN KEY (organismdbxref_id) REFERENCES public.organismdbxref(id);


--
-- Name: available_status_feature_type fk_jcoeehgesgr9lc34aqsc0iubc; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.available_status_feature_type
    ADD CONSTRAINT fk_jcoeehgesgr9lc34aqsc0iubc FOREIGN KEY (available_status_feature_types_id) REFERENCES public.available_status(id);


--
-- Name: phenotype fk_jh0fc3orduigl8s7ymentbtrs; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.phenotype
    ADD CONSTRAINT fk_jh0fc3orduigl8s7ymentbtrs FOREIGN KEY (cvalue_id) REFERENCES public.cvterm(id);


--
-- Name: user_group_users fk_jppito7humh6e3v5mjjtutd7h; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.user_group_users
    ADD CONSTRAINT fk_jppito7humh6e3v5mjjtutd7h FOREIGN KEY (user_id) REFERENCES public.grails_user(id);


--
-- Name: feature_property fk_jpvdxc57abfiridcr57x8130; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.feature_property
    ADD CONSTRAINT fk_jpvdxc57abfiridcr57x8130 FOREIGN KEY (feature_id) REFERENCES public.feature(id);


--
-- Name: feature_location_publication fk_jquf8fftudekrwgx1e870jy43; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.feature_location_publication
    ADD CONSTRAINT fk_jquf8fftudekrwgx1e870jy43 FOREIGN KEY (feature_location_feature_location_publications_id) REFERENCES public.feature_location(id);


--
-- Name: grails_user_roles fk_jsuq1rc9mb07tg4kubnqn8yw6; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.grails_user_roles
    ADD CONSTRAINT fk_jsuq1rc9mb07tg4kubnqn8yw6 FOREIGN KEY (user_id) REFERENCES public.grails_user(id);


--
-- Name: provenance_grails_user fk_jy0449i139iefb7uuwuvd0hp3; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.provenance_grails_user
    ADD CONSTRAINT fk_jy0449i139iefb7uuwuvd0hp3 FOREIGN KEY (user_id) REFERENCES public.grails_user(id);


--
-- Name: go_annotation fk_k5tey4kvd1siry9ga6008n751; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.go_annotation
    ADD CONSTRAINT fk_k5tey4kvd1siry9ga6008n751 FOREIGN KEY (feature_id) REFERENCES public.feature(id);


--
-- Name: organism_filter fk_k84f8qd6a80roie9yid9oa0gh; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.organism_filter
    ADD CONSTRAINT fk_k84f8qd6a80roie9yid9oa0gh FOREIGN KEY (canned_value_id) REFERENCES public.canned_value(id);


--
-- Name: organism_property_organismdbxref fk_kaayriabr4k4b3aomk46dmc77; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.organism_property_organismdbxref
    ADD CONSTRAINT fk_kaayriabr4k4b3aomk46dmc77 FOREIGN KEY (organism_property_organismdbxrefs_id) REFERENCES public.organism_property(id);


--
-- Name: cvterm_path fk_ke2nrw91sxil8mv7osgv83pw1; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.cvterm_path
    ADD CONSTRAINT fk_ke2nrw91sxil8mv7osgv83pw1 FOREIGN KEY (cv_id) REFERENCES public.cv(id);


--
-- Name: feature fk_kfq8esgv3in8wxml2x36f2md; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.feature
    ADD CONSTRAINT fk_kfq8esgv3in8wxml2x36f2md FOREIGN KEY (status_id) REFERENCES public.feature_property(id);


--
-- Name: organismdbxref fk_l1jfi0wpnyooutd820p5gskr; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.organismdbxref
    ADD CONSTRAINT fk_l1jfi0wpnyooutd820p5gskr FOREIGN KEY (organism_id) REFERENCES public.organism(id);


--
-- Name: analysis_feature fk_l94xl424xp988f06gr2b3t5tw; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.analysis_feature
    ADD CONSTRAINT fk_l94xl424xp988f06gr2b3t5tw FOREIGN KEY (feature_id) REFERENCES public.feature(id);


--
-- Name: feature_grails_user fk_lflwbgxduee8ljjwe5rfbdil2; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.feature_grails_user
    ADD CONSTRAINT fk_lflwbgxduee8ljjwe5rfbdil2 FOREIGN KEY (user_id) REFERENCES public.grails_user(id);


--
-- Name: feature_synonym fk_ll4cqdh994s6x8n7vku1q7iwd; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.feature_synonym
    ADD CONSTRAINT fk_ll4cqdh994s6x8n7vku1q7iwd FOREIGN KEY (synonym_id) REFERENCES public.synonym(id);


--
-- Name: feature_location_publication fk_n4lr2f61atuxmm8cb90qtkojq; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.feature_location_publication
    ADD CONSTRAINT fk_n4lr2f61atuxmm8cb90qtkojq FOREIGN KEY (publication_id) REFERENCES public.publication(id);


--
-- Name: feature_dbxref fk_n6n7lheb1qkmlde8u6gvvjxne; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.feature_dbxref
    ADD CONSTRAINT fk_n6n7lheb1qkmlde8u6gvvjxne FOREIGN KEY (dbxref_id) REFERENCES public.dbxref(id);


--
-- Name: feature_synonym fk_nf9qbuay984ixqd2k1425rnyo; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.feature_synonym
    ADD CONSTRAINT fk_nf9qbuay984ixqd2k1425rnyo FOREIGN KEY (feature_id) REFERENCES public.feature(id);


--
-- Name: dbxref fk_np3tfcu9g867to3qux6raf9y8; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.dbxref
    ADD CONSTRAINT fk_np3tfcu9g867to3qux6raf9y8 FOREIGN KEY (db_id) REFERENCES public.db(id);


--
-- Name: cvterm_path fk_nq02ir0qeydr5tj3071k9gl7b; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.cvterm_path
    ADD CONSTRAINT fk_nq02ir0qeydr5tj3071k9gl7b FOREIGN KEY (subjectcvterm_id) REFERENCES public.cvterm(id);


--
-- Name: cvterm_relationship fk_ob1d0vrfaix8b28j4tvilqnyv; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.cvterm_relationship
    ADD CONSTRAINT fk_ob1d0vrfaix8b28j4tvilqnyv FOREIGN KEY (type_id) REFERENCES public.cvterm(id);


--
-- Name: publicationdbxref fk_oh81hma8qx88fhvcmfugx836b; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.publicationdbxref
    ADD CONSTRAINT fk_oh81hma8qx88fhvcmfugx836b FOREIGN KEY (dbxref_id) REFERENCES public.dbxref(id);


--
-- Name: cvterm fk_oksfqluv12ktmut9s6o9jla7a; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.cvterm
    ADD CONSTRAINT fk_oksfqluv12ktmut9s6o9jla7a FOREIGN KEY (cv_id) REFERENCES public.cv(id);


--
-- Name: allele_info fk_ouqjfw5stpwvacl0o8rdn6i6c; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.allele_info
    ADD CONSTRAINT fk_ouqjfw5stpwvacl0o8rdn6i6c FOREIGN KEY (allele_id) REFERENCES public.allele(id);


--
-- Name: user_group_admin fk_p2dpr2q41es50wbr29lsuhrqi; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.user_group_admin
    ADD CONSTRAINT fk_p2dpr2q41es50wbr29lsuhrqi FOREIGN KEY (user_id) REFERENCES public.grails_user(id);


--
-- Name: gene_product_grails_user fk_p3efo5t83xiypbtpahbaw63q2; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.gene_product_grails_user
    ADD CONSTRAINT fk_p3efo5t83xiypbtpahbaw63q2 FOREIGN KEY (user_id) REFERENCES public.grails_user(id);


--
-- Name: canned_key_feature_type fk_p4j8je0rybguxq29hc2e3hosa; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.canned_key_feature_type
    ADD CONSTRAINT fk_p4j8je0rybguxq29hc2e3hosa FOREIGN KEY (canned_key_feature_types_id) REFERENCES public.canned_key(id);


--
-- Name: publicationdbxref fk_pgoyqd75q47r6ycwowcppbhk6; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.publicationdbxref
    ADD CONSTRAINT fk_pgoyqd75q47r6ycwowcppbhk6 FOREIGN KEY (publication_id) REFERENCES public.publication(id);


--
-- Name: phenotype fk_phmfgylejydjqyrvo3imc97go; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.phenotype
    ADD CONSTRAINT fk_phmfgylejydjqyrvo3imc97go FOREIGN KEY (attribute_id) REFERENCES public.cvterm(id);


--
-- Name: featurecvterm_dbxref fk_pniehpb3pk1rqe95ejk0od6vg; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.featurecvterm_dbxref
    ADD CONSTRAINT fk_pniehpb3pk1rqe95ejk0od6vg FOREIGN KEY (featurecvterm_featurecvtermdbxrefs_id) REFERENCES public.featurecvterm(id);


--
-- Name: gene_product_grails_user fk_pt2dm1hreb8678wlobaiseqnk; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.gene_product_grails_user
    ADD CONSTRAINT fk_pt2dm1hreb8678wlobaiseqnk FOREIGN KEY (gene_product_owners_id) REFERENCES public.gene_product(id);


--
-- Name: cvterm_relationship fk_pwxrfyx6rqu5krq4nj5wa3u4f; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.cvterm_relationship
    ADD CONSTRAINT fk_pwxrfyx6rqu5krq4nj5wa3u4f FOREIGN KEY (subjectcvterm_id) REFERENCES public.cvterm(id);


--
-- Name: featurecvterm fk_q3wop7ii25dgiofnp2l3yj9v0; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.featurecvterm
    ADD CONSTRAINT fk_q3wop7ii25dgiofnp2l3yj9v0 FOREIGN KEY (publication_id) REFERENCES public.publication(id);


--
-- Name: publication_relationship fk_q6hf14oiq9pomkjrhtndonmeh; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.publication_relationship
    ADD CONSTRAINT fk_q6hf14oiq9pomkjrhtndonmeh FOREIGN KEY (type_id) REFERENCES public.cvterm(id);


--
-- Name: phenotype_statement fk_q6jvhi3l7ty0m9tpbn09d8pxj; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.phenotype_statement
    ADD CONSTRAINT fk_q6jvhi3l7ty0m9tpbn09d8pxj FOREIGN KEY (environment_id) REFERENCES public.environment(id);


--
-- Name: feature_location fk_qml7xp9f5uojcw7jwdxcb35le; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.feature_location
    ADD CONSTRAINT fk_qml7xp9f5uojcw7jwdxcb35le FOREIGN KEY (feature_id) REFERENCES public.feature(id);


--
-- Name: feature_publication fk_qolh5l4blkx8vfmwcl7f3woan; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.feature_publication
    ADD CONSTRAINT fk_qolh5l4blkx8vfmwcl7f3woan FOREIGN KEY (feature_feature_publications_id) REFERENCES public.feature(id);


--
-- Name: suggested_name_feature_type fk_qwab1rve68gwjrhmfcs3wkn47; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.suggested_name_feature_type
    ADD CONSTRAINT fk_qwab1rve68gwjrhmfcs3wkn47 FOREIGN KEY (suggested_name_feature_types_id) REFERENCES public.suggested_name(id);


--
-- Name: organism_organism_property fk_qyxdgqthtlgixvtdkkhc8g3pu; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.organism_organism_property
    ADD CONSTRAINT fk_qyxdgqthtlgixvtdkkhc8g3pu FOREIGN KEY (organism_organism_properties_id) REFERENCES public.organism(id);


--
-- Name: cvterm_relationship fk_r1o1rnfnsf7oipuv1h7h1fln7; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.cvterm_relationship
    ADD CONSTRAINT fk_r1o1rnfnsf7oipuv1h7h1fln7 FOREIGN KEY (objectcvterm_id) REFERENCES public.cvterm(id);


--
-- Name: featurecvterm_dbxref fk_r9xhefcekikp1od79ectkb22b; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.featurecvterm_dbxref
    ADD CONSTRAINT fk_r9xhefcekikp1od79ectkb22b FOREIGN KEY (dbxref_id) REFERENCES public.dbxref(id);


--
-- Name: user_group_admin fk_rkx6r039wusc3cl254ljnmkkn; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.user_group_admin
    ADD CONSTRAINT fk_rkx6r039wusc3cl254ljnmkkn FOREIGN KEY (user_group_id) REFERENCES public.user_group(id);


--
-- Name: preference fk_ro87rogww8hoobbwya2nn16xk; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.preference
    ADD CONSTRAINT fk_ro87rogww8hoobbwya2nn16xk FOREIGN KEY (user_id) REFERENCES public.grails_user(id);


--
-- Name: environmentcvterm fk_rrwb96jjqgtg077yv8pbim3jj; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.environmentcvterm
    ADD CONSTRAINT fk_rrwb96jjqgtg077yv8pbim3jj FOREIGN KEY (environment_id) REFERENCES public.environment(id);


--
-- Name: sequence fk_rux0954nxr4lwvj2qgyjibua7; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.sequence
    ADD CONSTRAINT fk_rux0954nxr4lwvj2qgyjibua7 FOREIGN KEY (organism_id) REFERENCES public.organism(id);


--
-- Name: organismdbxref fk_s3vk7onqrk0n4c86xnvqmm3ho; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.organismdbxref
    ADD CONSTRAINT fk_s3vk7onqrk0n4c86xnvqmm3ho FOREIGN KEY (dbxref_id) REFERENCES public.dbxref(id);


--
-- Name: go_annotation_grails_user fk_s6sh7k841fpxc4dk2ok24naa5; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.go_annotation_grails_user
    ADD CONSTRAINT fk_s6sh7k841fpxc4dk2ok24naa5 FOREIGN KEY (user_id) REFERENCES public.grails_user(id);


--
-- Name: feature_relationship_feature_property fk_scm5rx2kuhgkhdfvskyo924cy; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.feature_relationship_feature_property
    ADD CONSTRAINT fk_scm5rx2kuhgkhdfvskyo924cy FOREIGN KEY (feature_property_id) REFERENCES public.feature_property(id);


--
-- Name: organism_filter fk_sd72dgaipyx0koibgt7wobga5; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.organism_filter
    ADD CONSTRAINT fk_sd72dgaipyx0koibgt7wobga5 FOREIGN KEY (organism_id) REFERENCES public.organism(id);


--
-- Name: allele fk_ssx0gv6xbtu3tbwt6ekkmn4iw; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.allele
    ADD CONSTRAINT fk_ssx0gv6xbtu3tbwt6ekkmn4iw FOREIGN KEY (variant_id) REFERENCES public.feature(id);


--
-- Name: phenotype_description fk_t52r166gd8710vffy3aompe7d; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.phenotype_description
    ADD CONSTRAINT fk_t52r166gd8710vffy3aompe7d FOREIGN KEY (environment_id) REFERENCES public.environment(id);


--
-- Name: dbxref_property fk_t6ojbvugx8kou45oklsie3rt5; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.dbxref_property
    ADD CONSTRAINT fk_t6ojbvugx8kou45oklsie3rt5 FOREIGN KEY (type_id) REFERENCES public.cvterm(id);


--
-- Name: organism_filter fk_tjfmsb0nhnpda6dptagpcpbkp; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.organism_filter
    ADD CONSTRAINT fk_tjfmsb0nhnpda6dptagpcpbkp FOREIGN KEY (canned_key_id) REFERENCES public.canned_key(id);


--
-- Name: phenotype_statement fk_tk1pgifvuhurefn0y3myfyyt4; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.phenotype_statement
    ADD CONSTRAINT fk_tk1pgifvuhurefn0y3myfyyt4 FOREIGN KEY (type_id) REFERENCES public.cvterm(id);


--
-- Name: cvterm_path fk_tlfh10092i00g6rlv589naqy5; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.cvterm_path
    ADD CONSTRAINT fk_tlfh10092i00g6rlv589naqy5 FOREIGN KEY (objectcvterm_id) REFERENCES public.cvterm(id);


--
-- Name: feature fk_toot78feskjigpn5d5i5v7o5s; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.feature
    ADD CONSTRAINT fk_toot78feskjigpn5d5i5v7o5s FOREIGN KEY (reference_allele_id) REFERENCES public.allele(id);


--
-- Name: environmentcvterm fk_tql9djnqw1d7migfndoj3lrph; Type: FK CONSTRAINT; Schema: public; Owner: apollo
--

ALTER TABLE ONLY public.environmentcvterm
    ADD CONSTRAINT fk_tql9djnqw1d7migfndoj3lrph FOREIGN KEY (cvterm_id) REFERENCES public.cvterm(id);


--
-- PostgreSQL database dump complete
--

