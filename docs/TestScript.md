# Apollo Testing Script

Note: The following steps are meant for testing purposes only, not for training manual annotators.

### A) Testing functions in the main window

1. Switch between organisms:

   - 1.1. Check that you can change organisms from the upper left-corner drop-down menu.

   - 1.2. Check that you can switch organisms from the _Organism_ tab in the _Annotator Panel_

   - 1.3. Check that the organism and sequence preference is preserved only on a single browser tab. Every tab / window opened should allow a different sequence and organism. The last organism + reference sequence (e. g. scaffold, chromosome) should be loaded into each, but each browser tab will be independent.

      - 1.3.1. The last "movement" of any browser for a particular organism should become the new preference (location + scaffold) for that organism.

      - 1.3.2. The last "movement" should become the new "default" organism.

      - 1.3.3. Switching back and forth between sequences should remember the last location for each.

      - 1.3.4. Switching back and forth between organisms should remember the last location (scaffold and location) for each.

   - 1.4. Switch between organisms after creating annotations on this reference sequence (e. g. scaffold, chromosome), if the two reference sequences have the same name in two (or more) different organisms.

      - 1.4.1. Use the JBrowse drop-down or search menu to navigate between the two scaffolds on different organisms at least twice and confirm that annotations that belong to an organism on one scaffold are only seen on that organism and not the other.

      - 1.4.2. Use the _Sequence Panel_ to confirm that scaffolds on a particular organism only belong to that organism. i. e., every scaffold listed should be part of the current organism.

      - 1.4.3. Use the _Sequence Dropdown_ to confirm that scaffolds on a particular organism only belong to that organism.

      - 1.4.4. Use the _Annotator Panel_ "Go To Annotation" button to confirm that scaffolds listed for a particular organism and scaffold only belong to that organism.

2. Test top-level menu options:

   - 2.1. Login / Logout

      - 2.1.1. Test that you are able to logout using the options on the upper right corner of the main window by clicking on your user ID and choosing to 'logout'. Then, test that you are able to log back in.

      - 2.1.2. Test that all browsers log out for a set user.

      - 2.1.3. When logged out you should still be able to view "public" organisms and browse public genomes from the link on the login screen.

   - 2.2. File

      - 2.2.1. /Open (Test that data can be loaded locally using URLs (File / Open / Remote URLs)).

      - 2.2.2. /Add Combination Track: test that the arithmetic combination of quantitative tracks is possible by dragging two of them into a 'combination track'. Test different operations (addition, substraction) and arrangements (left and right positions for each track) as appropriate.

      - 2.2.3. /Add Sequence Search Track and perform a search test.

   - 2.3. View

      - 2.3.1. Check the ability to set and clear highlights, show plus/minus strands, show track label, resize quantitative tracks, color by CDS, and changing the color scheme (dark, light, grid, no grid).

   - 2.4. Search Panel

      - 2.4.1. Confirm that an organism with a blatdb define displays the search panel and that an organism does not is hidden.

         - 2.4.1.1. Confirm that on changing organisms the Search Panel is only shown when blatdb is defined.

         - 2.4.1.2. Confirm that on reloading organisms Search Panel is only shown in some instances.

      - 2.4.2. For Honeybee, click on the Search panel and query the genome with BLAT, using an amino acid or nucleotide sequence. For example: Housekeeping gene Calpain small subunit 1 CPNS1, CAPNS1, CAPN4, CAPNS (UniProt).
```
>sp|P04632|CPNS1_HUMAN Calpain small subunit 1 MFLVNSFLKGGGGGGGGGGGLGGGLGNVLGGLISGAGGGGGGGGGGGGGGGGGGGGTAMRILGGVISAISEAAAQYNPEPPPPRTHYSNIEANESEEVRQFRRLFAQLAGDDMEVSATELMNILNKVVTRHPDLKTDGFGIDTCRSMVAVMDSDTTGKLGFEEFKYLWNNIKRWQAIYKQFDTDRSGTICSSELPGAFEAAGFHLNEHLYNMIIRRYSDESGNMDFDNFISCLVRLDAMFRAFKSLDKDGTGQIQVNIQEWLQLTMYS
```
         - 2.4.2.1. Click on multiple hits and confirm that it navigates to those hits.

         - 2.4.2.2. Click on `Create annotation from hit` and confirm that an annotation is creating with the same dimensions and the same strand.

         - 2.4.2.3. Open the genomic annotation sequence via the popup using nucleotide sequences. Click on `Search sequences` and confirm that the sequences are loaded into the search panel. Click on search sequence and confirm that hits are generated and repeat the steps in 2.4.2.

         - 2.4.2.4. Repeat 2.4.2 with a genomic annotation using nucleotide sequences. In this example, view the sequences that form that annotation and click on `Search sequences`.



   - 2.5. Help

      - Check that all links go to a new screen.


3. Test the Navigation Bar

   - 3.1. Search for an indexed gene (e. g. in honey bee demo CSN2_DANRE (it's on Group1.37:152689..155265.) by typing the gene name on the search box in the middle of the navigation bar in the main window.

4. Drag and drop a gene onto the “User-created Annotations” (U-cA) area.

   - 4.1. Drag and drop an HTML BAM read to the “User-created Annotations” (U-cA) area.

   - 4.2. Drag and drop an HTML BAM split-read to the “User-created Annotations” (U-cA) area (in volvox data under HTML Alignment).

   - 4.3. Right-click and create a new gene using a Canvas BAM split-read to the “User-created Annotations” (U-cA) area (in volvox data under Canvas Alignment).

5. Zoom in (double click) to inspect last exon (5'-3') of the displayed gene and:

   - 5.1. Change intron / exon boundary (dragging)

   - 5.2. Check the recalculated ORF

   - 5.3. Color by CDS using the corresponding option from the _View_ top-level menu

6. 'Zoom to Base Level' to reveal DNA Track and test sequence annotation alterations:

   - 6.1. Insertions

   - 6.2. Deletions

   - 6.3. Substitutions

7. 'Zoom back out', then reveal right-click menu.

   - 7.1. Test:

      - 7.1.1. Get Sequence, Get GFF3

      - 7.1.2. Delete, Merge, Split, Duplicate, Make Intron, Move to Opposite Strand.

      - 7.1.3. Set Translation Start, Set Translation End, Set Longest ORF, Set Readthrough Stop Codon.

      - 7.1.4. Set to Downstream Splice Donor, Set to Upstream Splice Donor, Set to Downstream Splice Acceptor, Set to Upstream Splice Acceptor.

      - 7.1.5. Check the _Undo_ and _Redo_ operations

      - 7.1.6. Show _History_ from the right click menu, and test the ability to revert to any of the previous versions of the feature by clicking on the arrow buttons to the right of each version.

      - 7.1.7. Annotation Editor popup removed, but selecting "Edit Annotation should work".

      - 7.1.8. Use both the genomic feature you are currently annotating and a genomic feature from one of the evidence tracks to modify the exon and UTR boundaries for the annotation in the _User-created Annotations_ area using the following operations from the right-click menu: _Set as 3' end_, _Set as 5' end_, _Set both ends_.

      - 7.1.9. Set exon boundary to create and remove an isoform, and use _History_ to conduct _Undo_ / _Redo_ operations on this isoform.

      - 7.1.10. Change the annotation type from the right-click menu and check _Undo_ / _Redo_ operations on this annotation.

      - 7.1.11. Create two isoforms for a single gene. Right-click on one of the isoforms and select "Disassociate Transcript from Gene" and verify that the transcript is now part of its own gene.
Select both isoforms and select "Associate Transcript with Gene". Verify that the gene now belongs to both isoforms.

8. Check that the URL can be used for sharing work (on a different browser) for both logged in and logged out (JBrowse only) mode: bring up different browser window and paste the shared URL. Check real-time update by dragging and dropping another exon to the model on the left (same strand); check that “non-canonical boundaries” warning sign appears as appropriate. Last, delete an exon, Redo/Undo to test.

9. Check that you are able to export data from the _User-created Annotations_ track using the drop down menu option (from the track label) and choosing the 'Save track data' option. Here check both GFF3 (with and without FASTA) and FASTA files (CDS, cDNA, peptide, and highlighted region (note: you must first highlight a region to test this)).

### B) Testing the _Annotator Panel_

10. Check that you can switch between organisms using both the drop-down menu in the upper left corner of the _Annotator Panel_ and the options in the and the _Organism_ tab.

11. Test that you are able to logout by clicking on the 'logout' arrow located in the upper right corner of the _Annotator Panel_. Then, test that you are able to log back in.

12. Test changing the user's password using the options available when clicking on the user ID button in the upper right corner of the _Annotator Panel_.

13. Test functionality for each of the tabs in the panel

   - 13.1. Annotations

      - 13.1.1. Check that you can navigate to an annotation by clicking on them from the list in the panel. Annotations for gene elements that produce transcripts will require one click on the name of the annotation, then double clicking on the transcript.

      - 13.1.2. Navigate To Annotation Details

         - 13.1.2.1. After clicking on an annotation, click on the _Details_ tab at the bottom of the _Annotator Panel_ to display metadata for each annotation. Then, click on the _Coding_ tab to reveal a list of exons, and click on one of the exons to reveal options to modify its
boundary using the arrows in the display. Modify a number explicitly and click outside the box. Confirm that a change was made for a legitimate value.

         - 13.1.2.2. Repeat for pseudogenes and non-coding RNAs.

         - 13.1.2.3. Reveal the _Details_ for Repeat Region and Transposable Element to display metadate for each annotation and demonstrate editing details.

      - 13.1.3. Find an annotation using the _Annotation Name_ search box, and use the filters from the drop down menus.

      - 13.1.4. Test that you can delete an annotation from the Details menu.

      - 13.1.5. Set Defaults First in Admin: Create Canned Comments, Canned Keys, Canned Values, Suggested Names, and Statuses for certain organisms and feature types. Verify add, edit, and delete for each.

         - 13.1.1. Details: Name, Symbol, DBXRefs / PubMed IDs, Alias, and Status. For a gene verify that Suggested Names show up and that multiple aliases can be added and changed from 1 to 2 and 2 to 1.

         - 13.1.2. Details: Name, Symbol, DBXRefs / PubMed IDs, Alias, and Status. Verify that same thing for the transcript on the gene, and the other main feature types (e.,g,. Terminator, non-coding RNA, Pseudogene).

         - 13.1.3. Test with GO, Gene Product, and Provenance. Should be able to edit, delete, and add. Both ECO and default evidence should be available.

         - 13.1.4. Test with DbXref, Comment, and Attributes. Should be able to edit, delete, and add. Both ECO and default evidence should be available. Verify that the defaults are there from earlier step and respect organism and feature type boundaries.

         - 13.1.5. Verify that the right-clicking GFF3 matches the annotations just added. Verify that you can export the GFF3 (you will need that for later).

         - 13.1.6. Verify that "GO" and "ID" work appropriate.

         - 13.1.7. Right-clicking this feature, should show this.

         - 13.1.8. Clicking on "Show Visible" and "Show All" should also work.

         - 13.1.9. Check that you can filter GO Annotations by clicking the checkbox at the top.


   - 13.2. Tracks

      - 13.2.1. Check the display of evidence available on all tracks by clicking to "check" and "uncheck" from the list of available tracks.

      - 13.2.2. Search for a track using the search box.

      - 13.2.3. Check that clicking on the show JBrowse tracks selector icons properly toggles the JBrowse tracks.

         - 13.2.3.1. Click on the track panel and confirm that selecting and unselecting the JBrowse track view and the main panel toggle icon.

         - 13.2.3.2. Click on the main panel toggle button and confirm that doing and undoing the toggle switch toggles the JBrowse track view and switches the track panel toggle as well.

         - 13.2.3.3. Confirm that reload in either case saves the prior preference.

         - 13.2.3.4. Test as Admin and non-Admin for one case to confirm layout.

         - 13.2.3.5. Test a set of track categories can handle opening and closing, searching, and select / unselect all.

      - 13.2.4. Check that as administrator that you can both add and remove multiple track types via the upload and that removed track contents are deleted.

   - 13.3. Ref Sequence

      - 13.3.1. Use the search box to find a scaffold / chromosome and navigate to it by double clicking on one of them.

      - 13.3.2. Test that you can export GFF3, FASTA, and CHADO files for one or more selected scaffolds at a time.

      - 13.3.3. Test that you can delete all annotations from 2 or more sequences.

      - 13.3.4. Check that you export GO Annotations as a GPAD 2 file.

   - 13.4. Organism

      - 13.4.1. Check that you can alter the metadata or source file for existing organisms, add new organisms, and delete current organisms.

      - 13.4.2. Test that you can switch between organisms by double clicking on one of them.

      - 13.4.3. Test that you duplicate an organism and obsolete the duplicate and that the show obsolete works correctly.

      - 13.4.4. Delete the duplicated organism

      - 13.4.5. Check that you can add an organism by uploading a FASTA file and add / remove tracks to that organism

      - 13.4.6. Check that when removing a temporary organism that all associated files are also removed.

   - 13.5. Users

      - 13.5.1. Create a new user and grant read, write, and publish permissions.

      - 13.5.2. Test altering information and permissions and group membership for users.

      - 13.5.3. Make a user 'inactive' and confirm that they are listed as such and they have no permissions and they only show up when "Show inactive" is selected.

      - 13.5.3. Make an 'inactive' user active and assign them permissions.


   - 13.6. Groups

      - 13.6.1. Test that you can add and delete groups, as well as assign users to these groups.

   - 13.7. Admin

      - 13.7.1. Click and corroborate that each item listed under the _Admin_ tab sends you to a new page. And test that you can add and delete fields and data as needed for each page.

   - 13.8. Public link

      - 13.8.1. Confirm that clicking on a public link option in the main window when _logged in_ will forward you to the Annotator Panel view.

      - 13.8.2. Confirm that clicking on a public link option in the main window when _logged out_ will give you the regular "JBrowse view".

         - Check that this link opens up a new window showing only the browser and JBrowse track menu, without the annotator panel.
         - When logging in, it should redirect to another login menu and then finally end up at the _Annotator Panel_ view with the same tracks selected.

   - 13.9. Logged in link

      - 13.9.1. Confirm that clicking on a logged in link option in the main window when _logged in_ will forward you to the _Annotator Panel_ view directly.

      - 13.9.2. Confirm that clicking on a logged in link option in the main window when _logged out_ will take you to the login screen and then redirect you to the proper _Annotator Panel_ view when approved with the same selections in-tact.

### C) Testing Security Linking

14. Test security redirect

   - 14.1. Test redirect by clicking on a JBrowse (public URL) link and then clicking login.

   - 14.2. Test through a proxy (nginx, apache, etc.)

   - 14.3. Test copy and pasting a logged in URL (goes to loadLink)

   - 14.4. When logged out and logging back in should come back to the same place.

      - 14.4.1. Should work even if on different browser.

### D) Testing Web Services

   - 15.1. Make sure that you can add organisms using the ```add_organism. groovy``` script. ```groovy add_organism. groovy -directory /apollo_data_directory/web_apollo_demo_yeast -genus bread -species yeast -name SampleOrganism -url http://testserver. gov/Apollo-staging -username adminuser@admin. gov -password demo -public```

      - 15.1.1. Verify that it also works for the shell (curl) script as well.

   - 15.2. Verify that you can use the ```add_users. groovy``` script. ```groovy add_users. groovy -newuser "SampleUser" -username adminuser@admin. gov -password adminpassword -destinationurl http://testserver. gov/Apollo-staging -newrole admin```

      - 15.2.1. Verify that it also works for the shell (curl) script as well.

   - 15.3. Make sure that you can add annotations using the ```add_features_from_gff3_to_annotations. pl``` script from exported annotations.

      - 15.3.1. Create annotations for new user on new organism.

      - 15.3.2. Download annotations as GFF3.

      - 15.3.3. Run ```add_features_from_gff3_to_annotations. pl``` against the same organism and user and confirm that it works. ```./add_features_from_gff3_to_annotations. pl -U http://testserver. gov/Apollo-staging -u adminuser@admin. gov -p adminpassword -i Annotations-chrI. gff3 --organism SampleOrganism```

      - 15.3.4. Run ```delete_annotations_from_organism. groovy``` and confirm that annotations from this organism have been removed. `groovy delete_annotations_from_organism. groovy -adminusername adminuser@admin. gov -adminpassword adminpassword -destinationurl http://testserver. gov/Apollo-staging -organismname SampleOrganism`


### E) Testing Variation Annotation

16. Go to a Human or Volvox organism (with variant data, preferably HTMLVariant and CanvasVariant tracks).

   - 16.1. Right-click on a HTML Annotation and "Create Annotation" and confirm that the same SNV has been added graphically.

      - 16.1.2. Confirm that you can drag up an HTML Variant Annotation.

      - 16.1.3. Confirm that you can create a Canvas Variant Annotation by right-clicking on it and click on Annotation.

   - 16.2. On any of the annotations, option-click (or right-click and select "Edit Information") and observe that it is selected and the details show for the variant in the Annotator Panel.


   - 16.3. Right-click on variant annotation and confirm that only that annotation appears in the Annotator Panel

      - 16.3.1. Observe that the tabs are shown in the details screen: Details, Alternate Alleles, Variant Info, Allele Info, DbXref, Comments, and Attributes.

      - 16.3.2. In the Details tab confirm that changes to Name and Description show up and match what is shown in the graphical editor and that you can change everything not grayed out.

      - 16.3.3. In the Variant Info tab confirm that the added info appears.

         - 16.3.3.1. Confirm that you can add and delete Variant Info.

      - 16.3.4. In the Allele Info tab confirm that the added info appears.

         - 16.3.4.1. Confirm that you can add and delete Allele Info.

   - 16.4. In Annotator Panel search switch between Variant and others and confirm that only variants shows up.

   - 16.5. Verify that you can zoom out and still see the variant info.

   - 16.6. On the Sequence Tab verify that the annotation shows up.

      - 16.6.1. Verify that you can click on the chromosome and export it as a valid VCF.

      - 16.6.2. Verify that the VCF contains all the variants and each variant has all the properties.

   - 16.7. Verify variant effect code.

      - 16.7.1. Click on a variant annotation deletion or insertion in a coding region and click on "View Variant Effect" and verify that an effect on the CDS is rendered (human BRCA1 works well for this).

      - 16.7.2. Click on generated sequence alteration and verify that the comment is "Effect of XXXX" where XXXX is the variant effecdt type.

      - 16.7.3. Click on "Remove Variant Effect" from the variant and verify that the sequence alteration and effect on the CDS are both removed.

      - 16.7.4. Repeat adding variant effects but do this for multiple variants on the same gene.

      - 16.7.5. Click on the effected gene and click on "Remove variant effects" and confirm that all alterations are removed from within the effected area.

### F) Test Docker load

   - 17.0. Run against docker built on Docker hub

   - 17.1. Load docker with a sample files directory and database:

    docker run -it -v /jbrowse/root/directory/:/data -v /postgres/data/directory:/var/lib/postgresql -p 8888:8080 gmod/apollo:latest

   - 17.2. Confirm that we can load a genome and tracks from the existing directory (e. g,. Honeybee)

      - 17.2.1. Confirm that we can create an annotation.

   - 17.3. Confirm that we can load a new genome with an existing fasta file (volvox. fa in the sample data)

      - 17.3.1. Confirm that we can add a new track to that genome with an existing fasta file (volvox. gff in the sample data)

      - 17.3.2. Confirm that we can create an annotation with the new track.

      - 17.3.3. Confirm that we can do a search against that annotation with the new track.

      - 17.3.4. Confirm that we can create an annotation with that search result.
