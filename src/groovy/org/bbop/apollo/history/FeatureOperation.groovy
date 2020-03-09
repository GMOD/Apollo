package org.bbop.apollo.history

/**
 * Created by ndunn on 4/7/15.
 */
enum FeatureOperation {

  ADD_FEATURE,
  DELETE_FEATURE,
  ADD_TRANSCRIPT,
  DELETE_TRANSCRIPT,
  ADD_EXON,
  DELETE_EXON,
  MERGE_EXONS,
  SPLIT_EXON,
  SET_EXON_BOUNDARIES,
  MERGE_TRANSCRIPTS,
  SPLIT_TRANSCRIPT,
  SET_TRANSLATION_START,
  UNSET_TRANSLATION_START,
  SET_TRANSLATION_END,
  UNSET_TRANSLATION_END,
  SET_TRANSLATION_ENDS,
  SET_LONGEST_ORF,
  FLIP_STRAND,
  REMOVE_CDS,
  SET_READTHROUGH_STOP_CODON,
  UNSET_READTHROUGH_STOP_CODON,
  SET_BOUNDARIES,
  CHANGE_ANNOTATION_TYPE,
  ASSOCIATE_TRANSCRIPT_TO_GENE,
  DISSOCIATE_TRANSCRIPT_FROM_GENE,
  ASSOCIATE_FEATURE_TO_GENE,
  DISSOCIATE_FEATURE_FROM_GENE,

  // structural data
  SET_SYMBOL(false),
  SET_NAME(false),
  SET_SYNONYMS(false),
  SET_DESCRIPTION(false),

  DELETE_DBXREF(false),
  SET_DBXREF(false),
  ADD_DBXREF(false),

  DELETE_COMMENT(false),
  SET_COMMENT(false),
  ADD_COMMENT(false),

  SET_STATUS(false),

  DELETE_ATTRIBUTE(false),
  SET_ATTRIBUTE(false),
  ADD_ATTRIBUTE(false),

  REMOVE_GO_ANNOTATION(false),
  UPDATE_GO_ANNOTATION(false),
  ADD_GO_ANNOTATION(false),

  private isStructural = true;

  private FeatureOperation() {}

  private FeatureOperation(Boolean isStructural) {
    this.isStructural = isStructural
  }

  def getIsStructural() {
    return isStructural
  }

  public String toLower() {
    return name().toLowerCase()
  }
}
