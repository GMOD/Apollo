# Apollo2
FROM tomcat:9-jdk8
MAINTAINER Nathan Dunn <nathandunn@lbl.gov>
ENV DEBIAN_FRONTEND noninteractive

ENV CATALINA_HOME=/usr/local/tomcat
ENV CONTEXT_PATH ROOT

RUN apt-get -qq update --fix-missing && \
	apt-get --no-install-recommends -y install \
	git build-essential maven libpq-dev postgresql-common openjdk-8-jdk wget \
	postgresql postgresql-client xmlstarlet netcat libpng-dev \
	zlib1g-dev libexpat1-dev ant curl ssl-cert zip unzip

RUN curl -sL https://deb.nodesource.com/setup_8.x | bash -
RUN apt-get -qq update --fix-missing && \
	apt-get --no-install-recommends -y install nodejs && \
	apt-get autoremove -y && apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/* /apollo/

RUN npm i -g yarn

RUN cp /usr/lib/jvm/java-8-openjdk-amd64/lib/tools.jar /usr/lib/jvm/java-8-openjdk-amd64/jre/lib/ext/tools.jar && \
	useradd -ms /bin/bash -d /apollo apollo

RUN curl -s "http://hgdownload.soe.ucsc.edu/admin/exe/linux.x86_64/blat/blat" -o /usr/local/bin/blat && \
 		chmod +x /usr/local/bin/blat && \
 		curl -s "http://hgdownload.soe.ucsc.edu/admin/exe/linux.x86_64/faToTwoBit" -o /usr/local/bin/faToTwoBit && \
 		chmod +x /usr/local/bin/faToTwoBit && \
		wget --quiet https://github.com/erasche/chado-schema-builder/releases/download/1.31-jenkins26/chado-1.31.sql.gz -O /chado.sql.gz && \
		gunzip /chado.sql.gz

#NOTE, we had problems with the build the archive-file coming in from github so using a clone instead
ADD . /apollo

# install grails
COPY docker-files/build.sh /bin/build.sh
ADD docker-files/docker-apollo-config.groovy /apollo/apollo-config.groovy

RUN chown -R apollo:apollo /apollo

USER apollo
RUN curl -s get.sdkman.io | bash && \
		/bin/bash -c "source $HOME/.sdkman/bin/sdkman-init.sh && yes | sdk install grails 2.5.5" && \
 		/bin/bash -c "source $HOME/.sdkman/bin/sdkman-init.sh && yes | sdk install gradle 3.2.1" && \
 		/bin/bash -c "source $HOME/.profile && source $HOME/.sdkman/bin/sdkman-init.sh && /bin/bash /bin/build.sh"

USER root
RUN rm -rf ${CATALINA_HOME}/webapps/* && \
	cp /apollo/apollo*.war ${CATALINA_HOME}/apollo.war

ADD docker-files/createenv.sh /createenv.sh
ADD docker-files/launch.sh /launch.sh
CMD "/launch.sh"
