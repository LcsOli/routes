package com.CF.routes.infrastructure.scheduler;

import com.CF.routes.application.usecase.RoteirizacaoUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoteirizacaoScheduler {

    private final RoteirizacaoUseCase useCase;

    @Scheduled(fixedDelayString = "${concept.automation.interval:60000}")
    public void executarAutomacao() {
        log.info("--- [ROBÔ] INICIANDO VERIFICAÇÃO DE FILA DE ROTEIRIZAÇÃO ---");
        
        try {
            // Processa os carregamentos marcados com ENVIAAPI='S' e IMPORTADOAPI='N'
            useCase.processarPendentes();
        } catch (Exception e) {
            log.error("--- [ROBÔ] ERRO NA AUTOMAÇÃO: {} ---", e.getMessage());
        }
        
        log.info("--- [ROBÔ] VERIFICAÇÃO FINALIZADA ---");
    }
}