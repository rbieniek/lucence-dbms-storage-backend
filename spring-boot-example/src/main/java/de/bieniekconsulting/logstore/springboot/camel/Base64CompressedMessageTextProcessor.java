package de.bieniekconsulting.logstore.springboot.camel;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.bieniekconsulting.logstore.springboot.components.Base64CompressedDecoder;
import de.bieniekconsulting.logstore.springboot.types.LogstoreMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor(onConstructor = @__({ @Autowired }))
@Slf4j
public class Base64CompressedMessageTextProcessor implements Processor {

	private final Base64CompressedDecoder decoder;

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
