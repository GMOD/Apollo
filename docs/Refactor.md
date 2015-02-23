

RefactorClient changes:

Sequence track specific changes on branch
- Remove DraggableHTMLFeatures inheritence  from SequenceTrack
- Make SequenceTrack inherit from jbrowse Sequence track and remove webapollo specific sequence rendering
- Remove maxPxPerBp zoom limit (fairly large change, many places in code were using the "calculatedCharWidth")
- Re-implement the NCBI custom codon table for new sequence track
- Re-implemented colorByCdsFrame feature with new SequenceTrack
- Used the same dijit right-click allocation scheme that is used for HTMLFeatures on new Sequence track
- Re-implemented substitutions/insertions/deletions on new SequenceTrack and use a transparent div similar to old SequenceTrack

Other changes on branch
- Login using AJAX instead of synchronous XHR (use proper dojo Deferred/promises)
- Refactor JSON used in for API requests (which was very cumbersome and used everywhere in AnnotTrack.js)
- Completely removed the "DraggableResultFeatures" because it was unused (has interesting feature for "Promote all to annotation track" if we need to revisit)
- Moved code for InformationEditor, GetSequence, and History dialogs from AnnotTrack into other modules (1500 lines refactored from annottrack.js)
- Silenced unnecessary console.log errors when changing chromosomes
- Added JBrowse as a submodule for the repo instead of using maven to clone the repo
- Make a little text that says "Success" before refreshing the page for logins (ideally wouldn't have to do a page refresh at all for logins)
- Made it so Dark theme is now instantly changes the theme instead of page request (also refactored CSS in process)


Casualties of the refactoring process so far:

- The sequence overlay on the annotation track (sequence is overlaid on top of feature)
- The visualization of the letters on the sequence alterations hasn't been implemented yet (might not be too hard)




Future

- Continue to remove unnecessary code from AnnotTrack which had "God class" anti-pattern https://en.wikipedia.org/wiki/God_object
- Remove the limitation that features without subfeatures cannot be annotated (Important!)

