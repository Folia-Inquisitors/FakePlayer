package me.chrommob.fakeplayer.data;

import me.chrommob.fakeplayer.FakePlayer;

import java.util.ArrayDeque;
import java.util.Queue;

public class FrequencyData {
    private int timeBetweenJoinsAvg = -1;
    private final Queue<Integer> timeBetweenJoins = new ArrayDeque<>();
    private int timeBetweenMessagesAvg = -1;
    private final Queue<Integer> timeBetweenMessages = new ArrayDeque<>();
    private int timeBetweenAchievementsAvg = -1;
    private final Queue<Integer> timeBetweenAchievements = new ArrayDeque<>();

    @Override
    public String toString() {
        return timeBetweenJoinsAvg + "," + timeBetweenMessagesAvg + "," + timeBetweenAchievementsAvg;
    }
    public static FrequencyData fromString(String data) {
        FrequencyData frequencyData = new FrequencyData();

        try {
            String[] split = data.split(",");
            frequencyData.timeBetweenJoinsAvg = Integer.parseInt(split[0]);
            frequencyData.timeBetweenMessagesAvg = Integer.parseInt(split[1]);
            frequencyData.timeBetweenAchievementsAvg = Integer.parseInt(split[2]);

            if (frequencyData.timeBetweenJoinsAvg != -1) {
                frequencyData.timeBetweenJoins.add(frequencyData.timeBetweenJoinsAvg);
            }
            if (frequencyData.timeBetweenMessagesAvg != -1) {
                frequencyData.timeBetweenMessages.add(frequencyData.timeBetweenMessagesAvg);
            }
            if (frequencyData.timeBetweenAchievementsAvg != -1) {
                frequencyData.timeBetweenAchievements.add(frequencyData.timeBetweenAchievementsAvg);
            }
        } catch (Throwable ignored) {
        }

        return frequencyData;
    }

    private int getPercentageOutLiner(int value, Queue<Integer> queue) {
        if (queue.size() < 5) {
            return 0;
        }
        int currentDeviationInQueue = 0;
        for (int i : queue) {
            currentDeviationInQueue += Math.abs(i - value);
        }
        currentDeviationInQueue /= queue.size();
        if (currentDeviationInQueue == 0) {
            return 0;
        }
        int newValuesDeviation = Math.abs(value - getAverage(queue));
        return (newValuesDeviation * 100 / currentDeviationInQueue) - 100;
    }

    private int getAverage(Queue<Integer> queue) {
        if (queue.isEmpty()) {
            return -1;
        }
        while (queue.size() > 100) {
            queue.poll();
        }
        return (int) queue.stream().mapToInt(i -> i).average().orElse(0);
    }

    private long lastTimeBetweenJoins = -1;
    public void newTimeBetweenJoins() {
        if (lastTimeBetweenJoins == -1) {
            lastTimeBetweenJoins = System.currentTimeMillis();
            return;
        }
        long diff = System.currentTimeMillis() - lastTimeBetweenJoins;
        int timeBetweenJoinsNew = (int) (diff / 50);
        int outLiner = getPercentageOutLiner(timeBetweenJoinsNew, timeBetweenJoins);
        int toDrop = FakePlayer.getPlugin(FakePlayer.class).getFakePlayerConfig().dynamicFrequencyOutliersDrop();
        if (toDrop != 0 && outLiner > toDrop) {
            lastTimeBetweenJoins = System.currentTimeMillis();
            return;
        }
        timeBetweenJoins.add(timeBetweenJoinsNew);
        timeBetweenAchievementsAvg = getAverage(timeBetweenAchievements);
        lastTimeBetweenJoins = System.currentTimeMillis();
        FakePlayer.getPlugin(FakePlayer.class).getDebugger().debug("New time between joins: " + timeBetweenJoins);
    }

    private long lastTimeBetweenMessages = -1;
    public void newTimeBetweenMessages() {
        if (lastTimeBetweenMessages == -1) {
            lastTimeBetweenMessages = System.currentTimeMillis();
            return;
        }
        long diff = System.currentTimeMillis() - lastTimeBetweenMessages;
        int timeBetweenMessagesNew = (int) (diff / 50);
        int outLiner = getPercentageOutLiner(timeBetweenMessagesNew, timeBetweenMessages);
        int toDrop = FakePlayer.getPlugin(FakePlayer.class).getFakePlayerConfig().dynamicFrequencyOutliersDrop();
        if (toDrop != 0 && outLiner > toDrop) {
            lastTimeBetweenMessages = System.currentTimeMillis();
            return;
        }
        timeBetweenMessages.add(timeBetweenMessagesNew);
        timeBetweenMessagesAvg = getAverage(timeBetweenMessages);
        lastTimeBetweenMessages = System.currentTimeMillis();
        FakePlayer.getPlugin(FakePlayer.class).getDebugger().debug("New time between messages: " + timeBetweenMessages);
    }

    private long lastTimeBetweenAchievements = -1;
    public void newTimeBetweenAchievements() {
        if (lastTimeBetweenAchievements == -1) {
            lastTimeBetweenAchievements = System.currentTimeMillis();
            return;
        }
        long diff = System.currentTimeMillis() - lastTimeBetweenAchievements;
        int timeBetweenAchievementsNew = (int) (diff / 50);
        int outLiner = getPercentageOutLiner(timeBetweenAchievementsNew, timeBetweenAchievements);
        int toDrop = FakePlayer.getPlugin(FakePlayer.class).getFakePlayerConfig().dynamicFrequencyOutliersDrop();
        if (toDrop != 0 && outLiner > toDrop) {
            lastTimeBetweenAchievements = System.currentTimeMillis();
            return;
        }
        timeBetweenAchievements.add(timeBetweenAchievementsNew);
        timeBetweenAchievementsAvg = getAverage(timeBetweenAchievements);
        lastTimeBetweenAchievements = System.currentTimeMillis();
        FakePlayer.getPlugin(FakePlayer.class).getDebugger().debug("New time between achievements: " + timeBetweenAchievements);
    }

    public int getRandomPlayerJoinQuitFrequency() {
        if (timeBetweenJoinsAvg == -1) {
            return -1;
        }
        int deviation = 0;
        for (int i : timeBetweenJoins) {
            deviation += Math.abs(i - timeBetweenJoinsAvg);
        }
        deviation /= timeBetweenJoins.size();
        return timeBetweenJoinsAvg + (int) (Math.random() * deviation * 2 - deviation);
    }

    public int getRandomMessageFrequency() {
        if (timeBetweenMessagesAvg == -1) {
            return -1;
        }
        int deviation = 0;
        for (int i : timeBetweenMessages) {
            deviation += Math.abs(i - timeBetweenMessagesAvg);
        }
        deviation /= timeBetweenMessages.size();
        return timeBetweenMessagesAvg + (int) (Math.random() * deviation * 2 - deviation);
    }

    public int getRandomAchievementFrequency() {
        if (timeBetweenAchievementsAvg == -1) {
            return -1;
        }
        int deviation = 0;
        for (int i : timeBetweenAchievements) {
            deviation += Math.abs(i - timeBetweenAchievementsAvg);
        }
        deviation /= timeBetweenAchievements.size();
        return timeBetweenAchievementsAvg + (int) (Math.random() * deviation * 2 - deviation);
    }
}
