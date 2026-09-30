package gr.codelearn.springbootai.ai.mcp.tools;

import org.jspecify.annotations.Nullable;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * A toy tool a model cannot do for itself: it has no clock. The same {@code @Tool} annotation as in the other modules; what changes in 11.4
 * is who calls it (an MCP client over HTTP, not the model's own application).
 */
@Component
public class ClockTools {

	private final Clock clock;

	ClockTools(final Clock clock) {
		this.clock = clock;
	}

	@Tool(description = "Returns the current date and time in an IANA time zone, for example Europe/Athens or America/New_York, "
						+ "as an ISO-8601 offset date-time. Use it whenever the answer depends on the current time.")
	public String currentTime(@ToolParam(description = "IANA time zone id, for example Europe/Athens; UTC when omitted",
										 required = false) final @Nullable String zoneId) {
		final ZoneId zone;
		try {
			zone = zoneId == null || zoneId.isBlank() ? ZoneId.of("UTC") : ZoneId.of(zoneId.strip());
		} catch (DateTimeException ex) {
			throw new IllegalArgumentException("Unknown time zone '" + zoneId + "'. Use an IANA id such as Europe/Athens.", ex);
		}
		return ZonedDateTime.now(clock.withZone(zone)).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
	}
}