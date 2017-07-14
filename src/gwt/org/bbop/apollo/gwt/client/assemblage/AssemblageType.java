package org.bbop.apollo.gwt.client.assemblage;

/**
 * Created by nathandunn on 1/9/17.
 */
public enum AssemblageType {

    SCAFFOLD("Scaffold"),
    COMBINED("Combined"),
    FEATURE("Feature"),;

    private String display ;

    AssemblageType(String display){
        this.display = display;
    }

    public String getDisplay() {
        return display;
    }

    public static AssemblageType getTypeForString(String displayType){
        for(AssemblageType assemblageType : values()){
            if(displayType.equals(assemblageType.display)){
                return assemblageType;
            }
        }
        return null ;
    }
}
