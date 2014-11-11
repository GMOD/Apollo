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

package: 
	mvn package
release: download-jbrowse copy-webapollo-plugin version build-jbrowse
	mv $(APOLLO_JBROWSE_GITHUB)/JBrowse-$(JBROWSE_VERSION) $(APOLLO_JBROWSE_DIRECTORY)
debug: download-jbrowse copy-webapollo-plugin version build-jbrowse
	mv $(APOLLO_JBROWSE_GITHUB)/JBrowse-$(JBROWSE_VERSION)-dev $(APOLLO_JBROWSE_DIRECTORY)
github: download-jbrowse copy-webapollo-plugin version
	cp -R $(APOLLO_JBROWSE_GITHUB) $(APOLLO_JBROWSE_DIRECTORY) && rm -rf $(APOLLO_JBROWSE_DIRECTORY)/.git
build-jbrowse:
	ulimit -n 1000;cd $(APOLLO_JBROWSE_GITHUB)&&$(MAKE) -f build/Makefile release-notest
version:
	echo "<a href='https://github.com/GMOD/Apollo/commit/$(GMOD_VERSION)' target='_blank'>Version: $(POM_VERSION)</a>" > $(APOLLO_WEBAPP_DIRECTORY)/version.jsp
download-jbrowse: | $(APOLLO_JBROWSE_GITHUB)
	test -d $(APOLLO_JBROWSE_GITHUB) || git clone --recursive $(JBROWSE_GITHUB) $(APOLLO_JBROWSE_GITHUB)
copy-webapollo-plugin:
	cp -R $(APOLLO_ROOT_DIRECTORY)/client/apollo $(APOLLO_JBROWSE_GITHUB)/plugins/WebApollo
clean: clean-webapp
clean-webapp:
	mvn clean
	rm -rf $(APOLLO_JBROWSE_DIRECTORY)
clean-repos: clean
	rm -rf $(APOLLO_JBROWSE_GITHUB)
clean-jbrowse-repo: clean
	cd $(APOLLO_JBROWSE_GITHUB)&&make -f build/Makefile superclean


.PHONY: clean clean-webapp clean-jbrowse-repo clean-repos debug release build-jbrowse github copy-webapollo-plugin copy-config-files version
