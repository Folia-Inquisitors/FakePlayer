package me.chrommob.fakeplayer.interaction;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class CommandPatternRule {
    private final String rawPattern;
    private final Pattern pattern;

    private CommandPatternRule(String rawPattern, Pattern pattern) {
        this.rawPattern = rawPattern;
        this.pattern = pattern;
    }

    public static Optional<CommandPatternRule> compile(String rawPattern) {
        if (rawPattern == null || rawPattern.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new CommandPatternRule(rawPattern, Pattern.compile(rawPattern, Pattern.CASE_INSENSITIVE)));
        } catch (PatternSyntaxException ignored) {
            return Optional.empty();
        }
    }

    public Optional<String> target(String command) {
        Matcher matcher = pattern.matcher(command);
        if (!matcher.find()) {
            return Optional.empty();
        }
        String target = namedGroup(matcher, "target");
        if (target == null && matcher.groupCount() >= 1) {
            target = matcher.group(1);
        }
        if (target == null || target.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(target);
    }

    public String rawPattern() {
        return rawPattern;
    }

    private String namedGroup(Matcher matcher, String groupName) {
        try {
            return matcher.group(groupName);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
