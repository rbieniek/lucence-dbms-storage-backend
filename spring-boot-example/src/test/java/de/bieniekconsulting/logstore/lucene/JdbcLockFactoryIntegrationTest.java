package de.bieniekconsulting.logstore.lucene;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.sql.DataSource;

import org.apache.lucene.store.Lock;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.LockReleaseFailedException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.StatementCallback;
import org.springframework.test.context.junit4.SpringRunner;

import de.bieniekconsulting.logstore.TestConfiguration;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.NONE, classes = JdbcLockFactoryIntegrationTest.TestConfig.class, properties = "spring.datasource.url=jdbc:h2:mem:JLFIT;DB_CLOSE_ON_EXIT=FALSE")
public class JdbcLockFactoryIntegrationTest {

	@Autowired
	private JdbcLockFactory lockFactory;

	@Autowired
	private DataSource dataSource;

	@Before
	public void init() {
		(new JdbcTemplate(dataSource)).execute((StatementCallback<Object>) stmt -> {
			stmt.execute("delete from LUCENE_LOCKS");
			return null;
		});
	}

	@Test
	public void shouldObtainLock() throws Exception {
		final JdbcDirectory mockDirectory = mock(JdbcDirectory.class);

		when(mockDirectory.getDirectoryName()).thenReturn("dir1");

		final Lock lock = lockFactory.obtainLock(mockDirectory, "lock1a");

		assertThat(lock).isNotNull();
	}

	@Test
	public void shouldVerifyObtainedLock() throws Exception {
		final JdbcDirectory mockDirectory = mock(JdbcDirectory.class);

		when(mockDirectory.getDirectoryName()).thenReturn("dir1");

		final Lock lock = lockFactory.obtainLock(mockDirectory, "lock1a");

		assertThat(lock).isNotNull();

		lock.ensureValid();
	}

	@Test(expected = LockObtainFailedException.class)
	public void shouldFailOnObtainDuplicateLock() throws Exception {
		final JdbcDirectory mockDirectory = mock(JdbcDirectory.class);

		when(mockDirectory.getDirectoryName()).thenReturn("dir1");

		assertThat(lockFactory.obtainLock(mockDirectory, "lock2a")).isNotNull();
		lockFactory.obtainLock(mockDirectory, "lock2a");
	}

	@Test
	public void shouldReleaseObtainedLock() throws Exception {
		final JdbcDirectory mockDirectory = mock(JdbcDirectory.class);

		when(mockDirectory.getDirectoryName()).thenReturn("dir1");

		final Lock lock = lockFactory.obtainLock(mockDirectory, "lock1a");

		assertThat(lock).isNotNull();

		lock.close();

		try {
			lock.ensureValid();
			fail();
		} catch (final IOException e) {
		}
	}

	@Test
	public void shouldFailDuplicateLockRelease() throws Exception {
		final JdbcDirectory mockDirectory = mock(JdbcDirectory.class);

		when(mockDirectory.getDirectoryName()).thenReturn("dir1");

		final Lock lock = lockFactory.obtainLock(mockDirectory, "lock1a");

		assertThat(lock).isNotNull();

		lock.close();

		try {
			lock.close();
			fail();
		} catch (final LockReleaseFailedException e) {
		}
	}

	@TestConfiguration
	@Import(LucenceConfiguration.class)
	@EnableAutoConfiguration
	public static class TestConfig {

	}
}
