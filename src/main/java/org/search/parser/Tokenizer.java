package org.search.parser;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class Tokenizer {
    private Tokenizer() {
    }

    public static List<String> tokenize(String input) {
        if (input == null || input.isBlank()) {
            return List.of();
        }

        String[] rawTokens = input.toLowerCase(Locale.ROOT).split("[^a-z0-9]+");
        Set<String> normalized = new LinkedHashSet<>();
        for (String token : rawTokens) {
            if (token.length() >= 2) {
                normalized.add(token);
            }
        }
        return new ArrayList<>(normalized);
    }
}
