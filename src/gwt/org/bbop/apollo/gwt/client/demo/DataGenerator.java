package org.bbop.apollo.gwt.client.demo;

import com.google.gwt.user.client.ui.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ndunn on 12/17/14.
 */
public class DataGenerator {

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


    public static List<String> getSequences() {
        List<String> sequences = new ArrayList<>();

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

}
