package org.gmod.gbol.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Different methods for dealing with sequence data.
 * 
 * @author elee
 *
 */

public class SequenceUtil {
	
	private static TranslationTable[] translationTables = null;
	private static Set<String> spliceAcceptorSites = new HashSet<String>();
	private static Set<String> spliceDonorSites = new HashSet<String>();
	
	/** Reverse complement a nucleotide sequence.
	 * 
	 * @param sequence - String for the nucleotide sequence to be reverse complemented
	 * @return Reverse complemented nucleotide sequence
	 */
	public static String reverseComplementSequence(String sequence) {
		StringBuilder buffer = new StringBuilder(sequence);
		buffer.reverse();
		for (int i = 0; i < buffer.length(); ++i) {
			switch (buffer.charAt(i)) {
			case 'A':
				buffer.setCharAt(i, 'T');
				break;
			case 'C':
				buffer.setCharAt(i, 'G');
				break;
			case 'G':
				buffer.setCharAt(i, 'C');
				break;
			case 'T':
				buffer.setCharAt(i, 'A');
				break;
			}
		}
		return buffer.toString();
	}

	/** Translate a nucleotide sequence into an amino acid sequence using the translation table.  The returned
	 *  translated sequence will not include the stop amino acid and will stop at the first stop codon it
	 *  finds as determined by the translation table.
	 * 
	 * @param sequence - Nucleotide sequence to be translated
	 * @param translationTable - TranslationTable that contains the codon translation table
	 * @return Translated amino acid sequence 
	 */
	public static String translateSequence(String sequence, TranslationTable translationTable) {
		return translateSequence(sequence, translationTable, false, false);
	}

	/**  Translate a nucleotide sequence into an amino acid sequence using the translation table.
	 * 
	 * @param sequence - Nucleotide sequence to be translated
	 * @param translationTable - TranslationTable that contains the codon translation table
	 * @param includeStop - Whether to include the stop amino acid in the translated sequence
	 * @param translateThroughStop - Whether to continue translation through stop codons
	 * @return Translated amino acid sequence 
	 */
	public static String translateSequence(String sequence, TranslationTable translationTable,
			boolean includeStop, boolean translateThroughStop) {
//		if (sequence.length() % 3 != 0) {
//			throw new GBOLUtilException("Sequence to be translated must have length of factor of 3");
//		}
		StringBuilder buffer = new StringBuilder();
		int stopCodonCount = 0;
		for (int i = 0; i + 3 <= sequence.length(); i += 3) {
			String codon = sequence.substring(i, i + 3);
			String aminoAcid = translationTable.translateCodon(codon);
			if (aminoAcid.equals(TranslationTable.STOP)) {
				if (includeStop) {
					buffer.append(aminoAcid);
				}
				if (!translateThroughStop) {
					break;
				}
				else {
					if (++stopCodonCount > 1) {
						break;
					}
				}
			}
			else {
				buffer.append(aminoAcid);
			}
		}
		return buffer.toString();
	}
	
	/** Get the translation table for a NCBI translation table code.
	 * 
	 * @param code - NCBI translation table code
	 * @return TranslationTable for the NCBI translation table code
	 * @throws GBOLUtilException - If an invalid NCBI translation table code is used
	 */
	public static TranslationTable getTranslationTableForGeneticCode(int code) throws GBOLUtilException {
		if (translationTables == null) {
			initTranslationTables();
		}
		if (code < 1 || code > translationTables.length) {
			throw new GBOLUtilException("Invalid translation table code");
		}
		return translationTables[code - 1];
	}

	/** Get the default translation table (NCBI translation table code 1).
	 * 
	 * @return Default translation table
	 */
	public static TranslationTable getDefaultTranslationTable() {
		if (translationTables == null) {
			initTranslationTables();
		}
		return translationTables[0];
	}
	
	/** Abstract class that all specific translation tables must inherit from.
	 * 
	 * @author elee
	 *
	 */
	public static abstract class TranslationTable {

		/** Amino acid for a stop codon.
		 * 
		 */
		public static final String STOP = "*";

		protected Map<String, String> translationTable;
		protected Set<String> startCodons;
		protected Set<String> stopCodons;
		protected Map<String, String> alternateTranslationTable;
		
		protected TranslationTable() {
			translationTable = new HashMap<String, String>();
			startCodons = new HashSet<String>();
			stopCodons = new HashSet<String>();
			alternateTranslationTable = new HashMap<String, String>();
		}
		
		public TranslationTable cloneTable() {
			TranslationTable clone = null;
			try {
				clone = getClass().newInstance();
				clone.translationTable = new HashMap<String, String>(translationTable);
				clone.startCodons = new HashSet<String>(startCodons);
				clone.stopCodons = new HashSet<String>(stopCodons);
				clone.alternateTranslationTable = new HashMap<String, String>(alternateTranslationTable);
			}
			catch (InstantiationException e) {
			}
			catch (IllegalAccessException e) {
			}
			return clone;
		}
		
		/** Return the amino acid corresponding to the codon translation.  Returns "X" if the codon doesn't
		 *  exist in the translation table.
		 * 
		 * @param codon - Codon to be translated
		 * @return Amino acid corresponding to the the codon
		 */
		public String translateCodon(String codon) {
			String aa = translationTable.get(codon);
			if (aa == null) {
				return "X";
			}
			return aa;
		}
		
		/** Return a collection of start codons for the translation table.
		 * 
		 * @return Collection of Strings representing the start codons
		 */
		public Collection<String> getStartCodons() {
			return startCodons;
		}
		
		/** Return a collection of stop codons for the translation table.
		 * 
		 * @return Collection of Strings representing the stop codons
		 */
		public Collection<String> getStopCodons() {
			return stopCodons;
		}
		
		protected boolean isStopCodon(String codon) {
			return translateCodon(codon).equals(STOP);
		}
		
		public Map<String, String> getTranslationTable() {
			return translationTable;
		}
		
		public Map<String, String> getAlternateTranslationTable() {
			return alternateTranslationTable;
		}
	}
	
	private static class StandardTranslationTable extends TranslationTable {

		public StandardTranslationTable() {
			translationTable.put("TTT", "F");
			translationTable.put("TTC", "F");
			translationTable.put("TTA", "L");
			translationTable.put("TTG", "L");
			translationTable.put("CTT", "L");
			translationTable.put("CTC", "L");
			translationTable.put("CTA", "L");
			translationTable.put("CTG", "L");
			translationTable.put("ATT", "I");
			translationTable.put("ATC", "I");
			translationTable.put("ATA", "I");
			translationTable.put("ATG", "M");
			translationTable.put("GTT", "V");
			translationTable.put("GTC", "V");
			translationTable.put("GTA", "V");
			translationTable.put("GTG", "V");

			translationTable.put("TCT", "S");
			translationTable.put("TCC", "S");
			translationTable.put("TCA", "S");
			translationTable.put("TCG", "S");
			translationTable.put("CCT", "P");
			translationTable.put("CCC", "P");
			translationTable.put("CCA", "P");
			translationTable.put("CCG", "P");
			translationTable.put("ACT", "T");
			translationTable.put("ACC", "T");
			translationTable.put("ACA", "T");
			translationTable.put("ACG", "T");
			translationTable.put("GCT", "A");
			translationTable.put("GCC", "A");
			translationTable.put("GCA", "A");
			translationTable.put("GCG", "A");
		
			translationTable.put("TAT", "Y");
			translationTable.put("TAC", "Y");
			translationTable.put("TAA", STOP);
			translationTable.put("TAG", STOP);
			translationTable.put("CAT", "H");
			translationTable.put("CAC", "H");
			translationTable.put("CAA", "Q");
			translationTable.put("CAG", "Q");
			translationTable.put("AAT", "N");
			translationTable.put("AAC", "N");
			translationTable.put("AAA", "K");
			translationTable.put("AAG", "K");
			translationTable.put("GAT", "D");
			translationTable.put("GAC", "D");
			translationTable.put("GAA", "E");
			translationTable.put("GAG", "E");
			
			translationTable.put("TGT", "C");
			translationTable.put("TGC", "C");
			translationTable.put("TGA", STOP);
			translationTable.put("TGG", "W");
			translationTable.put("CGT", "R");
			translationTable.put("CGC", "R");
			translationTable.put("CGA", "R");
			translationTable.put("CGG", "R");
			translationTable.put("AGT", "S");
			translationTable.put("AGC", "S");
			translationTable.put("AGA", "R");
			translationTable.put("AGG", "R");
			translationTable.put("GGT", "G");
			translationTable.put("GGC", "G");
			translationTable.put("GGA", "G");
			translationTable.put("GGG", "G");
			
			startCodons.add("ATG");
			
			stopCodons.add("TAA");
			stopCodons.add("TAG");
			stopCodons.add("TGA");
			
			alternateTranslationTable.put("TGA", "U");
		}
		
	}
	
	private static void initTranslationTables() {
		translationTables = new TranslationTable[23];
		translationTables[0] = new StandardTranslationTable();
	}

	/** Get the splice acceptor sites.
	 * 
	 * @return Set of strings for splice acceptor sites
	 */
	public static Set<String> getSpliceAcceptorSites() {
		return spliceAcceptorSites;
	}
	
	/** Add a splice acceptor site.
	 * 
	 * @param spliceAcceptorSite - String for splice acceptor site
	 */
	public static void addSpliceAcceptorSite(String spliceAcceptorSite) {
		SequenceUtil.spliceAcceptorSites.add(spliceAcceptorSite);
	}
	
	/** Remove a splice acceptor site.
	 * 
	 * @param spliceAcceptorSite - String for splice acceptor site
	 */
	public static void deleteSpliceAcceptorSite(String spliceAcceptorSite) {
		SequenceUtil.spliceAcceptorSites.remove(spliceAcceptorSite);
	}
	
	/** Get the splice donor sites.
	 * 
	 * @return Set of string for splice donor sites
	 */
	public static Set<String> getSpliceDonorSites() {
		return spliceDonorSites;
	}
	
	/** Add a splice donor site.
	 * 
	 * @param spliceDonorSite - Strings for splice donor site
	 */
	public static void addSpliceDonorSite(String spliceDonorSite) {
		SequenceUtil.spliceDonorSites.add(spliceDonorSite);
	}

	/** Remove a splice donor site.
	 * 
	 * @param spliceDonorSite - String for splice donor site
	 */
	public static void deleteSpliceDonorSite(String spliceDonorSite) {
		SequenceUtil.spliceDonorSites.remove(spliceDonorSite);
	}

}
