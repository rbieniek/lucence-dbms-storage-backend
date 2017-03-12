package de.bieniekconsulting.logstore.wildfly.camel;

import javax.inject.Inject;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.lang3.StringUtils;

import de.bieniekconsulting.logstore.wildfly.beans.Base64CompressedDecoder;
import de.bieniekconsulting.logstore.wildfly.types.LogstoreMessage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Base64CompressedMessageTextProcessor implements Processor {

	@Inject
	private Base64CompressedDecoder decoder;

	@Override
	public void process(final Exchange exchange) throws Exception {
		final LogstoreMessage message = exchange.getIn().getBody(LogstoreMessage.class);

		log.info("checking message '{}' for compressed message text", message);

		if (StringUtils.isNotBlank(message.getCompressedEncodedMessageText())) {
			message.setMessageText(decoder.decodeText(message.getCompressedEncodedMessageText()));
			message.setCompressedEncodedMessageText(null);
		}
	}

}
