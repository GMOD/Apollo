define( [],
        function() {

var SequenceOntologyUtils = {};
            
/**
 *  Sequence Ontology feature types that are known to not have CDS children
 *  NOT a complete list (complete list would be extensive)
 */
SequenceOntologyUtils.neverHasCDS = {
    match: true, 
    nucleotide_match: true, 
    expressed_sequence_match: true, 
    cDNA_match: true, 
    EST_match: true, 
    RST_match: true, 
    UST_match: true, 
    primer_match: true, 
    tranlated_nucleotide_match: true, 
    protein_match: true, 
    protein_hmm_match: true, 
    alignment: true, 
    repeat: true,
    repeat_region: true,
    transposable_element: true
};
            
SequenceOntologyUtils.neverHasExons = {
    match: true, 
    nucleotide_match: true, 
    expressed_sequence_match: true, 
    cDNA_match: true, 
    EST_match: true, 
    RST_match: true, 
    UST_match: true, 
    primer_match: true, 
    tranlated_nucleotide_match: true, 
    protein_match: true, 
    protein_hmm_match: true, 
    alignment: true, 
    repeat: true,
    repeat_region: true,
    transposable_element: true
};


/** 
 *  flattened Sequence Ontology for Gene
 *     Gene and is-a descendants
 */
SequenceOntologyUtils.geneTerms = {
    gene: true, 
    gene_cassette: true, 
    gene_rearranged_at_DNA_level: true, 
    gene_silenced_by_DNA_methylation: true, 
    gene_silenced_by_DNA_modification: true, 
    gene_silenced_by_RNA_interference: true, 
    gene_silenced_by_histone_deacetylation: true, 
    gene_silenced_by_histone_methylation: true, 
    gene_silenced_by_histone_modification: true, 
    gene_with_dicistronic_mRNA: true, 
    gene_with_dicistronic_primary_transcript: true, 
    gene_with_dicistronic_transcript: true, 
    gene_with_edited_transcript: true, 
    gene_with_mRNA_recoded_by_translational_bypass: true, 
    gene_with_mRNA_with_frameshift: true, 
    gene_with_non_canonical_start_codon: true, 
    gene_with_polyadenylated_mRNA: true, 
    gene_with_polycistronic_transcript: true, 
    gene_with_recoded_mRNA: true, 
    gene_with_start_codon_CUG: true, 
    gene_with_stop_codon_read_through: true, 
    gene_with_stop_codon_redefined_as_pyrrolysine: true, 
    gene_with_stop_codon_redefined_as_selenocysteine: true, 
    gene_with_trans_spliced_transcript: true, 
    gene_with_transcript_with_translational_frameshift: true, 
    kinetoplast_gene: true, 
    leucoplast_gene: true, 
    lincRNA_gene: true, 
    maternally_imprinted_gene: true, 
    maxicircle_gene: true, 
    miRNA_gene: true, 
    minicircle_gene: true, 
    mt_gene: true, 
    ncRNA_gene: true, 
    negatively_autoregulated_gene: true, 
    nuclear_gene: true, 
    nucleomorph_gene: true, 
    paternally_imprinted_gene: true, 
    piRNA_gene: true, 
    plasmid_gene: true, 
    plastid_gene: true, 
    positional_candidate_gene: true, 
    positively_autoregulated_gene: true, 
    post_translationally_regulated_gene: true, 
    predicted_gene: true, 
    proplastid_gene: true, 
    protein_coding_gene: true, 
    proviral_gene: true, 
    rRNA_gene: true, 
    recombinationally_inverted_gene: true, 
    recombinationally_rearranged_gene: true, 
    recombinationally_rearranged_vertebrate_immune_system_gene: true, 
    rescue_gene: true, 
    retrogene: true, 
    scRNA_gene: true, 
    silenced_gene: true, 
    snoRNA_gene: true, 
    stRNA_gene: true, 
    tRNA_gene: true, 
    telomerase_RNA_gene: true, 
    tmRNA_gene: true, 
    transgene: true, 
    translationally_regulated_gene: true, 
    transposable_element_gene: true, 
    wild_type_rescue_gene: true
};

/** 
 *  flattened Sequence Ontology for Transcript
 *  Transcript and is-a descendants (including mRNA)
 */
SequenceOntologyUtils.transcriptTerms = {
    ARIA: true, 
    ARRET: true, 
    C_D_box_snoRNA: true, 
    C_D_box_snoRNA_primary_transcript: true, 
    CsrB_RsmB_RNA: true, 
    DsrA_RNA: true, 
    GcvB_RNA: true, 
    H_ACA_box_snoRNA: true, 
    H_ACA_box_snoRNA_primary_transcript: true, 
    MicF_RNA: true, 
    OxyS_RNA: true, 
    RNA_6S: true, 
    RNase_MRP_RNA: true, 
    RNase_P_RNA: true, 
    RRE_RNA: true, 
    RprA_RNA: true, 
    SRP_RNA: true, 
    SRP_RNA_primary_transcript: true, 
    TERRA: true, 
    U11_snRNA: true, 
    U12_snRNA: true, 
    U14_snoRNA: true, 
    U14_snoRNA_primary_transcript: true, 
    U1_snRNA: true, 
    U2_snRNA: true, 
    U3_snoRNA: true, 
    U4_snRNA: true, 
    U4atac_snRNA: true, 
    U5_snRNA: true, 
    U6_snRNA: true, 
    U6atac_snRNA: true, 
    Y_RNA: true, 
    aberrant_processed_transcript: true, 
    alanine_tRNA_primary_transcript: true, 
    alanyl_tRNA: true, 
    alternatively_spliced_transcript: true, 
    anti_ARRET: true, 
    antisense_RNA: true, 
    antisense_lncRNA: true, 
    antisense_primary_transcript: true, 
    arginine_tRNA_primary_transcript: true, 
    arginyl_tRNA: true, 
    asparagine_tRNA_primary_transcript: true, 
    asparaginyl_tRNA: true, 
    aspartic_acid_tRNA_primary_transcript: true, 
    aspartyl_tRNA: true, 
    capped_mRNA: true, 
    capped_primary_transcript: true, 
    class_II_RNA: true, 
    class_I_RNA: true, 
    consensus_mRNA: true, 
    cysteine_tRNA_primary_transcript: true, 
    cysteinyl_tRNA: true, 
    dicistronic_mRNA: true, 
    dicistronic_primary_transcript: true, 
    dicistronic_transcript: true, 
    edited_mRNA: true, 
    edited_transcript: true, 
    edited_transcript_by_A_to_I_substitution: true, 
    enhancerRNA: true, 
    enzymatic_RNA: true, 
    exemplar_mRNA: true, 
    glutamic_acid_tRNA_primary_transcript: true, 
    glutamine_tRNA_primary_transcript: true, 
    glutaminyl_tRNA: true, 
    glutamyl_tRNA: true, 
    glycine_tRNA_primary_transcript: true, 
    glycyl_tRNA: true, 
    guide_RNA: true, 
    histidine_tRNA_primary_transcript: true, 
    histidyl_tRNA: true, 
    intronic_lncRNA: true, 
    isoleucine_tRNA_primary_transcript: true, 
    isoleucyl_tRNA: true, 
    large_subunit_rRNA: true, 
    leucine_tRNA_primary_transcript: true, 
    leucyl_tRNA: true, 
    lincRNA: true, 
    lnc_RNA: true, 
    lysine_tRNA_primary_transcript: true, 
    lysyl_tRNA: true, 
    mRNA: true, 
    mRNA_recoded_by_codon_redefinition: true, 
    mRNA_recoded_by_translational_bypass: true, 
    mRNA_with_frameshift: true, 
    mRNA_with_minus_1_frameshift: true, 
    mRNA_with_minus_2_frameshift: true, 
    mRNA_with_plus_1_frameshift: true, 
    mRNA_with_plus_2_frameshift: true, 
    mature_transcript: true, 
    methionine_tRNA_primary_transcript: true, 
    methionyl_tRNA: true, 
    methylation_guide_snoRNA: true, 
    methylation_guide_snoRNA_primary_transcript: true, 
    miRNA: true, 
    miRNA_primary_transcript: true, 
    mini_exon_donor_RNA: true, 
    monocistronic_mRNA: true, 
    monocistronic_primary_transcript: true, 
    monocistronic_transcript: true, 
    ncRNA: true, 
    nc_primary_transcript: true, 
    phenylalanine_tRNA_primary_transcript: true, 
    phenylalanyl_tRNA: true, 
    piRNA: true, 
    polyadenylated_mRNA: true, 
    polycistronic_mRNA: true, 
    polycistronic_primary_transcript: true, 
    polycistronic_transcript: true, 
    pre_edited_mRNA: true, 
    primary_transcript: true, 
    processed_transcript: true, 
    proline_tRNA_primary_transcript: true, 
    prolyl_tRNA: true, 
    protein_coding_primary_transcript: true, 
    pseudouridylation_guide_snoRNA: true, 
    pyrrolysine_tRNA_primary_transcript: true, 
    pyrrolysyl_tRNA: true, 
    rRNA: true, 
    rRNA_16S: true, 
    rRNA_18S: true, 
    rRNA_21S: true, 
    rRNA_23S: true, 
    rRNA_25S: true, 
    rRNA_28S: true, 
    rRNA_5S: true, 
    rRNA_5_8S: true, 
    rRNA_cleavage_RNA: true, 
    rRNA_cleavage_snoRNA_primary_transcript: true, 
    rRNA_large_subunit_primary_transcript: true, 
    rRNA_primary_transcript: true, 
    rRNA_small_subunit_primary_transcript: true, 
    rasiRNA: true, 
    recoded_mRNA: true, 
    regional_centromere_outer_repeat_transcript: true, 
    ribozyme: true, 
    scRNA: true, 
    scRNA_primary_transcript: true, 
    selenocysteine_tRNA_primary_transcript: true, 
    selenocysteinyl_tRNA: true, 
    serine_tRNA_primary_transcript: true, 
    seryl_tRNA: true, 
    siRNA: true, 
    small_regulatory_ncRNA: true, 
    small_subunit_rRNA: true, 
    snRNA: true, 
    snRNA_primary_transcript: true, 
    snoRNA: true, 
    snoRNA_primary_transcript: true, 
    spot_42_RNA: true, 
    stRNA: true, 
    stRNA_primary_transcript: true, 
    tRNA: true, 
    tRNA_primary_transcript: true, 
    tasiRNA: true, 
    tasiRNA_primary_transcript: true, 
    telomerase_RNA: true, 
    telomeric_transcript: true, 
    threonine_tRNA_primary_transcript: true, 
    threonyl_tRNA: true, 
    tmRNA: true, 
    tmRNA_primary_transcript: true, 
    trans_spliced_mRNA: true, 
    trans_spliced_transcript: true, 
    transcript: true, 
    transcript_bound_by_nucleic_acid: true, 
    transcript_bound_by_protein: true, 
    transcript_with_translational_frameshift: true, 
    tryptophan_tRNA_primary_transcript: true, 
    tryptophanyl_tRNA: true, 
    tyrosine_tRNA_primary_transcript: true, 
    tyrosyl_tRNA: true, 
    valine_tRNA_primary_transcript: true, 
    valyl_tRNA: true, 
    vault_RNA: true
};

/** 
 *  flattened Sequence Ontology for UTR
 *  UTR and is-a descendants
 *  also including some other terms, relationship to UTR is noted 
 */
SequenceOntologyUtils.utrTerms = {
    UTR: true, 
    three_prime_UTR: true, 
    five_prime_UTR: true, 
    internal_UTR: true, 
    untranslated_region_polycistronic_mRNA: true, 
    UTR_region: true,   /* part_of */
    /* not including UTR_region descendants, not appropriate:
       AU_rich_element, Bruno_response_element, iron_responsive_element, upstream_AUG_codon
    */
    /* not including five_prime_open_reading_frame (part-of UTR) */
    noncoding_region_of_exon: true,  /* part_of exon */
    five_prime_coding_exon_noncoding_region: true,  /* part_of exon */
    three_prime_coding_exon_noncoding_region: true  /* part_of exon */
};

/**
 *  flattened Sequence Ontology for CDS
 *  CDS and is-a children
 *  also including some other terms, relationship to CDS is noted 
 */
SequenceOntologyUtils.cdsTerms = {
    CDS: true, 
    CDS_fragment: true, 
    CDS_independently_known: true, 
    CDS_predicted: true, 
    CDS_supported_by_sequence_similarity_data: true, 
    CDS_supported_by_EST_or_cDNA_data: true, 
    CDS_supported_by_domain_match_data: true, 
    orphan_CDS: true, 
    edited_CDS: true, 
    transposable_element_CDS: true, 
    polypeptide: true, /* part_of */
    CDS_region: true,   /* part_of */
    /* not including CDS_region descendants, not appropriate: 
       coding_end, coding_start, codon, etc.
    */
    coding_region_of_exon: true, /* part_of exon */
    five_prime_coding_exon_coding_region: true, /* part_of exon */
    three_prime_coding_exon_coding_region: true /* part_of exon */
}; 

/**
 *  flattened Sequence Ontology for exon
 *  exon and is-a children
 *  also including some other terms, relationship to exon is noted 
 */
SequenceOntologyUtils.exonTerms = {
    exon: true, 
    exon_of_single_exon_gene: true, 
    coding_exon: true, 
    five_prime_coding_exon: true, 
    three_prime_coding_exon: true, 
    interior_coding_exon: true, 
    non_coding_exon: true, 
    five_prime_noncoding_exon: true, 
    three_prime_noncoding_exon: true, 
    interior_exon: true, 
    decayed_exon: true, /* non_functional_homolog_of */
    pseudogenic_exon: true, /* non_functional_homolog_of */
    exon_region: true /* part_of */
    /*  not including descendants of exon_region that are synonymous with UTR or CDS terms
        coding_region_of_exon: true, 
        five_prime_coding_exon_coding_region: true,
        three_prime_coding_exon_coding_region: true,
        noncoding_region_of_exon: true,
        five_prime_coding_exon_noncoding_region: true,
        three_prime_coding_exon_noncoding_region: true 
    */
};

SequenceOntologyUtils.startCodonTerms = {
    start_codon: true, 
    non_canonical_start_codon: true 
};

SequenceOntologyUtils.stopCodonTerms = {
    stop_codon: true
};

/* not yet complete */
SequenceOntologyUtils.spliceTerms = {
    splice_site: true, 
    cis_splice_site: true, 
    five_prime_cis_splice_site: true, 
    recursive_splice_site: true, 
    three_prime_cis_splice_site: true, 
    canonical_five_prime_splice_site: true, 
    canonical_three_prime_splice_site: true, 
    non_canonical_five_prime_splice_site: true, 
    non_canonical_three_prime_splice_site: true, 
    non_canonical_splice_site: true 
};

/* not yet complete? */
SequenceOntologyUtils.intronTerms = {
    intron: true, 
    five_prime_intron: true, 
    three_prime_intron: true, 
    interior_intron: true, 
    UTR_intron: true, 
    twintron: true, 
    spliceosomal_intron: true, 
    autocatalytically_spliced_intron: true, 
    endonuclease_spliced_intron: true, 
    mobile_intron: true
};

return SequenceOntologyUtils;
});