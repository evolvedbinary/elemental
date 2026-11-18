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
    @Nullable private Boolean enableTelemetry;

    @Override
    public void configure(final Configuration configuration) throws BrokerPoolServiceException {
        this.enableTelemetry = configuration.getProperty("telemetry.exporters.export.enabled").equals(true);
        if(!this.enableTelemetry) {
            return;
        }
        // OTLP/gRPC (4317) and OTLP/HTTP (4318)
        // "http://localhost:4317"
        this.collectorUri = URI.create(configuration.getProperty("telemetry.exporters.export.collector-uri").toString());
        // Duration.ofSeconds(10)
        this.exportInterval = Duration.ofMillis((Long) configuration.getProperty("telemetry.exporters.export.interval"));
    }

    @Override
    public void prepare(final BrokerPool brokerPool) throws BrokerPoolServiceException {
        final String instanceId = brokerPool.getId();
        // safeguard when there's no enabled config
        if(!this.enableTelemetry) {
            return;
        }

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
