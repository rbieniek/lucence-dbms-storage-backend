package de.bieniekconsulting.logstore.lucene.jdbc.directory;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class JdbcLockManager implements InitializingBean {
	private final DataSource dataSource;

	private SimpleJdbcInsert insert;
	private JdbcTemplate jdbcTemplate;

	@Override
	public void afterPropertiesSet() throws Exception {
		insert = new SimpleJdbcInsert(dataSource).withTableName("LUCENE_LOCKS");
		jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Transactional
	public Optional<JdbcLock> obtainLock(final JdbcDirectory dir, final String lockName) throws IOException {
		if (checkIfLockExists(dir.getDirectoryName(), lockName)) {
			return Optional.empty();
		}

		final UUID uuid = UUID.randomUUID();

		final SqlParameterSource parameters = new MapSqlParameterSource()
				.addValue("DIRECTORY_NAME", dir.getDirectoryName()).addValue("LOCK_NAME", lockName)
				.addValue("ID", uuid.toString());

		try {
			insert.execute(parameters);
		} catch (final DataAccessException e) {
			log.info("cannot create lock file {} in directoy {}", lockName, dir.getDirectoryName(), e);

			throw new IOException(e);
		}

		return Optional.of(new JdbcLock(uuid, this, dir.getDirectoryName(), lockName));
	}

	@Transactional
	public boolean releaseLock(final JdbcLock jdbcLock) throws IOException {
		try {
			return jdbcTemplate.execute((PreparedStatementCreator) con -> {
				final PreparedStatement ps = con.prepareStatement("delete from LUCENE_LOCKS where ID=?");

				ps.setString(1, jdbcLock.getUuid().toString());

				return ps;
			}, (PreparedStatementCallback<Boolean>) ps -> {
				return ps.executeUpdate() > 0;
			});
		} catch (final DataAccessException e) {
			log.info("cannot close lock file {}", jdbcLock.getUuid(), e);

			throw new IOException(e);
		}
	}

	@Transactional
	public int releaseAllLocks(final JdbcDirectory dir) throws IOException {
		try {
			return jdbcTemplate.execute((PreparedStatementCreator) con -> {
				final PreparedStatement ps = con.prepareStatement("delete from LUCENE_LOCKS where DIRECTORY_NAME=?");

				ps.setString(1, dir.getDirectoryName());

				return ps;
			}, (PreparedStatementCallback<Integer>) ps -> {
				return ps.executeUpdate();
			});
		} catch (final DataAccessException e) {
			log.info("cannot release locks in directory {}", dir.getDirectoryName(), e);

			throw new IOException(e);
		}
	}

	@Transactional
	public boolean isValid(final JdbcLock jdbcLock) throws IOException {
		try {
			return checkIfLockExists(jdbcLock.getUuid());
		} catch (final DataAccessException e) {
			log.info("cannot check lock file {} for validity", jdbcLock.getUuid(), e);

			throw new IOException(e);
		}
	}

	private boolean checkIfLockExists(final String directoryName, final String lockName) {
		return jdbcTemplate.execute((PreparedStatementCreator) con -> {
			final PreparedStatement ps = con
					.prepareStatement("select count(*) from LUCENE_LOCKS where DIRECTORY_NAME=? and LOCK_NAME=?");

			ps.setString(1, directoryName);
			ps.setString(2, lockName);

			return ps;
		}, (PreparedStatementCallback<Boolean>) ps -> {
			ResultSet rs = null;

			try {
				rs = ps.executeQuery();

				if (rs.next()) {
					return rs.getInt(1) > 0;
				} else {
					return false;
				}
			} finally {
				if (rs != null) {
					rs.close();
				}
			}
		});
	}

	private boolean checkIfLockExists(final UUID id) {
		return jdbcTemplate.execute((PreparedStatementCreator) con -> {
			final PreparedStatement ps = con.prepareStatement("select count(*) from LUCENE_LOCKS where ID=?");

			ps.setString(1, id.toString());

			return ps;
		}, (PreparedStatementCallback<Boolean>) ps -> {
			ResultSet rs = null;

			try {
				rs = ps.executeQuery();

				if (rs.next()) {
					return rs.getInt(1) > 0;
				} else {
					return false;
				}
			} finally {
				if (rs != null) {
					rs.close();
				}
			}
		});
	}
}
