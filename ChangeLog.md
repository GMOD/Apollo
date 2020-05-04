
## 2.6.0

Features

- Remove popups for annotations in favor of annotator panel [2334](https://github.com/GMOD/Apollo/pull/2334)
- Added 2 more pseudogenic SO terms and several more non-coding RNA terms and updated interfaces to reflect these [2475](https://github.com/GMOD/Apollo/pull/2475)
- Implemented gene product and field provenance annotations with evidence and GFF3 export [2371](https://github.com/GMOD/Apollo/pull/2371), [2234](https://github.com/GMOD/Apollo/pull/2234), [2312](https://github.com/GMOD/Apollo/pull/2312), [2424](https://github.com/GMOD/Apollo/pull/2424) 
- Added name and type to top of annotation details [2423](https://github.com/GMOD/Apollo/pull/2423)
- Added GO Annotations to GFF3 export and extended to transcript [2400](https://github.com/GMOD/Apollo/pull/2400)
- loadLink can take name of gene in evidence (if JBrowse names have been processed) [2444](https://github.com/GMOD/Apollo/pull/2444)
- Add alias to user interface and GFF3 export [2277](https://github.com/GMOD/Apollo/pull/2277)
- Add help menu feedback [2344](https://github.com/GMOD/Apollo/pull/2344)
- Allows for better performance when a large number of annotations are present [2477](https://github.com/GMOD/Apollo/pull/2477)
- Show genome features in current view [2346](https://github.com/GMOD/Apollo/pull/2346)
- Remove sequence lookup at the top [2407](https://github.com/GMOD/Apollo/pull/2407)
- Provide better security and feedback when trying to export with insufficient permissions [2431](https://github.com/GMOD/Apollo/pull/2431)
- Split out default GO Evidence Codes from ECO codes [2429](https://github.com/GMOD/Apollo/pull/2429)
- Added more info (e.g, JBrowse and general settings) to the about window and is visible on login [47f469f7](https://github.com/GMOD/Apollo/commit/47f469f7586c49f51e1c2f23b59a0a2102ca224a)


Infrastructure Changes

- Upgrade to [JBrowse 1.16.8](https://github.com/GMOD/jbrowse/releases/tag/1.16.8-release)
- Upgrade to [Node 13](https://github.com/GMOD/Apollo/issues/2358)
- Add python library to the docker image [2409](https://github.com/GMOD/Apollo/issues/2409)

Bug Fixes

- Open by unqiuename to get only the results of the name we are interested in [2338](https://github.com/GMOD/Apollo/pull/2338)
- Fixed ability to add multiple BAMs at once [2352](https://github.com/GMOD/Apollo/pull/2352)
- Fixed export of non-coding RNA if exon not present [2353](https://github.com/GMOD/Apollo/pull/2353)
- Removed sequence panel lookup [2388](https://github.com/GMOD/Apollo/pull/2353)
- Annotator/updateFeature should store history properly [2390](https://github.com/GMOD/Apollo/pull/2353)
- Makes sure that parent directory exists when unpacking [2437](https://github.com/GMOD/Apollo/pull/2437)
- Fixed bug when decompressing gff3.gz data when adding a new track [2434](https://github.com/GMOD/Apollo/pull/2434)
- Fixed boolean environment options being interpreted correctly [be31b81f7](https://github.com/GMOD/Apollo/commit/be31b81f7d0668916bf92463a758506757cc5ada)


## 2.5.0

Features

- Moved blat / blast search to its own tab [2259](https://github.com/GMOD/Apollo/pull/2259) 
- Allow creation of annotations from blat features [2225](https://github.com/GMOD/Apollo/pull/2225) 
- On upload of fasta file execute `faToTwoBit` if present to create searchable blat file [2262](https://github.com/GMOD/Apollo/pull/2262) 
- Allows history tracking for non-structural edits [2246](https://github.com/GMOD/Apollo/pull/2246)
- Search pop-up links directly to sequence search [2253](https://github.com/GMOD/Apollo/pull/2209)
- Organism and Group tab should searchable [2081](https://github.com/GMOD/Apollo/pull/2081) 
- Allow upload of blat data from web-services [2209](https://github.com/GMOD/Apollo/pull/2209)
- Added citation and documentation link to help menu [2243](https://github.com/GMOD/Apollo/pull/2243)
- Allow update of organism via rest API [2227](https://github.com/GMOD/Apollo/pull/2227)
- Allow inhibition of reloading sequences when uploading sequences to the data directory[2293](https://github.com/GMOD/Apollo/issues/2293)
- Adds GO annotation to right-click menu [2213](https://github.com/GMOD/Apollo/issues/2213)
- Adds support for GPI export [2238](https://github.com/GMOD/Apollo/issues/2238)


Bug Fixes

- Fixed color by type error [2203](https://github.com/GMOD/Apollo/issues/2203)
- Find all organisms creates exception when organism does not exist [2275](https://github.com/GMOD/Apollo/issues/2275) 
- Fixed bug where in the track web-service that hides genomic element that is overlapped by a larger genomic element [2255](https://github.com/GMOD/Apollo/issues/2275)
- Fixed errors with remote services organism info update [2248](https://github.com/GMOD/Apollo/pull/2248)
- Numerous GO annotation fixes [2229](https://github.com/GMOD/Apollo/pull/2229),  [2233](https://github.com/GMOD/Apollo/pull/2233), [2171](https://github.com/GMOD/Apollo/issues/2171)
- Allow deletion of exon with properties on it([2228](https://github.com/GMOD/Apollo/pull/2228)
- Fixed bug where automated name generation generates unusable names [2134](https://github.com/GMOD/Apollo/issues/2134)
- Errors in the track panel create a popup-error [2204](https://github.com/GMOD/Apollo/issues/2204)
- Numerous web-service fixes [2298](https://github.com/GMOD/Apollo/pull/2298) [2290](https://github.com/GMOD/Apollo/pull/2290) [2275](https://github.com/GMOD/Apollo/pull/2275)

Infrastructure Changes

- Fixed Docker build to pull directly from current source instead of GitHub [2300]((https://github.com/GMOD/Apollo/issues/2300))
- Upgrade to [JBrowse 1.16.6](https://github.com/GMOD/jbrowse/releases/tag/1.16.6-release), which fixes drag error on Chrome mac <https://github.com/GMOD/jbrowse/issues/1397>
- Allowed for Node 12 [2274](https://github.com/GMOD/Apollo/issues/2274)
- Reorder of documentation into multiple layers, moving user's guide into local [documentation / read the doc](https://genomearchitect.readthedocs.io/en/latest/).
- Added web-services tests using arrow's [python-apollo](https://github.com/galaxy-genome-annotation/python-apollo) from the [galaxy-genome-annoation](https://galaxyproject.org/use/gga/) group [2285](https://github.com/GMOD/Apollo/issues/2285).
 

## 2.4.0

 Features

- Added GO Annotations. [2172](https://github.com/GMOD/Apollo/pull/2172), [2162](https://github.com/GMOD/Apollo/pull/2162) [1134](https://github.com/GMOD/Apollo/issues/1134)
- Allow upload of genomic data to create new organisms. [2023](https://github.com/GMOD/Apollo/pull/2023)
- Allow upload of track data to create new removable tracks. [2024](https://github.com/GMOD/Apollo/pull/2024) [2084](https://github.com/GMOD/Apollo/pull/2084)
- Allow indication of variant effects [1971](https://github.com/GMOD/Apollo/issues/1971)
- Added ability to provide suggestable names [1991](https://github.com/GMOD/Apollo/issues/1991)
- Provide community level private evidence tracks [17](https://github.com/GMOD/Apollo/pull/17)
- Automatically kill or disable server if a common directory is not defined [2079](https://github.com/GMOD/Apollo/pull/2079)
- Added support for Shine Dalgarno sequence [2149](https://github.com/GMOD/Apollo/issues/2149) [1955](https://github.com/GMOD/Apollo/issues/1955)
- Allow for the export of a gene with uncertain internal structure - allow no CDS for an gene [1989](https://github.com/GMOD/Apollo/issues/1989)
- Added shortcut for sequence and GFF3 popups [2116](https://github.com/GMOD/Apollo/issues/2116)
- Added the ability to do group add /delete in bulk and group update in bulk [2105](https://github.com/GMOD/Apollo/issues/2105)


Infrastructure Changes

- Integrated Docker into the Apollo repository directly so that `latest` is always the current snapshot and `stable` is always the latest release. [2184](https://github.com/GMOD/Apollo/issues/2184)
- Upgrade to [JBrowse 1.16.5](https://github.com/GMOD/jbrowse/releases/tag/1.16.5-release)
- Added script to delete features by name and unique name [2138](https://github.com/GMOD/Apollo/issues/2138)
 
 Bug Fixes
 
- Fix issue with rendering neat features [2063](https://github.com/GMOD/Apollo/pull/2063) 
- Fix issue with creating annotations for reads with indels in them [2085](https://github.com/GMOD/Apollo/pull/2085) 
- Fixes error when changing annotation type when status is set [2167](https://github.com/GMOD/Apollo/issues/2167)
- Fixes for variant search in annotator panel [2147](https://github.com/GMOD/Apollo/issues/2147)
- Fixed bug where `txtz` is not supported when adding a gnome feature [2136](https://github.com/GMOD/Apollo/issues/2136)
- Fixed bug where sequence panel is VERY narrow [2118](https://github.com/GMOD/Apollo/issues/2118)
- Fixed bug where setting the 5' and 3' based on reads does not work [2110](https://github.com/GMOD/Apollo/issues/2110)
- Numerous UI issue fixes [2109](https://github.com/GMOD/Apollo/issues/2109)[2113](https://github.com/GMOD/Apollo/issues/2113)
- Fixed rendering of certain exons [2063](https://github.com/GMOD/Apollo/issues/2063)
- Fixed bug where indels in long RNAseq reads interpreted as introns when used to make annotations [2085](https://github.com/GMOD/Apollo/issues/2085)
- Fixed bug where unable to drag features when the new inferHTMLSubfeatures is fals [2099](https://github.com/GMOD/Apollo/issues/2099)
- Unable to create variants in some cases [2103](https://github.com/GMOD/Apollo/issues/2103)

 

## 2.3.1

Features


- Allowed duplication of organism from the Organism Panel [1968](https://github.com/GMOD/Apollo/pull/1968)
- Allows making an organism obsolete [1967](https://github.com/GMOD/Apollo/pull/1967)
- Create a filter and flag for inactive users [1937](https://github.com/GMOD/Apollo/pull/1937)
- Allowed deletion of feature from the Sequence and Annotator Panels [2040](https://github.com/GMOD/Apollo/pull/2040)
- Allow export of orig_id as an optional attribute [2002](https://github.com/GMOD/Apollo/pull/2002)

Bug Fixes

- Fixed issue when dragging features to resize was not providing an indicator box [1988](https://github.com/GMOD/Apollo/pull/1988)
- Fixed issue when sequence alterations were not rendering the full height of the sequence track [2049](https://github.com/GMOD/Apollo/pull/2049)
- Making organisms obsolete should remove associated permissions [2043](https://github.com/GMOD/Apollo/pull/2043)
- User-created features taken from split reads should create features with introns [2036](https://github.com/GMOD/Apollo/pull/2036)
- Should indicate split reads properly in evidence [2034](https://github.com/GMOD/Apollo/pull/2034)[2054](https://github.com/GMOD/Apollo/pull/2054)
- Temporary files not removed during export of GFF3 or other file types [1966](https://github.com/GMOD/Apollo/pull/1966)
- Fixed issue where you could not alter an isoform after deleting a gene name [1961](https://github.com/GMOD/Apollo/pull/1961)
- Fixed some minor issues associated with sequence alterations [1497](https://github.com/GMOD/Apollo/pull/1497)
- Multiple owners tagged on GFF3 export [29](https://github.com/GMOD/Apollo/pull/29)
- Setting gene description puts UcA into a bad state [2056](https://github.com/GMOD/Apollo/issues/2056)
- Fixes plugin inclusion error by reverting prior fix [2055](https://github.com/GMOD/Apollo/issues/2055)



## 2.3.0


Features

- Moved to JBrowse 1.16.2 by default [1988](https://github.com/GMOD/Apollo/pull/1988), which also fixed track styling issues [#1942](https://github.com/GMOD/Apollo/pull/1942)
- Moved to Neat Features as the default  [2021](https://github.com/GMOD/Apollo/pull/2021)
- Improved terminator annotation [1997](https://github.com/GMOD/Apollo/pull/1997)


Bug Fixes

- JBrowse bin directory not properly being installed [2017](https://github.com/GMOD/Apollo/pull/2017)
- Workaround for declaring plugins in trackList.json via a JBrowse bug [2014](https://github.com/GMOD/Apollo/pull/2014)
- Fixed problems with creating variant annotations with soft masking [2009](https://github.com/GMOD/Apollo/pull/2009)
- Fixed bugs in reporting code [2008](https://github.com/GMOD/Apollo/pull/2008)
- Fixed several build bugs #1996, #1994, #1993
- Fixed bug where deletions, insertions, and substitutions were not editable in the side-bpanel [#1923](https://github.com/GMOD/Apollo/pull/2008)



## 2.2.0

Features

- Move to JBrowse 1.15 and webpack (smaller file size) [1928](https://github.com/GMOD/Apollo/pull/1928),[1986](https://github.com/GMOD/Apollo/pull/1986)
- Add ability to annotate terminators [1954](https://github.com/GMOD/Apollo/issues/1954)
- Added a script for removing all features from a sequence [1935](https://github.com/GMOD/Apollo/pull/1935)
- Allowed removal of ALL user permissions (instead of deleting) to inactivate [777](https://github.com/GMOD/Apollo/issues/777)
- Clarified edge-detection [9](https://github.com/GMOD/Apollo/issues/9)


Bug Fixes

- Fixed bug where showing annotations by users was lost [1952](https://github.com/GMOD/Apollo/issues/1952)
- Fixed by where lowercase reference sequence does not translate correctly in 'Get Sequence' output  [1944](https://github.com/GMOD/Apollo/issues/1944)
- Fixed problem where removing an available status type caused an error [1909](https://github.com/GMOD/Apollo/issues/1909)
- Fixed bug where a logged-in link location was ignored when passed in by copying the logged in URL [1982](https://github.com/GMOD/Apollo/issues/1982)
- Fixed bug where insertion and deletion details did not come up in the Annotator Panel when clicked [1984](https://github.com/GMOD/Apollo/issues/1984)



## 2.1.0

Features

- Added ability to annotate a variant from VCF evidence tracks [1892](https://github.com/GMOD/Apollo/pull/1892)
- Allow forced assignment of transcript to a gene [#1851](https://github.com/GMOD/Apollo/pull/1851)
- Added proper Instructor and Organism Admin permission level [#1178](https://github.com/GMOD/Apollo/issues/1178)
- Indicate start / stop codons with color [#1852](https://github.com/GMOD/Apollo/pull/1852)
- Set the default biotype on track [#1861](https://github.com/GMOD/Apollo/issues/1861)
- Focus annotator panel on the current transcript [#1846](https://github.com/GMOD/Apollo/issues/1846)
- Allow fetching variant data from evidence tracks via web service [#1867](https://github.com/GMOD/Apollo/pull/1867)
- Recognized dot notation from JBrowse / Apollo [#1839](https://github.com/GMOD/Apollo/issues/1839)
- Allow setting default native track to true [#1848](https://github.com/GMOD/Apollo/pull/1848)
- Provide [sample data](https://github.com/GMOD/Apollo/blob/master/docs/Apollo2Build.md#adding-sample-data) (and links in doc)[#1817](https://github.com/GMOD/Apollo/pull/1817) 


Bug Fixes

- Fixed descriptor leak when loading bulk loading GFF3 [#1187](https://github.com/GMOD/Apollo/pull/1887)
- Fixed adding ability to create sequence alterations of uneven length [#1883](https://github.com/GMOD/Apollo/issues/1883)
- Fixed problem where canonical splice-sites were not recognized if sequence was being shown in lower-case [#1879](https://github.com/GMOD/Apollo/issues/1879)
- Prevents setting bad translation starts and ends [#1838](https://github.com/GMOD/Apollo/issues/1838)
- Improved performance of cache deletion code [#1824](https://github.com/GMOD/Apollo/pull/1824)
- Allow more special characters in the password [#1859](https://github.com/GMOD/Apollo/issues/1859)
- Fixed bug with 'Update Membership' and 'Update Group Admin' call in web services [#1891](https://github.com/GMOD/Apollo/issues/1891)
- Fixed bug with 'Update Organism Permission' call in web services [#1885](https://github.com/GMOD/Apollo/issues/1885)
- In some cases when the name store is not properly configured, the location is not remembered [#1895](https://github.com/GMOD/Apollo/issues/1895)

## 2.0.8

Features

- Added the ability to annotate from high performance [Alignments2](http://gmod.org/wiki/JBrowse_Configuration_Guide#Alignments2) BAM reads [#1789](https://github.com/GMOD/Apollo/pull/1789)
- Added support for indexed FASTA to be used as reference sequence. [#1791](https://github.com/GMOD/Apollo/pull/1791)
- Added sequence API [#1799](https://github.com/GMOD/Apollo/pull/1799)
- Added ability to remove gridlines from the view menu in both light and dark themes [#1547](https://github.com/GMOD/Apollo/pull/1547)

Bug Fixes 

- Fixed bug when flipping strand fails to flip the strand of the owning gene [#1769](https://github.com/GMOD/Apollo/issues/1769)
- Fixes to track services to allow remote jbrowse tracks and jsonz [#1767](https://github.com/GMOD/Apollo/issues/1767)
- Fixes to track services to return 404 when bad URL is given [#1768](https://github.com/GMOD/Apollo/issues/1768)
- Fixed CORS issues [#1760](https://github.com/GMOD/Apollo/issues/1760)
- Fixed bug where gene positions are sometimes wrong when a longer isoform is deleted from a gene [#1770](https://github.com/GMOD/Apollo/issues/1770)
- Fixed permissions for REMOTE_USER when using web services [#1759](https://github.com/GMOD/Apollo/issues/1759)
- Fixes build issues [#1756](https://github.com/GMOD/Apollo/issues/1756) [#1752](https://github.com/GMOD/Apollo/issues/1752) [#1773](https://github.com/GMOD/Apollo/issues/1773)
- Fixes error in SQL query for listing alterations [#1754](https://github.com/GMOD/Apollo/issues/1754)
- Minimum node version is version 6
- Fix UserPanel for a large number of users [#1800](https://github.com/GMOD/Apollo/pull/1800)
- Fixes recalculated gene positions for some delete exon operations [#1808](https://github.com/GMOD/Apollo/issues/1808)
- Fix big when updating organism via web-service [#1804](https://github.com/GMOD/Apollo/issues/1804)


## 2.0.7

Features

- Add the ability to upload organism sequence data and track data to a remote Apollo instance via Apollo Web Services [#1670](https://github.com/GMOD/Apollo/pull/1670).
- Allow setting of alternate translation table per organism using the _Details_ panel under the _Organism_ tab in the Annotator panel. [#95](https://github.com/GMOD/Apollo/issues/95)
- Draggable BAM tracks now support coloring by strand. Reads aligned to forward strand are colored blue, while those in the reverse strand are red.  [#412](https://github.com/GMOD/Apollo/issues/412)
- The list of _Tracks_ in the Annotator panel now allows for the separation of data types into categories. [#536](https://github.com/GMOD/Apollo/issues/536)
- Tracks in a category can be added or removed all at once. [#1733](https://github.com/GMOD/Apollo/pull/1733)
- When applicable, warnings now alert users of insufficient permissions to perform certain functions. [#553](https://github.com/GMOD/Apollo/issues/553)
- Restrictions are now in place to prevent users from modifying or deleting annotations that they did not create. [#1260](https://github.com/GMOD/Apollo/issues/1260)
- Updated settings for the ability to filter by organism when applying metadata. For instance, admin may now apply canned comments, keys and values, only to a subset of organisms in their server. As well, statuses can be retrieved per type of genomic element, per organism, etc. [#1676](https://github.com/GMOD/Apollo/pull/1676)
- Admins can now build public URLs to hyperlink directly to a specific genomic element. [#1482](https://github.com/GMOD/Apollo/pull/1482)
- It is now possible to set _Statuses_ as well as adding or editing _Canned elements_ using our Web Service (REST) API. [#1538](https://github.com/GMOD/Apollo/pull/1538)
- In the absence of ```Name``` attribute in GFF3 file, Apollo uses ```ID``` attribute to name the annotation in JSON. [#1639](https://github.com/GMOD/Apollo/pull/1639)
- A number of other improvements to performance have been made, such as fetching preferences from session. [#1604](https://github.com/GMOD/Apollo/pull/1604) [#1725](https://github.com/GMOD/Apollo/pull/1725)
- Added date created field to changes report. [#1728](https://github.com/GMOD/Apollo/pull/1728)
- Removal of bower in favor of npm to install JBrowse. [#1691](https://github.com/GMOD/Apollo/pull/1691)
- Added documentation for a Web Service wrapper for Python, PHP, etc. See [Web Services API documentation](http://genomearchitect.readthedocs.io/en/latest/Web_services.html).

Bug Fixes

- Fixed bug in which ```add_transcripts_from_gff3_to_annotations.pl``` replaced valid mRNA name with gene name. [#1475](https://github.com/GMOD/Apollo/issues/1475)
- Fixed bug in which ```REMOTE_USER``` was not cached everywhere and was being ignored on ping request. [#1492](https://github.com/GMOD/Apollo/pull/1492)
- Added warning to Production pre-requisites: if using gradle and gradlew, admins should define ```JAVA_HOME``` to avoid build fails. See documentation [here](http://genomearchitect.readthedocs.io/en/latest/Setup.html#production-pre-requisites).
- Fixed sorting bug on the dropdown list of organisms. [#1497](https://github.com/GMOD/Apollo/issues/1497)
- Fixed a bug in which the absence of an organism created downstream issues such as errors listing groups of users in Annotator panel. (Feature for admins). [#1504](https://github.com/GMOD/Apollo/pull/1504)
- Fixed a bug in which creating a user via Web Service API generated an error message. [#1510](https://github.com/GMOD/Apollo/pull/1510)
- Fixed import script ```add_transcripts_from_gff3_to_annotations.pl``` to introduce correct handling of sequence alterations and read-through stop codons. [#1524](https://github.com/GMOD/Apollo/pull/1524)
- Fixed bug that now allows leading start non-M codons in organisms with non-standard code to be translated as Methionine (M). [#1544](https://github.com/GMOD/Apollo/issues/1544)
- Updated GWT code to fix a bug that prevented Apollo from generating URLs appropriately - pipes were not being encoded. [#1606](https://github.com/GMOD/Apollo/pull/1606)
- Fixed bug in the calculation of open reading frames for the negative strand for the purpose of coloring each exon according to the CDS. Exported sequences had been - and remain - correctly generated. [#1629](https://github.com/GMOD/Apollo/issues/1629)
- Fixed bug that delayed propagation of updates when boundaries for an annotation's parent element were changed. [#1631](https://github.com/GMOD/Apollo/issues/1631) 
- Restored _'Pin to top'_ and _'Delete track'_ functionality for tracks with ```HTMLFeatures```. [#1632](https://github.com/GMOD/Apollo/issues/1632)
- Fixed cascade bug when changing annotation type for an annotation that has a read-through stop codon. [#1717](https://github.com/GMOD/Apollo/pull/1717)
- Apollo client being initialized twice in some instances. [#1742](https://github.com/GMOD/Apollo/issues/1742)



## 2.0.6

Features

+ Moved the native track panel button to the main window #1398
+ Add new 'default_group' param for remote_user auth #1445
+ Added icon to toggle view of native JBrowse tracks that is always visible #1452

Bug Fixes

+ Failure to load tracks when switching organisms with identical Sequence IDs #1391
+ Unable to add organism from script without a pre-existing organism #1388
+ When logged in, clicking on JBrowse would not load the Annotator Panel #1395
+ ```server_data``` may lock some times in dev mode #1419
+ Intron persists in tracks if a single exon if neat features enabled #1417
+ Authentication error with galaxy tools + remote_user #1423
+ When logged in as non-admin user, the show track panel button does not look or work properly #1429
+ When deleting an organism from the interface, should instantly update the organism list similar to add organism #1431
+ 404 errors on CSV metadata in some cases #1448
+ Problems loading list of Tracks when switching organisms on slower connection #1434
+ Setting exon details in details panel fails to set transcript boundaries properly #1428
+ Error opening on double-click for annotations listed in the second page of the annotation panel #1459
+ Transcripts of pseudogenes should NOT have the word 'transcript' or other type in the name #1451
+ Issue with gene names when performing undo and redo right after changing annotation type #1464
+ Interface freezes if right-clicking an unselected annotation #1465
+ Fixed issue where double-click on transcript navigates and then closes the transcript / gene #1467
+ Remote_user not authorized properly everywhere #1468
+ Fixes the small problem with the sticky tracks from loadLinks #1474
+ Bumped default JBrowse version


## 2.0.5

Features 

+ Increase UI and performance by calling setCurrentSequenceLocation less aggressively #1007
+ Various performance improvements #1272,#1276
+ Added ability for Apollo to call home to track server usage #1339
+ Allow multiple calls to google analytics to support users internally #1340
+ Numerous annotator tab user interface improvements #1343,#333
+ Updated 'changes' report page for more detail and better filtering #806
+ Added URL option to open and close the track panels #1332
+ Adds a findChanges web-services method #1316
+ Importing features should be able to optionally include metadata #52
+ Make organism, groups, users tabs more consistent #622
+ Convert javascript "alert" to something more appealing visually #630
+ Using Bootstrap in all panels #847
+ Improved report login window look and feel #1103
+ Upgraded to Java 8  #1327
+ Upgraded to Gwt 2.8.0 and Gwt-Bootstrap 0.9.4 #1075
+ Delete expired preferences #1368
+ Location URLs are now encapsulated in links #1361
+ Bumped default JBrowse version 


Bug Fixes

+ Web-service method 'getUserPermissionsForUser' #1230
+ UI glitch with more than ten groups #1242
+ Error going between full-screen and annotator panel when stored are loaded in the URL #1156,#1214,#1271,#1330,#1331, #1008, #1371
+ 'use_cds_for_new_transcripts' was not being picked up properly #1297
+ Client interprets 5'UTR and 3'UTR features as exons #1308
+ Flag when 3' end or 5' end are missing, still calculate longest ORF #1302
+ Limit Chado export to admins #1322
+ Improved security of non-public genomes #861
+ Adding transcripts via the load transcripts script added preferences #1277
+ Was not validating scaffold against organism on import #1173
+ Sequence display with annotation did not update automatically when moved to opposed strand #645
+ Needed to update the coding detail panel when changing transcript details #379
+ Warn user when making an intron is not possible #1331
+ Load script was returning the wrong error when the wrong password was given #1275
+ use_cds_for_new_transcripts is not being picked up properly #1297
+ Improve merging of functional annotations data during merge operation #646
+ Annotator panel calls the official set exon boundaries method #653
+ Pinstripes disappear in small scaffolds. #709
+ Spring to splice site functionality works once only #735
+ Genomic insertion coordinates: start is greater than end in gff3 output from Reference Sequence tab #747
+ Remove errant bootstrap calls #746
+ Remove Environment.TEST from code #655
+ No glyphs for sequence alterations #908
+ getUserPermissionsForUser is non-standard #1230
+ Fixed case in Webservices docs #1244
+ UI glitch on Groups tab when more than ten groups #1242
+ Fixed error with setting upstream donor failing #1379
+ NCBI Pubmed upgraded to use https form discontinued http #1377
+ Updating exon details in annotator panel fails #1375


## 2.0.4

Features

+ Upgraded to Grails 2.5.5
+ Allow REMOTE_USER authentication (apache / nginx) and added pluggable user authentication (#1042)
+ Ability to enter pre-specified (canned) values for Attributes in the 'Information Editor', similar to canned Comments. (#86)
+ Users may download the genomic sequence from highlighted regions using the export menu in the _User-created Annotations_ area export (#1163)
+ Information from parent features can be retained when loading transcripts onto the 'User-created Annotations' area using the add_transcript_from_gff3_to_annotations.pl loading script (#1171)
+ Added [documentation for using Apollo with Docker](https://github.com/GMOD/Apollo/blob/master/docs/Setup.md#configure-for-docker) (#1016)


Bug Fixes

+ Fixed multiple errors in the add_transcript_from_gff3_to_annotations.pl loading script (#1146)
+ Expired sessions or server disconnection triggers reconnection instead of a silent failure (#493)
+ Fixed bugs in certain web service scripts (#1155)
+ Fixed bug where garbage client token is created (#1172)
+ Fixed several deployment and installation issues (#1135, #1137, #1138, #1150)
+ Fixed adding comments to a sequence alteration fails to create the sequence alteration (#1179)
+ Parameters passed into loadLink are no longer dropped (#1140)
+ Fixed bug not allowing addition of FeatureType in admin menu (#1144)
+ Improved performance of loading the information editor for large genes (#1152)
+ Fixes error when deleting a DBXref or GO ID error (#1163)


## 2.0.3

__Warnings__

* __Before doing the build with Apollo v2.0.3, make sure to install node.js (which includes npm) and install bower. This process will be automatic in v2.0.4 and fixes are detailed in [#1138](https://github.com/GMOD/Apollo/pull/1138).__
* __Download node.js from https://nodejs.org/en/download/__
* __To install bower:__
    * <code>npm install -g bower</code>
* __Then__:
    * <code>./apollo clean-all</code>
    * <code>./apollo deploy</code>

Features

+ Added Ability to export Apollo annotations to a Chado database (#145).
+ Added ability to update an existing Chado database for Chado export (#967).
+ Updated application to use Grails 2.5.4 (#444).
+ Allow apollo-config.groovy configuration to create admin user on startup (#1040).
+ Added ability to change feature type in User created annotations track (#220).
+ Added ability to use multiple organisms at the same time (#441).
+ Added ability to search selected sequences (for export) and clear selection in Sequence Panel (#730).
+ Added ability to allow username to be a non-email based name (#939).
+ Sync with JBrowse 1.12.2-apollo for stability (#971).

Bug Fixes

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
+ Fixed a bug when creating a non-coding Transcript owner was not set and the username was not being displayed when hovering (#1085). 

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

Bug Fixes

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

Bug Fixes

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

Bug Fixes

+ Organism panel not showing all organisms (#540).
+ Admins for specific organisms have issues with giving other users permissions (#542).

## 2.0.0-RC6

Bug Fixes

+ Fixed multiple bugs having to do with sequence alterations (#534, #531, #458, #456).
+ Fixed logout for multiple windows on the same browser (#480).
+ JBrowse only mode not listening to websockets (#537).  


## 2.0.0-RC5

Features

+ Optimized transcript merging (#529,#515).

Bug Fixes

+ History operations fail when setting acceptor / donor (#530). 

## 2.0.0-RC4
Features

+ Optimized transcript merging (#529).
+ Added "add_comment", "add_attribute", "set_status" to the web services API (#492). 
+ Add an interim export panel (#78).
+ Added google analytics integration (#146).

Bug Fixes

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

Bug Fixes

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

Bug Fixes

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


Bug Fixes

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

Bug Fixes

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

Bug Fixes

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

Bug Fixes:

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

Bug Fixes:

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

