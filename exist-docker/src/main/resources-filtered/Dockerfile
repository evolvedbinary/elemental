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
# NOTE: Parts of this file contain code from 'The eXist-db Authors'.
#       The original license header is included below.
#
# =====================================================================
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

# Install latest JRE 8 in Debian Stretch (which is the base of gcr.io/distroless/java:8)
FROM debian:stretch-slim as updated-jre
RUN echo "deb http://archive.debian.org/debian stretch main" > /etc/apt/sources.list
RUN echo "deb http://archive.debian.org/debian stretch-backports main" >> /etc/apt/sources.list
RUN apt-get update && apt-get -y dist-upgrade
RUN apt-get install -y openjdk-8-jre-headless
RUN apt-get install -y expat fontconfig     # Install tools required by FOP

FROM gcr.io/distroless/java:8

# Copy over updated JRE from Debian Stretch
COPY --from=updated-jre /etc/java-8-openjdk /etc/java-8-openjdk
COPY --from=updated-jre /usr/lib/jvm/java-8-openjdk-amd64 /usr/lib/jvm/java-8-openjdk-amd64
COPY --from=updated-jre /usr/share/gdb/auto-load/usr/lib/jvm/java-8-openjdk-amd64 /usr/share/gdb/auto-load/usr/lib/jvm/java-8-openjdk-amd64

# Copy over dependencies for Apache FOP, missing from GCR's JRE
COPY --from=updated-jre /usr/lib/x86_64-linux-gnu/libfreetype.so.6 /usr/lib/x86_64-linux-gnu/libfreetype.so.6
COPY --from=updated-jre /usr/lib/x86_64-linux-gnu/liblcms2.so.2 /usr/lib/x86_64-linux-gnu/liblcms2.so.2
COPY --from=updated-jre /usr/lib/x86_64-linux-gnu/libpng16.so.16 /usr/lib/x86_64-linux-gnu/libpng16.so.16
COPY --from=updated-jre /usr/lib/x86_64-linux-gnu/libfontconfig.so.1 /usr/lib/x86_64-linux-gnu/libfontconfig.so.1

# Copy dependencies for Apache Batik (used by Apache FOP to handle SVG rendering)
COPY --from=updated-jre /etc/fonts /etc/fonts
COPY --from=updated-jre /lib/x86_64-linux-gnu/libexpat.so.1 /lib/x86_64-linux-gnu/libexpat.so.1
COPY --from=updated-jre /usr/share/fontconfig /usr/share/fontconfig
COPY --from=updated-jre /usr/share/fonts/truetype/dejavu /usr/share/fonts/truetype/dejavu

# Copy Elemental
COPY LICENSE /elemental/LICENSE
COPY autodeploy /elemental/autodeploy
COPY etc /elemental/etc
COPY lib /elemental/lib
COPY logs /elemental/logs


# Build-time metadata as defined at http://label-schema.org
# and used by autobuilder @hooks/build
LABEL org.label-schema.build-date=${maven.build.timestamp} \
      org.label-schema.description="Elemental - NoSQL Database" \
      org.label-schema.name="elemental" \
      org.label-schema.schema-version="1.0" \
      org.label-schema.url="${project.url}" \
      org.label-schema.vcs-ref=${build-commit-abbrev} \
      org.label-schema.vcs-url="${project.scm.url}" \
      org.label-schema.vendor="${project.organization.name}"

EXPOSE 8080 8443

# make CACHE_MEM, MAX_BROKER, and JVM_MAX_RAM_PERCENTAGE available to users
ARG CACHE_MEM
ARG MAX_BROKER
ARG JVM_MAX_RAM_PERCENTAGE

ENV ELEMENTAL_HOME "/elemental"
ENV EXIST_HOME "/elemental"
ENV CLASSPATH=/elemental/lib/${elemental.uber.jar.filename}

ENV JAVA_TOOL_OPTIONS \
  -Dfile.encoding=UTF8 \
  -Dsun.jnu.encoding=UTF-8 \
  -Djava.awt.headless=true \
  -Dorg.exist.db-connection.cacheSize=${CACHE_MEM:-256}M \
  -Dorg.exist.db-connection.pool.max=${MAX_BROKER:-20} \
  -Dlog4j.configurationFile=/elemental/etc/log4j2.xml \
  -Dexist.home=/elemental \
  -Dexist.configurationFile=/elemental/etc/conf.xml \
  -Djetty.home=/elemental \
  -Dexist.jetty.config=/elemental/etc/jetty/standard.enabled-jetty-configs \
  -XX:+UseG1GC \
  -XX:+UseStringDeduplication \
  -XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=${JVM_MAX_RAM_PERCENTAGE:-75.0} \
  -XX:+ExitOnOutOfMemoryError

HEALTHCHECK CMD [ "java", \
    "org.exist.start.Main", "client", \
    "--no-gui",  \
    "--user", "guest", "--password", "guest", \
    "--xpath", "system:get-version()" ]

ENTRYPOINT [ "java", \
    "org.exist.start.Main"]
CMD ["jetty" ]
