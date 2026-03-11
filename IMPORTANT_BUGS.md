Important Bugs Fixed During Grails 7 Migration
================================================

These are bugs that cause situational crashes, wrong results, silent failures,
or other issues. Critical bugs (affecting all users or causing data corruption)
are in CRITICAL_BUGS.md.

2. Bare `render jsonObject` without `as JSON` [FIXED]
   ~56 render calls across 4 controllers sent JSON objects without setting
   the content type.
   CONSEQUENCE: Clients receive JSON data labeled as HTML. Browser-side
   JSON parsing fails, breaking the annotation editor UI.

3. PreferenceService NPEs on lines 161, 243, 298-299, 373-374 [FIXED]
   Accessed properties on possibly-null preference objects.
   CONSEQUENCE: Server crash (500 error) when a user has no saved
   preference, e.g. on first login or after preferences are cleaned up.

4. IOServiceController GString in HQL (line 111) [FIXED]
   Used Groovy string interpolation inside a Hibernate query.
   CONSEQUENCE: GFF3/FASTA export fails with a query error whenever
   genes without exons exist in the database.

8. AnnotatorController variant method duplication [FIXED]
   7 copy-pasted methods collapsed into a shared helper.
   CONSEQUENCE: No runtime bug — just maintenance risk where a fix to
   one copy wouldn't be applied to the other six.

9. PreferenceService empty list .first() calls (lines 196, 698, 709) [FIXED]
   Calls .first() on lists that may be empty.
   CONSEQUENCE: Server crash during login or organism-switching when
   the database has no sequences or organisms.

10. Quartz scheduling replaced with Spring @Scheduled [FIXED]
    CleanupPreferencesJob used the Quartz plugin, which has no Grails 7
    compatible version.
    CONSEQUENCE: Without this fix, preference cleanup never runs, and
    stale preferences accumulate indefinitely in the database.

11. FeatureService.groovy NPE risks [FIXED]
    - Line 803: Assumes a transcript always has a feature location.
    - Lines 1528-1529: Assumes CV terms always exist in the database.
    CONSEQUENCE: Server crash when setting translation start on a
    malformed transcript, or when adding a property with an unknown
    CV term type.
    FIX: Added null checks with AnnotationException for missing
    feature location, and log warnings for missing CV/CVTerm.

12. SequenceService.groovy NPE risks [FIXED]
    - Line 363: Assumes sequence lookup always succeeds.
    - Lines 653, 669-673: Assumes feature lookup always succeeds.
    CONSEQUENCE: Server crash during data loading or GFF3 export if a
    feature was deleted between initial query and individual lookup.
    FIX: Added null checks with error logging and skip/continue.

13. PermissionService.groovy dead code (line 420-421) [FIXED]
    The fallback branch passes null as a query parameter, so the query
    always returns nothing.
    CONSEQUENCE: No user-visible bug — wasted database query on every
    permission check where no sequence name is provided.
    FIX: Removed the dead query.

14. AnnotationEditorController.getInformation permission mismatch (line 301) [FIXED]
    Checks WRITE permission without the request context.
    CONSEQUENCE: May check permissions against the wrong organism.
    FIX: Changed checkPermissions(PermissionEnum.WRITE) to
    checkPermissions(inputObject, PermissionEnum.WRITE).

15. GroupController.updateGroup sends no response on success (line 246-281) [FIXED]
    Only the error path sends a response. Success sends nothing.
    CONSEQUENCE: Client receives an empty response after a successful
    group update.
    FIX: Added `render new JSONObject() as JSON` after successful save.

18. Groovy 4 e.printStackTrace() calls (13 instances, 11 files) [FIXED]
    Prints to stderr instead of the logging framework.
    CONSEQUENCE: Error output lost in production (no log file capture).
    FIX: Replaced all 13 instances with `log.error(e.message, e)`.

21. UserController.updateOrganismPermission missing return [FIXED]
    After rendering user-not-found error, no `return` statement.
    CONSEQUENCE: Execution continues with null user, causing NPE or
    creating/modifying permissions for a null user.
    FIX: Added `return` after the error render.

22. UserController/GroupController getBoolean() on missing keys [FIXED]
    `dataObject.getBoolean(PermissionEnum.ADMINISTRATE.name())` throws
    JSONException when the key is absent from the request.
    CONSEQUENCE: API calls that don't include all 4 permission flags
    crash with JSONException.
    FIX: Replaced with `dataObject.optBoolean(perm.name(), false)`.

23. SequenceController.setCurrentSequence null binding + session NPE [FIXED]
    Domain class data binding fails when no ID in URL path; session NPE
    with inline credentials; currentUser null for inline-auth requests.
    CONSEQUENCE: 500 error when setting current sequence via API.
    FIX: Explicit lookup by params.id or sequenceName, session creation,
    inline-auth-aware user lookup.

26. OrganismController.updateTrackForOrganism undefined variable [FIXED]
    Referenced `organismJson` (undefined) instead of `requestObject`,
    and inverted admin check logic.
    CONSEQUENCE: Method always crashes with MissingPropertyException.
    FIX: Changed to `requestObject` and fixed the admin check condition.

28. PermissionService.getOrganismsWithMinimumPermission/getOrganismsWithPermission
    wrong user (lines 101, 128) [FIXED]
    Both methods accept a `User user` parameter but call
    `getOrganismPermissionsForUser(organism, currentUser)` instead of
    using the passed-in `user`.
    CONSEQUENCE: When checking permissions for a different user (e.g. admin
    viewing another user's organism list), the check runs against the admin's
    own permissions instead.
    FIX: Changed `currentUser` to `user` on both lines.

29. PreferenceService.evaluateSaves dead branch (lines 418-422) [FIXED]
    Both branches call `evaluateSave()` with identical arguments.
    CONSEQUENCE: When `onlySaveToken` is set, all tokens are saved instead
    of just the matching one. Causes unnecessary database writes.
    FIX: Added conditional filter so non-matching tokens are skipped.

30. FeatureService.convertFeatureToJSONLite wrong variable in owner loop [FIXED]
    Loop body uses `gsolFeature.owner.username` instead of `owner.username`.
    CONSEQUENCE: When a feature has multiple owners, the same owner name
    is repeated N times instead of listing each owner.
    FIX: Changed to `owner.username`.

31. AnnotationEditorController.getUserPermission dead code (lines 85-88) [FIXED]
    Built a permissions HashMap then immediately overwrote it.
    CONSEQUENCE: No runtime bug — dead code wasting CPU.
    FIX: Removed the dead lines.

34. VariantService.updateAlternateAlleles undefined variables in log (line 101) [FIXED]
    Log message referenced undefined variables, printing "null".
    FIX: Simplified log message to use defined variables.

35. VariantService.deleteAlleleInfo missing save (line 216) [FIXED]
    Method returns `feature` without calling `feature.save()`.
    CONSEQUENCE: Allele info deletions are not persisted to the database.
    FIX: Added `feature.save(flush: true, failOnError: true)`.

36. TranscriptService.addExon gene.save() outside null check (line 266) [FIXED]
    `gene.save()` was outside the `if (gene)` block.
    CONSEQUENCE: NPE crash when adding an exon to a transcript that has
    no parent gene (orphaned transcripts).
    FIX: Moved `gene.save()` inside the `if (gene)` block.

37. UserController.deleteUser render before delete (lines 558-559) [FIXED]
    Response sent before `user.delete()`. If delete fails, the response
    already told the client it succeeded.
    FIX: Delete first, then render.

38. GroupController.updateGroup wrong type conversion (line 225) [FIXED]
    `dataObject.id.split(',').collect() as Long` casts the entire list
    to Long instead of converting each element.
    CONSEQUENCE: ClassCastException crash on multi-group operations.
    FIX: Changed to `.collect { Long.parseLong(it.trim()) }`.

39. GroupController.updateGroup wrong error message (line 265) [FIXED]
    Error message says "Failed to delete the group" — copy-paste error.
    FIX: Changed to "Failed to find the group".

40. AnnotatorController.updateFeature status handling (lines 257-260) [FIXED]
    `Status oldStatus = data.status` should be `feature.status`;
    `feature.status == null` is comparison, not assignment.
    CONSEQUENCE: Status appears cleared in UI but persists in database.
    FIX: Changed to `feature.status` and `=` assignment.

41. FastaHandlerService.writeFeature NPE on null sequence (line 88) [FIXED]
    `seq.length()` called without null check.
    CONSEQUENCE: FASTA export crashes for features on unloaded sequences.
    FIX: Combined null and empty check.

42. CdsService.setStopCodonReadThrough duplicate relationship (lines 120-135) [FIXED]
    When `replace=true`, a new FeatureRelationship is always created
    in addition to the replacement, resulting in duplicates.
    CONSEQUENCE: Two identical CDS→StopCodonReadThrough relationships
    in the database.
    FIX: Skip creating new relationship if setChildForType succeeds.

43. ReportService wrong domain class in HQL queries (lines 82, 84, 162, 163) [FIXED]
    Used `TransposableElement.executeQuery(...)` for RepeatRegion and
    Exon count queries.
    CONSEQUENCE: Report counts may return incorrect results.
    FIX: Changed to correct domain classes.

44. ReportService transcriptCount overwritten (lines 28-34) [FIXED]
    Conditional calculation immediately overwritten by `Transcript.count`.
    FIX: Removed the dead conditional block.

45. ReportService unreachable admin branch (lines 188-200) [FIXED]
    Nested `if (isUserGlobalAdmin(owner))` inside `if (!isUserGlobalAdmin(owner))`
    is always false. Dead code.
    FIX: Removed the dead branch.

46. HomeController CSV column order mismatch (line 92) [FIXED]
    Header said `total,count,mean,max,min,stddev` but data was written
    as `total,count,min,max,mean,stddev`.
    CONSEQUENCE: CSV downloads have swapped min/mean columns.
    FIX: Changed header to match data order.

47. ProxyController null queryString (line 62) [FIXED]
    Concatenates "?null" when there are no query parameters.
    FIX: Added null check on request.queryString before appending.
