package de.bieniekconsulting.logstore.camel;

import java.util.Optional;

import org.apache.camel.Exchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import de.bieniekconsulting.jdbc.logstore.LogstoreRecord;
import de.bieniekconsulting.jdbc.logstore.LogstoreService;
import de.bieniekconsulting.logstore.types.LogstoreMessage;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RetrieveLogMessageEndpoint {

	private final LogstoreService logstoreService;

	public void retrieveLogMessage(final Exchange exchange) {
		final Long id = exchange.getIn().getHeader("id", Long.class);

		exchange.getOut().setBody(null);

		if (id != null) {
			final Optional<LogstoreRecord> record = logstoreService.retrieveLogstoreRecord(id);

			if (record.isPresent()) {
				exchange.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, HttpStatus.OK.value());
				exchange.getOut().setHeader(Exchange.HTTP_RESPONSE_TEXT, HttpStatus.OK.getReasonPhrase());
				exchange.getOut().setBody(LogstoreMessage.builder().messageText(record.get().getMessageText())
						.timestamp(record.get().getTimestamp()).build(), LogstoreMessage.class);
			} else {
				exchange.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, HttpStatus.NOT_FOUND.value());
				exchange.getOut().setHeader(Exchange.HTTP_RESPONSE_TEXT, HttpStatus.NOT_FOUND.getReasonPhrase());
				exchange.getOut().setBody(new byte[0]);
			}
		} else {
			exchange.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, HttpStatus.NOT_FOUND.value());
			exchange.getOut().setHeader(Exchange.HTTP_RESPONSE_TEXT, HttpStatus.NOT_FOUND.getReasonPhrase());
			exchange.getOut().setBody(new byte[0]);
		}

	}
}
