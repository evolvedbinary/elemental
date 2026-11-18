// TODO license

package org.exist.storage.xyz.elemental.otel;

import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import org.exist.storage.BrokerPool;
import org.exist.storage.BrokerPoolService;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import org.exist.storage.BrokerPoolServiceException;
import org.exist.util.Configuration;
import org.exist.util.Configuration.TelemetryExporter;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class OpenTelemetryBrokerPoolService implements BrokerPoolService {

    @Nullable private OpenTelemetrySdk openTelemetrySdk = null;

    private List<PeriodicMetricReader> periodicMetricReaderList = new ArrayList<>();

    @Override
    public void configure(final Configuration configuration) throws BrokerPoolServiceException {
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
        final String instanceId = brokerPool.getId();
        // safeguard when there's no enabled config
        if(this.periodicMetricReaderList.isEmpty()) {
            return;
        }

        final Resource resource = Resource.getDefault().merge(
                Resource.create(Attributes.of(
                        AttributeKey.stringKey("service.name"), instanceId)));

        final SdkMeterProviderBuilder meterProviderBuilder = SdkMeterProvider.builder().setResource(resource);


        for(PeriodicMetricReader periodicMetricReader : this.periodicMetricReaderList) {
            meterProviderBuilder.registerMetricReader(periodicMetricReader);
        }

        final SdkMeterProvider meterProvider = meterProviderBuilder.build();

        this.openTelemetrySdk = OpenTelemetrySdk.builder()
                .setMeterProvider(meterProvider)
                .buildAndRegisterGlobal();
    }

    @Override
    public void shutdown() {
        if (openTelemetrySdk != null) {
            openTelemetrySdk.close();
            openTelemetrySdk = null;
        }
    }
}
