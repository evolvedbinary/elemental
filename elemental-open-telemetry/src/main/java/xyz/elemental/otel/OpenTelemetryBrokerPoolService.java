// TODO license

package xyz.elemental.otel;

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

import javax.annotation.Nullable;
import java.net.URI;
import java.time.Duration;

public class OpenTelemetryBrokerPoolService implements BrokerPoolService {

    @Nullable private OpenTelemetrySdk openTelemetrySdk = null;

    @Nullable private URI collectorUri = null;
    @Nullable private Duration exportInterval;

    @Override
    public void configure(final Configuration configuration) throws BrokerPoolServiceException {
        // OTLP/gRPC (4317) and OTLP/HTTP (4318)
        // "http://localhost:4317"
        this.collectorUri = configuration.getProperty();
        // Duration.ofSeconds(10)
        this.exportInterval = configuration.getProperty();
    }

    @Override
    public void prepare(final BrokerPool brokerPool) throws BrokerPoolServiceException {
        final String instanceId = brokerPool.getId();

        final Resource resource = Resource.getDefault().merge(
                Resource.create(Attributes.of(
                        AttributeKey.stringKey("service.name"), instanceId)));

        final SdkMeterProvider meterProvider = SdkMeterProvider.builder()
                .setResource(resource)
                .registerMetricReader(
                        PeriodicMetricReader.builder(
                                        OtlpGrpcMetricExporter.builder()
                                                .setEndpoint(collectorUri.toString())
                                                .build())
                                .setInterval(exportInterval)
                                .build())
                .build();

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
