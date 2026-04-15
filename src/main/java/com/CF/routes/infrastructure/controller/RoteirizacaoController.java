package com.CF.routes.infrastructure.controller;

import com.CF.routes.application.usecase.RoteirizacaoUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/roteirizacao")
@RequiredArgsConstructor
public class RoteirizacaoController {

    private final RoteirizacaoUseCase useCase;

    @PostMapping("/executar")
    public ResponseEntity<String> executar(@RequestBody List<Long> carregamentos) {
        log.info("Recebida requisição REST para processar carregamentos manuais: {}", carregamentos);
        
        if (carregamentos == null || carregamentos.isEmpty()) {
            return ResponseEntity.badRequest().body("Erro: A lista de carregamentos não pode estar vazia.");
        }

        try {
            useCase.executar(carregamentos);
            return ResponseEntity.ok("Processamento manual iniciado para os carregamentos: " + carregamentos);
        } catch (Exception e) {
            log.error("Erro ao processar carregamentos manuais {}: {}", carregamentos, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body("Erro crítico no processamento manual: " + e.getMessage());
        }
    }

    @PostMapping("/processar-pendentes")
    public ResponseEntity<String> processarPendentes() {
        log.info("Recebida requisição REST para processar fila de carregamentos pendentes.");
        try {
            useCase.processarPendentes();
            return ResponseEntity.ok("Processamento da fila concluído.");
        } catch (Exception e) {
            log.error("Erro ao processar fila de pendentes: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body("Erro crítico no processamento da fila: " + e.getMessage());
        }
    }
    
    /**
     * Health Check: Verifica se o serviço está online.
     */
    @GetMapping("/status")
    public ResponseEntity<String> status() {
        return ResponseEntity.ok("Serviço de Roteirização Concept Sync está ativo.");
    }
}