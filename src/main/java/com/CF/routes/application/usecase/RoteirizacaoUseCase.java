package com.CF.routes.application.usecase;

import com.CF.routes.application.service.LogRoteirizacaoService;
import com.CF.routes.domain.dto.PedidoDTO;
import com.CF.routes.domain.mapper.PedidoMapper;
import com.CF.routes.infrastructure.client.ConceptSoapClient;
import com.CF.routes.infrastructure.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final LogRoteirizacaoService logRoteirizacaoService;

    public void processarPendentes() {
        List<Long> pendentes = repository.buscarCarregamentosPendentes();
        if (pendentes.isEmpty()) {
            return;
        }

        log.info("Iniciando processamento de fila para {} carregamento(s).", pendentes.size());
        for (Long numcar : pendentes) {
            try {
                // Executa o fluxo completo do carregamento de forma transacional isolada
                this.executarCarregamentoIndividual(numcar);
            } catch (Exception e) {
                log.error("Falha ao processar carregamento {}: {}", numcar, e.getMessage());
                
                // Grava o erro na tabela CF_LOG_ROTEIRIZACAO através da transação em escopo independente
                logRoteirizacaoService.registrarErro(numcar, e.getMessage());
                
                // Atualiza as flags no banco para importadoApi = 'N' e enviaapi = 'N' em caso de erro
                atualizarFlagsFalha(numcar);
            }
        }
    }

    @Transactional
    protected void executarCarregamentoIndividual(Long numcar) {
        // Executa as chamadas da API
        this.executar(List.of(numcar));
        
        // Se tudo rodar sem Exceptions, marca sucesso total: importadoApi = 'S', enviaapi = 'N' (ou a regra padrão do seu repository)
        repository.marcarComoImportado(numcar);
    }

    @Transactional
    protected void atualizarFlagsFalha(Long numcar) {
        try {
            repository.marcarFalhaIntegracao(numcar);
        } catch (Exception ex) {
            log.error("Erro ao atualizar flags de falha para o carregamento {}: {}", numcar, ex.getMessage());
        }
    }

    public void executar(List<Long> carregamentos) {
        List<PedidoDTO> pedidos = repository.buscarPedidosParaRoteirizacao(carregamentos);
        
        if (pedidos.isEmpty()) return;

        try {
            PedidoDTO ref = pedidos.get(0);
            String placaFormatada = formatarPlaca(ref.getPlaca());

            if (placaFormatada.isEmpty()) {
                throw new IllegalArgumentException("Placa do veículo inválida ou vazia para o carregamento.");
            }

            // Sincronização prévia de Entidades
            pedidos.stream().map(p -> new String[]{p.getCodZona(), p.getNomeZona()}).distinct()
                    .forEach(z -> soapClient.cadastrarZona(z[0], z[1]));
            
            pedidos.stream().map(p -> new String[]{p.getCodLoja(), p.getNomeLoja()}).distinct()
                    .forEach(l -> soapClient.cadastrarLoja(l[0], l[1]));

            soapClient.cadastrarMotorista(ref.getMotoristaMatricula(), ref.getMotoristaNome(), ref.getMotoristaCpf());

            // 1. Geração e Envio do XML de Importação de Pedidos
            StringBuilder xmlPedidos = new StringBuilder();
            for (PedidoDTO p : pedidos) {
                xmlPedidos.append(gerarFragmentoPedidoXml(p));
            }
            soapClient.importarPedidos(xmlPedidos.toString());
            log.info("Pedidos da carga {} importados com sucesso na Concept.", ref.getNumCar());

            // 2. Disparo da Roteirização Final
            log.info("Acionando o motor de roteirização para a carga {}...", ref.getNumCar());
            soapClient.roteirizarPedidos(ref.getNumCar(), placaFormatada, ref.getMotoristaNome());
            
            log.info("Carga {} Roteirizada com sucesso na plataforma.", ref.getNumCar());

        } catch (Exception e) {
            log.error("Erro Crítico no fluxo de roteirização: {}", e.getMessage());
            throw e; 
        }
    }

    private String formatarPlaca(String placa) {
        if (placa == null || placa.isEmpty()) return "";
        String limpa = placa.replaceAll("[^A-Za-z0-9]", "").toUpperCase().trim();
        if (limpa.length() == 7) {
            return limpa.substring(0, 3) + "-" + limpa.substring(3);
        }
        return limpa;
    }

    private String formatarDecimalSeguro(Double valor) {
        if (valor == null || valor == 0.0) return "0.00";
        if (valor >= 1000) valor = valor / 100;
        return String.format(java.util.Locale.US, "%.2f", valor);
    }

    private String gerarFragmentoPedidoXml(PedidoDTO p) {
        String dataAtual = LocalDate.now().toString();
        String horaAtual = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));

        String valorStr = formatarDecimalSeguro(p.getValorTotal());
        String pesoStr = formatarDecimalSeguro(p.getPeso());
        String volStr = formatarDecimalSeguro(p.getVolume());

        String nomeLoja = (p.getNomeLoja() == null || p.getNomeLoja().isEmpty()) ? p.getCodLoja() : p.getNomeLoja();
        String nomeVendedor = (p.getNomeVendedor() == null || p.getNomeVendedor().isEmpty()) ? p.getCodVendedor() : p.getNomeVendedor();
        String nomeZona = (p.getNomeZona() == null || p.getNomeZona().isEmpty()) ? p.getCodZona() : p.getNomeZona();

        String enderecoEscapado = mapper.escapeXml(p.getEndereco());
        String clienteEscapado = mapper.escapeXml(p.getNomeCliente());

        // Coordenadas de Fallback (Padrão do seu CD caso o cliente não possua no banco)
        String latDefault = "-20.797732132339135";
        String lngDefault = "-49.32830021380005";

        String latCliente = latDefault; 
        String lngCliente = lngDefault;

        return """
            <arg0>
               <cadastrarPontoInteresse>true</cadastrarPontoInteresse>
               <numeroPedido>%s</numeroPedido>
               <numeroCarregamento>%d</numeroCarregamento>
               <numeroNotaFiscal>%s</numeroNotaFiscal>
               
               <dataPedido>%s</dataPedido>
               <horaPedido>%s</horaPedido>
               
               <qtdItensPedido>1</qtdItensPedido>
               <valorPedido>%s</valorPedido>
               <descricao>%s</descricao>
               <codigoPontoInteresse>%s</codigoPontoInteresse>
               
               <loja>
                  <codigoLoja>%s</codigoLoja>
                  <nome>%s</nome>
               </loja>
               <vendedor>
                  <codigoVendedor>%s</codigoVendedor>
                  <nome>%s</nome>
               </vendedor>
               <zona>
                  <codigoZona>%s</codigoZona>
                  <nome>%s</nome>
               </zona>
               
               <horaEntregaInicial>08:00</horaEntregaInicial>
               <horaEntregaFinal>18:00</horaEntregaFinal>
               <tempoAtendimento>45</tempoAtendimento>
               <finalizacaoItinerarioAutomatico>true</finalizacaoItinerarioAutomatico>
               <dataCompromissoEntrega>%s</dataCompromissoEntrega>
               <horaCompromissoEntrega>%s</horaCompromissoEntrega>
               
               <peso>%s</peso>
               <volume>%s</volume>
               
               <endereco>%s</endereco>
               
               <poi>
                  <codigo>%s</codigo>
                  <nome>%s</nome>
                  <descricao>%s</descricao>
                  <latitude>%s</latitude>
                  <longitude>%s</longitude>
                  <grupo><nome>Clientes</nome></grupo>
                  <raio>0.1</raio>
                  <identificar>true</identificar>
                  <enviarAlerta>false</enviarAlerta>
                  <loja>
                     <codigoLoja>%s</codigoLoja>
                     <nome>%s</nome>
                  </loja>
               </poi>
            </arg0>
            """.formatted(
                p.getNumPed(), 
                p.getNumCar(),
                mapper.escapeXml(p.getNumNota()),
                
                p.getData().toString(), 
                p.getHora(),
                
                valorStr,
                enderecoEscapado,
                p.getCodCli(),
                
                p.getCodLoja(), nomeLoja,
                p.getCodVendedor(), nomeVendedor,
                p.getCodZona(), nomeZona,
                
                dataAtual, horaAtual,
                
                pesoStr, volStr,
                
                enderecoEscapado,
                
                p.getCodCli(), 
                clienteEscapado, 
                enderecoEscapado,
                latCliente, 
                lngCliente, 
                
                p.getCodLoja(), nomeLoja 
        );
    }
}