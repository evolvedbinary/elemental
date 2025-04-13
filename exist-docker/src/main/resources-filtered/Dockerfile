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

# Install latest JRE 21 in Chainguard Wolfi temporary builder image
FROM cgr.dev/chainguard/wolfi-base AS builder

RUN apk update && apk upgrade
# Install dependencies needed for JRE
RUN apk add zlib libjpeg-turbo libpng lcms2 freetype ttf-dejavu fontconfig-config libfontconfig1 expat libuuid libbrotlicommon1 libbrotlidec1 libbrotlienc1 libcrypt1
# Install latest JRE
RUN apk add openjdk-21-jre

# Use Chainguard distroless glibc base for dynamically linked libraries
FROM cgr.dev/chainguard/glibc-dynamic:latest

# Copy over dependencies for updated JRE from Wolfi
COPY --from=builder /etc/ca-certificates /etc/ca-certificates
COPY --from=builder /etc/ca-certificates.conf /etc/ca-certificates.conf
COPY --from=builder /lib/libz.so.1 /lib/libz.so.1
COPY --from=builder /usr/lib/libjpeg.so.8 /usr/lib/libjpeg.so.8
COPY --from=builder /usr/lib/libturbojpeg.so.0 /usr/lib/libturbojpeg.so.0
COPY --from=builder /usr/lib/libpng16.so.16 /usr/lib/libpng16.so.16
COPY --from=builder /usr/lib/liblcms2.so.2 /usr/lib/liblcms2.so.2
COPY --from=builder /usr/lib/libfreetype.so.6 /usr/lib/libfreetype.so.6
COPY --from=builder /usr/share/fonts /usr/share/fonts
COPY --from=builder /etc/fonts /etc/fonts
COPY --from=builder /usr/share/fontconfig /usr/share/fontconfig
COPY --from=builder /usr/share/gettext /usr/share/gettext
COPY --from=builder /usr/share/xml /usr/share/xml
COPY --from=builder /usr/lib/libfontconfig.so.1 /usr/lib/libfontconfig.so.1
COPY --from=builder /usr/lib/libexpat.so.1 /usr/lib/libexpat.so.1
COPY --from=builder /usr/lib/libuuid.so.1 /usr/lib/libuuid.so.1
COPY --from=builder /usr/lib/libbrotlicommon.so.1 /usr/lib/libbrotlicommon.so.1
COPY --from=builder /usr/lib/libbrotlidec.so.1 /usr/lib/libbrotlidec.so.1
COPY --from=builder /usr/lib/libbrotlienc.so.1 /usr/lib/libbrotlienc.so.1

# Copy over updated JRE from Wolfi
COPY --from=builder /usr/lib/jvm/java-21-openjdk /usr/lib/jvm/java-21-openjdk

# Switch to nonroot user
USER nonroot

# Copy Elemental
COPY LICENSE /elemental/LICENSE
COPY autodeploy /elemental/autodeploy
COPY etc /elemental/etc
COPY lib /elemental/lib
COPY --chown=nonroot logs /elemental/logs
COPY --chown=nonroot logs /elemental/data

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

ENV ELEMENTAL_HOME="/elemental"
ENV EXIST_HOME="/elemental"
ENV CLASSPATH="/elemental/lib/${elemental.uber.jar.filename}"

ENV JAVA_HOME="/usr/lib/jvm/java-21-openjdk"

ENV JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF8 -Dsun.jnu.encoding=UTF-8 -Djava.awt.headless=true -Dorg.exist.db-connection.cacheSize=${CACHE_MEM:-256}M -Dorg.exist.db-connection.pool.max=${MAX_BROKER:-20} -Dlog4j.configurationFile=/elemental/etc/log4j2.xml -Dexist.home=/elemental -Dexist.configurationFile=/elemental/etc/conf.xml -Djetty.home=/elemental -Dexist.jetty.config=/elemental/etc/jetty/standard.enabled-jetty-configs -XX:+UseNUMA -XX:+UseZGC -XX:+UseContainerSupport -XX:MaxRAMPercentage=${JVM_MAX_RAM_PERCENTAGE:-75.0} -XX:+ExitOnOutOfMemoryError"

ENV PATH="/usr/lib/jvm/java-21-openjdk/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"

HEALTHCHECK CMD [ "java", \
    "org.exist.start.Main", "client", \
    "--no-gui",  \
    "--user", "guest", "--password", "guest", \
    "--xpath", "system:get-version()" ]

ENTRYPOINT [ "java", \
    "org.exist.start.Main"]
CMD ["jetty" ]
