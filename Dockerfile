# Apollo2.X
FROM ubuntu:18.04
MAINTAINER Nathan Dunn <nathandunn@lbl.gov>
ENV DEBIAN_FRONTEND noninteractive

# where bin directories are
ENV CATALINA_HOME /usr/share/tomcat9
# where webapps are deployyed
ENV CATALINA_BASE /var/lib/tomcat9
ENV CONTEXT_PATH ROOT
ENV JAVA_HOME /usr/lib/jvm/java-8-openjdk-amd64

RUN apt-get -qq update --fix-missing && \
	apt-get --no-install-recommends -y install \
	git build-essential libpq-dev wget \
	lsb-release gnupg2 wget xmlstarlet netcat libpng-dev postgresql-common \
	zlib1g-dev libexpat1-dev curl ssl-cert zip unzip openjdk-8-jdk-headless

RUN sh -c 'echo "deb http://apt.postgresql.org/pub/repos/apt/ `lsb_release -cs`-pgdg main" >> /etc/apt/sources.list.d/pgdg.list' && \
    wget --quiet -O - https://www.postgresql.org/media/keys/ACCC4CF8.asc | apt-key add -

RUN apt-get -qq update --fix-missing && \
	apt-get --no-install-recommends -y install \
	postgresql-9.6 postgresql-client-9.6  tomcat9 && \
	apt-get autoremove -y && apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/* /apollo/

RUN curl -sL https://deb.nodesource.com/setup_12.x | bash -
RUN apt-get -qq update --fix-missing && \
	apt-get --no-install-recommends -y install nodejs

RUN npm i -g yarn

RUN useradd -ms /bin/bash -d /apollo apollo

RUN curl -s "http://hgdownload.soe.ucsc.edu/admin/exe/linux.x86_64/blat/blat" -o /usr/local/bin/blat && \
 		chmod +x /usr/local/bin/blat && \
 		curl -s "http://hgdownload.soe.ucsc.edu/admin/exe/linux.x86_64/faToTwoBit" -o /usr/local/bin/faToTwoBit && \
 		chmod +x /usr/local/bin/faToTwoBit && \
		wget --quiet https://github.com/erasche/chado-schema-builder/releases/download/1.31-jenkins26/chado-1.31.sql.gz -O /chado.sql.gz && \
		gunzip /chado.sql.gz

#NOTE, we had problems with the build the archive-file coming in from github so using a clone instead
COPY client /apollo/client
COPY gradlew /apollo
COPY grails-app /apollo/grails-app
COPY gwt-sdk /apollo/gwt-sdk
COPY lib /apollo/lib
COPY src /apollo/src
COPY web-app /apollo/web-app
COPY wrapper /apollo/wrapper
ADD gra* /apollo/
COPY apollo /apollo/apollo
ADD build* /apollo/
ADD settings.gradle /apollo
ADD application.properties /apollo
RUN ls /apollo

USER apollo
RUN curl -s get.sdkman.io | bash && \
		/bin/bash -c "source $HOME/.sdkman/bin/sdkman-init.sh && yes | sdk install grails 2.5.5" && \
 		/bin/bash -c "source $HOME/.sdkman/bin/sdkman-init.sh && yes | sdk install gradle 3.2.1"

USER root

COPY docker-files/build.sh /bin/build.sh
# Everything above here can be pre-built.

USER root
# install grails
ADD docker-files/docker-apollo-config.groovy /apollo/apollo-config.groovy
RUN chown -R apollo:apollo /apollo

USER apollo
RUN /bin/bash -c "source $HOME/.sdkman/bin/sdkman-init.sh && /bin/bash /bin/build.sh"

USER root
# remove from webapps and copy it into a staging directory
RUN rm -rf ${CATALINA_BASE}/webapps/* && \
	cp /apollo/apollo*.war ${CATALINA_BASE}/apollo.war


ADD docker-files/createenv.sh /createenv.sh
ADD docker-files/launch.sh /launch.sh
CMD "/launch.sh"
