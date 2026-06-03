package org.search.util;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

public final class PipelineTimer {

    private long lastMark;
    private final Map<String, Long> sequentialNanos = new LinkedHashMap<>();
    private final Map<String, Long> phaseNanos = new LinkedHashMap<>();
    private final Map<String, Long> nestedNanos = new LinkedHashMap<>();
    private final Map<String, Long> nestedCallCounts = new LinkedHashMap<>();

    public PipelineTimer() {
        this.lastMark = System.nanoTime();
    }

    public void checkpoint(String label) {
        long now = System.nanoTime();
        long delta = now - lastMark;
        lastMark = now;
        sequentialNanos.merge(label, delta, Long::sum);
    }

    public void recordPhase(String label, long nanos) {
        if (nanos <= 0) {
            return;
        }
        phaseNanos.merge(label, nanos, Long::sum);
    }

    public <T> T time(String label, Supplier<T> action) {
        long t0 = System.nanoTime();
        try {
            return action.get();
        } finally {
            long dt = System.nanoTime() - t0;
            nestedNanos.merge(label, dt, Long::sum);
            nestedCallCounts.merge(label, 1L, Long::sum);
        }
    }

    public String formatSummary(String title) {
        long seqTotal = sequentialNanos.values().stream().mapToLong(Long::longValue).sum();
        StringBuilder sb = new StringBuilder();
        sb.append("[pipeline timing] ").append(title).append(System.lineSeparator());
        sb.append("  sequential (wall between checkpoints):").append(System.lineSeparator());
        for (Map.Entry<String, Long> e : sequentialNanos.entrySet()) {
            sb.append(formatLine(e.getKey(), e.getValue(), seqTotal));
        }
        sb.append(String.format(Locale.ROOT, "  sequential total: %.2f ms%n", seqTotal / 1_000_000.0));

        if (!phaseNanos.isEmpty()) {
            long phaseTotal = phaseNanos.values().stream().mapToLong(Long::longValue).sum();
            sb.append("  findCandidatePaths phases (wall, exclusive):").append(System.lineSeparator());
            for (Map.Entry<String, Long> e : phaseNanos.entrySet()) {
                sb.append(formatLine(e.getKey(), e.getValue(), phaseTotal));
            }
            sb.append(String.format(Locale.ROOT, "  findCandidatePaths phase sum: %.2f ms%n", phaseTotal / 1_000_000.0));
        }

        if (!nestedNanos.isEmpty()) {
            long nestedTotal = nestedNanos.values().stream().mapToLong(Long::longValue).sum();
            sb.append("  nested DB (per call; subset of phases above):").append(System.lineSeparator());
            for (Map.Entry<String, Long> e : nestedNanos.entrySet()) {
                long calls = nestedCallCounts.getOrDefault(e.getKey(), 0L);
                String rowLabel = calls > 0 ? e.getKey() + " (" + calls + " calls)" : e.getKey();
                sb.append(formatLine(rowLabel, e.getValue(), nestedTotal));
            }
            sb.append(String.format(Locale.ROOT, "  nested DB sum: %.2f ms%n", nestedTotal / 1_000_000.0));
        }
        return sb.toString();
    }

    private static String formatLine(String label, long nanos, long basisTotal) {
        double ms = nanos / 1_000_000.0;
        double pct = basisTotal > 0 ? 100.0 * nanos / basisTotal : 0.0;
        return String.format(Locale.ROOT, "    %-28s %8.2f ms (%5.1f%%)%n", label + ":", ms, pct);
    }
}
