package de.bieniekconsulting.logstore.lucene;

import java.io.IOException;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.Lock;
import org.apache.lucene.store.LockFactory;
import org.apache.lucene.store.LockObtainFailedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class JdbcLockFactory extends LockFactory {
	private final JdbcLockManager lockManager;

	@Override
	public Lock obtainLock(final Directory dir, final String lockName) throws IOException {
		return lockManager.obtainLock((JdbcDirectory) dir, lockName).orElseThrow(() -> new LockObtainFailedException(
				"lock " + lockName + " already exists in directory " + ((JdbcDirectory) dir).getDirectoryName()));
	}

	public void releaseAllDirectoryLocks(final JdbcDirectory dir) throws IOException {
		lockManager.releaseAllLocks(dir);
	}
}
