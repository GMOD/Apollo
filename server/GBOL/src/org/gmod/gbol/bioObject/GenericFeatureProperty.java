package org.gmod.gbol.bioObject;

import org.gmod.gbol.bioObject.conf.BioObjectConfiguration;
import org.gmod.gbol.simpleObject.FeatureProperty;

public class GenericFeatureProperty extends AbstractBioFeatureProperty {

	private static final long serialVersionUID = 1L;
	private static final String TAG_VALUE_DELIMITER = "=";

	public GenericFeatureProperty(FeatureProperty featureProperty, BioObjectConfiguration conf) {
		super(featureProperty, conf);
	}
	
	public GenericFeatureProperty(AbstractBioFeature feature, String tag, String value, BioObjectConfiguration conf) {
		super(new FeatureProperty(
				conf.getDefaultCVTermForClass("GenericFeatureProperty"),
				feature.getFeature(),
				tag + TAG_VALUE_DELIMITER + value),
				conf);
	}

	public String getTag() {
		return featureProperty.getValue().split(TAG_VALUE_DELIMITER)[0];
	}
	
	public String getValue() {
		return featureProperty.getValue().split(TAG_VALUE_DELIMITER)[1];
	}
	
	public void setTag(String tag) {
		String value = getValue();
		featureProperty.setValue(createTagValue(tag, value));
	}
	
	public void setValue(String value) {
		String tag = getTag();
		featureProperty.setValue(createTagValue(tag, value));
	}
	
	public void setTagAndValue(String tag, String value) {
		setTag(tag);
		setValue(value);
	}
	
	private String createTagValue(String tag, String value) {
		return tag + TAG_VALUE_DELIMITER + value;
	}
	
}
