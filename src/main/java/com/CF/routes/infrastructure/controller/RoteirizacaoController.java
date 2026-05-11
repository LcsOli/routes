package com.CF.routes.infrastructure.controller;

import com.CF.routes.application.usecase.RoteirizacaoUseCase;
import com.CF.routes.infrastructure.client.ConceptSoapClient;
import com.CF.routes.infrastructure.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Slf4j
@RestController
@RequestMapping("/api/roteirizacao")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Essencial para o Dashboard frontend não ser bloqueado por CORS
public class RoteirizacaoController {

    private final RoteirizacaoUseCase useCase;
    private final ConceptSoapClient soapClient;
    private final PedidoRepository repository;

    /**
     * Dashboard Inteligente: Painel Geral
     * Cruza os dados de saída do WinThor com os status reais de entrega na Concept.
     * Ajustado com busca Case-Insensitive e Logs de Auditoria para depuração do XML.
     */
   @GetMapping("/painel-geral")
public ResponseEntity<List<Map<String, Object>>> obterPainelGeral(@RequestParam(required = false) String data) {
    List<Map<String, Object>> rotasWinThor = repository.buscarResumoRotasAtivas(data);
        List<Map<String, Object>> painelFinal = new ArrayList<>();

        for (Map<String, Object> rota : rotasWinThor) {
            Map<String, Object> item = new HashMap<>(rota);
            
            try {
                // Conversão segura de tipos vindos do driver Oracle
                Long numCar = ((Number) rota.get("numCar")).longValue();
                int totalWinThor = ((Number) rota.get("totalEntregas")).intValue();

                // 2. Consulta a API Concept (SOAP)
                String xmlResponse = soapClient.listarItinerariosCarregamento(numCar);
                
                // 3. Lógica de Auditoria: Padronizando para MAIÚSCULO para evitar problemas de Case Sensitivity (Entregue vs ENTREGUE)
                String xmlUpper = xmlResponse != null ? xmlResponse.toUpperCase() : "";
                
                int entreguesConcept = contarOcorrencias(xmlUpper, "<STATUS>ENTREGUE") 
                                     + contarOcorrencias(xmlUpper, "<STATUS>FINALIZADA")
                                     + contarOcorrencias(xmlUpper, "<STATUS>FINALIZADO")
                                     + contarOcorrencias(xmlUpper, "<STATUS>CONCLUIDO")
                                     + contarOcorrencias(xmlUpper, "<STATUS>CONCLUÍDO")
                                     + contarOcorrencias(xmlUpper, "<STATUS>REALIZADA")
                                     + contarOcorrencias(xmlUpper, "<STATUS>REALIZADO");
                
                // 3.1. MODO DETETIVE: Se vier vazio ou 0 entregas, loga o XML para descobrirmos qual é a tag real
                if (entreguesConcept == 0 && xmlResponse != null && xmlResponse.length() > 50) {
                    log.warn("[API-DETETIVE] Carga {} tem 0 entregas mapeadas. Trecho do XML retornado: {}", 
                             numCar, xmlResponse.substring(0, Math.min(xmlResponse.length(), 500)));
                }
                
                // 4. Consolidação de Indicadores
                int pendentes = Math.max(0, totalWinThor - entreguesConcept);
                double calculoProgresso = totalWinThor > 0 ? (double) entreguesConcept * 100 / totalWinThor : 0;
                
                item.put("entregues", entreguesConcept);
                item.put("pendentes", pendentes);
                item.put("progresso", Math.min(100, Math.round(calculoProgresso)));
                item.put("sincronizado", true);
                
            } catch (Exception e) {
                // Evita que falha em uma carga específica derrube o dashboard inteiro
                log.warn("[API] Erro ao sincronizar carga {}: {}", rota.get("numCar"), e.getMessage());
                item.put("entregues", 0);
                item.put("pendentes", rota.get("totalEntregas"));
                item.put("progresso", 0);
                item.put("error", true); 
            }
            
            painelFinal.add(item);
        }

        return ResponseEntity.ok(painelFinal);
    }

    /**
     * Dashboard: Retorna a lista bruta de viagens registradas na Concept.
     */
    @GetMapping("/viagens-geral")
    public ResponseEntity<String> listarViagensGeral() {
        log.info("[API] Solicitando lista geral de viagens.");
        try {
            return ResponseEntity.ok(soapClient.listarViagens());
        } catch (Exception e) {
            log.error("[API] Erro ao buscar viagens: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    /**
     * Dashboard: Retorna o detalhe técnico (XML) de um itinerário específico.
     */
    @GetMapping("/itinerario/{numCar}")
    public ResponseEntity<String> buscarItinerario(@PathVariable Long numCar) {
        log.info("[API] Solicitando itinerário detalhado: {}", numCar);
        try {
            return ResponseEntity.ok(soapClient.listarItinerariosCarregamento(numCar));
        } catch (Exception e) {
            log.error("[API] Erro ao buscar itinerário {}: {}", numCar, e.getMessage());
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    /**
     * Processamento Manual: Dispara a roteirização para carregamentos específicos.
     */
    @PostMapping("/executar")
    public ResponseEntity<String> executar(@RequestBody List<Long> carregamentos) {
        log.info("[API] Processamento manual solicitado para: {}", carregamentos);
        if (carregamentos == null || carregamentos.isEmpty()) {
            return ResponseEntity.badRequest().body("Erro: Lista de carregamentos vazia.");
        }
        try {
            useCase.executar(carregamentos);
            return ResponseEntity.ok("Processamento manual iniciado.");
        } catch (Exception e) {
            log.error("[API] Erro no processamento manual: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    /**
     * Fila: Processa todos os carregamentos pendentes (ENVIAAPI='S' e IMPORTADOAPI='N').
     */
    @PostMapping("/processar-pendentes")
    public ResponseEntity<String> processarPendentes() {
        log.info("[API] Disparando processamento de pendentes via API.");
        try {
            useCase.processarPendentes();
            return ResponseEntity.ok("Fila de pendentes processada.");
        } catch (Exception e) {
            log.error("[API] Erro ao processar pendentes: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    /**
     * Health Check: Status do serviço.
     */
    @GetMapping("/status")
    public ResponseEntity<String> status() {
        return ResponseEntity.ok("Serviço Concept Sync - Ativo e Operacional.");
    }

    /**
     * Utilitário: Conta ocorrências de uma substring no texto (contagem de status de pedidos).
     */
    private int contarOcorrencias(String texto, String termo) {
        if (texto == null || !texto.contains(termo)) return 0;
        return texto.split(termo).length - 1;
    }
}