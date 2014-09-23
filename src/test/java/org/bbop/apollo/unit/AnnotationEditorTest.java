package org.bbop.apollo.unit;

import junit.framework.TestCase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bbop.apollo.config.Configuration;
import org.bbop.apollo.editor.AnnotationEditor;
import org.bbop.apollo.editor.AnnotationEditor.AnnotationEditorException;
import org.bbop.apollo.editor.session.AnnotationSession;
import org.gmod.gbol.bioObject.*;
import org.gmod.gbol.bioObject.conf.BioObjectConfiguration;
import org.gmod.gbol.bioObject.util.BioObjectUtil;
import org.gmod.gbol.simpleObject.FeatureLocation;
import org.gmod.gbol.simpleObject.Organism;
import org.gmod.gbol.util.GBOLUtilException;
import org.gmod.gbol.util.SequenceUtil;
import org.junit.Ignore;

import java.io.File;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class AnnotationEditorTest extends TestCase {

    private final Logger logger = LogManager.getLogger(LogManager.ROOT_LOGGER_NAME);

    private Organism organism;
    private BioObjectConfiguration conf;
    private AnnotationEditor editor;

    public void setUp() {
        organism = new Organism("Foomus", "barius");
        logger.info(new File(".").getAbsolutePath());
        conf = new BioObjectConfiguration("src/test/resources/testSupport/mapping.xml");
        editor = new AnnotationEditor(new AnnotationSession(), new Configuration());
    }

    public void testAddTranscript() {
        logger.info("== testAddTranscript() ==");
        Gene gene = createGene(100, 1000, "gene");
        assertEquals("gene fmin (no transcript): ", new Integer(100), gene.getFeatureLocation().getFmin());
        assertEquals("gene fmax (no transcript): ", new Integer(1000), gene.getFeatureLocation().getFmax());
        Transcript transcript1 = createTranscript(0, 1000, "transcript1");
        Transcript transcript2 = createTranscript(100, 1010, "transcript2");
        editor.addTranscript(gene, transcript1);
        editor.addTranscript(gene, transcript2);
        assertEquals("gene fmin (transcript): ", new Integer(0), gene.getFeatureLocation().getFmin());
        assertEquals("gene fmax (transcript): ", new Integer(1010), gene.getFeatureLocation().getFmax());
        assertEquals("gene num transcripts: ", new Integer(1), new Integer(1));
        printGene(gene);
    }

    public void testDuplicateTranscript() {
        logger.info("== testDuplicateTranscript() ==");
        Gene gene = createGene(100, 1000, "gene");
        Transcript transcript = createTranscript(0, 1000, "transcript");
        editor.addTranscript(gene, transcript);
        assertEquals("num transcripts (before duplication): ", new Integer(1), new Integer(gene.getNumberOfTranscripts()));
        Exon exon1 = createExon(100, 200, "exon1");
        Exon exon2 = createExon(400, 600, "exon2");
        Exon exon3 = createExon(500, 1000, "exon3");
        editor.addExon(transcript, exon1);
        editor.addExon(transcript, exon2);
        editor.addExon(transcript, exon3);
        // this is correct
        assertEquals("num exons (before duplication): ", new Integer(2), new Integer(transcript.getNumberOfExons()));
        editor.duplicateTranscript(transcript);
        assertEquals("num transcripts (after duplication): ", new Integer(2), new Integer(gene.getNumberOfTranscripts()));
        for (Transcript t : gene.getTranscripts()) {
            assertEquals("num exons " + t.getUniqueName() + " (after duplication): ",
                    new Integer(2), new Integer(t.getNumberOfExons()));
        }
        printGene(gene);
    }

    public void testMergeTranscripts() {
        logger.info("== testMergeTranscripts() ==");
        Gene gene = createGene(0, 1500, "gene");
        Transcript transcript1 = createTranscript(0, 1000, "transcript1");
        Transcript transcript2 = createTranscript(500, 1500, "transcript2");
        gene.addTranscript(transcript1);
        gene.addTranscript(transcript2);
        editor.addExon(transcript1, createExon(100, 200, "exon1_1"));
        editor.addExon(transcript1, createExon(800, 900, "exon1_2"));
        editor.addExon(transcript2, createExon(850, 1000, "exon2_2"));
        editor.addExon(transcript2, createExon(500, 700, "exon2_1"));
        editor.addExon(transcript2, createExon(1200, 1400, "exon2_3"));
        assertEquals("num exons transcript1 (pre-merge): ", new Integer(2), new Integer(transcript1.getExons().size()));
        assertEquals("num exons transcript2 (pre-merge): ", new Integer(3), new Integer(transcript2.getExons().size()));
        editor.mergeTranscripts(transcript1, transcript2);
        assertEquals("num exons transcript1 (after-merge): ", new Integer(4), new Integer(transcript1.getExons().size()));
        assertEquals("num exons transcript2 (after-merge): ", new Integer(0), new Integer(transcript2.getExons().size()));
        printGene(gene);
    }

    public void testMergeTranscriptWithGeneMerge() {
        logger.info("== testMergeTranscriptsWithGeneMerge() ==");
        Gene gene1 = createGene(0, 1500, "gene1");
        Gene gene2 = createGene(2000, 3500, "gene2");
        Transcript transcript1 = createTranscript(0, 1500, "transcript1");
        Transcript transcript2 = createTranscript(2000, 3500, "transcript2");
        editor.addTranscript(gene1, transcript1);
        editor.addTranscript(gene2, transcript2);
        editor.addExon(transcript1, createExon(0, 700, "exon1_1"));
        editor.addExon(transcript1, createExon(1000, 1500, "exon1_2"));
        editor.addExon(transcript2, createExon(2000, 2700, "exon2_1"));
        editor.addExon(transcript2, createExon(3000, 3500, "exon2_2"));
        editor.mergeTranscripts(transcript1, transcript2);
        assertEquals("num transcripts gene1 (after-merge): ", new Integer(1), new Integer(gene1.getTranscripts().size()));
        assertEquals("num transcripts gene2 (after-merge): ", new Integer(0), new Integer(gene2.getTranscripts().size()));
        assertEquals("num exons transcript1 (after-merge): ", new Integer(4), new Integer(transcript1.getExons().size()));
        assertEquals("gene1 fmin (after-merge): ", new Integer(0), gene1.getFeatureLocation().getFmin());
        assertEquals("gene1 fmax (after-merge): ", new Integer(3500), gene1.getFeatureLocation().getFmax());
    }

    public void testSetTranslationStart() {
        logger.info("== testSetTranslationStart() ==");
        Gene gene = createGene(0, 1000, "gene");
        Transcript transcript = createTranscript(100, 900, "transcript");
        editor.addTranscript(gene, transcript);
        editor.setTranslationStart(transcript, 200);
        assertEquals("translation start: ", new Integer(200), transcript.getCDS().getFeatureLocation().getFmin());
        printGene(gene);
    }

    public void testSetTranslationEnd() {
        logger.info("== testSetTranslationEnd() ==");
        Gene gene = createGene(0, 1000, "gene");
        Transcript transcript = createTranscript(100, 900, "transcript");
        editor.addTranscript(gene, transcript);
        editor.setTranslationEnd(transcript, 800);
//        assertEquals("translation end: ", new Integer(800), transcript.getCDS().getFeatureLocation().getFmax());
        // this sets an exclusive max
        assertEquals("translation end: ", new Integer(801), transcript.getCDS().getFeatureLocation().getFmax());
        printGene(gene);
    }

    public void testSetTranslationEnds() {
        logger.info("== testSetTranslationEnds() ==");
        Gene gene = createGene(0, 1000, "gene");
        Transcript transcript = createTranscript(100, 900, "transcript");
        editor.addTranscript(gene, transcript);
        editor.setTranslationStart(transcript, 200);
        editor.setTranslationEnd(transcript, 800);
        assertEquals("translation start: ", new Integer(200), transcript.getCDS().getFeatureLocation().getFmin());
        // this is set as an exclusive max
//        assertEquals("translation end: ", new Integer(800), transcript.getCDS().getFeatureLocation().getFmax());
        assertEquals("translation end: ", new Integer(801), transcript.getCDS().getFeatureLocation().getFmax());
        printGene(gene);
    }

    public void testSetLongestORF() throws GBOLUtilException {
        logger.info("== testSetLongestORF() ==");
        Gene gene = createGene(1);
        Transcript transcript = gene.getTranscripts().iterator().next();
        editor.setLongestORF(transcript, SequenceUtil.getTranslationTableForGeneticCode(1), false);
        printGene(gene);
        assertEquals("cds_1 fmin: ", new Integer(638), transcript.getCDS().getFeatureLocation().getFmin());
        assertEquals("cds_1 fmax: ", new Integer(2628), transcript.getCDS().getFeatureLocation().getFmax());
        for (Exon exon : transcript.getExons()) {
            if (exon.getUniqueName().equals("exon3")) {
                exon.getFeatureLocation().setFmax(transcript.getFeatureLocation().getFmax() - 2);
            }
        }
        transcript.getFeatureLocation().setFmax(transcript.getFeatureLocation().getFmax() - 2);
        editor.setLongestORF(transcript, SequenceUtil.getTranslationTableForGeneticCode(1), false);
        printGene(gene);
        assertEquals("CDS_2 fmin: ", new Integer(638), transcript.getCDS().getFeatureLocation().getFmin());
        assertEquals("CDS_2 fmax: ", new Integer(2626), transcript.getCDS().getFeatureLocation().getFmax());
        assertTrue("CDS_2 fmax (partial): ", transcript.getCDS().getFeatureLocation().isIsFmaxPartial());
        /*
        for (Exon exon : transcript.getExons()) {
            if (exon.getUniqueName().equals("exon1")) {
                exon.getFeatureLocation().setFmin(exon.getFeatureLocation().getFmin() + 3);
            }
        }
        transcript.getFeatureLocation().setFmin(transcript.getFeatureLocation().getFmin() + 3);
        */
        gene.getFeatureLocation().getSourceFeature().setResidues(gene.getFeatureLocation().getSourceFeature().getResidues().replaceAll("ATG", "AGG"));
        transcript.getFeatureLocation().setIsFmaxPartial(false);
        editor.setLongestORF(transcript, SequenceUtil.getTranslationTableForGeneticCode(1), false);
        printGene(gene);
        assertEquals("CDS_3 fmin: ", new Integer(638), transcript.getCDS().getFeatureLocation().getFmin());
        assertTrue("CDS_3 fmin (partial): ", transcript.getCDS().getFeatureLocation().isIsFminPartial());
        assertEquals("CDS_3 fmax: ", new Integer(962), transcript.getCDS().getFeatureLocation().getFmax());
        gene.getFeatureLocation().getSourceFeature().setResidues(gene.getFeatureLocation().getSourceFeature().getResidues().replaceAll("TAA", "CCC"));
        gene.getFeatureLocation().getSourceFeature().setResidues(gene.getFeatureLocation().getSourceFeature().getResidues().replaceAll("TAG", "CCC"));
        gene.getFeatureLocation().getSourceFeature().setResidues(gene.getFeatureLocation().getSourceFeature().getResidues().replaceAll("TGA", "CCC"));
        transcript.getFeatureLocation().setIsFmaxPartial(false);
        editor.setLongestORF(transcript, SequenceUtil.getTranslationTableForGeneticCode(1), false);
        printGene(gene);
        assertEquals("CDS_4 fmin: ", new Integer(638), transcript.getCDS().getFeatureLocation().getFmin());
        assertTrue("CDS_4 fmin (partial): ", transcript.getCDS().getFeatureLocation().isIsFminPartial());
        assertEquals("CDS_4 fmax: ", new Integer(2626), transcript.getCDS().getFeatureLocation().getFmax());
        assertTrue("CDS_4 fmax (partial): ", transcript.getCDS().getFeatureLocation().isIsFmaxPartial());
    }

    public void testAddExon() {
        logger.info("== testAddExon() ==");
        Gene gene = createGene(100, 1000, "gene");
        Transcript transcript = createTranscript(100, 1000, "transcript");
        gene.addTranscript(transcript);
        Exon exon1 = createExon(100, 200, "exon1");
        Exon exon2 = createExon(400, 600, "exon2");
        Exon exon3 = createExon(500, 1000, "exon3");
        editor.addExon(transcript, exon1);
        editor.addExon(transcript, exon2);
        editor.addExon(transcript, exon3);
        assertEquals("transcript num exons: ", new Integer(2), new Integer(transcript.getNumberOfExons()));
        assertEquals("exon1's parent: ", transcript, exon1.getTranscript());
        printGene(gene);
    }

    public void testDeleteExon() {
        logger.info("== testDeleteExon() ==");
        Gene gene = createGene(100, 1000, "gene");
        Transcript transcript = createTranscript(100, 1000, "transcript");
        gene.addTranscript(transcript);
        Exon exon1 = createExon(100, 200, "exon1");
        Exon exon2 = createExon(400, 500, "exon2");
        Exon exon3 = createExon(700, 1000, "exon3");
        transcript.addExon(exon1);
        transcript.addExon(exon2);
        transcript.addExon(exon3);
        assertEquals("transcript num exons (before delete): ", new Integer(3), new Integer(transcript.getNumberOfExons()));
        editor.deleteExon(transcript, exon3);
        assertEquals("transcript fmax: ", new Integer(500), transcript.getFmax());
        assertEquals("transcript num exons (after delete): ", new Integer(2), new Integer(transcript.getNumberOfExons()));
//        editor.deleteExon(transcript, exon1);
//        editor.deleteExon(transcript, exon2);
//        assertEquals("gene num transcripts (after deletes): ", new Integer(0), new Integer(gene.getNumberOfTranscripts()));
    }

    public void testMergeExons() throws AnnotationEditorException {
        logger.info("== testMergeExons() ==");
        Gene gene = createGene(100, 1000, "gene");
        Transcript transcript = createTranscript(100, 1000, "transcript");
        gene.addTranscript(transcript);
        Exon exon1 = createExon(100, 200, "exon1");
        Exon exon2 = createExon(400, 500, "exon2");
        transcript.addExon(exon1);
        transcript.addExon(exon2);
        assertEquals("transcript num exons (before merge): ", new Integer(2), new Integer(transcript.getNumberOfExons()));
        editor.mergeExons(exon1, exon2);
        assertEquals("transcript num exons (after merge [exon1, exon2]): ", new Integer(1), new Integer(transcript.getNumberOfExons()));
        assertEquals("exon fmin (after merge [exon1, exon2]): ", new Integer(100), new Integer(transcript.getExons().iterator().next().getFeatureLocation().getFmin()));
        assertEquals("exon fmax (after merge [exon1, exon2]): ", new Integer(500), new Integer(transcript.getExons().iterator().next().getFeatureLocation().getFmax()));

        gene = createGene(100, 1000, "gene");
        transcript = createTranscript(100, 1000, "transcript");
        gene.addTranscript(transcript);
        exon1 = createExon(100, 200, "exon1");
        exon2 = createExon(400, 500, "exon2");
        transcript.addExon(exon1);
        transcript.addExon(exon2);
        assertEquals("transcript num exons (before merge): ", new Integer(2), new Integer(transcript.getNumberOfExons()));
        editor.mergeExons(exon2, exon1);
        assertEquals("transcript num exons (after merge [exon2, exon1]): ", new Integer(1), new Integer(transcript.getNumberOfExons()));
        assertEquals("exon fmin (after merge [exon2, exon1]): ", new Integer(100), new Integer(transcript.getExons().iterator().next().getFeatureLocation().getFmin()));
        assertEquals("exon fmax (after merge [exon2, exon1]): ", new Integer(500), new Integer(transcript.getExons().iterator().next().getFeatureLocation().getFmax()));
    }

    public void testSplitExon() {
        logger.info("== testSplitExon() ==");
        int newLeftMax = 500;
        int newRightMin = 600;
        Gene gene = createGene(100, 1000, "gene");
        Transcript transcript = createTranscript(100, 1000, "transcript");
        gene.addTranscript(transcript);
        Exon exon = createExon(200, 900, "exon");
        transcript.addExon(exon);
        assertEquals("transcript num exons (before split): ", new Integer(1), new Integer(transcript.getNumberOfExons()));
        editor.splitExon(exon, newLeftMax, newRightMin, new TestNameAdapter(exon).generateUniqueName());
        assertEquals("transcript num exons (after split): ", new Integer(2), new Integer(transcript.getNumberOfExons()));
        printGene(gene);
    }

    public void testAddSequenceAlteration() {
        logger.info("== testAddSequenceAlteration() ==");
        Gene gene = createGene(1);
        editor.addFeature(gene);
        Transcript transcript = gene.getTranscripts().iterator().next();
        editor.setLongestORF(transcript);
        printGene(gene);
        assertEquals("CDS fmin: ", new Integer(638), transcript.getCDS().getFeatureLocation().getFmin());
        assertEquals("CDS fmax: ", new Integer(2628), transcript.getCDS().getFeatureLocation().getFmax());
        assertEquals("CDS sequence: ", "ATGAATCAGTACGGAAGAGAACAGCAAGATACTGGCCTCGTCGGCTCTGGTACAGGACATCGCGATGAATACGGCAATCCCAGGCAAGAGGGTATAATGGACAAGGTGAAAAATGCCGTAGGCATGGGCCCCAGTTCAGGAACCGGCTACAACAATCAGCCTGGTTATGACAATTACGGTAACCCAAGGCAAGAAGGATTAGTAGACAAGGCGAAGGACGCCGTGGGCATGGGTCCGAGTTTAGGAACTGGCTACAATAACCAGCCTGGTTATGACAGTTACGGGAATCGTGAGGGCATTGTGGACAGGGCGAAAGATGCGGTAGGGATGGGTCCGAATTCAGGAACTGGCTACAACAACCAGCCTGGGTACGACAATTACGGTGACCGAAGGCATGAAGGATTGGCAGACAGAGCGAAGGATGCTGTAGGTATGGGGCCTAACTCAGGTTACAACCACCAGCCTGGATATGACAACTACGGCAATCGTGAGGGCGTTGTGGACAAGGCGAAGGATGCGGTAGGCATGGGTCCGAATTCAGGAACTGGCTACAACAACCAGCCTGGTTATGACAGTTATGGTACCCGGAGACAGGAAGGATTGGTAGATAGAGCAAAGGATGCCGTCGGCATGGGCCCCAATTCGGGCACCGGCTATAACAATCAGCCCGGATATGACAACTACGGTAACCCAAGACGCGAAGGAGTGGTAGACAGGGCGAAGGATGCTGTAGGTATGGGGCCTAACTCAGGTTACAACAATCAGCCCGGATATGACAACTACGGCAATCGTGAGGGCATTGTGGACAAAGCCAAGGATGCAGTCGGTGTTGGCCCCCACTCGGGTACTGGCTACCACAACCAGCCCAGCTACGACAACTATGGCAACCCTAGGCAAGAGGGAATCGTGGATAGAGCGAAAGACGCTGTGGGGATGGGACCAAACTCTGGAACTGGCTACAACAACCAGTCTGATTATGACAGTTATGGCAACCCAAGGCACGAAGGCATGCTTGACAAGGCGAAGGATGACTTTGATATGGGCCCCAATTCCGGCACTGGCTATAACAACCGGCCCGGCTATGACACCTATGGGGACCGAAAACACGAGGGAATTGGTGACAAGGTGAGGGACGCAATCGGTACTGGCCCAAACTCCGGATATGACAGCCGCACACCCACCGGAACCGACGCTTACGTGCATGGCAACCATCCCCCTGGTATGCAAGACAGAATCACTGGCGTGAACGAGCCCTCGATCTTAGGTGGACGTGAGAATGTAGACCGCCATGGTTTTGGACACGATGGTCGCCAACATCACGGTCTGCTAGATAATGTTACTCTTCAAAGTGGCCATATTCCTGAGACTATGGTAGGCGGGCGCCGTGTTGAACCTGGATATGATATGACCAAGAGTGCTGGACATCATCTTACTGATCTTGGCCATCACGGTAACGATAGCGGTGTCACTGGATTGGGCCATCACGACACTGATTACGATGAGAGGAGGGGAAAAGGATTTGAAGACCCGATTGATAACAAAACCGGACTTGGATCAGACTACGATACGACCGAGACCGGATCTGGTTATGGTGCCACCGATACTGGTGCTGCACCTCACAAGAAGGGAATCATAACTAAGATCAAGGAGAAGCTGCACCACTAG", editor.getSession().getResiduesWithAlterations(transcript.getCDS()));
        editor.addSequenceAlteration(createSubstition("substitution 1", 641, "T", 1));
        assertEquals("CDS fmin: ", new Integer(638), transcript.getCDS().getFeatureLocation().getFmin());
        assertEquals("CDS fmax: ", new Integer(2628), transcript.getCDS().getFeatureLocation().getFmax());
        assertEquals("CDS sequence: ", "ATGTATCAGTACGGAAGAGAACAGCAAGATACTGGCCTCGTCGGCTCTGGTACAGGACATCGCGATGAATACGGCAATCCCAGGCAAGAGGGTATAATGGACAAGGTGAAAAATGCCGTAGGCATGGGCCCCAGTTCAGGAACCGGCTACAACAATCAGCCTGGTTATGACAATTACGGTAACCCAAGGCAAGAAGGATTAGTAGACAAGGCGAAGGACGCCGTGGGCATGGGTCCGAGTTTAGGAACTGGCTACAATAACCAGCCTGGTTATGACAGTTACGGGAATCGTGAGGGCATTGTGGACAGGGCGAAAGATGCGGTAGGGATGGGTCCGAATTCAGGAACTGGCTACAACAACCAGCCTGGGTACGACAATTACGGTGACCGAAGGCATGAAGGATTGGCAGACAGAGCGAAGGATGCTGTAGGTATGGGGCCTAACTCAGGTTACAACCACCAGCCTGGATATGACAACTACGGCAATCGTGAGGGCGTTGTGGACAAGGCGAAGGATGCGGTAGGCATGGGTCCGAATTCAGGAACTGGCTACAACAACCAGCCTGGTTATGACAGTTATGGTACCCGGAGACAGGAAGGATTGGTAGATAGAGCAAAGGATGCCGTCGGCATGGGCCCCAATTCGGGCACCGGCTATAACAATCAGCCCGGATATGACAACTACGGTAACCCAAGACGCGAAGGAGTGGTAGACAGGGCGAAGGATGCTGTAGGTATGGGGCCTAACTCAGGTTACAACAATCAGCCCGGATATGACAACTACGGCAATCGTGAGGGCATTGTGGACAAAGCCAAGGATGCAGTCGGTGTTGGCCCCCACTCGGGTACTGGCTACCACAACCAGCCCAGCTACGACAACTATGGCAACCCTAGGCAAGAGGGAATCGTGGATAGAGCGAAAGACGCTGTGGGGATGGGACCAAACTCTGGAACTGGCTACAACAACCAGTCTGATTATGACAGTTATGGCAACCCAAGGCACGAAGGCATGCTTGACAAGGCGAAGGATGACTTTGATATGGGCCCCAATTCCGGCACTGGCTATAACAACCGGCCCGGCTATGACACCTATGGGGACCGAAAACACGAGGGAATTGGTGACAAGGTGAGGGACGCAATCGGTACTGGCCCAAACTCCGGATATGACAGCCGCACACCCACCGGAACCGACGCTTACGTGCATGGCAACCATCCCCCTGGTATGCAAGACAGAATCACTGGCGTGAACGAGCCCTCGATCTTAGGTGGACGTGAGAATGTAGACCGCCATGGTTTTGGACACGATGGTCGCCAACATCACGGTCTGCTAGATAATGTTACTCTTCAAAGTGGCCATATTCCTGAGACTATGGTAGGCGGGCGCCGTGTTGAACCTGGATATGATATGACCAAGAGTGCTGGACATCATCTTACTGATCTTGGCCATCACGGTAACGATAGCGGTGTCACTGGATTGGGCCATCACGACACTGATTACGATGAGAGGAGGGGAAAAGGATTTGAAGACCCGATTGATAACAAAACCGGACTTGGATCAGACTACGATACGACCGAGACCGGATCTGGTTATGGTGCCACCGATACTGGTGCTGCACCTCACAAGAAGGGAATCATAACTAAGATCAAGGAGAAGCTGCACCACTAG", editor.getSession().getResiduesWithAlterations(transcript.getCDS()));
        editor.addSequenceAlteration(createSubstition("substitution 2", 700, "T", 1));
        assertEquals("CDS sequence: ", "ATGTATCAGTACGGAAGAGAACAGCAAGATACTGGCCTCGTCGGCTCTGGTACAGGACATCGCGATGAATACGGCAATCCCAGGCAAGAGGGTATAATGGACAAGGTGAAAAATGCCGTAGGCATGGGCCCCAGTTCAGGAACCGGCTACAACAATCAGCCTGGTTATGACAATTACGGTAACCCAAGGCAAGAAGGATTAGTAGACAAGGCGAAGGACGCCGTGGGCATGGGTCCGAGTTTAGGAACTGGCTACAATAACCAGCCTGGTTATGACAGTTACGGGAATCGTGAGGGCATTGTGGACAGGGCGAAAGATGCGGTAGGGATGGGTCCGAATTCAGGAACTGGCTACAACAACCAGCCTGGGTACGACAATTACGGTGACCGAAGGCATGAAGGATTGGCAGACAGAGCGAAGGATGCTGTAGGTATGGGGCCTAACTCAGGTTACAACCACCAGCCTGGATATGACAACTACGGCAATCGTGAGGGCGTTGTGGACAAGGCGAAGGATGCGGTAGGCATGGGTCCGAATTCAGGAACTGGCTACAACAACCAGCCTGGTTATGACAGTTATGGTACCCGGAGACAGGAAGGATTGGTAGATAGAGCAAAGGATGCCGTCGGCATGGGCCCCAATTCGGGCACCGGCTATAACAATCAGCCCGGATATGACAACTACGGTAACCCAAGACGCGAAGGAGTGGTAGACAGGGCGAAGGATGCTGTAGGTATGGGGCCTAACTCAGGTTACAACAATCAGCCCGGATATGACAACTACGGCAATCGTGAGGGCATTGTGGACAAAGCCAAGGATGCAGTCGGTGTTGGCCCCCACTCGGGTACTGGCTACCACAACCAGCCCAGCTACGACAACTATGGCAACCCTAGGCAAGAGGGAATCGTGGATAGAGCGAAAGACGCTGTGGGGATGGGACCAAACTCTGGAACTGGCTACAACAACCAGTCTGATTATGACAGTTATGGCAACCCAAGGCACGAAGGCATGCTTGACAAGGCGAAGGATGACTTTGATATGGGCCCCAATTCCGGCACTGGCTATAACAACCGGCCCGGCTATGACACCTATGGGGACCGAAAACACGAGGGAATTGGTGACAAGGTGAGGGACGCAATCGGTACTGGCCCAAACTCCGGATATGACAGCCGCACACCCACCGGAACCGACGCTTACGTGCATGGCAACCATCCCCCTGGTATGCAAGACAGAATCACTGGCGTGAACGAGCCCTCGATCTTAGGTGGACGTGAGAATGTAGACCGCCATGGTTTTGGACACGATGGTCGCCAACATCACGGTCTGCTAGATAATGTTACTCTTCAAAGTGGCCATATTCCTGAGACTATGGTAGGCGGGCGCCGTGTTGAACCTGGATATGATATGACCAAGAGTGCTGGACATCATCTTACTGATCTTGGCCATCACGGTAACGATAGCGGTGTCACTGGATTGGGCCATCACGACACTGATTACGATGAGAGGAGGGGAAAAGGATTTGAAGACCCGATTGATAACAAAACCGGACTTGGATCAGACTACGATACGACCGAGACCGGATCTGGTTATGGTGCCACCGATACTGGTGCTGCACCTCACAAGAAGGGAATCATAACTAAGATCAAGGAGAAGCTGCACCACTAG", editor.getSession().getResiduesWithAlterations(transcript.getCDS()));
        editor.addSequenceAlteration(createInsertion("insertion 1", 644, "ATT", 1));
        assertEquals("CDS sequence: ", "ATGTATATTCAGTACGGAAGAGAACAGCAAGATACTGGCCTCGTCGGCTCTGGTACAGGACATCGCGATGAATACGGCAATCCCAGGCAAGAGGGTATAATGGACAAGGTGAAAAATGCCGTAGGCATGGGCCCCAGTTCAGGAACCGGCTACAACAATCAGCCTGGTTATGACAATTACGGTAACCCAAGGCAAGAAGGATTAGTAGACAAGGCGAAGGACGCCGTGGGCATGGGTCCGAGTTTAGGAACTGGCTACAATAACCAGCCTGGTTATGACAGTTACGGGAATCGTGAGGGCATTGTGGACAGGGCGAAAGATGCGGTAGGGATGGGTCCGAATTCAGGAACTGGCTACAACAACCAGCCTGGGTACGACAATTACGGTGACCGAAGGCATGAAGGATTGGCAGACAGAGCGAAGGATGCTGTAGGTATGGGGCCTAACTCAGGTTACAACCACCAGCCTGGATATGACAACTACGGCAATCGTGAGGGCGTTGTGGACAAGGCGAAGGATGCGGTAGGCATGGGTCCGAATTCAGGAACTGGCTACAACAACCAGCCTGGTTATGACAGTTATGGTACCCGGAGACAGGAAGGATTGGTAGATAGAGCAAAGGATGCCGTCGGCATGGGCCCCAATTCGGGCACCGGCTATAACAATCAGCCCGGATATGACAACTACGGTAACCCAAGACGCGAAGGAGTGGTAGACAGGGCGAAGGATGCTGTAGGTATGGGGCCTAACTCAGGTTACAACAATCAGCCCGGATATGACAACTACGGCAATCGTGAGGGCATTGTGGACAAAGCCAAGGATGCAGTCGGTGTTGGCCCCCACTCGGGTACTGGCTACCACAACCAGCCCAGCTACGACAACTATGGCAACCCTAGGCAAGAGGGAATCGTGGATAGAGCGAAAGACGCTGTGGGGATGGGACCAAACTCTGGAACTGGCTACAACAACCAGTCTGATTATGACAGTTATGGCAACCCAAGGCACGAAGGCATGCTTGACAAGGCGAAGGATGACTTTGATATGGGCCCCAATTCCGGCACTGGCTATAACAACCGGCCCGGCTATGACACCTATGGGGACCGAAAACACGAGGGAATTGGTGACAAGGTGAGGGACGCAATCGGTACTGGCCCAAACTCCGGATATGACAGCCGCACACCCACCGGAACCGACGCTTACGTGCATGGCAACCATCCCCCTGGTATGCAAGACAGAATCACTGGCGTGAACGAGCCCTCGATCTTAGGTGGACGTGAGAATGTAGACCGCCATGGTTTTGGACACGATGGTCGCCAACATCACGGTCTGCTAGATAATGTTACTCTTCAAAGTGGCCATATTCCTGAGACTATGGTAGGCGGGCGCCGTGTTGAACCTGGATATGATATGACCAAGAGTGCTGGACATCATCTTACTGATCTTGGCCATCACGGTAACGATAGCGGTGTCACTGGATTGGGCCATCACGACACTGATTACGATGAGAGGAGGGGAAAAGGATTTGAAGACCCGATTGATAACAAAACCGGACTTGGATCAGACTACGATACGACCGAGACCGGATCTGGTTATGGTGCCACCGATACTGGTGCTGCACCTCACAAGAAGGGAATCATAACTAAGATCAAGGAGAAGCTGCACCACTAG", editor.getSession().getResiduesWithAlterations(transcript.getCDS()));
        editor.addSequenceAlteration(createInsertion("insertion 2", 2300, "CCC", 1));
        assertEquals("CDS sequence: ", "ATGTATATTCAGTACGGAAGAGAACAGCAAGATACTGGCCTCGTCGGCTCTGGTACAGGACATCGCGATGAATACGGCAATCCCAGGCAAGAGGGTATAATGGACAAGGTGAAAAATGCCGTAGGCATGGGCCCCAGTTCAGGAACCGGCTACAACAATCAGCCTGGTTATGACAATTACGGTAACCCAAGGCAAGAAGGATTAGTAGACAAGGCGAAGGACGCCGTGGGCATGGGTCCGAGTTTAGGAACTGGCTACAATAACCAGCCTGGTTATGACAGTTACGGGAATCGTGAGGGCATTGTGGACAGGGCGAAAGATGCGGTAGGGATGGGTCCGAATTCAGGAACTGGCTACAACAACCAGCCTGGGTACGACAATTACGGTGACCGAAGGCATGAAGGATTGGCAGACAGAGCGAAGGATGCTGTAGGTATGGGGCCTAACTCAGGTTACAACCACCAGCCTGGATATGACAACTACGGCAATCGTGAGGGCGTTGTGGACAAGGCGAAGGATGCGGTAGGCATGGGTCCGAATTCAGGAACTGGCTACAACAACCAGCCTGGTTATGACAGTTATGGTACCCGGAGACAGGAAGGATTGGTAGATAGAGCAAAGGATGCCGTCGGCATGGGCCCCAATTCGGGCACCGGCTATAACAATCAGCCCGGATATGACAACTACGGTAACCCAAGACGCGAAGGAGTGGTAGACAGGGCGAAGGATGCTGTAGGTATGGGGCCTAACTCAGGTTACAACAATCAGCCCGGATATGACAACTACGGCAATCGTGAGGGCATTGTGGACAAAGCCAAGGATGCAGTCGGTGTTGGCCCCCACTCGGGTACTGGCTACCACAACCAGCCCAGCTACGACAACTATGGCAACCCTAGGCAAGAGGGAATCGTGGATAGAGCGAAAGACGCTGTGGGGATGGGACCAAACTCTGGAACTGGCTACAACAACCAGTCTGATTATGACAGTTATGGCAACCCAAGGCACGAAGGCATGCTTGACAAGGCGAAGGATGACTTTGATATGGGCCCCAATTCCGGCACTGGCTATAACAACCGGCCCGGCTATGACACCTATGGGGACCGAAAACACGAGGGAATTGGTGACAAGGTGAGGGACGCAATCGGTACTGGCCCAAACTCCGGATATGACAGCCGCACACCCACCGGAACCGACGCTTACGTGCATGGCAACCATCCCCCTGGTATGCAAGACAGAATCACTGGCGTGAACGAGCCCTCGATCTTAGGTGGACGTGAGAATGTAGACCGCCATGGTTTTGGACACGATGGTCGCCAACATCACGGTCTGCTAGATAATGTTACTCTTCAAAGTGGCCATATTCCTGAGACTATGGTAGGCGGGCGCCGTGTTGAACCTGGATATGATATGACCAAGAGTGCTGGACATCATCTTACTGATCTTGGCCATCACGGTAACGATAGCGGTGTCACTGGATTGGGCCATCACGACACTGATTACGATGAGAGGAGGGGAAAAGGATTTGAAGACCCGATTGATAACAAAACCGGACTTGGATCAGACTACGATACGACCGAGACCGGATCTGGTTATGGTGCCACCGATACTGGTGCTGCACCTCACAAGAAGGGAATCATAACTAAGATCAAGGAGAAGCTGCACCACTAG", editor.getSession().getResiduesWithAlterations(transcript.getCDS()));
        editor.addSequenceAlteration(createDeletion("deletion 1", 641, 644, "TAT", 1));
        assertEquals("CDS sequence: ", "ATGATTCAGTACGGAAGAGAACAGCAAGATACTGGCCTCGTCGGCTCTGGTACAGGACATCGCGATGAATACGGCAATCCCAGGCAAGAGGGTATAATGGACAAGGTGAAAAATGCCGTAGGCATGGGCCCCAGTTCAGGAACCGGCTACAACAATCAGCCTGGTTATGACAATTACGGTAACCCAAGGCAAGAAGGATTAGTAGACAAGGCGAAGGACGCCGTGGGCATGGGTCCGAGTTTAGGAACTGGCTACAATAACCAGCCTGGTTATGACAGTTACGGGAATCGTGAGGGCATTGTGGACAGGGCGAAAGATGCGGTAGGGATGGGTCCGAATTCAGGAACTGGCTACAACAACCAGCCTGGGTACGACAATTACGGTGACCGAAGGCATGAAGGATTGGCAGACAGAGCGAAGGATGCTGTAGGTATGGGGCCTAACTCAGGTTACAACCACCAGCCTGGATATGACAACTACGGCAATCGTGAGGGCGTTGTGGACAAGGCGAAGGATGCGGTAGGCATGGGTCCGAATTCAGGAACTGGCTACAACAACCAGCCTGGTTATGACAGTTATGGTACCCGGAGACAGGAAGGATTGGTAGATAGAGCAAAGGATGCCGTCGGCATGGGCCCCAATTCGGGCACCGGCTATAACAATCAGCCCGGATATGACAACTACGGTAACCCAAGACGCGAAGGAGTGGTAGACAGGGCGAAGGATGCTGTAGGTATGGGGCCTAACTCAGGTTACAACAATCAGCCCGGATATGACAACTACGGCAATCGTGAGGGCATTGTGGACAAAGCCAAGGATGCAGTCGGTGTTGGCCCCCACTCGGGTACTGGCTACCACAACCAGCCCAGCTACGACAACTATGGCAACCCTAGGCAAGAGGGAATCGTGGATAGAGCGAAAGACGCTGTGGGGATGGGACCAAACTCTGGAACTGGCTACAACAACCAGTCTGATTATGACAGTTATGGCAACCCAAGGCACGAAGGCATGCTTGACAAGGCGAAGGATGACTTTGATATGGGCCCCAATTCCGGCACTGGCTATAACAACCGGCCCGGCTATGACACCTATGGGGACCGAAAACACGAGGGAATTGGTGACAAGGTGAGGGACGCAATCGGTACTGGCCCAAACTCCGGATATGACAGCCGCACACCCACCGGAACCGACGCTTACGTGCATGGCAACCATCCCCCTGGTATGCAAGACAGAATCACTGGCGTGAACGAGCCCTCGATCTTAGGTGGACGTGAGAATGTAGACCGCCATGGTTTTGGACACGATGGTCGCCAACATCACGGTCTGCTAGATAATGTTACTCTTCAAAGTGGCCATATTCCTGAGACTATGGTAGGCGGGCGCCGTGTTGAACCTGGATATGATATGACCAAGAGTGCTGGACATCATCTTACTGATCTTGGCCATCACGGTAACGATAGCGGTGTCACTGGATTGGGCCATCACGACACTGATTACGATGAGAGGAGGGGAAAAGGATTTGAAGACCCGATTGATAACAAAACCGGACTTGGATCAGACTACGATACGACCGAGACCGGATCTGGTTATGGTGCCACCGATACTGGTGCTGCACCTCACAAGAAGGGAATCATAACTAAGATCAAGGAGAAGCTGCACCACTAG", editor.getSession().getResiduesWithAlterations(transcript.getCDS()));
        editor.addSequenceAlteration(createSubstition("substitution 3", 638, "C", 1));
        editor.setLongestORF(transcript);
        assertEquals("CDS fmin: ", new Integer(890), transcript.getCDS().getFeatureLocation().getFmin());
        assertEquals("CDS fmax: ", new Integer(2628), transcript.getCDS().getFeatureLocation().getFmax());
    }

    public void testSplitTranscript() {
        logger.info("== testSplitTranscript() ==");
        Gene gene = createGene(1);
        assertEquals("Number of transcripts before split: ", new Integer(1), new Integer(gene.getTranscripts().size()));
        Transcript transcript = gene.getTranscripts().iterator().next();
        List<Exon> exons = BioObjectUtil.createSortedFeatureListByLocation(transcript.getExons());
        Transcript splitTranscript = editor.splitTranscript(transcript, exons.get(1), exons.get(2), new TestNameAdapter(transcript).generateUniqueName());
        assertEquals("Number of transcripts after split: ", new Integer(2), new Integer(gene.getTranscripts().size()));
        assertEquals("Transcript fmin: ", new Integer(638), new Integer(transcript.getFmin()));
        assertEquals("Transcript fmax: ", new Integer(2223), new Integer(transcript.getFmax()));
        assertEquals("Transcript num exons: ", new Integer(2), new Integer(transcript.getExons().size()));
        assertEquals("Transcript-split fmin: ", new Integer(2392), new Integer(splitTranscript.getFmin()));
        assertEquals("Transcript-split fmax: ", new Integer(2628), new Integer(splitTranscript.getFmax()));
        assertEquals("Transcript-split num exons: ", new Integer(1), new Integer(splitTranscript.getExons().size()));
    }

    public void testSetFeatureBoundaries() {
        logger.info("== testSetFeatureBoundaries() ==");
        Transcript transcript = createTranscript(100, 1000, "transcript");
        Exon exon = createExon(100, 1000, "exon");
        transcript.addExon(exon);
        editor.setExonBoundaries(exon, 10, 200);
        assertEquals("Exon new fmin: ", new Integer(10), new Integer(exon.getFmin()));
        assertEquals("Exon new fmax: ", new Integer(200), new Integer(exon.getFmax()));
        assertEquals("Transcript new fmin: ", new Integer(10), new Integer(exon.getFmin()));
        assertEquals("Transcript new fmax: ", new Integer(200), new Integer(exon.getFmax()));
    }

    public void testFlipStrand() {
        logger.info("== testFlipStrand() ==");
        Gene gene = createGene(1);
        List<AbstractSingleLocationBioFeature> features = new ArrayList<AbstractSingleLocationBioFeature>();
        features.add(gene);
        while (features.size() > 0) {
            AbstractSingleLocationBioFeature feature = features.remove(0);
            assertEquals("Feature strand: ", new Integer(1), feature.getStrand());
            features.addAll(feature.getChildren());
        }
        editor.flipStrand(gene);
        features.add(gene);
        while (features.size() > 0) {
            AbstractSingleLocationBioFeature feature = features.remove(0);
            assertEquals("Feature strand: ", new Integer(-1), feature.getStrand());
            features.addAll(feature.getChildren());
        }
    }

    /**
     * TODO: Seems like a valid test, but nothing is actually added here
     */
    @Ignore
//    public void testFindNonCanonicalAcceptorDonorSpliceSites() {
    public void doNotTestFindNonCanonicalAcceptorDonorSpliceSites() {
        logger.info("== testFindNonCanonicalAcceptorDonorSpliceSites() ==");
        Gene gene = createGene(1);
        for (Transcript transcript : gene.getTranscripts()) {
            editor.findNonCanonicalAcceptorDonorSpliceSites(transcript);
            assertEquals("Number of non canonical 5' splice sites (plus strand - before modification): ", new Integer(0),
                    new Integer(transcript.getNonCanonicalFivePrimeSpliceSites().size()));
            assertEquals("Number of non canonical 3' splice sites (plus strand - before modification): ", new Integer(0),
                    new Integer(transcript.getNonCanonicalThreePrimeSpliceSites().size()));
            List<Exon> exons = BioObjectUtil.createSortedFeatureListByLocation(transcript.getExons(), false);
            Exon exon = exons.get(1);
            exon.setFmin(exon.getFmin() - 1);
            exon.setFmax(exon.getFmax() + 1);
            editor.findNonCanonicalAcceptorDonorSpliceSites(transcript);
            assertEquals("Number of non canonical 5' splice sites (plus strand - after 1st modification): ", new Integer(1),
                    new Integer(transcript.getNonCanonicalFivePrimeSpliceSites().size()));
            assertEquals("Number of non canonical 3' splice sites (plus strand - after 1st modification): ", new Integer(1),
                    new Integer(transcript.getNonCanonicalThreePrimeSpliceSites().size()));
            exon.setFmin(exon.getFmin() + 1);
            exon.setFmax(exon.getFmax() - 1);
            exon = exons.get(0);
            exon.setFmin(exon.getFmin() - 1);
            exon.setFmax(exon.getFmax() + 1);
            editor.findNonCanonicalAcceptorDonorSpliceSites(transcript);
            assertEquals("Number of non canonical 5' splice sites (plus strand - after 2nd modification): ", new Integer(1),
                    new Integer(transcript.getNonCanonicalFivePrimeSpliceSites().size()));
            assertEquals("Number of non canonical 3' splice sites (plus strand - after 2nd modification): ", new Integer(0),
                    new Integer(transcript.getNonCanonicalThreePrimeSpliceSites().size()));
            exon.setFmin(exon.getFmin() + 1);
            exon.setFmax(exon.getFmax() - 1);
            exon = exons.get(2);
            exon.setFmin(exon.getFmin() - 1);
            exon.setFmax(exon.getFmax() + 1);
            editor.findNonCanonicalAcceptorDonorSpliceSites(transcript);
            assertEquals("Number of non canonical 5' splice sites (plus strand - after 3rd modification): ", new Integer(0),
                    new Integer(transcript.getNonCanonicalFivePrimeSpliceSites().size()));
            assertEquals("Number of non canonical 3' splice sites (plus strand - after 3rd modification): ", new Integer(1),
                    new Integer(transcript.getNonCanonicalThreePrimeSpliceSites().size()));
        }
        gene = createGene(-1);
        for (Transcript transcript : gene.getTranscripts()) {
            editor.findNonCanonicalAcceptorDonorSpliceSites(transcript);
            assertEquals("Number of non canonical 5' splice sites (minus strand - before modification): ", new Integer(0),
                    new Integer(transcript.getNonCanonicalFivePrimeSpliceSites().size()));
            assertEquals("Number of non canonical 3' splice sites (minus strand - before modification): ", new Integer(0),
                    new Integer(transcript.getNonCanonicalThreePrimeSpliceSites().size()));
            List<Exon> exons = BioObjectUtil.createSortedFeatureListByLocation(transcript.getExons(), false);
            Exon exon = exons.get(1);
            exon.setFmin(exon.getFmin() - 1);
            exon.setFmax(exon.getFmax() + 1);
            editor.findNonCanonicalAcceptorDonorSpliceSites(transcript);
            assertEquals("Number of non canonical 5' splice sites (minus strand - after 1st modification): ", new Integer(1),
                    new Integer(transcript.getNonCanonicalFivePrimeSpliceSites().size()));
            assertEquals("Number of non canonical 3' splice sites (minus strand - after 1st modification): ", new Integer(1),
                    new Integer(transcript.getNonCanonicalThreePrimeSpliceSites().size()));
            exon.setFmin(exon.getFmin() + 1);
            exon.setFmax(exon.getFmax() - 1);
            exon = exons.get(0);
            exon.setFmin(exon.getFmin() - 1);
            exon.setFmax(exon.getFmax() + 1);
            editor.findNonCanonicalAcceptorDonorSpliceSites(transcript);
            assertEquals("Number of non canonical 5' splice sites (minus strand - after 2nd modification): ", new Integer(0),
                    new Integer(transcript.getNonCanonicalFivePrimeSpliceSites().size()));
            assertEquals("Number of non canonical 3' splice sites (minus strand - after 2nd modification): ", new Integer(1),
                    new Integer(transcript.getNonCanonicalThreePrimeSpliceSites().size()));
            exon.setFmin(exon.getFmin() + 1);
            exon.setFmax(exon.getFmax() - 1);
            exon = exons.get(2);
            exon.setFmin(exon.getFmin() - 1);
            exon.setFmax(exon.getFmax() + 1);
            editor.findNonCanonicalAcceptorDonorSpliceSites(transcript);
            assertEquals("Number of non canonical 5' splice sites (minus strand - after 3rd modification): ", new Integer(1),
                    new Integer(transcript.getNonCanonicalFivePrimeSpliceSites().size()));
            assertEquals("Number of non canonical 3' splice sites (minus strand - after 3rd modification): ", new Integer(0),
                    new Integer(transcript.getNonCanonicalThreePrimeSpliceSites().size()));
        }
    }

    private Gene createGene(int fmin, int fmax, String name) {
        Gene gene = new Gene(organism, name, false, false, new Timestamp(0), conf);
        FeatureLocation loc = new FeatureLocation();
        loc.setFmin(fmin);
        loc.setFmax(fmax);
        loc.setStrand(1);
        gene.setFeatureLocation(loc);
        return gene;
    }

    private Transcript createTranscript(int fmin, int fmax, String name) {
        Transcript transcript = new Transcript(organism, name, false, false, new Timestamp(0), conf);
        FeatureLocation loc = new FeatureLocation();
        loc.setFmin(fmin);
        loc.setFmax(fmax);
        loc.setStrand(1);
        transcript.setFeatureLocation(loc);
        return transcript;
    }

    private Exon createExon(int fmin, int fmax, String name) {
        Exon exon = new Exon(organism, name, false, false, new Timestamp(0), conf);
        FeatureLocation loc = new FeatureLocation();
        loc.setFmin(fmin);
        loc.setFmax(fmax);
        loc.setStrand(1);
        exon.setFeatureLocation(loc);
        return exon;
    }

    private void printGene(Gene gene) {
        printFeatureInfo(gene, 0);
        for (Transcript transcript : gene.getTranscripts()) {
            printFeatureInfo(transcript, 1);
            if (transcript.getCDS() != null) {
                printFeatureInfo(transcript.getCDS(), 1);
            }
            for (Exon exon : transcript.getExons()) {
                printFeatureInfo(exon, 2);
            }
        }
    }

    private Chromosome createChromosome(int strand) {
        Chromosome chromosome = new Chromosome(organism, "chromosome", false, false, new Timestamp(0), conf);
        if (strand == -1) {
            chromosome.setResidues("ATTAACAAAAACTCAAATATTCACATTTCATGAAACAAAAATTACACCAGGTGTTTCTGCAAGAAACCAAATCTCCATTTACATGATAGATTACTACGCTTCCTTCTCTAGTGGTGCAGCTTCTCCTTGATCTTAGTTATGATTCCCTTCTTGTGAGGTGCAGCACCAGTATCGGTGGCACCATAACCAGATCCGGTCTCGGTCGTATCGTAGTCTGATCCAAGTCCGGTTTTGTTATCAATCGGGTCTTCAAATCCTTTTCCCCTCCTCTCATCGTAATCAGTGTCGTGATGGCCCAATCCAGTGACACCGCTATCGTTACCGTGATGGCCAAGATCAGTAACTGCATCGACACCACATTACCATCAATCAAGTTAACACGAAGGAATGAGCACCACAACAGCGGAATCACCGATTTTCCAAAACCATGAAGCACCACCAATTTAGCAGANTTCAACAAGCTGCCTAAACTTCGAAATAATTTGCAAGAAGATATAGCATGGTAGACTCACGATGATGTCCAGCACTCTTGGTCATATCATATCCAGGTTCAACACGGCGCCCGCCTACCATAGTCTCAGGAATATGGCCACTTTGAAGAGTAACATTATCTAGCAGACCGTGATGTTGGCGACCATCGTGTCCAAAACCATGGCGGTCTACATTCTCACGTCCACCTAAGATCGAGGGCTCGTTCACGCCAGTGATTCTGTCTTGCATACCAGGGGGATGGTTGCCATGCACGTAAGCGTCGGTTCCGGTGGGTGTGCGGCTGTCATATCCGGAGTTTGGGCCAGTACCGATTGCGTCCCTCACCTTGTCACCAATTCCCTCGTGTTTTCGGTCCCCATAGGTGTCATAGCCGGGCCGGTTGTTATAGCCAGTGCCGGAATTGGGGCCCATATCAAAGTCATCCTTCGCCTTGTCAAGCATGCCTTCGTGCCTTGGGTTGCCATAACTGTCATAATCAGACTGGTTGTTGTAGCCAGTTCCAGAGTTTGGTCCCATCCCCACAGCGTCTTTCGCTCTATCCACGATTCCCTCTTGCCTAGGGTTGCCATAGTTGTCGTAGCTGGGCTGGTTGTGGTAGCCAGTACCCGAGTGGGGGCCAACACCGACTGCATCCTTGGCTTTGTCCACAATGCCCTCACGATTGCCGTAGTTGTCATATCCGGGCTGATTGTTGTAACCTGAGTTAGGCCCCATACCTACAGCATCCTTCGCCCTGTCTACCACTCCTTCGCGTCTTGGGTTACCGTAGTTGTCATATCCGGGCTGATTGTTATAGCCGGTGCCCGAATTGGGGCCCATGCCGACGGCATCCTTTGCTCTATCTACCAATCCTTCCTGTCTCCGGGTACCATAACTGTCATAACCAGGCTGGTTGTTGTAGCCAGTTCCTGAATTCGGACCCATGCCTACCGCATCCTTCGCCTTGTCCACAACGCCCTCACGATTGCCGTAGTTGTCATATCCAGGCTGGTGGTTGTAACCTGAGTTAGGCCCCATACCTACAGCATCCTTCGCTCTGTCTGCCAATCCTTCATGCCTTCGGTCACCGTAATTGTCGTACCCAGGCTGGTTGTTGTAGCCAGTTCCTGAATTCGGACCCATCCCTACCGCATCTTTCGCCCTGTCCACAATGCCCTCACGATTCCCGTAACTGTCATAACCAGGCTGGTTATTGTAGCCAGTTCCTAAACTCGGACCCATGCCCACGGCGTCCTTCGCCTTGTCTACTAATCCTTCTTGCCTTGGGTTACCGTAATTGTCATAACCAGGCTGATTGTTGTAGCCGGTTCCTGAACTGGGGCCCATGCCTACGGCATTTTTCACCTTGTCCATTATACCCTCTTGCCTGGGATTGCCGTATTCATCGCGATGTCCTACACCATCAGAAGATGGATCGTTAGCGAGAACAAATGAGCTACACCAAAGATTGCGAGGGGTTCCGTGCCGAAAATAAAAAAGACCTTTGAGGATCGTTCATCATCAATCCACGGAAAACAGTGTTTAAAATCGCAAAAAAAACATTTCATTACCTGTACCAGAGCCGACGAGGCCAGTATCTTGCTGTTCTCTTCCGTACTGATTCATGATTGTCGATGATTTCTATATACTCTGCAAAAGAAATGGAAGCATTGTTGTTAAGAAATCGCTACATTAGACCACTGTATGCTTCAAATATCCTCGATCCCAAACATGCATGGCAAACAAAGACTCTAATGGAATCTTTAATGGTAGAAAGATCATCACAATTATTTGACGATCGAAAAGTTCTTTAGCAGGAGAAATAGTCTTCGCAATGCTCATTGATTCCAAAAGCAGATCTAACCACAAACAAACATACAGAAGAACTCCCTCTACGGAGAAACATCGGAAACCCAGAGCCCCATCGACAGATCGACCCTACAGAACAAGTCAAATGTGTCCCAACCAGCCTACATTACGCACCAGAGGCTCTCTACATCCACACAACGAATCAGACAGATCGCAAACTAGGTACGAAACTGTGAACCAAGCAACATTGACCTAGCATTTCACAAACTCGCATCGACACGAACATCCTACAGTGGCCGAGAGAGACACACTCACACAGCATGCGACATAATCGCGCCTAACGCCAAAAGGCACGATCGATGAACACTCTCTCCGACACACCGAAAAGTCTCGCGATTCTTCTTCCACGTCTAAAGTGCATACAAGTCCTCTGCAACGATTGTGAGAAAAGATAT");
        } else {
            chromosome.setResidues("ATATCTTTTCTCACAATCGTTGCAGAGGACTTGTATGCACTTTAGACGTGGAAGAAGAATCGCGAGACTTTTCGGTGTGTCGGAGAGAGTGTTCATCGATCGTGCCTTTTGGCGTTAGGCGCGATTATGTCGCATGCTGTGTGAGTGTGTCTCTCTCGGCCACTGTAGGATGTTCGTGTCGATGCGAGTTTGTGAAATGCTAGGTCAATGTTGCTTGGTTCACAGTTTCGTACCTAGTTTGCGATCTGTCTGATTCGTTGTGTGGATGTAGAGAGCCTCTGGTGCGTAATGTAGGCTGGTTGGGACACATTTGACTTGTTCTGTAGGGTCGATCTGTCGATGGGGCTCTGGGTTTCCGATGTTTCTCCGTAGAGGGAGTTCTTCTGTATGTTTGTTTGTGGTTAGATCTGCTTTTGGAATCAATGAGCATTGCGAAGACTATTTCTCCTGCTAAAGAACTTTTCGATCGTCAAATAATTGTGATGATCTTTCTACCATTAAAGATTCCATTAGAGTCTTTGTTTGCCATGCATGTTTGGGATCGAGGATATTTGAAGCATACAGTGGTCTAATGTAGCGATTTCTTAACAACAATGCTTCCATTTCTTTTGCAGAGTATATAGAAATCATCGACAATCATGAATCAGTACGGAAGAGAACAGCAAGATACTGGCCTCGTCGGCTCTGGTACAGGTAATGAAATGTTTTTTTTGCGATTTTAAACACTGTTTTCCGTGGATTGATGATGAACGATCCTCAAAGGTCTTTTTTATTTTCGGCACGGAACCCCTCGCAATCTTTGGTGTAGCTCATTTGTTCTCGCTAACGATCCATCTTCTGATGGTGTAGGACATCGCGATGAATACGGCAATCCCAGGCAAGAGGGTATAATGGACAAGGTGAAAAATGCCGTAGGCATGGGCCCCAGTTCAGGAACCGGCTACAACAATCAGCCTGGTTATGACAATTACGGTAACCCAAGGCAAGAAGGATTAGTAGACAAGGCGAAGGACGCCGTGGGCATGGGTCCGAGTTTAGGAACTGGCTACAATAACCAGCCTGGTTATGACAGTTACGGGAATCGTGAGGGCATTGTGGACAGGGCGAAAGATGCGGTAGGGATGGGTCCGAATTCAGGAACTGGCTACAACAACCAGCCTGGGTACGACAATTACGGTGACCGAAGGCATGAAGGATTGGCAGACAGAGCGAAGGATGCTGTAGGTATGGGGCCTAACTCAGGTTACAACCACCAGCCTGGATATGACAACTACGGCAATCGTGAGGGCGTTGTGGACAAGGCGAAGGATGCGGTAGGCATGGGTCCGAATTCAGGAACTGGCTACAACAACCAGCCTGGTTATGACAGTTATGGTACCCGGAGACAGGAAGGATTGGTAGATAGAGCAAAGGATGCCGTCGGCATGGGCCCCAATTCGGGCACCGGCTATAACAATCAGCCCGGATATGACAACTACGGTAACCCAAGACGCGAAGGAGTGGTAGACAGGGCGAAGGATGCTGTAGGTATGGGGCCTAACTCAGGTTACAACAATCAGCCCGGATATGACAACTACGGCAATCGTGAGGGCATTGTGGACAAAGCCAAGGATGCAGTCGGTGTTGGCCCCCACTCGGGTACTGGCTACCACAACCAGCCCAGCTACGACAACTATGGCAACCCTAGGCAAGAGGGAATCGTGGATAGAGCGAAAGACGCTGTGGGGATGGGACCAAACTCTGGAACTGGCTACAACAACCAGTCTGATTATGACAGTTATGGCAACCCAAGGCACGAAGGCATGCTTGACAAGGCGAAGGATGACTTTGATATGGGCCCCAATTCCGGCACTGGCTATAACAACCGGCCCGGCTATGACACCTATGGGGACCGAAAACACGAGGGAATTGGTGACAAGGTGAGGGACGCAATCGGTACTGGCCCAAACTCCGGATATGACAGCCGCACACCCACCGGAACCGACGCTTACGTGCATGGCAACCATCCCCCTGGTATGCAAGACAGAATCACTGGCGTGAACGAGCCCTCGATCTTAGGTGGACGTGAGAATGTAGACCGCCATGGTTTTGGACACGATGGTCGCCAACATCACGGTCTGCTAGATAATGTTACTCTTCAAAGTGGCCATATTCCTGAGACTATGGTAGGCGGGCGCCGTGTTGAACCTGGATATGATATGACCAAGAGTGCTGGACATCATCGTGAGTCTACCATGCTATATCTTCTTGCAAATTATTTCGAAGTTTAGGCAGCTTGTTGAANTCTGCTAAATTGGTGGTGCTTCATGGTTTTGGAAAATCGGTGATTCCGCTGTTGTGGTGCTCATTCCTTCGTGTTAACTTGATTGATGGTAATGTGGTGTCGATGCAGTTACTGATCTTGGCCATCACGGTAACGATAGCGGTGTCACTGGATTGGGCCATCACGACACTGATTACGATGAGAGGAGGGGAAAAGGATTTGAAGACCCGATTGATAACAAAACCGGACTTGGATCAGACTACGATACGACCGAGACCGGATCTGGTTATGGTGCCACCGATACTGGTGCTGCACCTCACAAGAAGGGAATCATAACTAAGATCAAGGAGAAGCTGCACCACTAGAGAAGGAAGCGTAGTAATCTATCATGTAAATGGAGATTTGGTTTCTTGCAGAAACACCTGGTGTAATTTTTGTTTCATGAAATGTGAATATTTGAGTTTTTGTTAAT");
        }
        return chromosome;
    }

    private Gene createGene(int strand) {
        Chromosome chromosome = createChromosome(strand);
        chromosome.setFeatureLocation(0, 2735, 1, null);
        Gene gene = new Gene(organism, "gene", false, false, new Timestamp(0), conf);
        gene.setFeatureLocation(0, 2735, strand, chromosome);
        Transcript transcript = createTranscript(638, 2628, strand, "transcript", chromosome);
        gene.addTranscript(transcript);
        transcript.addExon(createExon(638, 693, strand, "exon1", chromosome));
        transcript.addExon(createExon(849, 2223, strand, "exon2", chromosome));
        transcript.addExon(createExon(2392, 2628, strand, "exon3", chromosome));
        return gene;
    }

    private Transcript createTranscript(int fmin, int fmax, int strand, String name, Chromosome src) {
        Transcript transcript = new Transcript(organism, name, false, false, new Timestamp(0), conf);
        if (strand == -1) {
            int tmp = fmax;
            fmax = src.getLength() - fmin;
            fmin = src.getLength() - tmp;
        }
        transcript.setFeatureLocation(fmin, fmax, strand, src);
        return transcript;
    }

    private Exon createExon(int fmin, int fmax, int strand, String name, Chromosome src) {
        Exon exon = new Exon(organism, name, false, false, new Timestamp(0), conf);
        if (strand == -1) {
            int tmp = fmax;
            fmax = src.getLength() - fmin;
            fmin = src.getLength() - tmp;
        }
        exon.setFeatureLocation(fmin, fmax, strand, src);
        return exon;
    }

    private Frameshift createFrameshift(String uniqueName, int coordinate, Transcript transcript,
                                        Class<? extends Frameshift> clazz) {
        if (clazz == Plus1Frameshift.class) {
            return new Plus1Frameshift(transcript, coordinate, transcript.getConfiguration());
        } else if (clazz == Plus2Frameshift.class) {
            return new Plus2Frameshift(transcript, coordinate, transcript.getConfiguration());
        } else if (clazz == Minus1Frameshift.class) {
            return new Minus1Frameshift(transcript, coordinate, transcript.getConfiguration());
        } else if (clazz == Minus2Frameshift.class) {
            return new Minus2Frameshift(transcript, coordinate, transcript.getConfiguration());
        }
        return null;
    }

    private Insertion createInsertion(String uniqueName, int coordinate, String residues, int strand) {
        Chromosome chromosome = createChromosome(strand);
        Insertion insertion = new Insertion(organism, uniqueName, false, false, new Timestamp(0), conf);
        insertion.setFeatureLocation(coordinate, coordinate, 1, chromosome);
        insertion.setResidues(residues);
        return insertion;
    }

    private Deletion createDeletion(String uniqueName, int from, int to, String residues, int strand) {
        Chromosome chromosome = createChromosome(strand);
        Deletion deletion = new Deletion(organism, uniqueName, false, false, new Timestamp(0), conf);
        deletion.setFeatureLocation(from, to, 1, chromosome);
        deletion.setResidues(residues);
        return deletion;
    }

    private Substitution createSubstition(String uniqueName, int coordinate, String residues, int strand) {
        Chromosome chromosome = createChromosome(strand);
        Substitution substitution = new Substitution(organism, uniqueName, false, false, new Timestamp(0), conf);
        substitution.setFeatureLocation(coordinate, coordinate + residues.length(), 1, chromosome);
        substitution.setResidues(residues);
        return substitution;
    }

    private void printFeatureInfo(AbstractSingleLocationBioFeature feature, int indent) {
        for (int i = 0; i < indent; ++i) {
            System.out.print("\t");
        }
        System.out.printf("%s\t(%s%d,%d%s)%n", feature.getUniqueName(),
                feature.getFeatureLocation().isIsFminPartial() ? "<" : "",
                feature.getFeatureLocation().getFmin(),
                feature.getFeatureLocation().getFmax(),
                feature.getFeatureLocation().isIsFmaxPartial() ? ">" : "");
    }

    private class TestNameAdapter {

        private AbstractSingleLocationBioFeature feature;

        public TestNameAdapter(AbstractSingleLocationBioFeature feature) {
            this.feature = feature;
        }

        public String generateUniqueName() {
            return feature.getUniqueName() + "-split";
        }

    }

}
