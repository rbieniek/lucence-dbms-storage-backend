package de.bieniekconsulting.logstore.camel;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.bieniekconsulting.logstore.lucene.jdbc.directory.LucenceService;
import de.bieniekconsulting.logstore.lucene.jdbc.types.LogRecord;
import de.bieniekconsulting.logstore.persistence.LogstoreRecord;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor(onConstructor = @__({ @Autowired }))
public class IndexLogstoreRecordProcessor implements Processor {

	private final LucenceService service;

	@Override
	public void process(final Exchange exchange) throws Exception {
		final LogstoreRecord record = exchange.getIn().getBody(LogstoreRecord.class);

		service.indexLogRecord(LogRecord.builder().id(record.getId()).messageText(record.getMessageText()).build());
	}

}
