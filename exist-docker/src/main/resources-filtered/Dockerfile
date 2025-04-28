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
