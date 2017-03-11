package de.bieniekconsulting.lucene.jdbc.directory;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.StatementCallback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import de.bieniekconsulting.lucene.jdbc.directory.JdbcDirectory;
import de.bieniekconsulting.lucene.jdbc.directory.JdbcFileManager;
import de.bieniekconsulting.lucene.jdbc.directory.JdbcIndexInput;
import de.bieniekconsulting.lucene.jdbc.directory.JdbcIndexOutput;
import de.bieniekconsulting.lucene.jdbc.directory.LucenceConfiguration;
import de.bieniekconsulting.springframework.support.TestConfiguration;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = JdbcIndexInputIntegrationTest.TestConfig.class)
public class JdbcIndexInputIntegrationTest {
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
	public void shouldWriteAndReadFileWitoutExtent() throws IOException {
		final JdbcIndexOutput jio = (JdbcIndexOutput) jdbcDirectory.createOutput("file1", null);

		jio.close();

		final JdbcIndexInput jii = (JdbcIndexInput) jdbcDirectory.openInput("file1", null);

		assertThat(jii).isNotNull();
		assertThat(jii.getFilePointer()).isEqualTo(0);
		assertThat(jii.length()).isEqualTo(0);
	}

	@Test
	public void shouldWriteAndReadFileWithOneExtent() throws IOException {
		final JdbcIndexOutput jio = (JdbcIndexOutput) jdbcDirectory.createOutput("file2", null);

		jio.writeBytes(createTestBuffer(1024), 1024);

		jio.close();

		final JdbcIndexInput jii = (JdbcIndexInput) jdbcDirectory.openInput("file2", null);

		assertThat(jii).isNotNull();
		assertThat(jii.getFilePointer()).isEqualTo(0);
		assertThat(jii.length()).isEqualTo(1024);

		final byte[] data = new byte[1024];

		jii.readBytes(data, 0, data.length);

		assertThat(data).isEqualTo(createTestBuffer(1024));
		assertThat(jii.getFilePointer()).isEqualTo(1024);
		assertThat(jii.length()).isEqualTo(1024);
	}

	@Test
	public void shouldWriteAndReadFileWithTwoExtents() throws IOException {
		final int length = JdbcFileManager.EXTENT_LENGTH + 2048;

		final JdbcIndexOutput jio = (JdbcIndexOutput) jdbcDirectory.createOutput("file3", null);

		jio.writeBytes(createTestBuffer(length), length);

		jio.close();

		final JdbcIndexInput jii = (JdbcIndexInput) jdbcDirectory.openInput("file3", null);

		assertThat(jii).isNotNull();
		assertThat(jii.getFilePointer()).isEqualTo(0);
		assertThat(jii.length()).isEqualTo(length);

		final byte[] data = new byte[length];

		jii.readBytes(data, 0, data.length);

		assertThat(data).isEqualTo(createTestBuffer(length));
		assertThat(jii.getFilePointer()).isEqualTo(length);
		assertThat(jii.length()).isEqualTo(length);
	}

	@Test
	public void shouldWriteAndReadFileWithTwoExtentsAndSeek() throws IOException {
		final int length = JdbcFileManager.EXTENT_LENGTH + 2048;

		final JdbcIndexOutput jio = (JdbcIndexOutput) jdbcDirectory.createOutput("file5", null);

		jio.writeBytes(createTestBuffer(length), length);

		jio.close();

		final JdbcIndexInput jii = (JdbcIndexInput) jdbcDirectory.openInput("file5", null);

		assertThat(jii).isNotNull();
		assertThat(jii.getFilePointer()).isEqualTo(0);
		assertThat(jii.length()).isEqualTo(length);

		jii.seek(JdbcFileManager.EXTENT_LENGTH);

		final byte[] data = new byte[2048];

		jii.readBytes(data, 0, data.length);

		assertThat(data).isEqualTo(createTestBuffer(data.length));
		assertThat(jii.getFilePointer()).isEqualTo(length);
		assertThat(jii.length()).isEqualTo(length);
	}

	@Test
	public void shouldReadSlice() throws IOException {
		final JdbcIndexOutput jio = (JdbcIndexOutput) jdbcDirectory.createOutput("file2", null);

		jio.writeBytes(createTestBuffer(1024), 1024);

		jio.close();

		final JdbcIndexInput jii = (JdbcIndexInput) jdbcDirectory.openInput("file2", null);

		assertThat(jii).isNotNull();
		assertThat(jii.getFilePointer()).isEqualTo(0);
		assertThat(jii.length()).isEqualTo(1024);

		final JdbcIndexInput slice = (JdbcIndexInput) jii.slice("foo", 256, 512);

		assertThat(slice.getFilePointer()).isEqualTo(0);
		assertThat(slice.length()).isEqualTo(512);

		final byte[] data = new byte[512];

		slice.readBytes(data, 0, data.length);

		assertThat(data).isEqualTo(createTestBuffer(512));
		assertThat(slice.getFilePointer()).isEqualTo(512);
		assertThat(slice.length()).isEqualTo(512);
	}

	@Test
	public void shouldWriteAndReadFileClone() throws IOException {
		final JdbcIndexOutput jio = (JdbcIndexOutput) jdbcDirectory.createOutput("file2", null);

		jio.writeBytes(createTestBuffer(1024), 1024);

		jio.close();

		final JdbcIndexInput jii = (JdbcIndexInput) jdbcDirectory.openInput("file2", null);

		assertThat(jii).isNotNull();
		assertThat(jii.getFilePointer()).isEqualTo(0);
		assertThat(jii.length()).isEqualTo(1024);

		final byte[] data = new byte[1024];

		jii.readBytes(data, 0, data.length);

		assertThat(data).isEqualTo(createTestBuffer(1024));
		assertThat(jii.getFilePointer()).isEqualTo(1024);
		assertThat(jii.length()).isEqualTo(1024);

		final JdbcIndexInput clone = (JdbcIndexInput) jii.clone();

		assertThat(clone).isNotNull();
		assertThat(clone.getFilePointer()).isEqualTo(0);
		assertThat(clone.length()).isEqualTo(1024);

		clone.readBytes(data, 0, data.length);

		assertThat(data).isEqualTo(createTestBuffer(1024));
		assertThat(clone.getFilePointer()).isEqualTo(1024);
		assertThat(clone.length()).isEqualTo(1024);

	}

	private byte[] createTestBuffer(final int length) {
		final byte[] data = new byte[length];

		for (int i = 0; i < length; i++) {
			data[i] = (byte) (i % 256 & 0x00ff);
		}

		return data;
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
