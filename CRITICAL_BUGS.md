Critical Logic Bugs Found During Grails 7 Migration Review
===========================================================

This file contains only CRITICAL bugs: those that affect all users, cause silent
data corruption, prevent app startup, or completely break major features.
Situational and non-critical bugs are in IMPORTANT_BUGS.md.

CRITICAL BUGS (ALL FIXED)
--------------------------

1. MISSING RETURN AFTER ERROR RENDER (all controllers) [FIXED]
   ~100 methods across all controllers sent an error response but kept
   running the rest of the method.
   CONSEQUENCE: Actions like addFeature, deleteFeature, etc. could execute
   even after a permission error was already sent back. Server crashes from
   trying to send two responses.

5. FeaturePropertyService deprecated new Integer() (lines 29, 31) [FIXED]
   Used a Java constructor removed in Java 16+.
   CONSEQUENCE: Crashes on Java 17 (required by Grails 7) whenever
   comments are sorted, breaking the comment display.

6. RequestHandlingService deprecated new Boolean() (line 673) [FIXED]
   Same issue — removed constructor.
   CONSEQUENCE: Crashes on Java 17 when getFeatures is called with a
   topLevel parameter, breaking the annotation panel.

7. FeatureProperty.equals() missing return true (line 47) [FIXED]
   The equals method never returned true, even for identical objects.
   CONSEQUENCE: Duplicate feature properties accumulate in the database.
   Set/Map operations on FeatureProperty objects silently malfunction
   (contains() always returns false, remove() never matches, etc.).

17. Groovy 4 log.error() type mismatch (multiple files) [FIXED]
    `log.error(e)` / `log.error(map)` / `log.error(e.printStackTrace())`
    pass non-String arguments to SLF4J Logger.error().
    CONSEQUENCE: Server crash (MissingMethodException) on any error path
    that tries to log — the error handling itself throws an exception.
    FIX: Changed to `log.error(e.message, e)`.

19. Groovy 4 Date.minus() removed (FeatureEventController line 216) [FIXED]
    `today.minus(20*365)` used a method removed in Groovy 4.
    CONSEQUENCE: Feature event history view crashes for all users.
    FIX: Replaced with Calendar-based date arithmetic.

20. Groovy 4 closure ClassNotFoundException in @Transactional services [FIXED]
    GORM's @Transactional AST transform renames methods with `__tt__`
    prefix, changing Groovy closure class names. In Groovy 4, these
    renamed closure classes fail to load at runtime.
    CONSEQUENCE: ClassNotFoundException crash when any affected code path
    executes (e.g. PermissionService.findHighestEnum, and potentially
    dozens more methods across all @Transactional services).
    FIX: Converted all .each{}, .collect{}, .findAll{}, .sort{} closures
    to for loops across 11 service files (~50 closures total).

24. Spring Security 6 session not persisted [FIXED]
    Spring Security 6 no longer auto-saves SecurityContext to HTTP session.
    CONSEQUENCE: Login succeeds but session is lost on next request —
    user appears logged out immediately after logging in.
    FIX: Added HttpSessionSecurityContextRepository in SecurityConfig and
    explicit session save in ApolloSecurityUtils.loginUser().

25. Static resources not served by embedded Tomcat [FIXED]
    Spring Boot embedded Tomcat does not auto-serve src/main/webapp/ files
    to DispatcherServlet.
    CONSEQUENCE: GWT annotator.nocache.js returns 404 — blank page on
    load. JBrowse assets also unreachable.
    FIX: Added addResourceHandlers() in Application.groovy for /annotator/,
    /jbrowse/, /css/, /js/, /images/, /translation_tables/.

27. RoleService.initRoles() silent save failure on PostgreSQL [FIXED]
    Role.save() without failOnError/flush — saves fail silently on
    PostgreSQL, and subsequent findByName returns null.
    CONSEQUENCE: NPE crash on startup with PostgreSQL. Application
    cannot start at all with a PostgreSQL database.
    FIX: Added `failOnError: true, flush: true` and null checks.

32. @Transactional closure conversions — second pass [FIXED]
    Converted remaining .each{}, .collect{}, .findAll{} closures to for
    loops in three more files:
    - RequestHandlingService: 8 closures
    - FeatureService: 4 closures
    - FeatureRelationshipService: 8 closures
    CONSEQUENCE: ClassNotFoundException at runtime when any of these code
    paths execute, due to GORM @Transactional AST method renaming.

33. VariantService.updateAlternateAlleles copy-paste bug (lines 236-237) [FIXED]
    Both `oldAllele` and `newAllele` were looked up using the same `bases`
    variable instead of `oldAlleleBases` / `newAlleleBases`.
    CONSEQUENCE: When updating an allele's bases, the old allele info is
    deleted from the wrong allele (or the same allele), and new info is
    added to the wrong allele. Allele updates are silently corrupted.
    FIX: Changed to use `oldAlleleBases` and `newAlleleBases` respectively.
