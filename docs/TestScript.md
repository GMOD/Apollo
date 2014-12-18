# Web Apollo Testing Script

121814

M. Munoz-Torres

Note: The following steps are meant for testing purposes only, not for training manual annotators.

## Web Apollo General Information
- The Web Apollo website:
http://GenomeArchitect.org
- The article describing Web Apollo can be found at:  
http://genomebiology.com/2013/14/8/R93/abstract
- The public Web Apollo honey bee (Apis mellifera) demonstration site is available at: 
http://genomearchitect.org/WebApolloDemo/
- You may find our user guide at:
http://genomearchitect.org/web_apollo_user_guide
- You may find a few slide presentations on the 'How Tos' of Web Apollo at:
http://www.slideshare.net/MonicaMunozTorres/
- Web Apollo at GMOD page: 
http://www.gmod.org/wiki/WebApollo 
- Web Apollo installation and configuration guide
http://webapollo.readthedocs.org/en/latest/

If testing the Web Apollo demo, go to: http://genomearchitect.org/WebApolloDemo/ 
Login|Password : demo|demo


## Testing a Web Apollo Instance

### Test the “Sequences” screen (formerly Select tracks)

1) Select one scaffold / group (containing annotations) and check that you are able to export GFF3 and FASTA from the File / Export menu option.

2) Check that you are able to query the entire genome using BLAT from this window using the Tools / Search sequence menu option.

3) Check all filters, pagination, and number of results shown.

4) Clicking on group name link should take you to the corresponding group track in the main window.

5) Test top-level menus.

5.1) Test that you are able to Log out from the upper-right corner, top-level menu.

5.2) In top-level menu, go to Tools -> Manage Users. Create a new user and grant read, write and publish permissions. Logout and log back in as the newly created user. Create some new annotations or modify existing ones.

###Testing functions in the main window

6) Check display of tracks by "check" and "uncheck" clicks on list of evidence tracks. Display evidence available on other tracks. 

7) Drag and drop a gene onto the “User-created Annotations” (U-cA) area.

8) Test top-level menu options in the main window.

8.1) Test functions on each menu option:

-- Login: login/logout. 

-- File 

   /Open (Test that data can be loaded locally using URLs (File / Open / Remote URLs)).

   /Add Combination Track (see 10. below)

   /Add Sequence Search Track (perform search test)

-- Tools (see 8.2)

-- View: check options to go to "Changes" and "Sequences" (select sequences) page, check the ability to set and clear highlights, show plus/minus strands, show track label, resize quantitative tracks, color by CDS (also tested in 10), and changing the color scheme (dark or light).

-- Help: All links go to a new screen.

8.2) From “Tools” menu, query genome with BLAT using a sequence: 

E.g: Housekeeping gene Calpain small subunit 1 CPNS1, CAPNS1, CAPN4, CAPNS (UniProt).

>sp|P04632|CPNS1_HUMAN Calpain small subunit 1 MFLVNSFLKGGGGGGGGGGGLGGGLGNVLGGLISGAGGGGGGGGGGGGGGGGGGGGTAMRILGGVISAISEAAAQYNPEPPPPRTHYSNIEANESEEVRQFRRLFAQLAGDDMEVSATELMNILNKVVTRHPDLKTDGFGIDTCRSMVAVMDSDTTGKLGFEEFKYLWNNIKRWQAIYKQFDTDRSGTICSSELPGAFEAAGFHLNEHLYNMIIRRYSDESGNMDFDNFISCLVRLDAMFRAFKSLDKDGTGQIQVNIQEWLQLTMYS

8.3) Clear highlight using the command from the 'View' menu.

9) Search for an indexed gene (e.g. in honey bee demo CSN2_DANRE (it's on Group1.37:152689..155265)) by typing the gene name on the search box. 

10) Zoom in (double click) to inspect last exon (5'-3') of the displayed gene and:

-- change intron/exon boundary (dragging)

-- check the recalculated ORF

-- color by CDS

11) 'Zoom to base level' to reveal DNA Track and test sequence annotation alterations: 

-- Insertions 

-- Deletions 

-- Substitutions

12) 'Zoom back out', then reveal right-click menu. 

13.1) Test: 

-- Get Sequence

-- Delete, merge, split, duplicate, make intron, flip strand, set translation start, set translation end, longest ORF, readthrough. 

-- Undo/Redo

-- History

-- Annotation Info Editor: dbXRefs, Comments, GO IDs, PubMed IDs

13.2) Also, use annotation in progress + feature from an evidence track to test: set as 3' end, set as 5' end, set both ends.

14) Check that the URL can be used for sharing work (on a different browser): bring up different browser window and paste the shared URL. Check real-time update by dragging and dropping another exon to the model on the left (same strand); check that “non-canonical boundaries” warning sign appears as appropriate. Last, delete an exon, Redo/Undo to test. 

15) Test Export of User-created Annotations to Chado, GFF3, FASTA

16) Combination tracks: test that arithmetic combination of quantitative tracks is possible by combining tracks using the menu option: 

File / Add combination track


### Test the “Changes” screen (formerly “Recent Changes”)

17) Check all filters, pagination, and number of results shown.

18) Clicking on group name link should take you to the corresponding group track in the main window.

19) Test all File Menu as in section 3.

<!--
### Test Bulk-Update

13) Click on "Changes"  Verify that we can select all / none / displayed and paginate

14) Verify that, if "Status" is enabled, we can update the status for multiple selected.

15) Verify that we can delete multiple selected types.   If a gene is deleted, the sub-features should also be deleted.   Should a gene exist without sub-features?

16) Select features across multiple tracks and confirm above bulk updates.
-->
