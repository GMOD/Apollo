package org.bbop.apollo.gwt.shared.go;

/**
 * http://geneontology.org/docs/guide-go-evidence-codes/
 */
public enum EvidenceCode {

    // expeerimental evidence

    EXP("Inferred from Experiment", "http://www.evidenceontology.org/term/ECO:0000269/"),
    IDA(10),
    IPI,
    IMP,
    IGI,
    IEP,

    // high throughput
    HTP,
    HDA(5),
    HMP,
    HGI,
    HEP,

    // phylogenetic
    IBA(10),
    IBD,
    IKR,
    IRD,

    // computational
    ISS(10),
    ISO(10),
    ISA,
    ISM,
    IGC,
    RCA,

    // author
    TAS,
    NAS(5),

    // curator
    IC (10),
    ND (10),

    // electronic
    IEA (10),
    ;


    private EvidenceCode() {
    }

    private EvidenceCode(Integer rank) {
        this.rank = rank ;
    }

    private EvidenceCode(String name, String link) {
        this.name = name;
        this.link = link;
    }

    private EvidenceCode(Integer rank,String name, String link) {
        this.rank = rank ;
        this.name = name;
        this.link = link;
    }

    private Integer rank = 0 ; // sorting rank
    private String name = name();
    private String link = null;


    public static EvidenceCode findCode(String selectedValue) {
        for(EvidenceCode evidenceCode : values()){
            if(evidenceCode.name.equals(selectedValue)){
                return evidenceCode ;
            }
        }
        return null ;
    }
}
