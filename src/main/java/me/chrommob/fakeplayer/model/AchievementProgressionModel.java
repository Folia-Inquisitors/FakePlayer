package me.chrommob.fakeplayer.model;

import me.chrommob.fakeplayer.data.model.FakePlayerProgress;
import me.chrommob.fakeplayer.data.model.WeightedMessageSet;
import net.kyori.adventure.text.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Selects fake advancement messages with a soft graph. The graph improves plausibility, but frequency data
 * from this server still carries the strongest signal and can override the default shape over time.
 */
public final class AchievementProgressionModel {
    private static final Map<String, AdvancementNode> PROGRESSION_GRAPH = createProgressionGraph();

    private final WeightedMessageSet achievementMessages;

    public AchievementProgressionModel(WeightedMessageSet achievementMessages) {
        this.achievementMessages = achievementMessages;
    }

    public Component nextMessage(FakePlayerProgress progress) {
        WeightedMessageSet.WeightedMessage message = achievementMessages.randomWeightedMessage(
                achievement -> !progress.hasSeenAchievement(achievement.id())
                        && !progress.hasSeenAchievementKey(advancementKey(achievement.serializedMessage())),
                achievement -> achievementWeight(progress, achievement)
        );
        if (message == null) {
            message = achievementMessages.randomWeightedMessage(null, achievement -> achievementWeight(progress, achievement));
        }
        if (message == null) {
            return null;
        }

        progress.recordAchievement(message, advancementKey(message.serializedMessage()), System.currentTimeMillis());
        return message.component();
    }

    private int achievementWeight(FakePlayerProgress progress, WeightedMessageSet.WeightedMessage achievement) {
        int baseWeight = Math.max(1, achievement.count());
        String key = advancementKey(achievement.serializedMessage());
        int noveltyWeight = progress.hasSeenAchievement(achievement.id()) || progress.hasSeenAchievementKey(key) ? 1 : 10;
        int graphWeight = progressionWeight(progress, key, achievement.serializedMessage());
        int variation = ThreadLocalRandom.current().nextInt(8, 13);

        long weighted = (long) baseWeight * noveltyWeight * graphWeight * variation / 10L;
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1L, weighted));
    }

    private int progressionWeight(FakePlayerProgress progress, String key, String serializedAchievement) {
        if (key == null) {
            return fallbackTierWeight(progress, serializedAchievement);
        }
        AdvancementNode node = PROGRESSION_GRAPH.get(key);
        if (node == null) {
            return fallbackTierWeight(progress, serializedAchievement);
        }

        int playerTier = Math.min(5, progress.achievementScore() / 4);
        int tierDistance = node.tier() - playerTier;
        int tierWeight;
        if (tierDistance > 2) {
            tierWeight = 1;
        } else if (tierDistance > 0) {
            tierWeight = 5 - tierDistance;
        } else if (tierDistance == 0) {
            tierWeight = 8;
        } else {
            tierWeight = Math.max(2, 6 + tierDistance);
        }

        if (node.prerequisites().isEmpty()) {
            return tierWeight;
        }

        long satisfiedPrerequisites = node.prerequisites().stream()
                .filter(progress::hasSeenAchievementKey)
                .count();
        int prerequisiteWeight = 2 + (int) ((satisfiedPrerequisites * 6L) / node.prerequisites().size());
        return Math.max(1, tierWeight * prerequisiteWeight);
    }

    private int fallbackTierWeight(FakePlayerProgress progress, String serializedAchievement) {
        int playerTier = Math.min(5, progress.achievementScore() / 4);
        int achievementTier = estimateCategoryTier(serializedAchievement);
        int tierDistance = achievementTier - playerTier;
        if (tierDistance > 2) {
            return 2;
        }
        if (tierDistance > 0) {
            return 6 - tierDistance;
        }
        if (tierDistance == 0) {
            return 8;
        }
        return Math.max(2, 6 + tierDistance);
    }

    private static String advancementKey(String serializedAchievement) {
        String value = serializedAchievement.toLowerCase(Locale.ROOT);
        for (String key : PROGRESSION_GRAPH.keySet()) {
            String translationKey = "advancements." + key.replace('/', '.') + ".";
            if (value.contains(translationKey) || value.contains(key)) {
                return key;
            }
        }
        return null;
    }

    private static int estimateCategoryTier(String serializedAchievement) {
        String value = serializedAchievement.toLowerCase(Locale.ROOT);
        if (value.contains("advancements.end.")) {
            return 5;
        }
        if (value.contains("advancements.nether.")) {
            return 4;
        }
        if (value.contains("advancements.adventure.")) {
            return 3;
        }
        if (value.contains("advancements.husbandry.")) {
            return 2;
        }
        return 1;
    }

    private static Map<String, AdvancementNode> createProgressionGraph() {
        Map<String, AdvancementNode> graph = new LinkedHashMap<>();
        add(graph, "story/root", 0);
        add(graph, "story/mine_stone", 0, "story/root");
        add(graph, "story/upgrade_tools", 0, "story/mine_stone");
        add(graph, "story/smelt_iron", 1, "story/mine_stone");
        add(graph, "story/obtain_armor", 1, "story/smelt_iron");
        add(graph, "story/lava_bucket", 1, "story/smelt_iron");
        add(graph, "story/iron_tools", 1, "story/smelt_iron");
        add(graph, "story/deflect_arrow", 1, "story/smelt_iron");
        add(graph, "story/form_obsidian", 2, "story/lava_bucket");
        add(graph, "story/mine_diamond", 2, "story/iron_tools");
        add(graph, "story/enter_the_nether", 3, "story/form_obsidian");
        add(graph, "story/shiny_gear", 3, "story/mine_diamond");
        add(graph, "story/enchant_item", 3, "story/mine_diamond");
        add(graph, "story/cure_zombie_villager", 3, "nether/brew_potion");
        add(graph, "story/follow_ender_eye", 4, "story/enter_the_nether");
        add(graph, "story/enter_the_end", 5, "story/follow_ender_eye");

        add(graph, "adventure/root", 1);
        add(graph, "adventure/sleep_in_bed", 1);
        add(graph, "adventure/kill_a_mob", 2, "adventure/root");
        add(graph, "adventure/shoot_arrow", 2, "adventure/kill_a_mob");
        add(graph, "adventure/trade", 2);
        add(graph, "adventure/throw_trident", 3, "adventure/kill_a_mob");
        add(graph, "adventure/kill_all_mobs", 4, "adventure/kill_a_mob");

        add(graph, "husbandry/root", 1);
        add(graph, "husbandry/plant_seed", 1, "husbandry/root");
        add(graph, "husbandry/fishy_business", 1, "husbandry/root");
        add(graph, "husbandry/breed_an_animal", 2, "husbandry/root");
        add(graph, "husbandry/tame_an_animal", 2, "husbandry/root");
        add(graph, "husbandry/balanced_diet", 4, "husbandry/root");

        add(graph, "nether/root", 3, "story/enter_the_nether");
        add(graph, "nether/return_to_sender", 3, "nether/root");
        add(graph, "nether/find_bastion", 3, "nether/root");
        add(graph, "nether/obtain_ancient_debris", 4, "nether/root");
        add(graph, "nether/obtain_blaze_rod", 4, "nether/root");
        add(graph, "nether/brew_potion", 4, "nether/obtain_blaze_rod");
        add(graph, "nether/create_beacon", 5, "nether/obtain_blaze_rod");

        add(graph, "end/root", 5, "story/enter_the_end");
        add(graph, "end/kill_dragon", 5, "end/root");
        add(graph, "end/enter_end_gateway", 5, "end/kill_dragon");
        return Map.copyOf(graph);
    }

    private static void add(Map<String, AdvancementNode> graph, String key, int tier, String... prerequisites) {
        graph.put(key, new AdvancementNode(key, tier, List.of(prerequisites)));
    }

    private record AdvancementNode(
            String key,
            int tier,
            List<String> prerequisites
    ) {
    }
}
