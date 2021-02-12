package ru.stqa.jaeger;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.exporter.jaeger.thrift.JaegerThriftSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.junit.jupiter.api.Test;

import java.time.Duration;

public class MyTest {

  @Test
  void myTest() {
    ManagedChannel jaegerChannel =
      ManagedChannelBuilder.forAddress("localhost", 14250).build();
    JaegerGrpcSpanExporter exporter =
      JaegerGrpcSpanExporter.builder()
        .setChannel(jaegerChannel)
        .setTimeout(Duration.ofMillis(30000))
        .build();

//    JaegerThriftSpanExporter exporter =
//      JaegerThriftSpanExporter.builder()
//        .setEndpoint("http://localhost:14268/api/traces")
//        .build();

    SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
      .addSpanProcessor(SimpleSpanProcessor.create(exporter))
      .build();

    OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
      .setTracerProvider(sdkTracerProvider)
      .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
      .buildAndRegisterGlobal();

    Tracer tracer =
      openTelemetry.getTracer("instrumentation-library-name", "1.0.0");

    for (int i = 0; i < 10; i++) {
      Span span = tracer.spanBuilder("my span").startSpan();
      try (Scope scope = span.makeCurrent()) {
        System.out.println("Hello, world!");
      } catch (Throwable t) {
        span.setStatus(StatusCode.ERROR, "Change it to your error message");
      } finally {
        span.end();
      }
    }

    sdkTracerProvider.shutdown();
  }
}
