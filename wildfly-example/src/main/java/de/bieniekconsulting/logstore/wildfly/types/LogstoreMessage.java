package de.bieniekconsulting.logstore.wildfly.types;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
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
@ValidLogstoreMessage
public class LogstoreMessage {

	@NotNull
	@Min(value = 1)
	private long timestamp;

	@Size(min = 1)
	private String messageText;

	@Size(min = 1)
	private String compressedEncodedMessageText;
}
