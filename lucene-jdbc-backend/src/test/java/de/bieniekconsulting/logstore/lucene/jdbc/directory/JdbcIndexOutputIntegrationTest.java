package de.bieniekconsulting.logstore.lucene.jdbc.directory;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.StatementCallback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import de.bieniekconsulting.logstore.lucene.jdbc.types.LuceneFileExtent;
import de.bieniekconsulting.springframework.support.TestConfiguration;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = JdbcIndexOutputIntegrationTest.TestConfig.class)
public class JdbcIndexOutputIntegrationTest {
	@Autowired
	private JdbcDirectory jdbcDirectory;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Before
	public void init() {
		jdbcTemplate.execute((StatementCallback<Object>) stmt -> {
			stmt.execute("delete from LUCENE_EXTENTS");
			return null;
		});

		jdbcTemplate.execute((StatementCallback<Object>) stmt -> {
			stmt.execute("delete from LUCENE_FILES");
			return null;
		});

		jdbcDirectory.setDirectoryName("outDir");
	}

	@Test
	public void shouldWriteFileWitoutExtent() throws IOException {
		assertThat(checkFileExists("file1")).isFalse();

		final JdbcIndexOutput jio = (JdbcIndexOutput) jdbcDirectory.createOutput("file1", null);

		jio.close();

		assertThat(checkFileExists("file1")).isTrue();
		assertThat(loadExtent(jio.getFileId(), 0)).isNotPresent();
	}

	@Test
	public void shouldWriteFileWithOneExtent() throws IOException {
		assertThat(checkFileExists("file2")).isFalse();

		final JdbcIndexOutput jio = (JdbcIndexOutput) jdbcDirectory.createOutput("file2", null);

		jio.writeBytes(createTestBuffer(1024), 1024);

		jio.close();

		assertThat(checkFileExists("file2")).isTrue();
		assertThat(loadExtent(jio.getFileId(), 0)).isPresent()
				.hasValueSatisfying(e -> assertThat(e.getExtentLength()).isEqualTo(1024));
	}

	@Test
	public void shouldWriteFileWithTwoExtents() throws IOException {
		final int length = JdbcFileManager.EXTENT_LENGTH + 2048;

		assertThat(checkFileExists("file3")).isFalse();

		final JdbcIndexOutput jio = (JdbcIndexOutput) jdbcDirectory.createOutput("file3", null);

		jio.writeBytes(createTestBuffer(length), length);

		jio.close();

		assertThat(checkFileExists("file3")).isTrue();
		assertThat(loadExtent(jio.getFileId(), 0)).isPresent()
				.hasValueSatisfying(e -> assertThat(e.getExtentLength()).isEqualTo(JdbcFileManager.EXTENT_LENGTH));
		assertThat(loadExtent(jio.getFileId(), 1)).isPresent()
				.hasValueSatisfying(e -> assertThat(e.getExtentLength()).isEqualTo(2048));
	}

	@Test
	public void shouldSyncFileWithOneExtent() throws IOException {
		assertThat(checkFileExists("file4")).isFalse();

		final JdbcIndexOutput jio = (JdbcIndexOutput) jdbcDirectory.createOutput("file4", null);

		jio.writeBytes(createTestBuffer(1024), 1024);

		jio.sync();
		assertThat(loadExtent(jio.getFileId(), 0)).isPresent()
				.hasValueSatisfying(e -> assertThat(e.getExtentLength()).isEqualTo(1024));

		jio.close();

		assertThat(loadExtent(jio.getFileId(), 0)).isPresent()
				.hasValueSatisfying(e -> assertThat(e.getExtentLength()).isEqualTo(1024));

		assertThat(checkFileExists("file4")).isTrue();
	}

	@Test
	public void shouldSyncAndWriteFileWithOneExtent() throws IOException {
		assertThat(checkFileExists("file4")).isFalse();

		final JdbcIndexOutput jio = (JdbcIndexOutput) jdbcDirectory.createOutput("file4", null);

		jio.writeBytes(createTestBuffer(1024), 1024);

		jio.sync();
		assertThat(loadExtent(jio.getFileId(), 0)).isPresent()
				.hasValueSatisfying(e -> assertThat(e.getExtentLength()).isEqualTo(1024));

		jio.writeBytes(createTestBuffer(1024), 1024);

		jio.close();

		assertThat(loadExtent(jio.getFileId(), 0)).isPresent()
				.hasValueSatisfying(e -> assertThat(e.getExtentLength()).isEqualTo(2048));

		assertThat(checkFileExists("file4")).isTrue();
	}

	@Test(expected = IOException.class)
	public void shouldFailWriteClosedFile() throws IOException {
		assertThat(checkFileExists("file2")).isFalse();

		final JdbcIndexOutput jio = (JdbcIndexOutput) jdbcDirectory.createOutput("file2", null);

		jio.close();

		jio.writeBytes(createTestBuffer(1024), 1024);
	}

	private byte[] createTestBuffer(final int length) {
		final byte[] data = new byte[length];

		for (int i = 0; i < length; i++) {
			data[i] = (byte) (i % 256 & 0x00ff);
		}

		return data;
	}

	private boolean checkFileExists(final String fileName) {
		return jdbcTemplate.execute((PreparedStatementCreator) con -> {
			final PreparedStatement ps = con
					.prepareStatement("select count(*) from LUCENE_FILES where DIRECTORY_NAME=? and FILE_NAME=?");

			ps.setString(1, jdbcDirectory.getDirectoryName());
			ps.setString(2, fileName);

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

	private Optional<LuceneFileExtent> loadExtent(final UUID fileId, final int extentIndex) {
		return jdbcTemplate.execute((PreparedStatementCreator) con -> {
			final PreparedStatement ps = con.prepareStatement(
					"select ID, FILE_ID, EXTENT_NUMBER, EXTENT_LENGTH, EXTENT_DATA from LUCENE_EXTENTS where FILE_ID=? and EXTENT_NUMBER=?");

			ps.setString(1, fileId.toString());
			ps.setInt(2, extentIndex);

			return ps;
		}, (PreparedStatementCallback<Optional<LuceneFileExtent>>) ps -> {
			ResultSet rs = null;

			try {
				rs = ps.executeQuery();

				if (rs.next()) {
					return Optional.of(LuceneFileExtent.builder().id(UUID.fromString(rs.getString(1)))
							.fileId(UUID.fromString(rs.getString(2))).extentNumber(rs.getInt(3))
							.extentLength(rs.getInt(4)).extentData(Base64.getDecoder().decode(rs.getString(5)))
							.build());
				} else {
					return Optional.empty();
				}
			} finally {
				if (rs != null) {
					rs.close();
				}
			}
		});
	}

	@TestConfiguration
	@Import({ LucenceConfiguration.class, CommonTestConfiguration.class })
	public static class TestConfig {
		@Bean
		@Autowired
		public JdbcTemplate jdbcTemplate(final DataSource dataSource) {
			return new JdbcTemplate(dataSource);
		}
	}

}
