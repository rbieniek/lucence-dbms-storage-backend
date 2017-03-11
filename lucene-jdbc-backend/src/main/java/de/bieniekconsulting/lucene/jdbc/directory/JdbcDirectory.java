package de.bieniekconsulting.lucene.jdbc.directory;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.lucene.index.IndexFileNames;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.Lock;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class JdbcDirectory extends Directory implements InitializingBean, DisposableBean {

	private final JdbcFileManager fileManager;
	private final JdbcFileRegistry fileRegistry;
	private final JdbcLockFactory lockFactory;

	@Getter
	@Setter
	private String directoryName;

	@Override
	public void afterPropertiesSet() throws Exception {
	}

	@Override
	public void destroy() throws Exception {
		close();
	}

	@Override
	public String[] listAll() throws IOException {
		final ArrayList<String> fileName = new ArrayList<>(
				fileManager.listFiles(directoryName).stream().map(p -> p.getRight()).collect(Collectors.toList()));

		return fileName.toArray(new String[0]);
	}

	@Override
	public void deleteFile(final String name) throws IOException {
		log.info("Delete file {} in directory {}", name, directoryName);

		fileManager.deleteFile(directoryName, name);
	}

	@Override
	public long fileLength(final String name) throws IOException {
		log.info("Getting length of file {} in directory {}", name, directoryName);

		return fileManager.fileLength(directoryName, name);
	}

	@Override
	public IndexOutput createOutput(final String name, final IOContext context) throws IOException {
		log.info("Create file {} in directory {} with context {}", name, directoryName, context);

		final Optional<UUID> fileId = fileManager.createFile(directoryName, name);

		if (!fileId.isPresent()) {
			throw new FileAlreadyExistsException(name);
		}

		return new JdbcIndexOutput(fileManager, fileRegistry, fileId.get(), directoryName, name);
	}

	@Override
	public IndexOutput createTempOutput(final String prefix, final String suffix, final IOContext context)
			throws IOException {
		log.info("Create file with prefix {} and suffix {} in directory {} with context {}", prefix, suffix,
				directoryName, context);

		final String name = IndexFileNames.segmentFileName(prefix, suffix + "_" + UUID.randomUUID().toString(), "tmp");

		log.info("Create file {} in directory {} with context {}", name, directoryName, context);

		final Optional<UUID> fileId = fileManager.createFile(directoryName, name);

		return new JdbcIndexOutput(fileManager, fileRegistry, fileId.get(), directoryName, name);
	}

	@Override
	public void sync(final Collection<String> names) throws IOException {
		for (final JdbcIndexOutput joi : fileRegistry.openFiles(v -> names.contains(v.getRight()))) {
			joi.sync();
		}
	}

	@Override
	public void rename(final String source, final String dest) throws IOException {
		log.info("Rename file {} to {} in directory {} with context {}", source, dest, directoryName);

		fileManager.renameFile(directoryName, source, dest);
	}

	@Override
	public void syncMetaData() throws IOException {
		// Intentionally left blank
	}

	@Override
	public IndexInput openInput(final String name, final IOContext context) throws IOException {
		log.info("Opening file {} in directory {} with context {}", name, directoryName, context);

		return new JdbcIndexInput(fileManager, directoryName, name);
	}

	@Override
	public Lock obtainLock(final String name) throws IOException {
		log.info("Obtain lock {} in directory {}", name, directoryName);

		return lockFactory.obtainLock(this, name);
	}

	@Override
	public void close() throws IOException {
		log.info("Closing directory {}", directoryName);

		fileRegistry.openFiles().forEach(file -> {
			try {
				file.close();
			} catch (final IOException e) {
				log.info("failed to close file {} in directory {}", file.getName(), directoryName, e);
			}
		});

		lockFactory.releaseAllDirectoryLocks(this);
	}

}
