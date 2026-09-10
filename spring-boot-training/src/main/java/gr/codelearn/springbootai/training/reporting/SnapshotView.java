package gr.codelearn.springbootai.training.reporting;

import java.time.LocalDateTime;

/**
 * The non-aggregate result of {@link SnapshotService#capture} - what a caller (a controller in U4) is handed instead of the
 * {@link ReportSnapshot} aggregate itself. Just enough to confirm the capture: the generated id, the label, when it was taken, and how many
 * category lines it holds.
 */
record SnapshotView(long id, String label, LocalDateTime capturedAt, int lineCount) {

	static SnapshotView of(final ReportSnapshot snapshot) {
		final Long id = snapshot.id();
		return new SnapshotView(id == null ? 0L : id, snapshot.label(), snapshot.capturedAt(), snapshot.lines().size());
	}
}