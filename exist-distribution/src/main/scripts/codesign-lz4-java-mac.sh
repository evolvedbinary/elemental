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


# $1 is .app/Contents/Java dir
# $2 is the lz4-java version
# $3 is temp work directory
# $4 the mac codesign identity


set -e
#set -x  ## enable to help debug

# ensure a clean temp work directory
if [ -d "${3}/net" ]
then
  rm -rf "${3}/net"
fi

# for each native arch
archs=('aarch64' 'x86_64')
for arch in ${archs[@]}
do
  # create the temp output dirs
  mkdir -p "${3}/net/jpountz/util/darwin/${arch}"

  # switch to temp output dir
  pushd "${3}"

  # extract the native files
  jar -xf "${1}/lz4-java-${2}.jar" "net/jpountz/util/darwin/${arch}/liblz4-java.dylib"

  # test if the file is unsigned, and sign if needed
  /usr/bin/codesign --verbose --test-requirement="=anchor trusted" --verify "net/jpountz/util/darwin/${arch}/liblz4-java.dylib" || /usr/bin/codesign --verbose --force --timestamp --sign "${4}" "net/jpountz/util/darwin/${arch}/liblz4-java.dylib"

  # overwrite the file in the jar
  jar -uf "${1}/lz4-java-${2}.jar" "net/jpountz/util/darwin/${arch}/liblz4-java.dylib"

  # switch back from temp output dir
  popd

done

