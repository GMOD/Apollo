package org.bbop.apollo

class GenericFeatureProperty extends FeatureProperty implements Ontological{

    private static final String TAG_VALUE_DELIMITER = "=";

    static constraints = {
    }

    String cvTerm  = "GenericFeatureProperty"

    public String getTag() {
        return getValue().split(TAG_VALUE_DELIMITER)[0];
    }

    public String getValue() {
        return getValue().split(TAG_VALUE_DELIMITER)[1];
    }

    public void setTag(String tag) {
        String value = getValue();
        setValue(createTagValue(tag, value));
    }

    public void setValue(String value) {
        String tag = getTag();
        setValue(createTagValue(tag, value));
    }

    public void setTagAndValue(String tag, String value) {
        setTag(tag);
        setValue(value);
    }

    private String createTagValue(String tag, String value) {
        return tag + TAG_VALUE_DELIMITER + value;
    }

}
