// TODO license

package xyz.elemental.otel;

import org.exist.storage.BrokerPoolService;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import org.exist.storage.BrokerPoolServiceException;
import org.exist.storage.DBBroker;
import org.exist.storage.txn.Txn;

import java.time.Duration;

public class OpenTelemetryBrokerPoolService implements BrokerPoolService {
    // TODO
        // Should be a single-user system service for the database
        // Should setup OpenTelemetry Java library and register SdkMeterProvider to export JVM runtime statistics

    public static OpenTelemetrySdk init() {
        Resource resource = Resource.getDefault().merge(
                Resource.create(Attributes.of(
                        AttributeKey.stringKey("service.name"), "OTEL")));

        SdkMeterProvider meterProvider = SdkMeterProvider.builder()
                .setResource(resource)
                .registerMetricReader(
                        PeriodicMetricReader.builder(
                                        OtlpGrpcMetricExporter.builder()
                                                // OTLP/gRPC (4317) and OTLP/HTTP (4318)
                                                .setEndpoint("http://localhost:4317")
                                                .build())
                                .setInterval(Duration.ofSeconds(10))
                                .build())
                .build();

        OpenTelemetrySdk sdk = OpenTelemetrySdk.builder()
                .setMeterProvider(meterProvider)
                .buildAndRegisterGlobal();

        Runtime.getRuntime().addShutdownHook(new Thread(sdk::close)); // flushes on exit
        return sdk;
    }

    @Override
    public void startSystem(DBBroker systemBroker, Txn transaction) throws BrokerPoolServiceException {
        OpenTelemetryBrokerPoolService.init();
    }
}
