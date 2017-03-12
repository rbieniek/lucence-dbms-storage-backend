package de.bieniekconsulting.logstore.wildfly.camel;

import javax.inject.Inject;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import de.bieniekconsulting.jdbc.logstore.LogstoreRecord;
import de.bieniekconsulting.jdbc.logstore.LogstoreService;
import de.bieniekconsulting.logstore.wildfly.types.LogstoreMessage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PersistLogstoreMessageProcessor implements Processor {

	@Inject
	private LogstoreService service;

	@Override
	public void process(final Exchange exchange) throws Exception {
		final LogstoreMessage message = exchange.getIn().getBody(LogstoreMessage.class);

		log.info("saving log message '{}' to database", message);

		final LogstoreRecord record = service.persistLogstoreRecord(LogstoreRecord.builder()
				.messageText(message.getMessageText()).timestamp(message.getTimestamp()).build());

		exchange.getIn().setBody(record);
	}

}
