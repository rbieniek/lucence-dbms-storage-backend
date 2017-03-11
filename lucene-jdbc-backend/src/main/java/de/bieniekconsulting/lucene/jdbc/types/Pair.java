package de.bieniekconsulting.lucene.jdbc.types;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Builder
@Getter
@EqualsAndHashCode
public class Pair<L, R> {
	private final L left;
	private final R right;
}
