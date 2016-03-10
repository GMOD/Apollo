--
-- PostgreSQL database dump
--

SET statement_timeout = 0;
SET lock_timeout = 0;
SET client_encoding = 'SQL_ASCII';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

--
-- Name: chado; Type: SCHEMA; Schema: -; Owner: nathandunn
--

CREATE SCHEMA chado;


ALTER SCHEMA chado OWNER TO nathandunn;

--
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


SET search_path = chado, pg_catalog;

--
-- Name: feature_by_fx_type; Type: TYPE; Schema: chado; Owner: nathandunn
--

CREATE TYPE feature_by_fx_type AS (
	feature_id integer,
	depth integer
);


ALTER TYPE feature_by_fx_type OWNER TO nathandunn;

--
-- Name: soi_type; Type: TYPE; Schema: chado; Owner: nathandunn
--

CREATE TYPE soi_type AS (
	type_id integer,
	subject_id integer,
	object_id integer
);


ALTER TYPE soi_type OWNER TO nathandunn;

--
-- Name: _fill_cvtermpath4node(integer, integer, integer, integer, integer); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION _fill_cvtermpath4node(integer, integer, integer, integer, integer) RETURNS integer
    LANGUAGE plpgsql
    AS $_$
DECLARE
    origin alias for $1;
    child_id alias for $2;
    cvid alias for $3;
    typeid alias for $4;
    depth alias for $5;
    cterm cvterm_relationship%ROWTYPE;
    exist_c int;
BEGIN
    --- RAISE NOTICE 'depth=% root=%', depth,child_id;
    --- not check type_id as it may be null and not very meaningful in cvtermpath when pathdistance > 1
    SELECT INTO exist_c count(*) FROM cvtermpath WHERE cv_id = cvid AND object_id = origin AND subject_id = child_id AND pathdistance = depth;
    IF (exist_c = 0) THEN
        INSERT INTO cvtermpath (object_id, subject_id, cv_id, type_id, pathdistance) VALUES(origin, child_id, cvid, typeid, depth);
    END IF;
    FOR cterm IN SELECT * FROM cvterm_relationship WHERE object_id = child_id LOOP
        PERFORM _fill_cvtermpath4node(origin, cterm.subject_id, cvid, cterm.type_id, depth+1);
    END LOOP;
    RETURN 1;
END;
$_$;


ALTER FUNCTION chado._fill_cvtermpath4node(integer, integer, integer, integer, integer) OWNER TO nathandunn;

--
-- Name: _fill_cvtermpath4node2detect_cycle(integer, integer, integer, integer, integer); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION _fill_cvtermpath4node2detect_cycle(integer, integer, integer, integer, integer) RETURNS integer
    LANGUAGE plpgsql
    AS $_$
DECLARE
    origin alias for $1;
    child_id alias for $2;
    cvid alias for $3;
    typeid alias for $4;
    depth alias for $5;
    cterm cvterm_relationship%ROWTYPE;
    exist_c int;
    ccount  int;
    ecount  int;
    rtn     int;
BEGIN
    EXECUTE 'SELECT * FROM tmpcvtermpath p1, tmpcvtermpath p2 WHERE p1.subject_id=p2.object_id AND p1.object_id=p2.subject_id AND p1.object_id = '|| origin || ' AND p2.subject_id = ' || child_id || 'AND ' || depth || '> 0';
    GET DIAGNOSTICS ccount = ROW_COUNT;
    IF (ccount > 0) THEN
        --RAISE EXCEPTION 'FOUND CYCLE: node % on cycle path',origin;
        RETURN origin;
    END IF;
    EXECUTE 'SELECT * FROM tmpcvtermpath WHERE cv_id = ' || cvid || ' AND object_id = ' || origin || ' AND subject_id = ' || child_id || ' AND ' || origin || '<>' || child_id;
    GET DIAGNOSTICS ecount = ROW_COUNT;
    IF (ecount > 0) THEN
        --RAISE NOTICE 'FOUND TWICE (node), will check root obj % subj %',origin, child_id;
        SELECT INTO rtn _fill_cvtermpath4root2detect_cycle(child_id, cvid);
        IF (rtn > 0) THEN
            RETURN rtn;
        END IF;
    END IF;
    EXECUTE 'SELECT * FROM tmpcvtermpath WHERE cv_id = ' || cvid || ' AND object_id = ' || origin || ' AND subject_id = ' || child_id || ' AND pathdistance = ' || depth;
    GET DIAGNOSTICS exist_c = ROW_COUNT;
    IF (exist_c = 0) THEN
        EXECUTE 'INSERT INTO tmpcvtermpath (object_id, subject_id, cv_id, type_id, pathdistance) VALUES(' || origin || ', ' || child_id || ', ' || cvid || ', ' || typeid || ', ' || depth || ')';
    END IF;
    FOR cterm IN SELECT * FROM cvterm_relationship WHERE object_id = child_id LOOP
        --RAISE NOTICE 'DOING for node, % %', origin, cterm.subject_id;
        SELECT INTO rtn _fill_cvtermpath4node2detect_cycle(origin, cterm.subject_id, cvid, cterm.type_id, depth+1);
        IF (rtn > 0) THEN
            RETURN rtn;
        END IF;
    END LOOP;
    RETURN 0;
END;
$_$;


ALTER FUNCTION chado._fill_cvtermpath4node2detect_cycle(integer, integer, integer, integer, integer) OWNER TO nathandunn;

--
-- Name: _fill_cvtermpath4root(integer, integer); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION _fill_cvtermpath4root(integer, integer) RETURNS integer
    LANGUAGE plpgsql
    AS $_$
DECLARE
    rootid alias for $1;
    cvid alias for $2;
    ttype int;
    cterm cvterm_relationship%ROWTYPE;
    child cvterm_relationship%ROWTYPE;
BEGIN
    SELECT INTO ttype cvterm_id FROM cvterm WHERE (name = 'isa' OR name = 'is_a');
    PERFORM _fill_cvtermpath4node(rootid, rootid, cvid, ttype, 0);
    FOR cterm IN SELECT * FROM cvterm_relationship WHERE object_id = rootid LOOP
        PERFORM _fill_cvtermpath4root(cterm.subject_id, cvid);
        -- RAISE NOTICE 'DONE for term, %', cterm.subject_id;
    END LOOP;
    RETURN 1;
END;
$_$;


ALTER FUNCTION chado._fill_cvtermpath4root(integer, integer) OWNER TO nathandunn;

--
-- Name: _fill_cvtermpath4root2detect_cycle(integer, integer); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION _fill_cvtermpath4root2detect_cycle(integer, integer) RETURNS integer
    LANGUAGE plpgsql
    AS $_$
DECLARE
    rootid alias for $1;
    cvid alias for $2;
    ttype int;
    ccount int;
    cterm cvterm_relationship%ROWTYPE;
    child cvterm_relationship%ROWTYPE;
    rtn     int;
BEGIN
    SELECT INTO ttype cvterm_id FROM cvterm WHERE (name = 'isa' OR name = 'is_a');
    SELECT INTO rtn _fill_cvtermpath4node2detect_cycle(rootid, rootid, cvid, ttype, 0);
    IF (rtn > 0) THEN
        RETURN rtn;
    END IF;
    FOR cterm IN SELECT * FROM cvterm_relationship WHERE object_id = rootid LOOP
        EXECUTE 'SELECT * FROM tmpcvtermpath p1, tmpcvtermpath p2 WHERE p1.subject_id=p2.object_id AND p1.object_id=p2.subject_id AND p1.object_id=' || rootid || ' AND p1.subject_id=' || cterm.subject_id;
        GET DIAGNOSTICS ccount = ROW_COUNT;
        IF (ccount > 0) THEN
            --RAISE NOTICE 'FOUND TWICE (root), will check root obj % subj %',rootid,cterm.subject_id;
            SELECT INTO rtn _fill_cvtermpath4node2detect_cycle(rootid, cterm.subject_id, cvid, ttype, 0);
            IF (rtn > 0) THEN
                RETURN rtn;
            END IF;
        ELSE
            SELECT INTO rtn _fill_cvtermpath4root2detect_cycle(cterm.subject_id, cvid);
            IF (rtn > 0) THEN
                RETURN rtn;
            END IF;
        END IF;
    END LOOP;
    RETURN 0;
END;
$_$;


ALTER FUNCTION chado._fill_cvtermpath4root2detect_cycle(integer, integer) OWNER TO nathandunn;

--
-- Name: _fill_cvtermpath4soi(integer, integer); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION _fill_cvtermpath4soi(integer, integer) RETURNS integer
    LANGUAGE plpgsql
    AS $_$
DECLARE
    rootid alias for $1;
    cvid alias for $2;
    ttype int;
    cterm soi_type%ROWTYPE;
BEGIN
    SELECT INTO ttype cvterm_id FROM cvterm WHERE name = 'isa';
    --RAISE NOTICE 'got ttype %',ttype;
    PERFORM _fill_cvtermpath4soinode(rootid, rootid, cvid, ttype, 0);
    FOR cterm IN SELECT tmp_type AS type_id, subject_id FROM tmpcvtr WHERE object_id = rootid LOOP
        PERFORM _fill_cvtermpath4soi(cterm.subject_id, cvid);
    END LOOP;
    RETURN 1;
END;   
$_$;


ALTER FUNCTION chado._fill_cvtermpath4soi(integer, integer) OWNER TO nathandunn;

--
-- Name: _fill_cvtermpath4soinode(integer, integer, integer, integer, integer); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION _fill_cvtermpath4soinode(integer, integer, integer, integer, integer) RETURNS integer
    LANGUAGE plpgsql
    AS $_$
DECLARE
    origin alias for $1;
    child_id alias for $2;
    cvid alias for $3;
    typeid alias for $4;
    depth alias for $5;
    cterm soi_type%ROWTYPE;
    exist_c int;
BEGIN
    --RAISE NOTICE 'depth=% o=%, root=%, cv=%, t=%', depth,origin,child_id,cvid,typeid;
    SELECT INTO exist_c count(*) FROM cvtermpath WHERE cv_id = cvid AND object_id = origin AND subject_id = child_id AND pathdistance = depth;
    --- longest path
    IF (exist_c > 0) THEN
        UPDATE cvtermpath SET pathdistance = depth WHERE cv_id = cvid AND object_id = origin AND subject_id = child_id;
    ELSE
        INSERT INTO cvtermpath (object_id, subject_id, cv_id, type_id, pathdistance) VALUES(origin, child_id, cvid, typeid, depth);
    END IF;
    FOR cterm IN SELECT tmp_type AS type_id, subject_id FROM tmpcvtr WHERE object_id = child_id LOOP
        PERFORM _fill_cvtermpath4soinode(origin, cterm.subject_id, cvid, cterm.type_id, depth+1);
    END LOOP;
    RETURN 1;
END;
$_$;


ALTER FUNCTION chado._fill_cvtermpath4soinode(integer, integer, integer, integer, integer) OWNER TO nathandunn;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: cvtermpath; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE cvtermpath (
    cvtermpath_id integer NOT NULL,
    type_id integer,
    subject_id integer NOT NULL,
    object_id integer NOT NULL,
    cv_id integer NOT NULL,
    pathdistance integer
);


ALTER TABLE cvtermpath OWNER TO nathandunn;

--
-- Name: TABLE cvtermpath; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE cvtermpath IS 'The reflexive transitive closure of
the cvterm_relationship relation.';


--
-- Name: COLUMN cvtermpath.type_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN cvtermpath.type_id IS 'The relationship type that
this is a closure over. If null, then this is a closure over ALL
relationship types. If non-null, then this references a relationship
cvterm - note that the closure will apply to both this relationship
AND the OBO_REL:is_a (subclass) relationship.';


--
-- Name: COLUMN cvtermpath.cv_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN cvtermpath.cv_id IS 'Closures will mostly be within
one cv. If the closure of a relationship traverses a cv, then this
refers to the cv of the object_id cvterm.';


--
-- Name: COLUMN cvtermpath.pathdistance; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN cvtermpath.pathdistance IS 'The number of steps
required to get from the subject cvterm to the object cvterm, counting
from zero (reflexive relationship).';


--
-- Name: _get_all_object_ids(integer); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION _get_all_object_ids(integer) RETURNS SETOF cvtermpath
    LANGUAGE plpgsql
    AS $_$
DECLARE
    leaf alias for $1;
    cterm cvtermpath%ROWTYPE;
    cterm2 cvtermpath%ROWTYPE;
BEGIN
    FOR cterm IN SELECT * FROM cvterm_relationship WHERE subject_id = leaf LOOP
        RETURN NEXT cterm;
        FOR cterm2 IN SELECT * FROM _get_all_object_ids(cterm.object_id) LOOP
            RETURN NEXT cterm2;
        END LOOP;
    END LOOP;
    RETURN;
END;   
$_$;


ALTER FUNCTION chado._get_all_object_ids(integer) OWNER TO nathandunn;

--
-- Name: _get_all_subject_ids(integer); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION _get_all_subject_ids(integer) RETURNS SETOF cvtermpath
    LANGUAGE plpgsql
    AS $_$
DECLARE
    root alias for $1;
    cterm cvtermpath%ROWTYPE;
    cterm2 cvtermpath%ROWTYPE;
BEGIN
    FOR cterm IN SELECT * FROM cvterm_relationship WHERE object_id = root LOOP
        RETURN NEXT cterm;
        FOR cterm2 IN SELECT * FROM _get_all_subject_ids(cterm.subject_id) LOOP
            RETURN NEXT cterm2;
        END LOOP;
    END LOOP;
    RETURN;
END;   
$_$;


ALTER FUNCTION chado._get_all_subject_ids(integer) OWNER TO nathandunn;

--
-- Name: boxquery(integer, integer); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION boxquery(integer, integer) RETURNS box
    LANGUAGE sql IMMUTABLE
    AS $_$SELECT box (create_point($1, $2), create_point($1, $2))$_$;


ALTER FUNCTION chado.boxquery(integer, integer) OWNER TO nathandunn;

--
-- Name: boxquery(integer, integer, integer); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION boxquery(integer, integer, integer) RETURNS box
    LANGUAGE sql IMMUTABLE
    AS $_$SELECT box (create_point($1, $2), create_point($1, $3))$_$;


ALTER FUNCTION chado.boxquery(integer, integer, integer) OWNER TO nathandunn;

--
-- Name: boxrange(integer, integer); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION boxrange(integer, integer) RETURNS box
    LANGUAGE sql IMMUTABLE
    AS $_$SELECT box (create_point(0, $1), create_point($2,500000000))$_$;


ALTER FUNCTION chado.boxrange(integer, integer) OWNER TO nathandunn;

--
-- Name: boxrange(integer, integer, integer); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION boxrange(integer, integer, integer) RETURNS box
    LANGUAGE sql IMMUTABLE
    AS $_$SELECT box (create_point($1, $2), create_point($1,$3))$_$;


ALTER FUNCTION chado.boxrange(integer, integer, integer) OWNER TO nathandunn;

--
-- Name: complement_residues(text); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION complement_residues(text) RETURNS text
    LANGUAGE sql
    AS $_$SELECT (translate($1, 
                   'acgtrymkswhbvdnxACGTRYMKSWHBVDNX',
                   'tgcayrkmswdvbhnxTGCAYRKMSWDVBHNX'))$_$;


ALTER FUNCTION chado.complement_residues(text) OWNER TO nathandunn;

--
-- Name: concat_pair(text, text); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION concat_pair(text, text) RETURNS text
    LANGUAGE sql
    AS $_$SELECT $1 || $2$_$;


ALTER FUNCTION chado.concat_pair(text, text) OWNER TO nathandunn;

--
-- Name: create_point(integer, integer); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION create_point(integer, integer) RETURNS point
    LANGUAGE sql
    AS $_$SELECT point ($1, $2)$_$;


ALTER FUNCTION chado.create_point(integer, integer) OWNER TO nathandunn;

--
-- Name: create_soi(); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION create_soi() RETURNS integer
    LANGUAGE plpgsql
    AS $$
DECLARE
    parent soi_type%ROWTYPE;
    isa_id cvterm.cvterm_id%TYPE;
    soi_term TEXT := 'soi';
    soi_def TEXT := 'ontology of SO feature instantiated in database';
    soi_cvid INTEGER;
    soiterm_id INTEGER;
    pcount INTEGER;
    count INTEGER := 0;
    cquery TEXT;
BEGIN
    SELECT INTO isa_id cvterm_id FROM cvterm WHERE name = 'isa';
    SELECT INTO soi_cvid cv_id FROM cv WHERE name = soi_term;
    IF (soi_cvid > 0) THEN
        DELETE FROM cvtermpath WHERE cv_id = soi_cvid;
        DELETE FROM cvterm WHERE cv_id = soi_cvid;
    ELSE
        INSERT INTO cv (name, definition) VALUES(soi_term, soi_def);
    END IF;
    SELECT INTO soi_cvid cv_id FROM cv WHERE name = soi_term;
    INSERT INTO cvterm (name, cv_id) VALUES(soi_term, soi_cvid);
    SELECT INTO soiterm_id cvterm_id FROM cvterm WHERE name = soi_term;
    CREATE TEMP TABLE tmpcvtr (tmp_type INT, type_id INT, subject_id INT, object_id INT);
    CREATE UNIQUE INDEX u_tmpcvtr ON tmpcvtr(subject_id, object_id);
    INSERT INTO tmpcvtr (tmp_type, type_id, subject_id, object_id)
        SELECT DISTINCT isa_id, soiterm_id, f.type_id, soiterm_id FROM feature f, cvterm t
        WHERE f.type_id = t.cvterm_id AND f.type_id > 0;
    EXECUTE 'select * from tmpcvtr where type_id = ' || soiterm_id || ';';
    get diagnostics pcount = row_count;
    raise notice 'all types in feature %',pcount;
--- do it hard way, delete any child feature type from above (NOT IN clause did not work)
    FOR parent IN SELECT DISTINCT 0, t.cvterm_id, 0 FROM feature c, feature_relationship fr, cvterm t
            WHERE t.cvterm_id = c.type_id AND c.feature_id = fr.subject_id LOOP
        DELETE FROM tmpcvtr WHERE type_id = soiterm_id and object_id = soiterm_id
            AND subject_id = parent.subject_id;
    END LOOP;
    EXECUTE 'select * from tmpcvtr where type_id = ' || soiterm_id || ';';
    get diagnostics pcount = row_count;
    raise notice 'all types in feature after delete child %',pcount;
    --- create feature type relationship (store in tmpcvtr)
    CREATE TEMP TABLE tmproot (cv_id INTEGER not null, cvterm_id INTEGER not null, status INTEGER DEFAULT 0);
    cquery := 'SELECT * FROM tmproot tmp WHERE tmp.status = 0;';
    ---temp use tmpcvtr to hold instantiated SO relationship for speed
    ---use soterm_id as type_id, will delete from tmpcvtr
    ---us tmproot for this as well
    INSERT INTO tmproot (cv_id, cvterm_id, status) SELECT DISTINCT soi_cvid, c.subject_id, 0 FROM tmpcvtr c
        WHERE c.object_id = soiterm_id;
    EXECUTE cquery;
    GET DIAGNOSTICS pcount = ROW_COUNT;
    WHILE (pcount > 0) LOOP
        RAISE NOTICE 'num child temp (to be inserted) in tmpcvtr: %',pcount;
        INSERT INTO tmpcvtr (tmp_type, type_id, subject_id, object_id)
            SELECT DISTINCT fr.type_id, soiterm_id, c.type_id, p.cvterm_id FROM feature c, feature_relationship fr,
            tmproot p, feature pf, cvterm t WHERE c.feature_id = fr.subject_id AND fr.object_id = pf.feature_id
            AND p.cvterm_id = pf.type_id AND t.cvterm_id = c.type_id AND p.status = 0;
        UPDATE tmproot SET status = 1 WHERE status = 0;
        INSERT INTO tmproot (cv_id, cvterm_id, status)
            SELECT DISTINCT soi_cvid, c.type_id, 0 FROM feature c, feature_relationship fr,
            tmproot tmp, feature p, cvterm t WHERE c.feature_id = fr.subject_id AND fr.object_id = p.feature_id
            AND tmp.cvterm_id = p.type_id AND t.cvterm_id = c.type_id AND tmp.status = 1;
        UPDATE tmproot SET status = 2 WHERE status = 1;
        EXECUTE cquery;
        GET DIAGNOSTICS pcount = ROW_COUNT; 
    END LOOP;
    DELETE FROM tmproot;
    ---get transitive closure for soi
    PERFORM _fill_cvtermpath4soi(soiterm_id, soi_cvid);
    DROP TABLE tmpcvtr;
    DROP TABLE tmproot;
    RETURN 1;
END;
$$;


ALTER FUNCTION chado.create_soi() OWNER TO nathandunn;

--
-- Name: feature; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE feature (
    feature_id integer NOT NULL,
    dbxref_id integer,
    organism_id integer NOT NULL,
    name character varying(255),
    uniquename text NOT NULL,
    residues text,
    seqlen integer,
    md5checksum character(32),
    type_id integer NOT NULL,
    is_analysis boolean DEFAULT false NOT NULL,
    is_obsolete boolean DEFAULT false NOT NULL,
    timeaccessioned timestamp without time zone DEFAULT now() NOT NULL,
    timelastmodified timestamp without time zone DEFAULT now() NOT NULL,
    searchable_name tsvector
);
ALTER TABLE ONLY feature ALTER COLUMN residues SET STORAGE EXTERNAL;


ALTER TABLE feature OWNER TO nathandunn;

--
-- Name: TABLE feature; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE feature IS 'A feature is a biological sequence or a
section of a biological sequence, or a collection of such
sections. Examples include genes, exons, transcripts, regulatory
regions, polypeptides, protein domains, chromosome sequences, sequence
variations, cross-genome match regions such as hits and HSPs and so
on; see the Sequence Ontology for more. The combination of
organism_id, uniquename and type_id should be unique.';


--
-- Name: COLUMN feature.dbxref_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN feature.dbxref_id IS 'An optional primary chado.stable
identifier for this feature. Secondary identifiers and external
dbxrefs go in the table feature_dbxref.';


--
-- Name: COLUMN feature.organism_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN feature.organism_id IS 'The organism to which this feature
belongs. This column is mandatory.';


--
-- Name: COLUMN feature.name; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN feature.name IS 'The optional human-readable common name for
a feature, for display purposes.';


--
-- Name: COLUMN feature.uniquename; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN feature.uniquename IS 'The unique name for a feature; may
not be necessarily be particularly human-readable, although this is
preferred. This name must be unique for this type of feature within
this organism.';


--
-- Name: COLUMN feature.residues; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN feature.residues IS 'A sequence of alphabetic characters
representing biological residues (nucleic acids, amino acids). This
column does not need to be manifested for all features; it is optional
for features such as exons where the residues can be derived from the
featureloc. It is recommended that the value for this column be
manifested for features which may may non-contiguous sublocations (e.g.
transcripts), since derivation at query time is non-trivial. For
expressed sequence, the DNA sequence should be used rather than the
RNA sequence. The default storage method for the residues column is
EXTERNAL, which will store it uncompressed to make substring operations
faster.';


--
-- Name: COLUMN feature.seqlen; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN feature.seqlen IS 'The length of the residue feature. See
column:residues. This column is partially redundant with the residues
column, and also with featureloc. This column is required because the
location may be unknown and the residue sequence may not be
manifested, yet it may be desirable to store and query the length of
the feature. The seqlen should always be manifested where the length
of the sequence is known.';


--
-- Name: COLUMN feature.md5checksum; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN feature.md5checksum IS 'The 32-character checksum of the sequence,
calculated using the MD5 algorithm. This is practically guaranteed to
be unique for any feature. This column thus acts as a unique
identifier on the mathematical sequence.';


--
-- Name: COLUMN feature.type_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN feature.type_id IS 'A required reference to a table:cvterm
giving the feature type. This will typically be a Sequence Ontology
identifier. This column is thus used to subclass the feature table.';


--
-- Name: COLUMN feature.is_analysis; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN feature.is_analysis IS 'Boolean indicating whether this
feature is annotated or the result of an automated analysis. Analysis
results also use the companalysis module. Note that the dividing line
between analysis and annotation may be fuzzy, this should be determined on
a per-project basis in a consistent manner. One requirement is that
there should only be one non-analysis version of each wild-type gene
feature in a genome, whereas the same gene feature can be predicted
multiple times in different analyses.';


--
-- Name: COLUMN feature.is_obsolete; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN feature.is_obsolete IS 'Boolean indicating whether this
feature has been obsoleted. Some chado instances may choose to simply
remove the feature altogether, others may choose to keep an obsolete
row in the table.';


--
-- Name: COLUMN feature.timeaccessioned; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN feature.timeaccessioned IS 'For handling object
accession or modification timestamps (as opposed to database auditing data,
handled elsewhere). The expectation is that these fields would be
available to software interacting with chado.';


--
-- Name: COLUMN feature.timelastmodified; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN feature.timelastmodified IS 'For handling object
accession or modification timestamps (as opposed to database auditing data,
handled elsewhere). The expectation is that these fields would be
available to software interacting with chado.';


--
-- Name: feature_disjoint_from(integer); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION feature_disjoint_from(integer) RETURNS SETOF feature
    LANGUAGE sql
    AS $_$SELECT feature.*
  FROM feature
   INNER JOIN featureloc AS x ON (x.feature_id=feature.feature_id)
   INNER JOIN featureloc AS y ON (y.feature_id = $1)
  WHERE
   x.srcfeature_id = y.srcfeature_id            AND
   ( x.fmax < y.fmin OR x.fmin > y.fmax ) $_$;


ALTER FUNCTION chado.feature_disjoint_from(integer) OWNER TO nathandunn;

--
-- Name: feature_overlaps(integer); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION feature_overlaps(integer) RETURNS SETOF feature
    LANGUAGE sql
    AS $_$SELECT feature.*
  FROM feature
   INNER JOIN featureloc AS x ON (x.feature_id=feature.feature_id)
   INNER JOIN featureloc AS y ON (y.feature_id = $1)
  WHERE
   x.srcfeature_id = y.srcfeature_id            AND
   ( x.fmax >= y.fmin AND x.fmin <= y.fmax ) $_$;


ALTER FUNCTION chado.feature_overlaps(integer) OWNER TO nathandunn;

--
-- Name: featureloc; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE featureloc (
    featureloc_id integer NOT NULL,
    feature_id integer NOT NULL,
    srcfeature_id integer,
    fmin integer,
    is_fmin_partial boolean DEFAULT false NOT NULL,
    fmax integer,
    is_fmax_partial boolean DEFAULT false NOT NULL,
    strand smallint,
    phase integer,
    residue_info text,
    locgroup integer DEFAULT 0 NOT NULL,
    rank integer DEFAULT 0 NOT NULL,
    CONSTRAINT featureloc_c2 CHECK ((fmin <= fmax))
);


ALTER TABLE featureloc OWNER TO nathandunn;

--
-- Name: TABLE featureloc; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE featureloc IS 'The location of a feature relative to
another feature. Important: interbase coordinates are used. This is
vital as it allows us to represent zero-length features e.g. splice
sites, insertion points without an awkward fuzzy system. Features
typically have exactly ONE location, but this need not be the
case. Some features may not be localized (e.g. a gene that has been
characterized genetically but no sequence or molecular information is
available). Note on multiple locations: Each feature can have 0 or
more locations. Multiple locations do NOT indicate non-contiguous
locations (if a feature such as a transcript has a non-contiguous
location, then the subfeatures such as exons should always be
manifested). Instead, multiple featurelocs for a feature designate
alternate locations or grouped locations; for instance, a feature
designating a blast hit or hsp will have two locations, one on the
query feature, one on the subject feature. Features representing
sequence variation could have alternate locations instantiated on a
feature on the mutant strain. The column:rank is used to
differentiate these different locations. Reflexive locations should
never be stored - this is for -proper- (i.e. non-self) locations only; nothing should be located relative to itself.';


--
-- Name: COLUMN featureloc.feature_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN featureloc.feature_id IS 'The feature that is being located. Any feature can have zero or more featurelocs.';


--
-- Name: COLUMN featureloc.srcfeature_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN featureloc.srcfeature_id IS 'The source feature which this location is relative to. Every location is relative to another feature (however, this column is nullable, because the srcfeature may not be known). All locations are -proper- that is, nothing should be located relative to itself. No cycles are allowed in the featureloc graph.';


--
-- Name: COLUMN featureloc.fmin; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN featureloc.fmin IS 'The leftmost/minimal boundary in the linear range represented by the featureloc. Sometimes (e.g. in Bioperl) this is called -start- although this is confusing because it does not necessarily represent the 5-prime coordinate. Important: This is space-based (interbase) coordinates, counting from zero. To convert this to the leftmost position in a base-oriented system (eg GFF, Bioperl), add 1 to fmin.';


--
-- Name: COLUMN featureloc.is_fmin_partial; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN featureloc.is_fmin_partial IS 'This is typically
false, but may be true if the value for column:fmin is inaccurate or
the leftmost part of the range is unknown/unbounded.';


--
-- Name: COLUMN featureloc.fmax; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN featureloc.fmax IS 'The rightmost/maximal boundary in the linear range represented by the featureloc. Sometimes (e.g. in bioperl) this is called -end- although this is confusing because it does not necessarily represent the 3-prime coordinate. Important: This is space-based (interbase) coordinates, counting from zero. No conversion is required to go from fmax to the rightmost coordinate in a base-oriented system that counts from 1 (e.g. GFF, Bioperl).';


--
-- Name: COLUMN featureloc.is_fmax_partial; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN featureloc.is_fmax_partial IS 'This is typically
false, but may be true if the value for column:fmax is inaccurate or
the rightmost part of the range is unknown/unbounded.';


--
-- Name: COLUMN featureloc.strand; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN featureloc.strand IS 'The orientation/directionality of the
location. Should be 0, -1 or +1.';


--
-- Name: COLUMN featureloc.phase; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN featureloc.phase IS 'Phase of translation with
respect to srcfeature_id.
Values are 0, 1, 2. It may not be possible to manifest this column for
some features such as exons, because the phase is dependant on the
spliceform (the same exon can appear in multiple spliceforms). This column is mostly useful for predicted exons and CDSs.';


--
-- Name: COLUMN featureloc.residue_info; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN featureloc.residue_info IS 'Alternative residues,
when these differ from feature.residues. For instance, a SNP feature
located on a wild and mutant protein would have different alternative residues.
for alignment/similarity features, the alternative residues is used to
represent the alignment string (CIGAR format). Note on variation
features; even if we do not want to instantiate a mutant
chromosome/contig feature, we can still represent a SNP etc with 2
locations, one (rank 0) on the genome, the other (rank 1) would have
most fields null, except for alternative residues.';


--
-- Name: COLUMN featureloc.locgroup; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN featureloc.locgroup IS 'This is used to manifest redundant,
derivable extra locations for a feature. The default locgroup=0 is
used for the DIRECT location of a feature. Important: most Chado users may
never use featurelocs WITH logroup > 0. Transitively derived locations
are indicated with locgroup > 0. For example, the position of an exon on
a BAC and in global chromosome coordinates. This column is used to
differentiate these groupings of locations. The default locgroup 0
is used for the main or primary location, from which the others can be
derived via coordinate transformations. Another example of redundant
locations is storing ORF coordinates relative to both transcript and
genome. Redundant locations open the possibility of the database
getting into inconsistent states; this schema gives us the flexibility
of both warehouse instantiations with redundant locations (easier for
querying) and management instantiations with no redundant
locations. An example of using both locgroup and rank: imagine a
feature indicating a conserved region between the chromosomes of two
different species. We may want to keep redundant locations on both
contigs and chromosomes. We would thus have 4 locations for the single
conserved region feature - two distinct locgroups (contig level and
chromosome level) and two distinct ranks (for the two species).';


--
-- Name: COLUMN featureloc.rank; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN featureloc.rank IS 'Used when a feature has >1
location, otherwise the default rank 0 is used. Some features (e.g.
blast hits and HSPs) have two locations - one on the query and one on
the subject. Rank is used to differentiate these. Rank=0 is always
used for the query, Rank=1 for the subject. For multiple alignments,
assignment of rank is arbitrary. Rank is also used for
sequence_variant features, such as SNPs. Rank=0 indicates the wildtype
(or baseline) feature, Rank=1 indicates the mutant (or compared) feature.';


--
-- Name: feature_subalignments(integer); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION feature_subalignments(integer) RETURNS SETOF featureloc
    LANGUAGE plpgsql
    AS $_$
DECLARE
  return_data featureloc%ROWTYPE;
  f_id ALIAS FOR $1;
  feature_data feature%rowtype;
  featureloc_data featureloc%rowtype;
  s text;
  fmin integer;
  slen integer;
BEGIN
  --RAISE NOTICE 'feature_id is %', featureloc_data.feature_id;
  SELECT INTO feature_data * FROM feature WHERE feature_id = f_id;
  FOR featureloc_data IN SELECT * FROM featureloc WHERE feature_id = f_id LOOP
    --RAISE NOTICE 'fmin is %', featureloc_data.fmin;
    return_data.feature_id      = f_id;
    return_data.srcfeature_id   = featureloc_data.srcfeature_id;
    return_data.is_fmin_partial = featureloc_data.is_fmin_partial;
    return_data.is_fmax_partial = featureloc_data.is_fmax_partial;
    return_data.strand          = featureloc_data.strand;
    return_data.phase           = featureloc_data.phase;
    return_data.residue_info    = featureloc_data.residue_info;
    return_data.locgroup        = featureloc_data.locgroup;
    return_data.rank            = featureloc_data.rank;
    s = feature_data.residues;
    fmin = featureloc_data.fmin;
    slen = char_length(s);
    WHILE char_length(s) LOOP
      --RAISE NOTICE 'residues is %', s;
      --trim off leading match
      s = trim(leading '|ATCGNatcgn' from s);
      --if leading match detected
      IF slen > char_length(s) THEN
        return_data.fmin = fmin;
        return_data.fmax = featureloc_data.fmin + (slen - char_length(s));
        --if the string started with a match, return it,
        --otherwise, trim the gaps first (ie do not return this iteration)
        RETURN NEXT return_data;
      END IF;
      --trim off leading gap
      s = trim(leading '-' from s);
      fmin = featureloc_data.fmin + (slen - char_length(s));
    END LOOP;
  END LOOP;
  RETURN;
END;
$_$;


ALTER FUNCTION chado.feature_subalignments(integer) OWNER TO nathandunn;

--
-- Name: featureloc_slice(integer, integer); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION featureloc_slice(integer, integer) RETURNS SETOF featureloc
    LANGUAGE sql
    AS $_$SELECT * from featureloc where boxquery($1, $2) @ boxrange(fmin,fmax)$_$;


ALTER FUNCTION chado.featureloc_slice(integer, integer) OWNER TO nathandunn;

--
-- Name: featureloc_slice(integer, integer, integer); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION featureloc_slice(integer, integer, integer) RETURNS SETOF featureloc
    LANGUAGE sql
    AS $_$SELECT * 
   FROM featureloc 
   WHERE boxquery($1, $2, $3) && boxrange(srcfeature_id,fmin,fmax)$_$;


ALTER FUNCTION chado.featureloc_slice(integer, integer, integer) OWNER TO nathandunn;

--
-- Name: featureloc_slice(character varying, integer, integer); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION featureloc_slice(character varying, integer, integer) RETURNS SETOF featureloc
    LANGUAGE sql
    AS $_$SELECT featureloc.* 
   FROM featureloc 
   INNER JOIN feature AS srcf ON (srcf.feature_id = featureloc.srcfeature_id)
   WHERE boxquery($2, $3) @ boxrange(fmin,fmax)
   AND srcf.name = $1 $_$;


ALTER FUNCTION chado.featureloc_slice(character varying, integer, integer) OWNER TO nathandunn;

--
-- Name: featureslice(integer, integer); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION featureslice(integer, integer) RETURNS SETOF featureloc
    LANGUAGE sql
    AS $_$SELECT * from featureloc where boxquery($1, $2) @ boxrange(fmin,fmax)$_$;


ALTER FUNCTION chado.featureslice(integer, integer) OWNER TO nathandunn;

--
-- Name: fill_cvtermpath(integer); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION fill_cvtermpath(integer) RETURNS integer
    LANGUAGE plpgsql
    AS $_$
DECLARE
    cvid alias for $1;
    root cvterm%ROWTYPE;
BEGIN
    DELETE FROM cvtermpath WHERE cv_id = cvid;
    FOR root IN SELECT DISTINCT t.* from cvterm t LEFT JOIN cvterm_relationship r ON (t.cvterm_id = r.subject_id) INNER JOIN cvterm_relationship r2 ON (t.cvterm_id = r2.object_id) WHERE t.cv_id = cvid AND r.subject_id is null LOOP
        PERFORM _fill_cvtermpath4root(root.cvterm_id, root.cv_id);
    END LOOP;
    RETURN 1;
END;   
$_$;


ALTER FUNCTION chado.fill_cvtermpath(integer) OWNER TO nathandunn;

--
-- Name: fill_cvtermpath(character varying); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION fill_cvtermpath(character varying) RETURNS integer
    LANGUAGE plpgsql
    AS $_$
DECLARE
    cvname alias for $1;
    cv_id   int;
    rtn     int;
BEGIN
    SELECT INTO cv_id cv.cv_id from cv WHERE cv.name = cvname;
    SELECT INTO rtn fill_cvtermpath(cv_id);
    RETURN rtn;
END;   
$_$;


ALTER FUNCTION chado.fill_cvtermpath(character varying) OWNER TO nathandunn;

--
-- Name: get_all_object_ids(integer); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION get_all_object_ids(integer) RETURNS SETOF cvtermpath
    LANGUAGE plpgsql
    AS $_$
DECLARE
    leaf alias for $1;
    cterm cvtermpath%ROWTYPE;
    exist_c int;
BEGIN
    SELECT INTO exist_c count(*) FROM cvtermpath WHERE object_id = leaf and pathdistance <= 0;
    IF (exist_c > 0) THEN
        FOR cterm IN SELECT * FROM cvtermpath WHERE subject_id = leaf AND pathdistance > 0 LOOP
            RETURN NEXT cterm;
        END LOOP;
    ELSE
        FOR cterm IN SELECT * FROM _get_all_object_ids(leaf) LOOP
            RETURN NEXT cterm;
        END LOOP;
    END IF;
    RETURN;
END;   
$_$;


ALTER FUNCTION chado.get_all_object_ids(integer) OWNER TO nathandunn;

--
-- Name: get_all_subject_ids(integer); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION get_all_subject_ids(integer) RETURNS SETOF cvtermpath
    LANGUAGE plpgsql
    AS $_$
DECLARE
    root alias for $1;
    cterm cvtermpath%ROWTYPE;
    exist_c int;
BEGIN
    SELECT INTO exist_c count(*) FROM cvtermpath WHERE object_id = root and pathdistance <= 0;
    IF (exist_c > 0) THEN
        FOR cterm IN SELECT * FROM cvtermpath WHERE object_id = root and pathdistance > 0 LOOP
            RETURN NEXT cterm;
        END LOOP;
    ELSE
        FOR cterm IN SELECT * FROM _get_all_subject_ids(root) LOOP
            RETURN NEXT cterm;
        END LOOP;
    END IF;
    RETURN;
END;   
$_$;


ALTER FUNCTION chado.get_all_subject_ids(integer) OWNER TO nathandunn;

--
-- Name: get_cv_id_for_feature(); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION get_cv_id_for_feature() RETURNS integer
    LANGUAGE sql
    AS $$SELECT cv_id FROM cv WHERE name='sequence'$$;


ALTER FUNCTION chado.get_cv_id_for_feature() OWNER TO nathandunn;

--
-- Name: get_cv_id_for_feature_relationsgip(); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION get_cv_id_for_feature_relationsgip() RETURNS integer
    LANGUAGE sql
    AS $$SELECT cv_id FROM cv WHERE name='relationship'$$;


ALTER FUNCTION chado.get_cv_id_for_feature_relationsgip() OWNER TO nathandunn;

--
-- Name: get_cv_id_for_featureprop(); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION get_cv_id_for_featureprop() RETURNS integer
    LANGUAGE sql
    AS $$SELECT cv_id FROM cv WHERE name='feature_property'$$;


ALTER FUNCTION chado.get_cv_id_for_featureprop() OWNER TO nathandunn;

--
-- Name: get_cycle_cvterm_id(integer); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION get_cycle_cvterm_id(integer) RETURNS integer
    LANGUAGE plpgsql
    AS $_$
DECLARE
    cvid alias for $1;
    root cvterm%ROWTYPE;
    rtn     int;
BEGIN
    CREATE TEMP TABLE tmpcvtermpath(object_id int, subject_id int, cv_id int, type_id int, pathdistance int);
    CREATE INDEX tmp_cvtpath1 ON tmpcvtermpath(object_id, subject_id);
    FOR root IN SELECT DISTINCT t.* from cvterm t LEFT JOIN cvterm_relationship r ON (t.cvterm_id = r.subject_id) INNER JOIN cvterm_relationship r2 ON (t.cvterm_id = r2.object_id) WHERE t.cv_id = cvid AND r.subject_id is null LOOP
        SELECT INTO rtn _fill_cvtermpath4root2detect_cycle(root.cvterm_id, root.cv_id);
        IF (rtn > 0) THEN
            DROP TABLE tmpcvtermpath;
            RETURN rtn;
        END IF;
    END LOOP;
    DROP TABLE tmpcvtermpath;
    RETURN 0;
END;   
$_$;


ALTER FUNCTION chado.get_cycle_cvterm_id(integer) OWNER TO nathandunn;

--
-- Name: get_cycle_cvterm_id(character varying); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION get_cycle_cvterm_id(character varying) RETURNS integer
    LANGUAGE plpgsql
    AS $_$
DECLARE
    cvname alias for $1;
    cv_id int;
    rtn int;
BEGIN
    SELECT INTO cv_id cv.cv_id from cv WHERE cv.name = cvname;
    SELECT INTO rtn  get_cycle_cvterm_id(cv_id);
    RETURN rtn;
END;   
$_$;


ALTER FUNCTION chado.get_cycle_cvterm_id(character varying) OWNER TO nathandunn;

--
-- Name: get_cycle_cvterm_id(integer, integer); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION get_cycle_cvterm_id(integer, integer) RETURNS integer
    LANGUAGE plpgsql
    AS $_$
DECLARE
    cvid alias for $1;
    rootid alias for $2;
    rtn     int;
BEGIN
    CREATE TEMP TABLE tmpcvtermpath(object_id int, subject_id int, cv_id int, type_id int, pathdistance int);
    CREATE INDEX tmp_cvtpath1 ON tmpcvtermpath(object_id, subject_id);
    SELECT INTO rtn _fill_cvtermpath4root2detect_cycle(rootid, cvid);
    IF (rtn > 0) THEN
        DROP TABLE tmpcvtermpath;
        RETURN rtn;
    END IF;
    DROP TABLE tmpcvtermpath;
    RETURN 0;
END;   
$_$;


ALTER FUNCTION chado.get_cycle_cvterm_id(integer, integer) OWNER TO nathandunn;

--
-- Name: get_cycle_cvterm_ids(integer); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION get_cycle_cvterm_ids(integer) RETURNS SETOF integer
    LANGUAGE plpgsql
    AS $_$
DECLARE
    cvid alias for $1;
    root cvterm%ROWTYPE;
    rtn     int;
BEGIN
    FOR root IN SELECT DISTINCT t.* from cvterm t WHERE cv_id = cvid LOOP
        SELECT INTO rtn get_cycle_cvterm_id(cvid,root.cvterm_id);
        IF (rtn > 0) THEN
            RETURN NEXT rtn;
        END IF;
    END LOOP;
    RETURN;
END;   
$_$;


ALTER FUNCTION chado.get_cycle_cvterm_ids(integer) OWNER TO nathandunn;

--
-- Name: get_feature_id(character varying, character varying, character varying); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION get_feature_id(character varying, character varying, character varying) RETURNS integer
    LANGUAGE sql
    AS $_$
  SELECT feature_id 
  FROM feature
  WHERE uniquename=$1
    AND type_id=get_feature_type_id($2)
    AND organism_id=get_organism_id($3)
 $_$;


ALTER FUNCTION chado.get_feature_id(character varying, character varying, character varying) OWNER TO nathandunn;

--
-- Name: get_feature_ids(text); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION get_feature_ids(text) RETURNS SETOF feature_by_fx_type
    LANGUAGE plpgsql
    AS $_$
DECLARE
    sql alias for $1;
    myrc feature_by_fx_type%ROWTYPE;
    myrc2 feature_by_fx_type%ROWTYPE;
    myrc3 feature_by_fx_type%ROWTYPE;
BEGIN
    FOR myrc IN EXECUTE sql LOOP
        RETURN NEXT myrc;
        FOR myrc2 IN SELECT * FROM get_up_feature_ids(myrc.feature_id) LOOP
            RETURN NEXT myrc2;
        END LOOP;
        FOR myrc3 IN SELECT * FROM get_sub_feature_ids(myrc.feature_id) LOOP
            RETURN NEXT myrc3;
        END LOOP;
    END LOOP;
    RETURN;
END;
$_$;


ALTER FUNCTION chado.get_feature_ids(text) OWNER TO nathandunn;

--
-- Name: get_feature_ids_by_child_count(character varying, character varying, integer, character varying, character); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION get_feature_ids_by_child_count(character varying, character varying, integer, character varying, character) RETURNS SETOF feature_by_fx_type
    LANGUAGE plpgsql
    AS $_$
DECLARE
    ptype alias for $1;
    ctype alias for $2;
    ccount alias for $3;
    operator alias for $4;
    is_an alias for $5;
    query TEXT;
    myrc feature_by_fx_type%ROWTYPE;
    myrc2 feature_by_fx_type %ROWTYPE;
BEGIN
    query := 'SELECT DISTINCT f.feature_id
        FROM feature f INNER join (select count(*) as c, p.feature_id FROM feature p
        INNER join cvterm pt ON (p.type_id = pt.cvterm_id) INNER join feature_relationship fr
        ON (p.feature_id = fr.object_id) INNER join feature c ON (c.feature_id = fr.subject_id)
        INNER join cvterm ct ON (c.type_id = ct.cvterm_id)
        WHERE pt.name = ' || quote_literal(ptype) || ' AND ct.name = ' || quote_literal(ctype)
        || ' AND p.is_analysis = ' || quote_literal(is_an) || ' group by p.feature_id) as cq
        ON (cq.feature_id = f.feature_id) WHERE cq.c ' || operator || ccount || ';';
    ---RAISE NOTICE '%', query; 
    FOR myrc IN SELECT * FROM get_feature_ids(query) LOOP
        RETURN NEXT myrc;
    END LOOP;
    RETURN;
END;
$_$;


ALTER FUNCTION chado.get_feature_ids_by_child_count(character varying, character varying, integer, character varying, character) OWNER TO nathandunn;

--
-- Name: get_feature_ids_by_ont(character varying, character varying); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION get_feature_ids_by_ont(character varying, character varying) RETURNS SETOF feature_by_fx_type
    LANGUAGE plpgsql
    AS $_$
DECLARE
    aspect alias for $1;
    term alias for $2;
    query TEXT;
    myrc feature_by_fx_type%ROWTYPE;
    myrc2 feature_by_fx_type%ROWTYPE;
BEGIN
    query := 'SELECT DISTINCT fcvt.feature_id 
        FROM feature_cvterm fcvt, cv, cvterm t WHERE cv.cv_id = t.cv_id AND
        t.cvterm_id = fcvt.cvterm_id AND cv.name = ' || quote_literal(aspect) ||
        ' AND t.name = ' || quote_literal(term) || ';';
    IF (STRPOS(term, '%') > 0) THEN
        query := 'SELECT DISTINCT fcvt.feature_id 
            FROM feature_cvterm fcvt, cv, cvterm t WHERE cv.cv_id = t.cv_id AND
            t.cvterm_id = fcvt.cvterm_id AND cv.name = ' || quote_literal(aspect) ||
            ' AND t.name like ' || quote_literal(term) || ';';
    END IF;
    FOR myrc IN SELECT * FROM get_feature_ids(query) LOOP
        RETURN NEXT myrc;
    END LOOP;
    RETURN;
END;
$_$;


ALTER FUNCTION chado.get_feature_ids_by_ont(character varying, character varying) OWNER TO nathandunn;

--
-- Name: get_feature_ids_by_ont_root(character varying, character varying); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION get_feature_ids_by_ont_root(character varying, character varying) RETURNS SETOF feature_by_fx_type
    LANGUAGE plpgsql
    AS $_$
DECLARE
    aspect alias for $1;
    term alias for $2;
    query TEXT;
    subquery TEXT;
    myrc feature_by_fx_type%ROWTYPE;
    myrc2 feature_by_fx_type%ROWTYPE;
BEGIN
    subquery := 'SELECT t.cvterm_id FROM cv, cvterm t WHERE cv.cv_id = t.cv_id 
        AND cv.name = ' || quote_literal(aspect) || ' AND t.name = ' || quote_literal(term) || ';';
    IF (STRPOS(term, '%') > 0) THEN
        subquery := 'SELECT t.cvterm_id FROM cv, cvterm t WHERE cv.cv_id = t.cv_id 
            AND cv.name = ' || quote_literal(aspect) || ' AND t.name like ' || quote_literal(term) || ';';
    END IF;
    query := 'SELECT DISTINCT fcvt.feature_id 
        FROM feature_cvterm fcvt INNER JOIN (SELECT cvterm_id FROM get_it_sub_cvterm_ids(' || quote_literal(subquery) || ')) AS ont ON (fcvt.cvterm_id = ont.cvterm_id);';
    FOR myrc IN SELECT * FROM get_feature_ids(query) LOOP
        RETURN NEXT myrc;
    END LOOP;
    RETURN;
END;
$_$;


ALTER FUNCTION chado.get_feature_ids_by_ont_root(character varying, character varying) OWNER TO nathandunn;

--
-- Name: get_feature_ids_by_property(character varying, character varying); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION get_feature_ids_by_property(character varying, character varying) RETURNS SETOF feature_by_fx_type
    LANGUAGE plpgsql
    AS $_$
DECLARE
    p_type alias for $1;
    p_val alias for $2;
    query TEXT;
    myrc feature_by_fx_type%ROWTYPE;
    myrc2 feature_by_fx_type%ROWTYPE;
BEGIN
    query := 'SELECT DISTINCT fprop.feature_id 
        FROM featureprop fprop, cvterm t WHERE t.cvterm_id = fprop.type_id AND t.name = ' ||
        quote_literal(p_type) || ' AND fprop.value = ' || quote_literal(p_val) || ';';
    IF (STRPOS(p_val, '%') > 0) THEN
        query := 'SELECT DISTINCT fprop.feature_id 
            FROM featureprop fprop, cvterm t WHERE t.cvterm_id = fprop.type_id AND t.name = ' ||
            quote_literal(p_type) || ' AND fprop.value like ' || quote_literal(p_val) || ';';
    END IF;
    FOR myrc IN SELECT * FROM get_feature_ids(query) LOOP
        RETURN NEXT myrc;
    END LOOP;
    RETURN;
END;
$_$;


ALTER FUNCTION chado.get_feature_ids_by_property(character varying, character varying) OWNER TO nathandunn;

--
-- Name: get_feature_ids_by_propval(character varying); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION get_feature_ids_by_propval(character varying) RETURNS SETOF feature_by_fx_type
    LANGUAGE plpgsql
    AS $_$
DECLARE
    p_val alias for $1;
    query TEXT;
    myrc feature_by_fx_type%ROWTYPE;
    myrc2 feature_by_fx_type%ROWTYPE;
BEGIN
    query := 'SELECT DISTINCT fprop.feature_id 
        FROM featureprop fprop WHERE fprop.value = ' || quote_literal(p_val) || ';';
    IF (STRPOS(p_val, '%') > 0) THEN
        query := 'SELECT DISTINCT fprop.feature_id 
            FROM featureprop fprop WHERE fprop.value like ' || quote_literal(p_val) || ';';
    END IF;
    FOR myrc IN SELECT * FROM get_feature_ids(query) LOOP
        RETURN NEXT myrc;
    END LOOP;
    RETURN;
END;
$_$;


ALTER FUNCTION chado.get_feature_ids_by_propval(character varying) OWNER TO nathandunn;

--
-- Name: get_feature_ids_by_type(character varying, character); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION get_feature_ids_by_type(character varying, character) RETURNS SETOF feature_by_fx_type
    LANGUAGE plpgsql
    AS $_$
DECLARE
    gtype alias for $1;
    is_an alias for $2;
    query TEXT;
    myrc feature_by_fx_type%ROWTYPE;
    myrc2 feature_by_fx_type%ROWTYPE;
BEGIN
    query := 'SELECT DISTINCT f.feature_id 
        FROM feature f, cvterm t WHERE t.cvterm_id = f.type_id AND t.name = ' || quote_literal(gtype) ||
        ' AND f.is_analysis = ' || quote_literal(is_an) || ';';
    IF (STRPOS(gtype, '%') > 0) THEN
        query := 'SELECT DISTINCT f.feature_id 
            FROM feature f, cvterm t WHERE t.cvterm_id = f.type_id AND t.name like '
            || quote_literal(gtype) || ' AND f.is_analysis = ' || quote_literal(is_an) || ';';
    END IF;
    FOR myrc IN SELECT * FROM get_feature_ids(query) LOOP
        RETURN NEXT myrc;
    END LOOP;
    RETURN;
END;
$_$;


ALTER FUNCTION chado.get_feature_ids_by_type(character varying, character) OWNER TO nathandunn;

--
-- Name: get_feature_ids_by_type_name(character varying, text, character); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION get_feature_ids_by_type_name(character varying, text, character) RETURNS SETOF feature_by_fx_type
    LANGUAGE plpgsql
    AS $_$
DECLARE
    gtype alias for $1;
    name alias for $2;
    is_an alias for $3;
    query TEXT;
    myrc feature_by_fx_type%ROWTYPE;
    myrc2 feature_by_fx_type%ROWTYPE;
BEGIN
    query := 'SELECT DISTINCT f.feature_id 
        FROM feature f INNER join cvterm t ON (f.type_id = t.cvterm_id)
        WHERE t.name = ' || quote_literal(gtype) || ' AND (f.uniquename = ' || quote_literal(name)
        || ' OR f.name = ' || quote_literal(name) || ') AND f.is_analysis = ' || quote_literal(is_an) || ';';
    IF (STRPOS(name, '%') > 0) THEN
        query := 'SELECT DISTINCT f.feature_id 
            FROM feature f INNER join cvterm t ON (f.type_id = t.cvterm_id)
            WHERE t.name = ' || quote_literal(gtype) || ' AND (f.uniquename like ' || quote_literal(name)
            || ' OR f.name like ' || quote_literal(name) || ') AND f.is_analysis = ' || quote_literal(is_an) || ';';
    END IF;
    FOR myrc IN SELECT * FROM get_feature_ids(query) LOOP
        RETURN NEXT myrc;
    END LOOP;
    RETURN;
END;
$_$;


ALTER FUNCTION chado.get_feature_ids_by_type_name(character varying, text, character) OWNER TO nathandunn;

--
-- Name: get_feature_ids_by_type_src(character varying, text, character); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION get_feature_ids_by_type_src(character varying, text, character) RETURNS SETOF feature_by_fx_type
    LANGUAGE plpgsql
    AS $_$
DECLARE
    gtype alias for $1;
    src alias for $2;
    is_an alias for $3;
    query TEXT;
    myrc feature_by_fx_type%ROWTYPE;
    myrc2 feature_by_fx_type%ROWTYPE;
BEGIN
    query := 'SELECT DISTINCT f.feature_id 
        FROM feature f INNER join cvterm t ON (f.type_id = t.cvterm_id) INNER join featureloc fl
        ON (f.feature_id = fl.feature_id) INNER join feature src ON (src.feature_id = fl.srcfeature_id)
        WHERE t.name = ' || quote_literal(gtype) || ' AND src.uniquename = ' || quote_literal(src)
        || ' AND f.is_analysis = ' || quote_literal(is_an) || ';';
    IF (STRPOS(gtype, '%') > 0) THEN
        query := 'SELECT DISTINCT f.feature_id 
            FROM feature f INNER join cvterm t ON (f.type_id = t.cvterm_id) INNER join featureloc fl
            ON (f.feature_id = fl.feature_id) INNER join feature src ON (src.feature_id = fl.srcfeature_id)
            WHERE t.name like ' || quote_literal(gtype) || ' AND src.uniquename = ' || quote_literal(src)
            || ' AND f.is_analysis = ' || quote_literal(is_an) || ';';
    END IF;
    FOR myrc IN SELECT * FROM get_feature_ids(query) LOOP
        RETURN NEXT myrc;
    END LOOP;
    RETURN;
END;
$_$;


ALTER FUNCTION chado.get_feature_ids_by_type_src(character varying, text, character) OWNER TO nathandunn;

--
-- Name: get_feature_relationship_type_id(character varying); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION get_feature_relationship_type_id(character varying) RETURNS integer
    LANGUAGE sql
    AS $_$
  SELECT cvterm_id 
  FROM cv INNER JOIN cvterm USING (cv_id)
  WHERE cvterm.name=$1 AND cv.name='relationship'
 $_$;


ALTER FUNCTION chado.get_feature_relationship_type_id(character varying) OWNER TO nathandunn;

--
-- Name: get_feature_type_id(character varying); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION get_feature_type_id(character varying) RETURNS integer
    LANGUAGE sql
    AS $_$ 
  SELECT cvterm_id 
  FROM cv INNER JOIN cvterm USING (cv_id)
  WHERE cvterm.name=$1 AND cv.name='sequence'
 $_$;


ALTER FUNCTION chado.get_feature_type_id(character varying) OWNER TO nathandunn;

--
-- Name: get_featureprop_type_id(character varying); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION get_featureprop_type_id(character varying) RETURNS integer
    LANGUAGE sql
    AS $_$
  SELECT cvterm_id 
  FROM cv INNER JOIN cvterm USING (cv_id)
  WHERE cvterm.name=$1 AND cv.name='feature_property'
 $_$;


ALTER FUNCTION chado.get_featureprop_type_id(character varying) OWNER TO nathandunn;

--
-- Name: get_graph_above(integer); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION get_graph_above(integer) RETURNS SETOF cvtermpath
    LANGUAGE plpgsql
    AS $_$
DECLARE
    leaf alias for $1;
    cterm cvtermpath%ROWTYPE;
    cterm2 cvtermpath%ROWTYPE;
BEGIN
    FOR cterm IN SELECT * FROM cvterm_relationship WHERE subject_id = leaf LOOP
        RETURN NEXT cterm;
        FOR cterm2 IN SELECT * FROM get_all_object_ids(cterm.object_id) LOOP
            RETURN NEXT cterm2;
        END LOOP;
    END LOOP;
    RETURN;
END;   
$_$;


ALTER FUNCTION chado.get_graph_above(integer) OWNER TO nathandunn;

--
-- Name: get_graph_below(integer); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION get_graph_below(integer) RETURNS SETOF cvtermpath
    LANGUAGE plpgsql
    AS $_$
DECLARE
    root alias for $1;
    cterm cvtermpath%ROWTYPE;
    cterm2 cvtermpath%ROWTYPE;
BEGIN
    FOR cterm IN SELECT * FROM cvterm_relationship WHERE object_id = root LOOP
        RETURN NEXT cterm;
        FOR cterm2 IN SELECT * FROM get_all_subject_ids(cterm.subject_id) LOOP
            RETURN NEXT cterm2;
        END LOOP;
    END LOOP;
    RETURN;
END;   
$_$;


ALTER FUNCTION chado.get_graph_below(integer) OWNER TO nathandunn;

--
-- Name: cvterm; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE cvterm (
    cvterm_id integer NOT NULL,
    cv_id integer NOT NULL,
    name character varying(1024) NOT NULL,
    definition text,
    dbxref_id integer NOT NULL,
    is_obsolete integer DEFAULT 0 NOT NULL,
    is_relationshiptype integer DEFAULT 0 NOT NULL
);


ALTER TABLE cvterm OWNER TO nathandunn;

--
-- Name: TABLE cvterm; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE cvterm IS 'A term, class, universal or type within an
ontology or controlled vocabulary.  This table is also used for
relations and properties. cvterms constitute nodes in the graph
defined by the collection of cvterms and cvterm_relationships.';


--
-- Name: COLUMN cvterm.cv_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN cvterm.cv_id IS 'The cv or ontology or namespace to which
this cvterm belongs.';


--
-- Name: COLUMN cvterm.name; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN cvterm.name IS 'A concise human-readable name or
label for the cvterm. Uniquely identifies a cvterm within a cv.';


--
-- Name: COLUMN cvterm.definition; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN cvterm.definition IS 'A human-readable text
definition.';


--
-- Name: COLUMN cvterm.dbxref_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN cvterm.dbxref_id IS 'Primary identifier dbxref - The
unique global OBO identifier for this cvterm.  Note that a cvterm may
have multiple secondary dbxrefs - see also table: cvterm_dbxref.';


--
-- Name: COLUMN cvterm.is_obsolete; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN cvterm.is_obsolete IS 'Boolean 0=false,1=true; see
GO documentation for details of obsoletion. Note that two terms with
different primary dbxrefs may exist if one is obsolete.';


--
-- Name: COLUMN cvterm.is_relationshiptype; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN cvterm.is_relationshiptype IS 'Boolean
0=false,1=true relations or relationship types (also known as Typedefs
in OBO format, or as properties or slots) form a cv/ontology in
themselves. We use this flag to indicate whether this cvterm is an
actual term/class/universal or a relation. Relations may be drawn from
the OBO Relations ontology, but are not exclusively drawn from there.';


--
-- Name: get_it_sub_cvterm_ids(text); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION get_it_sub_cvterm_ids(text) RETURNS SETOF cvterm
    LANGUAGE plpgsql
    AS $_$
DECLARE
    query alias for $1;
    cterm cvterm%ROWTYPE;
    cterm2 cvterm%ROWTYPE;
BEGIN
    FOR cterm IN EXECUTE query LOOP
        RETURN NEXT cterm;
        FOR cterm2 IN SELECT subject_id as cvterm_id FROM get_all_subject_ids(cterm.cvterm_id) LOOP
            RETURN NEXT cterm2;
        END LOOP;
    END LOOP;
    RETURN;
END;   
$_$;


ALTER FUNCTION chado.get_it_sub_cvterm_ids(text) OWNER TO nathandunn;

--
-- Name: get_organism_id(character varying); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION get_organism_id(character varying) RETURNS integer
    LANGUAGE sql
    AS $_$ 
SELECT organism_id
  FROM organism
  WHERE genus=substring($1,1,position(' ' IN $1)-1)
    AND species=substring($1,position(' ' IN $1)+1)
 $_$;


ALTER FUNCTION chado.get_organism_id(character varying) OWNER TO nathandunn;

--
-- Name: get_organism_id(character varying, character varying); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION get_organism_id(character varying, character varying) RETURNS integer
    LANGUAGE sql
    AS $_$
  SELECT organism_id 
  FROM organism
  WHERE genus=$1
    AND species=$2
 $_$;


ALTER FUNCTION chado.get_organism_id(character varying, character varying) OWNER TO nathandunn;

--
-- Name: get_organism_id_abbrev(character varying); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION get_organism_id_abbrev(character varying) RETURNS integer
    LANGUAGE sql
    AS $_$
SELECT organism_id
  FROM organism
  WHERE substr(genus,1,1)=substring($1,1,1)
    AND species=substring($1,position(' ' IN $1)+1)
 $_$;


ALTER FUNCTION chado.get_organism_id_abbrev(character varying) OWNER TO nathandunn;

--
-- Name: get_sub_feature_ids(integer); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION get_sub_feature_ids(integer) RETURNS SETOF feature_by_fx_type
    LANGUAGE plpgsql
    AS $_$
DECLARE
    root alias for $1;
    myrc feature_by_fx_type%ROWTYPE;
    myrc2 feature_by_fx_type%ROWTYPE;
BEGIN
    FOR myrc IN SELECT DISTINCT subject_id AS feature_id FROM feature_relationship WHERE object_id = root LOOP
        RETURN NEXT myrc;
        FOR myrc2 IN SELECT * FROM get_sub_feature_ids(myrc.feature_id) LOOP
            RETURN NEXT myrc2;
        END LOOP;
    END LOOP;
    RETURN;
END;
$_$;


ALTER FUNCTION chado.get_sub_feature_ids(integer) OWNER TO nathandunn;

--
-- Name: get_sub_feature_ids(text); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION get_sub_feature_ids(text) RETURNS SETOF feature_by_fx_type
    LANGUAGE plpgsql
    AS $_$
DECLARE
    sql alias for $1;
    myrc feature_by_fx_type%ROWTYPE;
    myrc2 feature_by_fx_type%ROWTYPE;
BEGIN
    FOR myrc IN EXECUTE sql LOOP
        FOR myrc2 IN SELECT * FROM get_sub_feature_ids(myrc.feature_id) LOOP
            RETURN NEXT myrc2;
        END LOOP;
    END LOOP;
    RETURN;
END;
$_$;


ALTER FUNCTION chado.get_sub_feature_ids(text) OWNER TO nathandunn;

--
-- Name: get_sub_feature_ids(integer, integer); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION get_sub_feature_ids(integer, integer) RETURNS SETOF feature_by_fx_type
    LANGUAGE plpgsql
    AS $_$
DECLARE
    root alias for $1;
    depth alias for $2;
    myrc feature_by_fx_type%ROWTYPE;
    myrc2 feature_by_fx_type%ROWTYPE;
BEGIN
    FOR myrc IN SELECT DISTINCT subject_id AS feature_id, depth FROM feature_relationship WHERE object_id = root LOOP
        RETURN NEXT myrc;
        FOR myrc2 IN SELECT * FROM get_sub_feature_ids(myrc.feature_id,depth+1) LOOP
            RETURN NEXT myrc2;
        END LOOP;
    END LOOP;
    RETURN;
END;
$_$;


ALTER FUNCTION chado.get_sub_feature_ids(integer, integer) OWNER TO nathandunn;

--
-- Name: get_sub_feature_ids_by_type_src(character varying, text, character); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION get_sub_feature_ids_by_type_src(character varying, text, character) RETURNS SETOF feature_by_fx_type
    LANGUAGE plpgsql
    AS $_$
DECLARE
    gtype alias for $1;
    src alias for $2;
    is_an alias for $3;
    query text;
    myrc feature_by_fx_type%ROWTYPE;
    myrc2 feature_by_fx_type%ROWTYPE;
BEGIN
    query := 'SELECT DISTINCT f.feature_id FROM feature f INNER join cvterm t ON (f.type_id = t.cvterm_id)
        INNER join featureloc fl
        ON (f.feature_id = fl.feature_id) INNER join feature src ON (src.feature_id = fl.srcfeature_id)
        WHERE t.name = ' || quote_literal(gtype) || ' AND src.uniquename = ' || quote_literal(src)
        || ' AND f.is_analysis = ' || quote_literal(is_an) || ';';
    IF (STRPOS(gtype, '%') > 0) THEN
        query := 'SELECT DISTINCT f.feature_id FROM feature f INNER join cvterm t ON (f.type_id = t.cvterm_id)
             INNER join featureloc fl
            ON (f.feature_id = fl.feature_id) INNER join feature src ON (src.feature_id = fl.srcfeature_id)
            WHERE t.name like ' || quote_literal(gtype) || ' AND src.uniquename = ' || quote_literal(src)
            || ' AND f.is_analysis = ' || quote_literal(is_an) || ';';
    END IF;
    FOR myrc IN SELECT * FROM get_sub_feature_ids(query) LOOP
        RETURN NEXT myrc;
    END LOOP;
    RETURN;
END;
$_$;


ALTER FUNCTION chado.get_sub_feature_ids_by_type_src(character varying, text, character) OWNER TO nathandunn;

--
-- Name: get_up_feature_ids(integer); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION get_up_feature_ids(integer) RETURNS SETOF feature_by_fx_type
    LANGUAGE plpgsql
    AS $_$
DECLARE
    leaf alias for $1;
    myrc feature_by_fx_type%ROWTYPE;
    myrc2 feature_by_fx_type%ROWTYPE;
BEGIN
    FOR myrc IN SELECT DISTINCT object_id AS feature_id FROM feature_relationship WHERE subject_id = leaf LOOP
        RETURN NEXT myrc;
        FOR myrc2 IN SELECT * FROM get_up_feature_ids(myrc.feature_id) LOOP
            RETURN NEXT myrc2;
        END LOOP;
    END LOOP;
    RETURN;
END;
$_$;


ALTER FUNCTION chado.get_up_feature_ids(integer) OWNER TO nathandunn;

--
-- Name: get_up_feature_ids(text); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION get_up_feature_ids(text) RETURNS SETOF feature_by_fx_type
    LANGUAGE plpgsql
    AS $_$
DECLARE
    sql alias for $1;
    myrc feature_by_fx_type%ROWTYPE;
    myrc2 feature_by_fx_type%ROWTYPE;
BEGIN
    FOR myrc IN EXECUTE sql LOOP
        FOR myrc2 IN SELECT * FROM get_up_feature_ids(myrc.feature_id) LOOP
            RETURN NEXT myrc2;
        END LOOP;
    END LOOP;
    RETURN;
END;
$_$;


ALTER FUNCTION chado.get_up_feature_ids(text) OWNER TO nathandunn;

--
-- Name: get_up_feature_ids(integer, integer); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION get_up_feature_ids(integer, integer) RETURNS SETOF feature_by_fx_type
    LANGUAGE plpgsql
    AS $_$
DECLARE
    leaf alias for $1;
    depth alias for $2;
    myrc feature_by_fx_type%ROWTYPE;
    myrc2 feature_by_fx_type%ROWTYPE;
BEGIN
    FOR myrc IN SELECT DISTINCT object_id AS feature_id, depth FROM feature_relationship WHERE subject_id = leaf LOOP
        RETURN NEXT myrc;
        FOR myrc2 IN SELECT * FROM get_up_feature_ids(myrc.feature_id,depth+1) LOOP
            RETURN NEXT myrc2;
        END LOOP;
    END LOOP;
    RETURN;
END;
$_$;


ALTER FUNCTION chado.get_up_feature_ids(integer, integer) OWNER TO nathandunn;

--
-- Name: gff_load_bins(character varying, integer); Type: FUNCTION; Schema: chado; Owner: ubuntu
--

CREATE FUNCTION gff_load_bins(character varying, integer) RETURNS void
    LANGUAGE plpgsql
    AS $_$
  DECLARE
    current_type  ALIAS FOR $1;
    current_srcf  ALIAS FOR $2;

    i             int;
    cumcount      int;
    result        gff_interval_stats%ROWTYPE;
  BEGIN
    cumcount = 0;

    FOR result IN SELECT * FROM gff_interval_stats WHERE typeid is not null
                                                   AND typeid = current_type
                                                   AND srcfeature_id = current_srcf
                                                   order by bin LOOP

        cumcount = result.cum_count + cumcount;

        UPDATE gff_interval_stats SET cum_count = cumcount
            WHERE typeid = current_type
              AND srcfeature_id = current_srcf
              AND bin = result.bin;
    END LOOP;
  END;
$_$;


ALTER FUNCTION chado.gff_load_bins(character varying, integer) OWNER TO ubuntu;

--
-- Name: gff_load_bins(character varying, integer, integer); Type: FUNCTION; Schema: chado; Owner: ubuntu
--

CREATE FUNCTION gff_load_bins(character varying, integer, integer) RETURNS void
    LANGUAGE plpgsql
    AS $_$
  DECLARE
    current_type  ALIAS FOR $1;
    current_srcf  ALIAS FOR $2;
    current_bin   ALIAS FOR $3;

    i             int;
    cumcount      int;
    result        gff_interval_stats%ROWTYPE;
  BEGIN
    cumcount = 0;

    FOR result IN SELECT * FROM gff_interval_stats WHERE typeid is not null
                                                   AND typeid = current_type
                                                   AND srcfeature_id = current_srcf
                                                   AND bin <= current_bin
                                                   order by bin LOOP

        cumcount = result.cum_count + cumcount;
        UPDATE gff_interval_stats SET cum_count = cumcount
            WHERE typeid = current_type
              AND srcfeature_id = current_srcf
              AND bin = result.bin;
    END LOOP;
  END;
$_$;


ALTER FUNCTION chado.gff_load_bins(character varying, integer, integer) OWNER TO ubuntu;

--
-- Name: gffattstring(integer); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION gffattstring(integer) RETURNS character varying
    LANGUAGE plpgsql
    AS $_$DECLARE
  return_string      varchar;
  f_id               ALIAS FOR $1;
  atts_view          gffatts%ROWTYPE;
  feature_row        feature%ROWTYPE;
  name               varchar;
  uniquename         varchar;
  parent             varchar;
  escape_loc         int; 
BEGIN
  --Get name from feature.name
  --Get ID from feature.uniquename
  SELECT INTO feature_row * FROM feature WHERE feature_id = f_id;
  name  = feature_row.name;
  return_string = 'ID=' || feature_row.uniquename;
  IF name IS NOT NULL AND name != ''
  THEN
    return_string = return_string ||';' || 'Name=' || name;
  END IF;
  --Get Parent from feature_relationship
  SELECT INTO feature_row * FROM feature f, feature_relationship fr
    WHERE fr.subject_id = f_id AND fr.object_id = f.feature_id;
  IF FOUND
  THEN
    return_string = return_string||';'||'Parent='||feature_row.uniquename;
  END IF;
  FOR atts_view IN SELECT * FROM gff3atts WHERE feature_id = f_id  LOOP
    escape_loc = position(';' in atts_view.attribute);
    IF escape_loc > 0 THEN
      atts_view.attribute = replace(atts_view.attribute, ';', '%3B');
    END IF;
    return_string = return_string || ';'
                     || atts_view.type || '='
                     || atts_view.attribute;
  END LOOP;
  RETURN return_string;
END;
$_$;


ALTER FUNCTION chado.gffattstring(integer) OWNER TO nathandunn;

--
-- Name: db; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE db (
    db_id integer NOT NULL,
    name character varying(255) NOT NULL,
    description character varying(255),
    urlprefix character varying(255),
    url character varying(255)
);


ALTER TABLE db OWNER TO nathandunn;

--
-- Name: TABLE db; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE db IS 'A database authority. Typical databases in
bioinformatics are FlyBase, GO, UniProt, NCBI, MGI, etc. The authority
is generally known by this shortened form, which is unique within the
bioinformatics and biomedical realm.  To Do - add support for URIs,
URNs (e.g. LSIDs). We can do this by treating the URL as a URI -
however, some applications may expect this to be resolvable - to be
decided.';


--
-- Name: dbxref; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE dbxref (
    dbxref_id integer NOT NULL,
    db_id integer NOT NULL,
    accession character varying(255) NOT NULL,
    version character varying(255) DEFAULT ''::character varying NOT NULL,
    description text,
    searchable_accession tsvector
);


ALTER TABLE dbxref OWNER TO nathandunn;

--
-- Name: TABLE dbxref; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE dbxref IS 'A unique, global, chado. stable identifier. Not necessarily an external reference - can reference data items inside the particular chado instance being used. Typically a row in a table can be uniquely identified with a primary identifier (called dbxref_id); a table may also have secondary identifiers (in a linking table <T>_dbxref). A dbxref is generally written as <DB>:<ACCESSION> or as <DB>:<ACCESSION>:<VERSION>.';


--
-- Name: COLUMN dbxref.accession; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN dbxref.accession IS 'The local part of the identifier. Guaranteed by the db authority to be unique for that db.';


--
-- Name: feature_cvterm; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE feature_cvterm (
    feature_cvterm_id integer NOT NULL,
    feature_id integer NOT NULL,
    cvterm_id integer NOT NULL,
    pub_id integer NOT NULL,
    is_not boolean DEFAULT false NOT NULL,
    rank integer DEFAULT 0 NOT NULL
);


ALTER TABLE feature_cvterm OWNER TO nathandunn;

--
-- Name: TABLE feature_cvterm; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE feature_cvterm IS 'Associate a term from a cv with a feature, for example, GO annotation.';


--
-- Name: COLUMN feature_cvterm.pub_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN feature_cvterm.pub_id IS 'Provenance for the annotation. Each annotation should have a single primary chado.tion (which may be of the appropriate type for computational analyses) where more details can be found. Additional provenance dbxrefs can be attached using feature_cvterm_dbxref.';


--
-- Name: COLUMN feature_cvterm.is_not; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN feature_cvterm.is_not IS 'If this is set to true, then this annotation is interpreted as a NEGATIVE annotation - i.e. the feature does NOT have the specified function, process, component, part, etc. See GO docs for more details.';


--
-- Name: feature_dbxref; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE feature_dbxref (
    feature_dbxref_id integer NOT NULL,
    feature_id integer NOT NULL,
    dbxref_id integer NOT NULL,
    is_current boolean DEFAULT true NOT NULL
);


ALTER TABLE feature_dbxref OWNER TO nathandunn;

--
-- Name: TABLE feature_dbxref; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE feature_dbxref IS 'Links a feature to dbxrefs. This is for secondary identifiers; primary identifiers should use feature.dbxref_id.';


--
-- Name: COLUMN feature_dbxref.is_current; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN feature_dbxref.is_current IS 'True if this secondary dbxref is the most up to date accession in the corresponding db. Retired accessions should set this field to false';


--
-- Name: feature_pub; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE feature_pub (
    feature_pub_id integer NOT NULL,
    feature_id integer NOT NULL,
    pub_id integer NOT NULL
);


ALTER TABLE feature_pub OWNER TO nathandunn;

--
-- Name: TABLE feature_pub; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE feature_pub IS 'Provenance. Linking table between features and chado.tions that mention them.';


--
-- Name: feature_synonym; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE feature_synonym (
    feature_synonym_id integer NOT NULL,
    synonym_id integer NOT NULL,
    feature_id integer NOT NULL,
    pub_id integer NOT NULL,
    is_current boolean DEFAULT false NOT NULL,
    is_internal boolean DEFAULT false NOT NULL
);


ALTER TABLE feature_synonym OWNER TO nathandunn;

--
-- Name: TABLE feature_synonym; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE feature_synonym IS 'Linking table between feature and synonym.';


--
-- Name: COLUMN feature_synonym.pub_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN feature_synonym.pub_id IS 'The pub_id link is for relating the usage of a given synonym to the chado.tion in which it was used.';


--
-- Name: COLUMN feature_synonym.is_current; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN feature_synonym.is_current IS 'The is_current boolean indicates whether the linked synonym is the  current -official- symbol for the linked feature.';


--
-- Name: COLUMN feature_synonym.is_internal; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN feature_synonym.is_internal IS 'Typically a synonym exists so that somebody querying the db with an obsolete name can find the object theyre looking for (under its current name.  If the synonym has been used chado.y and deliberately (e.g. in a paper), it may also be listed in reports as a synonym. If the synonym was not used deliberately (e.g. there was a typo which went chado., then the is_internal boolean may be set to -true- so that it is known that the synonym is -internal- and should be queryable but should not be listed in reports as a valid synonym.';


--
-- Name: featureprop; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE featureprop (
    featureprop_id integer NOT NULL,
    feature_id integer NOT NULL,
    type_id integer NOT NULL,
    value text,
    rank integer DEFAULT 0 NOT NULL
);


ALTER TABLE featureprop OWNER TO nathandunn;

--
-- Name: TABLE featureprop; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE featureprop IS 'A feature can have any number of slot-value property tags attached to it. This is an alternative to hardcoding a list of columns in the relational schema, and is completely extensible.';


--
-- Name: COLUMN featureprop.type_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN featureprop.type_id IS 'The name of the
property/slot is a cvterm. The meaning of the property is defined in
that cvterm. Certain property types will only apply to certain feature
types (e.g. the anticodon property will only apply to tRNA features) ;
the types here come from the sequence feature property ontology.';


--
-- Name: COLUMN featureprop.value; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN featureprop.value IS 'The value of the property, represented as text. Numeric values are converted to their text representation. This is less efficient than using native database types, but is easier to query.';


--
-- Name: COLUMN featureprop.rank; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN featureprop.rank IS 'Property-Value ordering. Any
feature can have multiple values for any particular property type -
these are ordered in a list using rank, counting from zero. For
properties that are single-valued rather than multi-valued, the
default 0 value should be used';


--
-- Name: pub; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE pub (
    pub_id integer NOT NULL,
    title text,
    volumetitle text,
    volume character varying(255),
    series_name character varying(255),
    issue character varying(255),
    pyear character varying(255),
    pages character varying(255),
    miniref character varying(255),
    uniquename text NOT NULL,
    type_id integer NOT NULL,
    is_obsolete boolean DEFAULT false,
    publisher character varying(255),
    pubplace character varying(255)
);


ALTER TABLE pub OWNER TO nathandunn;

--
-- Name: TABLE pub; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE pub IS 'A documented provenance artefact - chado.tions,
documents, personal communication.';


--
-- Name: COLUMN pub.title; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN pub.title IS 'Descriptive general heading.';


--
-- Name: COLUMN pub.volumetitle; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN pub.volumetitle IS 'Title of part if one of a series.';


--
-- Name: COLUMN pub.series_name; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN pub.series_name IS 'Full name of (journal) series.';


--
-- Name: COLUMN pub.pages; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN pub.pages IS 'Page number range[s], e.g. 457--459, viii + 664pp, lv--lvii.';


--
-- Name: COLUMN pub.type_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN pub.type_id IS 'The type of the chado.tion (book, journal, poem, graffiti, etc). Uses pub cv.';


--
-- Name: synonym; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE synonym (
    synonym_id integer NOT NULL,
    name character varying(255) NOT NULL,
    type_id integer NOT NULL,
    synonym_sgml character varying(255) NOT NULL,
    searchable_synonym_sgml tsvector
);


ALTER TABLE synonym OWNER TO nathandunn;

--
-- Name: TABLE synonym; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE synonym IS 'A synonym for a feature. One feature can have multiple synonyms, and the same synonym can apply to multiple features.';


--
-- Name: COLUMN synonym.name; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN synonym.name IS 'The synonym itself. Should be human-readable machine-searchable ascii text.';


--
-- Name: COLUMN synonym.type_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN synonym.type_id IS 'Types would be symbol and fullname for now.';


--
-- Name: COLUMN synonym.synonym_sgml; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN synonym.synonym_sgml IS 'The fully specified synonym, with any non-ascii characters encoded in SGML.';


--
-- Name: gffatts; Type: VIEW; Schema: chado; Owner: nathandunn
--

CREATE VIEW gffatts AS
 SELECT fs.feature_id,
    'Ontology_term'::text AS type,
    s.name AS attribute
   FROM cvterm s,
    feature_cvterm fs
  WHERE (fs.cvterm_id = s.cvterm_id)
UNION ALL
 SELECT fs.feature_id,
    'Dbxref'::text AS type,
    (((d.name)::text || ':'::text) || (s.accession)::text) AS attribute
   FROM dbxref s,
    feature_dbxref fs,
    db d
  WHERE ((fs.dbxref_id = s.dbxref_id) AND (s.db_id = d.db_id))
UNION ALL
 SELECT fs.feature_id,
    'Alias'::text AS type,
    s.name AS attribute
   FROM synonym s,
    feature_synonym fs
  WHERE (fs.synonym_id = s.synonym_id)
UNION ALL
 SELECT fp.feature_id,
    cv.name AS type,
    fp.value AS attribute
   FROM featureprop fp,
    cvterm cv
  WHERE (fp.type_id = cv.cvterm_id)
UNION ALL
 SELECT fs.feature_id,
    'pub'::text AS type,
    (((s.series_name)::text || ':'::text) || s.title) AS attribute
   FROM pub s,
    feature_pub fs
  WHERE (fs.pub_id = s.pub_id);


ALTER TABLE gffatts OWNER TO nathandunn;

--
-- Name: gfffeatureatts(integer); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION gfffeatureatts(integer) RETURNS SETOF gffatts
    LANGUAGE sql
    AS $_$
SELECT feature_id, 'Ontology_term' AS type,  s.name AS attribute
FROM cvterm s, feature_cvterm fs
WHERE fs.feature_id= $1 AND fs.cvterm_id = s.cvterm_id
UNION
SELECT feature_id, 'Dbxref' AS type, d.name || ':' || s.accession AS attribute
FROM dbxref s, feature_dbxref fs, db d
WHERE fs.feature_id= $1 AND fs.dbxref_id = s.dbxref_id AND s.db_id = d.db_id
UNION
SELECT feature_id, 'Alias' AS type, s.name AS attribute
FROM synonym s, feature_synonym fs
WHERE fs.feature_id= $1 AND fs.synonym_id = s.synonym_id
UNION
SELECT fp.feature_id,cv.name,fp.value
FROM featureprop fp, cvterm cv
WHERE fp.feature_id= $1 AND fp.type_id = cv.cvterm_id 
UNION
SELECT feature_id, 'pub' AS type, s.series_name || ':' || s.title AS attribute
FROM pub s, feature_pub fs
WHERE fs.feature_id= $1 AND fs.pub_id = s.pub_id
$_$;


ALTER FUNCTION chado.gfffeatureatts(integer) OWNER TO nathandunn;

--
-- Name: order_exons(integer); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION order_exons(integer) RETURNS void
    LANGUAGE plpgsql
    AS $_$
  DECLARE
    parent_type      ALIAS FOR $1;
    exon_id          int;
    part_of          int;
    exon_type        int;
    strand           int;
    arow             RECORD;
    order_by         varchar;
    rowcount         int;
    exon_count       int;
    ordered_exons    int;    
    transcript_id    int;
    transcript_row   feature%ROWTYPE;
  BEGIN
    SELECT INTO part_of cvterm_id FROM cvterm WHERE name='part_of'
      AND cv_id IN (SELECT cv_id FROM cv WHERE name='relationship');
    --SELECT INTO exon_type cvterm_id FROM cvterm WHERE name='exon'
    --  AND cv_id IN (SELECT cv_id FROM cv WHERE name='sequence');
    --RAISE NOTICE 'part_of %, exon %',part_of,exon_type;
    FOR transcript_row IN
      SELECT * FROM feature WHERE type_id = parent_type
    LOOP
      transcript_id = transcript_row.feature_id;
      SELECT INTO rowcount count(*) FROM feature_relationship
        WHERE object_id = transcript_id
          AND rank = 0;
      --Dont modify this transcript if there are already numbered exons or
      --if there is only one exon
      IF rowcount = 1 THEN
        --RAISE NOTICE 'skipping transcript %, row count %',transcript_id,rowcount;
        CONTINUE;
      END IF;
      --need to reverse the order if the strand is negative
      SELECT INTO strand strand FROM featureloc WHERE feature_id=transcript_id;
      IF strand > 0 THEN
          order_by = 'fl.fmin';      
      ELSE
          order_by = 'fl.fmax desc';
      END IF;
      exon_count = 0;
      FOR arow IN EXECUTE 
        'SELECT fr.*, fl.fmin, fl.fmax
          FROM feature_relationship fr, featureloc fl
          WHERE fr.object_id  = '||transcript_id||'
            AND fr.subject_id = fl.feature_id
            AND fr.type_id    = '||part_of||'
            ORDER BY '||order_by
      LOOP
        --number the exons for a given transcript
        UPDATE feature_relationship
          SET rank = exon_count 
          WHERE feature_relationship_id = arow.feature_relationship_id;
        exon_count = exon_count + 1;
      END LOOP; 
    END LOOP;
  END;
$_$;


ALTER FUNCTION chado.order_exons(integer) OWNER TO nathandunn;

--
-- Name: phylonode_depth(integer); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION phylonode_depth(integer) RETURNS double precision
    LANGUAGE plpgsql
    AS $_$DECLARE  id    ALIAS FOR $1;
  DECLARE  depth FLOAT := 0;
  DECLARE  curr_node phylonode%ROWTYPE;
  BEGIN
   SELECT INTO curr_node *
    FROM phylonode 
    WHERE phylonode_id=id;
   depth = depth + curr_node.distance;
   IF curr_node.parent_phylonode_id IS NULL
    THEN RETURN depth;
    ELSE RETURN depth + phylonode_depth(curr_node.parent_phylonode_id);
   END IF;
 END
$_$;


ALTER FUNCTION chado.phylonode_depth(integer) OWNER TO nathandunn;

--
-- Name: phylonode_height(integer); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION phylonode_height(integer) RETURNS double precision
    LANGUAGE sql
    AS $_$
  SELECT coalesce(max(phylonode_height(phylonode_id) + distance), 0.0)
    FROM phylonode
    WHERE parent_phylonode_id = $1
$_$;


ALTER FUNCTION chado.phylonode_height(integer) OWNER TO nathandunn;

--
-- Name: populate_gff_interval_stats(); Type: FUNCTION; Schema: chado; Owner: ubuntu
--

CREATE FUNCTION populate_gff_interval_stats() RETURNS void
    LANGUAGE plpgsql
    AS $$
  DECLARE
    binsize       int;
    resrow        record;

    current_bin   int;
    last_bin      int;
    current_srcf  int;
    current_type  varchar;
    ibin          int;

    i             int;
    tempvalue     int;
  BEGIN
    binsize       = 1000;
    current_bin   = -1;
    current_srcf  = -1;
    current_type  = '';


    FOR resrow IN SELECT cvterm.name ||':'|| dbxref.accession as typeid,
                         fl.srcfeature_id, fl.fmin, fl.fmax
        FROM featureloc fl left join feature f using (feature_id)
         left join cvterm on (f.type_id = cvterm.cvterm_id)
         left join feature_dbxref fd on (f.feature_id = fd.feature_id)
         left join dbxref on (fd.dbxref_id = dbxref.dbxref_id and dbxref.db_id = 2)
        ORDER BY typeid, fl.srcfeature_id, fl.fmin LOOP

        ibin = resrow.fmin/binsize;

        IF (ibin != current_bin) THEN
            IF ((resrow.srcfeature_id != current_srcf
                 OR resrow.typeid != current_type) AND current_srcf > 0) THEN

                PERFORM gff_load_bins(current_type,current_srcf);

            ELSE
                --I don't think any thing needs to happen here

            END IF;
        END IF;

        current_bin  = ibin;
        current_type = resrow.typeid;
        current_srcf = resrow.srcfeature_id;

        last_bin = (resrow.fmax-1)/binsize;
        FOR i IN ibin..last_bin LOOP
            SELECT INTO tempvalue COALESCE (cum_count,0) FROM gff_interval_stats
                        WHERE bin = i
                          AND srcfeature_id = resrow.srcfeature_id
                          AND typeid = resrow.typeid;
            IF (tempvalue > 0) THEN
                UPDATE gff_interval_stats SET cum_count = tempvalue + 1
                    WHERE bin = i
                      AND srcfeature_id = resrow.srcfeature_id
                      AND typeid = resrow.typeid;
            ELSEIF (resrow.typeid IS NOT NULL) THEN
                INSERT INTO gff_interval_stats (typeid,srcfeature_id,bin,cum_count)
                    VALUES (resrow.typeid,resrow.srcfeature_id,i,1);
            END IF;
        END LOOP;

    END LOOP;

    PERFORM gff_load_bins(current_type,current_srcf);

  END;
$$;


ALTER FUNCTION chado.populate_gff_interval_stats() OWNER TO ubuntu;

--
-- Name: project_featureloc_up(integer, integer); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION project_featureloc_up(integer, integer) RETURNS featureloc
    LANGUAGE plpgsql
    AS $_$
DECLARE
    in_featureloc_id alias for $1;
    up_srcfeature_id alias for $2;
    in_featureloc featureloc%ROWTYPE;
    up_featureloc featureloc%ROWTYPE;
    nu_featureloc featureloc%ROWTYPE;
    nu_fmin INT;
    nu_fmax INT;
    nu_strand INT;
BEGIN
 SELECT INTO in_featureloc
   featureloc.*
  FROM featureloc
  WHERE featureloc_id = in_featureloc_id;
 SELECT INTO up_featureloc
   up_fl.*
  FROM featureloc AS in_fl
  INNER JOIN featureloc AS up_fl
    ON (in_fl.srcfeature_id = up_fl.feature_id)
  WHERE
   in_fl.featureloc_id = in_featureloc_id AND
   up_fl.srcfeature_id = up_srcfeature_id;
  IF up_featureloc.strand IS NULL
   THEN RETURN NULL;
  END IF;
  IF up_featureloc.strand < 0
  THEN
   nu_fmin = project_point_up(in_featureloc.fmax,
                              up_featureloc.fmin,up_featureloc.fmax,-1);
   nu_fmax = project_point_up(in_featureloc.fmin,
                              up_featureloc.fmin,up_featureloc.fmax,-1);
   nu_strand = -in_featureloc.strand;
  ELSE
   nu_fmin = project_point_up(in_featureloc.fmin,
                              up_featureloc.fmin,up_featureloc.fmax,1);
   nu_fmax = project_point_up(in_featureloc.fmax,
                              up_featureloc.fmin,up_featureloc.fmax,1);
   nu_strand = in_featureloc.strand;
  END IF;
  in_featureloc.fmin = nu_fmin;
  in_featureloc.fmax = nu_fmax;
  in_featureloc.strand = nu_strand;
  in_featureloc.srcfeature_id = up_featureloc.srcfeature_id;
  RETURN in_featureloc;
END
$_$;


ALTER FUNCTION chado.project_featureloc_up(integer, integer) OWNER TO nathandunn;

--
-- Name: project_point_down(integer, integer, integer, integer); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION project_point_down(integer, integer, integer, integer) RETURNS integer
    LANGUAGE sql
    AS $_$SELECT
  CASE WHEN $4<0
   THEN $3-$1
   ELSE $1+$2
  END AS p$_$;


ALTER FUNCTION chado.project_point_down(integer, integer, integer, integer) OWNER TO nathandunn;

--
-- Name: project_point_g2t(integer, integer, integer); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION project_point_g2t(integer, integer, integer) RETURNS integer
    LANGUAGE plpgsql
    AS $_$
 DECLARE
    in_p             alias for $1;
    srcf_id          alias for $2;
    t_id             alias for $3;
    e_floc           featureloc%ROWTYPE;
    out_p            INT;
    exon_cvterm_id   INT;
BEGIN
 SELECT INTO exon_cvterm_id get_feature_type_id('exon');
 SELECT INTO out_p
  CASE 
   WHEN strand<0 THEN fmax-p
   ELSE p-fmin
   END AS p
  FROM featureloc
   INNER JOIN feature USING (feature_id)
   INNER JOIN feature_relationship ON (feature.feature_id=subject_id)
  WHERE
   object_id = t_id                     AND
   feature.type_id = exon_cvterm_id     AND
   featureloc.srcfeature_id = srcf_id   AND
   in_p >= fmin                         AND
   in_p <= fmax;
  RETURN in_featureloc;
END
$_$;


ALTER FUNCTION chado.project_point_g2t(integer, integer, integer) OWNER TO nathandunn;

--
-- Name: project_point_up(integer, integer, integer, integer); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION project_point_up(integer, integer, integer, integer) RETURNS integer
    LANGUAGE sql
    AS $_$SELECT
  CASE WHEN $4<0
   THEN $3-$1             -- rev strand
   ELSE $1-$2             -- fwd strand
  END AS p$_$;


ALTER FUNCTION chado.project_point_up(integer, integer, integer, integer) OWNER TO nathandunn;

--
-- Name: reverse_complement(text); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION reverse_complement(text) RETURNS text
    LANGUAGE sql
    AS $_$SELECT reverse_string(complement_residues($1))$_$;


ALTER FUNCTION chado.reverse_complement(text) OWNER TO nathandunn;

--
-- Name: reverse_string(text); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION reverse_string(text) RETURNS text
    LANGUAGE plpgsql
    AS $_$
 DECLARE 
  reversed_string TEXT;
  incoming ALIAS FOR $1;
 BEGIN
   reversed_string = '';
   FOR i IN REVERSE char_length(incoming)..1 loop
     reversed_string = reversed_string || substring(incoming FROM i FOR 1);
   END loop;
 RETURN reversed_string;
END$_$;


ALTER FUNCTION chado.reverse_string(text) OWNER TO nathandunn;

--
-- Name: share_exons(); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION share_exons() RETURNS void
    LANGUAGE plpgsql
    AS $$    
  DECLARE    
  BEGIN
    CREATE temporary TABLE shared_exons AS
      SELECT gene.feature_id as gene_feature_id
           , gene.uniquename as gene_uniquename
           , transcript1.uniquename as transcript1
           , exon1.feature_id as exon1_feature_id
           , exon1.uniquename as exon1_uniquename
           , transcript2.uniquename as transcript2
           , exon2.feature_id as exon2_feature_id
           , exon2.uniquename as exon2_uniquename
           , exon1_loc.fmin 
           , exon1_loc.fmax 
      FROM feature gene
        JOIN cvterm gene_type ON gene.type_id = gene_type.cvterm_id
        JOIN cv gene_type_cv USING (cv_id)
        JOIN feature_relationship gene_transcript1 ON gene.feature_id = gene_transcript1.object_id
        JOIN feature transcript1 ON gene_transcript1.subject_id = transcript1.feature_id
        JOIN cvterm transcript1_type ON transcript1.type_id = transcript1_type.cvterm_id
        JOIN cv transcript1_type_cv ON transcript1_type.cv_id = transcript1_type_cv.cv_id
        JOIN feature_relationship transcript1_exon1 ON transcript1_exon1.object_id = transcript1.feature_id
        JOIN feature exon1 ON transcript1_exon1.subject_id = exon1.feature_id
        JOIN cvterm exon1_type ON exon1.type_id = exon1_type.cvterm_id
        JOIN cv exon1_type_cv ON exon1_type.cv_id = exon1_type_cv.cv_id
        JOIN featureloc exon1_loc ON exon1_loc.feature_id = exon1.feature_id
        JOIN feature_relationship gene_transcript2 ON gene.feature_id = gene_transcript2.object_id
        JOIN feature transcript2 ON gene_transcript2.subject_id = transcript2.feature_id
        JOIN cvterm transcript2_type ON transcript2.type_id = transcript2_type.cvterm_id
        JOIN cv transcript2_type_cv ON transcript2_type.cv_id = transcript2_type_cv.cv_id
        JOIN feature_relationship transcript2_exon2 ON transcript2_exon2.object_id = transcript2.feature_id
        JOIN feature exon2 ON transcript2_exon2.subject_id = exon2.feature_id
        JOIN cvterm exon2_type ON exon2.type_id = exon2_type.cvterm_id
        JOIN cv exon2_type_cv ON exon2_type.cv_id = exon2_type_cv.cv_id
        JOIN featureloc exon2_loc ON exon2_loc.feature_id = exon2.feature_id
      WHERE gene_type_cv.name = 'sequence'
        AND gene_type.name = 'gene'
        AND transcript1_type_cv.name = 'sequence'
        AND transcript1_type.name = 'mRNA'
        AND transcript2_type_cv.name = 'sequence'
        AND transcript2_type.name = 'mRNA'
        AND exon1_type_cv.name = 'sequence'
        AND exon1_type.name = 'exon'
        AND exon2_type_cv.name = 'sequence'
        AND exon2_type.name = 'exon'
        AND exon1.feature_id < exon2.feature_id
        AND exon1_loc.rank = 0
        AND exon2_loc.rank = 0
        AND exon1_loc.fmin = exon2_loc.fmin
        AND exon1_loc.fmax = exon2_loc.fmax
    ;
    /* Choose one of the shared exons to be the canonical representative.
       We pick the one with the smallest feature_id.
     */
    CREATE temporary TABLE canonical_exon_representatives AS
      SELECT gene_feature_id, min(exon1_feature_id) AS canonical_feature_id, fmin
      FROM shared_exons
      GROUP BY gene_feature_id,fmin
    ;
    CREATE temporary TABLE exon_replacements AS
      SELECT DISTINCT shared_exons.exon2_feature_id AS actual_feature_id
                    , canonical_exon_representatives.canonical_feature_id
                    , canonical_exon_representatives.fmin
      FROM shared_exons
        JOIN canonical_exon_representatives USING (gene_feature_id)
      WHERE shared_exons.exon2_feature_id <> canonical_exon_representatives.canonical_feature_id
        AND shared_exons.fmin = canonical_exon_representatives.fmin
    ;
    UPDATE feature_relationship 
      SET subject_id = (
            SELECT canonical_feature_id
            FROM exon_replacements
            WHERE feature_relationship.subject_id = exon_replacements.actual_feature_id)
      WHERE subject_id IN (
        SELECT actual_feature_id FROM exon_replacements
    );
    UPDATE feature_relationship
      SET object_id = (
            SELECT canonical_feature_id
            FROM exon_replacements
            WHERE feature_relationship.subject_id = exon_replacements.actual_feature_id)
      WHERE object_id IN (
        SELECT actual_feature_id FROM exon_replacements
    );
    UPDATE feature
      SET is_obsolete = true
      WHERE feature_id IN (
        SELECT actual_feature_id FROM exon_replacements
    );
  END;    
$$;


ALTER FUNCTION chado.share_exons() OWNER TO nathandunn;

--
-- Name: store_analysis(character varying, character varying, character varying); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION store_analysis(character varying, character varying, character varying) RETURNS integer
    LANGUAGE plpgsql
    AS $_$DECLARE
   v_program            ALIAS FOR $1;
   v_programversion     ALIAS FOR $2;
   v_sourcename         ALIAS FOR $3;
   pkval                INTEGER;
 BEGIN
    SELECT INTO pkval analysis_id
      FROM analysis
      WHERE program=v_program AND
            programversion=v_programversion AND
            sourcename=v_sourcename;
    IF NOT FOUND THEN
      INSERT INTO analysis 
       (program,programversion,sourcename)
         VALUES
       (v_program,v_programversion,v_sourcename);
      RETURN currval('analysis_analysis_id_seq');
    END IF;
    RETURN pkval;
 END;
$_$;


ALTER FUNCTION chado.store_analysis(character varying, character varying, character varying) OWNER TO nathandunn;

--
-- Name: store_db(character varying); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION store_db(character varying) RETURNS integer
    LANGUAGE plpgsql
    AS $_$DECLARE
   v_name             ALIAS FOR $1;
   v_db_id            INTEGER;
 BEGIN
    SELECT INTO v_db_id db_id
      FROM db
      WHERE name=v_name;
    IF NOT FOUND THEN
      INSERT INTO db
       (name)
         VALUES
       (v_name);
       RETURN currval('db_db_id_seq');
    END IF;
    RETURN v_db_id;
 END;
$_$;


ALTER FUNCTION chado.store_db(character varying) OWNER TO nathandunn;

--
-- Name: store_dbxref(character varying, character varying); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION store_dbxref(character varying, character varying) RETURNS integer
    LANGUAGE plpgsql
    AS $_$DECLARE
   v_dbname                ALIAS FOR $1;
   v_accession             ALIAS FOR $1;
   v_db_id                 INTEGER;
   v_dbxref_id             INTEGER;
 BEGIN
    SELECT INTO v_db_id
      store_db(v_dbname);
    SELECT INTO v_dbxref_id dbxref_id
      FROM dbxref
      WHERE db_id=v_db_id       AND
            accession=v_accession;
    IF NOT FOUND THEN
      INSERT INTO dbxref
       (db_id,accession)
         VALUES
       (v_db_id,v_accession);
       RETURN currval('dbxref_dbxref_id_seq');
    END IF;
    RETURN v_dbxref_id;
 END;
$_$;


ALTER FUNCTION chado.store_dbxref(character varying, character varying) OWNER TO nathandunn;

--
-- Name: store_feature(integer, integer, integer, integer, integer, integer, character varying, character varying, integer, boolean); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION store_feature(integer, integer, integer, integer, integer, integer, character varying, character varying, integer, boolean) RETURNS integer
    LANGUAGE plpgsql
    AS $_$DECLARE
  v_srcfeature_id       ALIAS FOR $1;
  v_fmin                ALIAS FOR $2;
  v_fmax                ALIAS FOR $3;
  v_strand              ALIAS FOR $4;
  v_dbxref_id           ALIAS FOR $5;
  v_organism_id         ALIAS FOR $6;
  v_name                ALIAS FOR $7;
  v_uniquename          ALIAS FOR $8;
  v_type_id             ALIAS FOR $9;
  v_is_analysis         ALIAS FOR $10;
  v_feature_id          INT;
  v_featureloc_id       INT;
 BEGIN
    IF v_dbxref_id IS NULL THEN
      SELECT INTO v_feature_id feature_id
      FROM feature
      WHERE uniquename=v_uniquename     AND
            organism_id=v_organism_id   AND
            type_id=v_type_id;
    ELSE
      SELECT INTO v_feature_id feature_id
      FROM feature
      WHERE dbxref_id=v_dbxref_id;
    END IF;
    IF NOT FOUND THEN
      INSERT INTO feature
       ( dbxref_id           ,
         organism_id         ,
         name                ,
         uniquename          ,
         type_id             ,
         is_analysis         )
        VALUES
        ( v_dbxref_id           ,
          v_organism_id         ,
          v_name                ,
          v_uniquename          ,
          v_type_id             ,
          v_is_analysis         );
      v_feature_id = currval('feature_feature_id_seq');
    ELSE
      UPDATE feature SET
        dbxref_id   =  v_dbxref_id           ,
        organism_id =  v_organism_id         ,
        name        =  v_name                ,
        uniquename  =  v_uniquename          ,
        type_id     =  v_type_id             ,
        is_analysis =  v_is_analysis
      WHERE
        feature_id=v_feature_id;
    END IF;
  PERFORM store_featureloc(v_feature_id,
                           v_srcfeature_id,
                           v_fmin,
                           v_fmax,
                           v_strand,
                           0,
                           0);
  RETURN v_feature_id;
 END;
$_$;


ALTER FUNCTION chado.store_feature(integer, integer, integer, integer, integer, integer, character varying, character varying, integer, boolean) OWNER TO nathandunn;

--
-- Name: store_feature_synonym(integer, character varying, integer, boolean, boolean, integer); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION store_feature_synonym(integer, character varying, integer, boolean, boolean, integer) RETURNS integer
    LANGUAGE plpgsql
    AS $_$DECLARE
  v_feature_id          ALIAS FOR $1;
  v_syn                 ALIAS FOR $2;
  v_type_id             ALIAS FOR $3;
  v_is_current          ALIAS FOR $4;
  v_is_internal         ALIAS FOR $5;
  v_pub_id              ALIAS FOR $6;
  v_synonym_id          INT;
  v_feature_synonym_id  INT;
 BEGIN
    IF v_feature_id IS NULL THEN RAISE EXCEPTION 'feature_id cannot be null';
    END IF;
    SELECT INTO v_synonym_id synonym_id
      FROM synonym
      WHERE name=v_syn                  AND
            type_id=v_type_id;
    IF NOT FOUND THEN
      INSERT INTO synonym
        ( name,
          synonym_sgml,
          type_id)
        VALUES
        ( v_syn,
          v_syn,
          v_type_id);
      v_synonym_id = currval('synonym_synonym_id_seq');
    END IF;
    SELECT INTO v_feature_synonym_id feature_synonym_id
        FROM feature_synonym
        WHERE feature_id=v_feature_id   AND
              synonym_id=v_synonym_id   AND
              pub_id=v_pub_id;
    IF NOT FOUND THEN
      INSERT INTO feature_synonym
        ( feature_id,
          synonym_id,
          pub_id,
          is_current,
          is_internal)
        VALUES
        ( v_feature_id,
          v_synonym_id,
          v_pub_id,
          v_is_current,
          v_is_internal);
      v_feature_synonym_id = currval('feature_synonym_feature_synonym_id_seq');
    ELSE
      UPDATE feature_synonym
        SET is_current=v_is_current, is_internal=v_is_internal
        WHERE feature_synonym_id=v_feature_synonym_id;
    END IF;
  RETURN v_feature_synonym_id;
 END;
$_$;


ALTER FUNCTION chado.store_feature_synonym(integer, character varying, integer, boolean, boolean, integer) OWNER TO nathandunn;

--
-- Name: store_featureloc(integer, integer, integer, integer, integer, integer, integer); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION store_featureloc(integer, integer, integer, integer, integer, integer, integer) RETURNS integer
    LANGUAGE plpgsql
    AS $_$DECLARE
  v_feature_id          ALIAS FOR $1;
  v_srcfeature_id       ALIAS FOR $2;
  v_fmin                ALIAS FOR $3;
  v_fmax                ALIAS FOR $4;
  v_strand              ALIAS FOR $5;
  v_rank                ALIAS FOR $6;
  v_locgroup            ALIAS FOR $7;
  v_featureloc_id       INT;
 BEGIN
    IF v_feature_id IS NULL THEN RAISE EXCEPTION 'feature_id cannot be null';
    END IF;
    SELECT INTO v_featureloc_id featureloc_id
      FROM featureloc
      WHERE feature_id=v_feature_id     AND
            rank=v_rank                 AND
            locgroup=v_locgroup;
    IF NOT FOUND THEN
      INSERT INTO featureloc
        ( feature_id,
          srcfeature_id,
          fmin,
          fmax,
          strand,
          rank,
          locgroup)
        VALUES
        (  v_feature_id,
           v_srcfeature_id,
           v_fmin,
           v_fmax,
           v_strand,
           v_rank,
           v_locgroup);
      v_featureloc_id = currval('featureloc_featureloc_id_seq');
    ELSE
      UPDATE featureloc SET
        feature_id    =  v_feature_id,
        srcfeature_id =  v_srcfeature_id,
        fmin          =  v_fmin,
        fmax          =  v_fmax,
        strand        =  v_strand,
        rank          =  v_rank,
        locgroup      =  v_locgroup
      WHERE
        featureloc_id=v_featureloc_id;
    END IF;
  RETURN v_featureloc_id;
 END;
$_$;


ALTER FUNCTION chado.store_featureloc(integer, integer, integer, integer, integer, integer, integer) OWNER TO nathandunn;

--
-- Name: store_organism(character varying, character varying, character varying); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION store_organism(character varying, character varying, character varying) RETURNS integer
    LANGUAGE plpgsql
    AS $_$DECLARE
   v_genus            ALIAS FOR $1;
   v_species          ALIAS FOR $2;
   v_common_name      ALIAS FOR $3;
   v_organism_id      INTEGER;
 BEGIN
    SELECT INTO v_organism_id organism_id
      FROM organism
      WHERE genus=v_genus               AND
            species=v_species;
    IF NOT FOUND THEN
      INSERT INTO organism
       (genus,species,common_name)
         VALUES
       (v_genus,v_species,v_common_name);
       RETURN currval('organism_organism_id_seq');
    ELSE
      UPDATE organism
       SET common_name=v_common_name
      WHERE organism_id = v_organism_id;
    END IF;
    RETURN v_organism_id;
 END;
$_$;


ALTER FUNCTION chado.store_organism(character varying, character varying, character varying) OWNER TO nathandunn;

--
-- Name: subsequence(integer, integer, integer, integer); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION subsequence(integer, integer, integer, integer) RETURNS text
    LANGUAGE sql
    AS $_$SELECT 
  CASE WHEN $4<0 
   THEN reverse_complement(substring(srcf.residues,$2+1,($3-$2)))
   ELSE substring(residues,$2+1,($3-$2))
  END AS residues
  FROM feature AS srcf
  WHERE
   srcf.feature_id=$1$_$;


ALTER FUNCTION chado.subsequence(integer, integer, integer, integer) OWNER TO nathandunn;

--
-- Name: subsequence_by_feature(integer); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION subsequence_by_feature(integer) RETURNS text
    LANGUAGE sql
    AS $_$SELECT subsequence_by_feature($1,0,0)$_$;


ALTER FUNCTION chado.subsequence_by_feature(integer) OWNER TO nathandunn;

--
-- Name: subsequence_by_feature(integer, integer, integer); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION subsequence_by_feature(integer, integer, integer) RETURNS text
    LANGUAGE sql
    AS $_$SELECT 
  CASE WHEN strand<0 
   THEN reverse_complement(substring(srcf.residues,fmin+1,(fmax-fmin)))
   ELSE substring(srcf.residues,fmin+1,(fmax-fmin))
  END AS residues
  FROM feature AS srcf
   INNER JOIN featureloc ON (srcf.feature_id=featureloc.srcfeature_id)
  WHERE
   featureloc.feature_id=$1 AND
   featureloc.rank=$2 AND
   featureloc.locgroup=$3$_$;


ALTER FUNCTION chado.subsequence_by_feature(integer, integer, integer) OWNER TO nathandunn;

--
-- Name: subsequence_by_featureloc(integer); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION subsequence_by_featureloc(integer) RETURNS text
    LANGUAGE sql
    AS $_$SELECT 
  CASE WHEN strand<0 
   THEN reverse_complement(substring(srcf.residues,fmin+1,(fmax-fmin)))
   ELSE substring(srcf.residues,fmin+1,(fmax-fmin))
  END AS residues
  FROM feature AS srcf
   INNER JOIN featureloc ON (srcf.feature_id=featureloc.srcfeature_id)
  WHERE
   featureloc_id=$1$_$;


ALTER FUNCTION chado.subsequence_by_featureloc(integer) OWNER TO nathandunn;

--
-- Name: subsequence_by_subfeatures(integer); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION subsequence_by_subfeatures(integer) RETURNS text
    LANGUAGE sql
    AS $_$
SELECT subsequence_by_subfeatures($1,get_feature_relationship_type_id('part_of'),0,0)
$_$;


ALTER FUNCTION chado.subsequence_by_subfeatures(integer) OWNER TO nathandunn;

--
-- Name: subsequence_by_subfeatures(integer, integer); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION subsequence_by_subfeatures(integer, integer) RETURNS text
    LANGUAGE sql
    AS $_$SELECT subsequence_by_subfeatures($1,$2,0,0)$_$;


ALTER FUNCTION chado.subsequence_by_subfeatures(integer, integer) OWNER TO nathandunn;

--
-- Name: subsequence_by_subfeatures(integer, integer, integer, integer); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION subsequence_by_subfeatures(integer, integer, integer, integer) RETURNS text
    LANGUAGE plpgsql
    AS $_$
DECLARE v_feature_id ALIAS FOR $1;
DECLARE v_rtype_id   ALIAS FOR $2;
DECLARE v_rank       ALIAS FOR $3;
DECLARE v_locgroup   ALIAS FOR $4;
DECLARE subseq       TEXT;
DECLARE seqrow       RECORD;
BEGIN 
  subseq = '';
 FOR seqrow IN
   SELECT
    CASE WHEN strand<0 
     THEN reverse_complement(substring(srcf.residues,fmin+1,(fmax-fmin)))
     ELSE substring(srcf.residues,fmin+1,(fmax-fmin))
    END AS residues
    FROM feature AS srcf
     INNER JOIN featureloc ON (srcf.feature_id=featureloc.srcfeature_id)
     INNER JOIN feature_relationship AS fr
       ON (fr.subject_id=featureloc.feature_id)
    WHERE
     fr.object_id=v_feature_id AND
     fr.type_id=v_rtype_id AND
     featureloc.rank=v_rank AND
     featureloc.locgroup=v_locgroup
    ORDER BY fr.rank
  LOOP
   subseq = subseq  || seqrow.residues;
  END LOOP;
 RETURN subseq;
END
$_$;


ALTER FUNCTION chado.subsequence_by_subfeatures(integer, integer, integer, integer) OWNER TO nathandunn;

--
-- Name: subsequence_by_typed_subfeatures(integer, integer); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION subsequence_by_typed_subfeatures(integer, integer) RETURNS text
    LANGUAGE sql
    AS $_$SELECT subsequence_by_typed_subfeatures($1,$2,0,0)$_$;


ALTER FUNCTION chado.subsequence_by_typed_subfeatures(integer, integer) OWNER TO nathandunn;

--
-- Name: subsequence_by_typed_subfeatures(integer, integer, integer, integer); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION subsequence_by_typed_subfeatures(integer, integer, integer, integer) RETURNS text
    LANGUAGE plpgsql
    AS $_$
DECLARE v_feature_id ALIAS FOR $1;
DECLARE v_ftype_id   ALIAS FOR $2;
DECLARE v_rank       ALIAS FOR $3;
DECLARE v_locgroup   ALIAS FOR $4;
DECLARE subseq       TEXT;
DECLARE seqrow       RECORD;
BEGIN 
  subseq = '';
 FOR seqrow IN
   SELECT
    CASE WHEN strand<0 
     THEN reverse_complement(substring(srcf.residues,fmin+1,(fmax-fmin)))
     ELSE substring(srcf.residues,fmin+1,(fmax-fmin))
    END AS residues
  FROM feature AS srcf
   INNER JOIN featureloc ON (srcf.feature_id=featureloc.srcfeature_id)
   INNER JOIN feature AS subf ON (subf.feature_id=featureloc.feature_id)
   INNER JOIN feature_relationship AS fr ON (fr.subject_id=subf.feature_id)
  WHERE
     fr.object_id=v_feature_id AND
     subf.type_id=v_ftype_id AND
     featureloc.rank=v_rank AND
     featureloc.locgroup=v_locgroup
  ORDER BY fr.rank
   LOOP
   subseq = subseq  || seqrow.residues;
  END LOOP;
 RETURN subseq;
END
$_$;


ALTER FUNCTION chado.subsequence_by_typed_subfeatures(integer, integer, integer, integer) OWNER TO nathandunn;

--
-- Name: translate_codon(text, integer); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION translate_codon(text, integer) RETURNS character
    LANGUAGE sql
    AS $_$SELECT aa FROM genetic_code.gencode_codon_aa WHERE codon=$1 AND gencode_id=$2$_$;


ALTER FUNCTION chado.translate_codon(text, integer) OWNER TO nathandunn;

--
-- Name: translate_dna(text); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION translate_dna(text) RETURNS text
    LANGUAGE sql
    AS $_$SELECT translate_dna($1,1)$_$;


ALTER FUNCTION chado.translate_dna(text) OWNER TO nathandunn;

--
-- Name: translate_dna(text, integer); Type: FUNCTION; Schema: chado; Owner: nathandunn
--

CREATE FUNCTION translate_dna(text, integer) RETURNS text
    LANGUAGE plpgsql
    AS $_$
 DECLARE 
  dnaseq ALIAS FOR $1;
  gcode ALIAS FOR $2;
  translation TEXT;
  dnaseqlen INT;
  codon CHAR(3);
  aa CHAR(1);
  i INT;
 BEGIN
   translation = '';
   dnaseqlen = char_length(dnaseq);
   i=1;
   WHILE i+1 < dnaseqlen loop
     codon = substring(dnaseq,i,3);
     aa = translate_codon(codon,gcode);
     translation = translation || aa;
     i = i+3;
   END loop;
 RETURN translation;
END$_$;


ALTER FUNCTION chado.translate_dna(text, integer) OWNER TO nathandunn;

--
-- Name: concat(text); Type: AGGREGATE; Schema: chado; Owner: nathandunn
--

CREATE AGGREGATE concat(text) (
    SFUNC = concat_pair,
    STYPE = text,
    INITCOND = ''
);


ALTER AGGREGATE chado.concat(text) OWNER TO nathandunn;

--
-- Name: acquisition; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE acquisition (
    acquisition_id integer NOT NULL,
    assay_id integer NOT NULL,
    protocol_id integer,
    channel_id integer,
    acquisitiondate timestamp without time zone DEFAULT now(),
    name text,
    uri text
);


ALTER TABLE acquisition OWNER TO nathandunn;

--
-- Name: TABLE acquisition; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE acquisition IS 'This represents the scanning of hybridized material. The output of this process is typically a digital image of an array.';


--
-- Name: acquisition_acquisition_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE acquisition_acquisition_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE acquisition_acquisition_id_seq OWNER TO nathandunn;

--
-- Name: acquisition_acquisition_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE acquisition_acquisition_id_seq OWNED BY acquisition.acquisition_id;


--
-- Name: acquisition_relationship; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE acquisition_relationship (
    acquisition_relationship_id integer NOT NULL,
    subject_id integer NOT NULL,
    type_id integer NOT NULL,
    object_id integer NOT NULL,
    value text,
    rank integer DEFAULT 0 NOT NULL
);


ALTER TABLE acquisition_relationship OWNER TO nathandunn;

--
-- Name: TABLE acquisition_relationship; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE acquisition_relationship IS 'Multiple monochrome images may be merged to form a multi-color image. Red-green images of 2-channel hybridizations are an example of this.';


--
-- Name: acquisition_relationship_acquisition_relationship_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE acquisition_relationship_acquisition_relationship_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE acquisition_relationship_acquisition_relationship_id_seq OWNER TO nathandunn;

--
-- Name: acquisition_relationship_acquisition_relationship_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE acquisition_relationship_acquisition_relationship_id_seq OWNED BY acquisition_relationship.acquisition_relationship_id;


--
-- Name: acquisitionprop; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE acquisitionprop (
    acquisitionprop_id integer NOT NULL,
    acquisition_id integer NOT NULL,
    type_id integer NOT NULL,
    value text,
    rank integer DEFAULT 0 NOT NULL
);


ALTER TABLE acquisitionprop OWNER TO nathandunn;

--
-- Name: TABLE acquisitionprop; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE acquisitionprop IS 'Parameters associated with image acquisition.';


--
-- Name: acquisitionprop_acquisitionprop_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE acquisitionprop_acquisitionprop_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE acquisitionprop_acquisitionprop_id_seq OWNER TO nathandunn;

--
-- Name: acquisitionprop_acquisitionprop_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE acquisitionprop_acquisitionprop_id_seq OWNED BY acquisitionprop.acquisitionprop_id;


--
-- Name: all_feature_names; Type: TABLE; Schema: chado; Owner: ubuntu; Tablespace: 
--

CREATE TABLE all_feature_names (
    feature_id integer,
    name character varying(255),
    organism_id integer,
    searchable_name tsvector
);


ALTER TABLE all_feature_names OWNER TO ubuntu;

--
-- Name: analysis; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE analysis (
    analysis_id integer NOT NULL,
    name character varying(255),
    description text,
    program character varying(255) NOT NULL,
    programversion character varying(255) NOT NULL,
    algorithm character varying(255),
    sourcename character varying(255),
    sourceversion character varying(255),
    sourceuri text,
    timeexecuted timestamp without time zone DEFAULT now() NOT NULL
);


ALTER TABLE analysis OWNER TO nathandunn;

--
-- Name: TABLE analysis; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE analysis IS 'An analysis is a particular type of a
    computational analysis; it may be a blast of one sequence against
    another, or an all by all blast, or a different kind of analysis
    altogether. It is a single unit of computation.';


--
-- Name: COLUMN analysis.name; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN analysis.name IS 'A way of grouping analyses. This
    should be a handy short identifier that can help people find an
    analysis they want. For instance "tRNAscan", "cDNA", "FlyPep",
    "SwissProt", and it should not be assumed to be unique. For instance, there may be lots of separate analyses done against a cDNA database.';


--
-- Name: COLUMN analysis.program; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN analysis.program IS 'Program name, e.g. blastx, blastp, sim4, genscan.';


--
-- Name: COLUMN analysis.programversion; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN analysis.programversion IS 'Version description, e.g. TBLASTX 2.0MP-WashU [09-Nov-2000].';


--
-- Name: COLUMN analysis.algorithm; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN analysis.algorithm IS 'Algorithm name, e.g. blast.';


--
-- Name: COLUMN analysis.sourcename; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN analysis.sourcename IS 'Source name, e.g. cDNA, SwissProt.';


--
-- Name: COLUMN analysis.sourceuri; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN analysis.sourceuri IS 'This is an optional, permanent URL or URI for the source of the  analysis. The idea is that someone could recreate the analysis directly by going to this URI and fetching the source data (e.g. the blast database, or the training model).';


--
-- Name: analysis_analysis_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE analysis_analysis_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE analysis_analysis_id_seq OWNER TO nathandunn;

--
-- Name: analysis_analysis_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE analysis_analysis_id_seq OWNED BY analysis.analysis_id;


--
-- Name: analysis_organism; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE analysis_organism (
    analysis_id integer NOT NULL,
    organism_id integer NOT NULL
);


ALTER TABLE analysis_organism OWNER TO nathandunn;

--
-- Name: analysisfeature; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE analysisfeature (
    analysisfeature_id integer NOT NULL,
    feature_id integer NOT NULL,
    analysis_id integer NOT NULL,
    rawscore double precision,
    normscore double precision,
    significance double precision,
    identity double precision
);


ALTER TABLE analysisfeature OWNER TO nathandunn;

--
-- Name: TABLE analysisfeature; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE analysisfeature IS 'Computational analyses generate features (e.g. Genscan generates transcripts and exons; sim4 alignments generate similarity/match features). analysisfeatures are stored using the feature table from the sequence module. The analysisfeature table is used to decorate these features, with analysis specific attributes. A feature is an analysisfeature if and only if there is a corresponding entry in the analysisfeature table. analysisfeatures will have two or more featureloc entries,
 with rank indicating query/subject';


--
-- Name: COLUMN analysisfeature.rawscore; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN analysisfeature.rawscore IS 'This is the native score generated by the program; for example, the bitscore generated by blast, sim4 or genscan scores. One should not assume that high is necessarily better than low.';


--
-- Name: COLUMN analysisfeature.normscore; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN analysisfeature.normscore IS 'This is the rawscore but
    semi-normalized. Complete normalization to allow comparison of
    features generated by different programs would be nice but too
    difficult. Instead the normalization should strive to enforce the
    following semantics: * normscores are floating point numbers >= 0,
    * high normscores are better than low one. For most programs, it would be sufficient to make the normscore the same as this rawscore, providing these semantics are satisfied.';


--
-- Name: COLUMN analysisfeature.significance; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN analysisfeature.significance IS 'This is some kind of expectation or probability metric, representing the probability that the analysis would appear randomly given the model. As such, any program or person querying this table can assume the following semantics:
   * 0 <= significance <= n, where n is a positive number, theoretically unbounded but unlikely to be more than 10
  * low numbers are better than high numbers.';


--
-- Name: COLUMN analysisfeature.identity; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN analysisfeature.identity IS 'Percent identity between the locations compared.  Note that these 4 metrics do not cover the full range of scores possible; it would be undesirable to list every score possible, as this should be kept extensible. instead, for non-standard scores, use the analysisprop table.';


--
-- Name: analysisfeature_analysisfeature_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE analysisfeature_analysisfeature_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE analysisfeature_analysisfeature_id_seq OWNER TO nathandunn;

--
-- Name: analysisfeature_analysisfeature_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE analysisfeature_analysisfeature_id_seq OWNED BY analysisfeature.analysisfeature_id;


--
-- Name: analysisfeatureprop; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE analysisfeatureprop (
    analysisfeatureprop_id integer NOT NULL,
    analysisfeature_id integer NOT NULL,
    type_id integer NOT NULL,
    value text,
    rank integer NOT NULL
);


ALTER TABLE analysisfeatureprop OWNER TO nathandunn;

--
-- Name: analysisfeatureprop_analysisfeatureprop_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE analysisfeatureprop_analysisfeatureprop_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE analysisfeatureprop_analysisfeatureprop_id_seq OWNER TO nathandunn;

--
-- Name: analysisfeatureprop_analysisfeatureprop_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE analysisfeatureprop_analysisfeatureprop_id_seq OWNED BY analysisfeatureprop.analysisfeatureprop_id;


--
-- Name: analysisprop; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE analysisprop (
    analysisprop_id integer NOT NULL,
    analysis_id integer NOT NULL,
    type_id integer NOT NULL,
    value text,
    rank integer DEFAULT 0 NOT NULL
);


ALTER TABLE analysisprop OWNER TO nathandunn;

--
-- Name: analysisprop_analysisprop_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE analysisprop_analysisprop_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE analysisprop_analysisprop_id_seq OWNER TO nathandunn;

--
-- Name: analysisprop_analysisprop_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE analysisprop_analysisprop_id_seq OWNED BY analysisprop.analysisprop_id;


--
-- Name: arraydesign; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE arraydesign (
    arraydesign_id integer NOT NULL,
    manufacturer_id integer NOT NULL,
    platformtype_id integer NOT NULL,
    substratetype_id integer,
    protocol_id integer,
    dbxref_id integer,
    name text NOT NULL,
    version text,
    description text,
    array_dimensions text,
    element_dimensions text,
    num_of_elements integer,
    num_array_columns integer,
    num_array_rows integer,
    num_grid_columns integer,
    num_grid_rows integer,
    num_sub_columns integer,
    num_sub_rows integer
);


ALTER TABLE arraydesign OWNER TO nathandunn;

--
-- Name: TABLE arraydesign; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE arraydesign IS 'General properties about an array.
An array is a template used to generate physical slides, etc.  It
contains layout information, as well as global array properties, such
as material (glass, nylon) and spot dimensions (in rows/columns).';


--
-- Name: arraydesign_arraydesign_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE arraydesign_arraydesign_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE arraydesign_arraydesign_id_seq OWNER TO nathandunn;

--
-- Name: arraydesign_arraydesign_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE arraydesign_arraydesign_id_seq OWNED BY arraydesign.arraydesign_id;


--
-- Name: arraydesignprop; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE arraydesignprop (
    arraydesignprop_id integer NOT NULL,
    arraydesign_id integer NOT NULL,
    type_id integer NOT NULL,
    value text,
    rank integer DEFAULT 0 NOT NULL
);


ALTER TABLE arraydesignprop OWNER TO nathandunn;

--
-- Name: TABLE arraydesignprop; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE arraydesignprop IS 'Extra array design properties that are not accounted for in arraydesign.';


--
-- Name: arraydesignprop_arraydesignprop_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE arraydesignprop_arraydesignprop_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE arraydesignprop_arraydesignprop_id_seq OWNER TO nathandunn;

--
-- Name: arraydesignprop_arraydesignprop_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE arraydesignprop_arraydesignprop_id_seq OWNED BY arraydesignprop.arraydesignprop_id;


--
-- Name: assay; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE assay (
    assay_id integer NOT NULL,
    arraydesign_id integer NOT NULL,
    protocol_id integer,
    assaydate timestamp without time zone DEFAULT now(),
    arrayidentifier text,
    arraybatchidentifier text,
    operator_id integer NOT NULL,
    dbxref_id integer,
    name text,
    description text
);


ALTER TABLE assay OWNER TO nathandunn;

--
-- Name: TABLE assay; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE assay IS 'An assay consists of a physical instance of
an array, combined with the conditions used to create the array
(protocols, technician information). The assay can be thought of as a hybridization.';


--
-- Name: assay_assay_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE assay_assay_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE assay_assay_id_seq OWNER TO nathandunn;

--
-- Name: assay_assay_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE assay_assay_id_seq OWNED BY assay.assay_id;


--
-- Name: assay_biomaterial; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE assay_biomaterial (
    assay_biomaterial_id integer NOT NULL,
    assay_id integer NOT NULL,
    biomaterial_id integer NOT NULL,
    channel_id integer,
    rank integer DEFAULT 0 NOT NULL
);


ALTER TABLE assay_biomaterial OWNER TO nathandunn;

--
-- Name: TABLE assay_biomaterial; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE assay_biomaterial IS 'A biomaterial can be hybridized many times (technical replicates), or combined with other biomaterials in a single hybridization (for two-channel arrays).';


--
-- Name: assay_biomaterial_assay_biomaterial_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE assay_biomaterial_assay_biomaterial_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE assay_biomaterial_assay_biomaterial_id_seq OWNER TO nathandunn;

--
-- Name: assay_biomaterial_assay_biomaterial_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE assay_biomaterial_assay_biomaterial_id_seq OWNED BY assay_biomaterial.assay_biomaterial_id;


--
-- Name: assay_project; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE assay_project (
    assay_project_id integer NOT NULL,
    assay_id integer NOT NULL,
    project_id integer NOT NULL
);


ALTER TABLE assay_project OWNER TO nathandunn;

--
-- Name: TABLE assay_project; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE assay_project IS 'Link assays to projects.';


--
-- Name: assay_project_assay_project_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE assay_project_assay_project_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE assay_project_assay_project_id_seq OWNER TO nathandunn;

--
-- Name: assay_project_assay_project_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE assay_project_assay_project_id_seq OWNED BY assay_project.assay_project_id;


--
-- Name: assayprop; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE assayprop (
    assayprop_id integer NOT NULL,
    assay_id integer NOT NULL,
    type_id integer NOT NULL,
    value text,
    rank integer DEFAULT 0 NOT NULL
);


ALTER TABLE assayprop OWNER TO nathandunn;

--
-- Name: TABLE assayprop; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE assayprop IS 'Extra assay properties that are not accounted for in assay.';


--
-- Name: assayprop_assayprop_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE assayprop_assayprop_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE assayprop_assayprop_id_seq OWNER TO nathandunn;

--
-- Name: assayprop_assayprop_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE assayprop_assayprop_id_seq OWNED BY assayprop.assayprop_id;


--
-- Name: biomaterial; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE biomaterial (
    biomaterial_id integer NOT NULL,
    taxon_id integer,
    biosourceprovider_id integer,
    dbxref_id integer,
    name text,
    description text
);


ALTER TABLE biomaterial OWNER TO nathandunn;

--
-- Name: TABLE biomaterial; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE biomaterial IS 'A biomaterial represents the MAGE concept of BioSource, BioSample, and LabeledExtract. It is essentially some biological material (tissue, cells, serum) that may have been processed. Processed biomaterials should be traceable back to raw biomaterials via the biomaterialrelationship table.';


--
-- Name: biomaterial_biomaterial_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE biomaterial_biomaterial_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE biomaterial_biomaterial_id_seq OWNER TO nathandunn;

--
-- Name: biomaterial_biomaterial_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE biomaterial_biomaterial_id_seq OWNED BY biomaterial.biomaterial_id;


--
-- Name: biomaterial_dbxref; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE biomaterial_dbxref (
    biomaterial_dbxref_id integer NOT NULL,
    biomaterial_id integer NOT NULL,
    dbxref_id integer NOT NULL
);


ALTER TABLE biomaterial_dbxref OWNER TO nathandunn;

--
-- Name: biomaterial_dbxref_biomaterial_dbxref_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE biomaterial_dbxref_biomaterial_dbxref_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE biomaterial_dbxref_biomaterial_dbxref_id_seq OWNER TO nathandunn;

--
-- Name: biomaterial_dbxref_biomaterial_dbxref_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE biomaterial_dbxref_biomaterial_dbxref_id_seq OWNED BY biomaterial_dbxref.biomaterial_dbxref_id;


--
-- Name: biomaterial_relationship; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE biomaterial_relationship (
    biomaterial_relationship_id integer NOT NULL,
    subject_id integer NOT NULL,
    type_id integer NOT NULL,
    object_id integer NOT NULL
);


ALTER TABLE biomaterial_relationship OWNER TO nathandunn;

--
-- Name: TABLE biomaterial_relationship; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE biomaterial_relationship IS 'Relate biomaterials to one another. This is a way to track a series of treatments or material splits/merges, for instance.';


--
-- Name: biomaterial_relationship_biomaterial_relationship_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE biomaterial_relationship_biomaterial_relationship_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE biomaterial_relationship_biomaterial_relationship_id_seq OWNER TO nathandunn;

--
-- Name: biomaterial_relationship_biomaterial_relationship_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE biomaterial_relationship_biomaterial_relationship_id_seq OWNED BY biomaterial_relationship.biomaterial_relationship_id;


--
-- Name: biomaterial_treatment; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE biomaterial_treatment (
    biomaterial_treatment_id integer NOT NULL,
    biomaterial_id integer NOT NULL,
    treatment_id integer NOT NULL,
    unittype_id integer,
    value real,
    rank integer DEFAULT 0 NOT NULL
);


ALTER TABLE biomaterial_treatment OWNER TO nathandunn;

--
-- Name: TABLE biomaterial_treatment; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE biomaterial_treatment IS 'Link biomaterials to treatments. Treatments have an order of operations (rank), and associated measurements (unittype_id, value).';


--
-- Name: biomaterial_treatment_biomaterial_treatment_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE biomaterial_treatment_biomaterial_treatment_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE biomaterial_treatment_biomaterial_treatment_id_seq OWNER TO nathandunn;

--
-- Name: biomaterial_treatment_biomaterial_treatment_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE biomaterial_treatment_biomaterial_treatment_id_seq OWNED BY biomaterial_treatment.biomaterial_treatment_id;


--
-- Name: biomaterialprop; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE biomaterialprop (
    biomaterialprop_id integer NOT NULL,
    biomaterial_id integer NOT NULL,
    type_id integer NOT NULL,
    value text,
    rank integer DEFAULT 0 NOT NULL
);


ALTER TABLE biomaterialprop OWNER TO nathandunn;

--
-- Name: TABLE biomaterialprop; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE biomaterialprop IS 'Extra biomaterial properties that are not accounted for in biomaterial.';


--
-- Name: biomaterialprop_biomaterialprop_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE biomaterialprop_biomaterialprop_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE biomaterialprop_biomaterialprop_id_seq OWNER TO nathandunn;

--
-- Name: biomaterialprop_biomaterialprop_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE biomaterialprop_biomaterialprop_id_seq OWNED BY biomaterialprop.biomaterialprop_id;


--
-- Name: blast_hit_data; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE blast_hit_data (
    analysisfeature_id integer NOT NULL,
    analysis_id integer NOT NULL,
    feature_id integer NOT NULL,
    db_id integer NOT NULL,
    hit_num integer NOT NULL,
    hit_name character varying(1025),
    hit_url text,
    hit_description text,
    hit_organism character varying(1025),
    blast_org_id integer,
    hit_accession character varying(255),
    hit_best_eval double precision,
    hit_best_score double precision,
    hit_pid double precision
);


ALTER TABLE blast_hit_data OWNER TO nathandunn;

--
-- Name: blast_organisms; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE blast_organisms (
    blast_org_id integer NOT NULL,
    blast_org_name character varying(1025)
);


ALTER TABLE blast_organisms OWNER TO nathandunn;

--
-- Name: blast_organisms_blast_org_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE blast_organisms_blast_org_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE blast_organisms_blast_org_id_seq OWNER TO nathandunn;

--
-- Name: blast_organisms_blast_org_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE blast_organisms_blast_org_id_seq OWNED BY blast_organisms.blast_org_id;


--
-- Name: cell_line; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE cell_line (
    cell_line_id integer NOT NULL,
    name character varying(255),
    uniquename character varying(255) NOT NULL,
    organism_id integer NOT NULL,
    timeaccessioned timestamp without time zone DEFAULT now() NOT NULL,
    timelastmodified timestamp without time zone DEFAULT now() NOT NULL
);


ALTER TABLE cell_line OWNER TO nathandunn;

--
-- Name: cell_line_cell_line_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE cell_line_cell_line_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE cell_line_cell_line_id_seq OWNER TO nathandunn;

--
-- Name: cell_line_cell_line_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE cell_line_cell_line_id_seq OWNED BY cell_line.cell_line_id;


--
-- Name: cell_line_cvterm; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE cell_line_cvterm (
    cell_line_cvterm_id integer NOT NULL,
    cell_line_id integer NOT NULL,
    cvterm_id integer NOT NULL,
    pub_id integer NOT NULL,
    rank integer DEFAULT 0 NOT NULL
);


ALTER TABLE cell_line_cvterm OWNER TO nathandunn;

--
-- Name: cell_line_cvterm_cell_line_cvterm_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE cell_line_cvterm_cell_line_cvterm_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE cell_line_cvterm_cell_line_cvterm_id_seq OWNER TO nathandunn;

--
-- Name: cell_line_cvterm_cell_line_cvterm_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE cell_line_cvterm_cell_line_cvterm_id_seq OWNED BY cell_line_cvterm.cell_line_cvterm_id;


--
-- Name: cell_line_cvtermprop; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE cell_line_cvtermprop (
    cell_line_cvtermprop_id integer NOT NULL,
    cell_line_cvterm_id integer NOT NULL,
    type_id integer NOT NULL,
    value text,
    rank integer DEFAULT 0 NOT NULL
);


ALTER TABLE cell_line_cvtermprop OWNER TO nathandunn;

--
-- Name: cell_line_cvtermprop_cell_line_cvtermprop_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE cell_line_cvtermprop_cell_line_cvtermprop_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE cell_line_cvtermprop_cell_line_cvtermprop_id_seq OWNER TO nathandunn;

--
-- Name: cell_line_cvtermprop_cell_line_cvtermprop_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE cell_line_cvtermprop_cell_line_cvtermprop_id_seq OWNED BY cell_line_cvtermprop.cell_line_cvtermprop_id;


--
-- Name: cell_line_dbxref; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE cell_line_dbxref (
    cell_line_dbxref_id integer NOT NULL,
    cell_line_id integer NOT NULL,
    dbxref_id integer NOT NULL,
    is_current boolean DEFAULT true NOT NULL
);


ALTER TABLE cell_line_dbxref OWNER TO nathandunn;

--
-- Name: cell_line_dbxref_cell_line_dbxref_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE cell_line_dbxref_cell_line_dbxref_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE cell_line_dbxref_cell_line_dbxref_id_seq OWNER TO nathandunn;

--
-- Name: cell_line_dbxref_cell_line_dbxref_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE cell_line_dbxref_cell_line_dbxref_id_seq OWNED BY cell_line_dbxref.cell_line_dbxref_id;


--
-- Name: cell_line_feature; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE cell_line_feature (
    cell_line_feature_id integer NOT NULL,
    cell_line_id integer NOT NULL,
    feature_id integer NOT NULL,
    pub_id integer NOT NULL
);


ALTER TABLE cell_line_feature OWNER TO nathandunn;

--
-- Name: cell_line_feature_cell_line_feature_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE cell_line_feature_cell_line_feature_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE cell_line_feature_cell_line_feature_id_seq OWNER TO nathandunn;

--
-- Name: cell_line_feature_cell_line_feature_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE cell_line_feature_cell_line_feature_id_seq OWNED BY cell_line_feature.cell_line_feature_id;


--
-- Name: cell_line_library; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE cell_line_library (
    cell_line_library_id integer NOT NULL,
    cell_line_id integer NOT NULL,
    library_id integer NOT NULL,
    pub_id integer NOT NULL
);


ALTER TABLE cell_line_library OWNER TO nathandunn;

--
-- Name: cell_line_library_cell_line_library_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE cell_line_library_cell_line_library_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE cell_line_library_cell_line_library_id_seq OWNER TO nathandunn;

--
-- Name: cell_line_library_cell_line_library_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE cell_line_library_cell_line_library_id_seq OWNED BY cell_line_library.cell_line_library_id;


--
-- Name: cell_line_pub; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE cell_line_pub (
    cell_line_pub_id integer NOT NULL,
    cell_line_id integer NOT NULL,
    pub_id integer NOT NULL
);


ALTER TABLE cell_line_pub OWNER TO nathandunn;

--
-- Name: cell_line_pub_cell_line_pub_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE cell_line_pub_cell_line_pub_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE cell_line_pub_cell_line_pub_id_seq OWNER TO nathandunn;

--
-- Name: cell_line_pub_cell_line_pub_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE cell_line_pub_cell_line_pub_id_seq OWNED BY cell_line_pub.cell_line_pub_id;


--
-- Name: cell_line_relationship; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE cell_line_relationship (
    cell_line_relationship_id integer NOT NULL,
    subject_id integer NOT NULL,
    object_id integer NOT NULL,
    type_id integer NOT NULL
);


ALTER TABLE cell_line_relationship OWNER TO nathandunn;

--
-- Name: cell_line_relationship_cell_line_relationship_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE cell_line_relationship_cell_line_relationship_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE cell_line_relationship_cell_line_relationship_id_seq OWNER TO nathandunn;

--
-- Name: cell_line_relationship_cell_line_relationship_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE cell_line_relationship_cell_line_relationship_id_seq OWNED BY cell_line_relationship.cell_line_relationship_id;


--
-- Name: cell_line_synonym; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE cell_line_synonym (
    cell_line_synonym_id integer NOT NULL,
    cell_line_id integer NOT NULL,
    synonym_id integer NOT NULL,
    pub_id integer NOT NULL,
    is_current boolean DEFAULT false NOT NULL,
    is_internal boolean DEFAULT false NOT NULL
);


ALTER TABLE cell_line_synonym OWNER TO nathandunn;

--
-- Name: cell_line_synonym_cell_line_synonym_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE cell_line_synonym_cell_line_synonym_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE cell_line_synonym_cell_line_synonym_id_seq OWNER TO nathandunn;

--
-- Name: cell_line_synonym_cell_line_synonym_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE cell_line_synonym_cell_line_synonym_id_seq OWNED BY cell_line_synonym.cell_line_synonym_id;


--
-- Name: cell_lineprop; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE cell_lineprop (
    cell_lineprop_id integer NOT NULL,
    cell_line_id integer NOT NULL,
    type_id integer NOT NULL,
    value text,
    rank integer DEFAULT 0 NOT NULL
);


ALTER TABLE cell_lineprop OWNER TO nathandunn;

--
-- Name: cell_lineprop_cell_lineprop_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE cell_lineprop_cell_lineprop_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE cell_lineprop_cell_lineprop_id_seq OWNER TO nathandunn;

--
-- Name: cell_lineprop_cell_lineprop_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE cell_lineprop_cell_lineprop_id_seq OWNED BY cell_lineprop.cell_lineprop_id;


--
-- Name: cell_lineprop_pub; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE cell_lineprop_pub (
    cell_lineprop_pub_id integer NOT NULL,
    cell_lineprop_id integer NOT NULL,
    pub_id integer NOT NULL
);


ALTER TABLE cell_lineprop_pub OWNER TO nathandunn;

--
-- Name: cell_lineprop_pub_cell_lineprop_pub_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE cell_lineprop_pub_cell_lineprop_pub_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE cell_lineprop_pub_cell_lineprop_pub_id_seq OWNER TO nathandunn;

--
-- Name: cell_lineprop_pub_cell_lineprop_pub_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE cell_lineprop_pub_cell_lineprop_pub_id_seq OWNED BY cell_lineprop_pub.cell_lineprop_pub_id;


--
-- Name: chadoprop; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE chadoprop (
    chadoprop_id integer NOT NULL,
    type_id integer NOT NULL,
    value text,
    rank integer DEFAULT 0 NOT NULL
);


ALTER TABLE chadoprop OWNER TO nathandunn;

--
-- Name: TABLE chadoprop; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE chadoprop IS 'This table is different from other prop tables in the database, as it is for storing information about the database itself, like schema version';


--
-- Name: COLUMN chadoprop.type_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN chadoprop.type_id IS 'The name of the property or slot is a cvterm. The meaning of the property is defined in that cvterm.';


--
-- Name: COLUMN chadoprop.value; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN chadoprop.value IS 'The value of the property, represented as text. Numeric values are converted to their text representation.';


--
-- Name: COLUMN chadoprop.rank; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN chadoprop.rank IS 'Property-Value ordering. Any
cv can have multiple values for any particular property type -
these are ordered in a list using rank, counting from zero. For
properties that are single-valued rather than multi-valued, the
default 0 value should be used.';


--
-- Name: chadoprop_chadoprop_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE chadoprop_chadoprop_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE chadoprop_chadoprop_id_seq OWNER TO nathandunn;

--
-- Name: chadoprop_chadoprop_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE chadoprop_chadoprop_id_seq OWNED BY chadoprop.chadoprop_id;


--
-- Name: channel; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE channel (
    channel_id integer NOT NULL,
    name text NOT NULL,
    definition text NOT NULL
);


ALTER TABLE channel OWNER TO nathandunn;

--
-- Name: TABLE channel; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE channel IS 'Different array platforms can record signals from one or more channels (cDNA arrays typically use two CCD, but Affymetrix uses only one).';


--
-- Name: channel_channel_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE channel_channel_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE channel_channel_id_seq OWNER TO nathandunn;

--
-- Name: channel_channel_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE channel_channel_id_seq OWNED BY channel.channel_id;


--
-- Name: common_ancestor_cvterm; Type: VIEW; Schema: chado; Owner: nathandunn
--

CREATE VIEW common_ancestor_cvterm AS
 SELECT p1.subject_id AS cvterm1_id,
    p2.subject_id AS cvterm2_id,
    p1.object_id AS ancestor_cvterm_id,
    p1.pathdistance AS pathdistance1,
    p2.pathdistance AS pathdistance2,
    (p1.pathdistance + p2.pathdistance) AS total_pathdistance
   FROM cvtermpath p1,
    cvtermpath p2
  WHERE (p1.object_id = p2.object_id);


ALTER TABLE common_ancestor_cvterm OWNER TO nathandunn;

--
-- Name: VIEW common_ancestor_cvterm; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON VIEW common_ancestor_cvterm IS 'The common ancestor of any
two terms is the intersection of both terms ancestors. Two terms can
have multiple common ancestors. Use total_pathdistance to get the
least common ancestor';


--
-- Name: common_descendant_cvterm; Type: VIEW; Schema: chado; Owner: nathandunn
--

CREATE VIEW common_descendant_cvterm AS
 SELECT p1.object_id AS cvterm1_id,
    p2.object_id AS cvterm2_id,
    p1.subject_id AS ancestor_cvterm_id,
    p1.pathdistance AS pathdistance1,
    p2.pathdistance AS pathdistance2,
    (p1.pathdistance + p2.pathdistance) AS total_pathdistance
   FROM cvtermpath p1,
    cvtermpath p2
  WHERE (p1.subject_id = p2.subject_id);


ALTER TABLE common_descendant_cvterm OWNER TO nathandunn;

--
-- Name: VIEW common_descendant_cvterm; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON VIEW common_descendant_cvterm IS 'The common descendant of
any two terms is the intersection of both terms descendants. Two terms
can have multiple common descendants. Use total_pathdistance to get
the least common ancestor';


--
-- Name: contact; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE contact (
    contact_id integer NOT NULL,
    type_id integer,
    name character varying(255) NOT NULL,
    description character varying(255)
);


ALTER TABLE contact OWNER TO nathandunn;

--
-- Name: TABLE contact; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE contact IS 'Model persons, institutes, groups, organizations, etc.';


--
-- Name: COLUMN contact.type_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN contact.type_id IS 'What type of contact is this?  E.g. "person", "lab".';


--
-- Name: contact_contact_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE contact_contact_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE contact_contact_id_seq OWNER TO nathandunn;

--
-- Name: contact_contact_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE contact_contact_id_seq OWNED BY contact.contact_id;


--
-- Name: contact_relationship; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE contact_relationship (
    contact_relationship_id integer NOT NULL,
    type_id integer NOT NULL,
    subject_id integer NOT NULL,
    object_id integer NOT NULL
);


ALTER TABLE contact_relationship OWNER TO nathandunn;

--
-- Name: TABLE contact_relationship; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE contact_relationship IS 'Model relationships between contacts';


--
-- Name: COLUMN contact_relationship.type_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN contact_relationship.type_id IS 'Relationship type between subject and object. This is a cvterm, typically from the OBO relationship ontology, although other relationship types are allowed.';


--
-- Name: COLUMN contact_relationship.subject_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN contact_relationship.subject_id IS 'The subject of the subj-predicate-obj sentence. In a DAG, this corresponds to the child node.';


--
-- Name: COLUMN contact_relationship.object_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN contact_relationship.object_id IS 'The object of the subj-predicate-obj sentence. In a DAG, this corresponds to the parent node.';


--
-- Name: contact_relationship_contact_relationship_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE contact_relationship_contact_relationship_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE contact_relationship_contact_relationship_id_seq OWNER TO nathandunn;

--
-- Name: contact_relationship_contact_relationship_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE contact_relationship_contact_relationship_id_seq OWNED BY contact_relationship.contact_relationship_id;


--
-- Name: control; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE control (
    control_id integer NOT NULL,
    type_id integer NOT NULL,
    assay_id integer NOT NULL,
    tableinfo_id integer NOT NULL,
    row_id integer NOT NULL,
    name text,
    value text,
    rank integer DEFAULT 0 NOT NULL
);


ALTER TABLE control OWNER TO nathandunn;

--
-- Name: control_control_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE control_control_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE control_control_id_seq OWNER TO nathandunn;

--
-- Name: control_control_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE control_control_id_seq OWNED BY control.control_id;


--
-- Name: cv; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE cv (
    cv_id integer NOT NULL,
    name character varying(255) NOT NULL,
    definition text
);


ALTER TABLE cv OWNER TO nathandunn;

--
-- Name: TABLE cv; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE cv IS 'A controlled vocabulary or ontology. A cv is
composed of cvterms (AKA terms, classes, types, universals - relations
and properties are also stored in cvterm) and the relationships
between them.';


--
-- Name: COLUMN cv.name; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN cv.name IS 'The name of the ontology. This
corresponds to the obo-format -namespace-. cv names uniquely identify
the cv. In OBO file format, the cv.name is known as the namespace.';


--
-- Name: COLUMN cv.definition; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN cv.definition IS 'A text description of the criteria for
membership of this ontology.';


--
-- Name: cv_cv_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE cv_cv_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE cv_cv_id_seq OWNER TO nathandunn;

--
-- Name: cv_cv_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE cv_cv_id_seq OWNED BY cv.cv_id;


--
-- Name: cv_cvterm_count; Type: VIEW; Schema: chado; Owner: nathandunn
--

CREATE VIEW cv_cvterm_count AS
 SELECT cv.name,
    count(*) AS num_terms_excl_obs
   FROM (cv
     JOIN cvterm USING (cv_id))
  WHERE (cvterm.is_obsolete = 0)
  GROUP BY cv.name;


ALTER TABLE cv_cvterm_count OWNER TO nathandunn;

--
-- Name: VIEW cv_cvterm_count; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON VIEW cv_cvterm_count IS 'per-cv terms counts (excludes obsoletes)';


--
-- Name: cv_cvterm_count_with_obs; Type: VIEW; Schema: chado; Owner: nathandunn
--

CREATE VIEW cv_cvterm_count_with_obs AS
 SELECT cv.name,
    count(*) AS num_terms_incl_obs
   FROM (cv
     JOIN cvterm USING (cv_id))
  GROUP BY cv.name;


ALTER TABLE cv_cvterm_count_with_obs OWNER TO nathandunn;

--
-- Name: VIEW cv_cvterm_count_with_obs; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON VIEW cv_cvterm_count_with_obs IS 'per-cv terms counts (includes obsoletes)';


--
-- Name: cvterm_relationship; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE cvterm_relationship (
    cvterm_relationship_id integer NOT NULL,
    type_id integer NOT NULL,
    subject_id integer NOT NULL,
    object_id integer NOT NULL
);


ALTER TABLE cvterm_relationship OWNER TO nathandunn;

--
-- Name: TABLE cvterm_relationship; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE cvterm_relationship IS 'A relationship linking two
cvterms. Each cvterm_relationship constitutes an edge in the graph
defined by the collection of cvterms and cvterm_relationships. The
meaning of the cvterm_relationship depends on the definition of the
cvterm R refered to by type_id. However, in general the definitions
are such that the statement "all SUBJs REL some OBJ" is true. The
cvterm_relationship statement is about the subject, not the
object. For example "insect wing part_of thorax".';


--
-- Name: COLUMN cvterm_relationship.type_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN cvterm_relationship.type_id IS 'The nature of the
relationship between subject and object. Note that relations are also
housed in the cvterm table, typically from the OBO relationship
ontology, although other relationship types are allowed.';


--
-- Name: COLUMN cvterm_relationship.subject_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN cvterm_relationship.subject_id IS 'The subject of
the subj-predicate-obj sentence. The cvterm_relationship is about the
subject. In a graph, this typically corresponds to the child node.';


--
-- Name: COLUMN cvterm_relationship.object_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN cvterm_relationship.object_id IS 'The object of the
subj-predicate-obj sentence. The cvterm_relationship refers to the
object. In a graph, this typically corresponds to the parent node.';


--
-- Name: cv_leaf; Type: VIEW; Schema: chado; Owner: nathandunn
--

CREATE VIEW cv_leaf AS
 SELECT cvterm.cv_id,
    cvterm.cvterm_id
   FROM cvterm
  WHERE (NOT (cvterm.cvterm_id IN ( SELECT cvterm_relationship.object_id
           FROM cvterm_relationship)));


ALTER TABLE cv_leaf OWNER TO nathandunn;

--
-- Name: VIEW cv_leaf; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON VIEW cv_leaf IS 'the leaves of a cv are the set of terms
which have no children (terms that are not the object of a
relation). All cvs will have at least 1 leaf';


--
-- Name: cv_link_count; Type: VIEW; Schema: chado; Owner: nathandunn
--

CREATE VIEW cv_link_count AS
 SELECT cv.name AS cv_name,
    relation.name AS relation_name,
    relation_cv.name AS relation_cv_name,
    count(*) AS num_links
   FROM ((((cv
     JOIN cvterm ON ((cvterm.cv_id = cv.cv_id)))
     JOIN cvterm_relationship ON ((cvterm.cvterm_id = cvterm_relationship.subject_id)))
     JOIN cvterm relation ON ((cvterm_relationship.type_id = relation.cvterm_id)))
     JOIN cv relation_cv ON ((relation.cv_id = relation_cv.cv_id)))
  GROUP BY cv.name, relation.name, relation_cv.name;


ALTER TABLE cv_link_count OWNER TO nathandunn;

--
-- Name: VIEW cv_link_count; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON VIEW cv_link_count IS 'per-cv summary of number of
links (cvterm_relationships) broken down by
relationship_type. num_links is the total # of links of the specified
type in which the subject_id of the link is in the named cv';


--
-- Name: cv_path_count; Type: VIEW; Schema: chado; Owner: nathandunn
--

CREATE VIEW cv_path_count AS
 SELECT cv.name AS cv_name,
    relation.name AS relation_name,
    relation_cv.name AS relation_cv_name,
    count(*) AS num_paths
   FROM ((((cv
     JOIN cvterm ON ((cvterm.cv_id = cv.cv_id)))
     JOIN cvtermpath ON ((cvterm.cvterm_id = cvtermpath.subject_id)))
     JOIN cvterm relation ON ((cvtermpath.type_id = relation.cvterm_id)))
     JOIN cv relation_cv ON ((relation.cv_id = relation_cv.cv_id)))
  GROUP BY cv.name, relation.name, relation_cv.name;


ALTER TABLE cv_path_count OWNER TO nathandunn;

--
-- Name: VIEW cv_path_count; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON VIEW cv_path_count IS 'per-cv summary of number of
paths (cvtermpaths) broken down by relationship_type. num_paths is the
total # of paths of the specified type in which the subject_id of the
path is in the named cv. See also: cv_distinct_relations';


--
-- Name: cv_root; Type: VIEW; Schema: chado; Owner: nathandunn
--

CREATE VIEW cv_root AS
 SELECT cvterm.cv_id,
    cvterm.cvterm_id AS root_cvterm_id
   FROM cvterm
  WHERE ((NOT (cvterm.cvterm_id IN ( SELECT cvterm_relationship.subject_id
           FROM cvterm_relationship))) AND (cvterm.is_obsolete = 0));


ALTER TABLE cv_root OWNER TO nathandunn;

--
-- Name: VIEW cv_root; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON VIEW cv_root IS 'the roots of a cv are the set of terms
which have no parents (terms that are not the subject of a
relation). Most cvs will have a single root, some may have >1. All
will have at least 1';


--
-- Name: cv_root_mview; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE cv_root_mview (
    name character varying(1024),
    cvterm_id integer,
    cv_id integer,
    cv_name character varying(255)
);


ALTER TABLE cv_root_mview OWNER TO nathandunn;

--
-- Name: cvprop; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE cvprop (
    cvprop_id integer NOT NULL,
    cv_id integer NOT NULL,
    type_id integer NOT NULL,
    value text,
    rank integer DEFAULT 0 NOT NULL
);


ALTER TABLE cvprop OWNER TO nathandunn;

--
-- Name: TABLE cvprop; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE cvprop IS 'Additional extensible properties can be attached to a cv using this table.  A notable example would be the cv version';


--
-- Name: COLUMN cvprop.type_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN cvprop.type_id IS 'The name of the property or slot is a cvterm. The meaning of the property is defined in that cvterm.';


--
-- Name: COLUMN cvprop.value; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN cvprop.value IS 'The value of the property, represented as text. Numeric values are converted to their text representation.';


--
-- Name: COLUMN cvprop.rank; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN cvprop.rank IS 'Property-Value ordering. Any
cv can have multiple values for any particular property type -
these are ordered in a list using rank, counting from zero. For
properties that are single-valued rather than multi-valued, the
default 0 value should be used.';


--
-- Name: cvprop_cvprop_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE cvprop_cvprop_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE cvprop_cvprop_id_seq OWNER TO nathandunn;

--
-- Name: cvprop_cvprop_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE cvprop_cvprop_id_seq OWNED BY cvprop.cvprop_id;


--
-- Name: cvterm_cvterm_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE cvterm_cvterm_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE cvterm_cvterm_id_seq OWNER TO nathandunn;

--
-- Name: cvterm_cvterm_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE cvterm_cvterm_id_seq OWNED BY cvterm.cvterm_id;


--
-- Name: cvterm_dbxref; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE cvterm_dbxref (
    cvterm_dbxref_id integer NOT NULL,
    cvterm_id integer NOT NULL,
    dbxref_id integer NOT NULL,
    is_for_definition integer DEFAULT 0 NOT NULL
);


ALTER TABLE cvterm_dbxref OWNER TO nathandunn;

--
-- Name: TABLE cvterm_dbxref; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE cvterm_dbxref IS 'In addition to the primary
identifier (cvterm.dbxref_id) a cvterm can have zero or more secondary
identifiers/dbxrefs, which may refer to records in external
databases. The exact semantics of cvterm_dbxref are not fixed. For
example: the dbxref could be a pubmed ID that is pertinent to the
cvterm, or it could be an equivalent or similar term in another
ontology. For example, GO cvterms are typically linked to InterPro
IDs, even though the nature of the relationship between them is
largely one of statistical association. The dbxref may be have data
records attached in the same database instance, or it could be a
"hanging" dbxref pointing to some external database. NOTE: If the
desired objective is to link two cvterms together, and the nature of
the relation is known and holds for all instances of the subject
cvterm then consider instead using cvterm_relationship together with a
well-defined relation.';


--
-- Name: COLUMN cvterm_dbxref.is_for_definition; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN cvterm_dbxref.is_for_definition IS 'A
cvterm.definition should be supported by one or more references. If
this column is true, the dbxref is not for a term in an external database -
it is a dbxref for provenance information for the definition.';


--
-- Name: cvterm_dbxref_cvterm_dbxref_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE cvterm_dbxref_cvterm_dbxref_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE cvterm_dbxref_cvterm_dbxref_id_seq OWNER TO nathandunn;

--
-- Name: cvterm_dbxref_cvterm_dbxref_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE cvterm_dbxref_cvterm_dbxref_id_seq OWNED BY cvterm_dbxref.cvterm_dbxref_id;


--
-- Name: cvterm_relationship_cvterm_relationship_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE cvterm_relationship_cvterm_relationship_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE cvterm_relationship_cvterm_relationship_id_seq OWNER TO nathandunn;

--
-- Name: cvterm_relationship_cvterm_relationship_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE cvterm_relationship_cvterm_relationship_id_seq OWNED BY cvterm_relationship.cvterm_relationship_id;


--
-- Name: cvtermpath_cvtermpath_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE cvtermpath_cvtermpath_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE cvtermpath_cvtermpath_id_seq OWNER TO nathandunn;

--
-- Name: cvtermpath_cvtermpath_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE cvtermpath_cvtermpath_id_seq OWNED BY cvtermpath.cvtermpath_id;


--
-- Name: cvtermprop; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE cvtermprop (
    cvtermprop_id integer NOT NULL,
    cvterm_id integer NOT NULL,
    type_id integer NOT NULL,
    value text DEFAULT ''::text NOT NULL,
    rank integer DEFAULT 0 NOT NULL
);


ALTER TABLE cvtermprop OWNER TO nathandunn;

--
-- Name: TABLE cvtermprop; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE cvtermprop IS 'Additional extensible properties can be attached to a cvterm using this table. Corresponds to -AnnotationProperty- in W3C OWL format.';


--
-- Name: COLUMN cvtermprop.type_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN cvtermprop.type_id IS 'The name of the property or slot is a cvterm. The meaning of the property is defined in that cvterm.';


--
-- Name: COLUMN cvtermprop.value; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN cvtermprop.value IS 'The value of the property, represented as text. Numeric values are converted to their text representation.';


--
-- Name: COLUMN cvtermprop.rank; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN cvtermprop.rank IS 'Property-Value ordering. Any
cvterm can have multiple values for any particular property type -
these are ordered in a list using rank, counting from zero. For
properties that are single-valued rather than multi-valued, the
default 0 value should be used.';


--
-- Name: cvtermprop_cvtermprop_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE cvtermprop_cvtermprop_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE cvtermprop_cvtermprop_id_seq OWNER TO nathandunn;

--
-- Name: cvtermprop_cvtermprop_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE cvtermprop_cvtermprop_id_seq OWNED BY cvtermprop.cvtermprop_id;


--
-- Name: cvtermsynonym; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE cvtermsynonym (
    cvtermsynonym_id integer NOT NULL,
    cvterm_id integer NOT NULL,
    synonym character varying(1024) NOT NULL,
    type_id integer
);


ALTER TABLE cvtermsynonym OWNER TO nathandunn;

--
-- Name: TABLE cvtermsynonym; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE cvtermsynonym IS 'A cvterm actually represents a
distinct class or concept. A concept can be refered to by different
phrases or names. In addition to the primary name (cvterm.name) there
can be a number of alternative aliases or synonyms. For example, "T
cell" as a synonym for "T lymphocyte".';


--
-- Name: COLUMN cvtermsynonym.type_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN cvtermsynonym.type_id IS 'A synonym can be exact,
narrower, or broader than.';


--
-- Name: cvtermsynonym_cvtermsynonym_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE cvtermsynonym_cvtermsynonym_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE cvtermsynonym_cvtermsynonym_id_seq OWNER TO nathandunn;

--
-- Name: cvtermsynonym_cvtermsynonym_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE cvtermsynonym_cvtermsynonym_id_seq OWNED BY cvtermsynonym.cvtermsynonym_id;


--
-- Name: db_db_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE db_db_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE db_db_id_seq OWNER TO nathandunn;

--
-- Name: db_db_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE db_db_id_seq OWNED BY db.db_id;


--
-- Name: db_dbxref_count; Type: VIEW; Schema: chado; Owner: nathandunn
--

CREATE VIEW db_dbxref_count AS
 SELECT db.name,
    count(*) AS num_dbxrefs
   FROM (db
     JOIN dbxref USING (db_id))
  GROUP BY db.name;


ALTER TABLE db_dbxref_count OWNER TO nathandunn;

--
-- Name: VIEW db_dbxref_count; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON VIEW db_dbxref_count IS 'per-db dbxref counts';


--
-- Name: dbxref_dbxref_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE dbxref_dbxref_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE dbxref_dbxref_id_seq OWNER TO nathandunn;

--
-- Name: dbxref_dbxref_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE dbxref_dbxref_id_seq OWNED BY dbxref.dbxref_id;


--
-- Name: dbxrefprop; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE dbxrefprop (
    dbxrefprop_id integer NOT NULL,
    dbxref_id integer NOT NULL,
    type_id integer NOT NULL,
    value text DEFAULT ''::text NOT NULL,
    rank integer DEFAULT 0 NOT NULL
);


ALTER TABLE dbxrefprop OWNER TO nathandunn;

--
-- Name: TABLE dbxrefprop; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE dbxrefprop IS 'Metadata about a dbxref. Note that this is not defined in the dbxref module, as it depends on the cvterm table. This table has a structure analagous to cvtermprop.';


--
-- Name: dbxrefprop_dbxrefprop_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE dbxrefprop_dbxrefprop_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE dbxrefprop_dbxrefprop_id_seq OWNER TO nathandunn;

--
-- Name: dbxrefprop_dbxrefprop_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE dbxrefprop_dbxrefprop_id_seq OWNED BY dbxrefprop.dbxrefprop_id;


--
-- Name: dfeatureloc; Type: VIEW; Schema: chado; Owner: nathandunn
--

CREATE VIEW dfeatureloc AS
 SELECT featureloc.featureloc_id,
    featureloc.feature_id,
    featureloc.srcfeature_id,
    featureloc.fmin AS nbeg,
    featureloc.is_fmin_partial AS is_nbeg_partial,
    featureloc.fmax AS nend,
    featureloc.is_fmax_partial AS is_nend_partial,
    featureloc.strand,
    featureloc.phase,
    featureloc.residue_info,
    featureloc.locgroup,
    featureloc.rank
   FROM featureloc
  WHERE ((featureloc.strand < 0) OR (featureloc.phase < 0))
UNION
 SELECT featureloc.featureloc_id,
    featureloc.feature_id,
    featureloc.srcfeature_id,
    featureloc.fmax AS nbeg,
    featureloc.is_fmax_partial AS is_nbeg_partial,
    featureloc.fmin AS nend,
    featureloc.is_fmin_partial AS is_nend_partial,
    featureloc.strand,
    featureloc.phase,
    featureloc.residue_info,
    featureloc.locgroup,
    featureloc.rank
   FROM featureloc
  WHERE (((featureloc.strand IS NULL) OR (featureloc.strand >= 0)) OR (featureloc.phase >= 0));


ALTER TABLE dfeatureloc OWNER TO nathandunn;

--
-- Name: eimage; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE eimage (
    eimage_id integer NOT NULL,
    eimage_data text,
    eimage_type character varying(255) NOT NULL,
    image_uri character varying(255)
);


ALTER TABLE eimage OWNER TO nathandunn;

--
-- Name: COLUMN eimage.eimage_data; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN eimage.eimage_data IS 'We expect images in eimage_data (e.g. JPEGs) to be uuencoded.';


--
-- Name: COLUMN eimage.eimage_type; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN eimage.eimage_type IS 'Describes the type of data in eimage_data.';


--
-- Name: eimage_eimage_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE eimage_eimage_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE eimage_eimage_id_seq OWNER TO nathandunn;

--
-- Name: eimage_eimage_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE eimage_eimage_id_seq OWNED BY eimage.eimage_id;


--
-- Name: element; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE element (
    element_id integer NOT NULL,
    feature_id integer,
    arraydesign_id integer NOT NULL,
    type_id integer,
    dbxref_id integer
);


ALTER TABLE element OWNER TO nathandunn;

--
-- Name: TABLE element; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE element IS 'Represents a feature of the array. This is typically a region of the array coated or bound to DNA.';


--
-- Name: element_element_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE element_element_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE element_element_id_seq OWNER TO nathandunn;

--
-- Name: element_element_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE element_element_id_seq OWNED BY element.element_id;


--
-- Name: element_relationship; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE element_relationship (
    element_relationship_id integer NOT NULL,
    subject_id integer NOT NULL,
    type_id integer NOT NULL,
    object_id integer NOT NULL,
    value text,
    rank integer DEFAULT 0 NOT NULL
);


ALTER TABLE element_relationship OWNER TO nathandunn;

--
-- Name: TABLE element_relationship; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE element_relationship IS 'Sometimes we want to combine measurements from multiple elements to get a composite value. Affymetrix combines many probes to form a probeset measurement, for instance.';


--
-- Name: element_relationship_element_relationship_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE element_relationship_element_relationship_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE element_relationship_element_relationship_id_seq OWNER TO nathandunn;

--
-- Name: element_relationship_element_relationship_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE element_relationship_element_relationship_id_seq OWNED BY element_relationship.element_relationship_id;


--
-- Name: elementresult; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE elementresult (
    elementresult_id integer NOT NULL,
    element_id integer NOT NULL,
    quantification_id integer NOT NULL,
    signal double precision NOT NULL
);


ALTER TABLE elementresult OWNER TO nathandunn;

--
-- Name: TABLE elementresult; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE elementresult IS 'An element on an array produces a measurement when hybridized to a biomaterial (traceable through quantification_id). This is the base data from which tables that actually contain data inherit.';


--
-- Name: elementresult_elementresult_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE elementresult_elementresult_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE elementresult_elementresult_id_seq OWNER TO nathandunn;

--
-- Name: elementresult_elementresult_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE elementresult_elementresult_id_seq OWNED BY elementresult.elementresult_id;


--
-- Name: elementresult_relationship; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE elementresult_relationship (
    elementresult_relationship_id integer NOT NULL,
    subject_id integer NOT NULL,
    type_id integer NOT NULL,
    object_id integer NOT NULL,
    value text,
    rank integer DEFAULT 0 NOT NULL
);


ALTER TABLE elementresult_relationship OWNER TO nathandunn;

--
-- Name: TABLE elementresult_relationship; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE elementresult_relationship IS 'Sometimes we want to combine measurements from multiple elements to get a composite value. Affymetrix combines many probes to form a probeset measurement, for instance.';


--
-- Name: elementresult_relationship_elementresult_relationship_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE elementresult_relationship_elementresult_relationship_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE elementresult_relationship_elementresult_relationship_id_seq OWNER TO nathandunn;

--
-- Name: elementresult_relationship_elementresult_relationship_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE elementresult_relationship_elementresult_relationship_id_seq OWNED BY elementresult_relationship.elementresult_relationship_id;


--
-- Name: environment; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE environment (
    environment_id integer NOT NULL,
    uniquename text NOT NULL,
    description text
);


ALTER TABLE environment OWNER TO nathandunn;

--
-- Name: TABLE environment; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE environment IS 'The environmental component of a phenotype description.';


--
-- Name: environment_cvterm; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE environment_cvterm (
    environment_cvterm_id integer NOT NULL,
    environment_id integer NOT NULL,
    cvterm_id integer NOT NULL
);


ALTER TABLE environment_cvterm OWNER TO nathandunn;

--
-- Name: environment_cvterm_environment_cvterm_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE environment_cvterm_environment_cvterm_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE environment_cvterm_environment_cvterm_id_seq OWNER TO nathandunn;

--
-- Name: environment_cvterm_environment_cvterm_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE environment_cvterm_environment_cvterm_id_seq OWNED BY environment_cvterm.environment_cvterm_id;


--
-- Name: environment_environment_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE environment_environment_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE environment_environment_id_seq OWNER TO nathandunn;

--
-- Name: environment_environment_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE environment_environment_id_seq OWNED BY environment.environment_id;


--
-- Name: expression; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE expression (
    expression_id integer NOT NULL,
    uniquename text NOT NULL,
    md5checksum character(32),
    description text
);


ALTER TABLE expression OWNER TO nathandunn;

--
-- Name: TABLE expression; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE expression IS 'The expression table is essentially a bridge table.';


--
-- Name: expression_cvterm; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE expression_cvterm (
    expression_cvterm_id integer NOT NULL,
    expression_id integer NOT NULL,
    cvterm_id integer NOT NULL,
    rank integer DEFAULT 0 NOT NULL,
    cvterm_type_id integer NOT NULL
);


ALTER TABLE expression_cvterm OWNER TO nathandunn;

--
-- Name: expression_cvterm_expression_cvterm_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE expression_cvterm_expression_cvterm_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE expression_cvterm_expression_cvterm_id_seq OWNER TO nathandunn;

--
-- Name: expression_cvterm_expression_cvterm_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE expression_cvterm_expression_cvterm_id_seq OWNED BY expression_cvterm.expression_cvterm_id;


--
-- Name: expression_cvtermprop; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE expression_cvtermprop (
    expression_cvtermprop_id integer NOT NULL,
    expression_cvterm_id integer NOT NULL,
    type_id integer NOT NULL,
    value text,
    rank integer DEFAULT 0 NOT NULL
);


ALTER TABLE expression_cvtermprop OWNER TO nathandunn;

--
-- Name: TABLE expression_cvtermprop; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE expression_cvtermprop IS 'Extensible properties for
expression to cvterm associations. Examples: qualifiers.';


--
-- Name: COLUMN expression_cvtermprop.type_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN expression_cvtermprop.type_id IS 'The name of the
property/slot is a cvterm. The meaning of the property is defined in
that cvterm. For example, cvterms may come from the FlyBase miscellaneous cv.';


--
-- Name: COLUMN expression_cvtermprop.value; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN expression_cvtermprop.value IS 'The value of the
property, represented as text. Numeric values are converted to their
text representation. This is less efficient than using native database
types, but is easier to query.';


--
-- Name: COLUMN expression_cvtermprop.rank; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN expression_cvtermprop.rank IS 'Property-Value
ordering. Any expression_cvterm can have multiple values for any particular
property type - these are ordered in a list using rank, counting from
zero. For properties that are single-valued rather than multi-valued,
the default 0 value should be used.';


--
-- Name: expression_cvtermprop_expression_cvtermprop_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE expression_cvtermprop_expression_cvtermprop_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE expression_cvtermprop_expression_cvtermprop_id_seq OWNER TO nathandunn;

--
-- Name: expression_cvtermprop_expression_cvtermprop_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE expression_cvtermprop_expression_cvtermprop_id_seq OWNED BY expression_cvtermprop.expression_cvtermprop_id;


--
-- Name: expression_expression_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE expression_expression_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE expression_expression_id_seq OWNER TO nathandunn;

--
-- Name: expression_expression_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE expression_expression_id_seq OWNED BY expression.expression_id;


--
-- Name: expression_image; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE expression_image (
    expression_image_id integer NOT NULL,
    expression_id integer NOT NULL,
    eimage_id integer NOT NULL
);


ALTER TABLE expression_image OWNER TO nathandunn;

--
-- Name: expression_image_expression_image_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE expression_image_expression_image_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE expression_image_expression_image_id_seq OWNER TO nathandunn;

--
-- Name: expression_image_expression_image_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE expression_image_expression_image_id_seq OWNED BY expression_image.expression_image_id;


--
-- Name: expression_pub; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE expression_pub (
    expression_pub_id integer NOT NULL,
    expression_id integer NOT NULL,
    pub_id integer NOT NULL
);


ALTER TABLE expression_pub OWNER TO nathandunn;

--
-- Name: expression_pub_expression_pub_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE expression_pub_expression_pub_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE expression_pub_expression_pub_id_seq OWNER TO nathandunn;

--
-- Name: expression_pub_expression_pub_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE expression_pub_expression_pub_id_seq OWNED BY expression_pub.expression_pub_id;


--
-- Name: expressionprop; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE expressionprop (
    expressionprop_id integer NOT NULL,
    expression_id integer NOT NULL,
    type_id integer NOT NULL,
    value text,
    rank integer DEFAULT 0 NOT NULL
);


ALTER TABLE expressionprop OWNER TO nathandunn;

--
-- Name: expressionprop_expressionprop_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE expressionprop_expressionprop_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE expressionprop_expressionprop_id_seq OWNER TO nathandunn;

--
-- Name: expressionprop_expressionprop_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE expressionprop_expressionprop_id_seq OWNED BY expressionprop.expressionprop_id;


--
-- Name: f_type; Type: VIEW; Schema: chado; Owner: nathandunn
--

CREATE VIEW f_type AS
 SELECT f.feature_id,
    f.name,
    f.dbxref_id,
    c.name AS type,
    f.residues,
    f.seqlen,
    f.md5checksum,
    f.type_id,
    f.timeaccessioned,
    f.timelastmodified
   FROM feature f,
    cvterm c
  WHERE (f.type_id = c.cvterm_id);


ALTER TABLE f_type OWNER TO nathandunn;

--
-- Name: f_loc; Type: VIEW; Schema: chado; Owner: nathandunn
--

CREATE VIEW f_loc AS
 SELECT f.feature_id,
    f.name,
    f.dbxref_id,
    fl.nbeg,
    fl.nend,
    fl.strand
   FROM dfeatureloc fl,
    f_type f
  WHERE (f.feature_id = fl.feature_id);


ALTER TABLE f_loc OWNER TO nathandunn;

--
-- Name: feature_contains; Type: VIEW; Schema: chado; Owner: nathandunn
--

CREATE VIEW feature_contains AS
 SELECT x.feature_id AS subject_id,
    y.feature_id AS object_id
   FROM featureloc x,
    featureloc y
  WHERE ((x.srcfeature_id = y.srcfeature_id) AND ((y.fmin >= x.fmin) AND (y.fmin <= x.fmax)));


ALTER TABLE feature_contains OWNER TO nathandunn;

--
-- Name: VIEW feature_contains; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON VIEW feature_contains IS 'subject intervals contains (or is
same as) object interval. transitive,reflexive';


--
-- Name: feature_cvterm_dbxref; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE feature_cvterm_dbxref (
    feature_cvterm_dbxref_id integer NOT NULL,
    feature_cvterm_id integer NOT NULL,
    dbxref_id integer NOT NULL
);


ALTER TABLE feature_cvterm_dbxref OWNER TO nathandunn;

--
-- Name: TABLE feature_cvterm_dbxref; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE feature_cvterm_dbxref IS 'Additional dbxrefs for an association. Rows in the feature_cvterm table may be backed up by dbxrefs. For example, a feature_cvterm association that was inferred via a protein-protein interaction may be backed by by refering to the dbxref for the alternate protein. Corresponds to the WITH column in a GO gene association file (but can also be used for other analagous associations). See http://www.geneontology.org/doc/GO.annotation.shtml#file for more details.';


--
-- Name: feature_cvterm_dbxref_feature_cvterm_dbxref_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE feature_cvterm_dbxref_feature_cvterm_dbxref_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE feature_cvterm_dbxref_feature_cvterm_dbxref_id_seq OWNER TO nathandunn;

--
-- Name: feature_cvterm_dbxref_feature_cvterm_dbxref_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE feature_cvterm_dbxref_feature_cvterm_dbxref_id_seq OWNED BY feature_cvterm_dbxref.feature_cvterm_dbxref_id;


--
-- Name: feature_cvterm_feature_cvterm_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE feature_cvterm_feature_cvterm_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE feature_cvterm_feature_cvterm_id_seq OWNER TO nathandunn;

--
-- Name: feature_cvterm_feature_cvterm_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE feature_cvterm_feature_cvterm_id_seq OWNED BY feature_cvterm.feature_cvterm_id;


--
-- Name: feature_cvterm_pub; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE feature_cvterm_pub (
    feature_cvterm_pub_id integer NOT NULL,
    feature_cvterm_id integer NOT NULL,
    pub_id integer NOT NULL
);


ALTER TABLE feature_cvterm_pub OWNER TO nathandunn;

--
-- Name: TABLE feature_cvterm_pub; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE feature_cvterm_pub IS 'Secondary pubs for an
association. Each feature_cvterm association is supported by a single
primary publication. Additional secondary pubs can be added using this
linking table (in a GO gene association file, these corresponding to
any IDs after the pipe symbol in the publications column.';


--
-- Name: feature_cvterm_pub_feature_cvterm_pub_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE feature_cvterm_pub_feature_cvterm_pub_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE feature_cvterm_pub_feature_cvterm_pub_id_seq OWNER TO nathandunn;

--
-- Name: feature_cvterm_pub_feature_cvterm_pub_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE feature_cvterm_pub_feature_cvterm_pub_id_seq OWNED BY feature_cvterm_pub.feature_cvterm_pub_id;


--
-- Name: feature_cvtermprop; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE feature_cvtermprop (
    feature_cvtermprop_id integer NOT NULL,
    feature_cvterm_id integer NOT NULL,
    type_id integer NOT NULL,
    value text,
    rank integer DEFAULT 0 NOT NULL
);


ALTER TABLE feature_cvtermprop OWNER TO nathandunn;

--
-- Name: TABLE feature_cvtermprop; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE feature_cvtermprop IS 'Extensible properties for
feature to cvterm associations. Examples: GO evidence codes;
qualifiers; metadata such as the date on which the entry was curated
and the source of the association. See the featureprop table for
meanings of type_id, value and rank.';


--
-- Name: COLUMN feature_cvtermprop.type_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN feature_cvtermprop.type_id IS 'The name of the
property/slot is a cvterm. The meaning of the property is defined in
that cvterm. cvterms may come from the OBO evidence code cv.';


--
-- Name: COLUMN feature_cvtermprop.value; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN feature_cvtermprop.value IS 'The value of the
property, represented as text. Numeric values are converted to their
text representation. This is less efficient than using native database
types, but is easier to query.';


--
-- Name: COLUMN feature_cvtermprop.rank; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN feature_cvtermprop.rank IS 'Property-Value
ordering. Any feature_cvterm can have multiple values for any particular
property type - these are ordered in a list using rank, counting from
zero. For properties that are single-valued rather than multi-valued,
the default 0 value should be used.';


--
-- Name: feature_cvtermprop_feature_cvtermprop_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE feature_cvtermprop_feature_cvtermprop_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE feature_cvtermprop_feature_cvtermprop_id_seq OWNER TO nathandunn;

--
-- Name: feature_cvtermprop_feature_cvtermprop_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE feature_cvtermprop_feature_cvtermprop_id_seq OWNED BY feature_cvtermprop.feature_cvtermprop_id;


--
-- Name: feature_dbxref_feature_dbxref_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE feature_dbxref_feature_dbxref_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE feature_dbxref_feature_dbxref_id_seq OWNER TO nathandunn;

--
-- Name: feature_dbxref_feature_dbxref_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE feature_dbxref_feature_dbxref_id_seq OWNED BY feature_dbxref.feature_dbxref_id;


--
-- Name: feature_difference; Type: VIEW; Schema: chado; Owner: nathandunn
--

CREATE VIEW feature_difference AS
 SELECT x.feature_id AS subject_id,
    y.feature_id AS object_id,
    x.strand AS srcfeature_id,
    x.srcfeature_id AS fmin,
    x.fmin AS fmax,
    y.fmin AS strand
   FROM featureloc x,
    featureloc y
  WHERE ((x.srcfeature_id = y.srcfeature_id) AND ((x.fmin < y.fmin) AND (x.fmax >= y.fmax)))
UNION
 SELECT x.feature_id AS subject_id,
    y.feature_id AS object_id,
    x.strand AS srcfeature_id,
    x.srcfeature_id AS fmin,
    y.fmax,
    x.fmax AS strand
   FROM featureloc x,
    featureloc y
  WHERE ((x.srcfeature_id = y.srcfeature_id) AND ((x.fmax > y.fmax) AND (x.fmin <= y.fmin)));


ALTER TABLE feature_difference OWNER TO nathandunn;

--
-- Name: VIEW feature_difference; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON VIEW feature_difference IS 'size of gap between two features. must be abutting or disjoint';


--
-- Name: feature_disjoint; Type: VIEW; Schema: chado; Owner: nathandunn
--

CREATE VIEW feature_disjoint AS
 SELECT x.feature_id AS subject_id,
    y.feature_id AS object_id
   FROM featureloc x,
    featureloc y
  WHERE ((x.srcfeature_id = y.srcfeature_id) AND ((x.fmax < y.fmin) AND (x.fmin > y.fmax)));


ALTER TABLE feature_disjoint OWNER TO nathandunn;

--
-- Name: VIEW feature_disjoint; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON VIEW feature_disjoint IS 'featurelocs do not meet. symmetric';


--
-- Name: feature_distance; Type: VIEW; Schema: chado; Owner: nathandunn
--

CREATE VIEW feature_distance AS
 SELECT x.feature_id AS subject_id,
    y.feature_id AS object_id,
    x.srcfeature_id,
    x.strand AS subject_strand,
    y.strand AS object_strand,
        CASE
            WHEN (x.fmax <= y.fmin) THEN (x.fmax - y.fmin)
            ELSE (y.fmax - x.fmin)
        END AS distance
   FROM featureloc x,
    featureloc y
  WHERE ((x.srcfeature_id = y.srcfeature_id) AND ((x.fmax <= y.fmin) OR (x.fmin >= y.fmax)));


ALTER TABLE feature_distance OWNER TO nathandunn;

--
-- Name: feature_expression; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE feature_expression (
    feature_expression_id integer NOT NULL,
    expression_id integer NOT NULL,
    feature_id integer NOT NULL,
    pub_id integer NOT NULL
);


ALTER TABLE feature_expression OWNER TO nathandunn;

--
-- Name: feature_expression_feature_expression_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE feature_expression_feature_expression_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE feature_expression_feature_expression_id_seq OWNER TO nathandunn;

--
-- Name: feature_expression_feature_expression_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE feature_expression_feature_expression_id_seq OWNED BY feature_expression.feature_expression_id;


--
-- Name: feature_expressionprop; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE feature_expressionprop (
    feature_expressionprop_id integer NOT NULL,
    feature_expression_id integer NOT NULL,
    type_id integer NOT NULL,
    value text,
    rank integer DEFAULT 0 NOT NULL
);


ALTER TABLE feature_expressionprop OWNER TO nathandunn;

--
-- Name: TABLE feature_expressionprop; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE feature_expressionprop IS 'Extensible properties for
feature_expression (comments, for example). Modeled on feature_cvtermprop.';


--
-- Name: feature_expressionprop_feature_expressionprop_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE feature_expressionprop_feature_expressionprop_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE feature_expressionprop_feature_expressionprop_id_seq OWNER TO nathandunn;

--
-- Name: feature_expressionprop_feature_expressionprop_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE feature_expressionprop_feature_expressionprop_id_seq OWNED BY feature_expressionprop.feature_expressionprop_id;


--
-- Name: feature_feature_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE feature_feature_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE feature_feature_id_seq OWNER TO nathandunn;

--
-- Name: feature_feature_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE feature_feature_id_seq OWNED BY feature.feature_id;


--
-- Name: feature_genotype; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE feature_genotype (
    feature_genotype_id integer NOT NULL,
    feature_id integer NOT NULL,
    genotype_id integer NOT NULL,
    chromosome_id integer,
    rank integer NOT NULL,
    cgroup integer NOT NULL,
    cvterm_id integer NOT NULL
);


ALTER TABLE feature_genotype OWNER TO nathandunn;

--
-- Name: COLUMN feature_genotype.chromosome_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN feature_genotype.chromosome_id IS 'A feature of SO type "chromosome".';


--
-- Name: COLUMN feature_genotype.rank; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN feature_genotype.rank IS 'rank can be used for
n-ploid organisms or to preserve order.';


--
-- Name: COLUMN feature_genotype.cgroup; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN feature_genotype.cgroup IS 'Spatially distinguishable
group. group can be used for distinguishing the chromosomal groups,
for example (RNAi products and so on can be treated as different
groups, as they do not fall on a particular chromosome).';


--
-- Name: feature_genotype_feature_genotype_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE feature_genotype_feature_genotype_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE feature_genotype_feature_genotype_id_seq OWNER TO nathandunn;

--
-- Name: feature_genotype_feature_genotype_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE feature_genotype_feature_genotype_id_seq OWNED BY feature_genotype.feature_genotype_id;


--
-- Name: feature_intersection; Type: VIEW; Schema: chado; Owner: nathandunn
--

CREATE VIEW feature_intersection AS
 SELECT x.feature_id AS subject_id,
    y.feature_id AS object_id,
    x.srcfeature_id,
    x.strand AS subject_strand,
    y.strand AS object_strand,
        CASE
            WHEN (x.fmin < y.fmin) THEN y.fmin
            ELSE x.fmin
        END AS fmin,
        CASE
            WHEN (x.fmax > y.fmax) THEN y.fmax
            ELSE x.fmax
        END AS fmax
   FROM featureloc x,
    featureloc y
  WHERE ((x.srcfeature_id = y.srcfeature_id) AND ((x.fmax >= y.fmin) AND (x.fmin <= y.fmax)));


ALTER TABLE feature_intersection OWNER TO nathandunn;

--
-- Name: VIEW feature_intersection; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON VIEW feature_intersection IS 'set-intersection on interval defined by featureloc. featurelocs must meet';


--
-- Name: feature_meets; Type: VIEW; Schema: chado; Owner: nathandunn
--

CREATE VIEW feature_meets AS
 SELECT x.feature_id AS subject_id,
    y.feature_id AS object_id
   FROM featureloc x,
    featureloc y
  WHERE ((x.srcfeature_id = y.srcfeature_id) AND ((x.fmax >= y.fmin) AND (x.fmin <= y.fmax)));


ALTER TABLE feature_meets OWNER TO nathandunn;

--
-- Name: VIEW feature_meets; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON VIEW feature_meets IS 'intervals have at least one
interbase point in common (ie overlap OR abut). symmetric,reflexive';


--
-- Name: feature_meets_on_same_strand; Type: VIEW; Schema: chado; Owner: nathandunn
--

CREATE VIEW feature_meets_on_same_strand AS
 SELECT x.feature_id AS subject_id,
    y.feature_id AS object_id
   FROM featureloc x,
    featureloc y
  WHERE (((x.srcfeature_id = y.srcfeature_id) AND (x.strand = y.strand)) AND ((x.fmax >= y.fmin) AND (x.fmin <= y.fmax)));


ALTER TABLE feature_meets_on_same_strand OWNER TO nathandunn;

--
-- Name: VIEW feature_meets_on_same_strand; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON VIEW feature_meets_on_same_strand IS 'as feature_meets, but
featurelocs must be on the same strand. symmetric,reflexive';


--
-- Name: feature_phenotype; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE feature_phenotype (
    feature_phenotype_id integer NOT NULL,
    feature_id integer NOT NULL,
    phenotype_id integer NOT NULL
);


ALTER TABLE feature_phenotype OWNER TO nathandunn;

--
-- Name: feature_phenotype_feature_phenotype_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE feature_phenotype_feature_phenotype_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE feature_phenotype_feature_phenotype_id_seq OWNER TO nathandunn;

--
-- Name: feature_phenotype_feature_phenotype_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE feature_phenotype_feature_phenotype_id_seq OWNED BY feature_phenotype.feature_phenotype_id;


--
-- Name: feature_pub_feature_pub_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE feature_pub_feature_pub_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE feature_pub_feature_pub_id_seq OWNER TO nathandunn;

--
-- Name: feature_pub_feature_pub_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE feature_pub_feature_pub_id_seq OWNED BY feature_pub.feature_pub_id;


--
-- Name: feature_pubprop; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE feature_pubprop (
    feature_pubprop_id integer NOT NULL,
    feature_pub_id integer NOT NULL,
    type_id integer NOT NULL,
    value text,
    rank integer DEFAULT 0 NOT NULL
);


ALTER TABLE feature_pubprop OWNER TO nathandunn;

--
-- Name: TABLE feature_pubprop; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE feature_pubprop IS 'Property or attribute of a feature_pub link.';


--
-- Name: feature_pubprop_feature_pubprop_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE feature_pubprop_feature_pubprop_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE feature_pubprop_feature_pubprop_id_seq OWNER TO nathandunn;

--
-- Name: feature_pubprop_feature_pubprop_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE feature_pubprop_feature_pubprop_id_seq OWNED BY feature_pubprop.feature_pubprop_id;


--
-- Name: feature_relationship; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE feature_relationship (
    feature_relationship_id integer NOT NULL,
    subject_id integer NOT NULL,
    object_id integer NOT NULL,
    type_id integer NOT NULL,
    value text,
    rank integer DEFAULT 0 NOT NULL
);


ALTER TABLE feature_relationship OWNER TO nathandunn;

--
-- Name: TABLE feature_relationship; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE feature_relationship IS 'Features can be arranged in
graphs, e.g. "exon part_of transcript part_of gene"; If type is
thought of as a verb, the each arc or edge makes a statement
[Subject Verb Object]. The object can also be thought of as parent
(containing feature), and subject as child (contained feature or
subfeature). We include the relationship rank/order, because even
though most of the time we can order things implicitly by sequence
coordinates, we can not always do this - e.g. transpliced genes. It is also
useful for quickly getting implicit introns.';


--
-- Name: COLUMN feature_relationship.subject_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN feature_relationship.subject_id IS 'The subject of the subj-predicate-obj sentence. This is typically the subfeature.';


--
-- Name: COLUMN feature_relationship.object_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN feature_relationship.object_id IS 'The object of the subj-predicate-obj sentence. This is typically the container feature.';


--
-- Name: COLUMN feature_relationship.type_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN feature_relationship.type_id IS 'Relationship type between subject and object. This is a cvterm, typically from the OBO relationship ontology, although other relationship types are allowed. The most common relationship type is OBO_REL:part_of. Valid relationship types are constrained by the Sequence Ontology.';


--
-- Name: COLUMN feature_relationship.value; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN feature_relationship.value IS 'Additional notes or comments.';


--
-- Name: COLUMN feature_relationship.rank; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN feature_relationship.rank IS 'The ordering of subject features with respect to the object feature may be important (for example, exon ordering on a transcript - not always derivable if you take trans spliced genes into consideration). Rank is used to order these; starts from zero.';


--
-- Name: feature_relationship_feature_relationship_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE feature_relationship_feature_relationship_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE feature_relationship_feature_relationship_id_seq OWNER TO nathandunn;

--
-- Name: feature_relationship_feature_relationship_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE feature_relationship_feature_relationship_id_seq OWNED BY feature_relationship.feature_relationship_id;


--
-- Name: feature_relationship_pub; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE feature_relationship_pub (
    feature_relationship_pub_id integer NOT NULL,
    feature_relationship_id integer NOT NULL,
    pub_id integer NOT NULL
);


ALTER TABLE feature_relationship_pub OWNER TO nathandunn;

--
-- Name: TABLE feature_relationship_pub; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE feature_relationship_pub IS 'Provenance. Attach optional evidence to a feature_relationship in the form of a chado.tion.';


--
-- Name: feature_relationship_pub_feature_relationship_pub_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE feature_relationship_pub_feature_relationship_pub_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE feature_relationship_pub_feature_relationship_pub_id_seq OWNER TO nathandunn;

--
-- Name: feature_relationship_pub_feature_relationship_pub_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE feature_relationship_pub_feature_relationship_pub_id_seq OWNED BY feature_relationship_pub.feature_relationship_pub_id;


--
-- Name: feature_relationshipprop; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE feature_relationshipprop (
    feature_relationshipprop_id integer NOT NULL,
    feature_relationship_id integer NOT NULL,
    type_id integer NOT NULL,
    value text,
    rank integer DEFAULT 0 NOT NULL
);


ALTER TABLE feature_relationshipprop OWNER TO nathandunn;

--
-- Name: TABLE feature_relationshipprop; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE feature_relationshipprop IS 'Extensible properties
for feature_relationships. Analagous structure to featureprop. This
table is largely optional and not used with a high frequency. Typical
scenarios may be if one wishes to attach additional data to a
feature_relationship - for example to say that the
feature_relationship is only true in certain contexts.';


--
-- Name: COLUMN feature_relationshipprop.type_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN feature_relationshipprop.type_id IS 'The name of the
property/slot is a cvterm. The meaning of the property is defined in
that cvterm. Currently there is no standard ontology for
feature_relationship property types.';


--
-- Name: COLUMN feature_relationshipprop.value; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN feature_relationshipprop.value IS 'The value of the
property, represented as text. Numeric values are converted to their
text representation. This is less efficient than using native database
types, but is easier to query.';


--
-- Name: COLUMN feature_relationshipprop.rank; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN feature_relationshipprop.rank IS 'Property-Value
ordering. Any feature_relationship can have multiple values for any particular
property type - these are ordered in a list using rank, counting from
zero. For properties that are single-valued rather than multi-valued,
the default 0 value should be used.';


--
-- Name: feature_relationshipprop_feature_relationshipprop_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE feature_relationshipprop_feature_relationshipprop_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE feature_relationshipprop_feature_relationshipprop_id_seq OWNER TO nathandunn;

--
-- Name: feature_relationshipprop_feature_relationshipprop_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE feature_relationshipprop_feature_relationshipprop_id_seq OWNED BY feature_relationshipprop.feature_relationshipprop_id;


--
-- Name: feature_relationshipprop_pub; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE feature_relationshipprop_pub (
    feature_relationshipprop_pub_id integer NOT NULL,
    feature_relationshipprop_id integer NOT NULL,
    pub_id integer NOT NULL
);


ALTER TABLE feature_relationshipprop_pub OWNER TO nathandunn;

--
-- Name: TABLE feature_relationshipprop_pub; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE feature_relationshipprop_pub IS 'Provenance for feature_relationshipprop.';


--
-- Name: feature_relationshipprop_pub_feature_relationshipprop_pub_i_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE feature_relationshipprop_pub_feature_relationshipprop_pub_i_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE feature_relationshipprop_pub_feature_relationshipprop_pub_i_seq OWNER TO nathandunn;

--
-- Name: feature_relationshipprop_pub_feature_relationshipprop_pub_i_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE feature_relationshipprop_pub_feature_relationshipprop_pub_i_seq OWNED BY feature_relationshipprop_pub.feature_relationshipprop_pub_id;


--
-- Name: feature_synonym_feature_synonym_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE feature_synonym_feature_synonym_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE feature_synonym_feature_synonym_id_seq OWNER TO nathandunn;

--
-- Name: feature_synonym_feature_synonym_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE feature_synonym_feature_synonym_id_seq OWNED BY feature_synonym.feature_synonym_id;


--
-- Name: feature_union; Type: VIEW; Schema: chado; Owner: nathandunn
--

CREATE VIEW feature_union AS
 SELECT x.feature_id AS subject_id,
    y.feature_id AS object_id,
    x.srcfeature_id,
    x.strand AS subject_strand,
    y.strand AS object_strand,
        CASE
            WHEN (x.fmin < y.fmin) THEN x.fmin
            ELSE y.fmin
        END AS fmin,
        CASE
            WHEN (x.fmax > y.fmax) THEN x.fmax
            ELSE y.fmax
        END AS fmax
   FROM featureloc x,
    featureloc y
  WHERE ((x.srcfeature_id = y.srcfeature_id) AND ((x.fmax >= y.fmin) AND (x.fmin <= y.fmax)));


ALTER TABLE feature_union OWNER TO nathandunn;

--
-- Name: VIEW feature_union; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON VIEW feature_union IS 'set-union on interval defined by featureloc. featurelocs must meet';


--
-- Name: feature_uniquename_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE feature_uniquename_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE feature_uniquename_seq OWNER TO nathandunn;

--
-- Name: featureloc_featureloc_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE featureloc_featureloc_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE featureloc_featureloc_id_seq OWNER TO nathandunn;

--
-- Name: featureloc_featureloc_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE featureloc_featureloc_id_seq OWNED BY featureloc.featureloc_id;


--
-- Name: featureloc_pub; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE featureloc_pub (
    featureloc_pub_id integer NOT NULL,
    featureloc_id integer NOT NULL,
    pub_id integer NOT NULL
);


ALTER TABLE featureloc_pub OWNER TO nathandunn;

--
-- Name: TABLE featureloc_pub; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE featureloc_pub IS 'Provenance of featureloc. Linking table between featurelocs and chado.tions that mention them.';


--
-- Name: featureloc_pub_featureloc_pub_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE featureloc_pub_featureloc_pub_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE featureloc_pub_featureloc_pub_id_seq OWNER TO nathandunn;

--
-- Name: featureloc_pub_featureloc_pub_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE featureloc_pub_featureloc_pub_id_seq OWNED BY featureloc_pub.featureloc_pub_id;


--
-- Name: featuremap; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE featuremap (
    featuremap_id integer NOT NULL,
    name character varying(255),
    description text,
    unittype_id integer
);


ALTER TABLE featuremap OWNER TO nathandunn;

--
-- Name: featuremap_featuremap_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE featuremap_featuremap_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE featuremap_featuremap_id_seq OWNER TO nathandunn;

--
-- Name: featuremap_featuremap_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE featuremap_featuremap_id_seq OWNED BY featuremap.featuremap_id;


--
-- Name: featuremap_pub; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE featuremap_pub (
    featuremap_pub_id integer NOT NULL,
    featuremap_id integer NOT NULL,
    pub_id integer NOT NULL
);


ALTER TABLE featuremap_pub OWNER TO nathandunn;

--
-- Name: featuremap_pub_featuremap_pub_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE featuremap_pub_featuremap_pub_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE featuremap_pub_featuremap_pub_id_seq OWNER TO nathandunn;

--
-- Name: featuremap_pub_featuremap_pub_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE featuremap_pub_featuremap_pub_id_seq OWNED BY featuremap_pub.featuremap_pub_id;


--
-- Name: featurepos; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE featurepos (
    featurepos_id integer NOT NULL,
    featuremap_id integer NOT NULL,
    feature_id integer NOT NULL,
    map_feature_id integer NOT NULL,
    mappos double precision NOT NULL
);


ALTER TABLE featurepos OWNER TO nathandunn;

--
-- Name: COLUMN featurepos.map_feature_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN featurepos.map_feature_id IS 'map_feature_id
links to the feature (map) upon which the feature is being localized.';


--
-- Name: featurepos_featuremap_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE featurepos_featuremap_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE featurepos_featuremap_id_seq OWNER TO nathandunn;

--
-- Name: featurepos_featuremap_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE featurepos_featuremap_id_seq OWNED BY featurepos.featuremap_id;


--
-- Name: featurepos_featurepos_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE featurepos_featurepos_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE featurepos_featurepos_id_seq OWNER TO nathandunn;

--
-- Name: featurepos_featurepos_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE featurepos_featurepos_id_seq OWNED BY featurepos.featurepos_id;


--
-- Name: featureprop_featureprop_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE featureprop_featureprop_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE featureprop_featureprop_id_seq OWNER TO nathandunn;

--
-- Name: featureprop_featureprop_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE featureprop_featureprop_id_seq OWNED BY featureprop.featureprop_id;


--
-- Name: featureprop_pub; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE featureprop_pub (
    featureprop_pub_id integer NOT NULL,
    featureprop_id integer NOT NULL,
    pub_id integer NOT NULL
);


ALTER TABLE featureprop_pub OWNER TO nathandunn;

--
-- Name: TABLE featureprop_pub; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE featureprop_pub IS 'Provenance. Any featureprop assignment can optionally be supported by a chado.tion.';


--
-- Name: featureprop_pub_featureprop_pub_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE featureprop_pub_featureprop_pub_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE featureprop_pub_featureprop_pub_id_seq OWNER TO nathandunn;

--
-- Name: featureprop_pub_featureprop_pub_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE featureprop_pub_featureprop_pub_id_seq OWNED BY featureprop_pub.featureprop_pub_id;


--
-- Name: featurerange; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE featurerange (
    featurerange_id integer NOT NULL,
    featuremap_id integer NOT NULL,
    feature_id integer NOT NULL,
    leftstartf_id integer NOT NULL,
    leftendf_id integer,
    rightstartf_id integer,
    rightendf_id integer NOT NULL,
    rangestr character varying(255)
);


ALTER TABLE featurerange OWNER TO nathandunn;

--
-- Name: TABLE featurerange; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE featurerange IS 'In cases where the start and end of a mapped feature is a range, leftendf and rightstartf are populated. leftstartf_id, leftendf_id, rightstartf_id, rightendf_id are the ids of features with respect to which the feature is being mapped. These may be cytological bands.';


--
-- Name: COLUMN featurerange.featuremap_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN featurerange.featuremap_id IS 'featuremap_id is the id of the feature being mapped.';


--
-- Name: featurerange_featurerange_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE featurerange_featurerange_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE featurerange_featurerange_id_seq OWNER TO nathandunn;

--
-- Name: featurerange_featurerange_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE featurerange_featurerange_id_seq OWNED BY featurerange.featurerange_id;


--
-- Name: featureset_meets; Type: VIEW; Schema: chado; Owner: nathandunn
--

CREATE VIEW featureset_meets AS
 SELECT x.object_id AS subject_id,
    y.object_id
   FROM ((feature_meets r
     JOIN feature_relationship x ON ((r.subject_id = x.subject_id)))
     JOIN feature_relationship y ON ((r.object_id = y.subject_id)));


ALTER TABLE featureset_meets OWNER TO nathandunn;

--
-- Name: fnr_type; Type: VIEW; Schema: chado; Owner: nathandunn
--

CREATE VIEW fnr_type AS
 SELECT f.feature_id,
    f.name,
    f.dbxref_id,
    c.name AS type,
    f.residues,
    f.seqlen,
    f.md5checksum,
    f.type_id,
    f.timeaccessioned,
    f.timelastmodified
   FROM (feature f
     LEFT JOIN analysisfeature af ON ((f.feature_id = af.feature_id))),
    cvterm c
  WHERE ((f.type_id = c.cvterm_id) AND (af.feature_id IS NULL));


ALTER TABLE fnr_type OWNER TO nathandunn;

--
-- Name: fp_key; Type: VIEW; Schema: chado; Owner: nathandunn
--

CREATE VIEW fp_key AS
 SELECT fp.feature_id,
    c.name AS pkey,
    fp.value
   FROM featureprop fp,
    cvterm c
  WHERE (fp.featureprop_id = c.cvterm_id);


ALTER TABLE fp_key OWNER TO nathandunn;

--
-- Name: genotype; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE genotype (
    genotype_id integer NOT NULL,
    name text,
    uniquename text NOT NULL,
    description character varying(255),
    type_id integer NOT NULL
);


ALTER TABLE genotype OWNER TO nathandunn;

--
-- Name: TABLE genotype; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE genotype IS 'Genetic context. A genotype is defined by a collection of features, mutations, balancers, deficiencies, haplotype blocks, or engineered constructs.';


--
-- Name: COLUMN genotype.name; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN genotype.name IS 'Optional alternative name for a genotype, 
for display purposes.';


--
-- Name: COLUMN genotype.uniquename; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN genotype.uniquename IS 'The unique name for a genotype; 
typically derived from the features making up the genotype.';


--
-- Name: genotype_genotype_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE genotype_genotype_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE genotype_genotype_id_seq OWNER TO nathandunn;

--
-- Name: genotype_genotype_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE genotype_genotype_id_seq OWNED BY genotype.genotype_id;


--
-- Name: genotypeprop; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE genotypeprop (
    genotypeprop_id integer NOT NULL,
    genotype_id integer NOT NULL,
    type_id integer NOT NULL,
    value text,
    rank integer DEFAULT 0 NOT NULL
);


ALTER TABLE genotypeprop OWNER TO nathandunn;

--
-- Name: genotypeprop_genotypeprop_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE genotypeprop_genotypeprop_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE genotypeprop_genotypeprop_id_seq OWNER TO nathandunn;

--
-- Name: genotypeprop_genotypeprop_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE genotypeprop_genotypeprop_id_seq OWNED BY genotypeprop.genotypeprop_id;


--
-- Name: gff3atts; Type: VIEW; Schema: chado; Owner: nathandunn
--

CREATE VIEW gff3atts AS
 SELECT fs.feature_id,
    'Ontology_term'::text AS type,
        CASE
            WHEN ((db.name)::text ~~ '%Gene Ontology%'::text) THEN (('GO:'::text || (dbx.accession)::text))::character varying
            WHEN ((db.name)::text ~~ 'Sequence Ontology%'::text) THEN (('SO:'::text || (dbx.accession)::text))::character varying
            ELSE ((((db.name)::text || ':'::text) || (dbx.accession)::text))::character varying
        END AS attribute
   FROM cvterm s,
    dbxref dbx,
    feature_cvterm fs,
    db
  WHERE (((fs.cvterm_id = s.cvterm_id) AND (s.dbxref_id = dbx.dbxref_id)) AND (db.db_id = dbx.db_id))
UNION ALL
 SELECT fs.feature_id,
    'Dbxref'::text AS type,
    (((d.name)::text || ':'::text) || (s.accession)::text) AS attribute
   FROM dbxref s,
    feature_dbxref fs,
    db d
  WHERE (((fs.dbxref_id = s.dbxref_id) AND (s.db_id = d.db_id)) AND ((d.name)::text <> 'GFF_source'::text))
UNION ALL
 SELECT f.feature_id,
    'Alias'::text AS type,
    s.name AS attribute
   FROM synonym s,
    feature_synonym fs,
    feature f
  WHERE ((((fs.synonym_id = s.synonym_id) AND (f.feature_id = fs.feature_id)) AND ((f.name)::text <> (s.name)::text)) AND (f.uniquename <> (s.name)::text))
UNION ALL
 SELECT fp.feature_id,
    cv.name AS type,
    fp.value AS attribute
   FROM featureprop fp,
    cvterm cv
  WHERE (fp.type_id = cv.cvterm_id)
UNION ALL
 SELECT fs.feature_id,
    'pub'::text AS type,
    (((s.series_name)::text || ':'::text) || s.title) AS attribute
   FROM pub s,
    feature_pub fs
  WHERE (fs.pub_id = s.pub_id)
UNION ALL
 SELECT fr.subject_id AS feature_id,
    'Parent'::text AS type,
    parent.uniquename AS attribute
   FROM feature_relationship fr,
    feature parent
  WHERE ((fr.object_id = parent.feature_id) AND (fr.type_id = ( SELECT cvterm.cvterm_id
           FROM cvterm
          WHERE (((cvterm.name)::text = 'part_of'::text) AND (cvterm.cv_id IN ( SELECT cv.cv_id
                   FROM cv
                  WHERE ((cv.name)::text = 'relationship'::text)))))))
UNION ALL
 SELECT fr.subject_id AS feature_id,
    'Derives_from'::text AS type,
    parent.uniquename AS attribute
   FROM feature_relationship fr,
    feature parent
  WHERE ((fr.object_id = parent.feature_id) AND (fr.type_id = ( SELECT cvterm.cvterm_id
           FROM cvterm
          WHERE (((cvterm.name)::text = 'derives_from'::text) AND (cvterm.cv_id IN ( SELECT cv.cv_id
                   FROM cv
                  WHERE ((cv.name)::text = 'relationship'::text)))))))
UNION ALL
 SELECT fl.feature_id,
    'Target'::text AS type,
    (((((((target.name)::text || ' '::text) || (fl.fmin + 1)) || ' '::text) || fl.fmax) || ' '::text) || fl.strand) AS attribute
   FROM featureloc fl,
    feature target
  WHERE ((fl.srcfeature_id = target.feature_id) AND (fl.rank <> 0))
UNION ALL
 SELECT feature.feature_id,
    'ID'::text AS type,
    feature.uniquename AS attribute
   FROM feature
  WHERE (NOT (feature.type_id IN ( SELECT cvterm.cvterm_id
           FROM cvterm
          WHERE ((cvterm.name)::text = 'CDS'::text))))
UNION ALL
 SELECT feature.feature_id,
    'chado_feature_id'::text AS type,
    (feature.feature_id)::character varying AS attribute
   FROM feature
UNION ALL
 SELECT feature.feature_id,
    'Name'::text AS type,
    feature.name AS attribute
   FROM feature;


ALTER TABLE gff3atts OWNER TO nathandunn;

--
-- Name: gff3view; Type: VIEW; Schema: chado; Owner: nathandunn
--

CREATE VIEW gff3view AS
 SELECT f.feature_id,
    sf.name AS ref,
    COALESCE(gffdbx.accession, '.'::character varying(255)) AS source,
    cv.name AS type,
    (fl.fmin + 1) AS fstart,
    fl.fmax AS fend,
    COALESCE((af.significance)::text, '.'::text) AS score,
        CASE
            WHEN (fl.strand = (-1)) THEN '-'::text
            WHEN (fl.strand = 1) THEN '+'::text
            ELSE '.'::text
        END AS strand,
    COALESCE((fl.phase)::text, '.'::text) AS phase,
    f.seqlen,
    f.name,
    f.organism_id
   FROM (((((feature f
     LEFT JOIN featureloc fl ON ((f.feature_id = fl.feature_id)))
     LEFT JOIN feature sf ON ((fl.srcfeature_id = sf.feature_id)))
     LEFT JOIN ( SELECT fd.feature_id,
            d.accession
           FROM ((feature_dbxref fd
             JOIN dbxref d USING (dbxref_id))
             JOIN db USING (db_id))
          WHERE ((db.name)::text = 'GFF_source'::text)) gffdbx ON ((f.feature_id = gffdbx.feature_id)))
     LEFT JOIN cvterm cv ON ((f.type_id = cv.cvterm_id)))
     LEFT JOIN analysisfeature af ON ((f.feature_id = af.feature_id)));


ALTER TABLE gff3view OWNER TO nathandunn;

--
-- Name: gff_interval_stats; Type: TABLE; Schema: chado; Owner: ubuntu; Tablespace: 
--

CREATE TABLE gff_interval_stats (
    typeid character varying(1024) NOT NULL,
    srcfeature_id integer NOT NULL,
    bin integer NOT NULL,
    cum_count integer NOT NULL
);


ALTER TABLE gff_interval_stats OWNER TO ubuntu;

--
-- Name: gff_meta; Type: TABLE; Schema: chado; Owner: ubuntu; Tablespace: 
--

CREATE TABLE gff_meta (
    name character varying(100),
    hostname character varying(100),
    starttime timestamp without time zone DEFAULT now() NOT NULL
);


ALTER TABLE gff_meta OWNER TO ubuntu;

--
-- Name: go_count_analysis; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE go_count_analysis (
    cvname character varying(255),
    cvterm_id integer,
    analysis_id integer,
    organism_id integer,
    feature_count integer
);


ALTER TABLE go_count_analysis OWNER TO nathandunn;

--
-- Name: go_count_organism; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE go_count_organism (
    cvname character varying(255),
    cvterm_id integer,
    organism_id integer,
    feature_count integer
);


ALTER TABLE go_count_organism OWNER TO nathandunn;

--
-- Name: intron_combined_view; Type: VIEW; Schema: chado; Owner: nathandunn
--

CREATE VIEW intron_combined_view AS
 SELECT x1.feature_id AS exon1_id,
    x2.feature_id AS exon2_id,
        CASE
            WHEN (l1.strand = (-1)) THEN l2.fmax
            ELSE l1.fmax
        END AS fmin,
        CASE
            WHEN (l1.strand = (-1)) THEN l1.fmin
            ELSE l2.fmin
        END AS fmax,
    l1.strand,
    l1.srcfeature_id,
    r1.rank AS intron_rank,
    r1.object_id AS transcript_id
   FROM ((((((cvterm
     JOIN feature x1 ON ((x1.type_id = cvterm.cvterm_id)))
     JOIN feature_relationship r1 ON ((x1.feature_id = r1.subject_id)))
     JOIN featureloc l1 ON ((x1.feature_id = l1.feature_id)))
     JOIN feature x2 ON ((x2.type_id = cvterm.cvterm_id)))
     JOIN feature_relationship r2 ON ((x2.feature_id = r2.subject_id)))
     JOIN featureloc l2 ON ((x2.feature_id = l2.feature_id)))
  WHERE ((((((((cvterm.name)::text = 'exon'::text) AND ((r2.rank - r1.rank) = 1)) AND (r1.object_id = r2.object_id)) AND (l1.strand = l2.strand)) AND (l1.srcfeature_id = l2.srcfeature_id)) AND (l1.locgroup = 0)) AND (l2.locgroup = 0));


ALTER TABLE intron_combined_view OWNER TO nathandunn;

--
-- Name: intronloc_view; Type: VIEW; Schema: chado; Owner: nathandunn
--

CREATE VIEW intronloc_view AS
 SELECT DISTINCT intron_combined_view.exon1_id,
    intron_combined_view.exon2_id,
    intron_combined_view.fmin,
    intron_combined_view.fmax,
    intron_combined_view.strand,
    intron_combined_view.srcfeature_id
   FROM intron_combined_view;


ALTER TABLE intronloc_view OWNER TO nathandunn;

--
-- Name: kegg_by_organism; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE kegg_by_organism (
    analysis_name character varying(255),
    analysis_id integer,
    organism_id integer
);


ALTER TABLE kegg_by_organism OWNER TO nathandunn;

--
-- Name: library; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE library (
    library_id integer NOT NULL,
    organism_id integer NOT NULL,
    name character varying(255),
    uniquename text NOT NULL,
    type_id integer NOT NULL,
    is_obsolete integer DEFAULT 0 NOT NULL,
    timeaccessioned timestamp without time zone DEFAULT now() NOT NULL,
    timelastmodified timestamp without time zone DEFAULT now() NOT NULL
);


ALTER TABLE library OWNER TO nathandunn;

--
-- Name: COLUMN library.type_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN library.type_id IS 'The type_id foreign key links
to a controlled vocabulary of library types. Examples of this would be: "cDNA_library" or "genomic_library"';


--
-- Name: library_cvterm; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE library_cvterm (
    library_cvterm_id integer NOT NULL,
    library_id integer NOT NULL,
    cvterm_id integer NOT NULL,
    pub_id integer NOT NULL
);


ALTER TABLE library_cvterm OWNER TO nathandunn;

--
-- Name: TABLE library_cvterm; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE library_cvterm IS 'The table library_cvterm links a library to controlled vocabularies which describe the library.  For instance, there might be a link to the anatomy cv for "head" or "testes" for a head or testes library.';


--
-- Name: library_cvterm_library_cvterm_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE library_cvterm_library_cvterm_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE library_cvterm_library_cvterm_id_seq OWNER TO nathandunn;

--
-- Name: library_cvterm_library_cvterm_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE library_cvterm_library_cvterm_id_seq OWNED BY library_cvterm.library_cvterm_id;


--
-- Name: library_dbxref; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE library_dbxref (
    library_dbxref_id integer NOT NULL,
    library_id integer NOT NULL,
    dbxref_id integer NOT NULL,
    is_current boolean DEFAULT true NOT NULL
);


ALTER TABLE library_dbxref OWNER TO nathandunn;

--
-- Name: library_dbxref_library_dbxref_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE library_dbxref_library_dbxref_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE library_dbxref_library_dbxref_id_seq OWNER TO nathandunn;

--
-- Name: library_dbxref_library_dbxref_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE library_dbxref_library_dbxref_id_seq OWNED BY library_dbxref.library_dbxref_id;


--
-- Name: library_feature; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE library_feature (
    library_feature_id integer NOT NULL,
    library_id integer NOT NULL,
    feature_id integer NOT NULL
);


ALTER TABLE library_feature OWNER TO nathandunn;

--
-- Name: TABLE library_feature; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE library_feature IS 'library_feature links a library to the clones which are contained in the library.  Examples of such linked features might be "cDNA_clone" or  "genomic_clone".';


--
-- Name: library_feature_library_feature_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE library_feature_library_feature_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE library_feature_library_feature_id_seq OWNER TO nathandunn;

--
-- Name: library_feature_library_feature_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE library_feature_library_feature_id_seq OWNED BY library_feature.library_feature_id;


--
-- Name: library_library_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE library_library_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE library_library_id_seq OWNER TO nathandunn;

--
-- Name: library_library_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE library_library_id_seq OWNED BY library.library_id;


--
-- Name: library_pub; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE library_pub (
    library_pub_id integer NOT NULL,
    library_id integer NOT NULL,
    pub_id integer NOT NULL
);


ALTER TABLE library_pub OWNER TO nathandunn;

--
-- Name: library_pub_library_pub_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE library_pub_library_pub_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE library_pub_library_pub_id_seq OWNER TO nathandunn;

--
-- Name: library_pub_library_pub_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE library_pub_library_pub_id_seq OWNED BY library_pub.library_pub_id;


--
-- Name: library_synonym; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE library_synonym (
    library_synonym_id integer NOT NULL,
    synonym_id integer NOT NULL,
    library_id integer NOT NULL,
    pub_id integer NOT NULL,
    is_current boolean DEFAULT true NOT NULL,
    is_internal boolean DEFAULT false NOT NULL
);


ALTER TABLE library_synonym OWNER TO nathandunn;

--
-- Name: COLUMN library_synonym.pub_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN library_synonym.pub_id IS 'The pub_id link is for
relating the usage of a given synonym to the publication in which it was used.';


--
-- Name: COLUMN library_synonym.is_current; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN library_synonym.is_current IS 'The is_current bit indicates whether the linked synonym is the current -official- symbol for the linked library.';


--
-- Name: COLUMN library_synonym.is_internal; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN library_synonym.is_internal IS 'Typically a synonym
exists so that somebody querying the database with an obsolete name
can find the object they are looking for under its current name.  If
the synonym has been used publicly and deliberately (e.g. in a paper), it my also be listed in reports as a synonym.   If the synonym was not used deliberately (e.g., there was a typo which went public), then the is_internal bit may be set to "true" so that it is known that the synonym is "internal" and should be queryable but should not be listed in reports as a valid synonym.';


--
-- Name: library_synonym_library_synonym_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE library_synonym_library_synonym_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE library_synonym_library_synonym_id_seq OWNER TO nathandunn;

--
-- Name: library_synonym_library_synonym_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE library_synonym_library_synonym_id_seq OWNED BY library_synonym.library_synonym_id;


--
-- Name: libraryprop; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE libraryprop (
    libraryprop_id integer NOT NULL,
    library_id integer NOT NULL,
    type_id integer NOT NULL,
    value text,
    rank integer DEFAULT 0 NOT NULL
);


ALTER TABLE libraryprop OWNER TO nathandunn;

--
-- Name: libraryprop_libraryprop_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE libraryprop_libraryprop_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE libraryprop_libraryprop_id_seq OWNER TO nathandunn;

--
-- Name: libraryprop_libraryprop_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE libraryprop_libraryprop_id_seq OWNED BY libraryprop.libraryprop_id;


--
-- Name: libraryprop_pub; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE libraryprop_pub (
    libraryprop_pub_id integer NOT NULL,
    libraryprop_id integer NOT NULL,
    pub_id integer NOT NULL
);


ALTER TABLE libraryprop_pub OWNER TO nathandunn;

--
-- Name: libraryprop_pub_libraryprop_pub_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE libraryprop_pub_libraryprop_pub_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE libraryprop_pub_libraryprop_pub_id_seq OWNER TO nathandunn;

--
-- Name: libraryprop_pub_libraryprop_pub_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE libraryprop_pub_libraryprop_pub_id_seq OWNED BY libraryprop_pub.libraryprop_pub_id;


--
-- Name: magedocumentation; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE magedocumentation (
    magedocumentation_id integer NOT NULL,
    mageml_id integer NOT NULL,
    tableinfo_id integer NOT NULL,
    row_id integer NOT NULL,
    mageidentifier text NOT NULL
);


ALTER TABLE magedocumentation OWNER TO nathandunn;

--
-- Name: magedocumentation_magedocumentation_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE magedocumentation_magedocumentation_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE magedocumentation_magedocumentation_id_seq OWNER TO nathandunn;

--
-- Name: magedocumentation_magedocumentation_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE magedocumentation_magedocumentation_id_seq OWNED BY magedocumentation.magedocumentation_id;


--
-- Name: mageml; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE mageml (
    mageml_id integer NOT NULL,
    mage_package text NOT NULL,
    mage_ml text NOT NULL
);


ALTER TABLE mageml OWNER TO nathandunn;

--
-- Name: TABLE mageml; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE mageml IS 'This table is for storing extra bits of MAGEml in a denormalized form. More normalization would require many more tables.';


--
-- Name: mageml_mageml_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE mageml_mageml_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE mageml_mageml_id_seq OWNER TO nathandunn;

--
-- Name: mageml_mageml_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE mageml_mageml_id_seq OWNED BY mageml.mageml_id;


--
-- Name: materialized_view; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE materialized_view (
    materialized_view_id integer NOT NULL,
    last_update timestamp without time zone,
    refresh_time integer,
    name character varying(64),
    mv_schema character varying(64),
    mv_table character varying(128),
    mv_specs text,
    indexed text,
    query text,
    special_index text
);


ALTER TABLE materialized_view OWNER TO nathandunn;

--
-- Name: materialized_view_materialized_view_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE materialized_view_materialized_view_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE materialized_view_materialized_view_id_seq OWNER TO nathandunn;

--
-- Name: materialized_view_materialized_view_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE materialized_view_materialized_view_id_seq OWNED BY materialized_view.materialized_view_id;


--
-- Name: nd_experiment; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE nd_experiment (
    nd_experiment_id integer NOT NULL,
    nd_geolocation_id integer NOT NULL,
    type_id integer NOT NULL
);


ALTER TABLE nd_experiment OWNER TO nathandunn;

--
-- Name: nd_experiment_contact; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE nd_experiment_contact (
    nd_experiment_contact_id integer NOT NULL,
    nd_experiment_id integer NOT NULL,
    contact_id integer NOT NULL
);


ALTER TABLE nd_experiment_contact OWNER TO nathandunn;

--
-- Name: nd_experiment_contact_nd_experiment_contact_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE nd_experiment_contact_nd_experiment_contact_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nd_experiment_contact_nd_experiment_contact_id_seq OWNER TO nathandunn;

--
-- Name: nd_experiment_contact_nd_experiment_contact_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE nd_experiment_contact_nd_experiment_contact_id_seq OWNED BY nd_experiment_contact.nd_experiment_contact_id;


--
-- Name: nd_experiment_dbxref; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE nd_experiment_dbxref (
    nd_experiment_dbxref_id integer NOT NULL,
    nd_experiment_id integer NOT NULL,
    dbxref_id integer NOT NULL
);


ALTER TABLE nd_experiment_dbxref OWNER TO nathandunn;

--
-- Name: TABLE nd_experiment_dbxref; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE nd_experiment_dbxref IS 'Cross-reference experiment to accessions, images, etc';


--
-- Name: nd_experiment_dbxref_nd_experiment_dbxref_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE nd_experiment_dbxref_nd_experiment_dbxref_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nd_experiment_dbxref_nd_experiment_dbxref_id_seq OWNER TO nathandunn;

--
-- Name: nd_experiment_dbxref_nd_experiment_dbxref_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE nd_experiment_dbxref_nd_experiment_dbxref_id_seq OWNED BY nd_experiment_dbxref.nd_experiment_dbxref_id;


--
-- Name: nd_experiment_genotype; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE nd_experiment_genotype (
    nd_experiment_genotype_id integer NOT NULL,
    nd_experiment_id integer NOT NULL,
    genotype_id integer NOT NULL
);


ALTER TABLE nd_experiment_genotype OWNER TO nathandunn;

--
-- Name: TABLE nd_experiment_genotype; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE nd_experiment_genotype IS 'Linking table: experiments to the genotypes they produce. There is a one-to-one relationship between an experiment and a genotype since each genotype record should point to one experiment. Add a new experiment_id for each genotype record.';


--
-- Name: nd_experiment_genotype_nd_experiment_genotype_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE nd_experiment_genotype_nd_experiment_genotype_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nd_experiment_genotype_nd_experiment_genotype_id_seq OWNER TO nathandunn;

--
-- Name: nd_experiment_genotype_nd_experiment_genotype_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE nd_experiment_genotype_nd_experiment_genotype_id_seq OWNED BY nd_experiment_genotype.nd_experiment_genotype_id;


--
-- Name: nd_experiment_nd_experiment_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE nd_experiment_nd_experiment_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nd_experiment_nd_experiment_id_seq OWNER TO nathandunn;

--
-- Name: nd_experiment_nd_experiment_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE nd_experiment_nd_experiment_id_seq OWNED BY nd_experiment.nd_experiment_id;


--
-- Name: nd_experiment_phenotype; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE nd_experiment_phenotype (
    nd_experiment_phenotype_id integer NOT NULL,
    nd_experiment_id integer NOT NULL,
    phenotype_id integer NOT NULL
);


ALTER TABLE nd_experiment_phenotype OWNER TO nathandunn;

--
-- Name: TABLE nd_experiment_phenotype; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE nd_experiment_phenotype IS 'Linking table: experiments to the phenotypes they produce. There is a one-to-one relationship between an experiment and a phenotype since each phenotype record should point to one experiment. Add a new experiment_id for each phenotype record.';


--
-- Name: nd_experiment_phenotype_nd_experiment_phenotype_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE nd_experiment_phenotype_nd_experiment_phenotype_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nd_experiment_phenotype_nd_experiment_phenotype_id_seq OWNER TO nathandunn;

--
-- Name: nd_experiment_phenotype_nd_experiment_phenotype_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE nd_experiment_phenotype_nd_experiment_phenotype_id_seq OWNED BY nd_experiment_phenotype.nd_experiment_phenotype_id;


--
-- Name: nd_experiment_project; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE nd_experiment_project (
    nd_experiment_project_id integer NOT NULL,
    project_id integer NOT NULL,
    nd_experiment_id integer NOT NULL
);


ALTER TABLE nd_experiment_project OWNER TO nathandunn;

--
-- Name: nd_experiment_project_nd_experiment_project_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE nd_experiment_project_nd_experiment_project_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nd_experiment_project_nd_experiment_project_id_seq OWNER TO nathandunn;

--
-- Name: nd_experiment_project_nd_experiment_project_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE nd_experiment_project_nd_experiment_project_id_seq OWNED BY nd_experiment_project.nd_experiment_project_id;


--
-- Name: nd_experiment_protocol; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE nd_experiment_protocol (
    nd_experiment_protocol_id integer NOT NULL,
    nd_experiment_id integer NOT NULL,
    nd_protocol_id integer NOT NULL
);


ALTER TABLE nd_experiment_protocol OWNER TO nathandunn;

--
-- Name: TABLE nd_experiment_protocol; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE nd_experiment_protocol IS 'Linking table: experiments to the protocols they involve.';


--
-- Name: nd_experiment_protocol_nd_experiment_protocol_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE nd_experiment_protocol_nd_experiment_protocol_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nd_experiment_protocol_nd_experiment_protocol_id_seq OWNER TO nathandunn;

--
-- Name: nd_experiment_protocol_nd_experiment_protocol_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE nd_experiment_protocol_nd_experiment_protocol_id_seq OWNED BY nd_experiment_protocol.nd_experiment_protocol_id;


--
-- Name: nd_experiment_pub; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE nd_experiment_pub (
    nd_experiment_pub_id integer NOT NULL,
    nd_experiment_id integer NOT NULL,
    pub_id integer NOT NULL
);


ALTER TABLE nd_experiment_pub OWNER TO nathandunn;

--
-- Name: TABLE nd_experiment_pub; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE nd_experiment_pub IS 'Linking nd_experiment(s) to chado.tion(s)';


--
-- Name: nd_experiment_pub_nd_experiment_pub_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE nd_experiment_pub_nd_experiment_pub_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nd_experiment_pub_nd_experiment_pub_id_seq OWNER TO nathandunn;

--
-- Name: nd_experiment_pub_nd_experiment_pub_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE nd_experiment_pub_nd_experiment_pub_id_seq OWNED BY nd_experiment_pub.nd_experiment_pub_id;


--
-- Name: nd_experiment_stock; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE nd_experiment_stock (
    nd_experiment_stock_id integer NOT NULL,
    nd_experiment_id integer NOT NULL,
    stock_id integer NOT NULL,
    type_id integer NOT NULL
);


ALTER TABLE nd_experiment_stock OWNER TO nathandunn;

--
-- Name: TABLE nd_experiment_stock; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE nd_experiment_stock IS 'Part of a stock or a clone of a stock that is used in an experiment';


--
-- Name: COLUMN nd_experiment_stock.stock_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN nd_experiment_stock.stock_id IS 'stock used in the extraction or the corresponding stock for the clone';


--
-- Name: nd_experiment_stock_dbxref; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE nd_experiment_stock_dbxref (
    nd_experiment_stock_dbxref_id integer NOT NULL,
    nd_experiment_stock_id integer NOT NULL,
    dbxref_id integer NOT NULL
);


ALTER TABLE nd_experiment_stock_dbxref OWNER TO nathandunn;

--
-- Name: TABLE nd_experiment_stock_dbxref; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE nd_experiment_stock_dbxref IS 'Cross-reference experiment_stock to accessions, images, etc';


--
-- Name: nd_experiment_stock_dbxref_nd_experiment_stock_dbxref_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE nd_experiment_stock_dbxref_nd_experiment_stock_dbxref_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nd_experiment_stock_dbxref_nd_experiment_stock_dbxref_id_seq OWNER TO nathandunn;

--
-- Name: nd_experiment_stock_dbxref_nd_experiment_stock_dbxref_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE nd_experiment_stock_dbxref_nd_experiment_stock_dbxref_id_seq OWNED BY nd_experiment_stock_dbxref.nd_experiment_stock_dbxref_id;


--
-- Name: nd_experiment_stock_nd_experiment_stock_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE nd_experiment_stock_nd_experiment_stock_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nd_experiment_stock_nd_experiment_stock_id_seq OWNER TO nathandunn;

--
-- Name: nd_experiment_stock_nd_experiment_stock_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE nd_experiment_stock_nd_experiment_stock_id_seq OWNED BY nd_experiment_stock.nd_experiment_stock_id;


--
-- Name: nd_experiment_stockprop; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE nd_experiment_stockprop (
    nd_experiment_stockprop_id integer NOT NULL,
    nd_experiment_stock_id integer NOT NULL,
    type_id integer NOT NULL,
    value text,
    rank integer DEFAULT 0 NOT NULL
);


ALTER TABLE nd_experiment_stockprop OWNER TO nathandunn;

--
-- Name: TABLE nd_experiment_stockprop; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE nd_experiment_stockprop IS 'Property/value associations for experiment_stocks. This table can store the properties such as treatment';


--
-- Name: COLUMN nd_experiment_stockprop.nd_experiment_stock_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN nd_experiment_stockprop.nd_experiment_stock_id IS 'The experiment_stock to which the property applies.';


--
-- Name: COLUMN nd_experiment_stockprop.type_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN nd_experiment_stockprop.type_id IS 'The name of the property as a reference to a controlled vocabulary term.';


--
-- Name: COLUMN nd_experiment_stockprop.value; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN nd_experiment_stockprop.value IS 'The value of the property.';


--
-- Name: COLUMN nd_experiment_stockprop.rank; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN nd_experiment_stockprop.rank IS 'The rank of the property value, if the property has an array of values.';


--
-- Name: nd_experiment_stockprop_nd_experiment_stockprop_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE nd_experiment_stockprop_nd_experiment_stockprop_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nd_experiment_stockprop_nd_experiment_stockprop_id_seq OWNER TO nathandunn;

--
-- Name: nd_experiment_stockprop_nd_experiment_stockprop_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE nd_experiment_stockprop_nd_experiment_stockprop_id_seq OWNED BY nd_experiment_stockprop.nd_experiment_stockprop_id;


--
-- Name: nd_experimentprop; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE nd_experimentprop (
    nd_experimentprop_id integer NOT NULL,
    nd_experiment_id integer NOT NULL,
    type_id integer NOT NULL,
    value text,
    rank integer DEFAULT 0 NOT NULL
);


ALTER TABLE nd_experimentprop OWNER TO nathandunn;

--
-- Name: nd_experimentprop_nd_experimentprop_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE nd_experimentprop_nd_experimentprop_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nd_experimentprop_nd_experimentprop_id_seq OWNER TO nathandunn;

--
-- Name: nd_experimentprop_nd_experimentprop_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE nd_experimentprop_nd_experimentprop_id_seq OWNED BY nd_experimentprop.nd_experimentprop_id;


--
-- Name: nd_geolocation; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE nd_geolocation (
    nd_geolocation_id integer NOT NULL,
    description character varying(255),
    latitude real,
    longitude real,
    geodetic_datum character varying(32),
    altitude real
);


ALTER TABLE nd_geolocation OWNER TO nathandunn;

--
-- Name: TABLE nd_geolocation; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE nd_geolocation IS 'The geo-referencable location of the stock. NOTE: This entity is subject to change as a more general and possibly more OpenGIS-compliant geolocation module may be introduced into Chado.';


--
-- Name: COLUMN nd_geolocation.description; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN nd_geolocation.description IS 'A textual representation of the location, if this is the original georeference. Optional if the original georeference is available in lat/long coordinates.';


--
-- Name: COLUMN nd_geolocation.latitude; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN nd_geolocation.latitude IS 'The decimal latitude coordinate of the georeference, using positive and negative sign to indicate N and S, respectively.';


--
-- Name: COLUMN nd_geolocation.longitude; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN nd_geolocation.longitude IS 'The decimal longitude coordinate of the georeference, using positive and negative sign to indicate E and W, respectively.';


--
-- Name: COLUMN nd_geolocation.geodetic_datum; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN nd_geolocation.geodetic_datum IS 'The geodetic system on which the geo-reference coordinates are based. For geo-references measured between 1984 and 2010, this will typically be WGS84.';


--
-- Name: COLUMN nd_geolocation.altitude; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN nd_geolocation.altitude IS 'The altitude (elevation) of the location in meters. If the altitude is only known as a range, this is the average, and altitude_dev will hold half of the width of the range.';


--
-- Name: nd_geolocation_nd_geolocation_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE nd_geolocation_nd_geolocation_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nd_geolocation_nd_geolocation_id_seq OWNER TO nathandunn;

--
-- Name: nd_geolocation_nd_geolocation_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE nd_geolocation_nd_geolocation_id_seq OWNED BY nd_geolocation.nd_geolocation_id;


--
-- Name: nd_geolocationprop; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE nd_geolocationprop (
    nd_geolocationprop_id integer NOT NULL,
    nd_geolocation_id integer NOT NULL,
    type_id integer NOT NULL,
    value text,
    rank integer DEFAULT 0 NOT NULL
);


ALTER TABLE nd_geolocationprop OWNER TO nathandunn;

--
-- Name: TABLE nd_geolocationprop; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE nd_geolocationprop IS 'Property/value associations for geolocations. This table can store the properties such as location and environment';


--
-- Name: COLUMN nd_geolocationprop.type_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN nd_geolocationprop.type_id IS 'The name of the property as a reference to a controlled vocabulary term.';


--
-- Name: COLUMN nd_geolocationprop.value; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN nd_geolocationprop.value IS 'The value of the property.';


--
-- Name: COLUMN nd_geolocationprop.rank; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN nd_geolocationprop.rank IS 'The rank of the property value, if the property has an array of values.';


--
-- Name: nd_geolocationprop_nd_geolocationprop_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE nd_geolocationprop_nd_geolocationprop_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nd_geolocationprop_nd_geolocationprop_id_seq OWNER TO nathandunn;

--
-- Name: nd_geolocationprop_nd_geolocationprop_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE nd_geolocationprop_nd_geolocationprop_id_seq OWNED BY nd_geolocationprop.nd_geolocationprop_id;


--
-- Name: nd_protocol; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE nd_protocol (
    nd_protocol_id integer NOT NULL,
    name character varying(255) NOT NULL,
    type_id integer NOT NULL
);


ALTER TABLE nd_protocol OWNER TO nathandunn;

--
-- Name: TABLE nd_protocol; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE nd_protocol IS 'A protocol can be anything that is done as part of the experiment.';


--
-- Name: COLUMN nd_protocol.name; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN nd_protocol.name IS 'The protocol name.';


--
-- Name: nd_protocol_nd_protocol_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE nd_protocol_nd_protocol_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nd_protocol_nd_protocol_id_seq OWNER TO nathandunn;

--
-- Name: nd_protocol_nd_protocol_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE nd_protocol_nd_protocol_id_seq OWNED BY nd_protocol.nd_protocol_id;


--
-- Name: nd_protocol_reagent; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE nd_protocol_reagent (
    nd_protocol_reagent_id integer NOT NULL,
    nd_protocol_id integer NOT NULL,
    reagent_id integer NOT NULL,
    type_id integer NOT NULL
);


ALTER TABLE nd_protocol_reagent OWNER TO nathandunn;

--
-- Name: nd_protocol_reagent_nd_protocol_reagent_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE nd_protocol_reagent_nd_protocol_reagent_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nd_protocol_reagent_nd_protocol_reagent_id_seq OWNER TO nathandunn;

--
-- Name: nd_protocol_reagent_nd_protocol_reagent_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE nd_protocol_reagent_nd_protocol_reagent_id_seq OWNED BY nd_protocol_reagent.nd_protocol_reagent_id;


--
-- Name: nd_protocolprop; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE nd_protocolprop (
    nd_protocolprop_id integer NOT NULL,
    nd_protocol_id integer NOT NULL,
    type_id integer NOT NULL,
    value text,
    rank integer DEFAULT 0 NOT NULL
);


ALTER TABLE nd_protocolprop OWNER TO nathandunn;

--
-- Name: TABLE nd_protocolprop; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE nd_protocolprop IS 'Property/value associations for protocol.';


--
-- Name: COLUMN nd_protocolprop.nd_protocol_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN nd_protocolprop.nd_protocol_id IS 'The protocol to which the property applies.';


--
-- Name: COLUMN nd_protocolprop.type_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN nd_protocolprop.type_id IS 'The name of the property as a reference to a controlled vocabulary term.';


--
-- Name: COLUMN nd_protocolprop.value; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN nd_protocolprop.value IS 'The value of the property.';


--
-- Name: COLUMN nd_protocolprop.rank; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN nd_protocolprop.rank IS 'The rank of the property value, if the property has an array of values.';


--
-- Name: nd_protocolprop_nd_protocolprop_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE nd_protocolprop_nd_protocolprop_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nd_protocolprop_nd_protocolprop_id_seq OWNER TO nathandunn;

--
-- Name: nd_protocolprop_nd_protocolprop_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE nd_protocolprop_nd_protocolprop_id_seq OWNED BY nd_protocolprop.nd_protocolprop_id;


--
-- Name: nd_reagent; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE nd_reagent (
    nd_reagent_id integer NOT NULL,
    name character varying(80) NOT NULL,
    type_id integer NOT NULL,
    feature_id integer
);


ALTER TABLE nd_reagent OWNER TO nathandunn;

--
-- Name: TABLE nd_reagent; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE nd_reagent IS 'A reagent such as a primer, an enzyme, an adapter oligo, a linker oligo. Reagents are used in genotyping experiments, or in any other kind of experiment.';


--
-- Name: COLUMN nd_reagent.name; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN nd_reagent.name IS 'The name of the reagent. The name should be unique for a given type.';


--
-- Name: COLUMN nd_reagent.type_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN nd_reagent.type_id IS 'The type of the reagent, for example linker oligomer, or forward primer.';


--
-- Name: COLUMN nd_reagent.feature_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN nd_reagent.feature_id IS 'If the reagent is a primer, the feature that it corresponds to. More generally, the corresponding feature for any reagent that has a sequence that maps to another sequence.';


--
-- Name: nd_reagent_nd_reagent_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE nd_reagent_nd_reagent_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nd_reagent_nd_reagent_id_seq OWNER TO nathandunn;

--
-- Name: nd_reagent_nd_reagent_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE nd_reagent_nd_reagent_id_seq OWNED BY nd_reagent.nd_reagent_id;


--
-- Name: nd_reagent_relationship; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE nd_reagent_relationship (
    nd_reagent_relationship_id integer NOT NULL,
    subject_reagent_id integer NOT NULL,
    object_reagent_id integer NOT NULL,
    type_id integer NOT NULL
);


ALTER TABLE nd_reagent_relationship OWNER TO nathandunn;

--
-- Name: TABLE nd_reagent_relationship; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE nd_reagent_relationship IS 'Relationships between reagents. Some reagents form a group. i.e., they are used all together or not at all. Examples are adapter/linker/enzyme experiment reagents.';


--
-- Name: COLUMN nd_reagent_relationship.subject_reagent_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN nd_reagent_relationship.subject_reagent_id IS 'The subject reagent in the relationship. In parent/child terminology, the subject is the child. For example, in "linkerA 3prime-overhang-linker enzymeA" linkerA is the subject, 3prime-overhand-linker is the type, and enzymeA is the object.';


--
-- Name: COLUMN nd_reagent_relationship.object_reagent_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN nd_reagent_relationship.object_reagent_id IS 'The object reagent in the relationship. In parent/child terminology, the object is the parent. For example, in "linkerA 3prime-overhang-linker enzymeA" linkerA is the subject, 3prime-overhand-linker is the type, and enzymeA is the object.';


--
-- Name: COLUMN nd_reagent_relationship.type_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN nd_reagent_relationship.type_id IS 'The type (or predicate) of the relationship. For example, in "linkerA 3prime-overhang-linker enzymeA" linkerA is the subject, 3prime-overhand-linker is the type, and enzymeA is the object.';


--
-- Name: nd_reagent_relationship_nd_reagent_relationship_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE nd_reagent_relationship_nd_reagent_relationship_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nd_reagent_relationship_nd_reagent_relationship_id_seq OWNER TO nathandunn;

--
-- Name: nd_reagent_relationship_nd_reagent_relationship_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE nd_reagent_relationship_nd_reagent_relationship_id_seq OWNED BY nd_reagent_relationship.nd_reagent_relationship_id;


--
-- Name: nd_reagentprop; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE nd_reagentprop (
    nd_reagentprop_id integer NOT NULL,
    nd_reagent_id integer NOT NULL,
    type_id integer NOT NULL,
    value text,
    rank integer DEFAULT 0 NOT NULL
);


ALTER TABLE nd_reagentprop OWNER TO nathandunn;

--
-- Name: nd_reagentprop_nd_reagentprop_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE nd_reagentprop_nd_reagentprop_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nd_reagentprop_nd_reagentprop_id_seq OWNER TO nathandunn;

--
-- Name: nd_reagentprop_nd_reagentprop_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE nd_reagentprop_nd_reagentprop_id_seq OWNED BY nd_reagentprop.nd_reagentprop_id;


--
-- Name: organism; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE organism (
    organism_id integer NOT NULL,
    abbreviation character varying(255),
    genus character varying(255) NOT NULL,
    species character varying(255) NOT NULL,
    common_name character varying(255),
    comment text
);


ALTER TABLE organism OWNER TO nathandunn;

--
-- Name: TABLE organism; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE organism IS 'The organismal taxonomic
classification. Note that phylogenies are represented using the
phylogeny module, and taxonomies can be represented using the cvterm
module or the phylogeny module.';


--
-- Name: COLUMN organism.species; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN organism.species IS 'A type of organism is always
uniquely identified by genus and species. When mapping from the NCBI
taxonomy names.dmp file, this column must be used where it
is present, as the common_name column is not always unique (e.g. environmental
samples). If a particular strain or subspecies is to be represented,
this is appended onto the species name. Follows standard NCBI taxonomy
pattern.';


--
-- Name: organism_dbxref; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE organism_dbxref (
    organism_dbxref_id integer NOT NULL,
    organism_id integer NOT NULL,
    dbxref_id integer NOT NULL
);


ALTER TABLE organism_dbxref OWNER TO nathandunn;

--
-- Name: organism_dbxref_organism_dbxref_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE organism_dbxref_organism_dbxref_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE organism_dbxref_organism_dbxref_id_seq OWNER TO nathandunn;

--
-- Name: organism_dbxref_organism_dbxref_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE organism_dbxref_organism_dbxref_id_seq OWNED BY organism_dbxref.organism_dbxref_id;


--
-- Name: organism_feature_count; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE organism_feature_count (
    organism_id integer,
    genus character varying(255),
    species character varying(255),
    common_name character varying(255),
    num_features integer,
    cvterm_id integer,
    feature_type character varying(255)
);


ALTER TABLE organism_feature_count OWNER TO nathandunn;

--
-- Name: organism_organism_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE organism_organism_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE organism_organism_id_seq OWNER TO nathandunn;

--
-- Name: organism_organism_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE organism_organism_id_seq OWNED BY organism.organism_id;


--
-- Name: organismprop; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE organismprop (
    organismprop_id integer NOT NULL,
    organism_id integer NOT NULL,
    type_id integer NOT NULL,
    value text,
    rank integer DEFAULT 0 NOT NULL
);


ALTER TABLE organismprop OWNER TO nathandunn;

--
-- Name: TABLE organismprop; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE organismprop IS 'Tag-value properties - follows standard chado model.';


--
-- Name: organismprop_organismprop_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE organismprop_organismprop_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE organismprop_organismprop_id_seq OWNER TO nathandunn;

--
-- Name: organismprop_organismprop_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE organismprop_organismprop_id_seq OWNED BY organismprop.organismprop_id;


--
-- Name: phendesc; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE phendesc (
    phendesc_id integer NOT NULL,
    genotype_id integer NOT NULL,
    environment_id integer NOT NULL,
    description text NOT NULL,
    type_id integer NOT NULL,
    pub_id integer NOT NULL
);


ALTER TABLE phendesc OWNER TO nathandunn;

--
-- Name: TABLE phendesc; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE phendesc IS 'A summary of a _set_ of phenotypic statements for any one gcontext made in any one chado.tion.';


--
-- Name: phendesc_phendesc_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE phendesc_phendesc_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE phendesc_phendesc_id_seq OWNER TO nathandunn;

--
-- Name: phendesc_phendesc_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE phendesc_phendesc_id_seq OWNED BY phendesc.phendesc_id;


--
-- Name: phenotype; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE phenotype (
    phenotype_id integer NOT NULL,
    uniquename text NOT NULL,
    name text,
    observable_id integer,
    attr_id integer,
    value text,
    cvalue_id integer,
    assay_id integer
);


ALTER TABLE phenotype OWNER TO nathandunn;

--
-- Name: TABLE phenotype; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE phenotype IS 'A phenotypic statement, or a single
atomic phenotypic observation, is a controlled sentence describing
observable effects of non-wild type function. E.g. Obs=eye, attribute=color, cvalue=red.';


--
-- Name: COLUMN phenotype.observable_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN phenotype.observable_id IS 'The entity: e.g. anatomy_part, biological_process.';


--
-- Name: COLUMN phenotype.attr_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN phenotype.attr_id IS 'Phenotypic attribute (quality, property, attribute, character) - drawn from PATO.';


--
-- Name: COLUMN phenotype.value; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN phenotype.value IS 'Value of attribute - unconstrained free text. Used only if cvalue_id is not appropriate.';


--
-- Name: COLUMN phenotype.cvalue_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN phenotype.cvalue_id IS 'Phenotype attribute value (state).';


--
-- Name: COLUMN phenotype.assay_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN phenotype.assay_id IS 'Evidence type.';


--
-- Name: phenotype_comparison; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE phenotype_comparison (
    phenotype_comparison_id integer NOT NULL,
    genotype1_id integer NOT NULL,
    environment1_id integer NOT NULL,
    genotype2_id integer NOT NULL,
    environment2_id integer NOT NULL,
    phenotype1_id integer NOT NULL,
    phenotype2_id integer,
    pub_id integer NOT NULL,
    organism_id integer NOT NULL
);


ALTER TABLE phenotype_comparison OWNER TO nathandunn;

--
-- Name: TABLE phenotype_comparison; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE phenotype_comparison IS 'Comparison of phenotypes e.g., genotype1/environment1/phenotype1 "non-suppressible" with respect to genotype2/environment2/phenotype2.';


--
-- Name: phenotype_comparison_cvterm; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE phenotype_comparison_cvterm (
    phenotype_comparison_cvterm_id integer NOT NULL,
    phenotype_comparison_id integer NOT NULL,
    cvterm_id integer NOT NULL,
    pub_id integer NOT NULL,
    rank integer DEFAULT 0 NOT NULL
);


ALTER TABLE phenotype_comparison_cvterm OWNER TO nathandunn;

--
-- Name: phenotype_comparison_cvterm_phenotype_comparison_cvterm_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE phenotype_comparison_cvterm_phenotype_comparison_cvterm_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE phenotype_comparison_cvterm_phenotype_comparison_cvterm_id_seq OWNER TO nathandunn;

--
-- Name: phenotype_comparison_cvterm_phenotype_comparison_cvterm_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE phenotype_comparison_cvterm_phenotype_comparison_cvterm_id_seq OWNED BY phenotype_comparison_cvterm.phenotype_comparison_cvterm_id;


--
-- Name: phenotype_comparison_phenotype_comparison_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE phenotype_comparison_phenotype_comparison_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE phenotype_comparison_phenotype_comparison_id_seq OWNER TO nathandunn;

--
-- Name: phenotype_comparison_phenotype_comparison_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE phenotype_comparison_phenotype_comparison_id_seq OWNED BY phenotype_comparison.phenotype_comparison_id;


--
-- Name: phenotype_cvterm; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE phenotype_cvterm (
    phenotype_cvterm_id integer NOT NULL,
    phenotype_id integer NOT NULL,
    cvterm_id integer NOT NULL,
    rank integer DEFAULT 0 NOT NULL
);


ALTER TABLE phenotype_cvterm OWNER TO nathandunn;

--
-- Name: phenotype_cvterm_phenotype_cvterm_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE phenotype_cvterm_phenotype_cvterm_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE phenotype_cvterm_phenotype_cvterm_id_seq OWNER TO nathandunn;

--
-- Name: phenotype_cvterm_phenotype_cvterm_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE phenotype_cvterm_phenotype_cvterm_id_seq OWNED BY phenotype_cvterm.phenotype_cvterm_id;


--
-- Name: phenotype_phenotype_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE phenotype_phenotype_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE phenotype_phenotype_id_seq OWNER TO nathandunn;

--
-- Name: phenotype_phenotype_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE phenotype_phenotype_id_seq OWNED BY phenotype.phenotype_id;


--
-- Name: phenstatement; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE phenstatement (
    phenstatement_id integer NOT NULL,
    genotype_id integer NOT NULL,
    environment_id integer NOT NULL,
    phenotype_id integer NOT NULL,
    type_id integer NOT NULL,
    pub_id integer NOT NULL
);


ALTER TABLE phenstatement OWNER TO nathandunn;

--
-- Name: TABLE phenstatement; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE phenstatement IS 'Phenotypes are things like "larval lethal".  Phenstatements are things like "dpp-1 is recessive larval lethal". So essentially phenstatement is a linking table expressing the relationship between genotype, environment, and phenotype.';


--
-- Name: phenstatement_phenstatement_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE phenstatement_phenstatement_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE phenstatement_phenstatement_id_seq OWNER TO nathandunn;

--
-- Name: phenstatement_phenstatement_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE phenstatement_phenstatement_id_seq OWNED BY phenstatement.phenstatement_id;


--
-- Name: phylonode; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE phylonode (
    phylonode_id integer NOT NULL,
    phylotree_id integer NOT NULL,
    parent_phylonode_id integer,
    left_idx integer NOT NULL,
    right_idx integer NOT NULL,
    type_id integer,
    feature_id integer,
    label character varying(255),
    distance double precision
);


ALTER TABLE phylonode OWNER TO nathandunn;

--
-- Name: TABLE phylonode; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE phylonode IS 'This is the most pervasive
       element in the phylogeny module, cataloging the "phylonodes" of
       tree graphs. Edges are implied by the parent_phylonode_id
       reflexive closure. For all nodes in a nested set implementation the left and right index will be *between* the parents left and right indexes.';


--
-- Name: COLUMN phylonode.parent_phylonode_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN phylonode.parent_phylonode_id IS 'Root phylonode can have null parent_phylonode_id value.';


--
-- Name: COLUMN phylonode.type_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN phylonode.type_id IS 'Type: e.g. root, interior, leaf.';


--
-- Name: COLUMN phylonode.feature_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN phylonode.feature_id IS 'Phylonodes can have optional features attached to them e.g. a protein or nucleotide sequence usually attached to a leaf of the phylotree for non-leaf nodes, the feature may be a feature that is an instance of SO:match; this feature is the alignment of all leaf features beneath it.';


--
-- Name: phylonode_dbxref; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE phylonode_dbxref (
    phylonode_dbxref_id integer NOT NULL,
    phylonode_id integer NOT NULL,
    dbxref_id integer NOT NULL
);


ALTER TABLE phylonode_dbxref OWNER TO nathandunn;

--
-- Name: TABLE phylonode_dbxref; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE phylonode_dbxref IS 'For example, for orthology, paralogy group identifiers; could also be used for NCBI taxonomy; for sequences, refer to phylonode_feature, feature associated dbxrefs.';


--
-- Name: phylonode_dbxref_phylonode_dbxref_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE phylonode_dbxref_phylonode_dbxref_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE phylonode_dbxref_phylonode_dbxref_id_seq OWNER TO nathandunn;

--
-- Name: phylonode_dbxref_phylonode_dbxref_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE phylonode_dbxref_phylonode_dbxref_id_seq OWNED BY phylonode_dbxref.phylonode_dbxref_id;


--
-- Name: phylonode_organism; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE phylonode_organism (
    phylonode_organism_id integer NOT NULL,
    phylonode_id integer NOT NULL,
    organism_id integer NOT NULL
);


ALTER TABLE phylonode_organism OWNER TO nathandunn;

--
-- Name: TABLE phylonode_organism; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE phylonode_organism IS 'This linking table should only be used for nodes in taxonomy trees; it provides a mapping between the node and an organism. One node can have zero or one organisms, one organism can have zero or more nodes (although typically it should only have one in the standard NCBI taxonomy tree).';


--
-- Name: COLUMN phylonode_organism.phylonode_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN phylonode_organism.phylonode_id IS 'One phylonode cannot refer to >1 organism.';


--
-- Name: phylonode_organism_phylonode_organism_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE phylonode_organism_phylonode_organism_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE phylonode_organism_phylonode_organism_id_seq OWNER TO nathandunn;

--
-- Name: phylonode_organism_phylonode_organism_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE phylonode_organism_phylonode_organism_id_seq OWNED BY phylonode_organism.phylonode_organism_id;


--
-- Name: phylonode_phylonode_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE phylonode_phylonode_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE phylonode_phylonode_id_seq OWNER TO nathandunn;

--
-- Name: phylonode_phylonode_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE phylonode_phylonode_id_seq OWNED BY phylonode.phylonode_id;


--
-- Name: phylonode_pub; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE phylonode_pub (
    phylonode_pub_id integer NOT NULL,
    phylonode_id integer NOT NULL,
    pub_id integer NOT NULL
);


ALTER TABLE phylonode_pub OWNER TO nathandunn;

--
-- Name: phylonode_pub_phylonode_pub_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE phylonode_pub_phylonode_pub_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE phylonode_pub_phylonode_pub_id_seq OWNER TO nathandunn;

--
-- Name: phylonode_pub_phylonode_pub_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE phylonode_pub_phylonode_pub_id_seq OWNED BY phylonode_pub.phylonode_pub_id;


--
-- Name: phylonode_relationship; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE phylonode_relationship (
    phylonode_relationship_id integer NOT NULL,
    subject_id integer NOT NULL,
    object_id integer NOT NULL,
    type_id integer NOT NULL,
    rank integer,
    phylotree_id integer NOT NULL
);


ALTER TABLE phylonode_relationship OWNER TO nathandunn;

--
-- Name: TABLE phylonode_relationship; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE phylonode_relationship IS 'This is for 
relationships that are not strictly hierarchical; for example,
horizontal gene transfer. Most phylogenetic trees are strictly
hierarchical, nevertheless it is here for completeness.';


--
-- Name: phylonode_relationship_phylonode_relationship_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE phylonode_relationship_phylonode_relationship_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE phylonode_relationship_phylonode_relationship_id_seq OWNER TO nathandunn;

--
-- Name: phylonode_relationship_phylonode_relationship_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE phylonode_relationship_phylonode_relationship_id_seq OWNED BY phylonode_relationship.phylonode_relationship_id;


--
-- Name: phylonodeprop; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE phylonodeprop (
    phylonodeprop_id integer NOT NULL,
    phylonode_id integer NOT NULL,
    type_id integer NOT NULL,
    value text DEFAULT ''::text NOT NULL,
    rank integer DEFAULT 0 NOT NULL
);


ALTER TABLE phylonodeprop OWNER TO nathandunn;

--
-- Name: COLUMN phylonodeprop.type_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN phylonodeprop.type_id IS 'type_id could designate phylonode hierarchy relationships, for example: species taxonomy (kingdom, order, family, genus, species), "ortholog/paralog", "fold/superfold", etc.';


--
-- Name: phylonodeprop_phylonodeprop_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE phylonodeprop_phylonodeprop_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE phylonodeprop_phylonodeprop_id_seq OWNER TO nathandunn;

--
-- Name: phylonodeprop_phylonodeprop_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE phylonodeprop_phylonodeprop_id_seq OWNED BY phylonodeprop.phylonodeprop_id;


--
-- Name: phylotree; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE phylotree (
    phylotree_id integer NOT NULL,
    dbxref_id integer NOT NULL,
    name character varying(255),
    type_id integer,
    analysis_id integer,
    comment text
);


ALTER TABLE phylotree OWNER TO nathandunn;

--
-- Name: TABLE phylotree; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE phylotree IS 'Global anchor for phylogenetic tree.';


--
-- Name: COLUMN phylotree.type_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN phylotree.type_id IS 'Type: protein, nucleotide, taxonomy, for example. The type should be any SO type, or "taxonomy".';


--
-- Name: phylotree_phylotree_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE phylotree_phylotree_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE phylotree_phylotree_id_seq OWNER TO nathandunn;

--
-- Name: phylotree_phylotree_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE phylotree_phylotree_id_seq OWNED BY phylotree.phylotree_id;


--
-- Name: phylotree_pub; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE phylotree_pub (
    phylotree_pub_id integer NOT NULL,
    phylotree_id integer NOT NULL,
    pub_id integer NOT NULL
);


ALTER TABLE phylotree_pub OWNER TO nathandunn;

--
-- Name: TABLE phylotree_pub; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE phylotree_pub IS 'Tracks citations global to the tree e.g. multiple sequence alignment supporting tree construction.';


--
-- Name: phylotree_pub_phylotree_pub_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE phylotree_pub_phylotree_pub_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE phylotree_pub_phylotree_pub_id_seq OWNER TO nathandunn;

--
-- Name: phylotree_pub_phylotree_pub_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE phylotree_pub_phylotree_pub_id_seq OWNED BY phylotree_pub.phylotree_pub_id;


--
-- Name: project; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE project (
    project_id integer NOT NULL,
    name character varying(255) NOT NULL,
    description character varying(255) NOT NULL
);


ALTER TABLE project OWNER TO nathandunn;

--
-- Name: project_contact; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE project_contact (
    project_contact_id integer NOT NULL,
    project_id integer NOT NULL,
    contact_id integer NOT NULL
);


ALTER TABLE project_contact OWNER TO nathandunn;

--
-- Name: TABLE project_contact; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE project_contact IS 'Linking project(s) to contact(s)';


--
-- Name: project_contact_project_contact_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE project_contact_project_contact_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE project_contact_project_contact_id_seq OWNER TO nathandunn;

--
-- Name: project_contact_project_contact_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE project_contact_project_contact_id_seq OWNED BY project_contact.project_contact_id;


--
-- Name: project_project_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE project_project_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE project_project_id_seq OWNER TO nathandunn;

--
-- Name: project_project_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE project_project_id_seq OWNED BY project.project_id;


--
-- Name: project_pub; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE project_pub (
    project_pub_id integer NOT NULL,
    project_id integer NOT NULL,
    pub_id integer NOT NULL
);


ALTER TABLE project_pub OWNER TO nathandunn;

--
-- Name: TABLE project_pub; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE project_pub IS 'Linking project(s) to chado.tion(s)';


--
-- Name: project_pub_project_pub_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE project_pub_project_pub_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE project_pub_project_pub_id_seq OWNER TO nathandunn;

--
-- Name: project_pub_project_pub_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE project_pub_project_pub_id_seq OWNED BY project_pub.project_pub_id;


--
-- Name: project_relationship; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE project_relationship (
    project_relationship_id integer NOT NULL,
    subject_project_id integer NOT NULL,
    object_project_id integer NOT NULL,
    type_id integer NOT NULL
);


ALTER TABLE project_relationship OWNER TO nathandunn;

--
-- Name: TABLE project_relationship; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE project_relationship IS 'A project can be composed of several smaller scale projects';


--
-- Name: COLUMN project_relationship.type_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN project_relationship.type_id IS 'The type of relationship being stated, such as "is part of".';


--
-- Name: project_relationship_project_relationship_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE project_relationship_project_relationship_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE project_relationship_project_relationship_id_seq OWNER TO nathandunn;

--
-- Name: project_relationship_project_relationship_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE project_relationship_project_relationship_id_seq OWNED BY project_relationship.project_relationship_id;


--
-- Name: projectprop; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE projectprop (
    projectprop_id integer NOT NULL,
    project_id integer NOT NULL,
    type_id integer NOT NULL,
    value text,
    rank integer DEFAULT 0 NOT NULL
);


ALTER TABLE projectprop OWNER TO nathandunn;

--
-- Name: projectprop_projectprop_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE projectprop_projectprop_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE projectprop_projectprop_id_seq OWNER TO nathandunn;

--
-- Name: projectprop_projectprop_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE projectprop_projectprop_id_seq OWNED BY projectprop.projectprop_id;


--
-- Name: protocol; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE protocol (
    protocol_id integer NOT NULL,
    type_id integer NOT NULL,
    pub_id integer,
    dbxref_id integer,
    name text NOT NULL,
    uri text,
    protocoldescription text,
    hardwaredescription text,
    softwaredescription text
);


ALTER TABLE protocol OWNER TO nathandunn;

--
-- Name: TABLE protocol; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE protocol IS 'Procedural notes on how data was prepared and processed.';


--
-- Name: protocol_protocol_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE protocol_protocol_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE protocol_protocol_id_seq OWNER TO nathandunn;

--
-- Name: protocol_protocol_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE protocol_protocol_id_seq OWNED BY protocol.protocol_id;


--
-- Name: protocolparam; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE protocolparam (
    protocolparam_id integer NOT NULL,
    protocol_id integer NOT NULL,
    name text NOT NULL,
    datatype_id integer,
    unittype_id integer,
    value text,
    rank integer DEFAULT 0 NOT NULL
);


ALTER TABLE protocolparam OWNER TO nathandunn;

--
-- Name: TABLE protocolparam; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE protocolparam IS 'Parameters related to a
protocol. For example, if the protocol is a soak, this might include attributes of bath temperature and duration.';


--
-- Name: protocolparam_protocolparam_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE protocolparam_protocolparam_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE protocolparam_protocolparam_id_seq OWNER TO nathandunn;

--
-- Name: protocolparam_protocolparam_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE protocolparam_protocolparam_id_seq OWNED BY protocolparam.protocolparam_id;


--
-- Name: pub_dbxref; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE pub_dbxref (
    pub_dbxref_id integer NOT NULL,
    pub_id integer NOT NULL,
    dbxref_id integer NOT NULL,
    is_current boolean DEFAULT true NOT NULL
);


ALTER TABLE pub_dbxref OWNER TO nathandunn;

--
-- Name: TABLE pub_dbxref; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE pub_dbxref IS 'Handle links to repositories,
e.g. Pubmed, Biosis, zoorec, OCLC, Medline, ISSN, coden...';


--
-- Name: pub_dbxref_pub_dbxref_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE pub_dbxref_pub_dbxref_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE pub_dbxref_pub_dbxref_id_seq OWNER TO nathandunn;

--
-- Name: pub_dbxref_pub_dbxref_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE pub_dbxref_pub_dbxref_id_seq OWNED BY pub_dbxref.pub_dbxref_id;


--
-- Name: pub_pub_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE pub_pub_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE pub_pub_id_seq OWNER TO nathandunn;

--
-- Name: pub_pub_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE pub_pub_id_seq OWNED BY pub.pub_id;


--
-- Name: pub_relationship; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE pub_relationship (
    pub_relationship_id integer NOT NULL,
    subject_id integer NOT NULL,
    object_id integer NOT NULL,
    type_id integer NOT NULL
);


ALTER TABLE pub_relationship OWNER TO nathandunn;

--
-- Name: TABLE pub_relationship; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE pub_relationship IS 'Handle relationships between
publications, e.g. when one publication makes others obsolete, when one
publication contains errata with respect to other publication(s), or
when one publication also appears in another pub.';


--
-- Name: pub_relationship_pub_relationship_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE pub_relationship_pub_relationship_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE pub_relationship_pub_relationship_id_seq OWNER TO nathandunn;

--
-- Name: pub_relationship_pub_relationship_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE pub_relationship_pub_relationship_id_seq OWNED BY pub_relationship.pub_relationship_id;


--
-- Name: pubauthor; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE pubauthor (
    pubauthor_id integer NOT NULL,
    pub_id integer NOT NULL,
    rank integer NOT NULL,
    editor boolean DEFAULT false,
    surname character varying(100) NOT NULL,
    givennames character varying(100),
    suffix character varying(100)
);


ALTER TABLE pubauthor OWNER TO nathandunn;

--
-- Name: TABLE pubauthor; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE pubauthor IS 'An author for a chado.tion. Note the denormalisation (hence lack of _ in table name) - this is deliberate as it is in general too hard to assign IDs to authors.';


--
-- Name: COLUMN pubauthor.rank; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN pubauthor.rank IS 'Order of author in author list for this pub - order is important.';


--
-- Name: COLUMN pubauthor.editor; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN pubauthor.editor IS 'Indicates whether the author is an editor for linked chado.tion. Note: this is a boolean field but does not follow the normal chado convention for naming booleans.';


--
-- Name: COLUMN pubauthor.givennames; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN pubauthor.givennames IS 'First name, initials';


--
-- Name: COLUMN pubauthor.suffix; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN pubauthor.suffix IS 'Jr., Sr., etc';


--
-- Name: pubauthor_pubauthor_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE pubauthor_pubauthor_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE pubauthor_pubauthor_id_seq OWNER TO nathandunn;

--
-- Name: pubauthor_pubauthor_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE pubauthor_pubauthor_id_seq OWNED BY pubauthor.pubauthor_id;


--
-- Name: pubprop; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE pubprop (
    pubprop_id integer NOT NULL,
    pub_id integer NOT NULL,
    type_id integer NOT NULL,
    value text NOT NULL,
    rank integer
);


ALTER TABLE pubprop OWNER TO nathandunn;

--
-- Name: TABLE pubprop; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE pubprop IS 'Property-value pairs for a pub. Follows standard chado pattern.';


--
-- Name: pubprop_pubprop_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE pubprop_pubprop_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE pubprop_pubprop_id_seq OWNER TO nathandunn;

--
-- Name: pubprop_pubprop_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE pubprop_pubprop_id_seq OWNED BY pubprop.pubprop_id;


--
-- Name: quantification; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE quantification (
    quantification_id integer NOT NULL,
    acquisition_id integer NOT NULL,
    operator_id integer,
    protocol_id integer,
    analysis_id integer NOT NULL,
    quantificationdate timestamp without time zone DEFAULT now(),
    name text,
    uri text
);


ALTER TABLE quantification OWNER TO nathandunn;

--
-- Name: TABLE quantification; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE quantification IS 'Quantification is the transformation of an image acquisition to numeric data. This typically involves statistical procedures.';


--
-- Name: quantification_quantification_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE quantification_quantification_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE quantification_quantification_id_seq OWNER TO nathandunn;

--
-- Name: quantification_quantification_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE quantification_quantification_id_seq OWNED BY quantification.quantification_id;


--
-- Name: quantification_relationship; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE quantification_relationship (
    quantification_relationship_id integer NOT NULL,
    subject_id integer NOT NULL,
    type_id integer NOT NULL,
    object_id integer NOT NULL
);


ALTER TABLE quantification_relationship OWNER TO nathandunn;

--
-- Name: TABLE quantification_relationship; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE quantification_relationship IS 'There may be multiple rounds of quantification, this allows us to keep an audit trail of what values went where.';


--
-- Name: quantification_relationship_quantification_relationship_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE quantification_relationship_quantification_relationship_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE quantification_relationship_quantification_relationship_id_seq OWNER TO nathandunn;

--
-- Name: quantification_relationship_quantification_relationship_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE quantification_relationship_quantification_relationship_id_seq OWNED BY quantification_relationship.quantification_relationship_id;


--
-- Name: quantificationprop; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE quantificationprop (
    quantificationprop_id integer NOT NULL,
    quantification_id integer NOT NULL,
    type_id integer NOT NULL,
    value text,
    rank integer DEFAULT 0 NOT NULL
);


ALTER TABLE quantificationprop OWNER TO nathandunn;

--
-- Name: TABLE quantificationprop; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE quantificationprop IS 'Extra quantification properties that are not accounted for in quantification.';


--
-- Name: quantificationprop_quantificationprop_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE quantificationprop_quantificationprop_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE quantificationprop_quantificationprop_id_seq OWNER TO nathandunn;

--
-- Name: quantificationprop_quantificationprop_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE quantificationprop_quantificationprop_id_seq OWNED BY quantificationprop.quantificationprop_id;


--
-- Name: stats_paths_to_root; Type: VIEW; Schema: chado; Owner: nathandunn
--

CREATE VIEW stats_paths_to_root AS
 SELECT cvtermpath.subject_id AS cvterm_id,
    count(DISTINCT cvtermpath.cvtermpath_id) AS total_paths,
    avg(cvtermpath.pathdistance) AS avg_distance,
    min(cvtermpath.pathdistance) AS min_distance,
    max(cvtermpath.pathdistance) AS max_distance
   FROM (cvtermpath
     JOIN cv_root ON ((cvtermpath.object_id = cv_root.root_cvterm_id)))
  GROUP BY cvtermpath.subject_id;


ALTER TABLE stats_paths_to_root OWNER TO nathandunn;

--
-- Name: VIEW stats_paths_to_root; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON VIEW stats_paths_to_root IS 'per-cvterm statistics on its
placement in the DAG relative to the root. There may be multiple paths
from any term to the root. This gives the total number of paths, and
the average minimum and maximum distances. Here distance is defined by
cvtermpath.pathdistance';


--
-- Name: stock; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE stock (
    stock_id integer NOT NULL,
    dbxref_id integer,
    organism_id integer,
    name character varying(255),
    uniquename text NOT NULL,
    description text,
    type_id integer NOT NULL,
    is_obsolete boolean DEFAULT false NOT NULL
);


ALTER TABLE stock OWNER TO nathandunn;

--
-- Name: TABLE stock; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE stock IS 'Any stock can be globally identified by the
combination of organism, uniquename and stock type. A stock is the physical entities, either living or preserved, held by collections. Stocks belong to a collection; they have IDs, type, organism, description and may have a genotype.';


--
-- Name: COLUMN stock.dbxref_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN stock.dbxref_id IS 'The dbxref_id is an optional primary stable identifier for this stock. Secondary indentifiers and external dbxrefs go in table: stock_dbxref.';


--
-- Name: COLUMN stock.organism_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN stock.organism_id IS 'The organism_id is the organism to which the stock belongs. This column should only be left blank if the organism cannot be determined.';


--
-- Name: COLUMN stock.name; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN stock.name IS 'The name is a human-readable local name for a stock.';


--
-- Name: COLUMN stock.description; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN stock.description IS 'The description is the genetic description provided in the stock list.';


--
-- Name: COLUMN stock.type_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN stock.type_id IS 'The type_id foreign key links to a controlled vocabulary of stock types. The would include living stock, genomic DNA, preserved specimen. Secondary cvterms for stocks would go in stock_cvterm.';


--
-- Name: stock_cvterm; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE stock_cvterm (
    stock_cvterm_id integer NOT NULL,
    stock_id integer NOT NULL,
    cvterm_id integer NOT NULL,
    pub_id integer NOT NULL,
    is_not boolean DEFAULT false NOT NULL,
    rank integer DEFAULT 0 NOT NULL
);


ALTER TABLE stock_cvterm OWNER TO nathandunn;

--
-- Name: TABLE stock_cvterm; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE stock_cvterm IS 'stock_cvterm links a stock to cvterms. This is for secondary cvterms; primary cvterms should use stock.type_id.';


--
-- Name: stock_cvterm_stock_cvterm_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE stock_cvterm_stock_cvterm_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE stock_cvterm_stock_cvterm_id_seq OWNER TO nathandunn;

--
-- Name: stock_cvterm_stock_cvterm_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE stock_cvterm_stock_cvterm_id_seq OWNED BY stock_cvterm.stock_cvterm_id;


--
-- Name: stock_cvtermprop; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE stock_cvtermprop (
    stock_cvtermprop_id integer NOT NULL,
    stock_cvterm_id integer NOT NULL,
    type_id integer NOT NULL,
    value text,
    rank integer DEFAULT 0 NOT NULL
);


ALTER TABLE stock_cvtermprop OWNER TO nathandunn;

--
-- Name: TABLE stock_cvtermprop; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE stock_cvtermprop IS 'Extensible properties for
stock to cvterm associations. Examples: GO evidence codes;
qualifiers; metadata such as the date on which the entry was curated
and the source of the association. See the stockprop table for
meanings of type_id, value and rank.';


--
-- Name: COLUMN stock_cvtermprop.type_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN stock_cvtermprop.type_id IS 'The name of the
property/slot is a cvterm. The meaning of the property is defined in
that cvterm. cvterms may come from the OBO evidence code cv.';


--
-- Name: COLUMN stock_cvtermprop.value; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN stock_cvtermprop.value IS 'The value of the
property, represented as text. Numeric values are converted to their
text representation. This is less efficient than using native database
types, but is easier to query.';


--
-- Name: COLUMN stock_cvtermprop.rank; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN stock_cvtermprop.rank IS 'Property-Value
ordering. Any stock_cvterm can have multiple values for any particular
property type - these are ordered in a list using rank, counting from
zero. For properties that are single-valued rather than multi-valued,
the default 0 value should be used.';


--
-- Name: stock_cvtermprop_stock_cvtermprop_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE stock_cvtermprop_stock_cvtermprop_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE stock_cvtermprop_stock_cvtermprop_id_seq OWNER TO nathandunn;

--
-- Name: stock_cvtermprop_stock_cvtermprop_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE stock_cvtermprop_stock_cvtermprop_id_seq OWNED BY stock_cvtermprop.stock_cvtermprop_id;


--
-- Name: stock_dbxref; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE stock_dbxref (
    stock_dbxref_id integer NOT NULL,
    stock_id integer NOT NULL,
    dbxref_id integer NOT NULL,
    is_current boolean DEFAULT true NOT NULL
);


ALTER TABLE stock_dbxref OWNER TO nathandunn;

--
-- Name: TABLE stock_dbxref; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE stock_dbxref IS 'stock_dbxref links a stock to dbxrefs. This is for secondary identifiers; primary identifiers should use stock.dbxref_id.';


--
-- Name: COLUMN stock_dbxref.is_current; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN stock_dbxref.is_current IS 'The is_current boolean indicates whether the linked dbxref is the current -official- dbxref for the linked stock.';


--
-- Name: stock_dbxref_stock_dbxref_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE stock_dbxref_stock_dbxref_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE stock_dbxref_stock_dbxref_id_seq OWNER TO nathandunn;

--
-- Name: stock_dbxref_stock_dbxref_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE stock_dbxref_stock_dbxref_id_seq OWNED BY stock_dbxref.stock_dbxref_id;


--
-- Name: stock_dbxrefprop; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE stock_dbxrefprop (
    stock_dbxrefprop_id integer NOT NULL,
    stock_dbxref_id integer NOT NULL,
    type_id integer NOT NULL,
    value text,
    rank integer DEFAULT 0 NOT NULL
);


ALTER TABLE stock_dbxrefprop OWNER TO nathandunn;

--
-- Name: TABLE stock_dbxrefprop; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE stock_dbxrefprop IS 'A stock_dbxref can have any number of
slot-value property tags attached to it. This is useful for storing properties related to dbxref annotations of stocks, such as evidence codes, and references, and metadata, such as create/modify dates. This is an alternative to
hardcoding a list of columns in the relational schema, and is
completely extensible. There is a unique constraint, stock_dbxrefprop_c1, for
the combination of stock_dbxref_id, rank, and type_id. Multivalued property-value pairs must be differentiated by rank.';


--
-- Name: stock_dbxrefprop_stock_dbxrefprop_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE stock_dbxrefprop_stock_dbxrefprop_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE stock_dbxrefprop_stock_dbxrefprop_id_seq OWNER TO nathandunn;

--
-- Name: stock_dbxrefprop_stock_dbxrefprop_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE stock_dbxrefprop_stock_dbxrefprop_id_seq OWNED BY stock_dbxrefprop.stock_dbxrefprop_id;


--
-- Name: stock_genotype; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE stock_genotype (
    stock_genotype_id integer NOT NULL,
    stock_id integer NOT NULL,
    genotype_id integer NOT NULL
);


ALTER TABLE stock_genotype OWNER TO nathandunn;

--
-- Name: TABLE stock_genotype; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE stock_genotype IS 'Simple table linking a stock to
a genotype. Features with genotypes can be linked to stocks thru feature_genotype -> genotype -> stock_genotype -> stock.';


--
-- Name: stock_genotype_stock_genotype_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE stock_genotype_stock_genotype_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE stock_genotype_stock_genotype_id_seq OWNER TO nathandunn;

--
-- Name: stock_genotype_stock_genotype_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE stock_genotype_stock_genotype_id_seq OWNED BY stock_genotype.stock_genotype_id;


--
-- Name: stock_pub; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE stock_pub (
    stock_pub_id integer NOT NULL,
    stock_id integer NOT NULL,
    pub_id integer NOT NULL
);


ALTER TABLE stock_pub OWNER TO nathandunn;

--
-- Name: TABLE stock_pub; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE stock_pub IS 'Provenance. Linking table between stocks and, for example, a stocklist computer file.';


--
-- Name: stock_pub_stock_pub_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE stock_pub_stock_pub_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE stock_pub_stock_pub_id_seq OWNER TO nathandunn;

--
-- Name: stock_pub_stock_pub_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE stock_pub_stock_pub_id_seq OWNED BY stock_pub.stock_pub_id;


--
-- Name: stock_relationship; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE stock_relationship (
    stock_relationship_id integer NOT NULL,
    subject_id integer NOT NULL,
    object_id integer NOT NULL,
    type_id integer NOT NULL,
    value text,
    rank integer DEFAULT 0 NOT NULL
);


ALTER TABLE stock_relationship OWNER TO nathandunn;

--
-- Name: COLUMN stock_relationship.subject_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN stock_relationship.subject_id IS 'stock_relationship.subject_id is the subject of the subj-predicate-obj sentence. This is typically the substock.';


--
-- Name: COLUMN stock_relationship.object_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN stock_relationship.object_id IS 'stock_relationship.object_id is the object of the subj-predicate-obj sentence. This is typically the container stock.';


--
-- Name: COLUMN stock_relationship.type_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN stock_relationship.type_id IS 'stock_relationship.type_id is relationship type between subject and object. This is a cvterm, typically from the OBO relationship ontology, although other relationship types are allowed.';


--
-- Name: COLUMN stock_relationship.value; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN stock_relationship.value IS 'stock_relationship.value is for additional notes or comments.';


--
-- Name: COLUMN stock_relationship.rank; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN stock_relationship.rank IS 'stock_relationship.rank is the ordering of subject stocks with respect to the object stock may be important where rank is used to order these; starts from zero.';


--
-- Name: stock_relationship_cvterm; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE stock_relationship_cvterm (
    stock_relationship_cvterm_id integer NOT NULL,
    stock_relationship_id integer NOT NULL,
    cvterm_id integer NOT NULL,
    pub_id integer
);


ALTER TABLE stock_relationship_cvterm OWNER TO nathandunn;

--
-- Name: TABLE stock_relationship_cvterm; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE stock_relationship_cvterm IS 'For germplasm maintenance and pedigree data, stock_relationship. type_id will record cvterms such as "is a female parent of", "a parent for mutation", "is a group_id of", "is a source_id of", etc The cvterms for higher categories such as "generative", "derivative" or "maintenance" can be stored in table stock_relationship_cvterm';


--
-- Name: stock_relationship_cvterm_stock_relationship_cvterm_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE stock_relationship_cvterm_stock_relationship_cvterm_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE stock_relationship_cvterm_stock_relationship_cvterm_id_seq OWNER TO nathandunn;

--
-- Name: stock_relationship_cvterm_stock_relationship_cvterm_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE stock_relationship_cvterm_stock_relationship_cvterm_id_seq OWNED BY stock_relationship_cvterm.stock_relationship_cvterm_id;


--
-- Name: stock_relationship_pub; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE stock_relationship_pub (
    stock_relationship_pub_id integer NOT NULL,
    stock_relationship_id integer NOT NULL,
    pub_id integer NOT NULL
);


ALTER TABLE stock_relationship_pub OWNER TO nathandunn;

--
-- Name: TABLE stock_relationship_pub; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE stock_relationship_pub IS 'Provenance. Attach optional evidence to a stock_relationship in the form of a chado.tion.';


--
-- Name: stock_relationship_pub_stock_relationship_pub_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE stock_relationship_pub_stock_relationship_pub_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE stock_relationship_pub_stock_relationship_pub_id_seq OWNER TO nathandunn;

--
-- Name: stock_relationship_pub_stock_relationship_pub_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE stock_relationship_pub_stock_relationship_pub_id_seq OWNED BY stock_relationship_pub.stock_relationship_pub_id;


--
-- Name: stock_relationship_stock_relationship_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE stock_relationship_stock_relationship_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE stock_relationship_stock_relationship_id_seq OWNER TO nathandunn;

--
-- Name: stock_relationship_stock_relationship_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE stock_relationship_stock_relationship_id_seq OWNED BY stock_relationship.stock_relationship_id;


--
-- Name: stock_stock_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE stock_stock_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE stock_stock_id_seq OWNER TO nathandunn;

--
-- Name: stock_stock_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE stock_stock_id_seq OWNED BY stock.stock_id;


--
-- Name: stockcollection; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE stockcollection (
    stockcollection_id integer NOT NULL,
    type_id integer NOT NULL,
    contact_id integer,
    name character varying(255),
    uniquename text NOT NULL
);


ALTER TABLE stockcollection OWNER TO nathandunn;

--
-- Name: TABLE stockcollection; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE stockcollection IS 'The lab or stock center distributing the stocks in their collection.';


--
-- Name: COLUMN stockcollection.type_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN stockcollection.type_id IS 'type_id is the collection type cv.';


--
-- Name: COLUMN stockcollection.contact_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN stockcollection.contact_id IS 'contact_id links to the contact information for the collection.';


--
-- Name: COLUMN stockcollection.name; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN stockcollection.name IS 'name is the collection.';


--
-- Name: COLUMN stockcollection.uniquename; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN stockcollection.uniquename IS 'uniqename is the value of the collection cv.';


--
-- Name: stockcollection_stock; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE stockcollection_stock (
    stockcollection_stock_id integer NOT NULL,
    stockcollection_id integer NOT NULL,
    stock_id integer NOT NULL
);


ALTER TABLE stockcollection_stock OWNER TO nathandunn;

--
-- Name: TABLE stockcollection_stock; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE stockcollection_stock IS 'stockcollection_stock links
a stock collection to the stocks which are contained in the collection.';


--
-- Name: stockcollection_stock_stockcollection_stock_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE stockcollection_stock_stockcollection_stock_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE stockcollection_stock_stockcollection_stock_id_seq OWNER TO nathandunn;

--
-- Name: stockcollection_stock_stockcollection_stock_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE stockcollection_stock_stockcollection_stock_id_seq OWNED BY stockcollection_stock.stockcollection_stock_id;


--
-- Name: stockcollection_stockcollection_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE stockcollection_stockcollection_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE stockcollection_stockcollection_id_seq OWNER TO nathandunn;

--
-- Name: stockcollection_stockcollection_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE stockcollection_stockcollection_id_seq OWNED BY stockcollection.stockcollection_id;


--
-- Name: stockcollectionprop; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE stockcollectionprop (
    stockcollectionprop_id integer NOT NULL,
    stockcollection_id integer NOT NULL,
    type_id integer NOT NULL,
    value text,
    rank integer DEFAULT 0 NOT NULL
);


ALTER TABLE stockcollectionprop OWNER TO nathandunn;

--
-- Name: TABLE stockcollectionprop; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE stockcollectionprop IS 'The table stockcollectionprop
contains the value of the stock collection such as website/email URLs;
the value of the stock collection order URLs.';


--
-- Name: COLUMN stockcollectionprop.type_id; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON COLUMN stockcollectionprop.type_id IS 'The cv for the type_id is "stockcollection property type".';


--
-- Name: stockcollectionprop_stockcollectionprop_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE stockcollectionprop_stockcollectionprop_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE stockcollectionprop_stockcollectionprop_id_seq OWNER TO nathandunn;

--
-- Name: stockcollectionprop_stockcollectionprop_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE stockcollectionprop_stockcollectionprop_id_seq OWNED BY stockcollectionprop.stockcollectionprop_id;


--
-- Name: stockprop; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE stockprop (
    stockprop_id integer NOT NULL,
    stock_id integer NOT NULL,
    type_id integer NOT NULL,
    value text,
    rank integer DEFAULT 0 NOT NULL
);


ALTER TABLE stockprop OWNER TO nathandunn;

--
-- Name: TABLE stockprop; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE stockprop IS 'A stock can have any number of
slot-value property tags attached to it. This is an alternative to
hardcoding a list of columns in the relational schema, and is
completely extensible. There is a unique constraint, stockprop_c1, for
the combination of stock_id, rank, and type_id. Multivalued property-value pairs must be differentiated by rank.';


--
-- Name: stockprop_pub; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE stockprop_pub (
    stockprop_pub_id integer NOT NULL,
    stockprop_id integer NOT NULL,
    pub_id integer NOT NULL
);


ALTER TABLE stockprop_pub OWNER TO nathandunn;

--
-- Name: TABLE stockprop_pub; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE stockprop_pub IS 'Provenance. Any stockprop assignment can optionally be supported by a chado.tion.';


--
-- Name: stockprop_pub_stockprop_pub_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE stockprop_pub_stockprop_pub_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE stockprop_pub_stockprop_pub_id_seq OWNER TO nathandunn;

--
-- Name: stockprop_pub_stockprop_pub_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE stockprop_pub_stockprop_pub_id_seq OWNED BY stockprop_pub.stockprop_pub_id;


--
-- Name: stockprop_stockprop_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE stockprop_stockprop_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE stockprop_stockprop_id_seq OWNER TO nathandunn;

--
-- Name: stockprop_stockprop_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE stockprop_stockprop_id_seq OWNED BY stockprop.stockprop_id;


--
-- Name: study; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE study (
    study_id integer NOT NULL,
    contact_id integer NOT NULL,
    pub_id integer,
    dbxref_id integer,
    name text NOT NULL,
    description text
);


ALTER TABLE study OWNER TO nathandunn;

--
-- Name: study_assay; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE study_assay (
    study_assay_id integer NOT NULL,
    study_id integer NOT NULL,
    assay_id integer NOT NULL
);


ALTER TABLE study_assay OWNER TO nathandunn;

--
-- Name: study_assay_study_assay_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE study_assay_study_assay_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE study_assay_study_assay_id_seq OWNER TO nathandunn;

--
-- Name: study_assay_study_assay_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE study_assay_study_assay_id_seq OWNED BY study_assay.study_assay_id;


--
-- Name: study_study_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE study_study_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE study_study_id_seq OWNER TO nathandunn;

--
-- Name: study_study_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE study_study_id_seq OWNED BY study.study_id;


--
-- Name: studydesign; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE studydesign (
    studydesign_id integer NOT NULL,
    study_id integer NOT NULL,
    description text
);


ALTER TABLE studydesign OWNER TO nathandunn;

--
-- Name: studydesign_studydesign_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE studydesign_studydesign_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE studydesign_studydesign_id_seq OWNER TO nathandunn;

--
-- Name: studydesign_studydesign_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE studydesign_studydesign_id_seq OWNED BY studydesign.studydesign_id;


--
-- Name: studydesignprop; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE studydesignprop (
    studydesignprop_id integer NOT NULL,
    studydesign_id integer NOT NULL,
    type_id integer NOT NULL,
    value text,
    rank integer DEFAULT 0 NOT NULL
);


ALTER TABLE studydesignprop OWNER TO nathandunn;

--
-- Name: studydesignprop_studydesignprop_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE studydesignprop_studydesignprop_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE studydesignprop_studydesignprop_id_seq OWNER TO nathandunn;

--
-- Name: studydesignprop_studydesignprop_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE studydesignprop_studydesignprop_id_seq OWNED BY studydesignprop.studydesignprop_id;


--
-- Name: studyfactor; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE studyfactor (
    studyfactor_id integer NOT NULL,
    studydesign_id integer NOT NULL,
    type_id integer,
    name text NOT NULL,
    description text
);


ALTER TABLE studyfactor OWNER TO nathandunn;

--
-- Name: studyfactor_studyfactor_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE studyfactor_studyfactor_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE studyfactor_studyfactor_id_seq OWNER TO nathandunn;

--
-- Name: studyfactor_studyfactor_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE studyfactor_studyfactor_id_seq OWNED BY studyfactor.studyfactor_id;


--
-- Name: studyfactorvalue; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE studyfactorvalue (
    studyfactorvalue_id integer NOT NULL,
    studyfactor_id integer NOT NULL,
    assay_id integer NOT NULL,
    factorvalue text,
    name text,
    rank integer DEFAULT 0 NOT NULL
);


ALTER TABLE studyfactorvalue OWNER TO nathandunn;

--
-- Name: studyfactorvalue_studyfactorvalue_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE studyfactorvalue_studyfactorvalue_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE studyfactorvalue_studyfactorvalue_id_seq OWNER TO nathandunn;

--
-- Name: studyfactorvalue_studyfactorvalue_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE studyfactorvalue_studyfactorvalue_id_seq OWNED BY studyfactorvalue.studyfactorvalue_id;


--
-- Name: studyprop; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE studyprop (
    studyprop_id integer NOT NULL,
    study_id integer NOT NULL,
    type_id integer NOT NULL,
    value text,
    rank integer DEFAULT 0 NOT NULL
);


ALTER TABLE studyprop OWNER TO nathandunn;

--
-- Name: studyprop_feature; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE studyprop_feature (
    studyprop_feature_id integer NOT NULL,
    studyprop_id integer NOT NULL,
    feature_id integer NOT NULL,
    type_id integer
);


ALTER TABLE studyprop_feature OWNER TO nathandunn;

--
-- Name: studyprop_feature_studyprop_feature_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE studyprop_feature_studyprop_feature_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE studyprop_feature_studyprop_feature_id_seq OWNER TO nathandunn;

--
-- Name: studyprop_feature_studyprop_feature_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE studyprop_feature_studyprop_feature_id_seq OWNED BY studyprop_feature.studyprop_feature_id;


--
-- Name: studyprop_studyprop_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE studyprop_studyprop_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE studyprop_studyprop_id_seq OWNER TO nathandunn;

--
-- Name: studyprop_studyprop_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE studyprop_studyprop_id_seq OWNED BY studyprop.studyprop_id;


--
-- Name: synonym_synonym_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE synonym_synonym_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE synonym_synonym_id_seq OWNER TO nathandunn;

--
-- Name: synonym_synonym_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE synonym_synonym_id_seq OWNED BY synonym.synonym_id;


--
-- Name: tableinfo; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE tableinfo (
    tableinfo_id integer NOT NULL,
    name character varying(30) NOT NULL,
    primary_key_column character varying(30),
    is_view integer DEFAULT 0 NOT NULL,
    view_on_table_id integer,
    superclass_table_id integer,
    is_updateable integer DEFAULT 1 NOT NULL,
    modification_date date DEFAULT now() NOT NULL
);


ALTER TABLE tableinfo OWNER TO nathandunn;

--
-- Name: tableinfo_tableinfo_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE tableinfo_tableinfo_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE tableinfo_tableinfo_id_seq OWNER TO nathandunn;

--
-- Name: tableinfo_tableinfo_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE tableinfo_tableinfo_id_seq OWNED BY tableinfo.tableinfo_id;


--
-- Name: tmp_cds_handler; Type: TABLE; Schema: chado; Owner: ubuntu; Tablespace: 
--

CREATE TABLE tmp_cds_handler (
    cds_row_id integer NOT NULL,
    seq_id character varying(1024),
    gff_id character varying(1024),
    type character varying(1024) NOT NULL,
    fmin integer NOT NULL,
    fmax integer NOT NULL,
    object text NOT NULL
);


ALTER TABLE tmp_cds_handler OWNER TO ubuntu;

--
-- Name: tmp_cds_handler_cds_row_id_seq; Type: SEQUENCE; Schema: chado; Owner: ubuntu
--

CREATE SEQUENCE tmp_cds_handler_cds_row_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE tmp_cds_handler_cds_row_id_seq OWNER TO ubuntu;

--
-- Name: tmp_cds_handler_cds_row_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: ubuntu
--

ALTER SEQUENCE tmp_cds_handler_cds_row_id_seq OWNED BY tmp_cds_handler.cds_row_id;


--
-- Name: tmp_cds_handler_relationship; Type: TABLE; Schema: chado; Owner: ubuntu; Tablespace: 
--

CREATE TABLE tmp_cds_handler_relationship (
    rel_row_id integer NOT NULL,
    cds_row_id integer,
    parent_id character varying(1024),
    grandparent_id character varying(1024)
);


ALTER TABLE tmp_cds_handler_relationship OWNER TO ubuntu;

--
-- Name: tmp_cds_handler_relationship_rel_row_id_seq; Type: SEQUENCE; Schema: chado; Owner: ubuntu
--

CREATE SEQUENCE tmp_cds_handler_relationship_rel_row_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE tmp_cds_handler_relationship_rel_row_id_seq OWNER TO ubuntu;

--
-- Name: tmp_cds_handler_relationship_rel_row_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: ubuntu
--

ALTER SEQUENCE tmp_cds_handler_relationship_rel_row_id_seq OWNED BY tmp_cds_handler_relationship.rel_row_id;


--
-- Name: treatment; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE treatment (
    treatment_id integer NOT NULL,
    rank integer DEFAULT 0 NOT NULL,
    biomaterial_id integer NOT NULL,
    type_id integer NOT NULL,
    protocol_id integer,
    name text
);


ALTER TABLE treatment OWNER TO nathandunn;

--
-- Name: TABLE treatment; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON TABLE treatment IS 'A biomaterial may undergo multiple
treatments. Examples of treatments: apoxia, fluorophore and biotin labeling.';


--
-- Name: treatment_treatment_id_seq; Type: SEQUENCE; Schema: chado; Owner: nathandunn
--

CREATE SEQUENCE treatment_treatment_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE treatment_treatment_id_seq OWNER TO nathandunn;

--
-- Name: treatment_treatment_id_seq; Type: SEQUENCE OWNED BY; Schema: chado; Owner: nathandunn
--

ALTER SEQUENCE treatment_treatment_id_seq OWNED BY treatment.treatment_id;


--
-- Name: tripal_gff_temp; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE tripal_gff_temp (
    feature_id integer NOT NULL,
    organism_id integer NOT NULL,
    uniquename text NOT NULL,
    type_name character varying(1024) NOT NULL
);


ALTER TABLE tripal_gff_temp OWNER TO nathandunn;

--
-- Name: tripal_obo_temp; Type: TABLE; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE TABLE tripal_obo_temp (
    id character varying(255) NOT NULL,
    stanza text NOT NULL,
    type character varying(50) NOT NULL
);


ALTER TABLE tripal_obo_temp OWNER TO nathandunn;

--
-- Name: type_feature_count; Type: VIEW; Schema: chado; Owner: nathandunn
--

CREATE VIEW type_feature_count AS
 SELECT t.name AS type,
    count(*) AS num_features
   FROM (cvterm t
     JOIN feature ON ((feature.type_id = t.cvterm_id)))
  GROUP BY t.name;


ALTER TABLE type_feature_count OWNER TO nathandunn;

--
-- Name: VIEW type_feature_count; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON VIEW type_feature_count IS 'per-feature-type feature counts';


--
-- Name: acquisition_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY acquisition ALTER COLUMN acquisition_id SET DEFAULT nextval('acquisition_acquisition_id_seq'::regclass);


--
-- Name: acquisition_relationship_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY acquisition_relationship ALTER COLUMN acquisition_relationship_id SET DEFAULT nextval('acquisition_relationship_acquisition_relationship_id_seq'::regclass);


--
-- Name: acquisitionprop_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY acquisitionprop ALTER COLUMN acquisitionprop_id SET DEFAULT nextval('acquisitionprop_acquisitionprop_id_seq'::regclass);


--
-- Name: analysis_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY analysis ALTER COLUMN analysis_id SET DEFAULT nextval('analysis_analysis_id_seq'::regclass);


--
-- Name: analysisfeature_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY analysisfeature ALTER COLUMN analysisfeature_id SET DEFAULT nextval('analysisfeature_analysisfeature_id_seq'::regclass);


--
-- Name: analysisfeatureprop_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY analysisfeatureprop ALTER COLUMN analysisfeatureprop_id SET DEFAULT nextval('analysisfeatureprop_analysisfeatureprop_id_seq'::regclass);


--
-- Name: analysisprop_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY analysisprop ALTER COLUMN analysisprop_id SET DEFAULT nextval('analysisprop_analysisprop_id_seq'::regclass);


--
-- Name: arraydesign_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY arraydesign ALTER COLUMN arraydesign_id SET DEFAULT nextval('arraydesign_arraydesign_id_seq'::regclass);


--
-- Name: arraydesignprop_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY arraydesignprop ALTER COLUMN arraydesignprop_id SET DEFAULT nextval('arraydesignprop_arraydesignprop_id_seq'::regclass);


--
-- Name: assay_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY assay ALTER COLUMN assay_id SET DEFAULT nextval('assay_assay_id_seq'::regclass);


--
-- Name: assay_biomaterial_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY assay_biomaterial ALTER COLUMN assay_biomaterial_id SET DEFAULT nextval('assay_biomaterial_assay_biomaterial_id_seq'::regclass);


--
-- Name: assay_project_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY assay_project ALTER COLUMN assay_project_id SET DEFAULT nextval('assay_project_assay_project_id_seq'::regclass);


--
-- Name: assayprop_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY assayprop ALTER COLUMN assayprop_id SET DEFAULT nextval('assayprop_assayprop_id_seq'::regclass);


--
-- Name: biomaterial_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY biomaterial ALTER COLUMN biomaterial_id SET DEFAULT nextval('biomaterial_biomaterial_id_seq'::regclass);


--
-- Name: biomaterial_dbxref_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY biomaterial_dbxref ALTER COLUMN biomaterial_dbxref_id SET DEFAULT nextval('biomaterial_dbxref_biomaterial_dbxref_id_seq'::regclass);


--
-- Name: biomaterial_relationship_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY biomaterial_relationship ALTER COLUMN biomaterial_relationship_id SET DEFAULT nextval('biomaterial_relationship_biomaterial_relationship_id_seq'::regclass);


--
-- Name: biomaterial_treatment_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY biomaterial_treatment ALTER COLUMN biomaterial_treatment_id SET DEFAULT nextval('biomaterial_treatment_biomaterial_treatment_id_seq'::regclass);


--
-- Name: biomaterialprop_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY biomaterialprop ALTER COLUMN biomaterialprop_id SET DEFAULT nextval('biomaterialprop_biomaterialprop_id_seq'::regclass);


--
-- Name: blast_org_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY blast_organisms ALTER COLUMN blast_org_id SET DEFAULT nextval('blast_organisms_blast_org_id_seq'::regclass);


--
-- Name: cell_line_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY cell_line ALTER COLUMN cell_line_id SET DEFAULT nextval('cell_line_cell_line_id_seq'::regclass);


--
-- Name: cell_line_cvterm_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY cell_line_cvterm ALTER COLUMN cell_line_cvterm_id SET DEFAULT nextval('cell_line_cvterm_cell_line_cvterm_id_seq'::regclass);


--
-- Name: cell_line_cvtermprop_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY cell_line_cvtermprop ALTER COLUMN cell_line_cvtermprop_id SET DEFAULT nextval('cell_line_cvtermprop_cell_line_cvtermprop_id_seq'::regclass);


--
-- Name: cell_line_dbxref_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY cell_line_dbxref ALTER COLUMN cell_line_dbxref_id SET DEFAULT nextval('cell_line_dbxref_cell_line_dbxref_id_seq'::regclass);


--
-- Name: cell_line_feature_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY cell_line_feature ALTER COLUMN cell_line_feature_id SET DEFAULT nextval('cell_line_feature_cell_line_feature_id_seq'::regclass);


--
-- Name: cell_line_library_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY cell_line_library ALTER COLUMN cell_line_library_id SET DEFAULT nextval('cell_line_library_cell_line_library_id_seq'::regclass);


--
-- Name: cell_line_pub_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY cell_line_pub ALTER COLUMN cell_line_pub_id SET DEFAULT nextval('cell_line_pub_cell_line_pub_id_seq'::regclass);


--
-- Name: cell_line_relationship_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY cell_line_relationship ALTER COLUMN cell_line_relationship_id SET DEFAULT nextval('cell_line_relationship_cell_line_relationship_id_seq'::regclass);


--
-- Name: cell_line_synonym_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY cell_line_synonym ALTER COLUMN cell_line_synonym_id SET DEFAULT nextval('cell_line_synonym_cell_line_synonym_id_seq'::regclass);


--
-- Name: cell_lineprop_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY cell_lineprop ALTER COLUMN cell_lineprop_id SET DEFAULT nextval('cell_lineprop_cell_lineprop_id_seq'::regclass);


--
-- Name: cell_lineprop_pub_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY cell_lineprop_pub ALTER COLUMN cell_lineprop_pub_id SET DEFAULT nextval('cell_lineprop_pub_cell_lineprop_pub_id_seq'::regclass);


--
-- Name: chadoprop_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY chadoprop ALTER COLUMN chadoprop_id SET DEFAULT nextval('chadoprop_chadoprop_id_seq'::regclass);


--
-- Name: channel_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY channel ALTER COLUMN channel_id SET DEFAULT nextval('channel_channel_id_seq'::regclass);


--
-- Name: contact_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY contact ALTER COLUMN contact_id SET DEFAULT nextval('contact_contact_id_seq'::regclass);


--
-- Name: contact_relationship_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY contact_relationship ALTER COLUMN contact_relationship_id SET DEFAULT nextval('contact_relationship_contact_relationship_id_seq'::regclass);


--
-- Name: control_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY control ALTER COLUMN control_id SET DEFAULT nextval('control_control_id_seq'::regclass);


--
-- Name: cv_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY cv ALTER COLUMN cv_id SET DEFAULT nextval('cv_cv_id_seq'::regclass);


--
-- Name: cvprop_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY cvprop ALTER COLUMN cvprop_id SET DEFAULT nextval('cvprop_cvprop_id_seq'::regclass);


--
-- Name: cvterm_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY cvterm ALTER COLUMN cvterm_id SET DEFAULT nextval('cvterm_cvterm_id_seq'::regclass);


--
-- Name: cvterm_dbxref_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY cvterm_dbxref ALTER COLUMN cvterm_dbxref_id SET DEFAULT nextval('cvterm_dbxref_cvterm_dbxref_id_seq'::regclass);


--
-- Name: cvterm_relationship_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY cvterm_relationship ALTER COLUMN cvterm_relationship_id SET DEFAULT nextval('cvterm_relationship_cvterm_relationship_id_seq'::regclass);


--
-- Name: cvtermpath_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY cvtermpath ALTER COLUMN cvtermpath_id SET DEFAULT nextval('cvtermpath_cvtermpath_id_seq'::regclass);


--
-- Name: cvtermprop_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY cvtermprop ALTER COLUMN cvtermprop_id SET DEFAULT nextval('cvtermprop_cvtermprop_id_seq'::regclass);


--
-- Name: cvtermsynonym_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY cvtermsynonym ALTER COLUMN cvtermsynonym_id SET DEFAULT nextval('cvtermsynonym_cvtermsynonym_id_seq'::regclass);


--
-- Name: db_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY db ALTER COLUMN db_id SET DEFAULT nextval('db_db_id_seq'::regclass);


--
-- Name: dbxref_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY dbxref ALTER COLUMN dbxref_id SET DEFAULT nextval('dbxref_dbxref_id_seq'::regclass);


--
-- Name: dbxrefprop_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY dbxrefprop ALTER COLUMN dbxrefprop_id SET DEFAULT nextval('dbxrefprop_dbxrefprop_id_seq'::regclass);


--
-- Name: eimage_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY eimage ALTER COLUMN eimage_id SET DEFAULT nextval('eimage_eimage_id_seq'::regclass);


--
-- Name: element_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY element ALTER COLUMN element_id SET DEFAULT nextval('element_element_id_seq'::regclass);


--
-- Name: element_relationship_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY element_relationship ALTER COLUMN element_relationship_id SET DEFAULT nextval('element_relationship_element_relationship_id_seq'::regclass);


--
-- Name: elementresult_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY elementresult ALTER COLUMN elementresult_id SET DEFAULT nextval('elementresult_elementresult_id_seq'::regclass);


--
-- Name: elementresult_relationship_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY elementresult_relationship ALTER COLUMN elementresult_relationship_id SET DEFAULT nextval('elementresult_relationship_elementresult_relationship_id_seq'::regclass);


--
-- Name: environment_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY environment ALTER COLUMN environment_id SET DEFAULT nextval('environment_environment_id_seq'::regclass);


--
-- Name: environment_cvterm_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY environment_cvterm ALTER COLUMN environment_cvterm_id SET DEFAULT nextval('environment_cvterm_environment_cvterm_id_seq'::regclass);


--
-- Name: expression_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY expression ALTER COLUMN expression_id SET DEFAULT nextval('expression_expression_id_seq'::regclass);


--
-- Name: expression_cvterm_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY expression_cvterm ALTER COLUMN expression_cvterm_id SET DEFAULT nextval('expression_cvterm_expression_cvterm_id_seq'::regclass);


--
-- Name: expression_cvtermprop_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY expression_cvtermprop ALTER COLUMN expression_cvtermprop_id SET DEFAULT nextval('expression_cvtermprop_expression_cvtermprop_id_seq'::regclass);


--
-- Name: expression_image_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY expression_image ALTER COLUMN expression_image_id SET DEFAULT nextval('expression_image_expression_image_id_seq'::regclass);


--
-- Name: expression_pub_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY expression_pub ALTER COLUMN expression_pub_id SET DEFAULT nextval('expression_pub_expression_pub_id_seq'::regclass);


--
-- Name: expressionprop_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY expressionprop ALTER COLUMN expressionprop_id SET DEFAULT nextval('expressionprop_expressionprop_id_seq'::regclass);


--
-- Name: feature_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY feature ALTER COLUMN feature_id SET DEFAULT nextval('feature_feature_id_seq'::regclass);


--
-- Name: feature_cvterm_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY feature_cvterm ALTER COLUMN feature_cvterm_id SET DEFAULT nextval('feature_cvterm_feature_cvterm_id_seq'::regclass);


--
-- Name: feature_cvterm_dbxref_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY feature_cvterm_dbxref ALTER COLUMN feature_cvterm_dbxref_id SET DEFAULT nextval('feature_cvterm_dbxref_feature_cvterm_dbxref_id_seq'::regclass);


--
-- Name: feature_cvterm_pub_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY feature_cvterm_pub ALTER COLUMN feature_cvterm_pub_id SET DEFAULT nextval('feature_cvterm_pub_feature_cvterm_pub_id_seq'::regclass);


--
-- Name: feature_cvtermprop_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY feature_cvtermprop ALTER COLUMN feature_cvtermprop_id SET DEFAULT nextval('feature_cvtermprop_feature_cvtermprop_id_seq'::regclass);


--
-- Name: feature_dbxref_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY feature_dbxref ALTER COLUMN feature_dbxref_id SET DEFAULT nextval('feature_dbxref_feature_dbxref_id_seq'::regclass);


--
-- Name: feature_expression_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY feature_expression ALTER COLUMN feature_expression_id SET DEFAULT nextval('feature_expression_feature_expression_id_seq'::regclass);


--
-- Name: feature_expressionprop_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY feature_expressionprop ALTER COLUMN feature_expressionprop_id SET DEFAULT nextval('feature_expressionprop_feature_expressionprop_id_seq'::regclass);


--
-- Name: feature_genotype_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY feature_genotype ALTER COLUMN feature_genotype_id SET DEFAULT nextval('feature_genotype_feature_genotype_id_seq'::regclass);


--
-- Name: feature_phenotype_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY feature_phenotype ALTER COLUMN feature_phenotype_id SET DEFAULT nextval('feature_phenotype_feature_phenotype_id_seq'::regclass);


--
-- Name: feature_pub_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY feature_pub ALTER COLUMN feature_pub_id SET DEFAULT nextval('feature_pub_feature_pub_id_seq'::regclass);


--
-- Name: feature_pubprop_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY feature_pubprop ALTER COLUMN feature_pubprop_id SET DEFAULT nextval('feature_pubprop_feature_pubprop_id_seq'::regclass);


--
-- Name: feature_relationship_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY feature_relationship ALTER COLUMN feature_relationship_id SET DEFAULT nextval('feature_relationship_feature_relationship_id_seq'::regclass);


--
-- Name: feature_relationship_pub_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY feature_relationship_pub ALTER COLUMN feature_relationship_pub_id SET DEFAULT nextval('feature_relationship_pub_feature_relationship_pub_id_seq'::regclass);


--
-- Name: feature_relationshipprop_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY feature_relationshipprop ALTER COLUMN feature_relationshipprop_id SET DEFAULT nextval('feature_relationshipprop_feature_relationshipprop_id_seq'::regclass);


--
-- Name: feature_relationshipprop_pub_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY feature_relationshipprop_pub ALTER COLUMN feature_relationshipprop_pub_id SET DEFAULT nextval('feature_relationshipprop_pub_feature_relationshipprop_pub_i_seq'::regclass);


--
-- Name: feature_synonym_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY feature_synonym ALTER COLUMN feature_synonym_id SET DEFAULT nextval('feature_synonym_feature_synonym_id_seq'::regclass);


--
-- Name: featureloc_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY featureloc ALTER COLUMN featureloc_id SET DEFAULT nextval('featureloc_featureloc_id_seq'::regclass);


--
-- Name: featureloc_pub_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY featureloc_pub ALTER COLUMN featureloc_pub_id SET DEFAULT nextval('featureloc_pub_featureloc_pub_id_seq'::regclass);


--
-- Name: featuremap_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY featuremap ALTER COLUMN featuremap_id SET DEFAULT nextval('featuremap_featuremap_id_seq'::regclass);


--
-- Name: featuremap_pub_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY featuremap_pub ALTER COLUMN featuremap_pub_id SET DEFAULT nextval('featuremap_pub_featuremap_pub_id_seq'::regclass);


--
-- Name: featurepos_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY featurepos ALTER COLUMN featurepos_id SET DEFAULT nextval('featurepos_featurepos_id_seq'::regclass);


--
-- Name: featuremap_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY featurepos ALTER COLUMN featuremap_id SET DEFAULT nextval('featurepos_featuremap_id_seq'::regclass);


--
-- Name: featureprop_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY featureprop ALTER COLUMN featureprop_id SET DEFAULT nextval('featureprop_featureprop_id_seq'::regclass);


--
-- Name: featureprop_pub_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY featureprop_pub ALTER COLUMN featureprop_pub_id SET DEFAULT nextval('featureprop_pub_featureprop_pub_id_seq'::regclass);


--
-- Name: featurerange_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY featurerange ALTER COLUMN featurerange_id SET DEFAULT nextval('featurerange_featurerange_id_seq'::regclass);


--
-- Name: genotype_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY genotype ALTER COLUMN genotype_id SET DEFAULT nextval('genotype_genotype_id_seq'::regclass);


--
-- Name: genotypeprop_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY genotypeprop ALTER COLUMN genotypeprop_id SET DEFAULT nextval('genotypeprop_genotypeprop_id_seq'::regclass);


--
-- Name: library_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY library ALTER COLUMN library_id SET DEFAULT nextval('library_library_id_seq'::regclass);


--
-- Name: library_cvterm_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY library_cvterm ALTER COLUMN library_cvterm_id SET DEFAULT nextval('library_cvterm_library_cvterm_id_seq'::regclass);


--
-- Name: library_dbxref_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY library_dbxref ALTER COLUMN library_dbxref_id SET DEFAULT nextval('library_dbxref_library_dbxref_id_seq'::regclass);


--
-- Name: library_feature_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY library_feature ALTER COLUMN library_feature_id SET DEFAULT nextval('library_feature_library_feature_id_seq'::regclass);


--
-- Name: library_pub_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY library_pub ALTER COLUMN library_pub_id SET DEFAULT nextval('library_pub_library_pub_id_seq'::regclass);


--
-- Name: library_synonym_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY library_synonym ALTER COLUMN library_synonym_id SET DEFAULT nextval('library_synonym_library_synonym_id_seq'::regclass);


--
-- Name: libraryprop_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY libraryprop ALTER COLUMN libraryprop_id SET DEFAULT nextval('libraryprop_libraryprop_id_seq'::regclass);


--
-- Name: libraryprop_pub_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY libraryprop_pub ALTER COLUMN libraryprop_pub_id SET DEFAULT nextval('libraryprop_pub_libraryprop_pub_id_seq'::regclass);


--
-- Name: magedocumentation_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY magedocumentation ALTER COLUMN magedocumentation_id SET DEFAULT nextval('magedocumentation_magedocumentation_id_seq'::regclass);


--
-- Name: mageml_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY mageml ALTER COLUMN mageml_id SET DEFAULT nextval('mageml_mageml_id_seq'::regclass);


--
-- Name: materialized_view_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY materialized_view ALTER COLUMN materialized_view_id SET DEFAULT nextval('materialized_view_materialized_view_id_seq'::regclass);


--
-- Name: nd_experiment_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY nd_experiment ALTER COLUMN nd_experiment_id SET DEFAULT nextval('nd_experiment_nd_experiment_id_seq'::regclass);


--
-- Name: nd_experiment_contact_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY nd_experiment_contact ALTER COLUMN nd_experiment_contact_id SET DEFAULT nextval('nd_experiment_contact_nd_experiment_contact_id_seq'::regclass);


--
-- Name: nd_experiment_dbxref_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY nd_experiment_dbxref ALTER COLUMN nd_experiment_dbxref_id SET DEFAULT nextval('nd_experiment_dbxref_nd_experiment_dbxref_id_seq'::regclass);


--
-- Name: nd_experiment_genotype_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY nd_experiment_genotype ALTER COLUMN nd_experiment_genotype_id SET DEFAULT nextval('nd_experiment_genotype_nd_experiment_genotype_id_seq'::regclass);


--
-- Name: nd_experiment_phenotype_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY nd_experiment_phenotype ALTER COLUMN nd_experiment_phenotype_id SET DEFAULT nextval('nd_experiment_phenotype_nd_experiment_phenotype_id_seq'::regclass);


--
-- Name: nd_experiment_project_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY nd_experiment_project ALTER COLUMN nd_experiment_project_id SET DEFAULT nextval('nd_experiment_project_nd_experiment_project_id_seq'::regclass);


--
-- Name: nd_experiment_protocol_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY nd_experiment_protocol ALTER COLUMN nd_experiment_protocol_id SET DEFAULT nextval('nd_experiment_protocol_nd_experiment_protocol_id_seq'::regclass);


--
-- Name: nd_experiment_pub_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY nd_experiment_pub ALTER COLUMN nd_experiment_pub_id SET DEFAULT nextval('nd_experiment_pub_nd_experiment_pub_id_seq'::regclass);


--
-- Name: nd_experiment_stock_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY nd_experiment_stock ALTER COLUMN nd_experiment_stock_id SET DEFAULT nextval('nd_experiment_stock_nd_experiment_stock_id_seq'::regclass);


--
-- Name: nd_experiment_stock_dbxref_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY nd_experiment_stock_dbxref ALTER COLUMN nd_experiment_stock_dbxref_id SET DEFAULT nextval('nd_experiment_stock_dbxref_nd_experiment_stock_dbxref_id_seq'::regclass);


--
-- Name: nd_experiment_stockprop_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY nd_experiment_stockprop ALTER COLUMN nd_experiment_stockprop_id SET DEFAULT nextval('nd_experiment_stockprop_nd_experiment_stockprop_id_seq'::regclass);


--
-- Name: nd_experimentprop_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY nd_experimentprop ALTER COLUMN nd_experimentprop_id SET DEFAULT nextval('nd_experimentprop_nd_experimentprop_id_seq'::regclass);


--
-- Name: nd_geolocation_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY nd_geolocation ALTER COLUMN nd_geolocation_id SET DEFAULT nextval('nd_geolocation_nd_geolocation_id_seq'::regclass);


--
-- Name: nd_geolocationprop_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY nd_geolocationprop ALTER COLUMN nd_geolocationprop_id SET DEFAULT nextval('nd_geolocationprop_nd_geolocationprop_id_seq'::regclass);


--
-- Name: nd_protocol_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY nd_protocol ALTER COLUMN nd_protocol_id SET DEFAULT nextval('nd_protocol_nd_protocol_id_seq'::regclass);


--
-- Name: nd_protocol_reagent_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY nd_protocol_reagent ALTER COLUMN nd_protocol_reagent_id SET DEFAULT nextval('nd_protocol_reagent_nd_protocol_reagent_id_seq'::regclass);


--
-- Name: nd_protocolprop_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY nd_protocolprop ALTER COLUMN nd_protocolprop_id SET DEFAULT nextval('nd_protocolprop_nd_protocolprop_id_seq'::regclass);


--
-- Name: nd_reagent_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY nd_reagent ALTER COLUMN nd_reagent_id SET DEFAULT nextval('nd_reagent_nd_reagent_id_seq'::regclass);


--
-- Name: nd_reagent_relationship_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY nd_reagent_relationship ALTER COLUMN nd_reagent_relationship_id SET DEFAULT nextval('nd_reagent_relationship_nd_reagent_relationship_id_seq'::regclass);


--
-- Name: nd_reagentprop_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY nd_reagentprop ALTER COLUMN nd_reagentprop_id SET DEFAULT nextval('nd_reagentprop_nd_reagentprop_id_seq'::regclass);


--
-- Name: organism_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY organism ALTER COLUMN organism_id SET DEFAULT nextval('organism_organism_id_seq'::regclass);


--
-- Name: organism_dbxref_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY organism_dbxref ALTER COLUMN organism_dbxref_id SET DEFAULT nextval('organism_dbxref_organism_dbxref_id_seq'::regclass);


--
-- Name: organismprop_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY organismprop ALTER COLUMN organismprop_id SET DEFAULT nextval('organismprop_organismprop_id_seq'::regclass);


--
-- Name: phendesc_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY phendesc ALTER COLUMN phendesc_id SET DEFAULT nextval('phendesc_phendesc_id_seq'::regclass);


--
-- Name: phenotype_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY phenotype ALTER COLUMN phenotype_id SET DEFAULT nextval('phenotype_phenotype_id_seq'::regclass);


--
-- Name: phenotype_comparison_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY phenotype_comparison ALTER COLUMN phenotype_comparison_id SET DEFAULT nextval('phenotype_comparison_phenotype_comparison_id_seq'::regclass);


--
-- Name: phenotype_comparison_cvterm_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY phenotype_comparison_cvterm ALTER COLUMN phenotype_comparison_cvterm_id SET DEFAULT nextval('phenotype_comparison_cvterm_phenotype_comparison_cvterm_id_seq'::regclass);


--
-- Name: phenotype_cvterm_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY phenotype_cvterm ALTER COLUMN phenotype_cvterm_id SET DEFAULT nextval('phenotype_cvterm_phenotype_cvterm_id_seq'::regclass);


--
-- Name: phenstatement_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY phenstatement ALTER COLUMN phenstatement_id SET DEFAULT nextval('phenstatement_phenstatement_id_seq'::regclass);


--
-- Name: phylonode_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY phylonode ALTER COLUMN phylonode_id SET DEFAULT nextval('phylonode_phylonode_id_seq'::regclass);


--
-- Name: phylonode_dbxref_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY phylonode_dbxref ALTER COLUMN phylonode_dbxref_id SET DEFAULT nextval('phylonode_dbxref_phylonode_dbxref_id_seq'::regclass);


--
-- Name: phylonode_organism_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY phylonode_organism ALTER COLUMN phylonode_organism_id SET DEFAULT nextval('phylonode_organism_phylonode_organism_id_seq'::regclass);


--
-- Name: phylonode_pub_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY phylonode_pub ALTER COLUMN phylonode_pub_id SET DEFAULT nextval('phylonode_pub_phylonode_pub_id_seq'::regclass);


--
-- Name: phylonode_relationship_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY phylonode_relationship ALTER COLUMN phylonode_relationship_id SET DEFAULT nextval('phylonode_relationship_phylonode_relationship_id_seq'::regclass);


--
-- Name: phylonodeprop_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY phylonodeprop ALTER COLUMN phylonodeprop_id SET DEFAULT nextval('phylonodeprop_phylonodeprop_id_seq'::regclass);


--
-- Name: phylotree_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY phylotree ALTER COLUMN phylotree_id SET DEFAULT nextval('phylotree_phylotree_id_seq'::regclass);


--
-- Name: phylotree_pub_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY phylotree_pub ALTER COLUMN phylotree_pub_id SET DEFAULT nextval('phylotree_pub_phylotree_pub_id_seq'::regclass);


--
-- Name: project_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY project ALTER COLUMN project_id SET DEFAULT nextval('project_project_id_seq'::regclass);


--
-- Name: project_contact_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY project_contact ALTER COLUMN project_contact_id SET DEFAULT nextval('project_contact_project_contact_id_seq'::regclass);


--
-- Name: project_pub_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY project_pub ALTER COLUMN project_pub_id SET DEFAULT nextval('project_pub_project_pub_id_seq'::regclass);


--
-- Name: project_relationship_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY project_relationship ALTER COLUMN project_relationship_id SET DEFAULT nextval('project_relationship_project_relationship_id_seq'::regclass);


--
-- Name: projectprop_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY projectprop ALTER COLUMN projectprop_id SET DEFAULT nextval('projectprop_projectprop_id_seq'::regclass);


--
-- Name: protocol_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY protocol ALTER COLUMN protocol_id SET DEFAULT nextval('protocol_protocol_id_seq'::regclass);


--
-- Name: protocolparam_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY protocolparam ALTER COLUMN protocolparam_id SET DEFAULT nextval('protocolparam_protocolparam_id_seq'::regclass);


--
-- Name: pub_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY pub ALTER COLUMN pub_id SET DEFAULT nextval('pub_pub_id_seq'::regclass);


--
-- Name: pub_dbxref_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY pub_dbxref ALTER COLUMN pub_dbxref_id SET DEFAULT nextval('pub_dbxref_pub_dbxref_id_seq'::regclass);


--
-- Name: pub_relationship_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY pub_relationship ALTER COLUMN pub_relationship_id SET DEFAULT nextval('pub_relationship_pub_relationship_id_seq'::regclass);


--
-- Name: pubauthor_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY pubauthor ALTER COLUMN pubauthor_id SET DEFAULT nextval('pubauthor_pubauthor_id_seq'::regclass);


--
-- Name: pubprop_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY pubprop ALTER COLUMN pubprop_id SET DEFAULT nextval('pubprop_pubprop_id_seq'::regclass);


--
-- Name: quantification_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY quantification ALTER COLUMN quantification_id SET DEFAULT nextval('quantification_quantification_id_seq'::regclass);


--
-- Name: quantification_relationship_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY quantification_relationship ALTER COLUMN quantification_relationship_id SET DEFAULT nextval('quantification_relationship_quantification_relationship_id_seq'::regclass);


--
-- Name: quantificationprop_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY quantificationprop ALTER COLUMN quantificationprop_id SET DEFAULT nextval('quantificationprop_quantificationprop_id_seq'::regclass);


--
-- Name: stock_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY stock ALTER COLUMN stock_id SET DEFAULT nextval('stock_stock_id_seq'::regclass);


--
-- Name: stock_cvterm_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY stock_cvterm ALTER COLUMN stock_cvterm_id SET DEFAULT nextval('stock_cvterm_stock_cvterm_id_seq'::regclass);


--
-- Name: stock_cvtermprop_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY stock_cvtermprop ALTER COLUMN stock_cvtermprop_id SET DEFAULT nextval('stock_cvtermprop_stock_cvtermprop_id_seq'::regclass);


--
-- Name: stock_dbxref_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY stock_dbxref ALTER COLUMN stock_dbxref_id SET DEFAULT nextval('stock_dbxref_stock_dbxref_id_seq'::regclass);


--
-- Name: stock_dbxrefprop_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY stock_dbxrefprop ALTER COLUMN stock_dbxrefprop_id SET DEFAULT nextval('stock_dbxrefprop_stock_dbxrefprop_id_seq'::regclass);


--
-- Name: stock_genotype_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY stock_genotype ALTER COLUMN stock_genotype_id SET DEFAULT nextval('stock_genotype_stock_genotype_id_seq'::regclass);


--
-- Name: stock_pub_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY stock_pub ALTER COLUMN stock_pub_id SET DEFAULT nextval('stock_pub_stock_pub_id_seq'::regclass);


--
-- Name: stock_relationship_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY stock_relationship ALTER COLUMN stock_relationship_id SET DEFAULT nextval('stock_relationship_stock_relationship_id_seq'::regclass);


--
-- Name: stock_relationship_cvterm_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY stock_relationship_cvterm ALTER COLUMN stock_relationship_cvterm_id SET DEFAULT nextval('stock_relationship_cvterm_stock_relationship_cvterm_id_seq'::regclass);


--
-- Name: stock_relationship_pub_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY stock_relationship_pub ALTER COLUMN stock_relationship_pub_id SET DEFAULT nextval('stock_relationship_pub_stock_relationship_pub_id_seq'::regclass);


--
-- Name: stockcollection_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY stockcollection ALTER COLUMN stockcollection_id SET DEFAULT nextval('stockcollection_stockcollection_id_seq'::regclass);


--
-- Name: stockcollection_stock_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY stockcollection_stock ALTER COLUMN stockcollection_stock_id SET DEFAULT nextval('stockcollection_stock_stockcollection_stock_id_seq'::regclass);


--
-- Name: stockcollectionprop_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY stockcollectionprop ALTER COLUMN stockcollectionprop_id SET DEFAULT nextval('stockcollectionprop_stockcollectionprop_id_seq'::regclass);


--
-- Name: stockprop_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY stockprop ALTER COLUMN stockprop_id SET DEFAULT nextval('stockprop_stockprop_id_seq'::regclass);


--
-- Name: stockprop_pub_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY stockprop_pub ALTER COLUMN stockprop_pub_id SET DEFAULT nextval('stockprop_pub_stockprop_pub_id_seq'::regclass);


--
-- Name: study_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY study ALTER COLUMN study_id SET DEFAULT nextval('study_study_id_seq'::regclass);


--
-- Name: study_assay_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY study_assay ALTER COLUMN study_assay_id SET DEFAULT nextval('study_assay_study_assay_id_seq'::regclass);


--
-- Name: studydesign_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY studydesign ALTER COLUMN studydesign_id SET DEFAULT nextval('studydesign_studydesign_id_seq'::regclass);


--
-- Name: studydesignprop_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY studydesignprop ALTER COLUMN studydesignprop_id SET DEFAULT nextval('studydesignprop_studydesignprop_id_seq'::regclass);


--
-- Name: studyfactor_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY studyfactor ALTER COLUMN studyfactor_id SET DEFAULT nextval('studyfactor_studyfactor_id_seq'::regclass);


--
-- Name: studyfactorvalue_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY studyfactorvalue ALTER COLUMN studyfactorvalue_id SET DEFAULT nextval('studyfactorvalue_studyfactorvalue_id_seq'::regclass);


--
-- Name: studyprop_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY studyprop ALTER COLUMN studyprop_id SET DEFAULT nextval('studyprop_studyprop_id_seq'::regclass);


--
-- Name: studyprop_feature_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY studyprop_feature ALTER COLUMN studyprop_feature_id SET DEFAULT nextval('studyprop_feature_studyprop_feature_id_seq'::regclass);


--
-- Name: synonym_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY synonym ALTER COLUMN synonym_id SET DEFAULT nextval('synonym_synonym_id_seq'::regclass);


--
-- Name: tableinfo_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY tableinfo ALTER COLUMN tableinfo_id SET DEFAULT nextval('tableinfo_tableinfo_id_seq'::regclass);


--
-- Name: cds_row_id; Type: DEFAULT; Schema: chado; Owner: ubuntu
--

ALTER TABLE ONLY tmp_cds_handler ALTER COLUMN cds_row_id SET DEFAULT nextval('tmp_cds_handler_cds_row_id_seq'::regclass);


--
-- Name: rel_row_id; Type: DEFAULT; Schema: chado; Owner: ubuntu
--

ALTER TABLE ONLY tmp_cds_handler_relationship ALTER COLUMN rel_row_id SET DEFAULT nextval('tmp_cds_handler_relationship_rel_row_id_seq'::regclass);


--
-- Name: treatment_id; Type: DEFAULT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY treatment ALTER COLUMN treatment_id SET DEFAULT nextval('treatment_treatment_id_seq'::regclass);


--
-- Name: acquisition_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY acquisition
    ADD CONSTRAINT acquisition_c1 UNIQUE (name);


--
-- Name: acquisition_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY acquisition
    ADD CONSTRAINT acquisition_pkey PRIMARY KEY (acquisition_id);


--
-- Name: acquisition_relationship_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY acquisition_relationship
    ADD CONSTRAINT acquisition_relationship_c1 UNIQUE (subject_id, object_id, type_id, rank);


--
-- Name: acquisition_relationship_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY acquisition_relationship
    ADD CONSTRAINT acquisition_relationship_pkey PRIMARY KEY (acquisition_relationship_id);


--
-- Name: acquisitionprop_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY acquisitionprop
    ADD CONSTRAINT acquisitionprop_c1 UNIQUE (acquisition_id, type_id, rank);


--
-- Name: acquisitionprop_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY acquisitionprop
    ADD CONSTRAINT acquisitionprop_pkey PRIMARY KEY (acquisitionprop_id);


--
-- Name: analysis_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY analysis
    ADD CONSTRAINT analysis_c1 UNIQUE (program, programversion, sourcename);


--
-- Name: analysis_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY analysis
    ADD CONSTRAINT analysis_pkey PRIMARY KEY (analysis_id);


--
-- Name: analysisfeature_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY analysisfeature
    ADD CONSTRAINT analysisfeature_c1 UNIQUE (feature_id, analysis_id);


--
-- Name: analysisfeature_id_type_id_rank; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY analysisfeatureprop
    ADD CONSTRAINT analysisfeature_id_type_id_rank UNIQUE (analysisfeature_id, type_id, rank);


--
-- Name: analysisfeature_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY analysisfeature
    ADD CONSTRAINT analysisfeature_pkey PRIMARY KEY (analysisfeature_id);


--
-- Name: analysisfeatureprop_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY analysisfeatureprop
    ADD CONSTRAINT analysisfeatureprop_pkey PRIMARY KEY (analysisfeatureprop_id);


--
-- Name: analysisprop_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY analysisprop
    ADD CONSTRAINT analysisprop_c1 UNIQUE (analysis_id, type_id, rank);


--
-- Name: analysisprop_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY analysisprop
    ADD CONSTRAINT analysisprop_pkey PRIMARY KEY (analysisprop_id);


--
-- Name: arraydesign_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY arraydesign
    ADD CONSTRAINT arraydesign_c1 UNIQUE (name);


--
-- Name: arraydesign_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY arraydesign
    ADD CONSTRAINT arraydesign_pkey PRIMARY KEY (arraydesign_id);


--
-- Name: arraydesignprop_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY arraydesignprop
    ADD CONSTRAINT arraydesignprop_c1 UNIQUE (arraydesign_id, type_id, rank);


--
-- Name: arraydesignprop_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY arraydesignprop
    ADD CONSTRAINT arraydesignprop_pkey PRIMARY KEY (arraydesignprop_id);


--
-- Name: assay_biomaterial_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY assay_biomaterial
    ADD CONSTRAINT assay_biomaterial_c1 UNIQUE (assay_id, biomaterial_id, channel_id, rank);


--
-- Name: assay_biomaterial_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY assay_biomaterial
    ADD CONSTRAINT assay_biomaterial_pkey PRIMARY KEY (assay_biomaterial_id);


--
-- Name: assay_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY assay
    ADD CONSTRAINT assay_c1 UNIQUE (name);


--
-- Name: assay_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY assay
    ADD CONSTRAINT assay_pkey PRIMARY KEY (assay_id);


--
-- Name: assay_project_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY assay_project
    ADD CONSTRAINT assay_project_c1 UNIQUE (assay_id, project_id);


--
-- Name: assay_project_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY assay_project
    ADD CONSTRAINT assay_project_pkey PRIMARY KEY (assay_project_id);


--
-- Name: assayprop_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY assayprop
    ADD CONSTRAINT assayprop_c1 UNIQUE (assay_id, type_id, rank);


--
-- Name: assayprop_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY assayprop
    ADD CONSTRAINT assayprop_pkey PRIMARY KEY (assayprop_id);


--
-- Name: biomaterial_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY biomaterial
    ADD CONSTRAINT biomaterial_c1 UNIQUE (name);


--
-- Name: biomaterial_dbxref_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY biomaterial_dbxref
    ADD CONSTRAINT biomaterial_dbxref_c1 UNIQUE (biomaterial_id, dbxref_id);


--
-- Name: biomaterial_dbxref_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY biomaterial_dbxref
    ADD CONSTRAINT biomaterial_dbxref_pkey PRIMARY KEY (biomaterial_dbxref_id);


--
-- Name: biomaterial_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY biomaterial
    ADD CONSTRAINT biomaterial_pkey PRIMARY KEY (biomaterial_id);


--
-- Name: biomaterial_relationship_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY biomaterial_relationship
    ADD CONSTRAINT biomaterial_relationship_c1 UNIQUE (subject_id, object_id, type_id);


--
-- Name: biomaterial_relationship_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY biomaterial_relationship
    ADD CONSTRAINT biomaterial_relationship_pkey PRIMARY KEY (biomaterial_relationship_id);


--
-- Name: biomaterial_treatment_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY biomaterial_treatment
    ADD CONSTRAINT biomaterial_treatment_c1 UNIQUE (biomaterial_id, treatment_id);


--
-- Name: biomaterial_treatment_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY biomaterial_treatment
    ADD CONSTRAINT biomaterial_treatment_pkey PRIMARY KEY (biomaterial_treatment_id);


--
-- Name: biomaterialprop_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY biomaterialprop
    ADD CONSTRAINT biomaterialprop_c1 UNIQUE (biomaterial_id, type_id, rank);


--
-- Name: biomaterialprop_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY biomaterialprop
    ADD CONSTRAINT biomaterialprop_pkey PRIMARY KEY (biomaterialprop_id);


--
-- Name: blast_organisms_blast_org_name_uq_key; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY blast_organisms
    ADD CONSTRAINT blast_organisms_blast_org_name_uq_key UNIQUE (blast_org_name);


--
-- Name: blast_organisms_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY blast_organisms
    ADD CONSTRAINT blast_organisms_pkey PRIMARY KEY (blast_org_id);


--
-- Name: cell_line_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY cell_line
    ADD CONSTRAINT cell_line_c1 UNIQUE (uniquename, organism_id);


--
-- Name: cell_line_cvterm_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY cell_line_cvterm
    ADD CONSTRAINT cell_line_cvterm_c1 UNIQUE (cell_line_id, cvterm_id, pub_id, rank);


--
-- Name: cell_line_cvterm_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY cell_line_cvterm
    ADD CONSTRAINT cell_line_cvterm_pkey PRIMARY KEY (cell_line_cvterm_id);


--
-- Name: cell_line_cvtermprop_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY cell_line_cvtermprop
    ADD CONSTRAINT cell_line_cvtermprop_c1 UNIQUE (cell_line_cvterm_id, type_id, rank);


--
-- Name: cell_line_cvtermprop_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY cell_line_cvtermprop
    ADD CONSTRAINT cell_line_cvtermprop_pkey PRIMARY KEY (cell_line_cvtermprop_id);


--
-- Name: cell_line_dbxref_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY cell_line_dbxref
    ADD CONSTRAINT cell_line_dbxref_c1 UNIQUE (cell_line_id, dbxref_id);


--
-- Name: cell_line_dbxref_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY cell_line_dbxref
    ADD CONSTRAINT cell_line_dbxref_pkey PRIMARY KEY (cell_line_dbxref_id);


--
-- Name: cell_line_feature_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY cell_line_feature
    ADD CONSTRAINT cell_line_feature_c1 UNIQUE (cell_line_id, feature_id, pub_id);


--
-- Name: cell_line_feature_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY cell_line_feature
    ADD CONSTRAINT cell_line_feature_pkey PRIMARY KEY (cell_line_feature_id);


--
-- Name: cell_line_library_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY cell_line_library
    ADD CONSTRAINT cell_line_library_c1 UNIQUE (cell_line_id, library_id, pub_id);


--
-- Name: cell_line_library_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY cell_line_library
    ADD CONSTRAINT cell_line_library_pkey PRIMARY KEY (cell_line_library_id);


--
-- Name: cell_line_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY cell_line
    ADD CONSTRAINT cell_line_pkey PRIMARY KEY (cell_line_id);


--
-- Name: cell_line_pub_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY cell_line_pub
    ADD CONSTRAINT cell_line_pub_c1 UNIQUE (cell_line_id, pub_id);


--
-- Name: cell_line_pub_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY cell_line_pub
    ADD CONSTRAINT cell_line_pub_pkey PRIMARY KEY (cell_line_pub_id);


--
-- Name: cell_line_relationship_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY cell_line_relationship
    ADD CONSTRAINT cell_line_relationship_c1 UNIQUE (subject_id, object_id, type_id);


--
-- Name: cell_line_relationship_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY cell_line_relationship
    ADD CONSTRAINT cell_line_relationship_pkey PRIMARY KEY (cell_line_relationship_id);


--
-- Name: cell_line_synonym_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY cell_line_synonym
    ADD CONSTRAINT cell_line_synonym_c1 UNIQUE (synonym_id, cell_line_id, pub_id);


--
-- Name: cell_line_synonym_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY cell_line_synonym
    ADD CONSTRAINT cell_line_synonym_pkey PRIMARY KEY (cell_line_synonym_id);


--
-- Name: cell_lineprop_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY cell_lineprop
    ADD CONSTRAINT cell_lineprop_c1 UNIQUE (cell_line_id, type_id, rank);


--
-- Name: cell_lineprop_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY cell_lineprop
    ADD CONSTRAINT cell_lineprop_pkey PRIMARY KEY (cell_lineprop_id);


--
-- Name: cell_lineprop_pub_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY cell_lineprop_pub
    ADD CONSTRAINT cell_lineprop_pub_c1 UNIQUE (cell_lineprop_id, pub_id);


--
-- Name: cell_lineprop_pub_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY cell_lineprop_pub
    ADD CONSTRAINT cell_lineprop_pub_pkey PRIMARY KEY (cell_lineprop_pub_id);


--
-- Name: chadoprop_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY chadoprop
    ADD CONSTRAINT chadoprop_c1 UNIQUE (type_id, rank);


--
-- Name: chadoprop_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY chadoprop
    ADD CONSTRAINT chadoprop_pkey PRIMARY KEY (chadoprop_id);


--
-- Name: channel_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY channel
    ADD CONSTRAINT channel_c1 UNIQUE (name);


--
-- Name: channel_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY channel
    ADD CONSTRAINT channel_pkey PRIMARY KEY (channel_id);


--
-- Name: contact_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY contact
    ADD CONSTRAINT contact_c1 UNIQUE (name);


--
-- Name: contact_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY contact
    ADD CONSTRAINT contact_pkey PRIMARY KEY (contact_id);


--
-- Name: contact_relationship_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY contact_relationship
    ADD CONSTRAINT contact_relationship_c1 UNIQUE (subject_id, object_id, type_id);


--
-- Name: contact_relationship_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY contact_relationship
    ADD CONSTRAINT contact_relationship_pkey PRIMARY KEY (contact_relationship_id);


--
-- Name: control_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY control
    ADD CONSTRAINT control_pkey PRIMARY KEY (control_id);


--
-- Name: cv_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY cv
    ADD CONSTRAINT cv_c1 UNIQUE (name);


--
-- Name: cv_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY cv
    ADD CONSTRAINT cv_pkey PRIMARY KEY (cv_id);


--
-- Name: cvprop_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY cvprop
    ADD CONSTRAINT cvprop_c1 UNIQUE (cv_id, type_id, rank);


--
-- Name: cvprop_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY cvprop
    ADD CONSTRAINT cvprop_pkey PRIMARY KEY (cvprop_id);


--
-- Name: cvterm_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY cvterm
    ADD CONSTRAINT cvterm_c1 UNIQUE (name, cv_id, is_obsolete);


--
-- Name: cvterm_c2; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY cvterm
    ADD CONSTRAINT cvterm_c2 UNIQUE (dbxref_id);


--
-- Name: cvterm_dbxref_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY cvterm_dbxref
    ADD CONSTRAINT cvterm_dbxref_c1 UNIQUE (cvterm_id, dbxref_id);


--
-- Name: cvterm_dbxref_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY cvterm_dbxref
    ADD CONSTRAINT cvterm_dbxref_pkey PRIMARY KEY (cvterm_dbxref_id);


--
-- Name: cvterm_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY cvterm
    ADD CONSTRAINT cvterm_pkey PRIMARY KEY (cvterm_id);


--
-- Name: cvterm_relationship_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY cvterm_relationship
    ADD CONSTRAINT cvterm_relationship_c1 UNIQUE (subject_id, object_id, type_id);


--
-- Name: cvterm_relationship_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY cvterm_relationship
    ADD CONSTRAINT cvterm_relationship_pkey PRIMARY KEY (cvterm_relationship_id);


--
-- Name: cvtermpath_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY cvtermpath
    ADD CONSTRAINT cvtermpath_c1 UNIQUE (subject_id, object_id, type_id, pathdistance);


--
-- Name: cvtermpath_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY cvtermpath
    ADD CONSTRAINT cvtermpath_pkey PRIMARY KEY (cvtermpath_id);


--
-- Name: cvtermprop_cvterm_id_type_id_value_rank_key; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY cvtermprop
    ADD CONSTRAINT cvtermprop_cvterm_id_type_id_value_rank_key UNIQUE (cvterm_id, type_id, value, rank);


--
-- Name: cvtermprop_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY cvtermprop
    ADD CONSTRAINT cvtermprop_pkey PRIMARY KEY (cvtermprop_id);


--
-- Name: cvtermsynonym_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY cvtermsynonym
    ADD CONSTRAINT cvtermsynonym_c1 UNIQUE (cvterm_id, synonym);


--
-- Name: cvtermsynonym_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY cvtermsynonym
    ADD CONSTRAINT cvtermsynonym_pkey PRIMARY KEY (cvtermsynonym_id);


--
-- Name: db_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY db
    ADD CONSTRAINT db_c1 UNIQUE (name);


--
-- Name: db_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY db
    ADD CONSTRAINT db_pkey PRIMARY KEY (db_id);


--
-- Name: dbxref_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY dbxref
    ADD CONSTRAINT dbxref_c1 UNIQUE (db_id, accession, version);


--
-- Name: dbxref_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY dbxref
    ADD CONSTRAINT dbxref_pkey PRIMARY KEY (dbxref_id);


--
-- Name: dbxrefprop_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY dbxrefprop
    ADD CONSTRAINT dbxrefprop_c1 UNIQUE (dbxref_id, type_id, rank);


--
-- Name: dbxrefprop_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY dbxrefprop
    ADD CONSTRAINT dbxrefprop_pkey PRIMARY KEY (dbxrefprop_id);


--
-- Name: eimage_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY eimage
    ADD CONSTRAINT eimage_pkey PRIMARY KEY (eimage_id);


--
-- Name: element_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY element
    ADD CONSTRAINT element_c1 UNIQUE (feature_id, arraydesign_id);


--
-- Name: element_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY element
    ADD CONSTRAINT element_pkey PRIMARY KEY (element_id);


--
-- Name: element_relationship_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY element_relationship
    ADD CONSTRAINT element_relationship_c1 UNIQUE (subject_id, object_id, type_id, rank);


--
-- Name: element_relationship_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY element_relationship
    ADD CONSTRAINT element_relationship_pkey PRIMARY KEY (element_relationship_id);


--
-- Name: elementresult_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY elementresult
    ADD CONSTRAINT elementresult_c1 UNIQUE (element_id, quantification_id);


--
-- Name: elementresult_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY elementresult
    ADD CONSTRAINT elementresult_pkey PRIMARY KEY (elementresult_id);


--
-- Name: elementresult_relationship_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY elementresult_relationship
    ADD CONSTRAINT elementresult_relationship_c1 UNIQUE (subject_id, object_id, type_id, rank);


--
-- Name: elementresult_relationship_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY elementresult_relationship
    ADD CONSTRAINT elementresult_relationship_pkey PRIMARY KEY (elementresult_relationship_id);


--
-- Name: environment_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY environment
    ADD CONSTRAINT environment_c1 UNIQUE (uniquename);


--
-- Name: environment_cvterm_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY environment_cvterm
    ADD CONSTRAINT environment_cvterm_c1 UNIQUE (environment_id, cvterm_id);


--
-- Name: environment_cvterm_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY environment_cvterm
    ADD CONSTRAINT environment_cvterm_pkey PRIMARY KEY (environment_cvterm_id);


--
-- Name: environment_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY environment
    ADD CONSTRAINT environment_pkey PRIMARY KEY (environment_id);


--
-- Name: expression_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY expression
    ADD CONSTRAINT expression_c1 UNIQUE (uniquename);


--
-- Name: expression_cvterm_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY expression_cvterm
    ADD CONSTRAINT expression_cvterm_c1 UNIQUE (expression_id, cvterm_id, cvterm_type_id);


--
-- Name: expression_cvterm_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY expression_cvterm
    ADD CONSTRAINT expression_cvterm_pkey PRIMARY KEY (expression_cvterm_id);


--
-- Name: expression_cvtermprop_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY expression_cvtermprop
    ADD CONSTRAINT expression_cvtermprop_c1 UNIQUE (expression_cvterm_id, type_id, rank);


--
-- Name: expression_cvtermprop_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY expression_cvtermprop
    ADD CONSTRAINT expression_cvtermprop_pkey PRIMARY KEY (expression_cvtermprop_id);


--
-- Name: expression_image_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY expression_image
    ADD CONSTRAINT expression_image_c1 UNIQUE (expression_id, eimage_id);


--
-- Name: expression_image_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY expression_image
    ADD CONSTRAINT expression_image_pkey PRIMARY KEY (expression_image_id);


--
-- Name: expression_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY expression
    ADD CONSTRAINT expression_pkey PRIMARY KEY (expression_id);


--
-- Name: expression_pub_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY expression_pub
    ADD CONSTRAINT expression_pub_c1 UNIQUE (expression_id, pub_id);


--
-- Name: expression_pub_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY expression_pub
    ADD CONSTRAINT expression_pub_pkey PRIMARY KEY (expression_pub_id);


--
-- Name: expressionprop_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY expressionprop
    ADD CONSTRAINT expressionprop_c1 UNIQUE (expression_id, type_id, rank);


--
-- Name: expressionprop_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY expressionprop
    ADD CONSTRAINT expressionprop_pkey PRIMARY KEY (expressionprop_id);


--
-- Name: feature_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY feature
    ADD CONSTRAINT feature_c1 UNIQUE (organism_id, uniquename, type_id);


--
-- Name: feature_cvterm_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY feature_cvterm
    ADD CONSTRAINT feature_cvterm_c1 UNIQUE (feature_id, cvterm_id, pub_id, rank);


--
-- Name: feature_cvterm_dbxref_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY feature_cvterm_dbxref
    ADD CONSTRAINT feature_cvterm_dbxref_c1 UNIQUE (feature_cvterm_id, dbxref_id);


--
-- Name: feature_cvterm_dbxref_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY feature_cvterm_dbxref
    ADD CONSTRAINT feature_cvterm_dbxref_pkey PRIMARY KEY (feature_cvterm_dbxref_id);


--
-- Name: feature_cvterm_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY feature_cvterm
    ADD CONSTRAINT feature_cvterm_pkey PRIMARY KEY (feature_cvterm_id);


--
-- Name: feature_cvterm_pub_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY feature_cvterm_pub
    ADD CONSTRAINT feature_cvterm_pub_c1 UNIQUE (feature_cvterm_id, pub_id);


--
-- Name: feature_cvterm_pub_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY feature_cvterm_pub
    ADD CONSTRAINT feature_cvterm_pub_pkey PRIMARY KEY (feature_cvterm_pub_id);


--
-- Name: feature_cvtermprop_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY feature_cvtermprop
    ADD CONSTRAINT feature_cvtermprop_c1 UNIQUE (feature_cvterm_id, type_id, rank);


--
-- Name: feature_cvtermprop_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY feature_cvtermprop
    ADD CONSTRAINT feature_cvtermprop_pkey PRIMARY KEY (feature_cvtermprop_id);


--
-- Name: feature_dbxref_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY feature_dbxref
    ADD CONSTRAINT feature_dbxref_c1 UNIQUE (feature_id, dbxref_id);


--
-- Name: feature_dbxref_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY feature_dbxref
    ADD CONSTRAINT feature_dbxref_pkey PRIMARY KEY (feature_dbxref_id);


--
-- Name: feature_expression_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY feature_expression
    ADD CONSTRAINT feature_expression_c1 UNIQUE (expression_id, feature_id, pub_id);


--
-- Name: feature_expression_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY feature_expression
    ADD CONSTRAINT feature_expression_pkey PRIMARY KEY (feature_expression_id);


--
-- Name: feature_expressionprop_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY feature_expressionprop
    ADD CONSTRAINT feature_expressionprop_c1 UNIQUE (feature_expression_id, type_id, rank);


--
-- Name: feature_expressionprop_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY feature_expressionprop
    ADD CONSTRAINT feature_expressionprop_pkey PRIMARY KEY (feature_expressionprop_id);


--
-- Name: feature_genotype_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY feature_genotype
    ADD CONSTRAINT feature_genotype_c1 UNIQUE (feature_id, genotype_id, cvterm_id, chromosome_id, rank, cgroup);


--
-- Name: feature_genotype_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY feature_genotype
    ADD CONSTRAINT feature_genotype_pkey PRIMARY KEY (feature_genotype_id);


--
-- Name: feature_phenotype_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY feature_phenotype
    ADD CONSTRAINT feature_phenotype_c1 UNIQUE (feature_id, phenotype_id);


--
-- Name: feature_phenotype_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY feature_phenotype
    ADD CONSTRAINT feature_phenotype_pkey PRIMARY KEY (feature_phenotype_id);


--
-- Name: feature_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY feature
    ADD CONSTRAINT feature_pkey PRIMARY KEY (feature_id);


--
-- Name: feature_pub_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY feature_pub
    ADD CONSTRAINT feature_pub_c1 UNIQUE (feature_id, pub_id);


--
-- Name: feature_pub_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY feature_pub
    ADD CONSTRAINT feature_pub_pkey PRIMARY KEY (feature_pub_id);


--
-- Name: feature_pubprop_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY feature_pubprop
    ADD CONSTRAINT feature_pubprop_c1 UNIQUE (feature_pub_id, type_id, rank);


--
-- Name: feature_pubprop_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY feature_pubprop
    ADD CONSTRAINT feature_pubprop_pkey PRIMARY KEY (feature_pubprop_id);


--
-- Name: feature_relationship_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY feature_relationship
    ADD CONSTRAINT feature_relationship_c1 UNIQUE (subject_id, object_id, type_id, rank);


--
-- Name: feature_relationship_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY feature_relationship
    ADD CONSTRAINT feature_relationship_pkey PRIMARY KEY (feature_relationship_id);


--
-- Name: feature_relationship_pub_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY feature_relationship_pub
    ADD CONSTRAINT feature_relationship_pub_c1 UNIQUE (feature_relationship_id, pub_id);


--
-- Name: feature_relationship_pub_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY feature_relationship_pub
    ADD CONSTRAINT feature_relationship_pub_pkey PRIMARY KEY (feature_relationship_pub_id);


--
-- Name: feature_relationshipprop_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY feature_relationshipprop
    ADD CONSTRAINT feature_relationshipprop_c1 UNIQUE (feature_relationship_id, type_id, rank);


--
-- Name: feature_relationshipprop_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY feature_relationshipprop
    ADD CONSTRAINT feature_relationshipprop_pkey PRIMARY KEY (feature_relationshipprop_id);


--
-- Name: feature_relationshipprop_pub_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY feature_relationshipprop_pub
    ADD CONSTRAINT feature_relationshipprop_pub_c1 UNIQUE (feature_relationshipprop_id, pub_id);


--
-- Name: feature_relationshipprop_pub_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY feature_relationshipprop_pub
    ADD CONSTRAINT feature_relationshipprop_pub_pkey PRIMARY KEY (feature_relationshipprop_pub_id);


--
-- Name: feature_synonym_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY feature_synonym
    ADD CONSTRAINT feature_synonym_c1 UNIQUE (synonym_id, feature_id, pub_id);


--
-- Name: feature_synonym_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY feature_synonym
    ADD CONSTRAINT feature_synonym_pkey PRIMARY KEY (feature_synonym_id);


--
-- Name: featureloc_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY featureloc
    ADD CONSTRAINT featureloc_c1 UNIQUE (feature_id, locgroup, rank);


--
-- Name: featureloc_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY featureloc
    ADD CONSTRAINT featureloc_pkey PRIMARY KEY (featureloc_id);


--
-- Name: featureloc_pub_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY featureloc_pub
    ADD CONSTRAINT featureloc_pub_c1 UNIQUE (featureloc_id, pub_id);


--
-- Name: featureloc_pub_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY featureloc_pub
    ADD CONSTRAINT featureloc_pub_pkey PRIMARY KEY (featureloc_pub_id);


--
-- Name: featuremap_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY featuremap
    ADD CONSTRAINT featuremap_c1 UNIQUE (name);


--
-- Name: featuremap_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY featuremap
    ADD CONSTRAINT featuremap_pkey PRIMARY KEY (featuremap_id);


--
-- Name: featuremap_pub_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY featuremap_pub
    ADD CONSTRAINT featuremap_pub_pkey PRIMARY KEY (featuremap_pub_id);


--
-- Name: featurepos_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY featurepos
    ADD CONSTRAINT featurepos_pkey PRIMARY KEY (featurepos_id);


--
-- Name: featureprop_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY featureprop
    ADD CONSTRAINT featureprop_c1 UNIQUE (feature_id, type_id, rank);


--
-- Name: featureprop_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY featureprop
    ADD CONSTRAINT featureprop_pkey PRIMARY KEY (featureprop_id);


--
-- Name: featureprop_pub_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY featureprop_pub
    ADD CONSTRAINT featureprop_pub_c1 UNIQUE (featureprop_id, pub_id);


--
-- Name: featureprop_pub_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY featureprop_pub
    ADD CONSTRAINT featureprop_pub_pkey PRIMARY KEY (featureprop_pub_id);


--
-- Name: featurerange_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY featurerange
    ADD CONSTRAINT featurerange_pkey PRIMARY KEY (featurerange_id);


--
-- Name: genotype_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY genotype
    ADD CONSTRAINT genotype_c1 UNIQUE (uniquename);


--
-- Name: genotype_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY genotype
    ADD CONSTRAINT genotype_pkey PRIMARY KEY (genotype_id);


--
-- Name: genotypeprop_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY genotypeprop
    ADD CONSTRAINT genotypeprop_c1 UNIQUE (genotype_id, type_id, rank);


--
-- Name: genotypeprop_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY genotypeprop
    ADD CONSTRAINT genotypeprop_pkey PRIMARY KEY (genotypeprop_id);


--
-- Name: library_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY library
    ADD CONSTRAINT library_c1 UNIQUE (organism_id, uniquename, type_id);


--
-- Name: library_cvterm_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY library_cvterm
    ADD CONSTRAINT library_cvterm_c1 UNIQUE (library_id, cvterm_id, pub_id);


--
-- Name: library_cvterm_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY library_cvterm
    ADD CONSTRAINT library_cvterm_pkey PRIMARY KEY (library_cvterm_id);


--
-- Name: library_dbxref_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY library_dbxref
    ADD CONSTRAINT library_dbxref_c1 UNIQUE (library_id, dbxref_id);


--
-- Name: library_dbxref_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY library_dbxref
    ADD CONSTRAINT library_dbxref_pkey PRIMARY KEY (library_dbxref_id);


--
-- Name: library_feature_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY library_feature
    ADD CONSTRAINT library_feature_c1 UNIQUE (library_id, feature_id);


--
-- Name: library_feature_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY library_feature
    ADD CONSTRAINT library_feature_pkey PRIMARY KEY (library_feature_id);


--
-- Name: library_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY library
    ADD CONSTRAINT library_pkey PRIMARY KEY (library_id);


--
-- Name: library_pub_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY library_pub
    ADD CONSTRAINT library_pub_c1 UNIQUE (library_id, pub_id);


--
-- Name: library_pub_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY library_pub
    ADD CONSTRAINT library_pub_pkey PRIMARY KEY (library_pub_id);


--
-- Name: library_synonym_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY library_synonym
    ADD CONSTRAINT library_synonym_c1 UNIQUE (synonym_id, library_id, pub_id);


--
-- Name: library_synonym_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY library_synonym
    ADD CONSTRAINT library_synonym_pkey PRIMARY KEY (library_synonym_id);


--
-- Name: libraryprop_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY libraryprop
    ADD CONSTRAINT libraryprop_c1 UNIQUE (library_id, type_id, rank);


--
-- Name: libraryprop_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY libraryprop
    ADD CONSTRAINT libraryprop_pkey PRIMARY KEY (libraryprop_id);


--
-- Name: libraryprop_pub_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY libraryprop_pub
    ADD CONSTRAINT libraryprop_pub_c1 UNIQUE (libraryprop_id, pub_id);


--
-- Name: libraryprop_pub_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY libraryprop_pub
    ADD CONSTRAINT libraryprop_pub_pkey PRIMARY KEY (libraryprop_pub_id);


--
-- Name: magedocumentation_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY magedocumentation
    ADD CONSTRAINT magedocumentation_pkey PRIMARY KEY (magedocumentation_id);


--
-- Name: mageml_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY mageml
    ADD CONSTRAINT mageml_pkey PRIMARY KEY (mageml_id);


--
-- Name: materialized_view_name_key; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY materialized_view
    ADD CONSTRAINT materialized_view_name_key UNIQUE (name);


--
-- Name: nd_experiment_contact_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY nd_experiment_contact
    ADD CONSTRAINT nd_experiment_contact_pkey PRIMARY KEY (nd_experiment_contact_id);


--
-- Name: nd_experiment_dbxref_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY nd_experiment_dbxref
    ADD CONSTRAINT nd_experiment_dbxref_pkey PRIMARY KEY (nd_experiment_dbxref_id);


--
-- Name: nd_experiment_genotype_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY nd_experiment_genotype
    ADD CONSTRAINT nd_experiment_genotype_c1 UNIQUE (nd_experiment_id, genotype_id);


--
-- Name: nd_experiment_genotype_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY nd_experiment_genotype
    ADD CONSTRAINT nd_experiment_genotype_pkey PRIMARY KEY (nd_experiment_genotype_id);


--
-- Name: nd_experiment_phenotype_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY nd_experiment_phenotype
    ADD CONSTRAINT nd_experiment_phenotype_c1 UNIQUE (nd_experiment_id, phenotype_id);


--
-- Name: nd_experiment_phenotype_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY nd_experiment_phenotype
    ADD CONSTRAINT nd_experiment_phenotype_pkey PRIMARY KEY (nd_experiment_phenotype_id);


--
-- Name: nd_experiment_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY nd_experiment
    ADD CONSTRAINT nd_experiment_pkey PRIMARY KEY (nd_experiment_id);


--
-- Name: nd_experiment_project_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY nd_experiment_project
    ADD CONSTRAINT nd_experiment_project_pkey PRIMARY KEY (nd_experiment_project_id);


--
-- Name: nd_experiment_protocol_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY nd_experiment_protocol
    ADD CONSTRAINT nd_experiment_protocol_pkey PRIMARY KEY (nd_experiment_protocol_id);


--
-- Name: nd_experiment_pub_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY nd_experiment_pub
    ADD CONSTRAINT nd_experiment_pub_c1 UNIQUE (nd_experiment_id, pub_id);


--
-- Name: nd_experiment_pub_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY nd_experiment_pub
    ADD CONSTRAINT nd_experiment_pub_pkey PRIMARY KEY (nd_experiment_pub_id);


--
-- Name: nd_experiment_stock_dbxref_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY nd_experiment_stock_dbxref
    ADD CONSTRAINT nd_experiment_stock_dbxref_pkey PRIMARY KEY (nd_experiment_stock_dbxref_id);


--
-- Name: nd_experiment_stock_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY nd_experiment_stock
    ADD CONSTRAINT nd_experiment_stock_pkey PRIMARY KEY (nd_experiment_stock_id);


--
-- Name: nd_experiment_stockprop_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY nd_experiment_stockprop
    ADD CONSTRAINT nd_experiment_stockprop_c1 UNIQUE (nd_experiment_stock_id, type_id, rank);


--
-- Name: nd_experiment_stockprop_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY nd_experiment_stockprop
    ADD CONSTRAINT nd_experiment_stockprop_pkey PRIMARY KEY (nd_experiment_stockprop_id);


--
-- Name: nd_experimentprop_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY nd_experimentprop
    ADD CONSTRAINT nd_experimentprop_c1 UNIQUE (nd_experiment_id, type_id, rank);


--
-- Name: nd_experimentprop_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY nd_experimentprop
    ADD CONSTRAINT nd_experimentprop_pkey PRIMARY KEY (nd_experimentprop_id);


--
-- Name: nd_geolocation_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY nd_geolocation
    ADD CONSTRAINT nd_geolocation_pkey PRIMARY KEY (nd_geolocation_id);


--
-- Name: nd_geolocationprop_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY nd_geolocationprop
    ADD CONSTRAINT nd_geolocationprop_c1 UNIQUE (nd_geolocation_id, type_id, rank);


--
-- Name: nd_geolocationprop_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY nd_geolocationprop
    ADD CONSTRAINT nd_geolocationprop_pkey PRIMARY KEY (nd_geolocationprop_id);


--
-- Name: nd_protocol_name_key; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY nd_protocol
    ADD CONSTRAINT nd_protocol_name_key UNIQUE (name);


--
-- Name: nd_protocol_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY nd_protocol
    ADD CONSTRAINT nd_protocol_pkey PRIMARY KEY (nd_protocol_id);


--
-- Name: nd_protocol_reagent_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY nd_protocol_reagent
    ADD CONSTRAINT nd_protocol_reagent_pkey PRIMARY KEY (nd_protocol_reagent_id);


--
-- Name: nd_protocolprop_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY nd_protocolprop
    ADD CONSTRAINT nd_protocolprop_c1 UNIQUE (nd_protocol_id, type_id, rank);


--
-- Name: nd_protocolprop_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY nd_protocolprop
    ADD CONSTRAINT nd_protocolprop_pkey PRIMARY KEY (nd_protocolprop_id);


--
-- Name: nd_reagent_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY nd_reagent
    ADD CONSTRAINT nd_reagent_pkey PRIMARY KEY (nd_reagent_id);


--
-- Name: nd_reagent_relationship_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY nd_reagent_relationship
    ADD CONSTRAINT nd_reagent_relationship_pkey PRIMARY KEY (nd_reagent_relationship_id);


--
-- Name: nd_reagentprop_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY nd_reagentprop
    ADD CONSTRAINT nd_reagentprop_c1 UNIQUE (nd_reagent_id, type_id, rank);


--
-- Name: nd_reagentprop_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY nd_reagentprop
    ADD CONSTRAINT nd_reagentprop_pkey PRIMARY KEY (nd_reagentprop_id);


--
-- Name: organism_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY organism
    ADD CONSTRAINT organism_c1 UNIQUE (genus, species);


--
-- Name: organism_dbxref_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY organism_dbxref
    ADD CONSTRAINT organism_dbxref_c1 UNIQUE (organism_id, dbxref_id);


--
-- Name: organism_dbxref_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY organism_dbxref
    ADD CONSTRAINT organism_dbxref_pkey PRIMARY KEY (organism_dbxref_id);


--
-- Name: organism_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY organism
    ADD CONSTRAINT organism_pkey PRIMARY KEY (organism_id);


--
-- Name: organismprop_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY organismprop
    ADD CONSTRAINT organismprop_c1 UNIQUE (organism_id, type_id, rank);


--
-- Name: organismprop_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY organismprop
    ADD CONSTRAINT organismprop_pkey PRIMARY KEY (organismprop_id);


--
-- Name: phendesc_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY phendesc
    ADD CONSTRAINT phendesc_c1 UNIQUE (genotype_id, environment_id, type_id, pub_id);


--
-- Name: phendesc_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY phendesc
    ADD CONSTRAINT phendesc_pkey PRIMARY KEY (phendesc_id);


--
-- Name: phenotype_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY phenotype
    ADD CONSTRAINT phenotype_c1 UNIQUE (uniquename);


--
-- Name: phenotype_comparison_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY phenotype_comparison
    ADD CONSTRAINT phenotype_comparison_c1 UNIQUE (genotype1_id, environment1_id, genotype2_id, environment2_id, phenotype1_id, pub_id);


--
-- Name: phenotype_comparison_cvterm_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY phenotype_comparison_cvterm
    ADD CONSTRAINT phenotype_comparison_cvterm_c1 UNIQUE (phenotype_comparison_id, cvterm_id);


--
-- Name: phenotype_comparison_cvterm_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY phenotype_comparison_cvterm
    ADD CONSTRAINT phenotype_comparison_cvterm_pkey PRIMARY KEY (phenotype_comparison_cvterm_id);


--
-- Name: phenotype_comparison_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY phenotype_comparison
    ADD CONSTRAINT phenotype_comparison_pkey PRIMARY KEY (phenotype_comparison_id);


--
-- Name: phenotype_cvterm_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY phenotype_cvterm
    ADD CONSTRAINT phenotype_cvterm_c1 UNIQUE (phenotype_id, cvterm_id, rank);


--
-- Name: phenotype_cvterm_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY phenotype_cvterm
    ADD CONSTRAINT phenotype_cvterm_pkey PRIMARY KEY (phenotype_cvterm_id);


--
-- Name: phenotype_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY phenotype
    ADD CONSTRAINT phenotype_pkey PRIMARY KEY (phenotype_id);


--
-- Name: phenstatement_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY phenstatement
    ADD CONSTRAINT phenstatement_c1 UNIQUE (genotype_id, phenotype_id, environment_id, type_id, pub_id);


--
-- Name: phenstatement_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY phenstatement
    ADD CONSTRAINT phenstatement_pkey PRIMARY KEY (phenstatement_id);


--
-- Name: phylonode_dbxref_phylonode_id_dbxref_id_key; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY phylonode_dbxref
    ADD CONSTRAINT phylonode_dbxref_phylonode_id_dbxref_id_key UNIQUE (phylonode_id, dbxref_id);


--
-- Name: phylonode_dbxref_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY phylonode_dbxref
    ADD CONSTRAINT phylonode_dbxref_pkey PRIMARY KEY (phylonode_dbxref_id);


--
-- Name: phylonode_organism_phylonode_id_key; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY phylonode_organism
    ADD CONSTRAINT phylonode_organism_phylonode_id_key UNIQUE (phylonode_id);


--
-- Name: phylonode_organism_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY phylonode_organism
    ADD CONSTRAINT phylonode_organism_pkey PRIMARY KEY (phylonode_organism_id);


--
-- Name: phylonode_phylotree_id_left_idx_key; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY phylonode
    ADD CONSTRAINT phylonode_phylotree_id_left_idx_key UNIQUE (phylotree_id, left_idx);


--
-- Name: phylonode_phylotree_id_right_idx_key; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY phylonode
    ADD CONSTRAINT phylonode_phylotree_id_right_idx_key UNIQUE (phylotree_id, right_idx);


--
-- Name: phylonode_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY phylonode
    ADD CONSTRAINT phylonode_pkey PRIMARY KEY (phylonode_id);


--
-- Name: phylonode_pub_phylonode_id_pub_id_key; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY phylonode_pub
    ADD CONSTRAINT phylonode_pub_phylonode_id_pub_id_key UNIQUE (phylonode_id, pub_id);


--
-- Name: phylonode_pub_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY phylonode_pub
    ADD CONSTRAINT phylonode_pub_pkey PRIMARY KEY (phylonode_pub_id);


--
-- Name: phylonode_relationship_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY phylonode_relationship
    ADD CONSTRAINT phylonode_relationship_pkey PRIMARY KEY (phylonode_relationship_id);


--
-- Name: phylonode_relationship_subject_id_object_id_type_id_key; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY phylonode_relationship
    ADD CONSTRAINT phylonode_relationship_subject_id_object_id_type_id_key UNIQUE (subject_id, object_id, type_id);


--
-- Name: phylonodeprop_phylonode_id_type_id_value_rank_key; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY phylonodeprop
    ADD CONSTRAINT phylonodeprop_phylonode_id_type_id_value_rank_key UNIQUE (phylonode_id, type_id, value, rank);


--
-- Name: phylonodeprop_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY phylonodeprop
    ADD CONSTRAINT phylonodeprop_pkey PRIMARY KEY (phylonodeprop_id);


--
-- Name: phylotree_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY phylotree
    ADD CONSTRAINT phylotree_pkey PRIMARY KEY (phylotree_id);


--
-- Name: phylotree_pub_phylotree_id_pub_id_key; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY phylotree_pub
    ADD CONSTRAINT phylotree_pub_phylotree_id_pub_id_key UNIQUE (phylotree_id, pub_id);


--
-- Name: phylotree_pub_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY phylotree_pub
    ADD CONSTRAINT phylotree_pub_pkey PRIMARY KEY (phylotree_pub_id);


--
-- Name: project_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY project
    ADD CONSTRAINT project_c1 UNIQUE (name);


--
-- Name: project_contact_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY project_contact
    ADD CONSTRAINT project_contact_c1 UNIQUE (project_id, contact_id);


--
-- Name: project_contact_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY project_contact
    ADD CONSTRAINT project_contact_pkey PRIMARY KEY (project_contact_id);


--
-- Name: project_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY project
    ADD CONSTRAINT project_pkey PRIMARY KEY (project_id);


--
-- Name: project_pub_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY project_pub
    ADD CONSTRAINT project_pub_c1 UNIQUE (project_id, pub_id);


--
-- Name: project_pub_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY project_pub
    ADD CONSTRAINT project_pub_pkey PRIMARY KEY (project_pub_id);


--
-- Name: project_relationship_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY project_relationship
    ADD CONSTRAINT project_relationship_c1 UNIQUE (subject_project_id, object_project_id, type_id);


--
-- Name: project_relationship_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY project_relationship
    ADD CONSTRAINT project_relationship_pkey PRIMARY KEY (project_relationship_id);


--
-- Name: projectprop_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY projectprop
    ADD CONSTRAINT projectprop_c1 UNIQUE (project_id, type_id, rank);


--
-- Name: projectprop_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY projectprop
    ADD CONSTRAINT projectprop_pkey PRIMARY KEY (projectprop_id);


--
-- Name: protocol_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY protocol
    ADD CONSTRAINT protocol_c1 UNIQUE (name);


--
-- Name: protocol_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY protocol
    ADD CONSTRAINT protocol_pkey PRIMARY KEY (protocol_id);


--
-- Name: protocolparam_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY protocolparam
    ADD CONSTRAINT protocolparam_pkey PRIMARY KEY (protocolparam_id);


--
-- Name: pub_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY pub
    ADD CONSTRAINT pub_c1 UNIQUE (uniquename);


--
-- Name: pub_dbxref_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY pub_dbxref
    ADD CONSTRAINT pub_dbxref_c1 UNIQUE (pub_id, dbxref_id);


--
-- Name: pub_dbxref_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY pub_dbxref
    ADD CONSTRAINT pub_dbxref_pkey PRIMARY KEY (pub_dbxref_id);


--
-- Name: pub_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY pub
    ADD CONSTRAINT pub_pkey PRIMARY KEY (pub_id);


--
-- Name: pub_relationship_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY pub_relationship
    ADD CONSTRAINT pub_relationship_c1 UNIQUE (subject_id, object_id, type_id);


--
-- Name: pub_relationship_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY pub_relationship
    ADD CONSTRAINT pub_relationship_pkey PRIMARY KEY (pub_relationship_id);


--
-- Name: pubauthor_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY pubauthor
    ADD CONSTRAINT pubauthor_c1 UNIQUE (pub_id, rank);


--
-- Name: pubauthor_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY pubauthor
    ADD CONSTRAINT pubauthor_pkey PRIMARY KEY (pubauthor_id);


--
-- Name: pubprop_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY pubprop
    ADD CONSTRAINT pubprop_c1 UNIQUE (pub_id, type_id, rank);


--
-- Name: pubprop_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY pubprop
    ADD CONSTRAINT pubprop_pkey PRIMARY KEY (pubprop_id);


--
-- Name: quantification_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY quantification
    ADD CONSTRAINT quantification_c1 UNIQUE (name, analysis_id);


--
-- Name: quantification_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY quantification
    ADD CONSTRAINT quantification_pkey PRIMARY KEY (quantification_id);


--
-- Name: quantification_relationship_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY quantification_relationship
    ADD CONSTRAINT quantification_relationship_c1 UNIQUE (subject_id, object_id, type_id);


--
-- Name: quantification_relationship_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY quantification_relationship
    ADD CONSTRAINT quantification_relationship_pkey PRIMARY KEY (quantification_relationship_id);


--
-- Name: quantificationprop_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY quantificationprop
    ADD CONSTRAINT quantificationprop_c1 UNIQUE (quantification_id, type_id, rank);


--
-- Name: quantificationprop_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY quantificationprop
    ADD CONSTRAINT quantificationprop_pkey PRIMARY KEY (quantificationprop_id);


--
-- Name: stock_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY stock
    ADD CONSTRAINT stock_c1 UNIQUE (organism_id, uniquename, type_id);


--
-- Name: stock_cvterm_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY stock_cvterm
    ADD CONSTRAINT stock_cvterm_c1 UNIQUE (stock_id, cvterm_id, pub_id, rank);


--
-- Name: stock_cvterm_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY stock_cvterm
    ADD CONSTRAINT stock_cvterm_pkey PRIMARY KEY (stock_cvterm_id);


--
-- Name: stock_cvtermprop_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY stock_cvtermprop
    ADD CONSTRAINT stock_cvtermprop_c1 UNIQUE (stock_cvterm_id, type_id, rank);


--
-- Name: stock_cvtermprop_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY stock_cvtermprop
    ADD CONSTRAINT stock_cvtermprop_pkey PRIMARY KEY (stock_cvtermprop_id);


--
-- Name: stock_dbxref_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY stock_dbxref
    ADD CONSTRAINT stock_dbxref_c1 UNIQUE (stock_id, dbxref_id);


--
-- Name: stock_dbxref_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY stock_dbxref
    ADD CONSTRAINT stock_dbxref_pkey PRIMARY KEY (stock_dbxref_id);


--
-- Name: stock_dbxrefprop_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY stock_dbxrefprop
    ADD CONSTRAINT stock_dbxrefprop_c1 UNIQUE (stock_dbxref_id, type_id, rank);


--
-- Name: stock_dbxrefprop_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY stock_dbxrefprop
    ADD CONSTRAINT stock_dbxrefprop_pkey PRIMARY KEY (stock_dbxrefprop_id);


--
-- Name: stock_genotype_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY stock_genotype
    ADD CONSTRAINT stock_genotype_c1 UNIQUE (stock_id, genotype_id);


--
-- Name: stock_genotype_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY stock_genotype
    ADD CONSTRAINT stock_genotype_pkey PRIMARY KEY (stock_genotype_id);


--
-- Name: stock_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY stock
    ADD CONSTRAINT stock_pkey PRIMARY KEY (stock_id);


--
-- Name: stock_pub_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY stock_pub
    ADD CONSTRAINT stock_pub_c1 UNIQUE (stock_id, pub_id);


--
-- Name: stock_pub_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY stock_pub
    ADD CONSTRAINT stock_pub_pkey PRIMARY KEY (stock_pub_id);


--
-- Name: stock_relationship_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY stock_relationship
    ADD CONSTRAINT stock_relationship_c1 UNIQUE (subject_id, object_id, type_id, rank);


--
-- Name: stock_relationship_cvterm_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY stock_relationship_cvterm
    ADD CONSTRAINT stock_relationship_cvterm_pkey PRIMARY KEY (stock_relationship_cvterm_id);


--
-- Name: stock_relationship_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY stock_relationship
    ADD CONSTRAINT stock_relationship_pkey PRIMARY KEY (stock_relationship_id);


--
-- Name: stock_relationship_pub_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY stock_relationship_pub
    ADD CONSTRAINT stock_relationship_pub_c1 UNIQUE (stock_relationship_id, pub_id);


--
-- Name: stock_relationship_pub_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY stock_relationship_pub
    ADD CONSTRAINT stock_relationship_pub_pkey PRIMARY KEY (stock_relationship_pub_id);


--
-- Name: stockcollection_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY stockcollection
    ADD CONSTRAINT stockcollection_c1 UNIQUE (uniquename, type_id);


--
-- Name: stockcollection_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY stockcollection
    ADD CONSTRAINT stockcollection_pkey PRIMARY KEY (stockcollection_id);


--
-- Name: stockcollection_stock_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY stockcollection_stock
    ADD CONSTRAINT stockcollection_stock_c1 UNIQUE (stockcollection_id, stock_id);


--
-- Name: stockcollection_stock_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY stockcollection_stock
    ADD CONSTRAINT stockcollection_stock_pkey PRIMARY KEY (stockcollection_stock_id);


--
-- Name: stockcollectionprop_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY stockcollectionprop
    ADD CONSTRAINT stockcollectionprop_c1 UNIQUE (stockcollection_id, type_id, rank);


--
-- Name: stockcollectionprop_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY stockcollectionprop
    ADD CONSTRAINT stockcollectionprop_pkey PRIMARY KEY (stockcollectionprop_id);


--
-- Name: stockprop_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY stockprop
    ADD CONSTRAINT stockprop_c1 UNIQUE (stock_id, type_id, rank);


--
-- Name: stockprop_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY stockprop
    ADD CONSTRAINT stockprop_pkey PRIMARY KEY (stockprop_id);


--
-- Name: stockprop_pub_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY stockprop_pub
    ADD CONSTRAINT stockprop_pub_c1 UNIQUE (stockprop_id, pub_id);


--
-- Name: stockprop_pub_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY stockprop_pub
    ADD CONSTRAINT stockprop_pub_pkey PRIMARY KEY (stockprop_pub_id);


--
-- Name: study_assay_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY study_assay
    ADD CONSTRAINT study_assay_c1 UNIQUE (study_id, assay_id);


--
-- Name: study_assay_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY study_assay
    ADD CONSTRAINT study_assay_pkey PRIMARY KEY (study_assay_id);


--
-- Name: study_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY study
    ADD CONSTRAINT study_c1 UNIQUE (name);


--
-- Name: study_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY study
    ADD CONSTRAINT study_pkey PRIMARY KEY (study_id);


--
-- Name: studydesign_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY studydesign
    ADD CONSTRAINT studydesign_pkey PRIMARY KEY (studydesign_id);


--
-- Name: studydesignprop_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY studydesignprop
    ADD CONSTRAINT studydesignprop_c1 UNIQUE (studydesign_id, type_id, rank);


--
-- Name: studydesignprop_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY studydesignprop
    ADD CONSTRAINT studydesignprop_pkey PRIMARY KEY (studydesignprop_id);


--
-- Name: studyfactor_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY studyfactor
    ADD CONSTRAINT studyfactor_pkey PRIMARY KEY (studyfactor_id);


--
-- Name: studyfactorvalue_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY studyfactorvalue
    ADD CONSTRAINT studyfactorvalue_pkey PRIMARY KEY (studyfactorvalue_id);


--
-- Name: studyprop_feature_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY studyprop_feature
    ADD CONSTRAINT studyprop_feature_pkey PRIMARY KEY (studyprop_feature_id);


--
-- Name: studyprop_feature_studyprop_id_feature_id_key; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY studyprop_feature
    ADD CONSTRAINT studyprop_feature_studyprop_id_feature_id_key UNIQUE (studyprop_id, feature_id);


--
-- Name: studyprop_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY studyprop
    ADD CONSTRAINT studyprop_pkey PRIMARY KEY (studyprop_id);


--
-- Name: studyprop_study_id_type_id_rank_key; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY studyprop
    ADD CONSTRAINT studyprop_study_id_type_id_rank_key UNIQUE (study_id, type_id, rank);


--
-- Name: synonym_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY synonym
    ADD CONSTRAINT synonym_c1 UNIQUE (name, type_id);


--
-- Name: synonym_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY synonym
    ADD CONSTRAINT synonym_pkey PRIMARY KEY (synonym_id);


--
-- Name: tableinfo_c1; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY tableinfo
    ADD CONSTRAINT tableinfo_c1 UNIQUE (name);


--
-- Name: tableinfo_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY tableinfo
    ADD CONSTRAINT tableinfo_pkey PRIMARY KEY (tableinfo_id);


--
-- Name: tmp_cds_handler_pkey; Type: CONSTRAINT; Schema: chado; Owner: ubuntu; Tablespace: 
--

ALTER TABLE ONLY tmp_cds_handler
    ADD CONSTRAINT tmp_cds_handler_pkey PRIMARY KEY (cds_row_id);


--
-- Name: tmp_cds_handler_relationship_pkey; Type: CONSTRAINT; Schema: chado; Owner: ubuntu; Tablespace: 
--

ALTER TABLE ONLY tmp_cds_handler_relationship
    ADD CONSTRAINT tmp_cds_handler_relationship_pkey PRIMARY KEY (rel_row_id);


--
-- Name: treatment_pkey; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY treatment
    ADD CONSTRAINT treatment_pkey PRIMARY KEY (treatment_id);


--
-- Name: tripal_gff_temp_tripal_gff_temp_uq0_key; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY tripal_gff_temp
    ADD CONSTRAINT tripal_gff_temp_tripal_gff_temp_uq0_key UNIQUE (feature_id);


--
-- Name: tripal_gff_temp_tripal_gff_temp_uq1_key; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY tripal_gff_temp
    ADD CONSTRAINT tripal_gff_temp_tripal_gff_temp_uq1_key UNIQUE (uniquename, organism_id, type_name);


--
-- Name: tripal_obo_temp_tripal_obo_temp_uq0_key; Type: CONSTRAINT; Schema: chado; Owner: nathandunn; Tablespace: 
--

ALTER TABLE ONLY tripal_obo_temp
    ADD CONSTRAINT tripal_obo_temp_tripal_obo_temp_uq0_key UNIQUE (id);


--
-- Name: acquisition_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX acquisition_idx1 ON acquisition USING btree (assay_id);


--
-- Name: acquisition_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX acquisition_idx2 ON acquisition USING btree (protocol_id);


--
-- Name: acquisition_idx3; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX acquisition_idx3 ON acquisition USING btree (channel_id);


--
-- Name: acquisition_relationship_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX acquisition_relationship_idx1 ON acquisition_relationship USING btree (subject_id);


--
-- Name: acquisition_relationship_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX acquisition_relationship_idx2 ON acquisition_relationship USING btree (type_id);


--
-- Name: acquisition_relationship_idx3; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX acquisition_relationship_idx3 ON acquisition_relationship USING btree (object_id);


--
-- Name: acquisitionprop_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX acquisitionprop_idx1 ON acquisitionprop USING btree (acquisition_id);


--
-- Name: acquisitionprop_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX acquisitionprop_idx2 ON acquisitionprop USING btree (type_id);


--
-- Name: all_feature_names_feature_id; Type: INDEX; Schema: chado; Owner: ubuntu; Tablespace: 
--

CREATE INDEX all_feature_names_feature_id ON all_feature_names USING btree (feature_id);


--
-- Name: all_feature_names_name; Type: INDEX; Schema: chado; Owner: ubuntu; Tablespace: 
--

CREATE INDEX all_feature_names_name ON all_feature_names USING btree (name);


--
-- Name: analysis_organism_networkmod_qtl_indx0_idx; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX analysis_organism_networkmod_qtl_indx0_idx ON analysis_organism USING btree (analysis_id);


--
-- Name: analysis_organism_networkmod_qtl_indx1_idx; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX analysis_organism_networkmod_qtl_indx1_idx ON analysis_organism USING btree (organism_id);


--
-- Name: analysisfeature_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX analysisfeature_idx1 ON analysisfeature USING btree (feature_id);


--
-- Name: analysisfeature_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX analysisfeature_idx2 ON analysisfeature USING btree (analysis_id);


--
-- Name: analysisprop_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX analysisprop_idx1 ON analysisprop USING btree (analysis_id);


--
-- Name: analysisprop_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX analysisprop_idx2 ON analysisprop USING btree (type_id);


--
-- Name: arraydesign_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX arraydesign_idx1 ON arraydesign USING btree (manufacturer_id);


--
-- Name: arraydesign_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX arraydesign_idx2 ON arraydesign USING btree (platformtype_id);


--
-- Name: arraydesign_idx3; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX arraydesign_idx3 ON arraydesign USING btree (substratetype_id);


--
-- Name: arraydesign_idx4; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX arraydesign_idx4 ON arraydesign USING btree (protocol_id);


--
-- Name: arraydesign_idx5; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX arraydesign_idx5 ON arraydesign USING btree (dbxref_id);


--
-- Name: arraydesignprop_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX arraydesignprop_idx1 ON arraydesignprop USING btree (arraydesign_id);


--
-- Name: arraydesignprop_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX arraydesignprop_idx2 ON arraydesignprop USING btree (type_id);


--
-- Name: assay_biomaterial_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX assay_biomaterial_idx1 ON assay_biomaterial USING btree (assay_id);


--
-- Name: assay_biomaterial_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX assay_biomaterial_idx2 ON assay_biomaterial USING btree (biomaterial_id);


--
-- Name: assay_biomaterial_idx3; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX assay_biomaterial_idx3 ON assay_biomaterial USING btree (channel_id);


--
-- Name: assay_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX assay_idx1 ON assay USING btree (arraydesign_id);


--
-- Name: assay_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX assay_idx2 ON assay USING btree (protocol_id);


--
-- Name: assay_idx3; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX assay_idx3 ON assay USING btree (operator_id);


--
-- Name: assay_idx4; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX assay_idx4 ON assay USING btree (dbxref_id);


--
-- Name: assay_project_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX assay_project_idx1 ON assay_project USING btree (assay_id);


--
-- Name: assay_project_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX assay_project_idx2 ON assay_project USING btree (project_id);


--
-- Name: assayprop_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX assayprop_idx1 ON assayprop USING btree (assay_id);


--
-- Name: assayprop_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX assayprop_idx2 ON assayprop USING btree (type_id);


--
-- Name: binloc_boxrange; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX binloc_boxrange ON featureloc USING gist (boxrange(fmin, fmax));


--
-- Name: binloc_boxrange_src; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX binloc_boxrange_src ON featureloc USING gist (boxrange(srcfeature_id, fmin, fmax));


--
-- Name: biomaterial_dbxref_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX biomaterial_dbxref_idx1 ON biomaterial_dbxref USING btree (biomaterial_id);


--
-- Name: biomaterial_dbxref_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX biomaterial_dbxref_idx2 ON biomaterial_dbxref USING btree (dbxref_id);


--
-- Name: biomaterial_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX biomaterial_idx1 ON biomaterial USING btree (taxon_id);


--
-- Name: biomaterial_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX biomaterial_idx2 ON biomaterial USING btree (biosourceprovider_id);


--
-- Name: biomaterial_idx3; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX biomaterial_idx3 ON biomaterial USING btree (dbxref_id);


--
-- Name: biomaterial_relationship_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX biomaterial_relationship_idx1 ON biomaterial_relationship USING btree (subject_id);


--
-- Name: biomaterial_relationship_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX biomaterial_relationship_idx2 ON biomaterial_relationship USING btree (object_id);


--
-- Name: biomaterial_relationship_idx3; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX biomaterial_relationship_idx3 ON biomaterial_relationship USING btree (type_id);


--
-- Name: biomaterial_treatment_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX biomaterial_treatment_idx1 ON biomaterial_treatment USING btree (biomaterial_id);


--
-- Name: biomaterial_treatment_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX biomaterial_treatment_idx2 ON biomaterial_treatment USING btree (treatment_id);


--
-- Name: biomaterial_treatment_idx3; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX biomaterial_treatment_idx3 ON biomaterial_treatment USING btree (unittype_id);


--
-- Name: biomaterialprop_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX biomaterialprop_idx1 ON biomaterialprop USING btree (biomaterial_id);


--
-- Name: biomaterialprop_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX biomaterialprop_idx2 ON biomaterialprop USING btree (type_id);


--
-- Name: blast_hit_data_analysis_id_idx; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX blast_hit_data_analysis_id_idx ON blast_hit_data USING btree (analysis_id);


--
-- Name: blast_hit_data_analysisfeature_id_idx; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX blast_hit_data_analysisfeature_id_idx ON blast_hit_data USING btree (analysisfeature_id);


--
-- Name: blast_hit_data_blast_org_id_idx; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX blast_hit_data_blast_org_id_idx ON blast_hit_data USING btree (blast_org_id);


--
-- Name: blast_hit_data_db_id_idx; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX blast_hit_data_db_id_idx ON blast_hit_data USING btree (db_id);


--
-- Name: blast_hit_data_feature_id_idx; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX blast_hit_data_feature_id_idx ON blast_hit_data USING btree (feature_id);


--
-- Name: blast_hit_data_hit_accession_idx; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX blast_hit_data_hit_accession_idx ON blast_hit_data USING btree (hit_accession);


--
-- Name: blast_hit_data_hit_best_eval_idx; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX blast_hit_data_hit_best_eval_idx ON blast_hit_data USING btree (hit_best_eval);


--
-- Name: blast_hit_data_hit_name_idx; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX blast_hit_data_hit_name_idx ON blast_hit_data USING btree (hit_organism);


--
-- Name: blast_hit_data_hit_organism_idx; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX blast_hit_data_hit_organism_idx ON blast_hit_data USING btree (hit_organism);


--
-- Name: blast_organisms_blast_org_name_idx_idx; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX blast_organisms_blast_org_name_idx_idx ON blast_organisms USING btree (blast_org_name);


--
-- Name: contact_relationship_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX contact_relationship_idx1 ON contact_relationship USING btree (type_id);


--
-- Name: contact_relationship_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX contact_relationship_idx2 ON contact_relationship USING btree (subject_id);


--
-- Name: contact_relationship_idx3; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX contact_relationship_idx3 ON contact_relationship USING btree (object_id);


--
-- Name: control_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX control_idx1 ON control USING btree (type_id);


--
-- Name: control_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX control_idx2 ON control USING btree (assay_id);


--
-- Name: control_idx3; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX control_idx3 ON control USING btree (tableinfo_id);


--
-- Name: control_idx4; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX control_idx4 ON control USING btree (row_id);


--
-- Name: INDEX cvterm_c1; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON INDEX cvterm_c1 IS 'A name can mean different things in
different contexts; for example "chromosome" in SO and GO. A name
should be unique within an ontology or cv. A name may exist twice in a
cv, in both obsolete and non-obsolete forms - these will be for
different cvterms with different OBO identifiers; so GO documentation
for more details on obsoletion. Note that occasionally multiple
obsolete terms with the same name will exist in the same cv. If this
is a possibility for the ontology under consideration (e.g. GO) then the
ID should be appended to the name to ensure uniqueness.';


--
-- Name: INDEX cvterm_c2; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON INDEX cvterm_c2 IS 'The OBO identifier is globally unique.';


--
-- Name: cvterm_dbxref_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX cvterm_dbxref_idx1 ON cvterm_dbxref USING btree (cvterm_id);


--
-- Name: cvterm_dbxref_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX cvterm_dbxref_idx2 ON cvterm_dbxref USING btree (dbxref_id);


--
-- Name: cvterm_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX cvterm_idx1 ON cvterm USING btree (cv_id);


--
-- Name: cvterm_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX cvterm_idx2 ON cvterm USING btree (name);


--
-- Name: cvterm_idx3; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX cvterm_idx3 ON cvterm USING btree (dbxref_id);


--
-- Name: cvterm_relationship_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX cvterm_relationship_idx1 ON cvterm_relationship USING btree (type_id);


--
-- Name: cvterm_relationship_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX cvterm_relationship_idx2 ON cvterm_relationship USING btree (subject_id);


--
-- Name: cvterm_relationship_idx3; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX cvterm_relationship_idx3 ON cvterm_relationship USING btree (object_id);


--
-- Name: cvtermpath_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX cvtermpath_idx1 ON cvtermpath USING btree (type_id);


--
-- Name: cvtermpath_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX cvtermpath_idx2 ON cvtermpath USING btree (subject_id);


--
-- Name: cvtermpath_idx3; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX cvtermpath_idx3 ON cvtermpath USING btree (object_id);


--
-- Name: cvtermpath_idx4; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX cvtermpath_idx4 ON cvtermpath USING btree (cv_id);


--
-- Name: cvtermprop_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX cvtermprop_idx1 ON cvtermprop USING btree (cvterm_id);


--
-- Name: cvtermprop_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX cvtermprop_idx2 ON cvtermprop USING btree (type_id);


--
-- Name: cvtermsynonym_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX cvtermsynonym_idx1 ON cvtermsynonym USING btree (cvterm_id);


--
-- Name: dbxref_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX dbxref_idx1 ON dbxref USING btree (db_id);


--
-- Name: dbxref_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX dbxref_idx2 ON dbxref USING btree (accession);


--
-- Name: dbxref_idx3; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX dbxref_idx3 ON dbxref USING btree (version);


--
-- Name: dbxrefprop_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX dbxrefprop_idx1 ON dbxrefprop USING btree (dbxref_id);


--
-- Name: dbxrefprop_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX dbxrefprop_idx2 ON dbxrefprop USING btree (type_id);


--
-- Name: element_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX element_idx1 ON element USING btree (feature_id);


--
-- Name: element_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX element_idx2 ON element USING btree (arraydesign_id);


--
-- Name: element_idx3; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX element_idx3 ON element USING btree (type_id);


--
-- Name: element_idx4; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX element_idx4 ON element USING btree (dbxref_id);


--
-- Name: element_relationship_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX element_relationship_idx1 ON element_relationship USING btree (subject_id);


--
-- Name: element_relationship_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX element_relationship_idx2 ON element_relationship USING btree (type_id);


--
-- Name: element_relationship_idx3; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX element_relationship_idx3 ON element_relationship USING btree (object_id);


--
-- Name: element_relationship_idx4; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX element_relationship_idx4 ON element_relationship USING btree (value);


--
-- Name: elementresult_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX elementresult_idx1 ON elementresult USING btree (element_id);


--
-- Name: elementresult_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX elementresult_idx2 ON elementresult USING btree (quantification_id);


--
-- Name: elementresult_idx3; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX elementresult_idx3 ON elementresult USING btree (signal);


--
-- Name: elementresult_relationship_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX elementresult_relationship_idx1 ON elementresult_relationship USING btree (subject_id);


--
-- Name: elementresult_relationship_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX elementresult_relationship_idx2 ON elementresult_relationship USING btree (type_id);


--
-- Name: elementresult_relationship_idx3; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX elementresult_relationship_idx3 ON elementresult_relationship USING btree (object_id);


--
-- Name: elementresult_relationship_idx4; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX elementresult_relationship_idx4 ON elementresult_relationship USING btree (value);


--
-- Name: environment_cvterm_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX environment_cvterm_idx1 ON environment_cvterm USING btree (environment_id);


--
-- Name: environment_cvterm_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX environment_cvterm_idx2 ON environment_cvterm USING btree (cvterm_id);


--
-- Name: environment_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX environment_idx1 ON environment USING btree (uniquename);


--
-- Name: expression_cvterm_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX expression_cvterm_idx1 ON expression_cvterm USING btree (expression_id);


--
-- Name: expression_cvterm_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX expression_cvterm_idx2 ON expression_cvterm USING btree (cvterm_id);


--
-- Name: expression_cvterm_idx3; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX expression_cvterm_idx3 ON expression_cvterm USING btree (cvterm_type_id);


--
-- Name: expression_cvtermprop_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX expression_cvtermprop_idx1 ON expression_cvtermprop USING btree (expression_cvterm_id);


--
-- Name: expression_cvtermprop_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX expression_cvtermprop_idx2 ON expression_cvtermprop USING btree (type_id);


--
-- Name: expression_image_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX expression_image_idx1 ON expression_image USING btree (expression_id);


--
-- Name: expression_image_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX expression_image_idx2 ON expression_image USING btree (eimage_id);


--
-- Name: expression_pub_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX expression_pub_idx1 ON expression_pub USING btree (expression_id);


--
-- Name: expression_pub_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX expression_pub_idx2 ON expression_pub USING btree (pub_id);


--
-- Name: expressionprop_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX expressionprop_idx1 ON expressionprop USING btree (expression_id);


--
-- Name: expressionprop_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX expressionprop_idx2 ON expressionprop USING btree (type_id);


--
-- Name: feature_cvterm_dbxref_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX feature_cvterm_dbxref_idx1 ON feature_cvterm_dbxref USING btree (feature_cvterm_id);


--
-- Name: feature_cvterm_dbxref_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX feature_cvterm_dbxref_idx2 ON feature_cvterm_dbxref USING btree (dbxref_id);


--
-- Name: feature_cvterm_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX feature_cvterm_idx1 ON feature_cvterm USING btree (feature_id);


--
-- Name: feature_cvterm_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX feature_cvterm_idx2 ON feature_cvterm USING btree (cvterm_id);


--
-- Name: feature_cvterm_idx3; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX feature_cvterm_idx3 ON feature_cvterm USING btree (pub_id);


--
-- Name: feature_cvterm_pub_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX feature_cvterm_pub_idx1 ON feature_cvterm_pub USING btree (feature_cvterm_id);


--
-- Name: feature_cvterm_pub_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX feature_cvterm_pub_idx2 ON feature_cvterm_pub USING btree (pub_id);


--
-- Name: feature_cvtermprop_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX feature_cvtermprop_idx1 ON feature_cvtermprop USING btree (feature_cvterm_id);


--
-- Name: feature_cvtermprop_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX feature_cvtermprop_idx2 ON feature_cvtermprop USING btree (type_id);


--
-- Name: feature_dbxref_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX feature_dbxref_idx1 ON feature_dbxref USING btree (feature_id);


--
-- Name: feature_dbxref_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX feature_dbxref_idx2 ON feature_dbxref USING btree (dbxref_id);


--
-- Name: feature_expression_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX feature_expression_idx1 ON feature_expression USING btree (expression_id);


--
-- Name: feature_expression_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX feature_expression_idx2 ON feature_expression USING btree (feature_id);


--
-- Name: feature_expression_idx3; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX feature_expression_idx3 ON feature_expression USING btree (pub_id);


--
-- Name: feature_expressionprop_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX feature_expressionprop_idx1 ON feature_expressionprop USING btree (feature_expression_id);


--
-- Name: feature_expressionprop_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX feature_expressionprop_idx2 ON feature_expressionprop USING btree (type_id);


--
-- Name: feature_genotype_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX feature_genotype_idx1 ON feature_genotype USING btree (feature_id);


--
-- Name: feature_genotype_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX feature_genotype_idx2 ON feature_genotype USING btree (genotype_id);


--
-- Name: feature_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX feature_idx1 ON feature USING btree (dbxref_id);


--
-- Name: feature_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX feature_idx2 ON feature USING btree (organism_id);


--
-- Name: feature_idx3; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX feature_idx3 ON feature USING btree (type_id);


--
-- Name: feature_idx4; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX feature_idx4 ON feature USING btree (uniquename);


--
-- Name: feature_idx5; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX feature_idx5 ON feature USING btree (lower((name)::text));


--
-- Name: feature_name_ind1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX feature_name_ind1 ON feature USING btree (name);


--
-- Name: feature_phenotype_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX feature_phenotype_idx1 ON feature_phenotype USING btree (feature_id);


--
-- Name: feature_phenotype_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX feature_phenotype_idx2 ON feature_phenotype USING btree (phenotype_id);


--
-- Name: feature_pub_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX feature_pub_idx1 ON feature_pub USING btree (feature_id);


--
-- Name: feature_pub_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX feature_pub_idx2 ON feature_pub USING btree (pub_id);


--
-- Name: feature_pubprop_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX feature_pubprop_idx1 ON feature_pubprop USING btree (feature_pub_id);


--
-- Name: feature_relationship_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX feature_relationship_idx1 ON feature_relationship USING btree (subject_id);


--
-- Name: feature_relationship_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX feature_relationship_idx2 ON feature_relationship USING btree (object_id);


--
-- Name: feature_relationship_idx3; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX feature_relationship_idx3 ON feature_relationship USING btree (type_id);


--
-- Name: feature_relationship_pub_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX feature_relationship_pub_idx1 ON feature_relationship_pub USING btree (feature_relationship_id);


--
-- Name: feature_relationship_pub_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX feature_relationship_pub_idx2 ON feature_relationship_pub USING btree (pub_id);


--
-- Name: feature_relationshipprop_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX feature_relationshipprop_idx1 ON feature_relationshipprop USING btree (feature_relationship_id);


--
-- Name: feature_relationshipprop_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX feature_relationshipprop_idx2 ON feature_relationshipprop USING btree (type_id);


--
-- Name: feature_relationshipprop_pub_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX feature_relationshipprop_pub_idx1 ON feature_relationshipprop_pub USING btree (feature_relationshipprop_id);


--
-- Name: feature_relationshipprop_pub_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX feature_relationshipprop_pub_idx2 ON feature_relationshipprop_pub USING btree (pub_id);


--
-- Name: feature_synonym_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX feature_synonym_idx1 ON feature_synonym USING btree (synonym_id);


--
-- Name: feature_synonym_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX feature_synonym_idx2 ON feature_synonym USING btree (feature_id);


--
-- Name: feature_synonym_idx3; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX feature_synonym_idx3 ON feature_synonym USING btree (pub_id);


--
-- Name: featureloc_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX featureloc_idx1 ON featureloc USING btree (feature_id);


--
-- Name: featureloc_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX featureloc_idx2 ON featureloc USING btree (srcfeature_id);


--
-- Name: featureloc_idx3; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX featureloc_idx3 ON featureloc USING btree (srcfeature_id, fmin, fmax);


--
-- Name: featureloc_pub_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX featureloc_pub_idx1 ON featureloc_pub USING btree (featureloc_id);


--
-- Name: featureloc_pub_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX featureloc_pub_idx2 ON featureloc_pub USING btree (pub_id);


--
-- Name: featuremap_pub_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX featuremap_pub_idx1 ON featuremap_pub USING btree (featuremap_id);


--
-- Name: featuremap_pub_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX featuremap_pub_idx2 ON featuremap_pub USING btree (pub_id);


--
-- Name: featurepos_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX featurepos_idx1 ON featurepos USING btree (featuremap_id);


--
-- Name: featurepos_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX featurepos_idx2 ON featurepos USING btree (feature_id);


--
-- Name: featurepos_idx3; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX featurepos_idx3 ON featurepos USING btree (map_feature_id);


--
-- Name: INDEX featureprop_c1; Type: COMMENT; Schema: chado; Owner: nathandunn
--

COMMENT ON INDEX featureprop_c1 IS 'For any one feature, multivalued
property-value pairs must be differentiated by rank.';


--
-- Name: featureprop_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX featureprop_idx1 ON featureprop USING btree (feature_id);


--
-- Name: featureprop_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX featureprop_idx2 ON featureprop USING btree (type_id);


--
-- Name: featureprop_pub_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX featureprop_pub_idx1 ON featureprop_pub USING btree (featureprop_id);


--
-- Name: featureprop_pub_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX featureprop_pub_idx2 ON featureprop_pub USING btree (pub_id);


--
-- Name: featurerange_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX featurerange_idx1 ON featurerange USING btree (featuremap_id);


--
-- Name: featurerange_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX featurerange_idx2 ON featurerange USING btree (feature_id);


--
-- Name: featurerange_idx3; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX featurerange_idx3 ON featurerange USING btree (leftstartf_id);


--
-- Name: featurerange_idx4; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX featurerange_idx4 ON featurerange USING btree (leftendf_id);


--
-- Name: featurerange_idx5; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX featurerange_idx5 ON featurerange USING btree (rightstartf_id);


--
-- Name: featurerange_idx6; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX featurerange_idx6 ON featurerange USING btree (rightendf_id);


--
-- Name: genotype_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX genotype_idx1 ON genotype USING btree (uniquename);


--
-- Name: genotype_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX genotype_idx2 ON genotype USING btree (name);


--
-- Name: genotypeprop_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX genotypeprop_idx1 ON genotypeprop USING btree (genotype_id);


--
-- Name: genotypeprop_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX genotypeprop_idx2 ON genotypeprop USING btree (type_id);


--
-- Name: gff_interval_stats_idx1; Type: INDEX; Schema: chado; Owner: ubuntu; Tablespace: 
--

CREATE UNIQUE INDEX gff_interval_stats_idx1 ON gff_interval_stats USING btree (typeid, srcfeature_id, bin);


--
-- Name: idx_cv_root_mview_cv_id; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX idx_cv_root_mview_cv_id ON cv_root_mview USING btree (cv_id);


--
-- Name: idx_cv_root_mview_cvterm_id; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX idx_cv_root_mview_cvterm_id ON cv_root_mview USING btree (cvterm_id);


--
-- Name: idx_go_count_analysis_analysis_id; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX idx_go_count_analysis_analysis_id ON go_count_analysis USING btree (analysis_id);


--
-- Name: idx_go_count_analysis_cvterm_id; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX idx_go_count_analysis_cvterm_id ON go_count_analysis USING btree (cvterm_id);


--
-- Name: idx_go_count_analysis_organism_id; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX idx_go_count_analysis_organism_id ON go_count_analysis USING btree (organism_id);


--
-- Name: idx_go_count_organism_cvterm_id; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX idx_go_count_organism_cvterm_id ON go_count_organism USING btree (cvterm_id);


--
-- Name: idx_go_count_organism_organism_id; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX idx_go_count_organism_organism_id ON go_count_organism USING btree (organism_id);


--
-- Name: idx_kegg_by_organism_analysis_id; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX idx_kegg_by_organism_analysis_id ON kegg_by_organism USING btree (analysis_id);


--
-- Name: idx_kegg_by_organism_organism_id; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX idx_kegg_by_organism_organism_id ON kegg_by_organism USING btree (organism_id);


--
-- Name: idx_organism_feature_count_cvterm_id; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX idx_organism_feature_count_cvterm_id ON organism_feature_count USING btree (cvterm_id);


--
-- Name: idx_organism_feature_count_feature_type; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX idx_organism_feature_count_feature_type ON organism_feature_count USING btree (feature_type);


--
-- Name: idx_organism_feature_count_organism_id; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX idx_organism_feature_count_organism_id ON organism_feature_count USING btree (organism_id);


--
-- Name: library_cvterm_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX library_cvterm_idx1 ON library_cvterm USING btree (library_id);


--
-- Name: library_cvterm_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX library_cvterm_idx2 ON library_cvterm USING btree (cvterm_id);


--
-- Name: library_cvterm_idx3; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX library_cvterm_idx3 ON library_cvterm USING btree (pub_id);


--
-- Name: library_dbxref_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX library_dbxref_idx1 ON library_dbxref USING btree (library_id);


--
-- Name: library_dbxref_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX library_dbxref_idx2 ON library_dbxref USING btree (dbxref_id);


--
-- Name: library_feature_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX library_feature_idx1 ON library_feature USING btree (library_id);


--
-- Name: library_feature_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX library_feature_idx2 ON library_feature USING btree (feature_id);


--
-- Name: library_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX library_idx1 ON library USING btree (organism_id);


--
-- Name: library_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX library_idx2 ON library USING btree (type_id);


--
-- Name: library_idx3; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX library_idx3 ON library USING btree (uniquename);


--
-- Name: library_name_ind1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX library_name_ind1 ON library USING btree (name);


--
-- Name: library_pub_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX library_pub_idx1 ON library_pub USING btree (library_id);


--
-- Name: library_pub_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX library_pub_idx2 ON library_pub USING btree (pub_id);


--
-- Name: library_synonym_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX library_synonym_idx1 ON library_synonym USING btree (synonym_id);


--
-- Name: library_synonym_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX library_synonym_idx2 ON library_synonym USING btree (library_id);


--
-- Name: library_synonym_idx3; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX library_synonym_idx3 ON library_synonym USING btree (pub_id);


--
-- Name: libraryprop_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX libraryprop_idx1 ON libraryprop USING btree (library_id);


--
-- Name: libraryprop_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX libraryprop_idx2 ON libraryprop USING btree (type_id);


--
-- Name: libraryprop_pub_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX libraryprop_pub_idx1 ON libraryprop_pub USING btree (libraryprop_id);


--
-- Name: libraryprop_pub_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX libraryprop_pub_idx2 ON libraryprop_pub USING btree (pub_id);


--
-- Name: magedocumentation_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX magedocumentation_idx1 ON magedocumentation USING btree (mageml_id);


--
-- Name: magedocumentation_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX magedocumentation_idx2 ON magedocumentation USING btree (tableinfo_id);


--
-- Name: magedocumentation_idx3; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX magedocumentation_idx3 ON magedocumentation USING btree (row_id);


--
-- Name: nd_experiment_pub_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX nd_experiment_pub_idx1 ON nd_experiment_pub USING btree (nd_experiment_id);


--
-- Name: nd_experiment_pub_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX nd_experiment_pub_idx2 ON nd_experiment_pub USING btree (pub_id);


--
-- Name: organism_dbxref_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX organism_dbxref_idx1 ON organism_dbxref USING btree (organism_id);


--
-- Name: organism_dbxref_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX organism_dbxref_idx2 ON organism_dbxref USING btree (dbxref_id);


--
-- Name: organismprop_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX organismprop_idx1 ON organismprop USING btree (organism_id);


--
-- Name: organismprop_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX organismprop_idx2 ON organismprop USING btree (type_id);


--
-- Name: phendesc_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX phendesc_idx1 ON phendesc USING btree (genotype_id);


--
-- Name: phendesc_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX phendesc_idx2 ON phendesc USING btree (environment_id);


--
-- Name: phendesc_idx3; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX phendesc_idx3 ON phendesc USING btree (pub_id);


--
-- Name: phenotype_comparison_cvterm_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX phenotype_comparison_cvterm_idx1 ON phenotype_comparison_cvterm USING btree (phenotype_comparison_id);


--
-- Name: phenotype_comparison_cvterm_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX phenotype_comparison_cvterm_idx2 ON phenotype_comparison_cvterm USING btree (cvterm_id);


--
-- Name: phenotype_comparison_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX phenotype_comparison_idx1 ON phenotype_comparison USING btree (genotype1_id);


--
-- Name: phenotype_comparison_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX phenotype_comparison_idx2 ON phenotype_comparison USING btree (genotype2_id);


--
-- Name: phenotype_comparison_idx4; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX phenotype_comparison_idx4 ON phenotype_comparison USING btree (pub_id);


--
-- Name: phenotype_cvterm_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX phenotype_cvterm_idx1 ON phenotype_cvterm USING btree (phenotype_id);


--
-- Name: phenotype_cvterm_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX phenotype_cvterm_idx2 ON phenotype_cvterm USING btree (cvterm_id);


--
-- Name: phenotype_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX phenotype_idx1 ON phenotype USING btree (cvalue_id);


--
-- Name: phenotype_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX phenotype_idx2 ON phenotype USING btree (observable_id);


--
-- Name: phenotype_idx3; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX phenotype_idx3 ON phenotype USING btree (attr_id);


--
-- Name: phenstatement_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX phenstatement_idx1 ON phenstatement USING btree (genotype_id);


--
-- Name: phenstatement_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX phenstatement_idx2 ON phenstatement USING btree (phenotype_id);


--
-- Name: phylonode_dbxref_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX phylonode_dbxref_idx1 ON phylonode_dbxref USING btree (phylonode_id);


--
-- Name: phylonode_dbxref_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX phylonode_dbxref_idx2 ON phylonode_dbxref USING btree (dbxref_id);


--
-- Name: phylonode_organism_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX phylonode_organism_idx1 ON phylonode_organism USING btree (phylonode_id);


--
-- Name: phylonode_organism_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX phylonode_organism_idx2 ON phylonode_organism USING btree (organism_id);


--
-- Name: phylonode_pub_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX phylonode_pub_idx1 ON phylonode_pub USING btree (phylonode_id);


--
-- Name: phylonode_pub_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX phylonode_pub_idx2 ON phylonode_pub USING btree (pub_id);


--
-- Name: phylonode_relationship_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX phylonode_relationship_idx1 ON phylonode_relationship USING btree (subject_id);


--
-- Name: phylonode_relationship_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX phylonode_relationship_idx2 ON phylonode_relationship USING btree (object_id);


--
-- Name: phylonode_relationship_idx3; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX phylonode_relationship_idx3 ON phylonode_relationship USING btree (type_id);


--
-- Name: phylonodeprop_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX phylonodeprop_idx1 ON phylonodeprop USING btree (phylonode_id);


--
-- Name: phylonodeprop_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX phylonodeprop_idx2 ON phylonodeprop USING btree (type_id);


--
-- Name: phylotree_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX phylotree_idx1 ON phylotree USING btree (phylotree_id);


--
-- Name: phylotree_pub_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX phylotree_pub_idx1 ON phylotree_pub USING btree (phylotree_id);


--
-- Name: phylotree_pub_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX phylotree_pub_idx2 ON phylotree_pub USING btree (pub_id);


--
-- Name: project_contact_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX project_contact_idx1 ON project_contact USING btree (project_id);


--
-- Name: project_contact_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX project_contact_idx2 ON project_contact USING btree (contact_id);


--
-- Name: project_pub_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX project_pub_idx1 ON project_pub USING btree (project_id);


--
-- Name: project_pub_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX project_pub_idx2 ON project_pub USING btree (pub_id);


--
-- Name: protocol_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX protocol_idx1 ON protocol USING btree (type_id);


--
-- Name: protocol_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX protocol_idx2 ON protocol USING btree (pub_id);


--
-- Name: protocol_idx3; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX protocol_idx3 ON protocol USING btree (dbxref_id);


--
-- Name: protocolparam_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX protocolparam_idx1 ON protocolparam USING btree (protocol_id);


--
-- Name: protocolparam_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX protocolparam_idx2 ON protocolparam USING btree (datatype_id);


--
-- Name: protocolparam_idx3; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX protocolparam_idx3 ON protocolparam USING btree (unittype_id);


--
-- Name: pub_dbxref_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX pub_dbxref_idx1 ON pub_dbxref USING btree (pub_id);


--
-- Name: pub_dbxref_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX pub_dbxref_idx2 ON pub_dbxref USING btree (dbxref_id);


--
-- Name: pub_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX pub_idx1 ON pub USING btree (type_id);


--
-- Name: pub_relationship_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX pub_relationship_idx1 ON pub_relationship USING btree (subject_id);


--
-- Name: pub_relationship_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX pub_relationship_idx2 ON pub_relationship USING btree (object_id);


--
-- Name: pub_relationship_idx3; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX pub_relationship_idx3 ON pub_relationship USING btree (type_id);


--
-- Name: pubauthor_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX pubauthor_idx2 ON pubauthor USING btree (pub_id);


--
-- Name: pubprop_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX pubprop_idx1 ON pubprop USING btree (pub_id);


--
-- Name: pubprop_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX pubprop_idx2 ON pubprop USING btree (type_id);


--
-- Name: quantification_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX quantification_idx1 ON quantification USING btree (acquisition_id);


--
-- Name: quantification_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX quantification_idx2 ON quantification USING btree (operator_id);


--
-- Name: quantification_idx3; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX quantification_idx3 ON quantification USING btree (protocol_id);


--
-- Name: quantification_idx4; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX quantification_idx4 ON quantification USING btree (analysis_id);


--
-- Name: quantification_relationship_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX quantification_relationship_idx1 ON quantification_relationship USING btree (subject_id);


--
-- Name: quantification_relationship_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX quantification_relationship_idx2 ON quantification_relationship USING btree (type_id);


--
-- Name: quantification_relationship_idx3; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX quantification_relationship_idx3 ON quantification_relationship USING btree (object_id);


--
-- Name: quantificationprop_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX quantificationprop_idx1 ON quantificationprop USING btree (quantification_id);


--
-- Name: quantificationprop_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX quantificationprop_idx2 ON quantificationprop USING btree (type_id);


--
-- Name: searchable_all_feature_names_idx; Type: INDEX; Schema: chado; Owner: ubuntu; Tablespace: 
--

CREATE INDEX searchable_all_feature_names_idx ON all_feature_names USING gin (searchable_name);


--
-- Name: stock_cvterm_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX stock_cvterm_idx1 ON stock_cvterm USING btree (stock_id);


--
-- Name: stock_cvterm_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX stock_cvterm_idx2 ON stock_cvterm USING btree (cvterm_id);


--
-- Name: stock_cvterm_idx3; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX stock_cvterm_idx3 ON stock_cvterm USING btree (pub_id);


--
-- Name: stock_cvtermprop_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX stock_cvtermprop_idx1 ON stock_cvtermprop USING btree (stock_cvterm_id);


--
-- Name: stock_cvtermprop_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX stock_cvtermprop_idx2 ON stock_cvtermprop USING btree (type_id);


--
-- Name: stock_dbxref_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX stock_dbxref_idx1 ON stock_dbxref USING btree (stock_id);


--
-- Name: stock_dbxref_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX stock_dbxref_idx2 ON stock_dbxref USING btree (dbxref_id);


--
-- Name: stock_dbxrefprop_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX stock_dbxrefprop_idx1 ON stock_dbxrefprop USING btree (stock_dbxref_id);


--
-- Name: stock_dbxrefprop_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX stock_dbxrefprop_idx2 ON stock_dbxrefprop USING btree (type_id);


--
-- Name: stock_genotype_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX stock_genotype_idx1 ON stock_genotype USING btree (stock_id);


--
-- Name: stock_genotype_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX stock_genotype_idx2 ON stock_genotype USING btree (genotype_id);


--
-- Name: stock_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX stock_idx1 ON stock USING btree (dbxref_id);


--
-- Name: stock_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX stock_idx2 ON stock USING btree (organism_id);


--
-- Name: stock_idx3; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX stock_idx3 ON stock USING btree (type_id);


--
-- Name: stock_idx4; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX stock_idx4 ON stock USING btree (uniquename);


--
-- Name: stock_name_ind1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX stock_name_ind1 ON stock USING btree (name);


--
-- Name: stock_pub_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX stock_pub_idx1 ON stock_pub USING btree (stock_id);


--
-- Name: stock_pub_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX stock_pub_idx2 ON stock_pub USING btree (pub_id);


--
-- Name: stock_relationship_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX stock_relationship_idx1 ON stock_relationship USING btree (subject_id);


--
-- Name: stock_relationship_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX stock_relationship_idx2 ON stock_relationship USING btree (object_id);


--
-- Name: stock_relationship_idx3; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX stock_relationship_idx3 ON stock_relationship USING btree (type_id);


--
-- Name: stock_relationship_pub_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX stock_relationship_pub_idx1 ON stock_relationship_pub USING btree (stock_relationship_id);


--
-- Name: stock_relationship_pub_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX stock_relationship_pub_idx2 ON stock_relationship_pub USING btree (pub_id);


--
-- Name: stockcollection_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX stockcollection_idx1 ON stockcollection USING btree (contact_id);


--
-- Name: stockcollection_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX stockcollection_idx2 ON stockcollection USING btree (type_id);


--
-- Name: stockcollection_idx3; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX stockcollection_idx3 ON stockcollection USING btree (uniquename);


--
-- Name: stockcollection_name_ind1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX stockcollection_name_ind1 ON stockcollection USING btree (name);


--
-- Name: stockcollection_stock_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX stockcollection_stock_idx1 ON stockcollection_stock USING btree (stockcollection_id);


--
-- Name: stockcollection_stock_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX stockcollection_stock_idx2 ON stockcollection_stock USING btree (stock_id);


--
-- Name: stockcollectionprop_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX stockcollectionprop_idx1 ON stockcollectionprop USING btree (stockcollection_id);


--
-- Name: stockcollectionprop_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX stockcollectionprop_idx2 ON stockcollectionprop USING btree (type_id);


--
-- Name: stockprop_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX stockprop_idx1 ON stockprop USING btree (stock_id);


--
-- Name: stockprop_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX stockprop_idx2 ON stockprop USING btree (type_id);


--
-- Name: stockprop_pub_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX stockprop_pub_idx1 ON stockprop_pub USING btree (stockprop_id);


--
-- Name: stockprop_pub_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX stockprop_pub_idx2 ON stockprop_pub USING btree (pub_id);


--
-- Name: study_assay_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX study_assay_idx1 ON study_assay USING btree (study_id);


--
-- Name: study_assay_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX study_assay_idx2 ON study_assay USING btree (assay_id);


--
-- Name: study_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX study_idx1 ON study USING btree (contact_id);


--
-- Name: study_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX study_idx2 ON study USING btree (pub_id);


--
-- Name: study_idx3; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX study_idx3 ON study USING btree (dbxref_id);


--
-- Name: studydesign_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX studydesign_idx1 ON studydesign USING btree (study_id);


--
-- Name: studydesignprop_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX studydesignprop_idx1 ON studydesignprop USING btree (studydesign_id);


--
-- Name: studydesignprop_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX studydesignprop_idx2 ON studydesignprop USING btree (type_id);


--
-- Name: studyfactor_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX studyfactor_idx1 ON studyfactor USING btree (studydesign_id);


--
-- Name: studyfactor_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX studyfactor_idx2 ON studyfactor USING btree (type_id);


--
-- Name: studyfactorvalue_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX studyfactorvalue_idx1 ON studyfactorvalue USING btree (studyfactor_id);


--
-- Name: studyfactorvalue_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX studyfactorvalue_idx2 ON studyfactorvalue USING btree (assay_id);


--
-- Name: studyprop_feature_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX studyprop_feature_idx1 ON studyprop_feature USING btree (studyprop_id);


--
-- Name: studyprop_feature_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX studyprop_feature_idx2 ON studyprop_feature USING btree (feature_id);


--
-- Name: studyprop_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX studyprop_idx1 ON studyprop USING btree (study_id);


--
-- Name: studyprop_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX studyprop_idx2 ON studyprop USING btree (type_id);


--
-- Name: synonym_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX synonym_idx1 ON synonym USING btree (type_id);


--
-- Name: synonym_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX synonym_idx2 ON synonym USING btree (lower((synonym_sgml)::text));


--
-- Name: tmp_cds_handler_fmax; Type: INDEX; Schema: chado; Owner: ubuntu; Tablespace: 
--

CREATE INDEX tmp_cds_handler_fmax ON tmp_cds_handler USING btree (fmax);


--
-- Name: tmp_cds_handler_relationship_grandparent; Type: INDEX; Schema: chado; Owner: ubuntu; Tablespace: 
--

CREATE INDEX tmp_cds_handler_relationship_grandparent ON tmp_cds_handler_relationship USING btree (grandparent_id);


--
-- Name: tmp_cds_handler_seq_id; Type: INDEX; Schema: chado; Owner: ubuntu; Tablespace: 
--

CREATE INDEX tmp_cds_handler_seq_id ON tmp_cds_handler USING btree (seq_id);


--
-- Name: treatment_idx1; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX treatment_idx1 ON treatment USING btree (biomaterial_id);


--
-- Name: treatment_idx2; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX treatment_idx2 ON treatment USING btree (type_id);


--
-- Name: treatment_idx3; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX treatment_idx3 ON treatment USING btree (protocol_id);


--
-- Name: tripal_gff_temp_tripal_gff_temp_idx0_idx; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX tripal_gff_temp_tripal_gff_temp_idx0_idx ON tripal_gff_temp USING btree (organism_id);


--
-- Name: tripal_gff_temp_tripal_gff_temp_idx1_idx; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX tripal_gff_temp_tripal_gff_temp_idx1_idx ON tripal_gff_temp USING btree (uniquename);


--
-- Name: tripal_obo_temp_tripal_obo_temp_idx0_idx; Type: INDEX; Schema: chado; Owner: nathandunn; Tablespace: 
--

CREATE INDEX tripal_obo_temp_tripal_obo_temp_idx0_idx ON tripal_obo_temp USING btree (type);


--
-- Name: dbxref_searchable_iu; Type: TRIGGER; Schema: chado; Owner: nathandunn
--

CREATE TRIGGER dbxref_searchable_iu BEFORE INSERT OR UPDATE ON dbxref FOR EACH ROW EXECUTE PROCEDURE tsvector_update_trigger('searchable_accession', 'pg_catalog.english', 'accession');


--
-- Name: feature_searchable_iu; Type: TRIGGER; Schema: chado; Owner: nathandunn
--

CREATE TRIGGER feature_searchable_iu BEFORE INSERT OR UPDATE ON feature FOR EACH ROW EXECUTE PROCEDURE tsvector_update_trigger('searchable_name', 'pg_catalog.english', 'name', 'uniquename');


--
-- Name: synonym_searchable_iu; Type: TRIGGER; Schema: chado; Owner: nathandunn
--

CREATE TRIGGER synonym_searchable_iu BEFORE INSERT OR UPDATE ON synonym FOR EACH ROW EXECUTE PROCEDURE tsvector_update_trigger('searchable_synonym_sgml', 'pg_catalog.english', 'synonym_sgml');


--
-- Name: acquisition_assay_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY acquisition
    ADD CONSTRAINT acquisition_assay_id_fkey FOREIGN KEY (assay_id) REFERENCES assay(assay_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: acquisition_channel_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY acquisition
    ADD CONSTRAINT acquisition_channel_id_fkey FOREIGN KEY (channel_id) REFERENCES channel(channel_id) ON DELETE SET NULL DEFERRABLE INITIALLY DEFERRED;


--
-- Name: acquisition_protocol_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY acquisition
    ADD CONSTRAINT acquisition_protocol_id_fkey FOREIGN KEY (protocol_id) REFERENCES protocol(protocol_id) ON DELETE SET NULL DEFERRABLE INITIALLY DEFERRED;


--
-- Name: acquisition_relationship_object_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY acquisition_relationship
    ADD CONSTRAINT acquisition_relationship_object_id_fkey FOREIGN KEY (object_id) REFERENCES acquisition(acquisition_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: acquisition_relationship_subject_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY acquisition_relationship
    ADD CONSTRAINT acquisition_relationship_subject_id_fkey FOREIGN KEY (subject_id) REFERENCES acquisition(acquisition_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: acquisition_relationship_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY acquisition_relationship
    ADD CONSTRAINT acquisition_relationship_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: acquisitionprop_acquisition_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY acquisitionprop
    ADD CONSTRAINT acquisitionprop_acquisition_id_fkey FOREIGN KEY (acquisition_id) REFERENCES acquisition(acquisition_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: acquisitionprop_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY acquisitionprop
    ADD CONSTRAINT acquisitionprop_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: analysis_organism_analysis_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY analysis_organism
    ADD CONSTRAINT analysis_organism_analysis_id_fkey FOREIGN KEY (analysis_id) REFERENCES analysis(analysis_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: analysis_organism_organism_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY analysis_organism
    ADD CONSTRAINT analysis_organism_organism_id_fkey FOREIGN KEY (organism_id) REFERENCES organism(organism_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: analysisfeature_analysis_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY analysisfeature
    ADD CONSTRAINT analysisfeature_analysis_id_fkey FOREIGN KEY (analysis_id) REFERENCES analysis(analysis_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: analysisfeature_feature_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY analysisfeature
    ADD CONSTRAINT analysisfeature_feature_id_fkey FOREIGN KEY (feature_id) REFERENCES feature(feature_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: analysisfeatureprop_analysisfeature_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY analysisfeatureprop
    ADD CONSTRAINT analysisfeatureprop_analysisfeature_id_fkey FOREIGN KEY (analysisfeature_id) REFERENCES analysisfeature(analysisfeature_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: analysisfeatureprop_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY analysisfeatureprop
    ADD CONSTRAINT analysisfeatureprop_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: analysisprop_analysis_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY analysisprop
    ADD CONSTRAINT analysisprop_analysis_id_fkey FOREIGN KEY (analysis_id) REFERENCES analysis(analysis_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: analysisprop_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY analysisprop
    ADD CONSTRAINT analysisprop_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: arraydesign_dbxref_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY arraydesign
    ADD CONSTRAINT arraydesign_dbxref_id_fkey FOREIGN KEY (dbxref_id) REFERENCES dbxref(dbxref_id) ON DELETE SET NULL DEFERRABLE INITIALLY DEFERRED;


--
-- Name: arraydesign_manufacturer_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY arraydesign
    ADD CONSTRAINT arraydesign_manufacturer_id_fkey FOREIGN KEY (manufacturer_id) REFERENCES contact(contact_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: arraydesign_platformtype_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY arraydesign
    ADD CONSTRAINT arraydesign_platformtype_id_fkey FOREIGN KEY (platformtype_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: arraydesign_protocol_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY arraydesign
    ADD CONSTRAINT arraydesign_protocol_id_fkey FOREIGN KEY (protocol_id) REFERENCES protocol(protocol_id) ON DELETE SET NULL DEFERRABLE INITIALLY DEFERRED;


--
-- Name: arraydesign_substratetype_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY arraydesign
    ADD CONSTRAINT arraydesign_substratetype_id_fkey FOREIGN KEY (substratetype_id) REFERENCES cvterm(cvterm_id) ON DELETE SET NULL DEFERRABLE INITIALLY DEFERRED;


--
-- Name: arraydesignprop_arraydesign_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY arraydesignprop
    ADD CONSTRAINT arraydesignprop_arraydesign_id_fkey FOREIGN KEY (arraydesign_id) REFERENCES arraydesign(arraydesign_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: arraydesignprop_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY arraydesignprop
    ADD CONSTRAINT arraydesignprop_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: assay_arraydesign_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY assay
    ADD CONSTRAINT assay_arraydesign_id_fkey FOREIGN KEY (arraydesign_id) REFERENCES arraydesign(arraydesign_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: assay_biomaterial_assay_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY assay_biomaterial
    ADD CONSTRAINT assay_biomaterial_assay_id_fkey FOREIGN KEY (assay_id) REFERENCES assay(assay_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: assay_biomaterial_biomaterial_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY assay_biomaterial
    ADD CONSTRAINT assay_biomaterial_biomaterial_id_fkey FOREIGN KEY (biomaterial_id) REFERENCES biomaterial(biomaterial_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: assay_biomaterial_channel_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY assay_biomaterial
    ADD CONSTRAINT assay_biomaterial_channel_id_fkey FOREIGN KEY (channel_id) REFERENCES channel(channel_id) ON DELETE SET NULL DEFERRABLE INITIALLY DEFERRED;


--
-- Name: assay_dbxref_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY assay
    ADD CONSTRAINT assay_dbxref_id_fkey FOREIGN KEY (dbxref_id) REFERENCES dbxref(dbxref_id) ON DELETE SET NULL DEFERRABLE INITIALLY DEFERRED;


--
-- Name: assay_operator_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY assay
    ADD CONSTRAINT assay_operator_id_fkey FOREIGN KEY (operator_id) REFERENCES contact(contact_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: assay_project_assay_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY assay_project
    ADD CONSTRAINT assay_project_assay_id_fkey FOREIGN KEY (assay_id) REFERENCES assay(assay_id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: assay_project_project_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY assay_project
    ADD CONSTRAINT assay_project_project_id_fkey FOREIGN KEY (project_id) REFERENCES project(project_id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: assay_protocol_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY assay
    ADD CONSTRAINT assay_protocol_id_fkey FOREIGN KEY (protocol_id) REFERENCES protocol(protocol_id) ON DELETE SET NULL DEFERRABLE INITIALLY DEFERRED;


--
-- Name: assayprop_assay_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY assayprop
    ADD CONSTRAINT assayprop_assay_id_fkey FOREIGN KEY (assay_id) REFERENCES assay(assay_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: assayprop_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY assayprop
    ADD CONSTRAINT assayprop_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: biomaterial_biosourceprovider_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY biomaterial
    ADD CONSTRAINT biomaterial_biosourceprovider_id_fkey FOREIGN KEY (biosourceprovider_id) REFERENCES contact(contact_id) ON DELETE SET NULL DEFERRABLE INITIALLY DEFERRED;


--
-- Name: biomaterial_dbxref_biomaterial_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY biomaterial_dbxref
    ADD CONSTRAINT biomaterial_dbxref_biomaterial_id_fkey FOREIGN KEY (biomaterial_id) REFERENCES biomaterial(biomaterial_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: biomaterial_dbxref_dbxref_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY biomaterial_dbxref
    ADD CONSTRAINT biomaterial_dbxref_dbxref_id_fkey FOREIGN KEY (dbxref_id) REFERENCES dbxref(dbxref_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: biomaterial_dbxref_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY biomaterial
    ADD CONSTRAINT biomaterial_dbxref_id_fkey FOREIGN KEY (dbxref_id) REFERENCES dbxref(dbxref_id) ON DELETE SET NULL DEFERRABLE INITIALLY DEFERRED;


--
-- Name: biomaterial_relationship_object_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY biomaterial_relationship
    ADD CONSTRAINT biomaterial_relationship_object_id_fkey FOREIGN KEY (object_id) REFERENCES biomaterial(biomaterial_id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: biomaterial_relationship_subject_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY biomaterial_relationship
    ADD CONSTRAINT biomaterial_relationship_subject_id_fkey FOREIGN KEY (subject_id) REFERENCES biomaterial(biomaterial_id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: biomaterial_relationship_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY biomaterial_relationship
    ADD CONSTRAINT biomaterial_relationship_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: biomaterial_taxon_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY biomaterial
    ADD CONSTRAINT biomaterial_taxon_id_fkey FOREIGN KEY (taxon_id) REFERENCES organism(organism_id) ON DELETE SET NULL DEFERRABLE INITIALLY DEFERRED;


--
-- Name: biomaterial_treatment_biomaterial_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY biomaterial_treatment
    ADD CONSTRAINT biomaterial_treatment_biomaterial_id_fkey FOREIGN KEY (biomaterial_id) REFERENCES biomaterial(biomaterial_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: biomaterial_treatment_treatment_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY biomaterial_treatment
    ADD CONSTRAINT biomaterial_treatment_treatment_id_fkey FOREIGN KEY (treatment_id) REFERENCES treatment(treatment_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: biomaterial_treatment_unittype_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY biomaterial_treatment
    ADD CONSTRAINT biomaterial_treatment_unittype_id_fkey FOREIGN KEY (unittype_id) REFERENCES cvterm(cvterm_id) ON DELETE SET NULL DEFERRABLE INITIALLY DEFERRED;


--
-- Name: biomaterialprop_biomaterial_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY biomaterialprop
    ADD CONSTRAINT biomaterialprop_biomaterial_id_fkey FOREIGN KEY (biomaterial_id) REFERENCES biomaterial(biomaterial_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: biomaterialprop_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY biomaterialprop
    ADD CONSTRAINT biomaterialprop_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: cell_line_cvterm_cell_line_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY cell_line_cvterm
    ADD CONSTRAINT cell_line_cvterm_cell_line_id_fkey FOREIGN KEY (cell_line_id) REFERENCES cell_line(cell_line_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: cell_line_cvterm_cvterm_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY cell_line_cvterm
    ADD CONSTRAINT cell_line_cvterm_cvterm_id_fkey FOREIGN KEY (cvterm_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: cell_line_cvterm_pub_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY cell_line_cvterm
    ADD CONSTRAINT cell_line_cvterm_pub_id_fkey FOREIGN KEY (pub_id) REFERENCES pub(pub_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: cell_line_cvtermprop_cell_line_cvterm_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY cell_line_cvtermprop
    ADD CONSTRAINT cell_line_cvtermprop_cell_line_cvterm_id_fkey FOREIGN KEY (cell_line_cvterm_id) REFERENCES cell_line_cvterm(cell_line_cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: cell_line_cvtermprop_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY cell_line_cvtermprop
    ADD CONSTRAINT cell_line_cvtermprop_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: cell_line_dbxref_cell_line_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY cell_line_dbxref
    ADD CONSTRAINT cell_line_dbxref_cell_line_id_fkey FOREIGN KEY (cell_line_id) REFERENCES cell_line(cell_line_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: cell_line_dbxref_dbxref_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY cell_line_dbxref
    ADD CONSTRAINT cell_line_dbxref_dbxref_id_fkey FOREIGN KEY (dbxref_id) REFERENCES dbxref(dbxref_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: cell_line_feature_cell_line_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY cell_line_feature
    ADD CONSTRAINT cell_line_feature_cell_line_id_fkey FOREIGN KEY (cell_line_id) REFERENCES cell_line(cell_line_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: cell_line_feature_feature_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY cell_line_feature
    ADD CONSTRAINT cell_line_feature_feature_id_fkey FOREIGN KEY (feature_id) REFERENCES feature(feature_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: cell_line_feature_pub_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY cell_line_feature
    ADD CONSTRAINT cell_line_feature_pub_id_fkey FOREIGN KEY (pub_id) REFERENCES pub(pub_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: cell_line_library_cell_line_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY cell_line_library
    ADD CONSTRAINT cell_line_library_cell_line_id_fkey FOREIGN KEY (cell_line_id) REFERENCES cell_line(cell_line_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: cell_line_library_library_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY cell_line_library
    ADD CONSTRAINT cell_line_library_library_id_fkey FOREIGN KEY (library_id) REFERENCES library(library_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: cell_line_library_pub_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY cell_line_library
    ADD CONSTRAINT cell_line_library_pub_id_fkey FOREIGN KEY (pub_id) REFERENCES pub(pub_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: cell_line_organism_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY cell_line
    ADD CONSTRAINT cell_line_organism_id_fkey FOREIGN KEY (organism_id) REFERENCES organism(organism_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: cell_line_pub_cell_line_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY cell_line_pub
    ADD CONSTRAINT cell_line_pub_cell_line_id_fkey FOREIGN KEY (cell_line_id) REFERENCES cell_line(cell_line_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: cell_line_pub_pub_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY cell_line_pub
    ADD CONSTRAINT cell_line_pub_pub_id_fkey FOREIGN KEY (pub_id) REFERENCES pub(pub_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: cell_line_relationship_object_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY cell_line_relationship
    ADD CONSTRAINT cell_line_relationship_object_id_fkey FOREIGN KEY (object_id) REFERENCES cell_line(cell_line_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: cell_line_relationship_subject_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY cell_line_relationship
    ADD CONSTRAINT cell_line_relationship_subject_id_fkey FOREIGN KEY (subject_id) REFERENCES cell_line(cell_line_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: cell_line_relationship_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY cell_line_relationship
    ADD CONSTRAINT cell_line_relationship_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: cell_line_synonym_cell_line_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY cell_line_synonym
    ADD CONSTRAINT cell_line_synonym_cell_line_id_fkey FOREIGN KEY (cell_line_id) REFERENCES cell_line(cell_line_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: cell_line_synonym_pub_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY cell_line_synonym
    ADD CONSTRAINT cell_line_synonym_pub_id_fkey FOREIGN KEY (pub_id) REFERENCES pub(pub_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: cell_line_synonym_synonym_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY cell_line_synonym
    ADD CONSTRAINT cell_line_synonym_synonym_id_fkey FOREIGN KEY (synonym_id) REFERENCES synonym(synonym_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: cell_lineprop_cell_line_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY cell_lineprop
    ADD CONSTRAINT cell_lineprop_cell_line_id_fkey FOREIGN KEY (cell_line_id) REFERENCES cell_line(cell_line_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: cell_lineprop_pub_cell_lineprop_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY cell_lineprop_pub
    ADD CONSTRAINT cell_lineprop_pub_cell_lineprop_id_fkey FOREIGN KEY (cell_lineprop_id) REFERENCES cell_lineprop(cell_lineprop_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: cell_lineprop_pub_pub_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY cell_lineprop_pub
    ADD CONSTRAINT cell_lineprop_pub_pub_id_fkey FOREIGN KEY (pub_id) REFERENCES pub(pub_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: cell_lineprop_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY cell_lineprop
    ADD CONSTRAINT cell_lineprop_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: chadoprop_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY chadoprop
    ADD CONSTRAINT chadoprop_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: contact_relationship_object_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY contact_relationship
    ADD CONSTRAINT contact_relationship_object_id_fkey FOREIGN KEY (object_id) REFERENCES contact(contact_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: contact_relationship_subject_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY contact_relationship
    ADD CONSTRAINT contact_relationship_subject_id_fkey FOREIGN KEY (subject_id) REFERENCES contact(contact_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: contact_relationship_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY contact_relationship
    ADD CONSTRAINT contact_relationship_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: contact_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY contact
    ADD CONSTRAINT contact_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id);


--
-- Name: control_assay_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY control
    ADD CONSTRAINT control_assay_id_fkey FOREIGN KEY (assay_id) REFERENCES assay(assay_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: control_tableinfo_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY control
    ADD CONSTRAINT control_tableinfo_id_fkey FOREIGN KEY (tableinfo_id) REFERENCES tableinfo(tableinfo_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: control_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY control
    ADD CONSTRAINT control_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: cvprop_cv_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY cvprop
    ADD CONSTRAINT cvprop_cv_id_fkey FOREIGN KEY (cv_id) REFERENCES cv(cv_id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: cvprop_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY cvprop
    ADD CONSTRAINT cvprop_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: cvterm_cv_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY cvterm
    ADD CONSTRAINT cvterm_cv_id_fkey FOREIGN KEY (cv_id) REFERENCES cv(cv_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: cvterm_dbxref_cvterm_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY cvterm_dbxref
    ADD CONSTRAINT cvterm_dbxref_cvterm_id_fkey FOREIGN KEY (cvterm_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: cvterm_dbxref_dbxref_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY cvterm_dbxref
    ADD CONSTRAINT cvterm_dbxref_dbxref_id_fkey FOREIGN KEY (dbxref_id) REFERENCES dbxref(dbxref_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: cvterm_dbxref_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY cvterm
    ADD CONSTRAINT cvterm_dbxref_id_fkey FOREIGN KEY (dbxref_id) REFERENCES dbxref(dbxref_id) ON DELETE SET NULL DEFERRABLE INITIALLY DEFERRED;


--
-- Name: cvterm_relationship_object_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY cvterm_relationship
    ADD CONSTRAINT cvterm_relationship_object_id_fkey FOREIGN KEY (object_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: cvterm_relationship_subject_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY cvterm_relationship
    ADD CONSTRAINT cvterm_relationship_subject_id_fkey FOREIGN KEY (subject_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: cvterm_relationship_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY cvterm_relationship
    ADD CONSTRAINT cvterm_relationship_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: cvtermpath_cv_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY cvtermpath
    ADD CONSTRAINT cvtermpath_cv_id_fkey FOREIGN KEY (cv_id) REFERENCES cv(cv_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: cvtermpath_object_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY cvtermpath
    ADD CONSTRAINT cvtermpath_object_id_fkey FOREIGN KEY (object_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: cvtermpath_subject_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY cvtermpath
    ADD CONSTRAINT cvtermpath_subject_id_fkey FOREIGN KEY (subject_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: cvtermpath_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY cvtermpath
    ADD CONSTRAINT cvtermpath_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE SET NULL DEFERRABLE INITIALLY DEFERRED;


--
-- Name: cvtermprop_cvterm_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY cvtermprop
    ADD CONSTRAINT cvtermprop_cvterm_id_fkey FOREIGN KEY (cvterm_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE;


--
-- Name: cvtermprop_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY cvtermprop
    ADD CONSTRAINT cvtermprop_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE;


--
-- Name: cvtermsynonym_cvterm_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY cvtermsynonym
    ADD CONSTRAINT cvtermsynonym_cvterm_id_fkey FOREIGN KEY (cvterm_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: cvtermsynonym_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY cvtermsynonym
    ADD CONSTRAINT cvtermsynonym_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: dbxref_db_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY dbxref
    ADD CONSTRAINT dbxref_db_id_fkey FOREIGN KEY (db_id) REFERENCES db(db_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: dbxrefprop_dbxref_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY dbxrefprop
    ADD CONSTRAINT dbxrefprop_dbxref_id_fkey FOREIGN KEY (dbxref_id) REFERENCES dbxref(dbxref_id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: dbxrefprop_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY dbxrefprop
    ADD CONSTRAINT dbxrefprop_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: element_arraydesign_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY element
    ADD CONSTRAINT element_arraydesign_id_fkey FOREIGN KEY (arraydesign_id) REFERENCES arraydesign(arraydesign_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: element_dbxref_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY element
    ADD CONSTRAINT element_dbxref_id_fkey FOREIGN KEY (dbxref_id) REFERENCES dbxref(dbxref_id) ON DELETE SET NULL DEFERRABLE INITIALLY DEFERRED;


--
-- Name: element_feature_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY element
    ADD CONSTRAINT element_feature_id_fkey FOREIGN KEY (feature_id) REFERENCES feature(feature_id) ON DELETE SET NULL DEFERRABLE INITIALLY DEFERRED;


--
-- Name: element_relationship_object_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY element_relationship
    ADD CONSTRAINT element_relationship_object_id_fkey FOREIGN KEY (object_id) REFERENCES element(element_id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: element_relationship_subject_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY element_relationship
    ADD CONSTRAINT element_relationship_subject_id_fkey FOREIGN KEY (subject_id) REFERENCES element(element_id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: element_relationship_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY element_relationship
    ADD CONSTRAINT element_relationship_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: element_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY element
    ADD CONSTRAINT element_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE SET NULL DEFERRABLE INITIALLY DEFERRED;


--
-- Name: elementresult_element_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY elementresult
    ADD CONSTRAINT elementresult_element_id_fkey FOREIGN KEY (element_id) REFERENCES element(element_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: elementresult_quantification_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY elementresult
    ADD CONSTRAINT elementresult_quantification_id_fkey FOREIGN KEY (quantification_id) REFERENCES quantification(quantification_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: elementresult_relationship_object_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY elementresult_relationship
    ADD CONSTRAINT elementresult_relationship_object_id_fkey FOREIGN KEY (object_id) REFERENCES elementresult(elementresult_id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: elementresult_relationship_subject_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY elementresult_relationship
    ADD CONSTRAINT elementresult_relationship_subject_id_fkey FOREIGN KEY (subject_id) REFERENCES elementresult(elementresult_id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: elementresult_relationship_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY elementresult_relationship
    ADD CONSTRAINT elementresult_relationship_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: environment_cvterm_cvterm_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY environment_cvterm
    ADD CONSTRAINT environment_cvterm_cvterm_id_fkey FOREIGN KEY (cvterm_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE;


--
-- Name: environment_cvterm_environment_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY environment_cvterm
    ADD CONSTRAINT environment_cvterm_environment_id_fkey FOREIGN KEY (environment_id) REFERENCES environment(environment_id) ON DELETE CASCADE;


--
-- Name: expression_cvterm_cvterm_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY expression_cvterm
    ADD CONSTRAINT expression_cvterm_cvterm_id_fkey FOREIGN KEY (cvterm_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: expression_cvterm_cvterm_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY expression_cvterm
    ADD CONSTRAINT expression_cvterm_cvterm_type_id_fkey FOREIGN KEY (cvterm_type_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: expression_cvterm_expression_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY expression_cvterm
    ADD CONSTRAINT expression_cvterm_expression_id_fkey FOREIGN KEY (expression_id) REFERENCES expression(expression_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: expression_cvtermprop_expression_cvterm_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY expression_cvtermprop
    ADD CONSTRAINT expression_cvtermprop_expression_cvterm_id_fkey FOREIGN KEY (expression_cvterm_id) REFERENCES expression_cvterm(expression_cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: expression_cvtermprop_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY expression_cvtermprop
    ADD CONSTRAINT expression_cvtermprop_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: expression_image_eimage_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY expression_image
    ADD CONSTRAINT expression_image_eimage_id_fkey FOREIGN KEY (eimage_id) REFERENCES eimage(eimage_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: expression_image_expression_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY expression_image
    ADD CONSTRAINT expression_image_expression_id_fkey FOREIGN KEY (expression_id) REFERENCES expression(expression_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: expression_pub_expression_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY expression_pub
    ADD CONSTRAINT expression_pub_expression_id_fkey FOREIGN KEY (expression_id) REFERENCES expression(expression_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: expression_pub_pub_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY expression_pub
    ADD CONSTRAINT expression_pub_pub_id_fkey FOREIGN KEY (pub_id) REFERENCES pub(pub_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: expressionprop_expression_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY expressionprop
    ADD CONSTRAINT expressionprop_expression_id_fkey FOREIGN KEY (expression_id) REFERENCES expression(expression_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: expressionprop_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY expressionprop
    ADD CONSTRAINT expressionprop_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: feature_cvterm_cvterm_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY feature_cvterm
    ADD CONSTRAINT feature_cvterm_cvterm_id_fkey FOREIGN KEY (cvterm_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: feature_cvterm_dbxref_dbxref_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY feature_cvterm_dbxref
    ADD CONSTRAINT feature_cvterm_dbxref_dbxref_id_fkey FOREIGN KEY (dbxref_id) REFERENCES dbxref(dbxref_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: feature_cvterm_dbxref_feature_cvterm_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY feature_cvterm_dbxref
    ADD CONSTRAINT feature_cvterm_dbxref_feature_cvterm_id_fkey FOREIGN KEY (feature_cvterm_id) REFERENCES feature_cvterm(feature_cvterm_id) ON DELETE CASCADE;


--
-- Name: feature_cvterm_feature_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY feature_cvterm
    ADD CONSTRAINT feature_cvterm_feature_id_fkey FOREIGN KEY (feature_id) REFERENCES feature(feature_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: feature_cvterm_pub_feature_cvterm_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY feature_cvterm_pub
    ADD CONSTRAINT feature_cvterm_pub_feature_cvterm_id_fkey FOREIGN KEY (feature_cvterm_id) REFERENCES feature_cvterm(feature_cvterm_id) ON DELETE CASCADE;


--
-- Name: feature_cvterm_pub_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY feature_cvterm
    ADD CONSTRAINT feature_cvterm_pub_id_fkey FOREIGN KEY (pub_id) REFERENCES pub(pub_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: feature_cvterm_pub_pub_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY feature_cvterm_pub
    ADD CONSTRAINT feature_cvterm_pub_pub_id_fkey FOREIGN KEY (pub_id) REFERENCES pub(pub_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: feature_cvtermprop_feature_cvterm_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY feature_cvtermprop
    ADD CONSTRAINT feature_cvtermprop_feature_cvterm_id_fkey FOREIGN KEY (feature_cvterm_id) REFERENCES feature_cvterm(feature_cvterm_id) ON DELETE CASCADE;


--
-- Name: feature_cvtermprop_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY feature_cvtermprop
    ADD CONSTRAINT feature_cvtermprop_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: feature_dbxref_dbxref_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY feature_dbxref
    ADD CONSTRAINT feature_dbxref_dbxref_id_fkey FOREIGN KEY (dbxref_id) REFERENCES dbxref(dbxref_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: feature_dbxref_feature_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY feature_dbxref
    ADD CONSTRAINT feature_dbxref_feature_id_fkey FOREIGN KEY (feature_id) REFERENCES feature(feature_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: feature_dbxref_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY feature
    ADD CONSTRAINT feature_dbxref_id_fkey FOREIGN KEY (dbxref_id) REFERENCES dbxref(dbxref_id) ON DELETE SET NULL DEFERRABLE INITIALLY DEFERRED;


--
-- Name: feature_expression_expression_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY feature_expression
    ADD CONSTRAINT feature_expression_expression_id_fkey FOREIGN KEY (expression_id) REFERENCES expression(expression_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: feature_expression_feature_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY feature_expression
    ADD CONSTRAINT feature_expression_feature_id_fkey FOREIGN KEY (feature_id) REFERENCES feature(feature_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: feature_expression_pub_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY feature_expression
    ADD CONSTRAINT feature_expression_pub_id_fkey FOREIGN KEY (pub_id) REFERENCES pub(pub_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: feature_expressionprop_feature_expression_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY feature_expressionprop
    ADD CONSTRAINT feature_expressionprop_feature_expression_id_fkey FOREIGN KEY (feature_expression_id) REFERENCES feature_expression(feature_expression_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: feature_expressionprop_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY feature_expressionprop
    ADD CONSTRAINT feature_expressionprop_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: feature_genotype_chromosome_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY feature_genotype
    ADD CONSTRAINT feature_genotype_chromosome_id_fkey FOREIGN KEY (chromosome_id) REFERENCES feature(feature_id) ON DELETE SET NULL;


--
-- Name: feature_genotype_cvterm_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY feature_genotype
    ADD CONSTRAINT feature_genotype_cvterm_id_fkey FOREIGN KEY (cvterm_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE;


--
-- Name: feature_genotype_feature_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY feature_genotype
    ADD CONSTRAINT feature_genotype_feature_id_fkey FOREIGN KEY (feature_id) REFERENCES feature(feature_id) ON DELETE CASCADE;


--
-- Name: feature_genotype_genotype_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY feature_genotype
    ADD CONSTRAINT feature_genotype_genotype_id_fkey FOREIGN KEY (genotype_id) REFERENCES genotype(genotype_id) ON DELETE CASCADE;


--
-- Name: feature_organism_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY feature
    ADD CONSTRAINT feature_organism_id_fkey FOREIGN KEY (organism_id) REFERENCES organism(organism_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: feature_phenotype_feature_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY feature_phenotype
    ADD CONSTRAINT feature_phenotype_feature_id_fkey FOREIGN KEY (feature_id) REFERENCES feature(feature_id) ON DELETE CASCADE;


--
-- Name: feature_phenotype_phenotype_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY feature_phenotype
    ADD CONSTRAINT feature_phenotype_phenotype_id_fkey FOREIGN KEY (phenotype_id) REFERENCES phenotype(phenotype_id) ON DELETE CASCADE;


--
-- Name: feature_pub_feature_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY feature_pub
    ADD CONSTRAINT feature_pub_feature_id_fkey FOREIGN KEY (feature_id) REFERENCES feature(feature_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: feature_pub_pub_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY feature_pub
    ADD CONSTRAINT feature_pub_pub_id_fkey FOREIGN KEY (pub_id) REFERENCES pub(pub_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: feature_pubprop_feature_pub_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY feature_pubprop
    ADD CONSTRAINT feature_pubprop_feature_pub_id_fkey FOREIGN KEY (feature_pub_id) REFERENCES feature_pub(feature_pub_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: feature_pubprop_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY feature_pubprop
    ADD CONSTRAINT feature_pubprop_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: feature_relationship_object_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY feature_relationship
    ADD CONSTRAINT feature_relationship_object_id_fkey FOREIGN KEY (object_id) REFERENCES feature(feature_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: feature_relationship_pub_feature_relationship_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY feature_relationship_pub
    ADD CONSTRAINT feature_relationship_pub_feature_relationship_id_fkey FOREIGN KEY (feature_relationship_id) REFERENCES feature_relationship(feature_relationship_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: feature_relationship_pub_pub_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY feature_relationship_pub
    ADD CONSTRAINT feature_relationship_pub_pub_id_fkey FOREIGN KEY (pub_id) REFERENCES pub(pub_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: feature_relationship_subject_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY feature_relationship
    ADD CONSTRAINT feature_relationship_subject_id_fkey FOREIGN KEY (subject_id) REFERENCES feature(feature_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: feature_relationship_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY feature_relationship
    ADD CONSTRAINT feature_relationship_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: feature_relationshipprop_feature_relationship_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY feature_relationshipprop
    ADD CONSTRAINT feature_relationshipprop_feature_relationship_id_fkey FOREIGN KEY (feature_relationship_id) REFERENCES feature_relationship(feature_relationship_id) ON DELETE CASCADE;


--
-- Name: feature_relationshipprop_pub_feature_relationshipprop_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY feature_relationshipprop_pub
    ADD CONSTRAINT feature_relationshipprop_pub_feature_relationshipprop_id_fkey FOREIGN KEY (feature_relationshipprop_id) REFERENCES feature_relationshipprop(feature_relationshipprop_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: feature_relationshipprop_pub_pub_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY feature_relationshipprop_pub
    ADD CONSTRAINT feature_relationshipprop_pub_pub_id_fkey FOREIGN KEY (pub_id) REFERENCES pub(pub_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: feature_relationshipprop_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY feature_relationshipprop
    ADD CONSTRAINT feature_relationshipprop_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: feature_synonym_feature_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY feature_synonym
    ADD CONSTRAINT feature_synonym_feature_id_fkey FOREIGN KEY (feature_id) REFERENCES feature(feature_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: feature_synonym_pub_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY feature_synonym
    ADD CONSTRAINT feature_synonym_pub_id_fkey FOREIGN KEY (pub_id) REFERENCES pub(pub_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: feature_synonym_synonym_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY feature_synonym
    ADD CONSTRAINT feature_synonym_synonym_id_fkey FOREIGN KEY (synonym_id) REFERENCES synonym(synonym_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: feature_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY feature
    ADD CONSTRAINT feature_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: featureloc_feature_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY featureloc
    ADD CONSTRAINT featureloc_feature_id_fkey FOREIGN KEY (feature_id) REFERENCES feature(feature_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: featureloc_pub_featureloc_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY featureloc_pub
    ADD CONSTRAINT featureloc_pub_featureloc_id_fkey FOREIGN KEY (featureloc_id) REFERENCES featureloc(featureloc_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: featureloc_pub_pub_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY featureloc_pub
    ADD CONSTRAINT featureloc_pub_pub_id_fkey FOREIGN KEY (pub_id) REFERENCES pub(pub_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: featureloc_srcfeature_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY featureloc
    ADD CONSTRAINT featureloc_srcfeature_id_fkey FOREIGN KEY (srcfeature_id) REFERENCES feature(feature_id) ON DELETE SET NULL DEFERRABLE INITIALLY DEFERRED;


--
-- Name: featuremap_pub_featuremap_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY featuremap_pub
    ADD CONSTRAINT featuremap_pub_featuremap_id_fkey FOREIGN KEY (featuremap_id) REFERENCES featuremap(featuremap_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: featuremap_pub_pub_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY featuremap_pub
    ADD CONSTRAINT featuremap_pub_pub_id_fkey FOREIGN KEY (pub_id) REFERENCES pub(pub_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: featuremap_unittype_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY featuremap
    ADD CONSTRAINT featuremap_unittype_id_fkey FOREIGN KEY (unittype_id) REFERENCES cvterm(cvterm_id) ON DELETE SET NULL DEFERRABLE INITIALLY DEFERRED;


--
-- Name: featurepos_feature_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY featurepos
    ADD CONSTRAINT featurepos_feature_id_fkey FOREIGN KEY (feature_id) REFERENCES feature(feature_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: featurepos_featuremap_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY featurepos
    ADD CONSTRAINT featurepos_featuremap_id_fkey FOREIGN KEY (featuremap_id) REFERENCES featuremap(featuremap_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: featurepos_map_feature_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY featurepos
    ADD CONSTRAINT featurepos_map_feature_id_fkey FOREIGN KEY (map_feature_id) REFERENCES feature(feature_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: featureprop_feature_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY featureprop
    ADD CONSTRAINT featureprop_feature_id_fkey FOREIGN KEY (feature_id) REFERENCES feature(feature_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: featureprop_pub_featureprop_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY featureprop_pub
    ADD CONSTRAINT featureprop_pub_featureprop_id_fkey FOREIGN KEY (featureprop_id) REFERENCES featureprop(featureprop_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: featureprop_pub_pub_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY featureprop_pub
    ADD CONSTRAINT featureprop_pub_pub_id_fkey FOREIGN KEY (pub_id) REFERENCES pub(pub_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: featureprop_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY featureprop
    ADD CONSTRAINT featureprop_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: featurerange_feature_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY featurerange
    ADD CONSTRAINT featurerange_feature_id_fkey FOREIGN KEY (feature_id) REFERENCES feature(feature_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: featurerange_featuremap_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY featurerange
    ADD CONSTRAINT featurerange_featuremap_id_fkey FOREIGN KEY (featuremap_id) REFERENCES featuremap(featuremap_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: featurerange_leftendf_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY featurerange
    ADD CONSTRAINT featurerange_leftendf_id_fkey FOREIGN KEY (leftendf_id) REFERENCES feature(feature_id) ON DELETE SET NULL DEFERRABLE INITIALLY DEFERRED;


--
-- Name: featurerange_leftstartf_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY featurerange
    ADD CONSTRAINT featurerange_leftstartf_id_fkey FOREIGN KEY (leftstartf_id) REFERENCES feature(feature_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: featurerange_rightendf_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY featurerange
    ADD CONSTRAINT featurerange_rightendf_id_fkey FOREIGN KEY (rightendf_id) REFERENCES feature(feature_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: featurerange_rightstartf_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY featurerange
    ADD CONSTRAINT featurerange_rightstartf_id_fkey FOREIGN KEY (rightstartf_id) REFERENCES feature(feature_id) ON DELETE SET NULL DEFERRABLE INITIALLY DEFERRED;


--
-- Name: genotype_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY genotype
    ADD CONSTRAINT genotype_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE;


--
-- Name: genotypeprop_genotype_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY genotypeprop
    ADD CONSTRAINT genotypeprop_genotype_id_fkey FOREIGN KEY (genotype_id) REFERENCES genotype(genotype_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: genotypeprop_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY genotypeprop
    ADD CONSTRAINT genotypeprop_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: library_cvterm_cvterm_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY library_cvterm
    ADD CONSTRAINT library_cvterm_cvterm_id_fkey FOREIGN KEY (cvterm_id) REFERENCES cvterm(cvterm_id);


--
-- Name: library_cvterm_library_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY library_cvterm
    ADD CONSTRAINT library_cvterm_library_id_fkey FOREIGN KEY (library_id) REFERENCES library(library_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: library_cvterm_pub_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY library_cvterm
    ADD CONSTRAINT library_cvterm_pub_id_fkey FOREIGN KEY (pub_id) REFERENCES pub(pub_id);


--
-- Name: library_dbxref_dbxref_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY library_dbxref
    ADD CONSTRAINT library_dbxref_dbxref_id_fkey FOREIGN KEY (dbxref_id) REFERENCES dbxref(dbxref_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: library_dbxref_library_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY library_dbxref
    ADD CONSTRAINT library_dbxref_library_id_fkey FOREIGN KEY (library_id) REFERENCES library(library_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: library_feature_feature_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY library_feature
    ADD CONSTRAINT library_feature_feature_id_fkey FOREIGN KEY (feature_id) REFERENCES feature(feature_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: library_feature_library_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY library_feature
    ADD CONSTRAINT library_feature_library_id_fkey FOREIGN KEY (library_id) REFERENCES library(library_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: library_organism_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY library
    ADD CONSTRAINT library_organism_id_fkey FOREIGN KEY (organism_id) REFERENCES organism(organism_id);


--
-- Name: library_pub_library_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY library_pub
    ADD CONSTRAINT library_pub_library_id_fkey FOREIGN KEY (library_id) REFERENCES library(library_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: library_pub_pub_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY library_pub
    ADD CONSTRAINT library_pub_pub_id_fkey FOREIGN KEY (pub_id) REFERENCES pub(pub_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: library_synonym_library_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY library_synonym
    ADD CONSTRAINT library_synonym_library_id_fkey FOREIGN KEY (library_id) REFERENCES library(library_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: library_synonym_pub_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY library_synonym
    ADD CONSTRAINT library_synonym_pub_id_fkey FOREIGN KEY (pub_id) REFERENCES pub(pub_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: library_synonym_synonym_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY library_synonym
    ADD CONSTRAINT library_synonym_synonym_id_fkey FOREIGN KEY (synonym_id) REFERENCES synonym(synonym_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: library_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY library
    ADD CONSTRAINT library_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id);


--
-- Name: libraryprop_library_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY libraryprop
    ADD CONSTRAINT libraryprop_library_id_fkey FOREIGN KEY (library_id) REFERENCES library(library_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: libraryprop_pub_libraryprop_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY libraryprop_pub
    ADD CONSTRAINT libraryprop_pub_libraryprop_id_fkey FOREIGN KEY (libraryprop_id) REFERENCES libraryprop(libraryprop_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: libraryprop_pub_pub_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY libraryprop_pub
    ADD CONSTRAINT libraryprop_pub_pub_id_fkey FOREIGN KEY (pub_id) REFERENCES pub(pub_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: libraryprop_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY libraryprop
    ADD CONSTRAINT libraryprop_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id);


--
-- Name: magedocumentation_mageml_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY magedocumentation
    ADD CONSTRAINT magedocumentation_mageml_id_fkey FOREIGN KEY (mageml_id) REFERENCES mageml(mageml_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: magedocumentation_tableinfo_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY magedocumentation
    ADD CONSTRAINT magedocumentation_tableinfo_id_fkey FOREIGN KEY (tableinfo_id) REFERENCES tableinfo(tableinfo_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: nd_experiment_contact_contact_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY nd_experiment_contact
    ADD CONSTRAINT nd_experiment_contact_contact_id_fkey FOREIGN KEY (contact_id) REFERENCES contact(contact_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: nd_experiment_contact_nd_experiment_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY nd_experiment_contact
    ADD CONSTRAINT nd_experiment_contact_nd_experiment_id_fkey FOREIGN KEY (nd_experiment_id) REFERENCES nd_experiment(nd_experiment_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: nd_experiment_dbxref_dbxref_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY nd_experiment_dbxref
    ADD CONSTRAINT nd_experiment_dbxref_dbxref_id_fkey FOREIGN KEY (dbxref_id) REFERENCES dbxref(dbxref_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: nd_experiment_dbxref_nd_experiment_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY nd_experiment_dbxref
    ADD CONSTRAINT nd_experiment_dbxref_nd_experiment_id_fkey FOREIGN KEY (nd_experiment_id) REFERENCES nd_experiment(nd_experiment_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: nd_experiment_genotype_genotype_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY nd_experiment_genotype
    ADD CONSTRAINT nd_experiment_genotype_genotype_id_fkey FOREIGN KEY (genotype_id) REFERENCES genotype(genotype_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: nd_experiment_genotype_nd_experiment_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY nd_experiment_genotype
    ADD CONSTRAINT nd_experiment_genotype_nd_experiment_id_fkey FOREIGN KEY (nd_experiment_id) REFERENCES nd_experiment(nd_experiment_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: nd_experiment_nd_geolocation_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY nd_experiment
    ADD CONSTRAINT nd_experiment_nd_geolocation_id_fkey FOREIGN KEY (nd_geolocation_id) REFERENCES nd_geolocation(nd_geolocation_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: nd_experiment_phenotype_nd_experiment_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY nd_experiment_phenotype
    ADD CONSTRAINT nd_experiment_phenotype_nd_experiment_id_fkey FOREIGN KEY (nd_experiment_id) REFERENCES nd_experiment(nd_experiment_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: nd_experiment_phenotype_phenotype_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY nd_experiment_phenotype
    ADD CONSTRAINT nd_experiment_phenotype_phenotype_id_fkey FOREIGN KEY (phenotype_id) REFERENCES phenotype(phenotype_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: nd_experiment_project_nd_experiment_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY nd_experiment_project
    ADD CONSTRAINT nd_experiment_project_nd_experiment_id_fkey FOREIGN KEY (nd_experiment_id) REFERENCES nd_experiment(nd_experiment_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: nd_experiment_project_project_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY nd_experiment_project
    ADD CONSTRAINT nd_experiment_project_project_id_fkey FOREIGN KEY (project_id) REFERENCES project(project_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: nd_experiment_protocol_nd_experiment_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY nd_experiment_protocol
    ADD CONSTRAINT nd_experiment_protocol_nd_experiment_id_fkey FOREIGN KEY (nd_experiment_id) REFERENCES nd_experiment(nd_experiment_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: nd_experiment_protocol_nd_protocol_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY nd_experiment_protocol
    ADD CONSTRAINT nd_experiment_protocol_nd_protocol_id_fkey FOREIGN KEY (nd_protocol_id) REFERENCES nd_protocol(nd_protocol_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: nd_experiment_pub_nd_experiment_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY nd_experiment_pub
    ADD CONSTRAINT nd_experiment_pub_nd_experiment_id_fkey FOREIGN KEY (nd_experiment_id) REFERENCES nd_experiment(nd_experiment_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: nd_experiment_pub_pub_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY nd_experiment_pub
    ADD CONSTRAINT nd_experiment_pub_pub_id_fkey FOREIGN KEY (pub_id) REFERENCES pub(pub_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: nd_experiment_stock_dbxref_dbxref_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY nd_experiment_stock_dbxref
    ADD CONSTRAINT nd_experiment_stock_dbxref_dbxref_id_fkey FOREIGN KEY (dbxref_id) REFERENCES dbxref(dbxref_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: nd_experiment_stock_dbxref_nd_experiment_stock_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY nd_experiment_stock_dbxref
    ADD CONSTRAINT nd_experiment_stock_dbxref_nd_experiment_stock_id_fkey FOREIGN KEY (nd_experiment_stock_id) REFERENCES nd_experiment_stock(nd_experiment_stock_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: nd_experiment_stock_nd_experiment_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY nd_experiment_stock
    ADD CONSTRAINT nd_experiment_stock_nd_experiment_id_fkey FOREIGN KEY (nd_experiment_id) REFERENCES nd_experiment(nd_experiment_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: nd_experiment_stock_stock_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY nd_experiment_stock
    ADD CONSTRAINT nd_experiment_stock_stock_id_fkey FOREIGN KEY (stock_id) REFERENCES stock(stock_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: nd_experiment_stock_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY nd_experiment_stock
    ADD CONSTRAINT nd_experiment_stock_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: nd_experiment_stockprop_nd_experiment_stock_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY nd_experiment_stockprop
    ADD CONSTRAINT nd_experiment_stockprop_nd_experiment_stock_id_fkey FOREIGN KEY (nd_experiment_stock_id) REFERENCES nd_experiment_stock(nd_experiment_stock_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: nd_experiment_stockprop_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY nd_experiment_stockprop
    ADD CONSTRAINT nd_experiment_stockprop_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: nd_experiment_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY nd_experiment
    ADD CONSTRAINT nd_experiment_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: nd_experimentprop_nd_experiment_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY nd_experimentprop
    ADD CONSTRAINT nd_experimentprop_nd_experiment_id_fkey FOREIGN KEY (nd_experiment_id) REFERENCES nd_experiment(nd_experiment_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: nd_experimentprop_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY nd_experimentprop
    ADD CONSTRAINT nd_experimentprop_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: nd_geolocationprop_nd_geolocation_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY nd_geolocationprop
    ADD CONSTRAINT nd_geolocationprop_nd_geolocation_id_fkey FOREIGN KEY (nd_geolocation_id) REFERENCES nd_geolocation(nd_geolocation_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: nd_geolocationprop_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY nd_geolocationprop
    ADD CONSTRAINT nd_geolocationprop_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: nd_protocol_reagent_nd_protocol_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY nd_protocol_reagent
    ADD CONSTRAINT nd_protocol_reagent_nd_protocol_id_fkey FOREIGN KEY (nd_protocol_id) REFERENCES nd_protocol(nd_protocol_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: nd_protocol_reagent_reagent_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY nd_protocol_reagent
    ADD CONSTRAINT nd_protocol_reagent_reagent_id_fkey FOREIGN KEY (reagent_id) REFERENCES nd_reagent(nd_reagent_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: nd_protocol_reagent_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY nd_protocol_reagent
    ADD CONSTRAINT nd_protocol_reagent_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: nd_protocol_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY nd_protocol
    ADD CONSTRAINT nd_protocol_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: nd_protocolprop_nd_protocol_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY nd_protocolprop
    ADD CONSTRAINT nd_protocolprop_nd_protocol_id_fkey FOREIGN KEY (nd_protocol_id) REFERENCES nd_protocol(nd_protocol_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: nd_protocolprop_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY nd_protocolprop
    ADD CONSTRAINT nd_protocolprop_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: nd_reagent_relationship_object_reagent_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY nd_reagent_relationship
    ADD CONSTRAINT nd_reagent_relationship_object_reagent_id_fkey FOREIGN KEY (object_reagent_id) REFERENCES nd_reagent(nd_reagent_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: nd_reagent_relationship_subject_reagent_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY nd_reagent_relationship
    ADD CONSTRAINT nd_reagent_relationship_subject_reagent_id_fkey FOREIGN KEY (subject_reagent_id) REFERENCES nd_reagent(nd_reagent_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: nd_reagent_relationship_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY nd_reagent_relationship
    ADD CONSTRAINT nd_reagent_relationship_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: nd_reagent_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY nd_reagent
    ADD CONSTRAINT nd_reagent_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: nd_reagentprop_nd_reagent_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY nd_reagentprop
    ADD CONSTRAINT nd_reagentprop_nd_reagent_id_fkey FOREIGN KEY (nd_reagent_id) REFERENCES nd_reagent(nd_reagent_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: nd_reagentprop_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY nd_reagentprop
    ADD CONSTRAINT nd_reagentprop_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: organism_dbxref_dbxref_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY organism_dbxref
    ADD CONSTRAINT organism_dbxref_dbxref_id_fkey FOREIGN KEY (dbxref_id) REFERENCES dbxref(dbxref_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: organism_dbxref_organism_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY organism_dbxref
    ADD CONSTRAINT organism_dbxref_organism_id_fkey FOREIGN KEY (organism_id) REFERENCES organism(organism_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: organismprop_organism_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY organismprop
    ADD CONSTRAINT organismprop_organism_id_fkey FOREIGN KEY (organism_id) REFERENCES organism(organism_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: organismprop_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY organismprop
    ADD CONSTRAINT organismprop_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: phendesc_environment_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY phendesc
    ADD CONSTRAINT phendesc_environment_id_fkey FOREIGN KEY (environment_id) REFERENCES environment(environment_id) ON DELETE CASCADE;


--
-- Name: phendesc_genotype_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY phendesc
    ADD CONSTRAINT phendesc_genotype_id_fkey FOREIGN KEY (genotype_id) REFERENCES genotype(genotype_id) ON DELETE CASCADE;


--
-- Name: phendesc_pub_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY phendesc
    ADD CONSTRAINT phendesc_pub_id_fkey FOREIGN KEY (pub_id) REFERENCES pub(pub_id) ON DELETE CASCADE;


--
-- Name: phendesc_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY phendesc
    ADD CONSTRAINT phendesc_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE;


--
-- Name: phenotype_assay_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY phenotype
    ADD CONSTRAINT phenotype_assay_id_fkey FOREIGN KEY (assay_id) REFERENCES cvterm(cvterm_id) ON DELETE SET NULL;


--
-- Name: phenotype_attr_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY phenotype
    ADD CONSTRAINT phenotype_attr_id_fkey FOREIGN KEY (attr_id) REFERENCES cvterm(cvterm_id) ON DELETE SET NULL;


--
-- Name: phenotype_comparison_cvterm_cvterm_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY phenotype_comparison_cvterm
    ADD CONSTRAINT phenotype_comparison_cvterm_cvterm_id_fkey FOREIGN KEY (cvterm_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE;


--
-- Name: phenotype_comparison_cvterm_phenotype_comparison_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY phenotype_comparison_cvterm
    ADD CONSTRAINT phenotype_comparison_cvterm_phenotype_comparison_id_fkey FOREIGN KEY (phenotype_comparison_id) REFERENCES phenotype_comparison(phenotype_comparison_id) ON DELETE CASCADE;


--
-- Name: phenotype_comparison_cvterm_pub_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY phenotype_comparison_cvterm
    ADD CONSTRAINT phenotype_comparison_cvterm_pub_id_fkey FOREIGN KEY (pub_id) REFERENCES pub(pub_id) ON DELETE CASCADE;


--
-- Name: phenotype_comparison_environment1_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY phenotype_comparison
    ADD CONSTRAINT phenotype_comparison_environment1_id_fkey FOREIGN KEY (environment1_id) REFERENCES environment(environment_id) ON DELETE CASCADE;


--
-- Name: phenotype_comparison_environment2_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY phenotype_comparison
    ADD CONSTRAINT phenotype_comparison_environment2_id_fkey FOREIGN KEY (environment2_id) REFERENCES environment(environment_id) ON DELETE CASCADE;


--
-- Name: phenotype_comparison_genotype1_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY phenotype_comparison
    ADD CONSTRAINT phenotype_comparison_genotype1_id_fkey FOREIGN KEY (genotype1_id) REFERENCES genotype(genotype_id) ON DELETE CASCADE;


--
-- Name: phenotype_comparison_genotype2_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY phenotype_comparison
    ADD CONSTRAINT phenotype_comparison_genotype2_id_fkey FOREIGN KEY (genotype2_id) REFERENCES genotype(genotype_id) ON DELETE CASCADE;


--
-- Name: phenotype_comparison_organism_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY phenotype_comparison
    ADD CONSTRAINT phenotype_comparison_organism_id_fkey FOREIGN KEY (organism_id) REFERENCES organism(organism_id) ON DELETE CASCADE;


--
-- Name: phenotype_comparison_phenotype1_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY phenotype_comparison
    ADD CONSTRAINT phenotype_comparison_phenotype1_id_fkey FOREIGN KEY (phenotype1_id) REFERENCES phenotype(phenotype_id) ON DELETE CASCADE;


--
-- Name: phenotype_comparison_phenotype2_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY phenotype_comparison
    ADD CONSTRAINT phenotype_comparison_phenotype2_id_fkey FOREIGN KEY (phenotype2_id) REFERENCES phenotype(phenotype_id) ON DELETE CASCADE;


--
-- Name: phenotype_comparison_pub_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY phenotype_comparison
    ADD CONSTRAINT phenotype_comparison_pub_id_fkey FOREIGN KEY (pub_id) REFERENCES pub(pub_id) ON DELETE CASCADE;


--
-- Name: phenotype_cvalue_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY phenotype
    ADD CONSTRAINT phenotype_cvalue_id_fkey FOREIGN KEY (cvalue_id) REFERENCES cvterm(cvterm_id) ON DELETE SET NULL;


--
-- Name: phenotype_cvterm_cvterm_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY phenotype_cvterm
    ADD CONSTRAINT phenotype_cvterm_cvterm_id_fkey FOREIGN KEY (cvterm_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE;


--
-- Name: phenotype_cvterm_phenotype_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY phenotype_cvterm
    ADD CONSTRAINT phenotype_cvterm_phenotype_id_fkey FOREIGN KEY (phenotype_id) REFERENCES phenotype(phenotype_id) ON DELETE CASCADE;


--
-- Name: phenotype_observable_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY phenotype
    ADD CONSTRAINT phenotype_observable_id_fkey FOREIGN KEY (observable_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE;


--
-- Name: phenstatement_environment_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY phenstatement
    ADD CONSTRAINT phenstatement_environment_id_fkey FOREIGN KEY (environment_id) REFERENCES environment(environment_id) ON DELETE CASCADE;


--
-- Name: phenstatement_genotype_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY phenstatement
    ADD CONSTRAINT phenstatement_genotype_id_fkey FOREIGN KEY (genotype_id) REFERENCES genotype(genotype_id) ON DELETE CASCADE;


--
-- Name: phenstatement_phenotype_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY phenstatement
    ADD CONSTRAINT phenstatement_phenotype_id_fkey FOREIGN KEY (phenotype_id) REFERENCES phenotype(phenotype_id) ON DELETE CASCADE;


--
-- Name: phenstatement_pub_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY phenstatement
    ADD CONSTRAINT phenstatement_pub_id_fkey FOREIGN KEY (pub_id) REFERENCES pub(pub_id) ON DELETE CASCADE;


--
-- Name: phenstatement_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY phenstatement
    ADD CONSTRAINT phenstatement_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE;


--
-- Name: phylonode_dbxref_dbxref_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY phylonode_dbxref
    ADD CONSTRAINT phylonode_dbxref_dbxref_id_fkey FOREIGN KEY (dbxref_id) REFERENCES dbxref(dbxref_id) ON DELETE CASCADE;


--
-- Name: phylonode_dbxref_phylonode_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY phylonode_dbxref
    ADD CONSTRAINT phylonode_dbxref_phylonode_id_fkey FOREIGN KEY (phylonode_id) REFERENCES phylonode(phylonode_id) ON DELETE CASCADE;


--
-- Name: phylonode_feature_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY phylonode
    ADD CONSTRAINT phylonode_feature_id_fkey FOREIGN KEY (feature_id) REFERENCES feature(feature_id) ON DELETE CASCADE;


--
-- Name: phylonode_organism_organism_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY phylonode_organism
    ADD CONSTRAINT phylonode_organism_organism_id_fkey FOREIGN KEY (organism_id) REFERENCES organism(organism_id) ON DELETE CASCADE;


--
-- Name: phylonode_organism_phylonode_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY phylonode_organism
    ADD CONSTRAINT phylonode_organism_phylonode_id_fkey FOREIGN KEY (phylonode_id) REFERENCES phylonode(phylonode_id) ON DELETE CASCADE;


--
-- Name: phylonode_parent_phylonode_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY phylonode
    ADD CONSTRAINT phylonode_parent_phylonode_id_fkey FOREIGN KEY (parent_phylonode_id) REFERENCES phylonode(phylonode_id) ON DELETE CASCADE;


--
-- Name: phylonode_phylotree_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY phylonode
    ADD CONSTRAINT phylonode_phylotree_id_fkey FOREIGN KEY (phylotree_id) REFERENCES phylotree(phylotree_id) ON DELETE CASCADE;


--
-- Name: phylonode_pub_phylonode_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY phylonode_pub
    ADD CONSTRAINT phylonode_pub_phylonode_id_fkey FOREIGN KEY (phylonode_id) REFERENCES phylonode(phylonode_id) ON DELETE CASCADE;


--
-- Name: phylonode_pub_pub_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY phylonode_pub
    ADD CONSTRAINT phylonode_pub_pub_id_fkey FOREIGN KEY (pub_id) REFERENCES pub(pub_id) ON DELETE CASCADE;


--
-- Name: phylonode_relationship_object_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY phylonode_relationship
    ADD CONSTRAINT phylonode_relationship_object_id_fkey FOREIGN KEY (object_id) REFERENCES phylonode(phylonode_id) ON DELETE CASCADE;


--
-- Name: phylonode_relationship_phylotree_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY phylonode_relationship
    ADD CONSTRAINT phylonode_relationship_phylotree_id_fkey FOREIGN KEY (phylotree_id) REFERENCES phylotree(phylotree_id) ON DELETE CASCADE;


--
-- Name: phylonode_relationship_subject_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY phylonode_relationship
    ADD CONSTRAINT phylonode_relationship_subject_id_fkey FOREIGN KEY (subject_id) REFERENCES phylonode(phylonode_id) ON DELETE CASCADE;


--
-- Name: phylonode_relationship_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY phylonode_relationship
    ADD CONSTRAINT phylonode_relationship_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE;


--
-- Name: phylonode_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY phylonode
    ADD CONSTRAINT phylonode_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE;


--
-- Name: phylonodeprop_phylonode_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY phylonodeprop
    ADD CONSTRAINT phylonodeprop_phylonode_id_fkey FOREIGN KEY (phylonode_id) REFERENCES phylonode(phylonode_id) ON DELETE CASCADE;


--
-- Name: phylonodeprop_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY phylonodeprop
    ADD CONSTRAINT phylonodeprop_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE;


--
-- Name: phylotree_analysis_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY phylotree
    ADD CONSTRAINT phylotree_analysis_id_fkey FOREIGN KEY (analysis_id) REFERENCES analysis(analysis_id) ON DELETE CASCADE;


--
-- Name: phylotree_dbxref_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY phylotree
    ADD CONSTRAINT phylotree_dbxref_id_fkey FOREIGN KEY (dbxref_id) REFERENCES dbxref(dbxref_id) ON DELETE CASCADE;


--
-- Name: phylotree_pub_phylotree_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY phylotree_pub
    ADD CONSTRAINT phylotree_pub_phylotree_id_fkey FOREIGN KEY (phylotree_id) REFERENCES phylotree(phylotree_id) ON DELETE CASCADE;


--
-- Name: phylotree_pub_pub_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY phylotree_pub
    ADD CONSTRAINT phylotree_pub_pub_id_fkey FOREIGN KEY (pub_id) REFERENCES pub(pub_id) ON DELETE CASCADE;


--
-- Name: phylotree_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY phylotree
    ADD CONSTRAINT phylotree_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE;


--
-- Name: project_contact_contact_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY project_contact
    ADD CONSTRAINT project_contact_contact_id_fkey FOREIGN KEY (contact_id) REFERENCES contact(contact_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: project_contact_project_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY project_contact
    ADD CONSTRAINT project_contact_project_id_fkey FOREIGN KEY (project_id) REFERENCES project(project_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: project_pub_project_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY project_pub
    ADD CONSTRAINT project_pub_project_id_fkey FOREIGN KEY (project_id) REFERENCES project(project_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: project_pub_pub_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY project_pub
    ADD CONSTRAINT project_pub_pub_id_fkey FOREIGN KEY (pub_id) REFERENCES pub(pub_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: project_relationship_object_project_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY project_relationship
    ADD CONSTRAINT project_relationship_object_project_id_fkey FOREIGN KEY (object_project_id) REFERENCES project(project_id) ON DELETE CASCADE;


--
-- Name: project_relationship_subject_project_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY project_relationship
    ADD CONSTRAINT project_relationship_subject_project_id_fkey FOREIGN KEY (subject_project_id) REFERENCES project(project_id) ON DELETE CASCADE;


--
-- Name: project_relationship_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY project_relationship
    ADD CONSTRAINT project_relationship_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE RESTRICT;


--
-- Name: projectprop_project_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY projectprop
    ADD CONSTRAINT projectprop_project_id_fkey FOREIGN KEY (project_id) REFERENCES project(project_id) ON DELETE CASCADE;


--
-- Name: projectprop_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY projectprop
    ADD CONSTRAINT projectprop_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE;


--
-- Name: protocol_dbxref_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY protocol
    ADD CONSTRAINT protocol_dbxref_id_fkey FOREIGN KEY (dbxref_id) REFERENCES dbxref(dbxref_id) ON DELETE SET NULL DEFERRABLE INITIALLY DEFERRED;


--
-- Name: protocol_pub_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY protocol
    ADD CONSTRAINT protocol_pub_id_fkey FOREIGN KEY (pub_id) REFERENCES pub(pub_id) ON DELETE SET NULL DEFERRABLE INITIALLY DEFERRED;


--
-- Name: protocol_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY protocol
    ADD CONSTRAINT protocol_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: protocolparam_datatype_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY protocolparam
    ADD CONSTRAINT protocolparam_datatype_id_fkey FOREIGN KEY (datatype_id) REFERENCES cvterm(cvterm_id) ON DELETE SET NULL DEFERRABLE INITIALLY DEFERRED;


--
-- Name: protocolparam_protocol_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY protocolparam
    ADD CONSTRAINT protocolparam_protocol_id_fkey FOREIGN KEY (protocol_id) REFERENCES protocol(protocol_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: protocolparam_unittype_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY protocolparam
    ADD CONSTRAINT protocolparam_unittype_id_fkey FOREIGN KEY (unittype_id) REFERENCES cvterm(cvterm_id) ON DELETE SET NULL DEFERRABLE INITIALLY DEFERRED;


--
-- Name: pub_dbxref_dbxref_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY pub_dbxref
    ADD CONSTRAINT pub_dbxref_dbxref_id_fkey FOREIGN KEY (dbxref_id) REFERENCES dbxref(dbxref_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: pub_dbxref_pub_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY pub_dbxref
    ADD CONSTRAINT pub_dbxref_pub_id_fkey FOREIGN KEY (pub_id) REFERENCES pub(pub_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: pub_relationship_object_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY pub_relationship
    ADD CONSTRAINT pub_relationship_object_id_fkey FOREIGN KEY (object_id) REFERENCES pub(pub_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: pub_relationship_subject_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY pub_relationship
    ADD CONSTRAINT pub_relationship_subject_id_fkey FOREIGN KEY (subject_id) REFERENCES pub(pub_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: pub_relationship_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY pub_relationship
    ADD CONSTRAINT pub_relationship_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: pub_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY pub
    ADD CONSTRAINT pub_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: pubauthor_pub_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY pubauthor
    ADD CONSTRAINT pubauthor_pub_id_fkey FOREIGN KEY (pub_id) REFERENCES pub(pub_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: pubprop_pub_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY pubprop
    ADD CONSTRAINT pubprop_pub_id_fkey FOREIGN KEY (pub_id) REFERENCES pub(pub_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: pubprop_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY pubprop
    ADD CONSTRAINT pubprop_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: quantification_acquisition_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY quantification
    ADD CONSTRAINT quantification_acquisition_id_fkey FOREIGN KEY (acquisition_id) REFERENCES acquisition(acquisition_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: quantification_analysis_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY quantification
    ADD CONSTRAINT quantification_analysis_id_fkey FOREIGN KEY (analysis_id) REFERENCES analysis(analysis_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: quantification_operator_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY quantification
    ADD CONSTRAINT quantification_operator_id_fkey FOREIGN KEY (operator_id) REFERENCES contact(contact_id) ON DELETE SET NULL DEFERRABLE INITIALLY DEFERRED;


--
-- Name: quantification_protocol_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY quantification
    ADD CONSTRAINT quantification_protocol_id_fkey FOREIGN KEY (protocol_id) REFERENCES protocol(protocol_id) ON DELETE SET NULL DEFERRABLE INITIALLY DEFERRED;


--
-- Name: quantification_relationship_object_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY quantification_relationship
    ADD CONSTRAINT quantification_relationship_object_id_fkey FOREIGN KEY (object_id) REFERENCES quantification(quantification_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: quantification_relationship_subject_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY quantification_relationship
    ADD CONSTRAINT quantification_relationship_subject_id_fkey FOREIGN KEY (subject_id) REFERENCES quantification(quantification_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: quantification_relationship_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY quantification_relationship
    ADD CONSTRAINT quantification_relationship_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: quantificationprop_quantification_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY quantificationprop
    ADD CONSTRAINT quantificationprop_quantification_id_fkey FOREIGN KEY (quantification_id) REFERENCES quantification(quantification_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: quantificationprop_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY quantificationprop
    ADD CONSTRAINT quantificationprop_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: stock_cvterm_cvterm_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY stock_cvterm
    ADD CONSTRAINT stock_cvterm_cvterm_id_fkey FOREIGN KEY (cvterm_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: stock_cvterm_pub_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY stock_cvterm
    ADD CONSTRAINT stock_cvterm_pub_id_fkey FOREIGN KEY (pub_id) REFERENCES pub(pub_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: stock_cvterm_stock_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY stock_cvterm
    ADD CONSTRAINT stock_cvterm_stock_id_fkey FOREIGN KEY (stock_id) REFERENCES stock(stock_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: stock_cvtermprop_stock_cvterm_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY stock_cvtermprop
    ADD CONSTRAINT stock_cvtermprop_stock_cvterm_id_fkey FOREIGN KEY (stock_cvterm_id) REFERENCES stock_cvterm(stock_cvterm_id) ON DELETE CASCADE;


--
-- Name: stock_cvtermprop_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY stock_cvtermprop
    ADD CONSTRAINT stock_cvtermprop_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: stock_dbxref_dbxref_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY stock_dbxref
    ADD CONSTRAINT stock_dbxref_dbxref_id_fkey FOREIGN KEY (dbxref_id) REFERENCES dbxref(dbxref_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: stock_dbxref_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY stock
    ADD CONSTRAINT stock_dbxref_id_fkey FOREIGN KEY (dbxref_id) REFERENCES dbxref(dbxref_id) ON DELETE SET NULL DEFERRABLE INITIALLY DEFERRED;


--
-- Name: stock_dbxref_stock_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY stock_dbxref
    ADD CONSTRAINT stock_dbxref_stock_id_fkey FOREIGN KEY (stock_id) REFERENCES stock(stock_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: stock_dbxrefprop_stock_dbxref_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY stock_dbxrefprop
    ADD CONSTRAINT stock_dbxrefprop_stock_dbxref_id_fkey FOREIGN KEY (stock_dbxref_id) REFERENCES stock_dbxref(stock_dbxref_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: stock_dbxrefprop_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY stock_dbxrefprop
    ADD CONSTRAINT stock_dbxrefprop_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: stock_genotype_genotype_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY stock_genotype
    ADD CONSTRAINT stock_genotype_genotype_id_fkey FOREIGN KEY (genotype_id) REFERENCES genotype(genotype_id) ON DELETE CASCADE;


--
-- Name: stock_genotype_stock_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY stock_genotype
    ADD CONSTRAINT stock_genotype_stock_id_fkey FOREIGN KEY (stock_id) REFERENCES stock(stock_id) ON DELETE CASCADE;


--
-- Name: stock_organism_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY stock
    ADD CONSTRAINT stock_organism_id_fkey FOREIGN KEY (organism_id) REFERENCES organism(organism_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: stock_pub_pub_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY stock_pub
    ADD CONSTRAINT stock_pub_pub_id_fkey FOREIGN KEY (pub_id) REFERENCES pub(pub_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: stock_pub_stock_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY stock_pub
    ADD CONSTRAINT stock_pub_stock_id_fkey FOREIGN KEY (stock_id) REFERENCES stock(stock_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: stock_relationship_cvterm_cvterm_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY stock_relationship_cvterm
    ADD CONSTRAINT stock_relationship_cvterm_cvterm_id_fkey FOREIGN KEY (cvterm_id) REFERENCES cvterm(cvterm_id) ON DELETE RESTRICT;


--
-- Name: stock_relationship_cvterm_pub_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY stock_relationship_cvterm
    ADD CONSTRAINT stock_relationship_cvterm_pub_id_fkey FOREIGN KEY (pub_id) REFERENCES pub(pub_id) ON DELETE RESTRICT;


--
-- Name: stock_relationship_cvterm_stock_relationship_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY stock_relationship_cvterm
    ADD CONSTRAINT stock_relationship_cvterm_stock_relationship_id_fkey FOREIGN KEY (stock_relationship_id) REFERENCES stock_relationship(stock_relationship_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: stock_relationship_object_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY stock_relationship
    ADD CONSTRAINT stock_relationship_object_id_fkey FOREIGN KEY (object_id) REFERENCES stock(stock_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: stock_relationship_pub_pub_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY stock_relationship_pub
    ADD CONSTRAINT stock_relationship_pub_pub_id_fkey FOREIGN KEY (pub_id) REFERENCES pub(pub_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: stock_relationship_pub_stock_relationship_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY stock_relationship_pub
    ADD CONSTRAINT stock_relationship_pub_stock_relationship_id_fkey FOREIGN KEY (stock_relationship_id) REFERENCES stock_relationship(stock_relationship_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: stock_relationship_subject_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY stock_relationship
    ADD CONSTRAINT stock_relationship_subject_id_fkey FOREIGN KEY (subject_id) REFERENCES stock(stock_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: stock_relationship_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY stock_relationship
    ADD CONSTRAINT stock_relationship_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: stock_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY stock
    ADD CONSTRAINT stock_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: stockcollection_contact_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY stockcollection
    ADD CONSTRAINT stockcollection_contact_id_fkey FOREIGN KEY (contact_id) REFERENCES contact(contact_id) ON DELETE SET NULL DEFERRABLE INITIALLY DEFERRED;


--
-- Name: stockcollection_stock_stock_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY stockcollection_stock
    ADD CONSTRAINT stockcollection_stock_stock_id_fkey FOREIGN KEY (stock_id) REFERENCES stock(stock_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: stockcollection_stock_stockcollection_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY stockcollection_stock
    ADD CONSTRAINT stockcollection_stock_stockcollection_id_fkey FOREIGN KEY (stockcollection_id) REFERENCES stockcollection(stockcollection_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: stockcollection_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY stockcollection
    ADD CONSTRAINT stockcollection_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE;


--
-- Name: stockcollectionprop_stockcollection_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY stockcollectionprop
    ADD CONSTRAINT stockcollectionprop_stockcollection_id_fkey FOREIGN KEY (stockcollection_id) REFERENCES stockcollection(stockcollection_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: stockcollectionprop_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY stockcollectionprop
    ADD CONSTRAINT stockcollectionprop_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id);


--
-- Name: stockprop_pub_pub_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY stockprop_pub
    ADD CONSTRAINT stockprop_pub_pub_id_fkey FOREIGN KEY (pub_id) REFERENCES pub(pub_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: stockprop_pub_stockprop_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY stockprop_pub
    ADD CONSTRAINT stockprop_pub_stockprop_id_fkey FOREIGN KEY (stockprop_id) REFERENCES stockprop(stockprop_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: stockprop_stock_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY stockprop
    ADD CONSTRAINT stockprop_stock_id_fkey FOREIGN KEY (stock_id) REFERENCES stock(stock_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: stockprop_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY stockprop
    ADD CONSTRAINT stockprop_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: study_assay_assay_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY study_assay
    ADD CONSTRAINT study_assay_assay_id_fkey FOREIGN KEY (assay_id) REFERENCES assay(assay_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: study_assay_study_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY study_assay
    ADD CONSTRAINT study_assay_study_id_fkey FOREIGN KEY (study_id) REFERENCES study(study_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: study_contact_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY study
    ADD CONSTRAINT study_contact_id_fkey FOREIGN KEY (contact_id) REFERENCES contact(contact_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: study_dbxref_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY study
    ADD CONSTRAINT study_dbxref_id_fkey FOREIGN KEY (dbxref_id) REFERENCES dbxref(dbxref_id) ON DELETE SET NULL DEFERRABLE INITIALLY DEFERRED;


--
-- Name: study_pub_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY study
    ADD CONSTRAINT study_pub_id_fkey FOREIGN KEY (pub_id) REFERENCES pub(pub_id) ON DELETE SET NULL DEFERRABLE INITIALLY DEFERRED;


--
-- Name: studydesign_study_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY studydesign
    ADD CONSTRAINT studydesign_study_id_fkey FOREIGN KEY (study_id) REFERENCES study(study_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: studydesignprop_studydesign_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY studydesignprop
    ADD CONSTRAINT studydesignprop_studydesign_id_fkey FOREIGN KEY (studydesign_id) REFERENCES studydesign(studydesign_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: studydesignprop_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY studydesignprop
    ADD CONSTRAINT studydesignprop_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: studyfactor_studydesign_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY studyfactor
    ADD CONSTRAINT studyfactor_studydesign_id_fkey FOREIGN KEY (studydesign_id) REFERENCES studydesign(studydesign_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: studyfactor_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY studyfactor
    ADD CONSTRAINT studyfactor_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE SET NULL DEFERRABLE INITIALLY DEFERRED;


--
-- Name: studyfactorvalue_assay_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY studyfactorvalue
    ADD CONSTRAINT studyfactorvalue_assay_id_fkey FOREIGN KEY (assay_id) REFERENCES assay(assay_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: studyfactorvalue_studyfactor_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY studyfactorvalue
    ADD CONSTRAINT studyfactorvalue_studyfactor_id_fkey FOREIGN KEY (studyfactor_id) REFERENCES studyfactor(studyfactor_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: studyprop_feature_feature_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY studyprop_feature
    ADD CONSTRAINT studyprop_feature_feature_id_fkey FOREIGN KEY (feature_id) REFERENCES feature(feature_id) ON DELETE CASCADE;


--
-- Name: studyprop_feature_studyprop_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY studyprop_feature
    ADD CONSTRAINT studyprop_feature_studyprop_id_fkey FOREIGN KEY (studyprop_id) REFERENCES studyprop(studyprop_id) ON DELETE CASCADE;


--
-- Name: studyprop_feature_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY studyprop_feature
    ADD CONSTRAINT studyprop_feature_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE;


--
-- Name: studyprop_study_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY studyprop
    ADD CONSTRAINT studyprop_study_id_fkey FOREIGN KEY (study_id) REFERENCES study(study_id) ON DELETE CASCADE;


--
-- Name: studyprop_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY studyprop
    ADD CONSTRAINT studyprop_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE;


--
-- Name: synonym_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY synonym
    ADD CONSTRAINT synonym_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: tmp_cds_handler_relationship_cds_row_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: ubuntu
--

ALTER TABLE ONLY tmp_cds_handler_relationship
    ADD CONSTRAINT tmp_cds_handler_relationship_cds_row_id_fkey FOREIGN KEY (cds_row_id) REFERENCES tmp_cds_handler(cds_row_id) ON DELETE CASCADE;


--
-- Name: treatment_biomaterial_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY treatment
    ADD CONSTRAINT treatment_biomaterial_id_fkey FOREIGN KEY (biomaterial_id) REFERENCES biomaterial(biomaterial_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: treatment_protocol_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY treatment
    ADD CONSTRAINT treatment_protocol_id_fkey FOREIGN KEY (protocol_id) REFERENCES protocol(protocol_id) ON DELETE SET NULL DEFERRABLE INITIALLY DEFERRED;


--
-- Name: treatment_type_id_fkey; Type: FK CONSTRAINT; Schema: chado; Owner: nathandunn
--

ALTER TABLE ONLY treatment
    ADD CONSTRAINT treatment_type_id_fkey FOREIGN KEY (type_id) REFERENCES cvterm(cvterm_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: public; Type: ACL; Schema: -; Owner: nathandunn
--

REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM nathandunn;
GRANT ALL ON SCHEMA public TO nathandunn;
GRANT ALL ON SCHEMA public TO PUBLIC;


--
-- Name: all_feature_names; Type: ACL; Schema: chado; Owner: ubuntu
--

REVOKE ALL ON TABLE all_feature_names FROM PUBLIC;
REVOKE ALL ON TABLE all_feature_names FROM ubuntu;
GRANT ALL ON TABLE all_feature_names TO ubuntu;
GRANT SELECT ON TABLE all_feature_names TO PUBLIC;


--
-- Name: cell_line; Type: ACL; Schema: chado; Owner: nathandunn
--

REVOKE ALL ON TABLE cell_line FROM PUBLIC;
REVOKE ALL ON TABLE cell_line FROM nathandunn;
GRANT ALL ON TABLE cell_line TO nathandunn;
GRANT ALL ON TABLE cell_line TO PUBLIC;


--
-- Name: cell_line_cvterm; Type: ACL; Schema: chado; Owner: nathandunn
--

REVOKE ALL ON TABLE cell_line_cvterm FROM PUBLIC;
REVOKE ALL ON TABLE cell_line_cvterm FROM nathandunn;
GRANT ALL ON TABLE cell_line_cvterm TO nathandunn;
GRANT ALL ON TABLE cell_line_cvterm TO PUBLIC;


--
-- Name: cell_line_cvtermprop; Type: ACL; Schema: chado; Owner: nathandunn
--

REVOKE ALL ON TABLE cell_line_cvtermprop FROM PUBLIC;
REVOKE ALL ON TABLE cell_line_cvtermprop FROM nathandunn;
GRANT ALL ON TABLE cell_line_cvtermprop TO nathandunn;
GRANT ALL ON TABLE cell_line_cvtermprop TO PUBLIC;


--
-- Name: cell_line_dbxref; Type: ACL; Schema: chado; Owner: nathandunn
--

REVOKE ALL ON TABLE cell_line_dbxref FROM PUBLIC;
REVOKE ALL ON TABLE cell_line_dbxref FROM nathandunn;
GRANT ALL ON TABLE cell_line_dbxref TO nathandunn;
GRANT ALL ON TABLE cell_line_dbxref TO PUBLIC;


--
-- Name: cell_line_feature; Type: ACL; Schema: chado; Owner: nathandunn
--

REVOKE ALL ON TABLE cell_line_feature FROM PUBLIC;
REVOKE ALL ON TABLE cell_line_feature FROM nathandunn;
GRANT ALL ON TABLE cell_line_feature TO nathandunn;
GRANT ALL ON TABLE cell_line_feature TO PUBLIC;


--
-- Name: cell_line_library; Type: ACL; Schema: chado; Owner: nathandunn
--

REVOKE ALL ON TABLE cell_line_library FROM PUBLIC;
REVOKE ALL ON TABLE cell_line_library FROM nathandunn;
GRANT ALL ON TABLE cell_line_library TO nathandunn;
GRANT ALL ON TABLE cell_line_library TO PUBLIC;


--
-- Name: cell_line_pub; Type: ACL; Schema: chado; Owner: nathandunn
--

REVOKE ALL ON TABLE cell_line_pub FROM PUBLIC;
REVOKE ALL ON TABLE cell_line_pub FROM nathandunn;
GRANT ALL ON TABLE cell_line_pub TO nathandunn;
GRANT ALL ON TABLE cell_line_pub TO PUBLIC;


--
-- Name: cell_line_relationship; Type: ACL; Schema: chado; Owner: nathandunn
--

REVOKE ALL ON TABLE cell_line_relationship FROM PUBLIC;
REVOKE ALL ON TABLE cell_line_relationship FROM nathandunn;
GRANT ALL ON TABLE cell_line_relationship TO nathandunn;
GRANT ALL ON TABLE cell_line_relationship TO PUBLIC;


--
-- Name: cell_line_synonym; Type: ACL; Schema: chado; Owner: nathandunn
--

REVOKE ALL ON TABLE cell_line_synonym FROM PUBLIC;
REVOKE ALL ON TABLE cell_line_synonym FROM nathandunn;
GRANT ALL ON TABLE cell_line_synonym TO nathandunn;
GRANT ALL ON TABLE cell_line_synonym TO PUBLIC;


--
-- Name: cell_lineprop; Type: ACL; Schema: chado; Owner: nathandunn
--

REVOKE ALL ON TABLE cell_lineprop FROM PUBLIC;
REVOKE ALL ON TABLE cell_lineprop FROM nathandunn;
GRANT ALL ON TABLE cell_lineprop TO nathandunn;
GRANT ALL ON TABLE cell_lineprop TO PUBLIC;


--
-- Name: cell_lineprop_pub; Type: ACL; Schema: chado; Owner: nathandunn
--

REVOKE ALL ON TABLE cell_lineprop_pub FROM PUBLIC;
REVOKE ALL ON TABLE cell_lineprop_pub FROM nathandunn;
GRANT ALL ON TABLE cell_lineprop_pub TO nathandunn;
GRANT ALL ON TABLE cell_lineprop_pub TO PUBLIC;


--
-- PostgreSQL database dump complete
--

