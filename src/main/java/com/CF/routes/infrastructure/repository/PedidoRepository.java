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
import java.util.stream.Collectors;


@Slf4j
@Repository
public class PedidoRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public List<Long> buscarCarregamentosPendentes() {
        String sql = """
            SELECT NUMCAR 
            FROM PCCARREG 
            WHERE ENVIAAPI = 'S' 
              AND (IMPORTADOAPI = 'N' OR IMPORTADOAPI IS NULL)
            """;
        try {
            Query query = entityManager.createNativeQuery(sql);
            List<Number> result = query.getResultList();
            return result.stream().map(Number::longValue).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Erro ao consultar fila de carregamentos (PCCARREG): {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * SQL Principal: Busca detalhes dos pedidos para o XML.
     */
    public List<PedidoDTO> buscarPedidosParaRoteirizacao(List<Long> carregamentos) {
        if (carregamentos == null || carregamentos.isEmpty()) return List.of();

        String sql = """
            SELECT
                a.numped,
                a.data,
                LPAD(a.hora, 2, '0') || ':' || LPAD(a.minuto, 2, '0') AS hora,
                ROUND(a.vlatend, 2) AS valor_total,
                b.cliente AS nome_cliente,
                a.codcli,
                (b.enderent || ', ' || b.bairroent || ', ' || b.municent || ', ' || b.estent || ', CEP ' || b.cepent) AS endereco,
                b.codpraca AS cod_zona,
                b.municcom AS nome_zona,
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
                a.totpeso,
                a.totvolume,
                a.numcar,
                a.numnota,
                v.placa,
                e.matricula,
                e.nome AS nome_motorista,
                e.cpf 
            FROM pcpedc a 
            JOIN pcclient b ON a.codcli = b.codcli 
            JOIN pccarreg c ON a.numcar = c.numcar 
            JOIN pcusuari u ON a.codusur = u.codusur 
            LEFT JOIN pcveicul v ON c.codveiculo = v.codveiculo
            LEFT JOIN pcempr e ON c.codmotorista = e.matricula 
            WHERE a.numcar IN (:carregamentos)
              AND a.numnota > 0 
              AND a.dtfat IS NOT NULL
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("carregamentos", carregamentos);

        List<Object[]> results = query.getResultList();

        return results.stream().map(row -> PedidoDTO.builder()
                .numPed(row[0] != null ? String.valueOf(row[0]) : null)
                .data(row[1] != null ? ((Timestamp) row[1]).toLocalDateTime().toLocalDate() : null)
                .hora(row[2] != null ? String.valueOf(row[2]) : "08:00")
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
    }

    /**
     * Atualiza o status de importação na PCCARREG.
     */
    @Transactional
    public void marcarComoImportado(Long numcar) {
        String sql = "UPDATE PCCARREG SET IMPORTADOAPI = 'S' WHERE NUMCAR = ?";
        try {
            entityManager.createNativeQuery(sql)
                    .setParameter(1, numcar)
                    .executeUpdate();
            log.info("Carregamento {} atualizado: IMPORTADOAPI='S'.", numcar);
        } catch (Exception e) {
            log.error("Erro ao atualizar IMPORTADOAPI na PCCARREG: {}", e.getMessage());
        }
    }
}