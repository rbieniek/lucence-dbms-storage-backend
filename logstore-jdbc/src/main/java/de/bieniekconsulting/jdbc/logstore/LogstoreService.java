package de.bieniekconsulting.jdbc.logstore;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.bieniekconsulting.springcdi.bridge.api.SpringScoped;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@SpringScoped
public class LogstoreService implements InitializingBean {

	@Autowired
	private DataSource dataSource;

	private JdbcTemplate jdbcTemplate;

	@Transactional
	public LogstoreRecord persistLogstoreRecord(final LogstoreRecord message) {
		log.info("saving log message '{}' to database", message);

		final long id = nextId();

		jdbcTemplate.update(con -> {
			final PreparedStatement ps = con
					.prepareStatement("INSERT INTO LOGSTORE_RECORDS (id,timestamp,message_text) VALUES (?,?,?)");

			ps.setLong(1, id);
			ps.setLong(2, message.getTimestamp());
			ps.setString(3, message.getMessageText());

			return ps;
		});

		message.setId(id);

		return message;
	}

	@Transactional
	public Optional<LogstoreRecord> retrieveLogstoreRecord(final long id) {
		final List<LogstoreRecord> records = jdbcTemplate.query((PreparedStatementCreator) con -> {
			final PreparedStatement ps = con
					.prepareStatement("SELECT ID, TIMESTAMP, MESSAGE_TEXT from LOGSTORE_RECORDS where ID=?");

			ps.setLong(1, id);

			return ps;
		}, (RowMapper<LogstoreRecord>) (rs, rowNum) -> LogstoreRecord.builder().id(rs.getLong(1))
				.timestamp(rs.getLong(2)).messageText(rs.getString(3)).build());

		return Optional.ofNullable(records.isEmpty() ? null : records.get(0));
	}

	private long nextId() {
		return jdbcTemplate.query(
				(PreparedStatementCreator) con -> con.prepareStatement("SELECT MAX(id) FROM LOGSTORE_RECORDS"),
				(ResultSetExtractor<Long>) rs -> (rs.next() ? rs.getLong(1) + 1 : 0));
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}
}
