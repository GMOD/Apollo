# Apollo2.X
FROM gmod/apollo_env:latest
MAINTAINER Nathan Dunn <nathandunn@lbl.gov>
ENV DEBIAN_FRONTEND noninteractive


#NOTE, we had problems with the build the archive-file coming in from github so using a clone instead
COPY client /apollo/client
COPY gradlew /apollo
COPY grails-app /apollo/grails-app
COPY gwt-sdk /apollo/gwt-sdk
COPY lib /apollo/lib
COPY src /apollo/src
COPY web-app /apollo/web-app
COPY wrapper /apollo/wrapper
COPY test /apollo/test
COPY scripts /apollo/scripts
ADD gra* /apollo/
COPY apollo /apollo/apollo
ADD build* /apollo/
ADD settings.gradle /apollo
ADD application.properties /apollo
RUN ls /apollo


COPY docker-files/build.sh /bin/build.sh
ADD docker-files/docker-apollo-config.groovy /apollo/apollo-config.groovy
RUN chown -R apollo:apollo /apollo

# install grails
USER apollo
RUN curl -s get.sdkman.io | bash && \
		/bin/bash -c "source $HOME/.sdkman/bin/sdkman-init.sh && yes | sdk install grails 2.5.5" && \
 		/bin/bash -c "source $HOME/.sdkman/bin/sdkman-init.sh && yes | sdk install gradle 3.2.1" && \
        /bin/bash -c "source $HOME/.sdkman/bin/sdkman-init.sh && /bin/bash /bin/build.sh"

USER root
# remove from webapps and copy it into a staging directory
RUN rm -rf ${CATALINA_BASE}/webapps/* && \
	cp /apollo/apollo*.war ${CATALINA_BASE}/apollo.war

ADD docker-files/createenv.sh /createenv.sh
ADD docker-files/launch.sh /launch.sh
CMD "/launch.sh"
