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


# Basic start-up and connection tests
@test "container jvm responds from client" {
  run docker exec exist-ci java -version
  [ "$status" -eq 0 ]
}

@test "container can be reached via http" {
  result=$(curl -Is http://127.0.0.1:8080/ | grep -o 'Jetty')
  [ "$result" == 'Jetty' ]
}

@test "container reports healthy to docker" {
  result=$(docker ps | grep -o 'healthy')
  [ "$result" == 'healthy' ]
}

@test "logs show clean start" {
  result=$(docker logs exist-ci | grep -o 'Server has started')
  [ "$result" == 'Server has started' ]
}

@test "logs are error free" {
  result=$(docker logs exist-ci | grep -ow -c 'ERROR' || true)
  [ "$result" -eq 0 ]
}

@test "logs contain repo.log output" {
  result=$(docker logs exist-ci | grep -o -m 1 'Deployment.java')
  [ "$result" == 'Deployment.java' ]
}
