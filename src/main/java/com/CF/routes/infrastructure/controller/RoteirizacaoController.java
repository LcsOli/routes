package com.CF.routes.infrastructure.controller;

import com.CF.routes.application.usecase.RoteirizacaoUseCase;
import com.CF.routes.infrastructure.client.ConceptSoapClient;
import com.CF.routes.infrastructure.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Engenheiro Sénior: Controller Otimizado para Alta Performance.
 * Implementa a estratégia "Monitor-then-Poll" para evitar travamentos de I/O bloqueante.
 */
@Slf4j
@RestController
@RequestMapping("/api/roteirizacao")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RoteirizacaoController {

    private final RoteirizacaoUseCase useCase;
    private final ConceptSoapClient soapClient;
    private final PedidoRepository repository;

    /**
     * Dashboard: Painel Geral (Carregamento Ultrarrápido).
     * Retorna apenas os metadados do WinThor. O detalhamento do status (XML da Concept)
     * é resolvido de forma assíncrona pelo Frontend batendo na rota /itinerario/{numCar}.
     */
    @GetMapping("/painel-geral")
    public ResponseEntity<List<Map<String, Object>>> obterPainelGeral(@RequestParam(required = false) String data) {
        log.info("[API] Consultando resumo de rotas no WinThor. Filtro de data: {}", data);
        // O repositório já devolve a lista pronta e formatada
        List<Map<String, Object>> rotasWinThor = repository.buscarResumoRotasAtivas(data);
        return ResponseEntity.ok(rotasWinThor);
    }

    /**
     * Dashboard: Detalhe técnico do Itinerário (Consumido assincronamente pelo Frontend).
     * Devolve o XML bruto. O Frontend (escala.html) aplica a lógica processarXMLSeguro via JS.
     */
    @GetMapping("/itinerario/{numCar}")
    public ResponseEntity<String> buscarItinerario(@PathVariable Long numCar) {
        try {
            return ResponseEntity.ok(soapClient.listarItinerariosCarregamento(numCar));
        } catch (Exception e) {
            log.error("[API] Erro ao buscar itinerário da carga {}: {}", numCar, e.getMessage());
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PostMapping("/processar-pendentes")
    public ResponseEntity<String> processarPendentes() {
        try {
            log.info("[API] Disparo manual de roteirização solicitado.");
            useCase.processarPendentes();
            return ResponseEntity.ok("Fila processada.");
        } catch (Exception e) {
            log.error("[API] Falha no processamento manual: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @GetMapping("/status")
    public ResponseEntity<String> status() {
        return ResponseEntity.ok("Ativo.");
    }
}