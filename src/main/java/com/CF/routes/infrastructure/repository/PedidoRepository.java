package com.CF.routes.infrastructure.repository;

import com.CF.routes.domain.dto.PedidoDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class PedidoRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public List<Map<String, Object>> buscarResumoRotasAtivas(String dataFiltro) {
        // Lógica de filtro por data: Se não informada, traz os últimos 5 dias por padrão
        String condicaoData = (dataFiltro == null || dataFiltro.isEmpty()) 
            ? "C.DTSAIDA >= TRUNC(SYSDATE) - 5" 
            : "TRUNC(C.DTSAIDA) = TO_DATE(:dataFiltro, 'YYYY-MM-DD')";

        // Adicionamos o CASE na query retornando como ORIGEM
        String sql = """
            SELECT 
                C.NUMCAR, 
                NVL(V.PLACA, 'S/P') AS PLACA, 
                NVL(E.NOME, 'S/M') AS MOTORISTA,
                (SELECT COUNT(*) FROM PCPEDC P WHERE P.NUMCAR = C.NUMCAR AND P.DTFAT IS NOT NULL) as TOTAL_ENTREGAS,
                C.DTSAIDA,
                CASE 
                    WHEN c.codrotaprinc IN (98, 128, 172, 131, 174, 124, 140, 165, 170, 110, 232, 139, 96, 125, 204, 177, 122, 171, 135, 129, 161, 120, 112, 164, 180, 220, 130, 100, 101, 113, 107, 114, 159) THEN 'CD SUMARE'
                    WHEN c.codrotaprinc IN (117, 210, 99, 104, 143, 147, 142, 168, 167, 221, 157) THEN 'CD PRAIA GRANDE'
                    WHEN c.codrotaprinc IN (223, 169, 109, 173, 83, 152, 153, 102, 213, 214, 217, 219, 231, 91, 178, 97, 103, 134, 116, 181, 212, 225, 92, 155, 86, 149, 215, 211, 136, 95, 118, 119, 163, 179, 138, 200, 105, 106, 227, 228, 229, 230) THEN 'CD GUARULHOS'
                    WHEN c.codrotaprinc IN (123, 93, 94, 108, 126, 121, 162, 222, 226, 201) THEN 'CD SOROCABA'
                    WHEN c.codrotaprinc IN (205, 202, 137, 90, 115, 127, 144, 145, 158, 146, 150, 154, 175, 141, 176, 216, 218, 206, 156, 166, 80, 81, 82, 84, 85) THEN 'CD JACAREI'
                    WHEN c.codrotaprinc IN (133, 209, 88, 89, 87, 111, 151, 160, 207, 148, 208, 132, 203) THEN 'CD ITAPETININGA'
                    ELSE 'SAO JOSE DO RIO PRETO'
                END AS ORIGEM
            FROM PCCARREG C
            LEFT JOIN PCVEICUL V ON C.CODVEICULO = V.CODVEICULO
            LEFT JOIN PCEMPR E ON C.CODMOTORISTA = E.MATRICULA
            WHERE C.ENVIAAPI = 'S' 
              AND C.IMPORTADOAPI = 'S'
              AND %s
            ORDER BY C.DTSAIDA DESC, C.NUMCAR DESC
            """.formatted(condicaoData);
        
        try {
            Query query = entityManager.createNativeQuery(sql);
            
            if (dataFiltro != null && !dataFiltro.isEmpty()) {
                query.setParameter("dataFiltro", dataFiltro);
            }
            
            List<Object[]> results = query.getResultList();
            
            return results.stream().map(row -> {
                Map<String, Object> map = new HashMap<>();
                map.put("numCar", row[0]);
                map.put("placa", row[1]);
                map.put("motorista", row[2]);
                map.put("totalEntregas", row[3] != null ? ((Number) row[3]).intValue() : 0);
                map.put("dataSaida", row[4] != null ? new java.text.SimpleDateFormat("dd/MM/yyyy").format((Timestamp) row[4]) : "--/--/----");
                // Mapeia a nova coluna ORIGEM (índice 5)
                map.put("origem", row[5] != null ? row[5].toString() : "SAO JOSE DO RIO PRETO"); 
                return map;
            }).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Erro ao buscar resumo de rotas integradas no Oracle: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Fila: Busca carregamentos marcados para envio que ainda não foram processados pelo robô.
     * Restrição aplicada (ROWNUM <= 2) para evitar timeout por excesso de requisições simultâneas.
     */
    public List<Long> buscarCarregamentosPendentes() {
        String sql = """
            SELECT NUMCAR 
            FROM PCCARREG 
            WHERE ENVIAAPI = 'S' 
              AND (IMPORTADOAPI = 'N' OR IMPORTADOAPI IS NULL)
              AND ROWNUM <= 2
            """;
        try {
            Query query = entityManager.createNativeQuery(sql);
  
            List<Object> result = query.getResultList();
            return result.stream()
                .map(n -> ((Number) n).longValue())
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Erro ao consultar fila de carregamentos pendentes: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Detalhe: Busca todos os dados necessários para o mapeamento SOAP do itinerário.
     */
    public List<PedidoDTO> buscarPedidosParaRoteirizacao(List<Long> carregamentos) {
    if (carregamentos == null || carregamentos.isEmpty()) return List.of();

    String sql = """
        SELECT
            a.numped, a.data,
            LPAD(NVL(a.hora, 0), 2, '0') || ':' || LPAD(NVL(a.minuto, 0), 2, '0') AS hora,
            ROUND(a.vlatend, 2) AS valor_total,
            b.cliente AS nome_cliente,
            a.codcli,
            (b.enderent || ', ' || b.bairroent || ', ' || b.municent || ', ' || b.estent || ', CEP ' || b.cepent) AS endereco,
            b.codpraca AS cod_zona,
            NVL(p.praca, 'SEM NOME') AS nome_zona,
            u.codusur AS cod_vendedor,
            u.nome AS nome_vendedor,
            c.codrotaprinc AS cod_loja,
            CASE 
                WHEN c.codrotaprinc IN (98, 128, 172, 131, 174, 124, 140, 165, 170, 110, 232, 139, 96, 125, 204, 177, 122, 171, 135, 129, 161, 120, 112, 164, 180, 220, 130, 100, 101, 113, 107, 114, 159) THEN 'CD SUMARE'
                WHEN c.codrotaprinc IN (117, 210, 99, 104, 143, 147, 142, 168, 167, 221, 157) THEN 'CD PRAIA GRANDE'
                WHEN c.codrotaprinc IN (223, 169, 109, 173, 83, 152, 153, 102, 213, 214, 217, 219, 231, 91, 178, 97, 103, 134, 116, 181, 212, 225, 92, 155, 86, 149, 215, 211, 136, 95, 118, 119, 163, 179, 138, 200, 105, 106, 227, 228, 229, 230) THEN 'CD GUARULHOS'
                WHEN c.codrotaprinc IN (123, 93, 94, 108, 126, 121, 162, 222, 226, 201) THEN 'CD SOROCABA'
                WHEN c.codrotaprinc IN (205, 202, 137, 90, 115, 127, 144, 145, 158, 146, 150, 154, 175, 141, 176, 216, 218, 206, 156, 166, 80, 81, 82, 84, 85) THEN 'CD JACAREI'
                WHEN c.codrotaprinc IN (133, 209, 88, 89, 87, 111, 151, 160, 207, 148, 208, 132, 203) THEN 'CD ITAPETININGA'
                ELSE 'SAO JOSE DO RIO PRETO'
            END AS nome_loja,
            a.totpeso, a.totvolume, a.numcar, a.numnota, v.placa, e.matricula, e.nome AS nome_motorista, e.cpf 
        FROM pcpedc a 
        JOIN pcclient b ON a.codcli = b.codcli 
        LEFT JOIN pcpraca p ON b.codpraca = p.codpraca 
        JOIN pccarreg c ON a.numcar = c.numcar 
        JOIN pcusuari u ON a.codusur = u.codusur 
        LEFT JOIN pcveicul v ON c.codveiculo = v.codveiculo
        LEFT JOIN pcempr e ON c.codmotorista = e.matricula 
        LEFT JOIN pccarreg c_ant ON a.numcaranterior = c_ant.numcar
        WHERE a.numcar IN (:carregamentos)
          AND a.dtfat IS NOT NULL
          AND a.numnota > 0
          AND (
               NVL(a.numcaranterior, 0) = 0 
               OR 
               (NVL(c_ant.enviaapi, 'N') = 'N' AND NVL(c_ant.importadoapi, 'N') = 'N')
              )
        """;

        try {
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("carregamentos", carregamentos);

            List<Object[]> results = query.getResultList();

            return results.stream().map(row -> PedidoDTO.builder()
                    .numPed(row[0] != null ? String.valueOf(row[0]) : null)
                    .data(row[1] != null ? ((Timestamp) row[1]).toLocalDateTime().toLocalDate() : null)
                    .hora(row[2] != null ? String.valueOf(row[2]) : "00:00") // Alterado aqui de "08:00" para "00:00" como fallback protetivo
                    .valorTotal(row[3] != null ? ((Number) row[3]).doubleValue() : 0.0)
                    .nomeCliente(row[4] != null ? String.valueOf(row[4]) : "")
                    .codCli(row[5] != null ? String.valueOf(row[5]) : null)
                    .endereco(row[6] != null ? String.valueOf(row[6]) : "")
                    .codZona(row[7] != null ? String.valueOf(row[7]) : "")
                    .nomeZona(row[8] != null ? String.valueOf(row[8]) : "")
                    .codVendedor(row[9] != null ? String.valueOf(row[9]) : "")
                    .nomeVendedor(row[10] != null ? String.valueOf(row[10]) : "")
                    .codLoja(row[11] != null ? String.valueOf(row[11]) : "")
                    .nomeLoja(row[12] != null ? String.valueOf(row[12]) : "")
                    .peso(row[13] != null ? ((Number) row[13]).doubleValue() : 0.0)
                    .volume(row[14] != null ? ((Number) row[14]).doubleValue() : 0.0)
                    .numCar(row[15] != null ? ((Number) row[15]).longValue() : null)
                    .numNota(row[16] != null ? String.valueOf(row[16]) : "")
                    .placa(row[17] != null ? String.valueOf(row[17]) : "")
                    .motoristaMatricula(row[18] != null ? String.valueOf(row[18]) : "")
                    .motoristaNome(row[19] != null ? String.valueOf(row[19]) : "")
                    .motoristaCpf(row[20] != null ? String.valueOf(row[20]) : "")
                    .build()
            ).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Erro ao buscar pedidos detalhados para integração: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Atualiza o carregamento indicando que já foi enviado com sucesso para a API.
     */
    @Transactional
    public void marcarComoImportado(Long numcar) {
        String sql = "UPDATE PCCARREG SET IMPORTADOAPI = 'S', DATAIMPORTADOAPI = SYSDATE WHERE NUMCAR = ?";
        entityManager.createNativeQuery(sql).setParameter(1, numcar).executeUpdate();
    }

    /**
     * Modificação da Regra de negócio: Se a integração falhar, remove as flags do fluxo de envio.
     * Atualiza IMPORTADOAPI = 'N' e ENVIAAPI = 'N' para interromper o loop de erros no robô.
     */
    @Transactional
    public void marcarFalhaIntegracao(Long numcar) {
        String sql = "UPDATE PCCARREG SET IMPORTADOAPI = 'N', ENVIAAPI = 'N' WHERE NUMCAR = ?";
        entityManager.createNativeQuery(sql).setParameter(1, numcar).executeUpdate();
    }

    /**
     * Registra o log de sucesso na tabela de ocorrências do WinThor.
     */
    @Transactional
    public void registrarLogSucesso(Long numcar) {
        String sql = "INSERT INTO PCCORREN (DATA, HISTORICO, NUMCAR) VALUES (SYSDATE, 'Integrado via Concept Sync', ?)";
        entityManager.createNativeQuery(sql).setParameter(1, numcar).executeUpdate();
    }
}