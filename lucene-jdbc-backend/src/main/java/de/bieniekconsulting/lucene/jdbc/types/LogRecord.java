package de.bieniekconsulting.lucene.jdbc.types;

import java.io.Serializable;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
@EqualsAndHashCode
public class LogRecord implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 8804094301481660855L;

	private Long id;

	private String messageText;
}
