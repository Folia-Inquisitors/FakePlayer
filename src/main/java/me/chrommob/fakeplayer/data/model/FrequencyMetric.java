package me.chrommob.fakeplayer.data.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ThreadLocalRandom;

public final class FrequencyMetric {
    private static final int MAX_SAMPLES = 100;

    private int averageTicks = -1;
    private final Queue<Integer> samples = new ArrayDeque<>();
    private long lastObservedMillis = -1;

    public FrequencyMetric() {
    }

    private FrequencyMetric(int averageTicks, List<Integer> samples) {
        this.averageTicks = averageTicks;
        for (Integer sample : samples) {
            if (sample != null && sample > 0) {
                this.samples.add(sample);
            }
        }
        trimSamples();
        if (this.samples.isEmpty() && averageTicks > 0) {
            this.samples.add(averageTicks);
        }
        if (!this.samples.isEmpty()) {
            this.averageTicks = average(this.samples);
        }
    }

    public static FrequencyMetric fromAverage(int averageTicks) {
        return new FrequencyMetric(averageTicks, averageTicks > 0 ? List.of(averageTicks) : List.of());
    }

    public static FrequencyMetric fromYaml(Object object) {
        if (!(object instanceof Map<?, ?> yaml)) {
            return new FrequencyMetric();
        }
        int averageTicks = getInt(yaml.get("average-ticks"), getInt(yaml.get("average"), -1));
        List<Integer> samples = new ArrayList<>();
        Object samplesObject = yaml.get("samples");
        if (samplesObject instanceof List<?> sampleList) {
            for (Object sample : sampleList) {
                int sampleTicks = getInt(sample, -1);
                if (sampleTicks > 0) {
                    samples.add(sampleTicks);
                }
            }
        }
        return new FrequencyMetric(averageTicks, samples);
    }

    public synchronized boolean recordNow(long nowMillis, int outlierDropPercent) {
        if (lastObservedMillis == -1) {
            lastObservedMillis = nowMillis;
            return false;
        }

        int newSample = Math.max(1, (int) ((nowMillis - lastObservedMillis) / 50L));
        int outlierPercent = getOutlierPercent(newSample);
        if (outlierDropPercent != 0 && outlierPercent > outlierDropPercent) {
            lastObservedMillis = nowMillis;
            return false;
        }

        samples.add(newSample);
        trimSamples();
        averageTicks = average(samples);
        lastObservedMillis = nowMillis;
        return true;
    }

    public synchronized int randomFrequencyTicks() {
        if (averageTicks == -1 || samples.isEmpty()) {
            return -1;
        }
        int deviation = 0;
        for (int sample : samples) {
            deviation += Math.abs(sample - averageTicks);
        }
        deviation /= samples.size();
        if (deviation <= 0) {
            return averageTicks;
        }
        return Math.max(1, averageTicks + ThreadLocalRandom.current().nextInt(-deviation, deviation + 1));
    }

    public synchronized Map<String, Object> toYaml() {
        Map<String, Object> yaml = new LinkedHashMap<>();
        yaml.put("average-ticks", averageTicks);
        yaml.put("samples", new ArrayList<>(samples));
        return yaml;
    }

    public synchronized int averageTicks() {
        return averageTicks;
    }

    public synchronized List<Integer> samples() {
        return new ArrayList<>(samples);
    }

    private int getOutlierPercent(int value) {
        if (samples.size() < 5) {
            return 0;
        }
        int currentDeviation = 0;
        for (int sample : samples) {
            currentDeviation += Math.abs(sample - value);
        }
        currentDeviation /= samples.size();
        if (currentDeviation == 0) {
            return 0;
        }
        int newValueDeviation = Math.abs(value - average(samples));
        return (newValueDeviation * 100 / currentDeviation) - 100;
    }

    private void trimSamples() {
        while (samples.size() > MAX_SAMPLES) {
            samples.poll();
        }
    }

    private static int average(Queue<Integer> samples) {
        if (samples.isEmpty()) {
            return -1;
        }
        return (int) samples.stream().mapToInt(i -> i).average().orElse(0);
    }

    private static int getInt(Object object, int defaultValue) {
        if (object instanceof Number number) {
            return number.intValue();
        }
        if (object instanceof String string) {
            try {
                return Integer.parseInt(string);
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }
}
