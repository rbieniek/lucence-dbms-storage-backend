package de.bieniekconsulting.logstore.lucene;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.bieniekconsulting.logstore.types.LuceneFileExtent;
import de.bieniekconsulting.logstore.types.Pair;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class JdbcFileManager implements InitializingBean {

	public static final int EXTENT_LENGTH = 262144;

	private final DataSource dataSource;

	private SimpleJdbcInsert filesInsert;
	private JdbcTemplate jdbcTemplate;

	@Override
	public void afterPropertiesSet() throws Exception {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
		this.filesInsert = new SimpleJdbcInsert(dataSource).withTableName("LUCENE_FILES");
	}

	@Transactional
	public Optional<UUID> createFile(final String directoryName, final String fileName) throws IOException {
		if (checkIfFileExists(directoryName, fileName)) {
			return Optional.empty();
		}

		final UUID uuid = UUID.randomUUID();

		try {
			filesInsert.execute(new MapSqlParameterSource().addValue("ID", uuid.toString())
					.addValue("DIRECTORY_NAME", directoryName).addValue("FILE_NAME", fileName));
		} catch (final DataAccessException e) {
			log.info("cannot create file {} in directory {} ", fileName, directoryName, e);

			throw new IOException(e);
		}

		log.info("Create file {} in directory {} with ID {}", fileName, directoryName, uuid);

		return Optional.of(uuid);
	}

	@Transactional
	public Optional<UUID> findFile(final String directoryName, final String fileName) throws IOException {
		return jdbcTemplate.execute((PreparedStatementCreator) con -> {
			final PreparedStatement ps = con
					.prepareStatement("select ID from LUCENE_FILES where DIRECTORY_NAME=? and FILE_NAME=?");

			ps.setString(1, directoryName);
			ps.setString(2, fileName);

			return ps;
		}, (PreparedStatementCallback<Optional<UUID>>) ps ->

		{
			ResultSet rs = null;

			try {
				rs = ps.executeQuery();

				if (rs.next()) {
					return Optional.of(UUID.fromString(rs.getString(1)));
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

	@Transactional
	public long fileLength(final String directoryName, final String fileName) throws IOException {
		if (!checkIfFileExists(directoryName, fileName)) {
			throw new FileNotFoundException("File " + fileName + " not found in directory " + directoryName);
		}

		try {
			return jdbcTemplate.execute((PreparedStatementCreator) con -> {
				final PreparedStatement ps = con.prepareStatement(
						"select sum(le.EXTENT_LENGTH) from LUCENE_FILES lf join LUCENE_EXTENTS le on lf.id=le.file_id where lf.DIRECTORY_NAME=? and lf.FILE_NAME=?");

				ps.setString(1, directoryName);
				ps.setString(2, fileName);

				return ps;
			}, (PreparedStatementCallback<Long>) ps -> {
				ResultSet rs = null;

				try {
					rs = ps.executeQuery();

					if (rs.next()) {
						return rs.getLong(1);
					} else {
						return 0L;
					}
				} finally {
					if (rs != null) {
						rs.close();
					}
				}
			});
		} catch (final DataAccessException e) {
			log.info("cannot create file {} in directory {} ", fileName, directoryName, e);

			throw new IOException(e);
		}
	}

	@Transactional
	public long fileLength(final UUID fileId) throws IOException {

		try {
			return jdbcTemplate.execute((PreparedStatementCreator) con -> {
				final PreparedStatement ps = con.prepareStatement(
						"select sum(le.EXTENT_LENGTH) from LUCENE_FILES lf join LUCENE_EXTENTS le on lf.id=le.file_id where lf.ID=?");

				ps.setString(1, fileId.toString());

				return ps;
			}, (PreparedStatementCallback<Long>) ps -> {
				ResultSet rs = null;

				try {
					rs = ps.executeQuery();

					if (rs.next()) {
						return rs.getLong(1);
					} else {
						return 0L;
					}
				} finally {
					if (rs != null) {
						rs.close();
					}
				}
			});
		} catch (final DataAccessException e) {
			log.info("cannot get file length for file ID {}", fileId, e);

			throw new IOException(e);
		}
	}

	@Transactional
	public List<Pair<UUID, String>> listFiles(final String directoryName) throws IOException {
		try {
			return jdbcTemplate.execute((PreparedStatementCreator) con -> {
				final PreparedStatement ps = con
						.prepareStatement("select ID, FILE_NAME from LUCENE_FILES where DIRECTORY_NAME=?");

				ps.setString(1, directoryName);

				return ps;
			}, (PreparedStatementCallback<List<Pair<UUID, String>>>) ps -> {
				ResultSet rs = null;

				try {
					rs = ps.executeQuery();

					final List<Pair<UUID, String>> files = new LinkedList<>();

					while (rs.next()) {
						files.add(Pair.<UUID, String>builder().left(UUID.fromString(rs.getString(1)))
								.right(rs.getString(2)).build());
					}

					return files;
				} finally {
					if (rs != null) {
						rs.close();
					}
				}
			});
		} catch (final DataAccessException e) {
			log.info("cannot list files in directory {}", directoryName, e);

			throw new IOException(e);
		}
	}

	@Transactional
	public void deleteFile(final String directoryName, final String fileName) throws IOException {
		try {
			final UUID fileId = jdbcTemplate.execute((PreparedStatementCreator) con -> {
				final PreparedStatement ps = con
						.prepareStatement("select ID from LUCENE_FILES where DIRECTORY_NAME=? and FILE_NAME=?");

				ps.setString(1, directoryName);
				ps.setString(2, fileName);

				return ps;
			}, (PreparedStatementCallback<Optional<UUID>>) ps -> {
				ResultSet rs = null;

				try {
					rs = ps.executeQuery();

					if (rs.next()) {
						return Optional.of(UUID.fromString(rs.getString(1)));
					} else {
						return Optional.empty();
					}
				} finally {
					if (rs != null) {
						rs.close();
					}
				}
			}).orElseThrow(
					() -> new FileNotFoundException("File " + fileName + " not found in directory " + directoryName));

			jdbcTemplate.execute((PreparedStatementCreator) con -> {
				final PreparedStatement ps = con.prepareStatement("delete from LUCENE_EXTENTS where FILE_ID=?");

				ps.setString(1, fileId.toString());

				return ps;
			}, (PreparedStatementCallback<Void>) ps -> {
				ps.executeUpdate();

				return null;
			});

			jdbcTemplate.execute((PreparedStatementCreator) con -> {
				final PreparedStatement ps = con.prepareStatement("delete from LUCENE_FILES where ID=?");

				ps.setString(1, fileId.toString());

				return ps;
			}, (PreparedStatementCallback<Void>) ps -> {
				ps.executeUpdate();

				return null;
			});
		} catch (final DataAccessException e) {
			throw new IOException("Cannot unlink file " + fileName + " in directory " + directoryName);
		}
	}

	@Transactional
	public void renameFile(final String directoryName, final String oldName, final String newName) throws IOException {
		try {
			final UUID fileId = jdbcTemplate.execute((PreparedStatementCreator) con -> {
				final PreparedStatement ps = con
						.prepareStatement("select ID from LUCENE_FILES where DIRECTORY_NAME=? and FILE_NAME=?");

				ps.setString(1, directoryName);
				ps.setString(2, oldName);

				return ps;
			}, (PreparedStatementCallback<Optional<UUID>>) ps -> {
				ResultSet rs = null;

				try {
					rs = ps.executeQuery();

					if (rs.next()) {
						return Optional.of(UUID.fromString(rs.getString(1)));
					} else {
						return Optional.empty();
					}
				} finally {
					if (rs != null) {
						rs.close();
					}
				}
			}).orElseThrow(
					() -> new FileNotFoundException("File " + oldName + " not found in directory " + directoryName));

			if (jdbcTemplate.execute((PreparedStatementCreator) con -> {
				final PreparedStatement ps = con
						.prepareStatement("select ID from LUCENE_FILES where DIRECTORY_NAME=? and FILE_NAME=?");

				ps.setString(1, directoryName);
				ps.setString(2, newName);

				return ps;
			}, (PreparedStatementCallback<Optional<UUID>>) ps -> {
				ResultSet rs = null;

				try {
					rs = ps.executeQuery();

					if (rs.next()) {
						return Optional.of(UUID.fromString(rs.getString(1)));
					} else {
						return Optional.empty();
					}
				} finally {
					if (rs != null) {
						rs.close();
					}
				}
			}).isPresent()) {
				throw new IOException("File " + newName + " already exists in directory " + directoryName);
			}

			jdbcTemplate.execute((PreparedStatementCreator) con -> {
				final PreparedStatement ps = con.prepareStatement("update LUCENE_FILES set FILE_NAME=? where ID=?");

				ps.setString(1, newName);
				ps.setString(2, fileId.toString());

				return ps;
			}, (PreparedStatementCallback<Void>) ps -> {
				ps.executeUpdate();

				return null;
			});
		} catch (final DataAccessException e) {
			throw new IOException("Cannot unlink file " + oldName + " in directory " + directoryName);
		}
	}

	@Transactional
	public void writeExtent(final UUID fileId, final int extentIndex, final ByteBuffer dataBuffer) throws IOException {
		dataBuffer.flip();

		final int extentLength = dataBuffer.limit();
		final byte[] data = new byte[extentLength];

		dataBuffer.get(data);

		log.info("writing extent {} for file {} with size {}", extentIndex, fileId, extentLength);

		try {
			final boolean extentExists = jdbcTemplate.execute((PreparedStatementCreator) con -> {
				final PreparedStatement ps = con
						.prepareStatement("select count(*) from LUCENE_EXTENTS where FILE_ID=? and EXTENT_NUMBER=?");

				ps.setString(1, fileId.toString());
				ps.setInt(2, extentIndex);

				return ps;
			}, (PreparedStatementCallback<Boolean>) ps -> {
				ps.execute();
				ResultSet rs = null;

				try {
					rs = ps.executeQuery();

					if (rs.next()) {
						return rs.getLong(1) > 0;
					} else {
						return false;
					}
				} finally {
					if (rs != null) {
						rs.close();
					}
				}
			});

			if (extentExists) {
				log.info("updating existing extent {} for file {} with size {}", extentIndex, fileId, extentLength);

				jdbcTemplate.execute((PreparedStatementCreator) con -> {
					final PreparedStatement ps = con.prepareStatement(
							"update LUCENE_EXTENTS set EXTENT_DATA=?, EXTENT_LENGTH=? where FILE_ID=? and EXTENT_NUMBER=?");

					ps.setString(1, encodeBinaryData(data));
					ps.setInt(2, extentLength);
					ps.setString(3, fileId.toString());
					ps.setInt(4, extentIndex);

					return ps;
				}, (PreparedStatementCallback<Integer>) ps -> {
					return ps.executeUpdate();
				});
			} else {
				log.info("creating new extent {} for file {} with size {}", extentIndex, fileId, extentLength);

				jdbcTemplate.execute((PreparedStatementCreator) con -> {
					final PreparedStatement ps = con.prepareStatement(
							"INSERT INTO LUCENE_EXTENTS (id, file_id, extent_number, extent_length, extent_data) VALUES(?, ?, ?, ?, ?)");

					ps.setString(1, UUID.randomUUID().toString());
					ps.setString(2, fileId.toString());
					ps.setInt(3, extentIndex);
					ps.setInt(4, extentLength);
					ps.setString(5, encodeBinaryData(data));

					return ps;
				}, (PreparedStatementCallback<Integer>) ps -> {
					return ps.executeUpdate();
				});
//				this.extentsInsert.execute(new MapSqlParameterSource().addValue("ID", UUID.randomUUID().toString())
//						.addValue("FILE_ID", fileId.toString()).addValue("EXTENT_NUMBER", extentIndex)
//						.addValue("EXTENT_LENGTH", extentLength).addValue("EXTENT_DATA", data));
			}
		} catch (final DataAccessException e) {
			log.info("cannot write file {} extent {}", fileId, extentIndex, e);

			throw new IOException(e);
		}
	}

	@Transactional
	public Optional<LuceneFileExtent> readExtent(final UUID fileId, final int extentIndex) {
		log.info("Reading extend {} for file {}", extentIndex, fileId);

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
							.extentLength(rs.getInt(4)).extentData(decodeBinaryData(rs.getString(5))).build());
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

	@Transactional
	public int extentsForFile(final UUID fileId) {
		return jdbcTemplate.execute((PreparedStatementCreator) con -> {
			final PreparedStatement ps = con.prepareStatement("select count(*) from LUCENE_EXTENTS where FILE_ID=?");

			ps.setString(1, fileId.toString());

			return ps;
		}, (PreparedStatementCallback<Integer>) ps -> {
			ResultSet rs = null;

			try {
				rs = ps.executeQuery();

				if (rs.next()) {
					return rs.getInt(1);
				} else {
					return 0;
				}
			} finally {
				if (rs != null) {
					rs.close();
				}
			}
		});
	}

	private boolean checkIfFileExists(final String directoryName, final String fileName) {
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
	
	private String encodeBinaryData(final byte[] data) {
		return Base64.getEncoder().encodeToString(data);
	}
	
	private byte[] decodeBinaryData(final String data) {
		return Base64.getDecoder().decode(data);
	}

}
