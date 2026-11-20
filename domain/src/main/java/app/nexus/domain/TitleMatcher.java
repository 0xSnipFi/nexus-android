package app.nexus.domain;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/** Unicode-aware title comparison; prepared once per query. */
public final class TitleMatcher {
    private static final Pattern SEPARATOR = Pattern.compile("[^\\p{L}\\p{M}\\p{N}]+");
    private final String needle;
    public TitleMatcher(String query) { needle = key(query); }

    private static String key(String text) {
        return SEPARATOR.matcher(Normalizer.normalize(text, Normalizer.Form.NFKC)
            .toLowerCase(Locale.ROOT).replace("&", " and ")).replaceAll(" ").trim();
    }

    public boolean matches(String title) {
        return !needle.isEmpty() && key(title).contains(needle);
    }
}
