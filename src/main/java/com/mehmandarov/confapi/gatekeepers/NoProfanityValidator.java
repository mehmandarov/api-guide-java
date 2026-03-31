package com.mehmandarov.confapi.gatekeepers;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validator for {@link NoProfanity}.
 * <p>
 * Uses a trivially small blocklist for demo purposes — in production
 * you'd plug in a real content-moderation service or a larger dictionary.
 */
public class NoProfanityValidator implements ConstraintValidator<NoProfanity, String> {

    /**
     * Demo blocklist — intentionally mild words for a conference setting.
     */
    private static final Set<String> BLOCKED = Set.of(
            "spam", "scam", "phishing"
    );

    private static final Pattern WORD_BOUNDARY = Pattern.compile("\\b");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext ctx) {
        if (value == null || value.isBlank()) {
            return true; // let @NotBlank handle emptiness
        }
        String lower = value.toLowerCase();
        for (String blocked : BLOCKED) {
            if (lower.contains(blocked)) {
                ctx.disableDefaultConstraintViolation();
                ctx.buildConstraintViolationWithTemplate(
                        "Text contains prohibited word: '" + blocked + "'"
                ).addConstraintViolation();
                return false;
            }
        }
        return true;
    }
}

