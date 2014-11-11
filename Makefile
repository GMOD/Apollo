BASEDIR = $(PWD)
APOLLO_ROOT_DIRECTORY=$(BASEDIR)
APOLLO_BUILD_DIRECTORY=$(APOLLO_ROOT_DIRECTORY)
APOLLO_WEBAPP_DIRECTORY=$(APOLLO_ROOT_DIRECTORY)/src/main/webapp
APOLLO_JBROWSE_DIRECTORY=$(APOLLO_WEBAPP_DIRECTORY)/jbrowse
APOLLO_JBROWSE_GITHUB=$(APOLLO_ROOT_DIRECTORY)/jbrowse-github
JBROWSE_GITHUB=https://github.com/GMOD/jbrowse
JBROWSE_RELEASE=master
JBROWSE_VERSION=dev
GIT_VERSION=`git rev-parse --verify HEAD`
POM_VERSION=`mvn validate | grep Building | cut -d' ' -f4`

release: version build
	mv $(APOLLO_JBROWSE_GITHUB)/JBrowse-$(JBROWSE_VERSION) $(APOLLO_JBROWSE_DIRECTORY)
debug: version build
	mv $(APOLLO_JBROWSE_GITHUB)/JBrowse-$(JBROWSE_VERSION)-dev $(APOLLO_JBROWSE_DIRECTORY)
github: version
	cd $(APOLLO_JBROWSE_GITHUB)&&mkdir JBrowse-dev&&zip -r JBrowse-dev/JBrowse-dev.zip . -x \*.git\*&&cd JBrowse-dev&&unzip JBrowse-dev.zip&&cd .. &&mv $(APOLLO_JBROWSE_GITHUB)/JBrowse-$(JBROWSE_VERSION) $(APOLLO_JBROWSE_DIRECTORY)
build:
	ulimit -n 1000;cd $(APOLLO_JBROWSE_GITHUB)&&$(MAKE) -f build/Makefile release-notest
version:
	echo "<a href='https://github.com/GMOD/Apollo/commit/$(GMOD_VERSION)' target='_blank'>Version: $(POM_VERSION)</a>" > $(APOLLO_WEBAPP_DIRECTORY)/version.jsp

clean:
	mvn clean
	rm -rf $(APOLLO_JBROWSE_DIRECTORY)
clean-repos: clean
	rm -rf $(APOLLO_JBROWSE_GITHUB)
clean-jbrowse: clean
	cd $(APOLLO_JBROWSE_GITHUB)&&make -f build/Makefile superclean
.PHONY: clean clean-repos debug release build
