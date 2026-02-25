package org.jboss.sbomer.sbom.service.adapter.out;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.sbomer.events.orchestration.RequestsFinished;
import org.jboss.sbomer.sbom.service.core.port.spi.RequestsFinishedNotifier;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class KafkaRequestsFinishedNotifier implements RequestsFinishedNotifier {

    @Inject
    @Channel("requests-finished")
    Emitter<RequestsFinished> emitter;

    @Override
    public void notify(RequestsFinished requestsFinishedEvent) {
        emitter.send(requestsFinishedEvent);
        log.info("requests.finished sent successfully to Kafka topic 'requests.finished' for request ID: {}", requestsFinishedEvent.getData().getRequestId());
    }
}
