package com.CF.routes.application.usecase;

import com.CF.routes.domain.dto.PedidoDTO;
import com.CF.routes.domain.mapper.PedidoMapper;
import com.CF.routes.infrastructure.client.ConceptSoapClient;
import com.CF.routes.infrastructure.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoteirizacaoUseCase {

    private final PedidoRepository repository;
    private final ConceptSoapClient soapClient;
    private final PedidoMapper mapper;

    /**
     * Varre a tabela PCCARREG em busca de registros marcados para envio.
     */
    public void processarPendentes() {
        List<Long> pendentes = repository.buscarCarregamentosPendentes();
        if (pendentes.isEmpty()) {
            log.info("Nenhum carregamento pendente para processamento.");
            return;
        }

        log.info("Iniciando processamento de fila para {} carregamento(s).", pendentes.size());
        for (Long numcar : pendentes) {
            try {
                this.executar(List.of(numcar));
                // Sucesso: Marca como processado na PCCARREG para não repetir
                repository.marcarComoImportado(numcar);
            } catch (Exception e) {
                log.error("Falha ao processar carregamento individual {}: {}", numcar, e.getMessage());
            }
        }
    }

    /**
     * Executa o fluxo de roteirização técnica.
     */
    public void executar(List<Long> carregamentos) {
        log.info("### INICIANDO PROCESSO DE ROTEIRIZAÇÃO PARA: {} ###", carregamentos);

        // 1. Extração dos pedidos do WinThor (Oracle)
        List<PedidoDTO> pedidos = repository.buscarPedidosParaRoteirizacao(carregamentos);
        
        if (pedidos.isEmpty()) {
            log.warn("Nenhum pedido elegível encontrado para processamento.");
            return;
        }

        try {
            PedidoDTO ref = pedidos.get(0);
            String placaFormatada = formatarPlaca(ref.getPlaca());

            // Validação de placa: Se estiver vazia no banco, não adianta enviar para a API
            if (placaFormatada.isEmpty()) {
                log.error("ERRO: Carregamento {} ignorado pois a placa está vazia no banco.", ref.getNumCar());
                return;
            }

            // 2. Sincronização de Entidades (Zonas e Motorista)
            pedidos.stream()
                    .map(p -> new String[]{p.getCodZona(), p.getNomeZona()})
                    .distinct()
                    .forEach(z -> soapClient.cadastrarZona(z[0], z[1]));
            
            soapClient.cadastrarMotorista(ref.getMotoristaMatricula(), ref.getMotoristaNome(), ref.getMotoristaCpf());

            // 3. Importação Massiva de Pedidos
            StringBuilder xmlPedidos = new StringBuilder();
            for (PedidoDTO p : pedidos) {
                xmlPedidos.append(gerarFragmentoPedidoXml(p));
            }
            soapClient.importarPedidos(xmlPedidos.toString());

            // 4. Disparo da Roteirização Final na Concept
            log.info("Solicitando roteirização para o veículo: {}", placaFormatada);
            soapClient.roteirizarPedidos(ref.getNumCar(), placaFormatada, ref.getMotoristaNome());
            
            log.info("Integração concluída com sucesso para o carregamento {}.", ref.getNumCar());

        } catch (Exception e) {
            log.error("ERRO CRÍTICO no processamento: {}", e.getMessage());
            throw e; 
        }

        log.info("### FLUXO FINALIZADO PARA O CARREGAMENTO ###");
    }

    private String formatarPlaca(String placa) {
        if (placa == null || placa.isEmpty()) return "";
        
        String limpa = placa.replaceAll("[^A-Za-z0-9]", "").toUpperCase().trim();
        
        if (limpa.length() == 7) {
            return limpa.substring(0, 3) + "-" + limpa.substring(3);
        }
        
        return limpa;
    }

    /**
     * Gera o fragmento XML arg0 para cada pedido.
     */
    private String gerarFragmentoPedidoXml(PedidoDTO p) {
        String dataAtual = LocalDate.now().toString();
        String horaAtual = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));

        return """
            <arg0>
               <cadastrarPontoInteresse>false</cadastrarPontoInteresse>
               <numeroPedido>%s</numeroPedido>
               <numeroCarregamento>%d</numeroCarregamento>
               <numeroNotaFiscal>%s</numeroNotaFiscal>
               <dataPedido>%s</dataPedido>
               <horaPedido>%s</horaPedido>
               <qtdItensPedido>1</qtdItensPedido>
               <valorPedido>%s</valorPedido>
               <descricao>%s</descricao>
               <loja><codigoLoja>%s</codigoLoja><nome>%s</nome></loja>
               <vendedor><codigoVendedor>%s</codigoVendedor><nome>%s</nome></vendedor>
               <zona><codigoZona>%s</codigoZona><nome>%s</nome></zona>
               <horaEntregaInicial>08:00</horaEntregaInicial>
               <horaEntregaFinal>18:00</horaEntregaFinal>
               <tempoAtendimento>45</tempoAtendimento>
               <dataCompromissoEntrega>%s</dataCompromissoEntrega>
               <horaCompromissoEntrega>%s</horaCompromissoEntrega>
               <peso>%s</peso><volume>%s</volume>
               <endereco>%s</endereco>
            </arg0>
            """.formatted(
                p.getNumPed(), p.getNumCar(), p.getNumNota(),
                p.getData().toString(), p.getHora(),
                mapper.formatarDecimal(p.getValorTotal()),
                mapper.escapeXml(p.getNomeCliente()),
                p.getCodLoja(), mapper.escapeXml(p.getNomeLoja()),
                p.getCodVendedor(), mapper.escapeXml(p.getNomeVendedor()),
                p.getCodZona(), mapper.escapeXml(p.getNomeZona()),
                dataAtual, horaAtual,
                mapper.formatarDecimal(p.getPeso()),
                mapper.formatarDecimal(p.getVolume()),
                mapper.escapeXml(p.getEndereco())
        );
    }
}