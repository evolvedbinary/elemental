// TODO license

package org.exist.storage.xyz.elemental.otel;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.metrics.DoubleCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableLongMeasurement;
import io.opentelemetry.exporter.logging.LoggingMetricExporter;
import io.opentelemetry.instrumentation.log4j.appender.v2_17.OpenTelemetryAppender;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exist.Version;
import org.exist.repo.ExistRepository;
import org.exist.storage.BrokerPool;
import org.exist.storage.BrokerPoolService;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.instrumentation.runtimetelemetry.RuntimeTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import org.exist.storage.BrokerPoolServiceException;
import org.exist.util.Configuration;
import org.exist.util.Configuration.TelemetryExporter;
import org.exist.util.Configuration.TelemetryData;
import org.exist.SystemProperties;
import org.expath.pkg.repo.Packages;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static io.opentelemetry.api.common.AttributeKey.stringKey;

public class OpenTelemetryBrokerPoolService implements BrokerPoolService {
    private static final Logger LOG = LogManager.getLogger(Configuration.class); //Logger

    // org.exist.util.Configuration properties
    public final static String SERVICE_NAME = "service-name";
    public final static String HOST_NAME = "host-name";
    public final static String TELEMETRY_ELEMENT_NAME = "telemetry";
    public final static String TELEMETRY_EXPORTERS_ELEMENT_NAME = "telemetry.exporters";
    public final static String EXPORTERS_ELEMENT_NAME = "exporters";
    public final static String EXPORT_ELEMENT_NAME = "export";
    public final static String PROTOCOL = "protocol";
    public final static String ENABLED = "enabled";
    public final static String INTERVAL = "interval";
    public final static String COLLECTOR_URI =  "collector-uri";

    @Nullable private OpenTelemetrySdk openTelemetrySdk = null;
    @Nullable private RuntimeTelemetry runtimeTelemetry = null;
    private TelemetryData telemetryData;

    private List<PeriodicMetricReader> periodicMetricReaderList = new ArrayList<>();

    @Override
    public void configure(final Configuration configuration) throws BrokerPoolServiceException {
        this.telemetryData = (TelemetryData) configuration.getProperty("telemetry");
        final List<TelemetryExporter> telemetryExporters = (List<TelemetryExporter>) configuration.getProperty("telemetry.exporters");

        if (telemetryExporters == null) {
            return;
        }
        // for each config of the list we need to build one
        for (TelemetryExporter exporter : telemetryExporters) {
            if (!exporter.enabled()) {
                continue;
            }
            //TODO(YB) choose the right  PeriodicMetricReader based on protocol
            final PeriodicMetricReader periodicMetricReader = PeriodicMetricReader.builder(
                            OtlpGrpcMetricExporter.builder()
                                    .setEndpoint(String.valueOf(exporter.collecctorUri()))
                                    .build())
                    .setInterval(exporter.interval())
                    .build();
            this.periodicMetricReaderList.add(periodicMetricReader);
        }


    }

    @Override
    public void prepare(final BrokerPool brokerPool) throws BrokerPoolServiceException {
        // Skip when there's nothing to export
        if (periodicMetricReaderList.isEmpty()) {
            return;
        }

        // if there is no service name configured, use the instance id (i.e. brokerPool id)
        if (telemetryData.serviceName() == null) {
            this.telemetryData = new TelemetryData(brokerPool.getId(), telemetryData.hostName());
        }

        // TODO(YB) development.enviremnt dev prod //todo read the docs

        // Load Resources
//        vendor=Evolved Binary Ltd
//        vendor-url=https://www.elemental.xyz
//        product-name=Elemental
//        product-version=${project.version}
//        product-build=${project.build.outputTimestamp}
//        git-branch=${build-version}
//        git-commit=${build-commit}
//        git-commit-timestamp=${build-tstamp}
//        exist-db-expath-pkg-compatible-version=${exist-db.expath.pkg.compatible.version}
        // TODO(YB) The resources should be built in a new function/class
        Attributes attributes = Attributes.builder()
                .put(stringKey("service.name"), telemetryData.serviceName())
                .put(stringKey("host.name"), telemetryData.hostName())
                .put(stringKey("service.version"), Version.getVersion() + "; " + Version.getGitCommit())
                .put(stringKey("Java.version"), "21")
                .put(stringKey("elemental.git.commit"), SystemProperties.getInstance().getSystemProperty("git-commit"))
                .put(stringKey("elemental.vendor"), SystemProperties.getInstance().getSystemProperty("vendor"))
                .build();
        final Resource resource = Resource.getDefault().merge(
                Resource.create(attributes));

        final SdkMeterProviderBuilder meterProviderBuilder = SdkMeterProvider.builder().setResource(resource);

        for (PeriodicMetricReader periodicMetricReader : periodicMetricReaderList) {
            meterProviderBuilder.registerMetricReader(periodicMetricReader);
        }

        final SdkMeterProvider meterProvider = meterProviderBuilder.build();

        this.openTelemetrySdk = OpenTelemetrySdk.builder()
                .setMeterProvider(meterProvider)
                .buildAndRegisterGlobal();

        // register the JVM instruments
        this.runtimeTelemetry = RuntimeTelemetry.create(this.openTelemetrySdk);

//        OpenTelemetryAppender.install(openTelemetrySdk);

        // Send some metrics and they will hold all of the resources attributes

        Meter meter = openTelemetrySdk.getMeter("xyz.elemental.otel");
        meter.gaugeBuilder("app.info")
                .ofLongs()
                .setDescription("1 per running instance")
                .buildWithCallback(m -> m.record(1));


        ObservableLongMeasurement expathPackagesCounter = meter.gaugeBuilder("expath.number")
                .ofLongs()
                .setDescription("Number of installed Expath packages")
                .buildObserver();
        expathPackagesCounter.record(1);
        expathPackagesCounter.record(2);
        expathPackagesCounter.record(3);
        expathPackagesCounter.record(4);

        // How do we get installed packages? are we even in a stage that they are installed already?
        // we need the package info as well
        // maybe we add it to org/expath/pkg/repo/Repository.java
        // so when ever you install a new package it gets picked up
        // org.exist.repo.Deployment.installAndDeploy(org.exist.storage.DBBroker, org.exist.storage.txn.Txn, org.expath.pkg.repo.XarSource, org.exist.repo.PackageLoader, boolean)
        // seems a better place, it's in the same package and we don't have to introduce any new dependency



    }


    @Override
    public void shutdown() {
        if (runtimeTelemetry != null) {
            runtimeTelemetry.close();
            runtimeTelemetry = null;
        }
        if (openTelemetrySdk != null) {
            openTelemetrySdk.close();
            openTelemetrySdk = null;
        }
    }
}
