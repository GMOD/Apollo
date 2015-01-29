RefactorClient goals:
- Modular code
- Testable code
- Avoid ad-hoc fixes, do the right thing
- Modern javascript approaches
- Configurable and extensible

RefactorClient changes:
- Remove DraggableHTMLFeatures inheritence  from SequenceTrack
- Make SequenceTrack inherit from jbrowse Sequence track
- Add CodonTable.js change in JBrowse to allow multiple CodonTracks
- Removed all the webapollo Sequencetrack code to make room for inheriting jbrowse code
- Login using AJAX. Many "ajax" requests we have are using synchronous XHR instead.
- Remove maxPxPerBp limit to allow zooming in farther into the sequence
- Remove “BrowserZoomFix” (unnecessary after Sequencetrack refactor).
- Make the JSON requests use actual JSON instead of string building
- Use the quickHelp/aboutThisBrowser config options
- The title now says webapollo instead of jbrowse (was implemented but fixed now)
- Make Tool menu appear before help (was previously implemented but fixed now)

Casualties of the refactoring process, might be reimplemented soon:
- the sequence displaying inside the feature when zoomed in (should this be reimplemented?)
- color by CDS doesn’t highlight frame (temporarily)
- the highlight doing both top and bottom (temporarily)
- creating new insertions and deletions (temporarily)



Envision:
- Simple highlighting bases when we have insertion or deletion
- Remove “login” code from annottrack, place in main plugin or different module
- Remove information editor code from annottrack, place in new module

![Refactor](http://i.imgur.com/2QnCnJP.png)
