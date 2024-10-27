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
