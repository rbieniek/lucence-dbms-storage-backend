package de.bieniekconsulting.logstore.lucene.jdbc.directory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.UUID;

import org.apache.lucene.store.IndexInput;

import de.bieniekconsulting.logstore.lucene.jdbc.types.LuceneFileExtent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JdbcIndexInput extends IndexInput {

	private final JdbcFileManager fileManager;
	private final String directoryName;
	private final String fileName;

	private FilePointer filePointer;

	JdbcIndexInput(final JdbcFileManager fileManager, final String directoryName, final String fileName)
			throws IOException {
		super("JdbcIndexInput/" + directoryName + "/" + fileName);

		this.fileManager = fileManager;
		this.fileName = fileName;
		this.directoryName = directoryName;

		this.filePointer = new FilePointer(fileManager);
		this.filePointer.initializeFromFilename(directoryName, fileName);
	}

	private JdbcIndexInput(final JdbcFileManager fileManager, final String directoryName, final String fileName,
			final FilePointer fp) throws IOException {
		super("JdbcIndexInput/" + directoryName + "/" + fileName);

		this.fileManager = fileManager;
		this.fileName = fileName;
		this.directoryName = directoryName;

		this.filePointer = new FilePointer(fileManager);
		this.filePointer.initializeFromFilePointer(fp).seek(0L);
	}

	@Override
	public IndexInput clone() {
		try {
			return new JdbcIndexInput(fileManager, directoryName, fileName, filePointer);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void close() throws IOException {
		// intentionally left blank
	}

	@Override
	public long getFilePointer() {
		return filePointer.getCurrentPosition();
	}

	@Override
	public long length() {
		return filePointer.getFileLength();
	}

	@Override
	public IndexInput slice(final String sliceDescription, final long offset, final long length) throws IOException {
		log.info("slicing file id {} with offset {} and length {}", filePointer.getFileId(), offset, length);

		return new JdbcIndexInput(fileManager, directoryName, fileName,
				new FilePointer(fileManager).initializeFromFilePointer(filePointer).slice(offset, length).seek(0));
	}

	@Override
	public byte readByte() throws IOException {
		return filePointer.readByte();
	}

	@Override
	public void readBytes(final byte[] b, final int offset, final int len) throws IOException {
		if ((offset + len) > b.length) {
			throw new IOException();
		}

		for (int i = 0; i < len; i++) {
			b[offset + i] = readByte();
		}
	}

	@Override
	public void seek(final long pos) throws IOException {
		filePointer.seek(pos);
	}

	@Getter
	@Setter
	@RequiredArgsConstructor
	private static class FilePointer {
		private final JdbcFileManager fileManager;

		private UUID fileId;
		private UUID filePointerId = UUID.randomUUID();

		private SlidingWindow slidingWindow;
		private ByteBuffer dataBuffer;
		private int extentsForFile;

		public FilePointer initializeFromFilename(final String directoryName, final String fileName)
				throws IOException {
			setFileId(fileManager.findFile(directoryName, fileName).orElseThrow(() -> new FileNotFoundException(
					"cannot open file " + fileName + " in directory " + directoryName)));

			slidingWindow = new SlidingWindow(0, fileManager.fileLength(directoryName, fileName));

			setExtentsForFile(fileManager.extentsForFile(getFileId()));

			log.info("initialized file id {} file pointer id {} for file {} in directory {}", fileId, filePointerId,
					fileName, directoryName);

			loadCurrentExtent();

			return this;
		}

		public FilePointer initializeFromFilePointer(final FilePointer fp) throws IOException {
			log.info("initializing file pointer id {} from file pointer id {} for file {}", filePointerId,
					fp.getFilePointerId(), fp.getFileId());

			setFileId(fp.getFileId());
			slidingWindow = new SlidingWindow(fp.getSlidingWindow());

			setExtentsForFile(fp.getExtentsForFile());

			loadCurrentExtent();

			return this;
		}

		public long getCurrentPosition() {
			return slidingWindow.getPosition();
		}

		public FilePointer seek(final long position) throws IOException {
			log.info("seeking file id {} file pointer id {} to position {}", fileId, filePointerId, position);

			if (position < 0) {
				throw new IOException("cannot seek file " + fileId + " before beginning of file");
			}

			if (!slidingWindow.canSeek(position)) {
				throw new IOException("Attempt to seek file " + fileId + " beyond file length");
			}

			final SlidingWindow previousWindow = new SlidingWindow(slidingWindow);

			slidingWindow.newPosition(position);

			if (!slidingWindow.sameExtent(previousWindow)) {
				loadCurrentExtent();
			}
			getDataBuffer().position(slidingWindow.positionOfCurrentPosition());

			return this;
		}

		public FilePointer slice(final long offset, final long length) throws IOException {
			log.info("slicing file id {} file pointer id {} window {} to offset {} and length {}", fileId,
					filePointerId, slidingWindow, offset, length);

			slidingWindow = slidingWindow.slice(offset, length);

			return this;
		}

		public byte readByte() throws IOException {
			if (!slidingWindow.hasRemaining()) {
				throw new IOException("cannot read beyond end of file " + fileId);
			}

			if (!dataBuffer.hasRemaining()) {
				if (slidingWindow.extentOfCurrentPosition() >= extentsForFile) {
					throw new IOException("cannot read beyond end of file " + fileId);
				}

				loadCurrentExtent();
			}

			try {
				slidingWindow.incrementPosition();

				return dataBuffer.get();
			} catch (final BufferUnderflowException e) {
				throw new IOException(e);
			}
		}

		public long getFileLength() {
			return slidingWindow.getWindowSize();
		}

		private void loadCurrentExtent() throws IOException {
			log.info("loading extent {} for file id {} file pointer id {}", slidingWindow.extentOfCurrentPosition(),
					fileId, filePointerId);

			if (getExtentsForFile() > 0) {
				final LuceneFileExtent extent = fileManager
						.readExtent(getFileId(), slidingWindow.extentOfCurrentPosition())
						.orElseThrow(() -> new IOException("no data available for file " + fileId));
				setDataBuffer(ByteBuffer.wrap(extent.getExtentData()));
				getDataBuffer().position(slidingWindow.positionOfCurrentPosition());
			} else {
				setDataBuffer(ByteBuffer.allocate(0));
			}
		}

	}

	@Getter
	@RequiredArgsConstructor
	@ToString
	private static class SlidingWindow {
		private final long startOffset;
		private final long windowSize;
		private long position;

		public SlidingWindow(final SlidingWindow sw) {
			this.startOffset = sw.getStartOffset();
			this.windowSize = sw.getWindowSize();
		}

		public boolean hasRemaining() {
			return position < windowSize;
		}

		public boolean canSeek(final long newPosition) {
			return newPosition < windowSize;
		}

		public void incrementPosition() {
			position++;
		}

		public void newPosition(final long position) {
			this.position = position;
		}

		public int extentOfCurrentPosition() {
			return (int) (startOffset + position) / JdbcFileManager.EXTENT_LENGTH;
		}

		public int positionOfCurrentPosition() {
			return (int) (startOffset + position) % JdbcFileManager.EXTENT_LENGTH;
		}

		public SlidingWindow slice(final long offset, final long length) throws IOException {
			if (offset + length > windowSize) {
				throw new IOException("Cannot slice current with offset " + offset + " and length " + length
						+ " while window size is " + windowSize);
			}

			return new SlidingWindow(startOffset + offset, length);
		}

		public boolean sameExtent(final SlidingWindow previousWindow) {
			return extentOfCurrentPosition() == previousWindow.extentOfCurrentPosition();
		}
	}
}
