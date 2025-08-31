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

### START: Container build time args for Elemental Server
##
# Names of the Linux user account and group to run the Elemental Server service under
ARG ELEMENTAL_SERVER_SERVICE_ACCOUNT="edb01"
ARG ELEMENTAL_SERVER_SERVICE_GROUP="edb01"

# Elemental data cache size
ARG ELEMENTAL_SERVER_CACHE_MEM="256"
# Elemental maximum number of database brokers
ARG ELEMENTAL_SERVER_MAX_BROKER="20"
##
### END: Container build time args for Elemental Server

### START: Container build time args for JVM (Java Virtual Machine)
##
# JVM maximum RAM use (as a percentage of RAM available to the Docker Container)
ARG JVM_MAX_RAM_PERCENTAGE="75.0"
# JVM Garbage Collector
ARG JVM_GC="G1"

# Any additional options to be added to the JAVA_TOOL_OPTIONS Environment Variable for the JVM
ARG ADDITIONAL_JAVA_TOOL_OPTIONS
##
### END: Container build time args for JVM (Java Virtual Machine)


# Install latest JRE 8 in Chainguard Wolfi temporary builder image
FROM cgr.dev/chainguard/wolfi-base AS builder

# Inherit global args to this build stage
ARG ELEMENTAL_SERVER_SERVICE_ACCOUNT
ARG ELEMENTAL_SERVER_SERVICE_GROUP

RUN apk update && apk upgrade
# Install dependencies needed for JRE
RUN apk add tzdata zlib libjpeg-turbo libpng lcms2 freetype ttf-dejavu fontconfig-config libfontconfig1 expat libuuid libbrotlicommon1 libbrotlidec1 libbrotlienc1 libcrypt1
# Install latest CA certificates
RUN apk add ca-certificates java-cacerts
# Install latest JRE
RUN apk add openjdk-8-jre

# Add Elemental Server service group and account
RUN addgroup -S ${ELEMENTAL_SERVER_SERVICE_GROUP} \
    && adduser -S -G ${ELEMENTAL_SERVER_SERVICE_GROUP} -H -h /nonexistent -s /sbin/nologin -g "Elemental Database Server - Instance 01" ${ELEMENTAL_SERVER_SERVICE_ACCOUNT}


# Use Chainguard distroless glibc base for dynamically linked libraries
FROM cgr.dev/chainguard/glibc-dynamic:latest

# Inherit global args to this build stage
ARG ELEMENTAL_SERVER_SERVICE_ACCOUNT
ARG ELEMENTAL_SERVER_SERVICE_GROUP
ARG ELEMENTAL_SERVER_CACHE_MEM
ARG ELEMENTAL_SERVER_MAX_BROKER
ARG JVM_MAX_RAM_PERCENTAGE
ARG JVM_GC
ARG ADDITIONAL_JAVA_TOOL_OPTIONS

# Copy over dependencies for updated JRE from Wolfi
COPY --from=builder /usr/share/zoneinfo /usr/share/zoneinfo
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

# Copy over certificates for updated JRE from Wolfi
COPY --from=builder /etc/ca-certificates /etc/ca-certificates
COPY --from=builder /etc/ca-certificates.conf /etc/ca-certificates.conf
COPY --from=builder /etc/apk/protected_paths.d/ca-certificates.list /etc/apk/protected_paths.d/ca-certificates.list
COPY --from=builder /etc/ssl /etc/ssl
COPY --from=builder /etc/pki /etc/pki
COPY --from=builder /usr/share/ca-certificates /usr/share/ca-certificates

# Copy over updated JRE from Wolfi
COPY --from=builder /usr/lib/jvm/java-1.8-openjdk /usr/lib/jvm/java-1.8-openjdk

# Copy Elemental Server service group and account
COPY --from=builder --chown=root:root --chmod=0644 /etc/passwd /etc/passwd
COPY --from=builder --chown=root:root --chmod=0644 /etc/group /etc/group
COPY --from=builder --chown=root:root --chmod=0600 /etc/shadow /etc/shadow

# Switch to Elemental Server service account
USER ${ELEMENTAL_SERVER_SERVICE_ACCOUNT}

# Copy Elemental
COPY --chown=${ELEMENTAL_SERVER_SERVICE_ACCOUNT}:${ELEMENTAL_SERVER_SERVICE_GROUP} --chmod=0555 logs /elemental
COPY --chown=${ELEMENTAL_SERVER_SERVICE_ACCOUNT}:${ELEMENTAL_SERVER_SERVICE_GROUP} --chmod=0444 LICENSE /elemental/LICENSE
COPY --chown=${ELEMENTAL_SERVER_SERVICE_ACCOUNT}:${ELEMENTAL_SERVER_SERVICE_GROUP} --chmod=0570 autodeploy /elemental/autodeploy
COPY --chown=${ELEMENTAL_SERVER_SERVICE_ACCOUNT}:${ELEMENTAL_SERVER_SERVICE_GROUP} --chmod=0570 etc /elemental/etc
COPY --chown=${ELEMENTAL_SERVER_SERVICE_ACCOUNT}:${ELEMENTAL_SERVER_SERVICE_GROUP} --chmod=0550 lib /elemental/lib
COPY --chown=${ELEMENTAL_SERVER_SERVICE_ACCOUNT}:${ELEMENTAL_SERVER_SERVICE_GROUP} --chmod=0750 logs /elemental/logs
COPY --chown=${ELEMENTAL_SERVER_SERVICE_ACCOUNT}:${ELEMENTAL_SERVER_SERVICE_GROUP} --chmod=0750 logs /elemental/data

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

ENV ELEMENTAL_HOME="/elemental"
ENV EXIST_HOME="/elemental"
ENV CLASSPATH="/elemental/lib/${elemental.uber.jar.filename}"

ENV JAVA_HOME="/usr/lib/jvm/java-1.8-openjdk"

ENV JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF8 -Dsun.jnu.encoding=UTF-8 -Djava.awt.headless=true -Dorg.exist.db-connection.cacheSize=${ELEMENTAL_SERVER_CACHE_MEM}M -Dorg.exist.db-connection.pool.max=${ELEMENTAL_SERVER_MAX_BROKER} -Dlog4j.configurationFile=/elemental/etc/log4j2.xml -Dexist.home=/elemental -Dexist.configurationFile=/elemental/etc/conf.xml -Djetty.home=/elemental -Dexist.jetty.config=/elemental/etc/jetty/standard.enabled-jetty-configs -XX:+Use${JVM_GC}GC -XX:+UseStringDeduplication -XX:+UseContainerSupport -XX:MaxRAMPercentage=${JVM_MAX_RAM_PERCENTAGE} -XX:+ExitOnOutOfMemoryError ${ADDITIONAL_JAVA_TOOL_OPTIONS}"

ENV PATH="/usr/lib/jvm/java-1.8-openjdk/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"

HEALTHCHECK CMD [ "java", \
    "org.exist.start.Main", "client", \
    "--no-gui",  \
    "--user", "guest", "--password", "guest", \
    "--xpath", "system:get-version()" ]

ENTRYPOINT [ "java", \
    "org.exist.start.Main"]
CMD ["jetty" ]
