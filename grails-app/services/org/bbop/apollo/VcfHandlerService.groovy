package org.bbop.apollo

import grails.transaction.Transactional
import java.text.SimpleDateFormat

@Transactional
class VcfHandlerService {

    def sequenceService
    def featureService
    def featurePropertyService
    def variantService

    static final format = "VCFv4.2"
    static final header = ["CHROM", "POS", "ID", "REF", "ALT", "QUAL", "FILTER", "INFO"]
    SimpleDateFormat dateFormat = new SimpleDateFormat("YYYYMMdd")
    String source
    Organism organism

    /**
     * Export all variants to a file in VCF format
     * @param organism
     * @param sequences
     * @param variants
     * @param path
     * @param source
     */
    public void writeVariantsToText(Organism organism, def variants, String path, String source) {
        println("[VcfHandlerService][writeVariantsToText] path: ${path} variants: ${variants.size()} source: ${source}")
        this.source = source
        this.organism = organism
        File file = new File(path)
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(file, true)))
        writeVcfHeaders(out, variants)
        writeVariants(out, variants)
        out.flush()
        out.close()
    }

    /**
     * Write headers for VCF
     * @param writer
     * @param variants
     */
    public void writeVcfHeaders(PrintWriter writer, def variants) {
        String reference = organism.genomeFasta ? organism.directory + File.separator + organism.genomeFasta : organism.directory + File.separator + "seq/"
        writer.write("##fileformat=${format}\n")
        writer.write("##fileDate=${dateFormat.format(new Date())}\n")
        writer.write("##source=${this.source}\n")
        writer.write("##reference=${reference}\n")
        def sequences = []
        for (SequenceAlteration variant : variants) {
            if (!sequences.contains(variant.featureLocation.sequence)) {
                sequences.add(variant.featureLocation.sequence)
            }
        }
        sequences.sort({ a,b -> a.name <=> b.name })

        for (Sequence sequence : sequences) {
            writer.write("##contig=<ID=${sequence.name},length=${sequence.length}>\n")
        }
        writer.write("#${header.join('\t')}\n")
    }

    /**
     * Write VCF records for a list of variants
     * @param writer
     * @param variants
     */
    public void writeVariants(PrintWriter writer, def variants) {
        def variantsBySequence = [:]
        for (SequenceAlteration variant : variants) {
            Sequence sequence = variant.featureLocation.sequence
            if (variantsBySequence.containsKey(sequence)) {
                variantsBySequence.get(sequence).add(variant)
            }
            else {
                variantsBySequence.put(sequence, [variant])
            }
        }
        variantsBySequence.sort { it.key }
        for (def entry : variantsBySequence.entrySet()) {
            for(SequenceAlteration variant : entry.getValue()) {
                writeVariants(writer, variant)
            }
        }
    }

    /**
     * Write VCF record for a variant
     * @param writer
     * @param variant
     */
    public void writeVariants(PrintWriter writer, SequenceAlteration variant) {
        Allele referenceAllele = variantService.getReferenceAllele(variant)
        def alternateAlleles = variantService.getAlternateAlleles(variant)
        def record = [variant.featureLocation.sequence.name, variant.featureLocation.fmin + 1, variant.uniqueName, referenceAllele ? referenceAllele.bases : "."]
        def alleleInfoMap = [:]
        alternateAlleles.each { allele ->
            if (alleleInfoMap.containsKey('allele_order')) {
                alleleInfoMap.get('allele_order').add(allele.bases)
            }
            else {
                alleleInfoMap['allele_order'] = [allele.bases]
            }

            def alleleInfos = allele.alleleInfo
            alleleInfos.each {
                if (alleleInfoMap.containsKey(it.tag)) {
                    alleleInfoMap.get(it.tag).add(it.value)
                }
                else {
                    alleleInfoMap[it.tag] = [it.value]
                }
            }
        }

        if (alleleInfoMap.containsKey("allele_order")) {
            record.add(alleleInfoMap.get("allele_order").join(','))
        }
        else {
            record.add(".")
        }

        record.add(".")
        record.add(".")

        alleleInfoMap.remove("allele_order")

        def attributes = []

        // Variant Info
        def variantInfoList = []
        for (VariantInfo info : variant.variantInfo) {
            String variantInfoString = "${info.tag}=${info.value}"
            variantInfoList.add(variantInfoString)
        }
        attributes.addAll(variantInfoList)

        // Allele Info
        def alleleInfoList = []
        for (def entry : alleleInfoMap.entrySet()) {
            alleleInfoList.add("${entry.key}=${entry.value.join(',')}")
        }
        attributes.addAll(alleleInfoList)

        // Dbxref - publications, phenotype ontology
        def dbxrefs = []
        for (def dbxref : variant.featureDBXrefs) {
            String dbxrefString = encodeString("${dbxref.db.name}:${dbxref.accession}")
            dbxrefs.add(dbxrefString)
        }
        if (dbxrefs.size() > 0) {
            attributes.add("dbxref=${dbxrefs.join(',')}")
        }

        // comments
        def comments = []
        for (def comment : featurePropertyService.getComments(variant)) {
            comments.add(comment.value)
        }

        if (comments.size() > 0) {
            attributes.add("comments=${comments.join(',')}")
        }

        if (attributes.size() > 0) {
            record.add(attributes.join(';'))
        }
        else {
            record.add(".")
        }

        writer.write("${record.join('\t')}\n")
    }

    /**
     * Escape characters
     * @param str
     * @return
     */
    static private String encodeString(String str) {
        return str ? str.replaceAll(",", "%2C").replaceAll("=", "%3D").replaceAll(";", "%3B").replaceAll("\t", "%09") : ""
    }
}
