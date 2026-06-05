package com.CF.routes.infrastructure.controller;

import com.CF.routes.application.usecase.RoteirizacaoUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/roteirizacao")
public class RoteirizacaoController {

    private final RoteirizacaoUseCase roteirizacaoUseCase;

    public RoteirizacaoController(RoteirizacaoUseCase roteirizacaoUseCase) {
        this.roteirizacaoUseCase = roteirizacaoUseCase;
    }

    @PostMapping("/processar-pendentes")
    public ResponseEntity<String> processarPendentes() {
        roteirizacaoUseCase.processarPendentes();
        
        return ResponseEntity.ok("Processamento da fila de pendentes concluído. Verifique a tabela CF_LOG_ROTEIRIZACAO para checar rejeições.");
    }

    @PostMapping("/forcar-carregamentos")
    public ResponseEntity<String> forcarCarregamentos(@RequestBody List<Long> carregamentos) {
        if (carregamentos == null || carregamentos.isEmpty()) {
            return ResponseEntity.badRequest().body("A lista de carregamentos não pode estar vazia.");
        }

        for (Long numcar : carregamentos) {
            try {
                roteirizacaoUseCase.executar(List.of(numcar));
            } catch (Exception e) {
            }
        }

        return ResponseEntity.ok("Processamento dos carregamentos específicos finalizado. Verifique os resultados no banco.");
    }
}