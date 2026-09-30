package gr.codelearn.springbootai.ai.mcp.tools;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ToolsTest {

	private final ClockTools clockTools = new ClockTools(Clock.fixed(Instant.parse("2026-09-30T10:15:30Z"), ZoneOffset.UTC));
	private final MathTools mathTools = new MathTools();

	@Test
	void currentTimeUsesTheRequestedZone() {
		assertThat(clockTools.currentTime("Europe/Athens")).isEqualTo("2026-09-30T13:15:30+03:00");
	}

	@Test
	void currentTimeDefaultsToUtc() {
		assertThat(clockTools.currentTime(null)).isEqualTo("2026-09-30T10:15:30Z");
		assertThat(clockTools.currentTime("  ")).isEqualTo("2026-09-30T10:15:30Z");
	}

	@Test
	void currentTimeRejectsAnUnknownZoneWithAReadableMessage() {
		assertThatIllegalArgumentException().isThrownBy(() -> clockTools.currentTime("Mars/Olympus"))
											.withMessageContaining("Unknown time zone 'Mars/Olympus'")
											.withMessageContaining("Europe/Athens");
	}

	@Test
	void addIsExactWhereDoublesAreNot() {
		assertThat(mathTools.add(new BigDecimal("0.1"), new BigDecimal("0.2"))).isEqualTo("0.3");
		assertThat(mathTools.add(new BigDecimal("19.99"), new BigDecimal("0.01"))).isEqualTo("20.00");
	}
}
