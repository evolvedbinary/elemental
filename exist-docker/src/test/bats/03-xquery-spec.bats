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


# Tests that execute xquery via Java entrypoint
@test "Change admin password" {
  run docker exec exist-ci java org.exist.start.Main client -q -u admin -P '' -x 'sm:passwd("admin", "nimda")'
  [ "$status" -eq 0 ]
}

# Tests that use rest endpoint, this might be disabled by default soon
@test "confirm new password" {
  result=$(curl -s -H 'Content-Type: text/xml' -u 'admin:nimda' --data-binary @exist-docker/src/test/bats/fixtures/dba-xq.xml http://127.0.0.1:8080/exist/rest/db)
  [ "$result" == 'true' ]
}

@test "GET version via rest" {
  run curl -s -u 'admin:nimda' "http://127.0.0.1:8080/exist/rest/db?_query=system:get-version()&_wrap=no"
  [ "$status" -eq 0 ]
  echo '# ' $output >&3
}

@test "POST list repo query" {
  result=$(curl -s -H 'Content-Type: text/xml' -u 'admin:nimda' --data-binary @exist-docker/src/test/bats/fixtures/repo-list.xml http://127.0.0.1:8080/exist/rest/db | grep -o 'http://' | head -1)
  [ "$result" == 'http://' ]
}

@test "PUT xq should succeed and trigger auto-register" {
  run curl -i -X PUT -H "Content-Type: application/xquery" -d $'xquery version "3.0";\nmodule namespace host = "http://host/service";\nimport module namespace rest = "http://exquery.org/ns/restxq";\ndeclare\n %rest:POST\n	%rest:path("/forgot")\n %rest:query-param("email", "{$email}")\n	%rest:consumes("application/x-www-form-urlencoded")\n %rest:produces("text/html")\nfunction host:function1($email) {\n let $doc := \n<Customer>\n <Metadata>\n <Created>{current-dateTime()}</Created>\n </Metadata>\n <Contact>\n <Email>{$email}</Email>\n </Contact>\n </Customer>\n return\n let $new-file-path := xmldb:store("/db/forgot", concat($email, ".xml"), $doc)\n return\n <html xmlns="http://www.w3.org/1999/xhtml">\n <body>SUCCESS</body>\n </html>\n};' http://admin:nimda@127.0.0.1:8080/exist/rest/db/forgot.xqm
  [ "$status" -eq 0 ]
  result=$(docker exec exist-ci java org.exist.start.Main client -q -u admin -P 'nimda' -x 'rest:resource-functions()' | grep -o 'http://host/service')
  [ "$result" == 'http://host/service' ]
}

@test "teardown revert changes" {
  run docker exec exist-ci java org.exist.start.Main client -q -u admin -P 'nimda' -x 'sm:passwd("admin", "")'
  [ "$status" -eq 0 ]
}