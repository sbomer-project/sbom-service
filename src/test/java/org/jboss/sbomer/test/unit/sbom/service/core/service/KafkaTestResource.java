package org.jboss.sbomer.test.unit.sbom.service.core.service;

import java.util.Map;

import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class KafkaTestResource implements QuarkusTestResourceLifecycleManager {
    private static final String FULL_IMAGE_NAME = "apache/kafka:latest";

    private KafkaContainer kafka;

    @Override
    public Map<String, String> start() {
        kafka = new KafkaContainer(DockerImageName.parse(FULL_IMAGE_NAME));
        kafka.start();
        return Map.of("kafka.bootstrap.servers", kafka.getBootstrapServers(), "kafka.apicurio.registry.auto-register", "false", "kafka.apicurio.registry.url", "");
    }

    @Override
    public void stop() {
        if (kafka != null) {
            kafka.stop();
        }
    }
}
