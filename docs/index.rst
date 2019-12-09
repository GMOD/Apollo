Apollo
============

Apollo - A collaborative, real-time, genome annotation web-based editor.

The application's technology stack includes a Grails-based Java web application with flexible database backends and a
Javascript client that runs in a web browser as a JBrowse plugin.

You can find the latest release here: https://github.com/GMOD/Apollo/releases/latest and our setup guide: http://genomearchitect.readthedocs.io/en/latest/Setup.html

- Apollo general documentation: `http://genomearchitect.github.io/ <http://genomearchitect.github.io>`__

- JBrowse general documentation: `http://jbrowse.org <http://jbrowse.org>`__

- Citing Apollo: `Dunn, N. A. et al. Apollo: Democratizing genome annotation. PLoS Comput. Biol. 15, e1006790 (2019) <https://doi.org/10.1371/journal.pcbi.1006790>`_

.. image:: https://travis-ci.org/GMOD/Apollo.png?branch=master

Note: This documentation covers release versions 2.x of Apollo. For the 1.0.4 installation please refer to
the installation guide found at `http://genomearchitect.readthedocs.io/en/1.0.4/ <http://genomearchitect.readthedocs.io/en/1.0.4/>`__


.. A PDF version of this documentation is also available for download.

.. Link `https://media.readthedocs.org/pdf/webapollo/latest/webapollo.pdf <https://media.readthedocs.org/pdf/webapollo/latest/webapollo.pdf>`__



Contents:

.. toctree::
   :maxdepth: 2
   :glob:
   :caption: Installing Apollo

   Setup
   Docker
   Configure
   ChadoExport
   Data_loading
   Data_Loading_via_web_services
   Troubleshooting
   ExampleBuild
   OpenIDConnectAuthentication
   Migration

.. toctree::
   :maxdepth: 2
   :glob:
   :caption: Using Apollo

   Demo
   UsersGuide
   Permissions

.. toctree::
   :maxdepth: 2
   :glob:
   :caption: Developing Apollo

   Apollo2Build
   Contributing
   Testing_notes
   Architecture
   Command_line
   Web_services
