## 2.0.3

Features

+ Added Ability to export Apollo annotations to a Chado database (#145).
+ Added ability to update an existing Chado database for Chado export (#967).
+ Updated application to use Grails 2.5.4 (#444).
+ Allow apollo-config.groovy configuration to create admin user on startup (#1040).
+ Added ability to change feature type in User created annotations track (#220).
+ Added ability to use multiple organisms at the same time (#441).
+ Added ability to search selected sequences (for export) and clear selection in Sequence Panel (#730).
+ Added ability to allow username to be a non-email based name (#939).
+ Sync with JBrowse 1.12.2-release for stability (#971).

Bugfixes

+ Fixed a bug were set translation start, in an intron, produces an uncaught out of bounds exception (#532).
+ Remove alternate hover CSS on tables in Annotator Panel for better visibility of entries (#632).
+ Fixed clientToken not found error for operation `get_gff3` and `get_sequence` (#1027).
+ Fixed a bug where PubMed and Gene Ontology lookup, in Information Editor, fails (#1028).
+ Fixed issues with export sequence API (#1045).
+ Fixed a bug where changing the number of transcripts of a gene did not update the drop-down in Information Editor (#587).
+ Fixed a bug where the bookmark icon did not show up for the current feature in History window upon revert (#769).
+ Fixed a bug where CDS FASTA export was attempting to export sequences of ncRNAs. (#833).
+ Fixed a bug where using `?organism=organismName` does not work if logged in (#845).
+ Fixed a bug where if a mRNA, without a strand, is added to the annotations track then it cannot be assigned a strand (#873).
+ Fixed a bug where import generates unique 'gene' name based on the existing gene (#879).
+ Fixed a bug where Comments field was restricted to 256 characters (#963).
+ Fixed a bug where an undo operation on a pseudogene causes an error (#1001).
+ Fixed inconsistencies when converting a feature to a JSONObject and vice-versa (#1003).
+ Fixed clientToken error originating from `getCurrentOrganismForCurrentUser` (#1054).
+ Fixed UI problems with 'Full-screen view' mode (#1055).
+ Fixed a bug where `targetURL` was not preserved properly through login in loadLink and Reports page (#1058).
+ Fixed a bug where user/group selection drop-down goes the wrong direction (#1066).
+ Fixed a bug where creating a pseudogene, repeat region or transposable element from a Canvas feature track led to an error (#1077).

## 2.0.2

Features

+ Added ability to filter summary of annotations by user (#79).
+ Re-added translation table support (#757).
+ Improved speed of GFF3 export via optimization (#274).
+ Enhanced GFF3 export of sequence alterations by including the altered residues (#754).
+ Added warning for Tomcat memory settings when doing apollo deploy (#767).
+ Replaced the Split Panel interface of Annotator with Dock Panel (#768).
+ Added label to the 'Revert' button in History window (#769).
+ Added ability to sort annotations by 'Last Updated' on the Annotator Panel (#770).
+ Added ability to add a comment to sequence alterations (#781).
+ Added annotation count to the Sequences Panel (#803).
+ Optimized pagination in Annotator Panel when dealing with large number of annotations (#820).
+ Enhanced the web service API to enable the ability to edit name of features (#776).
+ Enhanced the web service API to enable the ability to add GO, Dbxref, attributes and publications to features (#829).
+ Improved the speed of FASTA export via optimization (#854).
+ Updated history window to indicate current position (#797).

Bugfixes

+ Fixed a bug where sequence modifications weren't being included in the GFF3 export (#748).
+ Provide alternate translation table support on the client (#759).
+ Fixed a bug that causes a persistent screen over 'Links to this Location' popup (#778).
+ Fixed a bug that made it unable to retrieve GFF3 of pseudogenes (#784).
+ Fixed a bug where merging or splitting transcripts and then doing an undo causes error (#842).
+ Fixed a bug which causes links to transcript on the second page of the Annotator Panel to fail (#801).
+ Fixed a bug which causes filter and sort to be mutually exclusive in Changes report page (#824).
+ Fixed a bug where repeat regions, transposable elements and sequence alterations were not part of GFF3 export (#836).
+ Fixed a bug where merging of transcripts fail in certain cases (#850).
+ Fixed a bug that was as a result of using native javascript confirm boxes (#851).
+ Fixed a bug where removing a dbxref visually removes the feature from track (#764).
+ Fixed a bug which causes all genomic elements to disappear when 'Show minus strand' option was selected (#782).
+ Fixed a bug where modification of a gene model was taking a long time when the backend was MySQL (#743).

## 2.0.1

Features

+ Added more intuitive paging to sidebar panels (#700).
+ Added support for flatfile-to-json tracks loaded with the `--compress` option in the data loading pipeline (#517).
+ Added right-click option to CanvasFeatures tracks to "Create new annotation" (alternative to drag and drop) (#576).
+ Added an option for enabling/disabling the public JBrowse mode (#433).
+ Added an `-ignoressl` option to bypass certificate authority for groovy command line scripts (#557).
+ Made the administration of group membership more clear from the User/Group panels (#598).
+ Added scripts and web services for managing user and group permissions (#595).
+ Added additional Web Services API documentation generated from source code annotations (#582).
+ Added ability to toggle the JBrowse tracklist from the annotator panel (#597).
+ Added ability to collapse HTMLFeatures evidence tracks from track menu (#571).
+ Added case insensitive search to annotator panel search boxes (#575).
+ Added some bootstrap styling to annotator panel features (#489).
+ Added a feature to remember the width of the annotator panel (#591).
+ Added scripts and web services for deleting all features from a given organism (#539).
+ Added proxy support to support https servers and general user-configurable proxy support (#148).
+ Improved user experience for login, user, and group pages (#603, #601, #592).
+ Improved, expanded, and automated web services documentation (#546).
+ Added feature to let users change their own password (#620).
+ Updated the /featureEvent/changes page to show list of recently changed features (#642).
+ Enhanced the loadGroups and loadUsers API to retrieve the info of specific groups or users (#643).
+ Enhanced the findAllOrganisms API to retrieve the info of specific organisms (#666).
+ Added ability to reference the organism by name in the jbrowse URL for easier to remember URL formats (#653).
+ Added a get_fasta.groovy script to fetch FASTA for annotations via web services.

Bugfixes

+ Fixed the permissions to only allow the global admin role to create and delete organisms (#542).
+ Fixed an issue with JBrowse compatibility for certain refSeqs.json files not containing length.
+ Fixed the calculation of isoform overlap (#558).
+ Fixed a bug that made certain annotation operations slow down over time (#555).
+ Fixed a bug that made changing the location of the organisms's data directory cause problems (#567).
+ Fixed a bug that occured when splitting and merging a transcript back together again (#588).
+ Fixed a bug that prevented multiple values for an attribute in the Information Editor (#579). 
+ Fixed a bug preventing features with long names (#580).
+ Fixed a bug where a closed track in the genome browser was not showing up as un-checked in the side-panel (#554). 
+ Fixed a bug where the group permissions where not being displayed correctly on the Group panel (#664).


## 2.0.0

Bugfixes

+ Organism panel not showing all organisms (#540).
+ Admins for specific organisms have issues with giving other users permissions (#542).

## 2.0.0-RC6

Bugfixes

+ Fixed multiple bugs having to do with sequence alterations (#534, #531, #458, #456).
+ Fixed logout for multiple windows on the same browser (#480).
+ JBrowse only mode not listening to websockets (#537).  


## 2.0.0-RC5

Features

+ Optimized transcript merging (#529,#515).

Bugfixes

+ History operations fail when setting acceptor / donor (#530). 

## 2.0.0-RC4
Features

+ Optimized transcript merging (#529).
+ Added "add_comment", "add_attribute", "set_status" to the web services API (#492). 
+ Add an interim export panel (#78).
+ Added google analytics integration (#146).

Bugfixes

+ User's last location isn't preserved on page on page refresh (#522).
+ Added security to report pages (#513).
+ Unble to add / view more than 4 organism permissions (#512).
+ Set current sequence dropdown not selecting sequence (#511).
+ Peptide sequence not exporting properly (#453).
+ Peptide sequence not exporting properly (#453).


## 2.0.0-RC3

Features

+ Added CSV downloads of reports, and created more extensible framework for creating customizable reports.
+ Updated application to use Grails 2.4.5.
+ Updated bulk loader to support loading for a specific organism with the --organism parameter (See #505).
+ Updated bulk loader to support looking up the name of a feature from a specific GFF3 tag with the --name_attributes. Thanks to @anaome for the idea and implementation (See #396).
+ Raise limit on number of tracks allowed in track panel (#502).
+ Added some database optimizations for retrieving sequence features (#504, #452).
+ Optimized annotation panel (#389).
+ UI improvements (#385).
+ Add compression to gzip / fasta (#252).
+ Add stress testing frameworks (#137).

Bugfixes

+ Fixed bug that prevented deleting of certain isoforms after database optimizations were applied in RC2 (#497).
+ Moving to opposite strand was not recalculating the ORF (#468).
+ Secure GFF3 / FASTA export (#464, #467).
+ Unable to add organisms from some users (#463).
+ Fixed bug when not stopping (#448).
+ Readthrough stop codons are not being highlighted after undo/redo (#400).



## 2.0.0-RC2

Features

+ Added a PDF version of documentation to [readthedocs](http://genomearchitect.readthedocs.io/).
+ Added a button to generate a public link to the genome browser for a particular organism.
+ Added ability to manage statuses, custom comments, and feature types using the "Admin panel".
+ Added a feature to logout all instances of webapollo from different windows if one window is logged out (#409).
+ Added stress testing scripts using JMeter and tested application reliability.
+ Added new report pages for getting overview of annotations, organisms, sequences, users, and system performance statistics.
+ Added asynchronous data provider to the "Annotator panel" for faster "on-demand" download of data.
+ Added customizable data adapter configurations in the configuration guide.
+ Added gzip functionality to data downloads (#252).
+ Added command line exporter for GFF3.

Bugfixes

+ Fixed small bug with permission checking on creating new organism permissions (#463).
+ Fixed bug with stop codons being retained in peptide sequence exports (#448).
+ Fixed bug with stop codon readthrough features not being restored after "Undo/Redo" (#400).
+ Fixed a bug with downloading data files (#464).
+ Fixed a bug that prevented running multiple instances of Apollo at the same time (#462).
+ Fixed a bug where tracks without a key would cause track panel to produce error (#461).

## 2.0.0-RC1

Features

+ Created a major rewrite of the backend using [Grails](https://grails.org/), which is a scalable, high-concurrency framework based on Spring and Hibernate
+ Added ability to support multiple organisms in a single application instance.
+ Added organism-level permissions for users and groups and created new admin panel for setting these permissions.
+ Added webservices and command-line scripts for creating new organisms (#360).
+ Added webservices and command line scripts for adding users and [support for migrating annotations from WA1 to WA2](https://github.com/GMOD/Apollo/blob/master/docs/Migration.md) (#255).
+ Created a new "Annotator panel", a side-bar for viewing annotations, reference sequences, export options, and admin features.
+ Added ability to load Apollo client-side plugin automatically, so there is no need to run add-webapollo-plugin.pl (#435).
+ Implemented new data models for Hibernate with support for MySQL, PostgreSQL, Oracle, and H2.
+ Implemented simplified configuration via apollo-config.groovy.
+ Added websocket implementation for annotation updates, with long-polling fallback (#14).
+ Optimized non-canonical splice site search (#454).
+ Updated undo/redo operations to work in WA2.0 and fixed several issues with undoing merged transcript operations (#356).


Bugfixes

+ Fixed several bugs with sequence alternations (#442, #447, 428, #420).
+ Fixed bug with in-frame stop codons not being identified after manually setting translation start (#55).
+ Fixed bug with exon identified as having non-canonical splice site with non-existent boundaries (#16).
+ Fix merge of unlike annotation types, causes mixed subfeatures (#23).
+ Fixed bug with where CDS calculation was triggered on non-coding features (#30).
+ Extension of mRNA causes 500 error (#27).


## 1.0.4 release

Features

+ Update to JBrowse 1.11.6 (http://jbrowse.org/jbrowse-1-11-6/)
+ Added new Help page with Web Apollo specific content (#153).
+ Added Drupal authentication module to share authentication with an existing Drupal DB (#117).
+ Made "Show track labels" and "Color by CDS" more persistent (#120).
+ Added a "Collapse" option to the "User-created Annotations" track. The track labels are automatically removed when selecting to "Collapse" the track, but can be shown again. (#155).
+ Changed maxHeight on "User-created Annotations" track to prevent overflow (#124).
+ Allow single-level features to be dragged to the "User-created Annotations" track for editing (#193).

Bugfixes

+ Updated URL to new server to access Gene Ontology terms (GOLR) (#190).
+ Fixed an issue where the API could be used to create random berkeley DBs (#152).
+ Fixed the sample log4j2 implementation and added extra notes on it to the documentation (#151).
+ Fixed an issue where the config files were readable by the outside world in previous 1.x versions.
+ Changed default user database to be encrypted. Unencrypted options are still available via command line scripts for people with older configurations (#147).
+ Fixed bug where Tomcat could report "Too many open files error" (#162).
+ Fixed bug where the dark theme made the reference sequence too dark (#119).
+ Added some basic help text for search parameters in the sequences view (#160).
+ Added MIME types for bigwig files (#166).
+ Added labels to the boxes in the Sequence Search page to inform user of acceptable query options (#158).
+ Fixed GFF3 export to update fileds with non-specified phase, score, and strand (#177).
+ Fixed "Previous" button on sequence page to update datagrid appropriately (#176).
+ Fixed "Show track labels" feature that was causing feature labels to go offscreen (#179).
+ Renamed "Edit Annotation" menu item to "Edit Information" and camel-case file-menu options. (#172)
+ Fixed mislabeled column in "Changes" page (#169). 
+ Fixed "Add sequence search track" function not matching amino acid queries (#168).
+ Fixed bad layout on "Changes" page (#180).
+ Fixed plus/minus strand filters making bigwig score go to zero (#181).
+ Fixed problem encountered with using iframe embedded mode options (#183).
+ Fixed problem with "Add user" popup using outdated server configuration (#182).
+ Fixed minor issue raised when attempting to retrieve (non-existent) peptides from untranslated regions (#157).

## 1.0.3 release

Features 

+ Added ability to view GFF3 for individual annotations

Bugfixes

+ Speed up set\_track\_permissions.pl (#118)
+ Fix some cases where error reporting was broken on login pages (#111)
+ Fix Chado export case where DBXref or DB were not pre-existing (#103)
+ Fixed issue where HTTP header size could become large when exporting all tracks (#101)
+ Fixed issue when jbrowse "bin" directory not created properly during deployment (#97)
+ Added apollo "release" target to build a precompiled target (#96)
+ Provided support to visualize GFF3 files on a per-feature basis (#89)
+ Fixed URL encoding of multiple attributes with the same key in GFF3 export (#82)
+ Fixed GFF3 and FASTA export where no annotations existed (#62)
+ Fixed where genome insertion trigers recalculate CDS on non-coding features (#30)

## 1.0.2 release 

Features: 

+ Using JBrowse 1.11.5
+ Make subfeature unresizable after it becomes unselected
+ Added functionality for extending to downstream/upstream acceptor/donor splice sites
+ Added EncryptedLocalDbUserAuthentication option which allows use of encrypted database but does not require it
+ Added support for EncryptedLocalDbUserAuthentication in add_user.pl,change_user_password.pl
+ Added ability to encrypt an unencrypted  database via encrypt_passwords.pl
+ Added "revert" buttons on rows in history to allow one click change of state
+ Added "{" and "}" for navigating between top level features
+ Added feature coordinates for get_sequence output
+ Added "disableJBrowseMode" option to disable JBrowse mode
+ Added a forceRedirect option for logins that can be used for custom login classes
+ Added tool to split all isoforms into individual genes (RemoveIsoforms.java)
+ Added tool for fixing gene boundaries based on children transcripts (FixGeneBoundaries.java)
+ Using javascript minimization for JBrowse+Apollo to allow faster initial load time
+ Using Maven build system for deployment ([See new installation guide] (https://github.com/GMOD/Apollo/blob/master/docs/index.md))
+ Integrate with TravisCI: https://travis-ci.org/GMOD/Apollo
+ Added menus for changes / sequences to annotation screen, fixing memory issues from selectTrack.jsp and recentChanges.jsp pages.
+ Added ability to hide track labels under "view".
+ alt/Option click brings up Information Editor on annotated track.
+ Added webservices doc links to interface.
+ Added command line exporters for GFF3 files.
+ Added light / dark color schemes.

Bugfixes:

+ Fixed phase in GFF3 output
+ Disabled scrollToPreviousEdge/scrollToNextEdge if the feature is fully visible at the current zoom level
+ Included Chado libraries that were missing in 2014-04-03 release
+ Removed 'null' tracks from changes.
+ Menus use proper CSS pointer.


## 2014-04-03 release

Features:

+ Using JBrowse 1.11.3
- Removed "Information" from context menu
+ Renamed "Annotation Info Editor" to "Information Editor" and added date created and date last modified fields
+ Added tooltips when hovering over annotations in annotation track (displays type, owner, date of most recent modification)
+ Changed the behavior when dragging discontiguous features into annotation track from creating separate features to creating a single feature
+ Added functionality for setting end of translation
+ Added the ability to switch between isoforms when editing meta data in the annotation info editor
+ Enabled floating arrow heads appear to show orientation of feature when zoomed in too much to see the whole feature
+ Added <export_source_genomic_sequence> option for whether to export the underlying source genomic sequence data for GFF3 adapter
+ Search results from reference sequence selection screen how opens up JBrowse with the region highlighted (the same behavior as when searching from within JBrowse itself)
+ Search result window within JBrowse now doesn't automatically close when selecting a result (requires explicit closing from the user)
+ Deleting all subfeatures now pops up a warning about deleting the whole feature not being undoable, like when selecting the parent feature and deleting that
+ History page (recentChanges.jsp)
+ Added the ability to export metadata in in deflines for FASTA exporting
+ Can now continue dragging an exon boundary after initial drag (used to require reclicking on the exon)
+ FASTA output in GFF3 now contains at most 60 residues per line
+ Can now directly set an annotation to a specific state from the history display
+ Added / updated filters for changes and sequences to be more memory efficient and added added filters.

Bugfixes:

+ Fixed bug of not applying timestamp to temporary BLAT searches (also now requires unique token to guarantee uniqueness)
+ Fixed building of feature_relationship pointers in hybrid data store
+ Long polling now tries to reconnect when connection returns a status of 0 (up to five times, at increasing timeouts)
+ Fixed coloring by CDS bug
+ Fixed showing DNA residues on annotation when zoomed in enough
+ Create DB entry on Chado if it doesn't already exist when writing data to Chado
+ Fixed issue with null time last modified meta data
+ Fixed issue with null owner meta data
+ Chado writer will now throw an exception if an expected entity isn't found
+ Fixed 1-off error in BLAT searches for the end of the match
+ Added fixes for properly setting name for non mRNA features
+ Fixed issue where single level features sometimes disappeared while scrolling
+ Fix error adding transposable_element annotation with only a match and no match_part
+ Fix off-by-one error in blat highlight
+ Fixed empty block that hides tracks on top when zooming in to base level then zooming out while not logged in
+ In sequences, data adapters will now not appear in the Export submenu if the current user does not have sufficient privileges to use the adapter (assumes same privileges through all genomic regions for the user)



## 2013-11-22 release

Features:

+ using JBrowse 1.10.9 release
+ new hybrid store (memory/disk) should use much less memory than the pure memory store (with a small degrade in performance) - useful for genomes with many annotations (you can configure which one to use as best fits your needs)
+ viewing of annotation info editor for users without write privilege (cannot modify the data)
+ FASTA data adapter
+ different annotation info editor configurations for different annotation types
+ database/history merger tool (command line)
+ data adapters now use iterators when exporting data (improves memory footprint)
+ add a configurable option for dumping owner and other meta-data to the GFF3 adapter
+ undoing an "add_feature/add_transcript" operation will now warn the user that proceed will delete the feature
+ data adapter grouping (see FASTA adapter)
+ adding/updating PubMed ID will now show the publication title for confirmation
+ improved add_transcripts_from_gff3_to_annotations.pl
+ adding of different annotation types (gene, pseudogene, tRNA, snRNA, snoRNA, ncRNA, rRNA, miRNA, repeat_region, transposable_element)
+ adding GO terms now supports searching/autocompletion
+ annotation info editor now requests data from the server in batch mode (should improve speed)

Fixes:

+ Chado featureprop writeback for generic attributes
+ add_transcripts_from_gff3_to_annotations.pl now properly loads status attributes
+ attributes being lost after undo/redo
+ redo of a merge_transcript operation after deleting one of the transcripts *should* work now
+ properly handle dragged transcripts that contain UTR elements
+ compressed data sent to the client no longer causes the client to hang when there are too many annotations
+ rewrote much of the split_transcript operation to better handle gene splitting


## 2013-09-02 release

Features:

+ Updated core client base code to JBrowse 1.10.2
+ 'Share' tab has been removed from the toolbar on top. The complete URL can be obtained from the browser line, it keeps track of all displayed tracks, and highlighted regions, if any.
+ in the aligned sequence BAM reads display, green, yellow and red colors flag insertions, substitutions and deletions (respectively). When the user hovers over them, details about the substituting nucleotide, or the size of the insertion or deletion are displayed.
+ Pinning a track to the top is now possible. The "User-Created Annotations" track is always pinned to the top, and may not be removed.
+ Alternate codon translation tables are now available and configurable.
+ Alternate splice sites are now configurable.
+ Filtering the list of available tracks is now possible.
+ It is also possible to combine the information from different quantitative tracks into a 'Combination Track', under the 'File' menu. Data from tracks containing graphs may be compared and combined in an additive, subtractive, or divisive manner. The resulting track highlights the differences between the data. This feature is useful for visualization of differential expression analyses, to observe changes in expression depending on he conditions.
+ The 'View' tab in the toolbar at the top now contains the menus from the 'Options' tab in the last version. Also, it is possible to 'Highlight' a region by selecting the option, then rubber-band marking the region. The highlight option will automatically be turned 'On' when inspecting the results from a BLAT search.
+ From the 'right/apple-click' menu, there are a number of new options.
    + It is now possible to set the 5', 3', or both boundaries from a track of evidence or alignment, as the boundaries of an exon in the 'User-Created Annotations' area.
    + It is also possible to annotate 'Readthrough Stop' signals using the apple/right-click menu. The current 'Stop' exon will be highlighted in purple, and the next 'Stop' signal in frame will be used as the end of translation.
    + It is also possible to use the 'Set longest ORF' option.
+ New metadata configuration in the 'Annotation Information Editor' now includes:
    + Name, Symbol, and Description Lines
    + Fields to input crossed references to other Data Bases (DBXRefs) such as WormBase, FlyBase, NCBI, etc.
    + PubMed IDs
    + GO Terms/IDs
    + The field 'Attributes' captures all additional metadata not already included in Comments, PubMed IDs, GO, and DBXRefs in a 'Tag/Value' format.
    + When ready to export, all metadata in the AIE goes to the 9th column of the GFF3.
+ GFF3 adapter now allows configuring the value to be put in the source column (column 2)
+ Rewrote 'add_transcripts_to_annotations_from_gff3.pl' script to write data to server in chunks (helps with efficiency and timeouts), no longer using BioPerl so it can properly handle CDS features
+ Numerous bug fixes



## 2013-05-17 release:

Features:

+ Fully revamped genomic sequence selection screen
+ sorting by name and length
+ uses customizable JavaScript function
+ filtering of genomic sequence names
+ Annotation info editor
+ allows editing of symbols
+ editing of comments
    + editing of dbxrefs
    + Script for bulk loading gene/transcript/exons to annotation track
+ Improved login system
    + allows logging in from either genomic region selection screen or editor
+ Configuration for using computed CDS (if available) when first creating a transcript (rather that using the longest ORF)
+ Improved memory management
+ Improved handling of concurrent edits
+ HTTPS support
+ Deletion of transcripts now warn users
+ Accessing editor without logging in hides annotation track
+ Using "[" and "]" to navigate through subfeatures when a feature in the annotation track is selected
+ Improved interface for adding insertions and deletions
+ Option to hide plus and/or minus strand
+ Various bugfixes

