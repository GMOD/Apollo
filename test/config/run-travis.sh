#!/bin/bash

EXIT_STATUS=0

echo "Running test $TEST_SUITE"

if [[ $TEST_SUITE == "apollo" ]]; then
  travis_wait ./gradlew installJBrowseWebOnly gwtc installJBrowseTools && ./grailsw refresh-dependencies --stacktrace && ./grailsw test-app -coverage -xml --stacktrace && ./grailsw coveralls
fi
if [[ $TEST_SUITE == "javascript" ]]; then
  jshint client/apollo/js
fi
if [[ $TEST_SUITE == "python-apollo" ]]; then
#  set -ex
#  cp src/integration-test/groovy/resources/travis/python-apollo.travis apollo-config.groovy
#  ./grailsw run-app &
#  git clone --single-branch --branch 4.2.2 --depth=1 https://github.com/galaxy-genome-annotation/python-apollo
#  cd python-apollo
#  sed -i 's|8888|8080/apollo|' `pwd`/test-data/local-arrow.yml
#  export ARROW_GLOBAL_CONFIG_PATH=`pwd`/test-data/local-arrow.yml
#  python3 --version
#  python3 -m venv .venv
#  . .venv/bin/activate
#  python3 --version
#  pip3 install .
#  pip3 install nose
#  ./bootstrap_apollo.sh --nodocker
#  python3 setup.py nosetests
#  killall java || true
      set -ex
      cp test/config/python-apollo.travis apollo-config.groovy
      ./grailsw run-app &
#      git clone --single-branch --branch fix-default-apollo-test --depth=1 https://github.com/galaxy-genome-annotation/python-apollo
      git clone --single-branch --branch 4.2.2 --depth=1 https://github.com/galaxy-genome-annotation/python-apollo
      cd python-apollo
      sed -i -e 's|8080/|8080/apollo|' "`pwd`/test-data/local-arrow.yml"
      ARROW_GLOBAL_CONFIG_PATH="`pwd`/test-data/local-arrow.yml"
      export ARROW_GLOBAL_CONFIG_PATH
      echo "`pwd`/test-data/local-arrow.yml"
      python3 -m venv .venv
      . .venv/bin/activate
      pip3 install .
      pip3 install nose
      ./bootstrap_apollo.sh --nodocker
      python3 setup.py nosetests
      killall java || true
fi

exit $EXIT_STATUS
