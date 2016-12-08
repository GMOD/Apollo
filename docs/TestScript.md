# Apollo Testing Script

Note: The following steps are meant for testing purposes only, not for training manual annotators.

## Apollo General Information
- The Apollo website:
http://GenomeArchitect.org
- The article describing Apollo can be found at:  
http://genomebiology.com/2013/14/8/R93/abstract
- The public Apollo honey bee (_Apis mellifera_) demonstration site is available at: 
http://genomearchitect.org/demo/
- You may find our user guide at:
http://genomearchitect.org/users-guide/
- You may find a few slide presentations on the 'How Tos' of Apollo at:
http://www.slideshare.net/MonicaMunozTorres/
- Apollo at GMOD page: 
http://www.gmod.org/wiki/WebApollo 
- Apollo installation and configuration guide
http://genomearchitect.readthedocs.io/en/latest/


## Testing an Apollo Instance

### A) Testing functions in the main window

1) Switch between organisms:

1.1) Check that you can change organisms from the upper left-corner drop-down menu. 

1.2) Check that you can switch organisms from the _Organism_ tab in the _Annotator Panel_

1.3) Check that the organism preference is preserved only on a single browser tab.

2) Test top-level menu options:

2.1) Login / Logout
   
   Test that you are able to logout using the options on the upper right corner of the main window by clicking on your user ID and choosing to 'logout'. Then, test that you are able to log back in. 
   
   Test that all browsers log out for a set user. 
   
   When logged out you should still be able to view "public" organisms and browse public genomes from the link on the login screen.

2.2) File 

   /Open (Test that data can be loaded locally using URLs (File / Open / Remote URLs)).

   /Add Combination Track: test that the arithmetic combination of quantitative tracks is possible by dragging two of them into a 'combination track'. Test different operations (addition, substraction) and arrangements (left and right positions for each track) as appropriate.
   
   /Add Sequence Search Track and perform a search test.

2.3) View
   
   Check the ability to set and clear highlights, show plus/minus strands, show track label, resize quantitative tracks, color by CDS, and changing the color scheme (dark or light).

2.4) Tools

   From _Tools_ menu, query the genome with BLAT, using an amino acid or nucleotide sequence. For example: Housekeeping gene Calpain small subunit 1 CPNS1, CAPNS1, CAPN4, CAPNS (UniProt).

   >sp|P04632|CPNS1_HUMAN Calpain small subunit 1 MFLVNSFLKGGGGGGGGGGGLGGGLGNVLGGLISGAGGGGGGGGGGGGGGGGGGGGTAMRILGGVISAISEAAAQYNPEPPPPRTHYSNIEANESEEVRQFRRLFAQLAGDDMEVSATELMNILNKVVTRHPDLKTDGFGIDTCRSMVAVMDSDTTGKLGFEEFKYLWNNIKRWQAIYKQFDTDRSGTICSSELPGAFEAAGFHLNEHLYNMIIRRYSDESGNMDFDNFISCLVRLDAMFRAFKSLDKDGTGQIQVNIQEWLQLTMYS
   
   Clear the highlighted region using the option from the _View_ menu.

2.5) Help

   Check that all links go to a new screen.

2.6) Full-screen view

   Check that this link opens up a new window showing only the browser and JBrowse track menu, without the annotator panel. Then, check that you are able to return from _Full-screen view_ to a window that includes both the main annotation area with the _Annotator Panel_ on the side by using the _Show Annotator Panel_ button.

3) Test the Navigation Bar

3.1) Search for an indexed gene (e.g. in honey bee demo CSN2_DANRE (it's on Group1.37:152689..155265)) by typing the gene name on the search box in the middle of the navigation bar in the main window. 

4) Drag and drop a gene onto the “User-created Annotations” (U-cA) area.

5) Zoom in (double click) to inspect last exon (5'-3') of the displayed gene and:

5.1) Change intron/exon boundary (dragging)

5.2) Check the recalculated ORF

5.3) Color by CDS using the corresponding option from the _View_ top-level menu

6) 'Zoom to Base Level' to reveal DNA Track and test sequence annotation alterations: 

6.1) Insertions 

6.2) Deletions 

6.3) Substitutions

7) 'Zoom back out', then reveal right-click menu. 

7.1) Test: 

7.1.1) Get Sequence, Get GFF3

7.1.2) Delete, Merge, Split, Duplicate, Make Intron, Move to Opposite Strand.

7.1.3) Set Translation Start, Set Translation End, Set Longest ORF, Set Readthrough Stop Codon.

7.1.4) Set to Downstream Splice Donor, Set to Upstream Splice Donor, Set to Downstream Splice Acceptor, Set to Upstream Splice Acceptor.

7.1.5) Check the _Undo_ and _Redo_ operations

7.1.6) Show _History_ from the right click menu, and test the ability to revert to any of the previous versions of the feature by clicking on the arrow buttons to the right of each version.

7.1.7) Annotation Information Editor: Name, Symbol, DBXRefs, Comments, Gene Ontology IDs, and PubMed IDs.

7.1.8) Use both the genomic feature you are currently annotating and a genomic feature from one of the evidence tracks to modify the exon and UTR boundaries for the annotation in the _User-created Annotations_ area using the following operations from the right-click menu: _Set as 3' end_, _Set as 5' end_, _Set both ends_.

7.1.9) Set exon boundary to create and remove an isoform, and use _History_ to conduct _Undo_ / _Redo_ operations on this isoform.

7.1.10) Change the annotation type from the right-click menu and check _Undo_ / _Redo_ operations on this annotation.

8) Check that the URL can be used for sharing work (on a different browser) for both logged in and logged out (JBrowse only) mode: bring up different browser window and paste the shared URL. Check real-time update by dragging and dropping another exon to the model on the left (same strand); check that “non-canonical boundaries” warning sign appears as appropriate. Last, delete an exon, Redo/Undo to test.  

9) Check that you are able to export data from the _User-created Annotations_ track using the drop down menu option (from the track label) and choosing the 'Save track data' option. Here check both GFF3 (with and without FASTA) and FASTA files (CDS, cDNA, peptide, and highlighted region (note: you must first highlight a region to test this)).


### B) Testing the _Annotator Panel_

10) Check that you can switch between organisms using both the drop-down menu in the upper left corner of the _Annotator Panel_ and the options in the and the _Organism_ tab.

11) Test that you are able to logout by clicking on the 'logout' arrow located in the upper right corner of the _Annotator Panel_. Then, test that you are able to log back in. 

12) Test changing the user's password using the options available when clicking on the user ID button in the upper right corner of the _Annotator Panel_.

13) Test functionality for each of the tabs in the panel

13.1) Annotations

13.1.1) Check that you can navigate to an annotation by clicking on them from the list in the panel. Annotations for gene elements that produce transcripts will require one click on the name of the annotation, then double clicking on the transcript.

13.1.2) After clicking on an annotation, click on the _Details_ tab at the bottom of the _Annotator Panel_ to display metadata for each annotation. Then, click on the _Coding_ tab to reveal a list of exons, and click on one of the exons to reveal optiond to modify its boundary using the arrows in the display. 

13.1.3) Find an annotation using the _Annotation Name_ search box, and use the filters from the drop down menus. 

13.2) Tracks

13.2.1) Check the display of evidence available on all tracks by clicking to "check" and "uncheck" fromthe list of available tracks.

13.2.2) Search for a track using the search box.

13.3) Ref Sequence

13.3.1) Use the search box to find a scaffold / chromosome and navigate to it by double clicking on one of them. 

13.3.2) Test that you can export GFF3, FASTA, and CHADO files for one or more selected scaffolds at a time. 

13.4) Organism

13.4.1) Check that you can alter the metadata or source file for existing organisms, add new organisms, and delete current organisms. 

13.4.2) Test that you can switch between organisms by double clicking on one of them. 

13.5) Users 

13.5.1) Create a new user and grant read, write, and publish permissions.

13.5.2) Test altering information and permissions and group membership for users. 

13.6) Groups

13.6.1) Test that you can add and delete groups, as well as assign users to these groups. 

13.7) Admin

13.7.1) Click and corroborate that each item listed under the _Admin_ tab sends you to a new page. And test that you can add and delete fields and data as needed for each page. 


### C) Testing Integration

14) Test security redirect 

14.1) Test redirect by clicking on a JBrowse (public URL) link and then clicking login.  

14.2) Test through a proxy (nginx, apache, etc.)

14.3) Test copy and pasting a logged in URL (goes to loadLink)

14.4) When logged out and logging back in should come back to the same place.

14.4.1) Should work even if on different browser.



