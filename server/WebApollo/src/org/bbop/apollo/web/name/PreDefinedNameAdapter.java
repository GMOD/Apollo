package org.bbop.apollo.web.name;


public class PreDefinedNameAdapter extends AbstractNameAdapter {

	private String uniqueName;
	
	public PreDefinedNameAdapter(String uniqueName) {
		this.uniqueName = uniqueName;
	}
	
	@Override
	public String generateUniqueName() {
		return uniqueName;
	}

}
