package org.bbop.apollo

/**
 * Created by nathandunn on 11/17/16.
 */
enum PhoneHomeEnum {

    SERVER,
    ENVIRONMENT,
    MESSAGE,
    START,
    STOP,
    RUNNING,
    NUM_USERS,
    NUM_ANNOTATIONS,
    NUM_ORGANISMS,

    private String value;

    PhoneHomeEnum(String value){this.value = value }
    PhoneHomeEnum(){this.value = name().toLowerCase() }

    String getValue() {
        return value
    }

    @Override
    String toString() {
        return value
    }
}