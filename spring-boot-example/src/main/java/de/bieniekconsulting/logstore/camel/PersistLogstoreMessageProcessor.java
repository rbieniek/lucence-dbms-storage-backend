package de.bieniekconsulting.logstore.camel;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.bieniekconsulting.logstore.persistence.LogstoreRecord;
import de.bieniekconsulting.logstore.persistence.LogstoreService;
import de.bieniekconsulting.logstore.types.LogstoreMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor(onConstructor = @__({ @Autowired }))
@Slf4j
public class PersistLogstoreMessageProcessor implements Processor {

	private final LogstoreService service;

	@Override
	public void process(final Exchange exchange) throws Exception {
		final LogstoreMessage message = exchange.getIn().getBody(LogstoreMessage.class);

		log.info("saving log message '{}' to database", message);

		final LogstoreRecord record = service.persistLogstoreMessage(message);

		exchange.getIn().setBody(record);
	}

}
