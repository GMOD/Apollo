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
+ Using javascript minimization for JBrowse+WebApollo to allow faster initial load time
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

