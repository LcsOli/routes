package com.CF.routes.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "concept.api")
public class ConceptConfig {
    private String cnpj;
    private String senhaCliente;
    private String senhaCentral;
    
    private Endpoints endpoints = new Endpoints();

    @Data
    public static class Endpoints {
        private String automatizador;
        private String importador;
    }
}