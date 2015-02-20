

RefactorClient changes:

- Remove DraggableHTMLFeatures inheritence  from SequenceTrack
- Make SequenceTrack inherit from jbrowse Sequence track
- Add CodonTable.js change in JBrowse to allow configurable CodonTable
- Removed all the webapollo SequenceTrack code and inherit JBrowse Sequence track
- Login using AJAX instead of synchronouse XHR
- Remove maxPxPerBp limit on the calculated char width to allow further zooming in
- Use actual JSON instead of creating JSON via string building for API requests
- Use the quickHelp/aboutThisBrowser config options
- Browser title says Web Apollo instead of JBrowse (this was implemented previously but was fixed now)
- Make Tool menu appear before help (this was  implemented previously but fixed now)
- Fixed resizing of features at zoom levels higher than the calculated char width
- Completely removed the "DraggableResultFeatures" because it was unused
- Moved code for InformationEditor, GetSequence, and History dialogs from AnnotTrack into other modules
- Silenced noisy console.log due to long-polling
- Use dojo/declare for proper class declarations on legacy modules
- Re-implemented colorByCds back for the new SequenceTrack
- Added JBrowse as a submodule for the repo instead of having maven clone the repo
- Use the same dijit/Menu right-click menu resource allocation scheme that is used for HTMLFeatures to avoid memory overflows (slightly tricky)
- Make a little text that says "Success" before refreshing the page for logins (ideally wouldn't have to do a page refresh)
- Added substitutions/insertions/deletions on new SequenceTrack
- Made substitutions/insertions/deletions overlaid using a transparent div similar to old SequenceTrack
- Made it so Dark theme is now instant and doesn't require a page reload
- The thin borderline between tracks is made invisible in dark mode which is much nicer. Also blue highlight instead of yellow for that super-cool blue feeling.


Casualties of the refactoring process so far:

- The sequence overlay on the annotation track (sequence is overlaid on top of feature)




Future

- Remove "login" code from AnnotTrack, place in main plugin or different module
- Remove the limitation that features without subfeatures cannot be annotated

