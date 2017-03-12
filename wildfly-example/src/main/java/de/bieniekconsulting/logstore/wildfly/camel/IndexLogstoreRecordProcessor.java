package de.bieniekconsulting.logstore.wildfly.camel;

import javax.inject.Inject;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import de.bieniekconsulting.jdbc.logstore.LogstoreRecord;
import de.bieniekconsulting.lucene.jdbc.directory.LucenceService;
import de.bieniekconsulting.lucene.jdbc.types.LogRecord;

public class IndexLogstoreRecordProcessor implements Processor {

	@Inject
	private LucenceService service;

	@Override
	public void process(final Exchange exchange) throws Exception {
		final LogstoreRecord record = exchange.getIn().getBody(LogstoreRecord.class);

		service.indexLogRecord(LogRecord.builder().id(record.getId()).messageText(record.getMessageText()).build());
	}

}
