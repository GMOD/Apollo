package org.bbop.apollo.gwt.client.demo;

import com.google.gwt.user.client.ui.*;

//import com.google.gwt.user.client.ui.FlexTable;
//import com.google.gwt.user.client.ui.HTML;
//import com.google.gwt.user.client.ui.HorizontalPanel;
//import com.google.gwt.user.client.ui.TreeItem;
//import org.gwtbootstrap3.client.ui.Anchor;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.*;
import org.gwtbootstrap3.client.ui.Button;
//import org.gwtbootstrap3.client.ui.ListBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ndunn on 12/17/14.
 */
public class DataGenerator {

    final static String SEQUENCE_PREFIX = "LG";

    public static String[] organisms = {
            "Zebrafish",
            "Alligator Pipefish",
            "Bloody Stickleback",
            "Brook Stickleback",
            "Three-spined Stickleback",
            "Amur Stickleback",
            "Spinach Stickleback"
    };

    public static List<String> getOrganisms() {
        List<String> organisms = new ArrayList<>();

        return organisms;
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

        return users;
    }

    public static List<String> getGroups() {
        List<String> groups = new ArrayList<>();

        return groups;
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
}
