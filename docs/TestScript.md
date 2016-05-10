# Apollo Testing Script

2016-05-09

M. Munoz-Torres

Note: The following steps are meant for testing purposes only, not for training manual annotators.

## Apollo General Information
- The Apollo website:
http://GenomeArchitect.org
- The article describing Apollo can be found at:  
http://genomebiology.com/2013/14/8/R93/abstract
- The public Apollo honey bee (Apis mellifera) demonstration site is available at: 
http://genomearchitect.org/WebApolloDemo/
- You may find our user guide at:
http://genomearchitect.org/web_apollo_user_guide
- You may find a few slide presentations on the 'How Tos' of Apollo at:
http://www.slideshare.net/MonicaMunozTorres/
- Apollo at GMOD page: 
http://www.gmod.org/wiki/WebApollo 
- Apollo installation and configuration guide
http://genomearchitect.readthedocs.io/en/latest/

If testing the Apollo demo, go to: http://genomearchitect.org/WebApolloDemo/ 


## Testing a Apollo Instance

### Test the “Sequences” screen (formerly Select tracks)

1) Select one scaffold / group (containing annotations) and check that you are able to export GFF3 and FASTA from the File / Export menu option.

2) Check that you are able to query the entire genome using BLAT from this window using the Tools / Search sequence menu option.

3) Check the use of filters (e.g. group/scaffold/chromosome name, lenght) and review pagination and number of results shown.

4) Clicking on group/scaffold/chromosome name link should take you to the corresponding sequence track in the main window.

5) Test that you are able to Log out from the upper-right corner, top-level menu.

6) For Administrators: From top-level menu choose the option Tools -> Manage Users. 

6.1) Create a new user and grant read, write, and publish permissions.

6.2) Logout and log back in as the newly created user, then create new annotations and modify existing ones (if available).

###Testing functions in the main window

7) Check the display of evidence available on all  tracks by using "check" and "uncheck" clicks on the list of available tracks.

8) Drag and drop a gene onto the “User-created Annotations” (U-cA) area.

9) Test top-level menu options in the main window.

9.1) Test functions on each menu option:

9.1.1) Login: login/logout. 

9.1.2) File 

   /Open (Test that data can be loaded locally using URLs (File / Open / Remote URLs)).

   /Add Combination Track (see 16. below)

   /Add Sequence Search Track (perform search test)

9.1.3) Tools (see 9.2)

9.1.4) View: follow menu options to go to "Changes" and "Sequences" (select sequences) page, check the ability to set and clear highlights, show plus/minus strands, show track label, resize quantitative tracks, color by CDS (also tested in 10), and changing the color scheme (dark or light).

9.1.5) Help: All links go to a new screen.

9.2) From “Tools” menu, query genome with BLAT using a sequence: 

E.g: Housekeeping gene Calpain small subunit 1 CPNS1, CAPNS1, CAPN4, CAPNS (UniProt).

>sp|P04632|CPNS1_HUMAN Calpain small subunit 1 MFLVNSFLKGGGGGGGGGGGLGGGLGNVLGGLISGAGGGGGGGGGGGGGGGGGGGGTAMRILGGVISAISEAAAQYNPEPPPPRTHYSNIEANESEEVRQFRRLFAQLAGDDMEVSATELMNILNKVVTRHPDLKTDGFGIDTCRSMVAVMDSDTTGKLGFEEFKYLWNNIKRWQAIYKQFDTDRSGTICSSELPGAFEAAGFHLNEHLYNMIIRRYSDESGNMDFDNFISCLVRLDAMFRAFKSLDKDGTGQIQVNIQEWLQLTMYS

9.3) Clear highlight using the command from the 'View' menu.

10) Search for an indexed gene (e.g. in honey bee demo CSN2_DANRE (it's on Group1.37:152689..155265)) by typing the gene name on the search box. 

11) Zoom in (double click) to inspect last exon (5'-3') of the displayed gene and:

11.1) change intron/exon boundary (dragging)

11.2) check the recalculated ORF

11.3) color by CDS

12) 'Zoom to Base Level' to reveal DNA Track and test sequence annotation alterations: 

12.1) Insertions 

12.2) Deletions 

12.3) Substitutions

13) 'Zoom back out', then reveal right-click menu. 

13.1) Test: 

13.1.1) Get Sequence, Get GFF3

13.1.2) Delete, Merge, Split, Duplicate, Make Intron, Move to Opposite Strand.

13.1.3) Set Translation Start, Set Translation End, Set Longest ORF, Set Readthrough Stop Codon.

13.1.4) Set to Downstream Splice Donor, Set to Upstream Splice Donor, Set to Downstream Splice Acceptor, Set to Upstream Splice Acceptor.

13.1.5) Undo, Redo

13.1.6) Show History, and test the ability to revert to any of the previous versions of the feature by clicking on the arrow buttons to the right of each version.

13.1.7) Annotation Information Editor: Name, Symbol, DBXRefs, Comments, Gene Ontology IDs, and PubMed IDs.

13.2) Use annotation in progress + feature from an evidence track to test: set as 3' end, set as 5' end, set both ends.

14) Check that the URL can be used for sharing work (on a different browser): bring up different browser window and paste the shared URL. Check real-time update by dragging and dropping another exon to the model on the left (same strand); check that “non-canonical boundaries” warning sign appears as appropriate. Last, delete an exon, Redo/Undo to test. 

15) Test Export of User-created Annotations to Chado, GFF3, FASTA

16) Combination tracks: test that arithmetic combination of quantitative tracks is possible by combining tracks using the menu option: File / Add combination track


### Test the “Changes” screen (formerly “Recent Changes”)

17) Check all filters, pagination, and number of results shown.

18) Clicking on group name link should take you to the corresponding group track in the main window.

19) Test all File Menu as was done for the “Sequences” screen.

<!--
### Test Bulk-Update

13) Click on "Changes"  Verify that we can select all / none / displayed and paginate

14) Verify that, if "Status" is enabled, we can update the status for multiple selected.

15) Verify that we can delete multiple selected types.   If a gene is deleted, the sub-features should also be deleted.   Should a gene exist without sub-features?

16) Select features across multiple tracks and confirm above bulk updates.
-->
