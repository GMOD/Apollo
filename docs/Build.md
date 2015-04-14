Building Web Apollo 2.0
--------------------

While the shortcut `apollo deploy` takes care of basic application deployment, understanding the full build process of
Web Apollo 2.0 can help you to optimize and improve your deployed instances.


## Pre-requisites for Javascript minimization
In addition to the system [pre-requisites](Prerequisites.md), the javascript compilation will use NodeJS, which can be
installed from a package manager on many platforms.

    # install nodejs (debian/ubuntu)
    sudo apt-get install git nodejs
    # install nodejs (centOS/redhat)
    sudo yum install epel-release
    sudo yum install git npm
    # install nodejs (macOSX/homebrew)
    brew install node


Additionally some extra Perl modules are needed, which can be downloaded with cpanm or similar:

    cpanm DateTime Text::Markdown


## Performing the javascript minimization

The current preferred method for building a release with Javascript minimization is

    apollo release



## Performing active development

To perform active development of the codebase, use

    apollo debug
    
This will launch a temporary instance Apollo using `ant devmode` so that any changes to the Java or Javascript files will be pick up any changes that you make to the Java files
during the process.


Alternatively, a basic temporary server without devmode can be initialized with

    apollo run-app

