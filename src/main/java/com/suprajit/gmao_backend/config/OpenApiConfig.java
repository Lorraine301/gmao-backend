package com.suprajit.gmao_backend.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "GMAO Intelligente – API REST",
        version = "1.0.0",
        description = "Documentation de l'API REST de l'application de Gestion de Maintenance Assistée par Ordinateur (GMAO) développée pour Suprajit Maroc. Cette API intègre un moteur de règles métier et un LLM (Groq API) pour la priorisation intelligente des pannes.",
        contact = @Contact(
            name = "Lorraine Agnès",
            email = "lorraine@suprajit.ma"
        )
    ),
    servers = {
        @Server(url = "http://localhost:8080", description = "Serveur de développement")
    }
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    in = SecuritySchemeIn.HEADER,
    description = "Entrez votre token JWT. Obtenez-le via POST /api/auth/login"
)
public class OpenApiConfig {
    // Configuration déclarative via annotations
}