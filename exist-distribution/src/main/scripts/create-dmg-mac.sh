#!/usr/bin/env bash
#
# Elemental
# Copyright (C) 2024, Evolved Binary Ltd
#
# admin@evolvedbinary.com
# https://www.evolvedbinary.com | https://www.elemental.xyz
#
# Use of this software is governed by the Business Source License 1.1
# included in the LICENSE file and at www.mariadb.com/bsl11.
#
# Change Date: 2028-04-27
#
# On the date above, in accordance with the Business Source License, use
# of this software will be governed by the Apache License, Version 2.0.
#
# Additional Use Grant: Production use of the Licensed Work for a permitted
# purpose. A Permitted Purpose is any purpose other than a Competing Use.
# A Competing Use means making the Software available to others in a commercial
# product or service that: substitutes for the Software; substitutes for any
# other product or service we offer using the Software that exists as of the
# date we make the Software available; or offers the same or substantially
# similar functionality as the Software.
#

## Uses dmgbuild (https://github.com/al45tair/dmgbuild) to build a DMG file

# $1 is the path to settings.py for dmgbuild
# $2 is app name e.g. VolName
# $3 is the output DMG file path and name
dmgbuild_settings_py=$1
volname=$2
output_dmg=$3

set -x

# cleanup any previous DMG file before creating a new DMG image
if [[ -f "${output_dmg}" ]]; then
    echo "Removing previous DMG"
    rm -v "${output_dmg}"
fi

# Make sure that dmgbuild is installed
if ! pip show dmgbuild > /dev/null 2>&1; then
  yes | pip install dmgbuild
fi

set -e

# Build a new DMG image
dmgbuild -s ${dmgbuild_settings_py} "${volname}" "${output_dmg}"
