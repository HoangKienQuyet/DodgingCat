package com.dodgingcat.i18n;

/**
 * Supported display languages.
 */
public enum Language {
    EN("English", "en"),
    VI("Tiếng Việt", "vi");

    private final String displayName;
    private final String code;

    Language(String displayName, String code) {
        this.displayName = displayName;
        this.code = code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCode() {
        return code;
    }

    public static Language fromCode(String code) {
        if ("vi".equalsIgnoreCase(code)) return VI;
        return EN;
    }
}
