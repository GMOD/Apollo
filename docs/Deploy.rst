Deploying the servlet
---------------------

View On GitHub

Copy the WAR file to the webapps directory
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

After the WAR file is generated using the apollo , it will be outputted
to the WEB\_APOLLO\_DIR/target directory (e.g. target/apollo-1.0.war)
This WAR file can be copied into the tomcat7 webapps directory:

::

    cp WEB_APOLLO_DIR/target/apollo-1.x.war TOMCAT_WEBAPPS_DIR/WebApollo.war

We recommend not touching the contents of the WAR file after it has been
deployed to the Tomcat server. The configuration and contents of the WAR
should be finalized before deployment to avoid data loss during Tomcat
Undeploy operations.

Accessing your WebApollo installation
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

After copying your WAR file to the tomcat webapps directory, the app
will be automatically started. Let's test out our installation. Point
your browser to

::

    http://SERVER_ADDRESS:8080/WebApollo

The user name and password from our setup will be ``web_apollo_admin``
or whatever was configured earlier.

If we are using the sample data, we will only see one reference sequence
(scaffold) to annotate since we're only working with a small example.
Click on ``scf1117875582023`` under the ``Name`` column to be taken to
the genome browser.

If you have any problems after this stage, please see the
`troubleshooting <Troubleshooting.md>`__ page.

Now have fun annotating!!!
