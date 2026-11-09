package com.indoornav.backend.service;

import com.indoornav.backend.exception.FieldValidationException;
import java.util.Locale;

/** Turns user search text into safe, lower-case LIKE patterns (API.md Section 5). */
final class SearchPatterns {

    private static final String ANY = "%";
    private static final int MAX_QUERY_LENGTH = 60;

    private SearchPatterns() {
    }

    /** Returns the trimmed text, or an empty string when absent or blank, which means browse. */
    static String normaliseQuery(String query) {
        String trimmed = query == null ? "" : query.strip();
        if (trimmed.length() > MAX_QUERY_LENGTH) {
            throw new FieldValidationException(
                    "query", "must be at most " + MAX_QUERY_LENGTH + " characters");
        }
        return trimmed;
    }

    static String contains(String normalisedQuery) {
        return normalisedQuery.isEmpty() ? ANY : ANY + escape(normalisedQuery) + ANY;
    }

    static String startsWith(String normalisedQuery) {
        return normalisedQuery.isEmpty() ? ANY : escape(normalisedQuery) + ANY;
    }

    /** Makes % and _ ordinary characters; the queries declare backslash as the escape. */
    private static String escape(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
