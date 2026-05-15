package me.chrommob.fakeplayer.impl;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.logging.Level;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

public final class FakeActivityScheduler {
    private static final int FAILURE_BACKOFF_TICKS = 20 * 30;

    private final Plugin plugin;
    private final boolean folia;
    private final IntSupplier joinQuitFrequency;
    private final BooleanSupplier deathMessagesEnabled;
    private final IntSupplier deathMessageFrequency;
    private final BooleanSupplier achievementMessagesEnabled;
    private final IntSupplier achievementMessageFrequency;
    private final Runnable joinQuitTask;
    private final Runnable deathMessageTask;
    private final Runnable achievementMessageTask;
    private ScheduledTask foliaSchedulerTask;
    private BukkitTask bukkitSchedulerTask;
    private long schedulerTick;
    private long nextJoinQuitTick = -1;
    private long nextDeathMessageTick = -1;
    private long nextAchievementMessageTick = -1;

    public FakeActivityScheduler(
            Plugin plugin,
            boolean folia,
            IntSupplier joinQuitFrequency,
            BooleanSupplier deathMessagesEnabled,
            IntSupplier deathMessageFrequency,
            BooleanSupplier achievementMessagesEnabled,
            IntSupplier achievementMessageFrequency,
            Runnable joinQuitTask,
            Runnable deathMessageTask,
            Runnable achievementMessageTask
    ) {
        this.plugin = plugin;
        this.folia = folia;
        this.joinQuitFrequency = joinQuitFrequency;
        this.deathMessagesEnabled = deathMessagesEnabled;
        this.deathMessageFrequency = deathMessageFrequency;
        this.achievementMessagesEnabled = achievementMessagesEnabled;
        this.achievementMessageFrequency = achievementMessageFrequency;
        this.joinQuitTask = joinQuitTask;
        this.deathMessageTask = deathMessageTask;
        this.achievementMessageTask = achievementMessageTask;
    }

    public void start() {
        cancel();
        schedulerTick = 0;
        resetDelays();
        if (folia) {
            foliaSchedulerTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin,
                    scheduledTask -> runSchedulerTick(), 1L, 1L);
        } else {
            bukkitSchedulerTask = Bukkit.getScheduler().runTaskTimer(plugin, this::runSchedulerTick, 1L, 1L);
        }
    }

    public void resetDelays() {
        nextJoinQuitTick = -1;
        nextDeathMessageTick = -1;
        nextAchievementMessageTick = -1;
    }

    public void cancel() {
        if (foliaSchedulerTask != null) {
            foliaSchedulerTask.cancel();
            foliaSchedulerTask = null;
        }
        if (bukkitSchedulerTask != null) {
            bukkitSchedulerTask.cancel();
            bukkitSchedulerTask = null;
        }
    }

    private void runSchedulerTick() {
        schedulerTick++;
        nextJoinQuitTick = scheduleIfReady("join/quit", nextJoinQuitTick, joinQuitFrequency, () -> true, joinQuitTask);
        nextDeathMessageTick = scheduleIfReady("death-message", nextDeathMessageTick, deathMessageFrequency,
                deathMessagesEnabled, deathMessageTask);
        nextAchievementMessageTick = scheduleIfReady("achievement-message", nextAchievementMessageTick,
                achievementMessageFrequency, achievementMessagesEnabled, achievementMessageTask);
    }

    private long scheduleIfReady(
            String taskName,
            long nextRunTick,
            IntSupplier frequencySupplier,
            BooleanSupplier enabledSupplier,
            Runnable task
    ) {
        try {
            boolean enabled = enabledSupplier.getAsBoolean();
            int frequency = frequencySupplier.getAsInt();
            if (!enabled || frequency == -1) {
                return -1;
            }
            int safeFrequency = Math.max(1, frequency);
            if (nextRunTick == -1) {
                return schedulerTick + safeFrequency;
            }
            if (schedulerTick >= nextRunTick) {
                task.run();
                return schedulerTick + safeFrequency;
            }
            return nextRunTick;
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.WARNING,
                    "Fake activity task '" + taskName + "' failed; delaying retry to avoid console spam",
                    exception);
            return schedulerTick + FAILURE_BACKOFF_TICKS;
        }
    }
}
