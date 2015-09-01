package org.bbop.apollo.gwt.client.demo;

import com.google.gwt.user.client.ui.*;

//import com.google.gwt.user.client.ui.FlexTable;
//import com.google.gwt.user.client.ui.HTML;
//import com.google.gwt.user.client.ui.HorizontalPanel;
//import com.google.gwt.user.client.ui.TreeItem;
//import org.gwtbootstrap3.client.ui.Anchor;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.ListBox;
import org.bbop.apollo.gwt.client.dto.TrackInfo;
import org.gwtbootstrap3.client.ui.*;
import org.gwtbootstrap3.client.ui.Button;
//import org.gwtbootstrap3.client.ui.ListBox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Nathan Dunn on 12/17/14.
 */
public class DataGenerator {

    public final static String SEQUENCE_PREFIX = "LG";

    public static String[] organisms = {
            "Zebrafish"
            ,"Alligator Pipefish"
            ,"Bloody Stickleback"
            ,"Brook Stickleback"
            ,"Three-spined Stickleback"
            ,"Amur Stickleback"
            ,"Spinach Stickleback"
            ,"Bean weevil"
            ,"Flour mite"
            ,"May conehead"
            ,"Wheat curl mite"
            ,"Sea spiders"
            ,"Lesser wax moth"
            ,"Acacia psyllid"
            ,"Panamanian leafcutter ant"
            ,"Bluegreen aphid"
            ,"Pea aphid"
            ,"Pale spruce gall adelgid"
            ,"Balsam woolly adelgid"
            ,"Hemlock woolly adelgid"
            ,"Yellow fever mosquito"
            ,"Asian tiger mosquito"
            ,"Eastern salt marsh mosquito"
            ,"Floodwater mosquito"
            ,"Small tortoiseshell"
            ,"Emerald ash borer"
            ,"Catalan furry blue"
            ,"Black cutworm"
            ,"Turnip moth"
            ,"Citrus spiny whitefly"
            ,"Brown legged grain mite"
            ,"Striped ground cricket"
            ,"Southern ground cricket"
            ,"Lesser mealworm"
            ,"Lone star tick"
            ,"Cayenne tick"
            ,"Bont tick"
            ,"The Gulf-Coast Tick"
            ,"Red-banded sand wasp"
            ,"Large raspberry aphid"
            ,"Emerald cockroach wasp"
            ,"Mediterranean flour moth"
            ,"Squash bug"
            ,"South American fruit fly"
            ,"Mexican fruit fly"
            ,"West Indian fruit fly"
            ,"Guava fruitfly"
            ,"Caribbean fruitfly"
            ,"Sri Lankan relict ant"
            ,"Seashore earwig"
            ,"Maritime earwig"
            ,"Two striped walking stick"
            ,"African malaria mosquito"
            ,"African malaria mosquito"
            ,"Common malaria mosquito"
            ,"Asian malaria mosquito"
            ,"Asian longhorned beetle"
            ,"Indian muga silkmoth"
            ,"Tussore silk moth"
            ,"Chinese oak silkmoth"
            ,"Japanese oak silkmoth"
            ,"Boll weevil"
            ,"Webspinner"
            ,"Soybean aphid"
            ,"Cotton aphid"
            ,"Small raspberry aphid"
            ,"Giant honeybee"
            ,"Red dwarf honey bee"
            ,"Honey bee"
            ,"East Asian elm sawfly"
            ,"European garden spider"
            ,"African giant black millipede"
            ,"Orb-weaving spider"
            ,"Brine shrimp"
            ,"Waterlouse"
            ,"Turnip sawfly"
            ,"Sydney funnel spider"
            ,"Leafcutter ant"
            ,"Leafcutter ant"
            ,"Texas leafcutter ant"
            ,"Silver Y moth"
    };

    public static List<String> getOrganisms() {
        List<String> arrayList = new ArrayList<>();
        for(String o : organisms){
            arrayList.add(o);
        }
        return arrayList;
    }


    public static List<String> getSequences(int numberToGet) {
        List<String> sequences = new ArrayList<>();
        for(int i = 1 ; i < numberToGet+1 ;i++){
            sequences.add(SEQUENCE_PREFIX+i);
        }

        return sequences;
    }

    public static List<String> getAnnotations() {
        List<String> annotations = new ArrayList<>();

        return annotations;
    }

    public static List<String> getUsers() {
        List<String> users = new ArrayList<>();
        users.add("Yvone Patague");
        users.add("Darcel Ostendorf");
        users.add("Yolande Boisvert");
        users.add("Fawn Pettyjohn");
        users.add("Shantel Bufford");
        users.add("Edwina Haag");
        users.add("Glady Larimer");
        users.add("Marylynn Marez");
        users.add("Cherly Hanshaw");
        users.add("Margeret Vasta");
        users.add("Vickey Wolfrum");
        users.add("Cindy Kirshner");
        users.add("Duncan Hallberg");
        users.add("Art Villagomez");
        users.add("Ryann Noakes");
        users.add("Branda Clower");
        users.add("Nora Siemers");
        users.add("Marita Dagostino");
        users.add("Anastacia Hevey");
        users.add("Luann Celaya");
        users.add("Yvone Patague");
        users.add("Darcel Ostendorf");
        users.add("Yolande Boisvert");
        users.add("Fawn Pettyjohn");
        users.add("Shantel Bufford");
        users.add("Edwina Haag");
        users.add("Glady Larimer");
        users.add("Marylynn Marez");
        users.add("Cherly Hanshaw");
        users.add("Margeret Vasta");
        users.add("Vickey Wolfrum");
        users.add("Cindy Kirshner");
        users.add("Duncan Hallberg");
        users.add("Art Villagomez");
        users.add("Ryann Noakes");
        users.add("Branda Clower");
        users.add("Nora Siemers");
        users.add("Marita Dagostino");
        users.add("Anastacia Hevey");
        users.add("Luann Celaya");


        return users;
    }



    public static void populateOrganismList(ListBox organismList) {
        organismList.clear();
        for(String organism : organisms){
            organismList.addItem(organism);
        }
    }

    public static TreeItem generateTreeItem(String geneName) {
        HTML html ;
        boolean isGene = Math.random()>0.5;
        if(isGene){
            html = new HTML(geneName  +" <div class='label label-success'>Gene</div>");
        }
        else{
            html = new HTML(geneName  +" <div class='label label-warning'>Pseudogene</div>");
        }

        TreeItem sox9b = new TreeItem(html);
        int i =0  ;
        sox9b.addItem(createTranscript(geneName, isGene,i++));
        if(Math.random()>0.5){
            sox9b.addItem(createTranscript(geneName, isGene,i++));
        }
        sox9b.setState(true);
        return sox9b;
    }

    public static TreeItem createTranscript(String geneName, boolean isGene,int index){

        Integer randomLength = (int) Math.round(Math.random()*5000.0);
        TreeItem treeItem = new TreeItem();
        HTML transcriptHTML ;
        if(isGene){
            transcriptHTML = new HTML(geneName + "-00"+index+ " <div class='label label-success' style='display:inline;'>mRNA</div><div class='badge pull-right' style='display:inline;'>"+randomLength+"</div>");
        }
        else{
            transcriptHTML  = new HTML(geneName + "-00"+index+ " <div class='label label-warning' style='display:inline;'>Transcript</div><div class='badge pull-right' style='display:inline;'>"+randomLength+"</div>");
        }
        if(Math.random()>0.7){
            transcriptHTML.setHTML(transcriptHTML.getHTML()+"<div class='label label-danger' style='display:inline;'>Stop Codon</div>");
        }

        treeItem.setWidget(transcriptHTML);
        int j = 0 ;
        treeItem.addItem(createExon(geneName + "-00"+index,j++));
        if(Math.random()>0.2){
            treeItem.addItem(createExon(geneName + "-00"+index,j++));
        }
        if(Math.random()>0.2){
            treeItem.addItem(createExon(geneName + "-00"+index,j++));
        }
        treeItem.setState(true);


        return treeItem ;
    }

    private static HTML createExon(String geneName,int index) {
        Integer randomStart = (int) Math.round(Math.random()*5000.0);
        Integer randomLength = (int) Math.round(Math.random()*500.0);
        Integer randomFinish = randomStart + randomLength;
        return new HTML(geneName + "-00-"+index+ " <div class='label label-info' style='display:inline;'>Exon</div><div class='badge pull-right' style='display:inline;'>"+randomStart + "-"+randomFinish+"</div>");
    }

    public static void generateSequenceRow(FlexTable sequenceTable, int i) {
            Anchor link = new Anchor(SEQUENCE_PREFIX+i);
            sequenceTable.setWidget(i, 0, link);
            sequenceTable.setHTML(i, 1, Math.rint(Math.random() * 100) + "");
//        configurationTable.setHTML(i, 2, Math.rint(Math.random() * 100) + "");
            Button button = new Button("Annotate");
//        Button button2 = new Button("Details");
            HorizontalPanel actionPanel = new HorizontalPanel();
            actionPanel.add(button);
//        actionPanel.add(button2);
            sequenceTable.setWidget(i, 2, actionPanel);

    }

    public static void populateOrganismTable(FlexTable organismTable) {
        int i = 0 ;
        for(String organism : organisms){

            Anchor link = new Anchor(organism);
            organismTable.setWidget(i, 0, link);
            organismTable.setHTML(i, 1, Math.rint(Math.random() * 100) + "");
            Button button = new Button("Set");
//        Button button2 = new Button("Details");
            HorizontalPanel actionPanel = new HorizontalPanel();
            actionPanel.add(button);
//        actionPanel.add(button2);
            organismTable.setWidget(i, 2, actionPanel);

//            organismTable.createOrganismRow(organism,i++);
            ++i ;
        }
    }

    public static void populateSequenceList(ListBox sequenceList) {

        List<String> sequences = getSequences(40);
        for(String seq : sequences){
            sequenceList.addItem(seq);
        }

    }

    public static List<String> getGroups() {
        List<String> groups = new ArrayList<>();
        groups.add("USDA i5K");
        groups.add("Augmented Integration Tactical");
        groups.add("Australian Training Wholesale");
        groups.add("Automotive Sports");
        groups.add("Bailey Financial Future Equipment");
        groups.add("British Speciality Research");
        groups.add("Casey Holding Containers");
        groups.add("Chemical Apex Realizations");
        groups.add("Diaz Fabrication Development");
        groups.add("Digital Logistics");
        groups.add("Dynamics Of Moscow");
        groups.add("Fuentes Healthy Pharmecutical Integration");
        groups.add("Fuller Technology Networks");
        groups.add("Genetic Physiotronics");
        groups.add("Hawkins Equity Solutions");
        groups.add("Hill Unlimited Medical");
        groups.add("Hurley Soft");
        groups.add("Marquez Photologistics");
        groups.add("Marsh Motors");
        groups.add("Mcneil Chemical Instruments");
        groups.add("Norris Semiconductor Of Kiev");
        groups.add("Progressive Manufacturing Leasing");
        groups.add("Russian Technology Horizons");
        groups.add("Spanish Housing");
        groups.add("Strategic Training Corporation");
        groups.add("Waters Financial Metals");
        groups.add("Archon Rose Five");
        groups.add("Baron Wraith Guild");
        groups.add("Coffin Mind Legion");
        groups.add("Crimson Werewolf Guild");
        groups.add("Dog Altar Seven");
        groups.add("Drake Soul Hundred");
        groups.add("Enchanted Fortress School");
        groups.add("Gold Demon Guild");
        groups.add("Master Phoenix Guild");
        groups.add("Mystic Cheetah Alliance");
        groups.add("Mystic Chime Guild");
        groups.add("Phantom Hawk Guild");
        groups.add("Randy Hippocampus Band");
        groups.add("Recognized Totem Family");
        groups.add("The Basilisk Confederation");
        groups.add("The Phylactery College");
        groups.add("The Serpent Thousand");
        groups.add("Totem Soul Posse");
        groups.add("Warrior Coffin Guild");
        groups.add("Wraith Ambassador Conspiracy");
        groups.add("Collective of Worlds");
        groups.add("Constellation's Republic");
        groups.add("Federated Directorate of Constellations");
        groups.add("Federated Directorate of Galaxies");
        groups.add("Federation of Planets");
        groups.add("Galaxies' Oligarcy");
        groups.add("Galaxy's Confederacy");
        groups.add("Grand Collective of Constellations");
        groups.add("Heavenly Federation of Worlds");
        groups.add("Mercantile Coalition of Constellations");
        groups.add("Nation of Spheres");
        groups.add("Perfected Plutocracy");
        groups.add("Perfected Technocracy");
        groups.add("Planets' Electorate");
        groups.add("Solidarity of Stars");
        groups.add("System's Union");
        groups.add("Theocratic Constellations");
        groups.add("Tyranical Government of Constellations");
        groups.add("Unified Empire");
        groups.add("Worlds' Solidarity");


        return groups;
    }

    public static void populateTrackList(List<TrackInfo> trackInfoList) {

        trackInfoList.add(new TrackInfo("Official Gene Set v3.2","HTMLFeature",true));
        trackInfoList.add(new TrackInfo("GeneID","HTMLFeature",true));
        trackInfoList.add(new TrackInfo("Fgenesh","HTMLFeature",false));
        trackInfoList.add(new TrackInfo("Cflo_OGSv3.3","HTMLFeature",true));
        trackInfoList.add(new TrackInfo("NCBI ESTs","HTMLFeature",true));
        for(int i = 0 ; i < 40 ; i++){
            trackInfoList.add(new TrackInfo("Track" + i));
        }

    }

    public static String[] sequenceTypes = {
            "All"
            ,"Gene"
            ,"Pseudogene"
            ,"mRNA"
            ,"miRNA"
            ,"tRNA"
            ,"rRNA"
            ,"ncRNA"
            ,"snRNA"
            ,"snoRNA"
            ,"repeat region"
            ,"transposable element"
    };

    public static void populateTypeList(ListBox typeList) {

            for(String seq : sequenceTypes){
                typeList.addItem(seq);
            }
    }
}
