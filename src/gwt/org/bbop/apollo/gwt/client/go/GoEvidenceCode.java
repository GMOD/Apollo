package org.bbop.apollo.gwt.client.go;

/**
 * Matching order and descriptions here:  http://geneontology.org/docs/guide-go-evidence-codes/
 */
public enum GoEvidenceCode {

    EXP("experimental evidence used in manual assertion","ECO:0000269"),
    IDA("direct assay evidence used in manual assertion","ECO:0000314"),
    IPI("physical interaction evidence used in manual assertion","ECO:0000353"),
    IMP("mutant phenotype evidence used in manual assertion","ECO:0000315"),
    IGI("genetic interaction evidence used in manual assertion","ECO:0000316"),
    IEP("inferred from expression pattern","ECO:0000270"),


    HTP("inferred from high throughput experiment","ECO:0000270"),
    HDA("inferred from high throughput direct assay","ECO:0007005"),
    HMP("inferred from high throughput mutant phenotype","ECO:0007001"),
    HGI("inferred from high throughput genetic interaction","ECO:0007003"),
    HEP("inferred from high throughput expression pattern","ECO:0007007"),


    IBA("inferred from biological aspect of ancestor","ECO:0000318"),
    IBD("inferred from biological aspect of descendants","ECO:0000319"),
    IKR("inferred from key residues","ECO:0000320"),
    IRD("inferred from rapid divergence","ECO:0000321"),


    ISS("sequence similarity evidence used in manual assertion","ECO:0000250"),
    ISO("sequence orthology evidence used in manual assertion","ECO:0000266"),
    ISA("sequence alignment evidence used in manual assertion","ECO:0000247"),
    ISM("inferred from sequence model","ECO:0000255"),
    IGC("inferred from genome context","ECO:0000317"),
    RCA ("inferred from reviewed computational analysis","ECO:0000245"),


    TAS("traceable author statement","ECO:0000304"),
    NAS("non-traceable author statement","ECO:0000303"),


    IC("inferred by curator","ECO:0000305"),
    ND("no biological data available","ECO:0000307"),

    IEA("evidence used in automatic assertion","ECO:0000501"),
    ;


    GoEvidenceCode(String description, String curie) {
        this.description = description;
        this.curie = curie;
    }


    private String description;
    private String curie;

    public static boolean requiresWith(String evidenceCode) {
        if(evidenceCode.equals(IPI.curie)) return true;
        if(evidenceCode.equals(ISS.curie)) return true;
        if(evidenceCode.equals(ISO.curie)) return true;
        if(evidenceCode.equals(IGI.curie)) return true;
        if(evidenceCode.equals(ISA.curie)) return true;
        if(evidenceCode.equals(IBA.curie)) return true;
        if(evidenceCode.equals(IC.curie)) return true;
        return false;
    }

    public String getDescription() {
        return description;
    }

    public String getCurie() {
        return curie;
    }
}
