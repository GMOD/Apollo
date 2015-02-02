
// from grails-gwt plugin, maybe be exapnded

//includeTargets << new File("${gwtPluginDir}/scripts/_GwtInternal.groovy")

eventCompileStart = {
//  println "COMPILE START"
//  checkGwtHome()
//  updateClasspath()
    projectCompiler.srcDirectories << "${basedir}/src/gwt/org/bbop/apollo/gwt/shared"
}

//eventSetClasspath = { ClassLoader rootLoader ->
//  updateClasspath(rootLoader)
//}
//
//// Called when the compilation phase completes.
//eventCompileEnd = {
//    // Compile the GWT modules. This target is provided by '_GwtInternal'.
//    checkGwtHome()
//    if (!usingGwt16) {
//        compileGwtModules()
//    }
//}
//
//// Clean up the GWT-generated files on "clean".
//eventCleanEnd = {
//    gwtClean()
//}
//
//eventConfigureWarNameEnd = {
//    // If any of the GWT modules haven't been compiled, force a compilation
//    // now. This ensures that WAR files are always created with the latest
//    // compiled JS files.
//    if (!gwtModulesCompiled) {
//        gwtForceCompile = true
//
//        // Disable draft mode when we create a WAR.
//        gwtDraftCompile = false
//        compileGwtModules()
//    }
//}
//
////
//// The GWT libs must be copied to the WAR file. In addition, although
//// we don't do dynamic compilation in production mode, the plugin
//// groovy class gets compiled with the UnableToCompleteException in
//// the class file. Thus, we also have to include this particular file
//// in the system.
////
//eventCreateWarStart = { warName, stagingDir ->
//    if (gwtHome) {
//      // Extract the UnableToCompleteException file from gwt-dev-*.jar
//      ant.unjar(dest: "${stagingDir}/WEB-INF/classes") {
//          patternset(includes: "com/google/gwt/core/ext/UnableToCompleteException.class")
//          fileset(dir: "${gwtHome}", includes: "gwt-dev-*.jar")
//      }
//    } else if (gwtResolvedDependencies) {
//      def gwtDevJar = gwtResolvedDependencies.find { it.name.contains("gwt-dev")}
//      // Extract the UnableToCompleteException file from gwt-dev-*.jar
//      ant.unjar(dest: "${stagingDir}/WEB-INF/classes") {
//          patternset(includes: "com/google/gwt/core/ext/UnableToCompleteException.class")
//          path(location: gwtDevJar.absolutePath)
//      }
//    }
//
//}
//
////
//// Adds the GWT servlet library to the root loader.
////
//eventPackageAppEnd = {
//  if (getBinding().variables.containsKey("gwtHome")) {
//    def gwtServlet = new File(gwtHome, "gwt-servlet.jar")
//    if (gwtServlet.exists()) {
//      rootLoader.addURL(gwtServlet.toURI().toURL())
//    }
//  }
//}
//
//eventGwtRunHostedStart = {
//    compileGwtClasses()
//}
//
//eventGwtCompileStart = {
//    compileGwtClasses()
//}
//
//void compileGwtClasses(forceCompile = false) {
//    if (!gwtClassesCompiled && (usingGoogleGin || forceCompile)) {
//        // Hack to work around an issue in Google Gin:
//        //
//        //    http://code.google.com/p/google-gin/issues/detail?id=36
//        //
//        ant.mkdir(dir: gwtClassesDir)
//        gwtJavac( destDir: gwtClassesDir, includes: "**/*.java") {
//            src(path: 'src/gwt')//current project gwt modules
//            //include any sources from any included plugins
//            buildConfig?.gwt?.plugins?.each {pluginName ->
//              def pluginDir = binding.variables["${pluginName}PluginDir"]
//              if (pluginDir && new File("${pluginDir}/src/gwt").exists()) {
//                src(path: "${pluginDir}/src/gwt")
//              }
//            }
//            ant.classpath {
//
//                gwtResolvedDependencies.each { File f ->
//                    pathElement(location: f.absolutePath)
//                }
//
//                fileset(dir: gwtHome) {
//                    include(name: "gwt-dev*.jar")
//                    include(name: "gwt-user.jar")
//                }
//
//                if (gwtLibFile.exists()) {
//                    fileset(dir: gwtLibPath) {
//                        include(name: "*.jar")
//                    }
//                }
//
//                if (buildConfig.gwt.use.provided.deps == true) {
//                    if (grailsSettings.metaClass.hasProperty(grailsSettings, "providedDependencies")) {
//                        grailsSettings.providedDependencies.each { dep ->
//                            pathElement(location: dep.absolutePath)
//                        }
//                    }
//                    else {
//                        ant.echo message: "WARN: You have set gwt.use.provided.deps, " +
//                                          "but are using a pre-1.2 version of Grails. The setting " +
//                                          "will be ignored."
//                    }
//                }
//                pathElement(location: grailsSettings.classesDir.path)
//
//                // Fix to get this working with Grails 1.3+. We have to
//                // add the directory where plugin classes are compiled
//                // to. Pre-1.3, plugin classes were compiled to the same
//                // directory as the application classes.
//                if (grailsSettings.metaClass.hasProperty(grailsSettings, "pluginClassesDir")) {
//                    pathElement(location: grailsSettings.pluginClassesDir.path)
//                }
//            }
//        }
//        gwtClassesCompiled = true
//    }
//}
//
//loadGwtTestTypeClass = { ->
//    def doLoad = { -> classLoader.loadClass('org.codehaus.groovy.grails.plugins.gwt.GwtJUnitGrailsTestType') }
//    try {
//      doLoad()
//    } catch (ClassNotFoundException e) {
//      includeTargets << grailsScript("_GrailsCompile")
//      compile()
//      doLoad()
//    }
//  }
//
//registerGwtTestTypes = {
//    // register gwt test types in unit test phase
//    if (!binding.variables.containsKey("unitTests") || gwtTestTypesRegistered) return
//    def type = loadGwtTestTypeClass()
//    unitTests << type.newInstance(gwtTestTypeName, gwtRelativeTestSrcPath)
//    unitTests << type.newInstance(gwtProdTestTypeName, gwtRelativeTestSrcPath)
//    gwtTestTypesRegistered = true
//}
//
//eventAllTestsStart = {
//    registerGwtTestTypes()
//}
//
//eventTestCompileStart = { types ->
//    // both gwt and normal unit test can refer GWT classes, hence - they must be compiled before compiling test classes
//    compileGwtClasses(true)
//    (gwtDependencies + gwtClassesDir).each { classLoader.addURL(it.toURI().toURL()) }
//
//    // if we use specific JDK for compiling GWT classes, then we need to compile test/gwt classes as well
//    // before Grails attempts that
//    if (types.class == loadGwtTestTypeClass() && gwtJavacCmd) {
//        def destDir = new File(grailsSettings.testClassesDir, types.relativeSourcePath)
//        ant.mkdir(dir: destDir)
//        gwtJavac(destdir: destDir, classpathref: "grails.test.classpath", debug: "yes") {
//            src(path: new File("${testSourceDir}", types.relativeSourcePath))
//        }
//    }
//
//}
//
//eventPackagePluginsEnd = {
//    // invoked after installing plugin and compiling its classes
//    // and from other places in the build process. However, adjusting
//    // classpaths and registering gwt test types should happen only once
//
//    // if GWT dependencies are not discovered, do it now
//    if (!gwtDependencies) {
//        eventSetClasspath(classLoader)
//        classpathSet = false
//        classpath()
//    }
//    registerGwtTestTypes()
//}