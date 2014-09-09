package org.bbop.apollo.unit;

import junit.framework.TestCase;
import org.bbop.apollo.editor.session.AnnotationSession;
import org.gmod.gbol.bioObject.*;
import org.gmod.gbol.bioObject.conf.BioObjectConfiguration;
import org.gmod.gbol.simpleObject.Feature;
import org.gmod.gbol.simpleObject.FeatureLocation;
import org.gmod.gbol.simpleObject.Organism;

import java.sql.Timestamp;
import java.util.Collection;

public class AnnotationSessionTest extends TestCase {

    private Organism organism;
    private BioObjectConfiguration conf;
    
    public void setUp() {
        organism = new Organism("Foomus", "barius");
        conf = new BioObjectConfiguration("src/test/resources/testSupport/mapping.xml");
    }

    public void testAddFeature() {
        System.out.println("== testAddFeature() ==");
        AnnotationSession session = new AnnotationSession();
        session.addFeature(createGene("gene 1", 1, 20, 1));
        session.addFeature(createGene("gene 2", 10, 30, 1));
        session.addFeature(createGene("gene 3", 20, 40, 1));
        session.addFeature(createGene("gene 4", 30, 50, 1));
        assertEquals("Number of features: ", new Integer(4), new Integer(session.getFeatures().size()));
    }
    
    public void testDeleteFeature() {
        System.out.println("== testDeleteFeature() ==");
        AnnotationSession session = new AnnotationSession();
        session.addFeature(createGene("gene 1", 1, 20, 1));
        session.addFeature(createGene("gene 2", 10, 30, 1));
        session.addFeature(createGene("gene 3", 20, 40, 1));
        session.addFeature(createGene("gene 4", 30, 50, 1));
        assertEquals("Number of features: ", new Integer(4), new Integer(session.getFeatures().size()));
        session.deleteFeature(createGene("gene 1", 1, 20, 1));
        assertEquals("Number of features: ", new Integer(3), new Integer(session.getFeatures().size()));
        session.deleteFeature(createGene("gene X", 1, 50, 1));
        assertEquals("Number of features: ", new Integer(3), new Integer(session.getFeatures().size()));
        session.deleteFeature(createGene("gene Y", 20, 40, -1));
        assertEquals("Number of features: ", new Integer(3), new Integer(session.getFeatures().size()));
    }
    
    public void testGetOverlappingFeatures() {
        System.out.println("== testGetOverlappingFeatures() ==");
        AnnotationSession session = new AnnotationSession();
        session.addFeature(createGene("gene 1", 1, 20, 1));
        session.addFeature(createGene("gene 2", 10, 30, 1));
        session.addFeature(createGene("gene 3", 20, 40, 1));
        session.addFeature(createGene("gene 4", 30, 50, 1));
        session.addFeature(createGene("gene 5", 33, 43, 1));
        Collection<AbstractSingleLocationBioFeature> features =
            session.getOverlappingFeatures(createFeatureLocation(35, 37, 1));
        assertEquals("Number of overlapping features: ", new Integer(3), new Integer(features.size()));
        features = session.getOverlappingFeatures(createFeatureLocation(5, 8, 1));
        assertEquals("Number of overlapping features: ", new Integer(1), new Integer(features.size()));
    }
    
    public void testAddSequenceAlteration() {
        System.out.println("== testAddSequenceAlteration() ==");
        AnnotationSession session = new AnnotationSession();
        session.addSequenceAlteration(createInsertion("insertion", 5, "A"));
        
        assertEquals("Number of sequence alterations: ", new Integer(1), new Integer(session.getSequenceAlterations().size()));
    }

    public void testDeleteSequenceAlteration() {
        System.out.println("== testDeleteSequenceAlteration() ==");
        AnnotationSession session = new AnnotationSession();
        session.addSequenceAlteration(createInsertion("insertion", 5, "A"));
        session.addSequenceAlteration(createSubstition("substition", 10, "C"));
        assertEquals("Number of sequence alterations: ", new Integer(2), new Integer(session.getSequenceAlterations().size()));

        session.deleteSequenceAlteration(createSubstition("substition", 10, "C"));
        assertEquals("Number of sequence alterations: ", new Integer(1), new Integer(session.getSequenceAlterations().size()));

        session.deleteSequenceAlteration(createInsertion("insertion", 5, "A"));
        assertEquals("Number of sequence alterations: ", new Integer(0), new Integer(session.getSequenceAlterations().size()));
    }
    
    public void testGetResiduesWithAlterationsForInsertions() {
        System.out.println("== testGetResiduesWithAlterationsForInsertions() ==");
        AnnotationSession session = new AnnotationSession();
        Gene gene = createGene(1);
        Transcript transcript = gene.getTranscripts().iterator().next();
        assertEquals("Gene pre-alteration: ", "ATCGTCGCGGATCGTCGCGGATCGTCGCGGATCGTCGCGGATCGTCGCGG", session.getResiduesWithAlterations(gene));
        assertEquals("Transcript pre-alteration: ", "CGCGGATCGTATCGTCGCGGATCGTATCGT", session.getResiduesWithAlterations(transcript));
        session.addSequenceAlteration(createInsertion("insertion1", 10, "A"));
        assertEquals("Gene insertion 1: ", "ATCGTCGCGGAATCGTCGCGGATCGTCGCGGATCGTCGCGGATCGTCGCGG", session.getResiduesWithAlterations(gene));
        assertEquals("Transcript insertion 1: ", "CGCGGAATCGTATCGTCGCGGATCGTATCGT", session.getResiduesWithAlterations(transcript));
        session.addSequenceAlteration(createInsertion("insertion 2", 7, "CC"));
        assertEquals("Gene insertion 2: ", "ATCGTCGCCCGGAATCGTCGCGGATCGTCGCGGATCGTCGCGGATCGTCGCGG", session.getResiduesWithAlterations(gene));
        assertEquals("Transcript insertion 2: ", "CGCCCGGAATCGTATCGTCGCGGATCGTATCGT", session.getResiduesWithAlterations(transcript));
        session.addSequenceAlteration(createInsertion("insertion 3", 11, "TTT"));
        assertEquals("Gene insertion 3: ", "ATCGTCGCCCGGAATTTTCGTCGCGGATCGTCGCGGATCGTCGCGGATCGTCGCGG", session.getResiduesWithAlterations(gene));
        assertEquals("Transcript insertion 3: ", "CGCCCGGAATTTTCGTATCGTCGCGGATCGTATCGT", session.getResiduesWithAlterations(transcript));
        session.addSequenceAlteration(createInsertion("insertion 4", 25, "GGGG"));
        assertEquals("Gene insertion 4: ", "ATCGTCGCCCGGAATTTTCGTCGCGGATCGTGGGGCGCGGATCGTCGCGGATCGTCGCGG", session.getResiduesWithAlterations(gene));
        assertEquals("Transcript insertion 4: ", "CGCCCGGAATTTTCGTATCGTGGGGCGCGGATCGTATCGT", session.getResiduesWithAlterations(transcript));
        
        session = new AnnotationSession();
        gene = createGene(-1);
        transcript = gene.getTranscripts().iterator().next();
        assertEquals("Gene pre-alteration: ", "CCGCGACGATCCGCGACGATCCGCGACGATCCGCGACGATCCGCGACGAT", session.getResiduesWithAlterations(gene));
        assertEquals("Transcript pre-alteration: ", "ACGATACGATCCGCGACGATACGATCCGCG", session.getResiduesWithAlterations(transcript));
        session.addSequenceAlteration(createInsertion("insertion1", 10, "A"));
        assertEquals("Gene insertion 1: ", "CCGCGACGATCCGCGACGATCCGCGACGATCCGCGACGATTCCGCGACGAT", session.getResiduesWithAlterations(gene));
        assertEquals("Transcript insertion 1: ", "ACGATACGATCCGCGACGATACGATTCCGCG", session.getResiduesWithAlterations(transcript));
        session.addSequenceAlteration(createInsertion("insertion 2", 7, "CC"));
        assertEquals("Gene insertion 2: ", "CCGCGACGATCCGCGACGATCCGCGACGATCCGCGACGATTCCGGGCGACGAT", session.getResiduesWithAlterations(gene));
        assertEquals("Transcript insertion 2: ", "ACGATACGATCCGCGACGATACGATTCCGGGCG", session.getResiduesWithAlterations(transcript));
        session.addSequenceAlteration(createInsertion("insertion 3", 11, "TTT"));
        assertEquals("Gene insertion 3: ", "CCGCGACGATCCGCGACGATCCGCGACGATCCGCGACGAAAATTCCGGGCGACGAT", session.getResiduesWithAlterations(gene));
        assertEquals("Transcript insertion 3: ", "ACGATACGATCCGCGACGATACGAAAATTCCGGGCG", session.getResiduesWithAlterations(transcript));
        session.addSequenceAlteration(createInsertion("insertion 4", 25, "GGGG"));
        assertEquals("Gene insertion 4: ", "CCGCGACGATCCGCGACGATCCGCGCCCCACGATCCGCGACGAAAATTCCGGGCGACGAT", session.getResiduesWithAlterations(gene));
        assertEquals("Transcript insertion 4: ", "ACGATACGATCCGCGCCCCACGATACGAAAATTCCGGGCG", session.getResiduesWithAlterations(transcript));
        
//        assertEquals("Gene insertion 4: ", "CCGCGACGATCCGCGACGATCCGCCCCCGACGATCCGCGACGAAAATTCCGGGCGACGAT", session.getResiduesWithAlterations(gene));
    }
    
    public void testGetResiduesWithAlterationsForDeletions() {
        System.out.println("== testGetResiduesWithAlterationsForDeletions() == ");
        AnnotationSession session = new AnnotationSession();
        Gene gene = createGene(1);
        Transcript transcript = gene.getTranscripts().iterator().next();
        assertEquals("Gene pre-alteration: ", "ATCGTCGCGGATCGTCGCGGATCGTCGCGGATCGTCGCGGATCGTCGCGG", session.getResiduesWithAlterations(gene));
        assertEquals("Transcript pre-alteration: ", "CGCGGATCGTATCGTCGCGGATCGTATCGT", session.getResiduesWithAlterations(transcript));
        session.addSequenceAlteration(createDeletion("deletion 1", 10, 11));
        assertEquals("Gene deletion 1: ", "ATCGTCGCGGTCGTCGCGGATCGTCGCGGATCGTCGCGGATCGTCGCGG", session.getResiduesWithAlterations(gene));
        assertEquals("Transcript deletion 1: ", "CGCGGTCGTATCGTCGCGGATCGTATCGT", session.getResiduesWithAlterations(transcript));
        session.addSequenceAlteration(createDeletion("deletion 2", 30, 33));
        assertEquals("Gene deletion 2: ", "ATCGTCGCGGTCGTCGCGGATCGTCGCGGGTCGCGGATCGTCGCGG", session.getResiduesWithAlterations(gene));
        assertEquals("Transcript deletion 2: ", "CGCGGTCGTATCGTCGCGGGTATCGT", session.getResiduesWithAlterations(transcript));
        session.addSequenceAlteration(createDeletion("deletion 3", 40, 44));
        assertEquals("Gene deletion 3: ", "ATCGTCGCGGTCGTCGCGGATCGTCGCGGGTCGCGGTCGCGG", session.getResiduesWithAlterations(gene));
        assertEquals("Transcript deletion 3: ", "CGCGGTCGTATCGTCGCGGGTT", session.getResiduesWithAlterations(transcript));
        
        session = new AnnotationSession();
        gene = createGene(-1);
        transcript = gene.getTranscripts().iterator().next();
        assertEquals("Gene pre-alteration: ", "CCGCGACGATCCGCGACGATCCGCGACGATCCGCGACGATCCGCGACGAT", session.getResiduesWithAlterations(gene));
        assertEquals("Transcript pre-alteration: ", "ACGATACGATCCGCGACGATACGATCCGCG", session.getResiduesWithAlterations(transcript));
        session.addSequenceAlteration(createDeletion("deletion 1", 10, 11));
        assertEquals("Gene deletion 1: ", "CCGCGACGATCCGCGACGATCCGCGACGATCCGCGACGACCGCGACGAT", session.getResiduesWithAlterations(gene));
        assertEquals("Transcript deletion 1: ", "ACGATACGATCCGCGACGATACGACCGCG", session.getResiduesWithAlterations(transcript));
        session.addSequenceAlteration(createDeletion("deletion 2", 30, 33));
        assertEquals("Gene deletion 2: ", "CCGCGACGATCCGCGACCCGCGACGATCCGCGACGACCGCGACGAT", session.getResiduesWithAlterations(gene));
        assertEquals("Transcript deletion 2: ", "ACGATACCCGCGACGATACGACCGCG", session.getResiduesWithAlterations(transcript));
        session.addSequenceAlteration(createDeletion("deletion 3", 40, 44));
        assertEquals("Gene deletion 3: ", "CCGCGACCGCGACCCGCGACGATCCGCGACGACCGCGACGAT", session.getResiduesWithAlterations(gene));
        assertEquals("Transcript deletion 3: ", "AACCCGCGACGATACGACCGCG", session.getResiduesWithAlterations(transcript));
    }

    public void testGetResiduesWithAlterationsForSubstitutions() {
        System.out.println("== testGetResiduesWithAlterationsForSubstitutions() == ");
        AnnotationSession session = new AnnotationSession();
        Gene gene = createGene(1);
        Transcript transcript = gene.getTranscripts().iterator().next();
        assertEquals("Gene pre-alteration: ", "ATCGTCGCGGATCGTCGCGGATCGTCGCGGATCGTCGCGGATCGTCGCGG", session.getResiduesWithAlterations(gene));
        assertEquals("Transcript pre-alteration: ", "CGCGGATCGTATCGTCGCGGATCGTATCGT", session.getResiduesWithAlterations(transcript));
        session.addSequenceAlteration(createSubstition("substitution 1", 10, "C"));
        assertEquals("Gene substitution 1: ", "ATCGTCGCGGCTCGTCGCGGATCGTCGCGGATCGTCGCGGATCGTCGCGG", session.getResiduesWithAlterations(gene));
        assertEquals("Transcript substitution 1: ", "CGCGGCTCGTATCGTCGCGGATCGTATCGT", session.getResiduesWithAlterations(transcript));
        session.addSequenceAlteration(createSubstition("substitution 2", 30, "C"));
        assertEquals("Gene substitution 2: ", "ATCGTCGCGGCTCGTCGCGGATCGTCGCGGCTCGTCGCGGATCGTCGCGG", session.getResiduesWithAlterations(gene));
        assertEquals("Transcript substitution 2: ", "CGCGGCTCGTATCGTCGCGGCTCGTATCGT", session.getResiduesWithAlterations(transcript));

        session = new AnnotationSession();
        gene = createGene(-1);
        transcript = gene.getTranscripts().iterator().next();
        assertEquals("Gene pre-alteration: ", "CCGCGACGATCCGCGACGATCCGCGACGATCCGCGACGATCCGCGACGAT", session.getResiduesWithAlterations(gene));
        assertEquals("Transcript pre-alteration: ", "ACGATACGATCCGCGACGATACGATCCGCG", session.getResiduesWithAlterations(transcript));
        session.addSequenceAlteration(createSubstition("substitution 1", 10, "C"));
        assertEquals("Gene substitution 1: ", "CCGCGACGATCCGCGACGATCCGCGACGATCCGCGACGAGCCGCGACGAT", session.getResiduesWithAlterations(gene));
        assertEquals("Transcript substitution 1: ", "ACGATACGATCCGCGACGATACGAGCCGCG", session.getResiduesWithAlterations(transcript));
        session.addSequenceAlteration(createSubstition("substitution 2", 30, "C"));
        assertEquals("Gene substitution 2: ", "CCGCGACGATCCGCGACGAGCCGCGACGATCCGCGACGAGCCGCGACGAT", session.getResiduesWithAlterations(gene));
        assertEquals("Transcript substitution 2: ", "ACGATACGAGCCGCGACGATACGAGCCGCG", session.getResiduesWithAlterations(transcript));
    }
    
    public void testGetResiduesWithPlusFrameshifts() {
        System.out.println("== testGetResiduesWithPlusFrameshifts() == ");
        AnnotationSession session = new AnnotationSession();
        Gene gene = createGene(1);
        Transcript transcript = gene.getTranscripts().iterator().next();
        assertEquals("Transcript pre-frameshifts: ", "CGCGGATCGTATCGTCGCGGATCGTATCGT", session.getResiduesWithFrameshifts(transcript));
        assertEquals("CDS pre-frameshifts: ", "ATCGTATCGTCGCGGATCGTAT", session.getResiduesWithFrameshifts(transcript.getCDS()));
        transcript.addFrameshift(createFrameshift("frameshift 1", 11, transcript, Plus1Frameshift.class));
        assertEquals("Transcript frameshift 1: ", "CGCGGATCGTATCGTCGCGGATCGTATCGT", session.getResiduesWithFrameshifts(transcript));
        assertEquals("CDS frameshift 1: ", "ACGTATCGTCGCGGATCGTAT", session.getResiduesWithFrameshifts(transcript.getCDS()));
        transcript.addFrameshift(createFrameshift("frameshift 2", 25, transcript, Plus2Frameshift.class));
        assertEquals("Transcript frameshift 2: ", "CGCGGATCGTATCGTCGCGGATCGTATCGT", session.getResiduesWithFrameshifts(transcript));
        assertEquals("CDS frameshift 2: ", "ACGTATCGTCGGATCGTAT", session.getResiduesWithFrameshifts(transcript.getCDS()));

        gene = createGene(-1);
        transcript = gene.getTranscripts().iterator().next();
        assertEquals("Transcript pre-alteration: ", "ACGATACGATCCGCGACGATACGATCCGCG", session.getResiduesWithAlterations(transcript));
        assertEquals("CDS pre-frameshifts: ", "ATACGATCCGCGACGATACGAT", session.getResiduesWithFrameshifts(transcript.getCDS()));
        transcript.addFrameshift(createFrameshift("frameshift 1", 11, transcript, Plus1Frameshift.class));
        assertEquals("Transcript frameshift 1: ", "ACGATACGATCCGCGACGATACGATCCGCG", session.getResiduesWithFrameshifts(transcript));
        assertEquals("CDS frameshift 1: ", "ATACGATCCGCGACGATACGT", session.getResiduesWithFrameshifts(transcript.getCDS()));
        transcript.addFrameshift(createFrameshift("frameshift 2", 25, transcript, Plus2Frameshift.class));
        assertEquals("Transcript frameshift 2: ", "ACGATACGATCCGCGACGATACGATCCGCG", session.getResiduesWithFrameshifts(transcript));
        assertEquals("CDS frameshift 2: ", "ATACGATCCGACGATACGT", session.getResiduesWithFrameshifts(transcript.getCDS()));
    }
    
    public void testGetResiduesWithMinusFrameshifts() {
        System.out.println("== testGetResiduesWithMinusFrameshifts() == ");
        AnnotationSession session = new AnnotationSession();
        Gene gene = createGene(1);
        Transcript transcript = gene.getTranscripts().iterator().next();
        assertEquals("Transcript pre-frameshifts: ", "CGCGGATCGTATCGTCGCGGATCGTATCGT", session.getResiduesWithFrameshifts(transcript));
        assertEquals("CDS pre-frameshifts: ", "ATCGTATCGTCGCGGATCGTAT", session.getResiduesWithFrameshifts(transcript.getCDS()));
        transcript.addFrameshift(createFrameshift("frameshift 1", 11, transcript, Minus1Frameshift.class));
        assertEquals("Transcript frameshift 1: ", "CGCGGATCGTATCGTCGCGGATCGTATCGT", session.getResiduesWithFrameshifts(transcript));
        assertEquals("CDS frameshift 1: ", "AATCGTATCGTCGCGGATCGTAT", session.getResiduesWithFrameshifts(transcript.getCDS()));
        transcript.addFrameshift(createFrameshift("frameshift 2", 25, transcript, Minus2Frameshift.class));
        assertEquals("Transcript frameshift 2: ", "CGCGGATCGTATCGTCGCGGATCGTATCGT", session.getResiduesWithFrameshifts(transcript));
        assertEquals("CDS frameshift 2: ", "AATCGTATCGTGTCGCGGATCGTAT", session.getResiduesWithFrameshifts(transcript.getCDS()));

        gene = createGene(-1);
        transcript = gene.getTranscripts().iterator().next();
        assertEquals("Transcript pre-alteration: ", "ACGATACGATCCGCGACGATACGATCCGCG", session.getResiduesWithAlterations(transcript));
        assertEquals("CDS pre-frameshifts: ", "ATACGATCCGCGACGATACGAT", session.getResiduesWithFrameshifts(transcript.getCDS()));
        transcript.addFrameshift(createFrameshift("frameshift 1", 11, transcript, Minus1Frameshift.class));
        assertEquals("Transcript frameshift 1: ", "ACGATACGATCCGCGACGATACGATCCGCG", session.getResiduesWithFrameshifts(transcript));
        assertEquals("CDS frameshift 1: ", "ATACGATCCGCGACGATACGATT", session.getResiduesWithFrameshifts(transcript.getCDS()));
        transcript.addFrameshift(createFrameshift("frameshift 2", 25, transcript, Minus2Frameshift.class));
        assertEquals("Transcript frameshift 1: ", "ACGATACGATCCGCGACGATACGATCCGCG", session.getResiduesWithFrameshifts(transcript));
        assertEquals("CDS frameshift 1: ", "ATACGATCCGCGACACGATACGATT", session.getResiduesWithFrameshifts(transcript.getCDS()));
    }
    
    public void testConvertModifiedLocalCoordinateToSourceCoordinate() {
        System.out.println("== testConvertModifiedLocalCoordinateToSourceCoordinate() ==");
        AnnotationSession session = new AnnotationSession();
        Gene gene = createGene(1);
        Transcript transcript = gene.getTranscripts().iterator().next();
        session.addSequenceAlteration(createInsertion("insertion", 7, "AT"));
        assertEquals("Gene sequence modified 1: ", "ATCGTCGATCGGATCGTCGCGGATCGTCGCGGATCGTCGCGGATCGTCGCGG", session.getResiduesWithAlterationsAndFrameshifts(gene));
        assertEquals("Gene modified local coordinate (11): ", new Integer(9), new Integer(session.convertModifiedLocalCoordinateToSourceCoordinate(gene, 11)));
        assertEquals("Transcript sequence modified 1: ", "CGATCGGATCGTATCGTCGCGGATCGTATCGT", session.getResiduesWithAlterationsAndFrameshifts(transcript));
        assertEquals("Transcript modified local coordinate (5): ", new Integer(8), new Integer(session.convertModifiedLocalCoordinateToSourceCoordinate(transcript, 5)));
        session.addSequenceAlteration(createDeletion("deletion", 30, 33));
        assertEquals("Gene sequence modified 2: ", "ATCGTCGATCGGATCGTCGCGGATCGTCGCGGGTCGCGGATCGTCGCGG", session.getResiduesWithAlterationsAndFrameshifts(gene));
        assertEquals("Gene modified local coordinate (35): ", new Integer(36), new Integer(session.convertModifiedLocalCoordinateToSourceCoordinate(gene, 35)));
        assertEquals("Transcript sequence modified 2: ", "CGATCGGATCGTATCGTCGCGGGTATCGT", session.getResiduesWithAlterationsAndFrameshifts(transcript));
        assertEquals("Transcript modified local coordinate (24): ", new Integer(35), new Integer(session.convertModifiedLocalCoordinateToSourceCoordinate(transcript, 24)));
        session.addSequenceAlteration(createSubstition("substitution", 43, "C"));
        assertEquals("Gene sequence modified 3: ", "ATCGTCGATCGGATCGTCGCGGATCGTCGCGGGTCGCGGATCCTCGCGG", session.getResiduesWithAlterationsAndFrameshifts(gene));
        assertEquals("Gene modified local coordinate (43): ", new Integer(44), new Integer(session.convertModifiedLocalCoordinateToSourceCoordinate(gene, 43)));
        assertEquals("Transcript sequence modified 3: ", "CGATCGGATCGTATCGTCGCGGGTATCCT", session.getResiduesWithAlterationsAndFrameshifts(transcript));
        assertEquals("Transcript modified local coordinate (27): ", new Integer(43), new Integer(session.convertModifiedLocalCoordinateToSourceCoordinate(transcript, 27)));
    }
    
    public void testGetFeatureByUniqueName() {
        System.out.println("== testGetFeatureByUniqueName() ==");
        AnnotationSession session = new AnnotationSession();
        session.addFeature(createGene(1));
        assertEquals("Gene unique name: ", "gene", session.getFeatureByUniqueName("gene").getUniqueName());
        assertEquals("Transcript unique name:", "transcript", session.getFeatureByUniqueName("transcript").getUniqueName());
        assertEquals("Exon unique name: ", "exon1", session.getFeatureByUniqueName("exon1").getUniqueName());
        assertEquals("Exon unique name: ", "exon2", session.getFeatureByUniqueName("exon2").getUniqueName());
        assertEquals("Exon unique name: ", "exon3", session.getFeatureByUniqueName("exon3").getUniqueName());
        assertEquals("CDS unique name: ", "cds", session.getFeatureByUniqueName("cds").getUniqueName());
        assertNotSame("Exon unique name: ", "exon1", session.getFeatureByUniqueName("exon2").getUniqueName());
        assertNull("Non existing feature: ", session.getFeatureByUniqueName("foo"));
    }
    
    private Chromosome createChromosome() {
        Chromosome chromosome = new Chromosome(organism, "chromosome", false, false, new Timestamp(0), conf);
        chromosome.setResidues("ATCGTCGCGGATCGTCGCGGATCGTCGCGGATCGTCGCGGATCGTCGCGG");
        return chromosome;
    }
    
    private Gene createGene(String uniqueName, int fmin, int fmax, int strand) {
        Chromosome chromosome = createChromosome();
        Gene gene = new Gene(organism, uniqueName, false, false, new Timestamp(0), conf);
        gene.setFeatureLocation(fmin, fmax, strand, chromosome);
        return gene;
    }
    
    private Gene createGene(int strand) {
        Chromosome chromosome = createChromosome();
        Gene gene = new Gene(organism, "gene", false, false, new Timestamp(0), conf);
        gene.setFeatureLocation(0, chromosome.getResidues().length(), strand, chromosome);
        gene.addTranscript(createTranscript(strand));
        return gene;
    }
    
    private Transcript createTranscript(int strand) {
        Chromosome chromosome = createChromosome();
        Transcript transcript = new Transcript(organism, "transcript", false, false, new Timestamp(0), conf);
        transcript.setFeatureLocation(5, 45, strand, chromosome);
        Exon exon1 = new Exon(organism, "exon1", false, false, new Timestamp(0), conf);
        exon1.setFeatureLocation(5, 15, strand, chromosome);
        transcript.addExon(exon1);
        Exon exon2 = new Exon(organism, "exon2", false, false, new Timestamp(0), conf);
        exon2.setFeatureLocation(20, 35, strand, chromosome);
        transcript.addExon(exon2);
        Exon exon3 = new Exon(organism, "exon3", false, false, new Timestamp(0), conf);
        exon3.setFeatureLocation(40, 45, strand, chromosome);
        transcript.addExon(exon3);
        CDS cds = new CDS(organism, "cds", false, false, new Timestamp(0), conf);
        cds.setFeatureLocation(10, 42, strand, chromosome);
        transcript.setCDS(cds);
        return transcript;
    }
    
    private Insertion createInsertion(String uniqueName, int coordinate, String residues) {
        Chromosome chromosome = createChromosome();
        Insertion insertion = new Insertion(organism, uniqueName, false, false, new Timestamp(0), conf);
        insertion.setFeatureLocation(coordinate, coordinate, 1, chromosome);
        insertion.setResidues(residues);
        return insertion;
    }

    private Deletion createDeletion(String uniqueName, int from, int to) {
        Chromosome chromosome = createChromosome();
        Deletion deletion = new Deletion(organism, uniqueName, false, false, new Timestamp(0), conf);
        deletion.setFeatureLocation(from, to, 1, chromosome);
        return deletion;
    }
    
    private Substitution createSubstition(String uniqueName, int coordinate, String residues) {
        Chromosome chromosome = createChromosome();
        Substitution substitution = new Substitution(organism, uniqueName, false, false, new Timestamp(0), conf);
        substitution.setFeatureLocation(coordinate, coordinate + residues.length(), 1, chromosome);
        substitution.setResidues(residues);
        return substitution;
    }
    
    private Frameshift createFrameshift(String uniqueName, int coordinate, Transcript transcript,
            Class<? extends Frameshift> clazz) {
        if (clazz == Plus1Frameshift.class) {
            return new Plus1Frameshift(transcript, coordinate, transcript.getConfiguration());
        }
        else if (clazz == Plus2Frameshift.class) {
            return new Plus2Frameshift(transcript, coordinate, transcript.getConfiguration());
        }
        else if (clazz == Minus1Frameshift.class) {
            return new Minus1Frameshift(transcript, coordinate, transcript.getConfiguration());
        }
        else if (clazz == Minus2Frameshift.class) {
            return new Minus2Frameshift(transcript, coordinate, transcript.getConfiguration());
        }
        return null;
    }
    
    private FeatureLocation createFeatureLocation(int fmin, int fmax, int strand) {
        FeatureLocation loc = new FeatureLocation();
        loc.setFmin(fmin);
        loc.setFmax(fmax);
        loc.setStrand(strand);
        Feature chromosome = new Feature(
                conf.getDefaultCVTermForClass("Chromosome"),
                null,
                organism,
                null,
                "chromosome",
                null,
                null,
                null,
                false,
                false,
                new Timestamp(0),
                null);
        loc.setSourceFeature(chromosome);
        return loc;
    }
}
