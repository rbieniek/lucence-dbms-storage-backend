package de.bieniekconsulting.logstore.lucene;

import java.io.IOException;
import java.util.UUID;

import org.apache.lucene.store.Lock;
import org.apache.lucene.store.LockReleaseFailedException;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class JdbcLock extends Lock {

	@Getter
	private final UUID uuid;
	private final JdbcLockManager lockManager;
	private final String directoryName;
	private final String lockName;

	@Override
	public void close() throws IOException {
		if (!lockManager.releaseLock(this)) {
			throw new LockReleaseFailedException("cannot close lock " + lockName + " in directory " + directoryName);
		}
	}

	@Override
	public void ensureValid() throws IOException {
		if (!lockManager.isValid(this)) {
			throw new IOException("invalid lock " + lockName + " in directory " + directoryName);
		}
	}
}
