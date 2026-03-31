package com.mehmandarov.confapi;

import jakarta.annotation.security.DeclareRoles;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import org.eclipse.microprofile.auth.LoginConfig;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.info.Contact;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.info.License;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.servers.Server;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * JAX-RS application root — runtime-agnostic.
 * <p>
 * The {@code @LoginConfig} triggers MicroProfile JWT authentication.
 * The {@code @DeclareRoles} registers RBAC roles used by
 * {@code @RolesAllowed} on resource methods.
 */
@ApplicationPath("/api")
@LoginConfig(authMethod = "MP-JWT", realmName = "conference")
@DeclareRoles({"ORGANIZER", "SPEAKER", "ATTENDEE"})
@OpenAPIDefinition(
    info = @Info(
        title = "Conference Session API",
        version = "1.0.0",
        description = "Demo API for \"An Opinionated Guide to Bulletproof APIs with Java\". "
                    + "Showcases Jakarta EE 11 + MicroProfile 7 patterns for production-grade APIs.",
        contact = @Contact(name = "API Guide Talk", url = "https://github.com/example/api-guide-java"),
        license = @License(name = "Apache 2.0", url = "https://www.apache.org/licenses/LICENSE-2.0")
    ),
    servers = {
        @Server(url = "http://localhost:8080", description = "Local dev server")
    },
    tags = {
        @Tag(name = "Sessions",  description = "Conference session management"),
        @Tag(name = "Speakers",  description = "Speaker profiles"),
        @Tag(name = "Rooms",     description = "Venue rooms"),
        @Tag(name = "v2",        description = "Version 2 — enriched responses")
    }
)
@SecurityScheme(
    securitySchemeName = "jwt",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "MicroProfile JWT — pass a signed Bearer token"
)
public class ApiApplication extends Application {
    // No overrides — let the runtime scan for @Path / @Provider classes.
}

