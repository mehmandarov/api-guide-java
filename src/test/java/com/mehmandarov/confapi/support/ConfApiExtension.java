package com.mehmandarov.confapi.support;

import io.restassured.RestAssured;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * JUnit 5 extension that starts the shared {@link ConfApiContainer}
 * and configures REST Assured to point at it.
 * <p>
 * Usage: annotate each IT class with {@code @ExtendWith(ConfApiExtension.class)}.
 * The container is a singleton — it starts once and is reused by every test class.
 */
public class ConfApiExtension implements BeforeAllCallback {

    @Override
    public void beforeAll(ExtensionContext context) {
        ConfApiContainer container = ConfApiContainer.getInstance();
        RestAssured.baseURI = "http://" + container.getHost();
        RestAssured.port = container.getMappedPort(8080);
    }
}

