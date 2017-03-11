package de.bieniekconsulting.lucene.jdbc.directory;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import de.bieniekconsulting.lucene.jdbc.types.Pair;

@Component
public class JdbcFileRegistry {

	private final List<JdbcIndexOutput> openFiles = new LinkedList<>();

	public void registerOpenFile(final JdbcIndexOutput jdbcIndexOutput) {
		openFiles.add(jdbcIndexOutput);
	}

	public void unregisterOpenFile(final JdbcIndexOutput jdbcIndexOutput) {
		openFiles.remove(jdbcIndexOutput);
	}

	public List<JdbcIndexOutput> openFiles(final Predicate<Pair<UUID, String>> predicate) {
		return openFiles.stream().filter(v -> v.isOpen()).filter(
				v -> predicate.test(Pair.<UUID, String>builder().left(v.getFileId()).right(v.getName()).build()))
				.collect(Collectors.toList());
	}

	public List<JdbcIndexOutput> openFiles() {
		return openFiles.stream().filter(v -> v.isOpen()).collect(Collectors.toList());
	}
}
