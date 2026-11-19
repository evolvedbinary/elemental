// TODO license

package org.exist.storage.xyz.elemental.otel;

import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.sdk.metrics.InstrumentSelector;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import io.opentelemetry.sdk.metrics.View;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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


import javax.annotation.Nullable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static io.opentelemetry.api.common.AttributeKey.stringKey;

public class OpenTelemetryBrokerPoolService implements BrokerPoolService {
    private static final Logger LOG = LogManager.getLogger(Configuration.class); //Logger


    @Nullable private OpenTelemetrySdk openTelemetrySdk = null;
    @Nullable private RuntimeTelemetry runtimeTelemetry = null;
    private TelemetryData telemetryData;

    private List<PeriodicMetricReader> periodicMetricReaderList = new ArrayList<>();

    @Override
    public void configure(final Configuration configuration) throws BrokerPoolServiceException {
        this.telemetryData = (TelemetryData) configuration.getProperty("telemetry");
        final List<TelemetryExporter> telemetryExporters = (List<TelemetryExporter>) configuration.getProperty("telemetry.exporters");

        if(telemetryExporters == null) {
            return;
        }
        // for each config of the list we need to build one
        for(TelemetryExporter exporter : telemetryExporters) {
            if(!exporter.enabled()) {
                continue;
            }
            //TODO choose the right  PeriodicMetricReader based on protocol
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
        // safeguard when there's no enabled config
        if(this.periodicMetricReaderList.isEmpty()) {
            return;
        }

        final String instanceId =  this.telemetryData.serviceName() == null ? brokerPool.getId() : this.telemetryData.serviceName();
        @Nullable String hostName;

        try {
            hostName =  this.telemetryData.hostName() == null ? InetAddress.getLocalHost().getHostName() : this.telemetryData.hostName();
        } catch (UnknownHostException e) {
            LOG.error("Unable to get the hostname", e.getMessage(), e);
            // this is the worst possible case.
            hostName = "no-host";
        }

        // Hostname -> host.name
        // development.enviremnt dev prod //todo read the docs
        //
        final Resource resource = Resource.getDefault().merge(
                // The limit is 6 Attributes be careful with it.
                Resource.create(Attributes.of(
                        stringKey("service.name"), instanceId,
                        stringKey("host.name"), hostName)));

        final SdkMeterProviderBuilder meterProviderBuilder = SdkMeterProvider.builder().setResource(resource);


        for(PeriodicMetricReader periodicMetricReader : this.periodicMetricReaderList) {
            meterProviderBuilder.registerMetricReader(periodicMetricReader);
        }

        // Register a view
        meterProviderBuilder.registerView(
                InstrumentSelector.builder().setName("elemental_version").build(),
                View.builder().build()
        );

        final SdkMeterProvider meterProvider = meterProviderBuilder.build();

        this.openTelemetrySdk = OpenTelemetrySdk.builder()
                .setMeterProvider(meterProvider)
                .buildAndRegisterGlobal();

        // register the JVM instruments
        this.runtimeTelemetry = RuntimeTelemetry.create(this.openTelemetrySdk);

        Meter meter = this.openTelemetrySdk.getMeter("org.exist.storage.xyz.elemental.otel");

        LongCounter counter = meter.counterBuilder("version_number").build();
        counter.add(1);

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
