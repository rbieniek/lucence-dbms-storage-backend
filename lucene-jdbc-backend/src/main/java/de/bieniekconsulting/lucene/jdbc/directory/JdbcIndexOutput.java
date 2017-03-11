package de.bieniekconsulting.lucene.jdbc.directory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import org.apache.lucene.store.IndexOutput;

import lombok.Getter;

public class JdbcIndexOutput extends IndexOutput {

	private final JdbcFileManager fileManager;
	private final JdbcFileRegistry fileRegistry;

	@Getter
	private final UUID fileId;

	private Checksum checksum = new CRC32();
	private ByteBuffer dataBuffer;
	private int extentIndex = 0;
	@Getter
	private boolean open;

	JdbcIndexOutput(final JdbcFileManager fileManager, final JdbcFileRegistry fileRegistry, final UUID fileId,
			final String directoryName, final String filename) {
		super("JdbcIndexOutput/" + directoryName + "/" + filename, filename);

		this.fileManager = fileManager;
		this.fileId = fileId;
		this.fileRegistry = fileRegistry;

		fileRegistry.registerOpenFile(this);

		dataBuffer = ByteBuffer.allocate(JdbcFileManager.EXTENT_LENGTH);
		open = true;
	}

	@Override
	public void close() throws IOException {
		ensureOpen();
		syncBuffer(dataBuffer);

		open = false;
		fileRegistry.unregisterOpenFile(this);
	}

	@Override
	public long getFilePointer() {
		return (extentIndex * JdbcFileManager.EXTENT_LENGTH) + dataBuffer.position();
	}

	@Override
	public long getChecksum() throws IOException {
		return checksum.getValue();
	}

	public void sync() throws IOException {
		if (open) {
			syncBuffer(dataBuffer.duplicate());

		}
	}

	@Override
	public void writeByte(final byte b) throws IOException {
		ensureOpen();

		dataBuffer.put(b);
		checksum.update(b);

		if (!dataBuffer.hasRemaining()) {
			syncBuffer(dataBuffer);

			dataBuffer = ByteBuffer.allocate(JdbcFileManager.EXTENT_LENGTH);
			extentIndex++;
		}
	}

	@Override
	public void writeBytes(final byte[] b, final int offset, final int length) throws IOException {
		ensureOpen();

		try {
			for (int i = 0; i < length; i++) {
				writeByte(b[offset + i]);
			}
		} catch (final IndexOutOfBoundsException e) {
			throw new IOException(e);
		}
	}

	private void syncBuffer(final ByteBuffer buffer) throws IOException {
		if (buffer.position() > 0) {
			fileManager.writeExtent(fileId, extentIndex, buffer);
		}
	}

	private void ensureOpen() throws IOException {
		if (!open) {
			throw new IOException("file " + getName() + " already closed");
		}
	}
}
