
package io.agenda4j.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * CancelQuery describes how to match jobs to cancel.
 *
 * <p>This is an API-layer object (NOT a MongoDB query). The store layer will later translate it
 * into an actual database query.
 */
public final class CancelQuery {

    private final String name;
    private final String uniqueKey;
    private final Map<String, Object> unique;

    private CancelQuery(String name, String uniqueKey, Map<String, Object> unique) {
        this.name = (name == null || name.isBlank()) ? null : name;
        this.uniqueKey = (uniqueKey == null || uniqueKey.isBlank()) ? null : uniqueKey;
        this.unique = (unique == null || unique.isEmpty())
                ? null
                : Collections.unmodifiableMap(new LinkedHashMap<>(unique));
    }

    /**
     * Job handler name (e.g. "subscribe-to-channel").
     */
    public String name() {
        return name;
    }

    /**
     * Deterministic uniqueKey string. Usually paired with name.
     */
    public String uniqueKey() {
        return uniqueKey;
    }

    /**
     * Flexible unique map (e.g. {guildId, sourceId}).
     *
     * <p>When present, store layer should match fields like: unique.guildId, unique.sourceId, ...
     */
    public Map<String, Object> unique() {
        return unique;
    }

    /**
     * Returns true if this query has at least one selector.
     */
    public boolean isEmpty() {
        return name == null
                && uniqueKey == null
                && (unique == null || unique.isEmpty());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String name;
        private String uniqueKey;
        private final Map<String, Object> unique = new LinkedHashMap<>();

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder uniqueKey(String uniqueKey) {
            this.uniqueKey = uniqueKey;
            return this;
        }

        public Builder unique(Map<String, Object> unique) {
            this.unique.clear();
            if (unique != null) {
                this.unique.putAll(unique);
            }
            return this;
        }

        /**
         * Add a single unique field (e.g. key="guildId", value="123").
         */
        public Builder put(String key, Object value) {
            Objects.requireNonNull(key, "key must not be null");
            if (key.isBlank()) {
                throw new IllegalArgumentException("key must not be blank");
            }
            if (value == null) {
                return this;
            }
            this.unique.put(key, value);
            return this;
        }

        public CancelQuery build() {
            boolean hasName = name != null && !name.isBlank();
            boolean hasUniqueKey = uniqueKey != null && !uniqueKey.isBlank();
            boolean hasUnique = !unique.isEmpty();

            if (!hasName && !hasUniqueKey && !hasUnique) {
                throw new IllegalStateException(
                        "CancelQuery must contain at least one condition: name, uniqueKey, or unique"
                );
            }
            return new CancelQuery(name, uniqueKey, unique);
        }
    }
}
