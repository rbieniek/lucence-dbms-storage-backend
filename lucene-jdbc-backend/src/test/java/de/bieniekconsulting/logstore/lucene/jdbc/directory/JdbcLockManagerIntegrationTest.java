package de.bieniekconsulting.logstore.lucene.jdbc.directory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.StatementCallback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import de.bieniekconsulting.springframework.support.TestConfiguration;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = JdbcLockManagerIntegrationTest.TestConfig.class)
public class JdbcLockManagerIntegrationTest {

	@Autowired
	private JdbcLockManager lockManager;

	@Autowired
	private DataSource dataSource;

	@Before
	public void init() {
		new JdbcTemplate(dataSource).execute((StatementCallback<Object>) stmt -> {
			stmt.execute("delete from LUCENE_LOCKS");
			return null;
		});
	}

	@Test
	public void shouldObtainLock() throws Exception {
		final JdbcDirectory mockDirectory = mock(JdbcDirectory.class);

		when(mockDirectory.getDirectoryName()).thenReturn("dir1");

		final Optional<JdbcLock> lock = lockManager.obtainLock(mockDirectory, "lock1");

		assertThat(lock).isPresent().hasValueSatisfying(l -> assertThat(l).isInstanceOf(JdbcLock.class))
				.hasValueSatisfying(l -> assertThat(l.getUuid()).isInstanceOf(UUID.class));
	}

	@Test
	public void shouldFailOnObtainDuplicateLock() throws Exception {
		final JdbcDirectory mockDirectory = mock(JdbcDirectory.class);

		when(mockDirectory.getDirectoryName()).thenReturn("dir1");

		assertThat(lockManager.obtainLock(mockDirectory, "lock2")).isPresent();
		assertThat(lockManager.obtainLock(mockDirectory, "lock2")).isNotPresent();
	}

	@Test
	public void shouldVerifydObtainedLock() throws Exception {
		final JdbcDirectory mockDirectory = mock(JdbcDirectory.class);

		when(mockDirectory.getDirectoryName()).thenReturn("dir1");

		final Optional<JdbcLock> lock = lockManager.obtainLock(mockDirectory, "lock3");

		assertThat(lock).isPresent();
		assertThat(lockManager.isValid(lock.get())).isTrue();
	}

	@Test
	public void shouldNotVerifydUnknownLock() throws Exception {
		final JdbcLock mockLock = mock(JdbcLock.class);

		when(mockLock.getUuid()).thenReturn(UUID.randomUUID());

		assertThat(lockManager.isValid(mockLock)).isFalse();
	}

	@Test
	public void shouldReleaseObtainedLock() throws Exception {
		final JdbcDirectory mockDirectory = mock(JdbcDirectory.class);

		when(mockDirectory.getDirectoryName()).thenReturn("dir1");

		final Optional<JdbcLock> lock = lockManager.obtainLock(mockDirectory, "lock4");

		assertThat(lock).isPresent();
		assertThat(lockManager.releaseLock(lock.get())).isTrue();
	}

	@Test
	public void shouldFailOnDuplicateLockRelease() throws Exception {
		final JdbcDirectory mockDirectory = mock(JdbcDirectory.class);

		when(mockDirectory.getDirectoryName()).thenReturn("dir1");

		final JdbcLock lock = lockManager.obtainLock(mockDirectory, "lock4").get();

		assertThat(lockManager.releaseLock(lock)).isTrue();
		assertThat(lockManager.releaseLock(lock)).isFalse();
	}

	@TestConfiguration
	@Import({ LucenceConfiguration.class, CommonTestConfiguration.class })
	public static class TestConfig {

	}
}
