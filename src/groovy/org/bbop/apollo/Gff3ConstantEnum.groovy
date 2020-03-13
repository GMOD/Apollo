package org.bbop.apollo

enum Gff3ConstantEnum {
    RANK,
    TERM,
    DB_XREF,
    EVIDENCE,
    ALTERNATE,
    NOTE,
    BASED_ON,
    LAST_UPDATED,
    DATE_CREATED,

    private String value

//        Gff3ConstantEnum(String value) { this.value = value }
    Gff3ConstantEnum() { this.value = name().toLowerCase() }

    String getValue() {
        return value
    }
}