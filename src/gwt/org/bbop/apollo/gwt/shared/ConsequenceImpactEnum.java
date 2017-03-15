package org.bbop.apollo.gwt.shared;

/**
 * Created by deepak.unni3 on 9/1/16.
 */
public enum ConsequenceImpactEnum {
    LOW,
    MODERATE,
    HIGH,
    MODIFIER;

    private String value;

    ConsequenceImpactEnum(String value) { this.value = value; }

    ConsequenceImpactEnum() { this.value = name().toLowerCase(); }

    @Override
    public String toString() { return value; }

    public String getValue() { return value; }
}
