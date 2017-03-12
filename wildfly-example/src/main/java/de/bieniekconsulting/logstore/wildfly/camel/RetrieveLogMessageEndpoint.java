package de.bieniekconsulting.logstore.wildfly.camel;

import java.util.Optional;

import javax.inject.Inject;

import org.apache.camel.Exchange;
import org.apache.http.HttpStatus;

import de.bieniekconsulting.jdbc.logstore.LogstoreRecord;
import de.bieniekconsulting.jdbc.logstore.LogstoreService;
import de.bieniekconsulting.logstore.wildfly.types.LogstoreMessage;

public class RetrieveLogMessageEndpoint {

	@Inject
	private LogstoreService logstoreService;

	public void retrieveLogMessage(final Exchange exchange) {
		final Long id = exchange.getIn().getHeader("id", Long.class);

		exchange.getOut().setBody(null);

		if (id != null) {
			final Optional<LogstoreRecord> record = logstoreService.retrieveLogstoreRecord(id);

			if (record.isPresent()) {
				exchange.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, HttpStatus.SC_OK);
				exchange.getOut().setBody(LogstoreMessage.builder().messageText(record.get().getMessageText())
						.timestamp(record.get().getTimestamp()).build(), LogstoreMessage.class);
			} else {
				exchange.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, HttpStatus.SC_NOT_FOUND);
				exchange.getOut().setBody(new byte[0]);
			}
		} else {
			exchange.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, HttpStatus.SC_NOT_FOUND);
			exchange.getOut().setBody(new byte[0]);
		}

	}
}
