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

