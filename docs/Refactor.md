
RefactorClient goals:
- Modular code
- Net reduction in LOC
- Testable code
- No ad-hoc fixes, do the right thing
- Modern javascript approaches
- Configurable and extensible
- The intangible benefit of clean code

RefactorClient changes:
- Remove DraggableHTMLFeatures inheritence  from SequenceTrack
- Make SequenceTrack inherit from jbrowse Sequence track
- Add CodonTable.js change in JBrowse to allow multiple CodonTracks
- Removed all the webapollo Sequencetrack code to make room for inheriting jbrowse code
- Create new translation table renderer in CodonTable.js for jbrowse
- Login using AJAX (instead of synchronous XHR)!! Many “ajax” requests we have are using synchronous flag. Very bad practice
- Remove maxPxPerBp limit to allow zooming in farther into the sequence
- Remove “BrowserZoomFix” (unnecessary after Sequencetrack refactor)
- Use the quickHelp/aboutThisBrowser config options instead of custom help links
- Random fix: the title now says webapollo in the browser window instead of jbrowse


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
