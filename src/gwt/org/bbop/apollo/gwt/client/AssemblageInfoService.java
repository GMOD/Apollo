package org.bbop.apollo.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
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
import org.gwtbootstrap3.client.ui.constants.Emphasis;
import org.gwtbootstrap3.client.ui.constants.IconType;
import org.gwtbootstrap3.client.ui.constants.Pull;

import java.util.*;

/**
 * Created by nathandunn on 9/19/16.
 */
public class AssemblageInfoService {

    public static ButtonGroup buildLocationWidget(final AssemblageInfo assemblageInfo) {

        Map<String,Boolean> scaffoldComplementMap = new HashMap<>();
        AssemblageSequenceList assemblageSequenceList = assemblageInfo.getSequenceList();
        for(int i = 0 ; assemblageSequenceList!=null && i < assemblageSequenceList.size() ; i++){
            AssemblageSequence assemblageSequence = assemblageSequenceList.getSequence(i);
            String scaffoldName = assemblageSequence.getName();
//
            scaffoldComplementMap.put(scaffoldName, assemblageSequence.getReverse());
        }
        if(scaffoldComplementMap.size()==0) {
            return null ;
        }

        Iterator<Map.Entry<String,Boolean>> scaffoldIterator = scaffoldComplementMap.entrySet().iterator();
        ButtonGroup buttonGroup = new ButtonGroup() ;
        while (scaffoldIterator.hasNext()){
            final Map.Entry<String,Boolean> entry = scaffoldIterator.next();
//            Window.alert(entry.getKey());
            Button button = new Button();
            button.setText(entry.getKey());
            button.setType(ButtonType.DEFAULT);

            Icon rightIcon = new Icon(IconType.ARROW_RIGHT);
            rightIcon.setPull(Pull.RIGHT);
            button.add(rightIcon);
            rightIcon.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    if(entry.getValue()){
                        Window.alert("forwarding");
                        assemblageInfo.getSequenceList().getSequence(0).setReverse(false);
                        MainPanel.updateGenomicViewerForAssemblage(assemblageInfo);
                    }
                }
            });


            Icon leftIcon = new Icon(IconType.ARROW_LEFT);
            leftIcon.setPull(Pull.LEFT);
            button.add(leftIcon);
            leftIcon.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    if(!entry.getValue()){
                        Window.alert("reversing");
                        assemblageInfo.getSequenceList().getSequence(0).setReverse(true);
                        MainPanel.updateGenomicViewerForAssemblage(assemblageInfo);
                    }
                }
            });

            if(entry.getValue()){
                rightIcon.setColor("#d3d3d3");
                leftIcon.setColor("blue");
            }
            else{
                rightIcon.setColor("blue");
                leftIcon.setColor("#d3d3d3");
            }

//            if(scaffoldComplementMap.get(scaffoldName)==null || !scaffoldComplementMap.get(scaffoldName) ){
//            }
//            else{
//            }
            buttonGroup.add(button);
        }

        return buttonGroup;
    }

    public static Widget buildDescriptionWidget(AssemblageInfo assemblageInfo, Set<String> usedSequences) {

        Map<String,Integer> scaffoldFeatureMap = new HashMap<>();
        Map<String,Boolean> scaffoldComplementMap = new HashMap<>();
        AssemblageSequenceList assemblageSequenceList = assemblageInfo.getSequenceList();

        for(int i = 0 ; assemblageSequenceList!=null && i < assemblageSequenceList.size() ; i++){
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

            // if used add a checkmark or something
            if(usedSequences.contains(scaffoldName)){
                Icon usedIcon = new Icon(IconType.CHECK_CIRCLE);
                usedIcon.setPull(Pull.LEFT);
                usedIcon.setColor("gray");
                button.add(usedIcon);
            }
            else{
                Icon unUsedIcon = new Icon(IconType.CIRCLE_O);
                unUsedIcon.setPull(Pull.LEFT);
//                unUsedIcon.setColor("green");
                button.add(unUsedIcon);
            }

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
                icon.setPull(Pull.RIGHT);
                button.add(icon);
            }
            else{
                Icon icon = new Icon(IconType.ARROW_LEFT);
                icon.setPull(Pull.LEFT);
                button.add(icon);
            }
            button.setText(scaffoldName + (featureCount > 0 ? " " : ""));
            buttonGroup.add(button);
        }

        return buttonGroup;
    }
}
