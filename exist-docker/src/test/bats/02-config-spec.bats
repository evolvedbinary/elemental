#!/usr/bin/env bats
#
# eXist-db Open Source Native XML Database
# Copyright (C) 2001 The eXist-db Authors
#
# info@exist-db.org
# http://www.exist-db.org
#
# This library is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 2.1 of the License, or (at your option) any later version.
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


# Tests for modifying eXist's configuration files
@test "copy configuration file from container to disk" {
  run docker cp exist-ci:exist/etc/conf.xml ./conf.xml && [[ -e ./conf.xml ]] && ls -l ./conf.xml
  [ "$status" -eq 0 ]
}

@test "modify the copied config file" {
  run sed -i.bak 's/wait-before-shutdown="120000"/wait-before-shutdown="60000"/' ./conf.xml
  [ "$status" -eq 0 ]
}

@test "create modified image" {
  run docker create --name ex-mod -p 9090:8080 existdb/exist-ci-build:latest
  [ "$status" -eq 0 ]
  run docker cp ./conf.xml ex-mod:exist/config/conf.xml
  [ "$status" -eq 0 ]
  run docker start ex-mod
  [ "$status" -eq 0 ]
}

@test "modification is applied in container" {
  # Make sure container is running
  result=$(docker ps | grep -o 'ex-mod')
  [ "$result" == 'ex-mod' ]
  sleep 30
  result=$(docker logs ex-mod | grep -o "60,000 ms during shutdown")
  [ "$result" == '60,000 ms during shutdown' ]
}

# TODO(DP): see https://github.com/eXist-db/exist/issues/2987 
#  modify MAX_CACHE via ARG and confirm result 

# TODO(DP): see https://github.com/eXist-db/exist/issues/1771
# upload xar with jar, and make see if it works

@test "teardown modified image" {
  run docker stop ex-mod
  [ "$status" -eq 0 ]
  [ "$output" == "ex-mod" ]
  run docker rm ex-mod
  [ "$status" -eq 0 ]
  [ "$output" == "ex-mod" ]
  run rm ./conf.xml
  [ "$status" -eq 0 ]
  run rm ./conf.xml.bak
  [ "$status" -eq 0 ]
}

@test "log queries to system are visible to docker" {
  run docker exec exist-ci java org.exist.start.Main client -q -u admin -P '' -x 'util:log-system-out("HELLO SYSTEM-OUT")'
  [ "$status" -eq 0 ]
  result=$(docker logs exist-ci | grep -o "HELLO SYSTEM-OUT" | head -1)
  [ "$result" == "HELLO SYSTEM-OUT" ]
}

@test "regular log queries are visible to docker" {
  run docker exec exist-ci java org.exist.start.Main client -q -u admin -P '' -x 'util:log("INFO", "HELLO logged INFO")'
  [ "$status" -eq 0 ]
  result=$(docker logs exist-ci | grep -o "HELLO logged INFO" | head -1)
  [ "$result" == "HELLO logged INFO" ]
}