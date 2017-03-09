package de.bieniekconsulting.logstore.persistence;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.bieniekconsulting.logstore.types.LogstoreMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class LogstoreService {

	private final LogstoreRepository repository;

	@Transactional
	public LogstoreRecord persistLogstoreMessage(final LogstoreMessage message) {
		log.info("saving log message '{}' to database", message);

		return repository.saveAndFlush(LogstoreRecord.builder().messageText(message.getMessageText())
				.timestamp(message.getTimestamp()).build());
	}

	@Transactional
	public Optional<LogstoreMessage> retrieveLogstoreMessage(final long id) {
		final LogstoreRecord record = repository.findOne(id);

		if (record != null) {
			return Optional.of(LogstoreMessage.builder().timestamp(record.getTimestamp())
					.messageText(record.getMessageText()).build());
		} else {
			return Optional.empty();
		}
	}
}
