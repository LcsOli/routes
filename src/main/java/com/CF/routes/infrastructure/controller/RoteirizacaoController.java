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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Engenheiro Sénior: Controller atualizado com a lógica de processamento seguro de XML
 * extraída do script de automação (Google Apps Script).
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
     * Dashboard: Painel Geral com sincronização em tempo real.
     * Implementa a lógica "Total - Aberto = Realizado" para precisão absoluta.
     */
    @GetMapping("/painel-geral")
    public ResponseEntity<List<Map<String, Object>>> obterPainelGeral(@RequestParam(required = false) String data) {
        List<Map<String, Object>> rotasWinThor = repository.buscarResumoRotasAtivas(data);
        List<Map<String, Object>> painelFinal = new ArrayList<>();

        for (Map<String, Object> rota : rotasWinThor) {
            Map<String, Object> item = new HashMap<>(rota);
            
            try {
                Long numCar = ((Number) rota.get("numCar")).longValue();
                
                // 1. Busca XML na Concept
                String xmlResponse = soapClient.listarItinerariosCarregamento(numCar);
                
                // 2. Processamento Seguro (Lógica do Script)
                Map<String, Integer> resumo = processarXMLSeguro(xmlResponse);

                // 3. Consolidação (Se a Concept não tiver dados, mantém o total do WinThor)
                int totalFinal = resumo.get("total") > 0 ? resumo.get("total") : ((Number) rota.get("totalEntregas")).intValue();
                int realizados = resumo.get("realizados");
                int pendentes = resumo.get("aberto") > 0 ? resumo.get("aberto") : (totalFinal - realizados);

                double progresso = totalFinal > 0 ? (double) realizados * 100 / totalFinal : 0;

                item.put("totalEntregas", totalFinal);
                item.put("entregues", realizados); // O seu "Concluído"
                item.put("pendentes", pendentes);
                item.put("progresso", Math.min(100, Math.round(progresso)));
                item.put("sincronizado", true);

            } catch (Exception e) {
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
     * Dashboard: Detalhe técnico do Itinerário.
     */
    @GetMapping("/itinerario/{numCar}")
    public ResponseEntity<String> buscarItinerario(@PathVariable Long numCar) {
        try {
            return ResponseEntity.ok(soapClient.listarItinerariosCarregamento(numCar));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    /**
     * LÓGICA PORTADA DO SCRIPT: processarXMLSeguro
     * Remove ruídos de logs/ocorrências e conta status reais.
     */
    private Map<String, Integer> processarXMLSeguro(String xml) {
        Map<String, Integer> result = new HashMap<>();
        result.put("total", 0);
        result.put("aberto", 0);
        result.put("realizados", 0);

        if (xml == null || xml.isEmpty() || xml.contains("Nenhuma")) {
            return result;
        }

        // 1. Remove o bloco de ocorrências (histórico) para não contar status antigos/repetidos
        String xmlLimpo = xml.replaceAll("(?i)<[^>]*listaOcorrencias[^>]*>[\\s\\S]*?<\\/[^>]*listaOcorrencias>", "");

        // 2. Regex para capturar o conteúdo de todas as tags <status>
        Pattern pattern = Pattern.compile("(?i)<[^>]*status[^>]*>(.*?)<\\/[^>]*status>");
        Matcher matcher = pattern.matcher(xmlLimpo);

        int total = 0;
        int aberto = 0;

        while (matcher.find()) {
            String valorStatus = matcher.group(1).trim().toUpperCase();
            total++;
            if (valorStatus.equals("ABERTO")) {
                aberto++;
            }
        }

        result.put("total", total);
        result.put("aberto", aberto);
        result.put("realizados", Math.max(0, total - aberto));

        return result;
    }

    @PostMapping("/processar-pendentes")
    public ResponseEntity<String> processarPendentes() {
        try {
            useCase.processarPendentes();
            return ResponseEntity.ok("Fila processada.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @GetMapping("/status")
    public ResponseEntity<String> status() {
        return ResponseEntity.ok("Ativo.");
    }
}