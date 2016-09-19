package org.bbop.apollo.gwt.client;

import com.google.gwt.user.client.ui.Widget;
import org.bbop.apollo.gwt.client.dto.assemblage.AssemblageInfo;
import org.bbop.apollo.gwt.client.dto.assemblage.AssemblageSequence;
import org.bbop.apollo.gwt.client.dto.assemblage.AssemblageSequenceList;
import org.bbop.apollo.gwt.client.dto.assemblage.SequenceFeatureInfo;
import org.gwtbootstrap3.client.ui.Badge;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.ButtonGroup;
import org.gwtbootstrap3.client.ui.Icon;
import org.gwtbootstrap3.client.ui.constants.ButtonType;
import org.gwtbootstrap3.client.ui.constants.IconType;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by nathandunn on 9/19/16.
 */
public class AssemblageInfoService {

    public static Widget buildDescriptionWidget(AssemblageInfo assemblageInfo) {

        Map<String,Integer> scaffoldFeatureMap = new HashMap<>();
        Map<String,Boolean> scaffoldComplementMap = new HashMap<>();
        AssemblageSequenceList assemblageSequenceList = assemblageInfo.getSequenceList();

        for(int i = 0 ; i < assemblageSequenceList.size() ; i++){
            AssemblageSequence assemblageSequence = assemblageSequenceList.getSequence(i);
            String scaffoldName = assemblageSequence.getName();
            Integer featureCount = scaffoldFeatureMap.get(scaffoldName);

            SequenceFeatureInfo sequenceFeatureInfo = assemblageSequence.getFeature();

            featureCount = featureCount==null ? 0 : featureCount ;
            featureCount = sequenceFeatureInfo!=null ? featureCount + 1 : featureCount ;
            scaffoldFeatureMap.put(scaffoldName,featureCount);
            scaffoldComplementMap.put(scaffoldName, assemblageSequence.getReverse());
        }

        Iterator<String> scaffoldIterator = scaffoldFeatureMap.keySet().iterator();
        ButtonGroup buttonGroup = new ButtonGroup() ;
        while (scaffoldIterator.hasNext()){
            String scaffoldName = scaffoldIterator.next();
            Button button = new Button();
            Integer featureCount = scaffoldFeatureMap.get(scaffoldName) ;

            if(featureCount>0){
                button.add(new Badge(featureCount+""));
            }
            // determine color
            if(featureCount>0){
                button.setType(ButtonType.INFO);
            }
            else
            if(scaffoldFeatureMap.size()>1){
                button.setType(ButtonType.WARNING);
            }
            else{
                button.setType(ButtonType.DEFAULT);
            }
            if(scaffoldComplementMap.get(scaffoldName)==null || !scaffoldComplementMap.get(scaffoldName) ){
                Icon icon = new Icon(IconType.ARROW_RIGHT);
                icon.addStyleName("pull-right");
                button.add(icon);
            }
            else{
                Icon icon = new Icon(IconType.ARROW_LEFT);
                icon.addStyleName("pull-left");
                button.add(icon);
            }
            button.setText(scaffoldName + (featureCount > 0 ? " " : ""));
            buttonGroup.add(button);
        }

        return buttonGroup;
    }
}
