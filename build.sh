#!/usr/bin/env bash
#
# Elemental
# Copyright (C) 2024, Evolved Binary Ltd
#
# admin@evolvedbinary.com
# https://www.evolvedbinary.com | https://www.elemental.xyz
#
# This library is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; version 2.1.
#
# This library is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public
# License along with this library; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
#

##
# Simple build Script for Elemental that tries to make it easier to build a few of the usual targets
# Author: Adam Retter
##

set -e

TARGET="useage"
OFFLINE=false
CONCURRENCY="-T2C"

POSITIONAL=()
while [[ $# -gt 0 ]]
do
key="$1"

case $key in
    quick|quick-archives|quick-docker|quick-archives-docker|quick-install|test|site|license-check|license-format|dependency-check|dependency-security-check)
    TARGET="$1"
    shift
    ;;
    --offline)
    OFFLINE=true
    shift
    ;;
    -h|--help)
    TARGET="useage"
    shift
    ;;
    *)
    POSITIONAL+=("$1") # save it in an array for later
    shift
    ;;
esac
done

function print-useage() {
  echo -e "\n./build.sh [--offline] <target> | --help"
  echo -e "\nAvailable build targets are:"
  echo -e "\tquick - A distribution directory that can be found in exist-distribution/target/elemental-x.y.x-dir"
  echo -e "\tquick-archives - A distribution directory, and distribution archives that can be found in exist-distribution/target/"
  echo -e "\tquick-docker - A distribution directory and Docker Image"
  echo -e "\tquick-archives-docker - A distribution directory, distribution archives, and Docker Image"
  echo -e "\tquick-install - A distribution directory, and installs Maven Artifacts to your local Maven repository"
  echo -e "\ttest - Runs the test suite"
  echo -e "\tsite - Runs the test suite and produces a Maven Site in target/site/ that details the results"
  echo -e "\tlicence-check - Checks that all source files have the correct license header"
  echo -e "\tlicence-format - Adds the correct license header to any source files that are missing it"
  echo -e "\tdependency-check - Checks that all modules have correctly declared their dependencies"
  echo -e "\tdependency-security-check - Checks that all dependencies have no unexpected CVE security issues"
  echo -e "\n--offline - attempts to run the Maven build in offline mode"
}

set -- "${POSITIONAL[@]}" # restore positional parameters

if [ "${TARGET}" == "useage" ]; then
  print-useage
  exit 0;
fi


SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
BASE_CMD="${SCRIPT_DIR}/mvnw -V"

if [ "${OFFLINE}" == "true" ]; then
  BASE_CMD="${BASE_CMD} --offline"
fi

if [ "${TARGET}" == "quick" ]; then
  CMD="${BASE_CMD} ${CONCURRENCY} clean package -DskipTests -Ddependency-check.skip=true -Dappbundler.skip=true -Ddocker=false -P !mac-dmg-on-mac,!codesign-mac-app,!codesign-mac-dmg,!mac-dmg-on-unix,!installer,!concurrency-stress-tests,!micro-benchmarks,skip-build-dist-archives"
  $CMD
  exit 0;
fi

if [ "${TARGET}" == "quick-archives" ]; then
  CMD="${BASE_CMD} ${CONCURRENCY} clean package -DskipTests -Ddependency-check.skip=true -Ddocker=true -P installer,!concurrency-stress-tests,!micro-benchmarks"
  $CMD
  exit 0;
fi

if [ "${TARGET}" == "quick-docker" ]; then
  CMD="${BASE_CMD} ${CONCURRENCY} clean package -DskipTests -Ddependency-check.skip=true -Dappbundler.skip=true -Ddocker=true -P docker,!mac-dmg-on-mac,!codesign-mac-app,!codesign-mac-dmg,!mac-dmg-on-unix,!installer,!concurrency-stress-tests,!micro-benchmarks,skip-build-dist-archives"
  $CMD
  exit 0;
fi

if [ "${TARGET}" == "quick-archives-docker" ]; then
  CMD="${BASE_CMD} ${CONCURRENCY} clean package -DskipTests -Ddependency-check.skip=true -Ddocker=true -P installer,-P docker,!concurrency-stress-tests,!micro-benchmarks"
  $CMD
  exit 0;
fi

if [ "${TARGET}" == "quick-install" ]; then
  CMD="${BASE_CMD} ${CONCURRENCY} clean install package -DskipTests -Ddependency-check.skip=true -Dappbundler.skip=true -Ddocker=false -P !mac-dmg-on-mac,!codesign-mac-app,!codesign-mac-dmg,!mac-dmg-on-unix,!installer,!concurrency-stress-tests,!micro-benchmarks,skip-build-dist-archives"
  $CMD
  exit 0;
fi

if [ "${TARGET}" == "test" ]; then
  CMD="${BASE_CMD} clean test -Ddependency-check.skip=true"
  $CMD
  exit 0;
fi

if [ "${TARGET}" == "site" ]; then
  CMD="${BASE_CMD} clean test -Ddependency-check.skip=true"
  $CMD
  exit 0;
fi

if [ "${TARGET}" == "license-check" ]; then
  CMD="${BASE_CMD} license:check"
  $CMD
  exit 0;
fi

if [ "${TARGET}" == "license-format" ]; then
  CMD="${BASE_CMD} license:format"
  $CMD
  exit 0;
fi

if [ "${TARGET}" == "dependency-check" ]; then
  CMD="${BASE_CMD} dependency:analyze"
  $CMD
  exit 0;
fi

if [ "${TARGET}" == "dependency-security-check" ]; then
  CMD="${BASE_CMD} dependency-check:check"
  $CMD
  exit 0;
fi

print-useage
exit 0;