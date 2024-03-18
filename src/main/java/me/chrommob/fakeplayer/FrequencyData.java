package me.chrommob.fakeplayer;

public class FrequencyData {
    private int timeBetweenJoinsMin = -1;
    private int timeBetweenJoinsMax = -1;
    private int timeBetweenMessagesMin = -1;
    private int timeBetweenMessagesMax = -1;
    private int timeBetweenAchievementsMin = -1;
    private int timeBetweenAchievementsMax = -1;

    private long lastTimeBetweenJoins = -1;

    @Override
    public String toString() {
        return timeBetweenJoinsMin + "," + timeBetweenJoinsMax + "," + timeBetweenMessagesMin + "," + timeBetweenMessagesMax + "," + timeBetweenAchievementsMin + "," + timeBetweenAchievementsMax;
    }
    public static FrequencyData fromString(String data) {
        FrequencyData frequencyData = new FrequencyData();
        String[] split = data.split(",");
        frequencyData.timeBetweenJoinsMin = Integer.parseInt(split[0]);
        frequencyData.timeBetweenJoinsMax = Integer.parseInt(split[1]);
        frequencyData.timeBetweenMessagesMin = Integer.parseInt(split[2]);
        frequencyData.timeBetweenMessagesMax = Integer.parseInt(split[3]);
        frequencyData.timeBetweenAchievementsMin = Integer.parseInt(split[4]);
        frequencyData.timeBetweenAchievementsMax = Integer.parseInt(split[5]);
        return frequencyData;
    }

    public void newTimeBetweenJoins() {
        if (lastTimeBetweenJoins == -1) {
            lastTimeBetweenJoins = System.currentTimeMillis();
            return;
        }
        long diff = System.currentTimeMillis() - lastTimeBetweenJoins;
        int timeBetweenJoins = (int) (diff / 50);
        if (timeBetweenJoins < timeBetweenJoinsMin || timeBetweenJoinsMin == -1) {
            timeBetweenJoinsMin = timeBetweenJoins;
        }
        if (timeBetweenJoins > timeBetweenJoinsMax || timeBetweenJoinsMax == -1) {
            timeBetweenJoinsMax = timeBetweenJoins;
        }
        lastTimeBetweenJoins = System.currentTimeMillis();
    }

    private long lastTimeBetweenMessages = -1;
    public void newTimeBetweenMessages() {
        if (lastTimeBetweenMessages == -1) {
            lastTimeBetweenMessages = System.currentTimeMillis();
            return;
        }
        long diff = System.currentTimeMillis() - lastTimeBetweenMessages;
        int timeBetweenMessages = (int) (diff / 50);
        if (timeBetweenMessages < timeBetweenMessagesMin || timeBetweenMessagesMin == -1) {
            timeBetweenMessagesMin = timeBetweenMessages;
        }
        if (timeBetweenMessages > timeBetweenMessagesMax || timeBetweenMessagesMax == -1) {
            timeBetweenMessagesMax = timeBetweenMessages;
        }
        lastTimeBetweenMessages = System.currentTimeMillis();
    }

    private long lastTimeBetweenAchievements = -1;
    public void newTimeBetweenAchievements() {
        if (lastTimeBetweenAchievements == -1) {
            lastTimeBetweenAchievements = System.currentTimeMillis();
            return;
        }
        long diff = System.currentTimeMillis() - lastTimeBetweenAchievements;
        int timeBetweenAchievements = (int) (diff / 50);
        if (timeBetweenAchievements < timeBetweenAchievementsMin || timeBetweenAchievementsMin == -1) {
            timeBetweenAchievementsMin = timeBetweenAchievements;
        }
        if (timeBetweenAchievements > timeBetweenAchievementsMax || timeBetweenAchievementsMax == -1) {
            timeBetweenAchievementsMax = timeBetweenAchievements;
        }
        lastTimeBetweenAchievements = System.currentTimeMillis();
    }

    public int getRandomPlayerJoinQuitFrequency() {
        if (timeBetweenJoinsMin == -1) {
            return -1;
        }
        return (int) (Math.random() * (timeBetweenJoinsMax - timeBetweenJoinsMin) + timeBetweenJoinsMin);
    }

    public int getRandomMessageFrequency() {
        if (timeBetweenMessagesMin == -1) {
            return -1;
        }
        return (int) (Math.random() * (timeBetweenMessagesMax - timeBetweenMessagesMin) + timeBetweenMessagesMin);
    }

    public int getRandomAchievementFrequency() {
        if (timeBetweenAchievementsMin == -1) {
            return -1;
        }
        return (int) (Math.random() * (timeBetweenAchievementsMax - timeBetweenAchievementsMin) + timeBetweenAchievementsMin);
    }
}
