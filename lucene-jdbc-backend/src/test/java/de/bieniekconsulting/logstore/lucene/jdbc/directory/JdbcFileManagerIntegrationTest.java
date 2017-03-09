package de.bieniekconsulting.logstore.lucene.jdbc.directory;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
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

@ContextConfiguration(classes = JdbcFileManagerIntegrationTest.TestConfig.class)
public class JdbcFileManagerIntegrationTest {

	@Autowired
	private JdbcFileManager fileManager;

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
	}

	@Test
	public void shouldCreateFile() throws Exception {
		assertThat(checkFileExists("testDir", "file1")).isFalse();

		final Optional<UUID> uuid = fileManager.createFile("testDir", "file1");

		assertThat(uuid).isPresent();

		assertThat(checkFileExists("testDir", "file1")).isTrue();
	}

	@Test
	public void shouldFindFile() throws Exception {
		assertThat(checkFileExists("testDir", "file20")).isFalse();

		assertThat(fileManager.findFile("testDir", "file20")).isNotPresent();

		final Optional<UUID> uuid = fileManager.createFile("testDir", "file20");

		assertThat(uuid).isPresent();

		assertThat(checkFileExists("testDir", "file20")).isTrue();
		assertThat(fileManager.findFile("testDir", "file20")).isPresent()
				.hasValueSatisfying(u -> assertThat(u).isEqualTo(uuid.get()));
	}

	@Test
	public void shouldDeleteFile() throws Exception {
		assertThat(checkFileExists("testDir", "file2")).isFalse();

		final Optional<UUID> uuid = fileManager.createFile("testDir", "file2");

		assertThat(uuid).isPresent();

		assertThat(checkFileExists("testDir", "file2")).isTrue();

		fileManager.deleteFile("testDir", "file2");

		assertThat(checkFileExists("testDir", "file2")).isFalse();
	}

	@Test(expected = FileNotFoundException.class)
	public void shouldFailOnDeleteNonexistentFile() throws Exception {
		fileManager.deleteFile("testDir", "file3");
	}

	@Test
	public void shouldHaveZeroFileLenghtForFileWithoutExtents() throws Exception {
		assertThat(checkFileExists("testDir", "file5")).isFalse();

		final Optional<UUID> uuid = fileManager.createFile("testDir", "file5");

		assertThat(uuid).isPresent();

		assertThat(fileManager.fileLength("testDir", "file5")).isEqualTo(0);
	}

	@Test(expected = FileNotFoundException.class)
	public void shouldFailOnFileLengthOfNonexistentFile() throws Exception {
		fileManager.fileLength("testDir", "file4");
	}

	@Test
	public void shouldCreateFileAndWriteExtent() throws Exception {
		assertThat(checkFileExists("testDir", "file7")).isFalse();

		final Optional<UUID> uuid = fileManager.createFile("testDir", "file7");

		assertThat(uuid).isPresent();

		assertThat(checkIfExtentExists(uuid.get(), 1)).isFalse();

		final ByteBuffer buffer = ByteBuffer.allocate(4);

		buffer.put(new byte[] { 1, 2, 3, 4 });

		fileManager.writeExtent(uuid.get(), 1, buffer);

		assertThat(fileManager.extentsForFile(uuid.get())).isEqualTo(1);

		assertThat(loadExtent(uuid.get(), 1)).isPresent()
				.hasValueSatisfying(e -> assertThat(e.getFileId()).isEqualTo(uuid.get()))
				.hasValueSatisfying(e -> assertThat(e.getExtentLength()).isEqualTo(4))
				.hasValueSatisfying(e -> assertThat(e.getExtentNumber()).isEqualTo(1))
				.hasValueSatisfying(e -> assertThat(e.getExtentData()).isEqualTo(new byte[] { 1, 2, 3, 4 }));
	}

	@Test
	public void shouldCreateFileAndOverwriteExtent() throws Exception {
		assertThat(checkFileExists("testDir", "file8")).isFalse();

		final Optional<UUID> uuid = fileManager.createFile("testDir", "file8");

		assertThat(uuid).isPresent();

		assertThat(checkIfExtentExists(uuid.get(), 1)).isFalse();

		final ByteBuffer buffer1 = ByteBuffer.allocate(4);

		buffer1.put(new byte[] { 1, 2, 3, 4 });

		fileManager.writeExtent(uuid.get(), 1, buffer1);

		assertThat(loadExtent(uuid.get(), 1)).isPresent()
				.hasValueSatisfying(e -> assertThat(e.getFileId()).isEqualTo(uuid.get()))
				.hasValueSatisfying(e -> assertThat(e.getExtentLength()).isEqualTo(4))
				.hasValueSatisfying(e -> assertThat(e.getExtentNumber()).isEqualTo(1))
				.hasValueSatisfying(e -> assertThat(e.getExtentData()).isEqualTo(new byte[] { 1, 2, 3, 4 }));

		final ByteBuffer buffer2 = ByteBuffer.allocate(6);

		buffer2.put(new byte[] { 1, 2, 3, 4, 5, 6 });

		fileManager.writeExtent(uuid.get(), 1, buffer2);

		assertThat(loadExtent(uuid.get(), 1)).isPresent()
				.hasValueSatisfying(e -> assertThat(e.getFileId()).isEqualTo(uuid.get()))
				.hasValueSatisfying(e -> assertThat(e.getExtentLength()).isEqualTo(6))
				.hasValueSatisfying(e -> assertThat(e.getExtentNumber()).isEqualTo(1))
				.hasValueSatisfying(e -> assertThat(e.getExtentData()).isEqualTo(new byte[] { 1, 2, 3, 4, 5, 6 }));
	}

	@Test
	public void shouldGetFileLengthWithExtent() throws Exception {
		assertThat(checkFileExists("testDir", "file11")).isFalse();

		final Optional<UUID> uuid = fileManager.createFile("testDir", "file11");

		assertThat(uuid).isPresent();

		assertThat(checkIfExtentExists(uuid.get(), 1)).isFalse();

		final ByteBuffer buffer = ByteBuffer.allocate(4);

		buffer.put(new byte[] { 1, 2, 3, 4 });

		fileManager.writeExtent(uuid.get(), 1, buffer);

		assertThat(loadExtent(uuid.get(), 1)).isPresent()
				.hasValueSatisfying(e -> assertThat(e.getFileId()).isEqualTo(uuid.get()))
				.hasValueSatisfying(e -> assertThat(e.getExtentLength()).isEqualTo(4))
				.hasValueSatisfying(e -> assertThat(e.getExtentNumber()).isEqualTo(1))
				.hasValueSatisfying(e -> assertThat(e.getExtentData()).isEqualTo(new byte[] { 1, 2, 3, 4 }));

		assertThat(fileManager.fileLength("testDir", "file11")).isEqualTo(4);
	}

	@Test
	public void shouldRenameFile() throws Exception {
		assertThat(checkFileExists("testDir", "file9")).isFalse();

		final Optional<UUID> uuid = fileManager.createFile("testDir", "file9");

		assertThat(uuid).isPresent();

		assertThat(checkFileExists("testDir", "file9")).isTrue();

		fileManager.renameFile("testDir", "file9", "file10");
		assertThat(checkFileExists("testDir", "file9")).isFalse();
		assertThat(checkFileExists("testDir", "file10")).isTrue();
	}

	@Test(expected = FileNotFoundException.class)
	public void shouldFileOnRenameNonExistentFile() throws Exception {
		assertThat(checkFileExists("testDir", "file12")).isFalse();

		fileManager.renameFile("testDir", "file12", "file13");
	}

	@Test(expected = IOException.class)
	public void shouldFileOnRenameWithxisteningTargetFile() throws Exception {
		assertThat(checkFileExists("testDir", "file12")).isFalse();
		assertThat(checkFileExists("testDir", "file14")).isFalse();

		final Optional<UUID> uuid = fileManager.createFile("testDir", "file14");

		assertThat(uuid).isPresent();

		fileManager.renameFile("testDir", "file12", "file14");
	}

	@Test
	public void shouldCreateReadCreatedExtent() throws Exception {
		assertThat(checkFileExists("testDir", "file21")).isFalse();

		final Optional<UUID> uuid = fileManager.createFile("testDir", "file21");

		assertThat(uuid).isPresent();

		assertThat(checkIfExtentExists(uuid.get(), 1)).isFalse();

		final ByteBuffer buffer = ByteBuffer.allocate(4);

		buffer.put(new byte[] { 1, 2, 3, 4 });

		fileManager.writeExtent(uuid.get(), 1, buffer);

		assertThat(fileManager.readExtent(uuid.get(), 1)).isPresent()
				.hasValueSatisfying(e -> assertThat(e.getFileId()).isEqualTo(uuid.get()))
				.hasValueSatisfying(e -> assertThat(e.getExtentLength()).isEqualTo(4))
				.hasValueSatisfying(e -> assertThat(e.getExtentNumber()).isEqualTo(1))
				.hasValueSatisfying(e -> assertThat(e.getExtentData()).isEqualTo(new byte[] { 1, 2, 3, 4 }));
	}

	@Test
	public void shouldNotReadNotExistingExtent() throws Exception {
		assertThat(fileManager.readExtent(UUID.randomUUID(), 1)).isNotPresent();
	}

	private boolean checkFileExists(final String directoryName, final String fileName) {
		return jdbcTemplate.execute((PreparedStatementCreator) con -> {
			final PreparedStatement ps = con
					.prepareStatement("select count(*) from LUCENE_FILES where DIRECTORY_NAME=? and FILE_NAME=?");

			ps.setString(1, directoryName);
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

	private boolean checkIfExtentExists(final UUID fileId, final int extentIndex) {
		return jdbcTemplate.execute((PreparedStatementCreator) con -> {
			final PreparedStatement ps = con
					.prepareStatement("select count(*) from LUCENE_EXTENTS where FILE_ID=? and EXTENT_NUMBER=?");

			ps.setString(1, fileId.toString());
			ps.setInt(2, extentIndex);

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
