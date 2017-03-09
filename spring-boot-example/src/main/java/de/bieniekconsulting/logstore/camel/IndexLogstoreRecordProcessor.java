package de.bieniekconsulting.logstore.camel;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.bieniekconsulting.logstore.lucene.LucenceService;
import de.bieniekconsulting.logstore.persistence.LogstoreRecord;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor(onConstructor = @__({ @Autowired }))
public class IndexLogstoreRecordProcessor implements Processor {

	private final LucenceService service;

	@Override
	public void process(final Exchange exchange) throws Exception {
		service.indexLogstoreRecord(exchange.getIn().getBody(LogstoreRecord.class));
	}

}
