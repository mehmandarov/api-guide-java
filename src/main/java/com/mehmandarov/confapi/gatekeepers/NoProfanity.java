package com.mehmandarov.confapi.gatekeepers;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom Bean Validation constraint — rejects strings containing
 * a naive set of "profane" words.  Demonstrates how to build
 * domain-specific validators that keep resource code clean.
 */
@Documented
@Constraint(validatedBy = NoProfanityValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface NoProfanity {

    String message() default "Text contains prohibited content";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

