package de.bieniekconsulting.logstore.types;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Builder
@Getter
@EqualsAndHashCode
public class Triple<L, M, R> {
	private final L left;
	private final M middle;
	private final R right;
}
