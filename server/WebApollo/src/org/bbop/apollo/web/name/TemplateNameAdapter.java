package org.bbop.apollo.web.name;

import javax.servlet.http.HttpSession;

import org.bbop.apollo.editor.session.AnnotationSession;
import org.gmod.gbol.simpleObject.Feature;

public class TemplateNameAdapter extends HttpSessionNameAdapter {

	private String template;
	
	public TemplateNameAdapter(HttpSession httpSession, AnnotationSession annotationSession, String template) {
		super(httpSession, annotationSession);
		this.template = template;
	}
	
	@Override
	public String generateName(Feature feature) {
		String name = template;
		if (name.contains("{fmin}")) {
			name.replace("{fmin}", feature.getFeatureLocations().iterator().next().getFmin().toString());
		}
		if (name.contains("{fmax}")) {
			name.replace("{fmax}", feature.getFeatureLocations().iterator().next().getFmax().toString());
		}
		if (name.contains("{strand}")) {
			int strand = feature.getFeatureLocations().iterator().next().getStrand();
			String strandString = "";
			switch (strand) {
			case -1:
				strandString = "minus";
				break;
			case 0:
				strandString = "strandless";
				break;
			case 1:
				strandString = "plus";
				break;
			}
			name.replace("{strand}", strandString);
		}
		if (name.contains("{source_id}")) {
			name.replace("{source_id}", feature.getFeatureLocations().iterator().next().getSourceFeature().getUniqueName());
		}
		return name;
	}
	
}
