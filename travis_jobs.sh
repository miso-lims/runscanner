#!/bin/bash
set -ev

if [ "$JOB" = "SONAR_AND_UNIT_TESTS" ]; then
    if [[ ${TRAVIS_PULL_REQUEST_SLUG} == ${TRAVIS_REPO_SLUG} ]] ; then 
        # Sonar
        mvn org.jacoco:jacoco-maven-plugin:prepare-agent sonar:sonar
    else 
        echo "[WARN] SonarCloud cannot run on pull requests from forks."
    fi
    # Unit Tests
    mvn clean test
elif [ "$JOB" = "RUNSCANNER_TEST" ]; then
    pushd runscanner-illumina && ./build-illumina-interop && autoreconf -i && ./configure && make && popd;
    cd runscanner
    PATH=$PATH:$(pwd)/../runscanner-illumina mvn clean test -DskipIllumina=false