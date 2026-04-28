package com.CF.routes.infrastructure.client;

import com.CF.routes.domain.mapper.PedidoMapper;
import com.CF.routes.infrastructure.config.ConceptConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Engenheiro Sénior: Cliente SOAP de alta performance.
 * Centraliza as comunicações com a API Concept GPS.
 * Redireciona chamadas específicas para endpoints funcionais observados em homologação.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConceptSoapClient {

    private final ConceptConfig config;
    private final PedidoMapper mapper;

    // Namespace padrão para os métodos da fachada Concept
    private static final String NS_FACHADA = "http://fachada.concept/";

    /**
     * Dashboard: Lista o itinerário detalhado de um carregamento.
     * Obs: Redirecionado para o endpoint do importador conforme testes funcionais.
     */
    public String listarItinerariosCarregamento(Long numCar) {
        String xml = """
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:fac="%s">
               <soapenv:Header/>
               <soapenv:Body>
                  <fac:listarItinerariosCarregamento>
                     <arg0>%d</arg0>
                     <arg1>%s</arg1>
                     <arg2>%s</arg2>
                     <arg3>%s</arg3>
                  </fac:listarItinerariosCarregamento>
               </soapenv:Body>
            </soapenv:Envelope>
            """.formatted(NS_FACHADA, numCar, config.getCnpj(), config.getSenhaCliente(), config.getSenhaCentral());
        
        // URL específica onde o método 'listarItinerariosCarregamento' está respondendo
        String urlCorreta = "http://52.6.27.50:8181/importadorPedidos";
        
        return enviarRequestComRetorno(urlCorreta, xml, "listarItinerariosCarregamento");
    }

    /**
     * Consulta a lista de viagens ativas na plataforma Concept.
     */
    public String listarViagens() {
        String xml = """
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:fac="%s">
               <soapenv:Header/>
               <soapenv:Body>
                  <fac:listarViagens>
                     <arg0>%s</arg0>
                     <arg1>%s</arg1>
                     <arg2>%s</arg2>
                  </fac:listarViagens>
               </soapenv:Body>
            </soapenv:Envelope>
            """.formatted(NS_FACHADA, config.getCnpj(), config.getSenhaCliente(), config.getSenhaCentral());
        
        return enviarRequestComRetorno(config.getEndpoints().getAutomatizador(), xml, "listarViagens");
    }

    /**
     * Cadastra ou atualiza uma zona (praça) no sistema Concept.
     */
    public void cadastrarZona(String codigo, String nome) {
        String xml = """
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:fac="%s">
               <soapenv:Body>
                  <fac:cadastrarZona>
                     <arg0><id>0</id><codigoZona>%s</codigoZona><nome>%s</nome></arg0>
                     <arg1>%s</arg1><arg2>%s</arg2><arg3>%s</arg3>
                  </fac:cadastrarZona>
               </soapenv:Body>
            </soapenv:Envelope>
            """.formatted(NS_FACHADA, codigo, mapper.escapeXml(nome), config.getCnpj(), config.getSenhaCliente(), config.getSenhaCentral());
        enviarRequest(config.getEndpoints().getAutomatizador(), xml, "cadastrarZona");
    }

    /**
     * Cadastra ou atualiza uma loja (cliente) com coordenadas geográficas.
     */
    public void cadastrarLoja(String codigo, String nome) {
        // Coordenadas padrão (podem ser parametrizadas futuramente)
        String lat = "-20.797732132339135";
        String lng = "-49.32830021380005";
        
        String xml = """
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:fac="%s">
               <soapenv:Body>
                  <fac:cadastrarLoja>
                     <arg0>
                        <id>0</id>
                        <codigoLoja>%s</codigoLoja>
                        <nome>%s</nome>
                        <latitude>%s</latitude>
                        <longitude>%s</longitude>
                        <raio>500</raio>
                     </arg0>
                     <arg1>%s</arg1><arg2>%s</arg2><arg3>%s</arg3>
                  </fac:cadastrarLoja>
               </soapenv:Body>
            </soapenv:Envelope>
            """.formatted(NS_FACHADA, codigo, mapper.escapeXml(nome), lat, lng, config.getCnpj(), config.getSenhaCliente(), config.getSenhaCentral());
        enviarRequest(config.getEndpoints().getAutomatizador(), xml, "cadastrarLoja");
    }

    /**
     * Cadastra um motorista no sistema para fins de roteirização.
     */
    public void cadastrarMotorista(String matricula, String nome, String cpf) {
        String xml = """
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:fac="%s">
               <soapenv:Body>
                  <fac:cadastrarMotorista>
                     <arg0>
                        <id>0</id>
                        <matricula>%s</matricula>
                        <nome>%s</nome>
                        <cpf>%s</cpf>
                     </arg0>
                     <arg1>%s</arg1><arg2>%s</arg2><arg3>%s</arg3>
                  </fac:cadastrarMotorista>
               </soapenv:Body>
            </soapenv:Envelope>
            """.formatted(NS_FACHADA, matricula, mapper.escapeXml(nome), mapper.formatarCpf(cpf), config.getCnpj(), config.getSenhaCliente(), config.getSenhaCentral());
        enviarRequest(config.getEndpoints().getAutomatizador(), xml, "cadastrarMotorista");
    }

    /**
     * Importa blocos de pedidos (XML fragmentado) para o sistema.
     */
    public void importarPedidos(String blocosArg0Xml) {
        String xml = """
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:fac="%s">
               <soapenv:Body>
                  <fac:importarPedidos>
                     %s
                     <arg1>%s</arg1>
                     <arg2>%s</arg2>
                     <arg3>%s</arg3>
                  </fac:importarPedidos>
               </soapenv:Body>
            </soapenv:Envelope>
            """.formatted(NS_FACHADA, blocosArg0Xml, config.getCnpj(), config.getSenhaCliente(), config.getSenhaCentral());
        enviarRequest(config.getEndpoints().getImportador(), xml, "importarPedidos");
    }

    /**
     * Dispara o comando de roteirização para uma carga específica.
     */
    public void roteirizarPedidos(Long numCar, String placa, String motorista) {
        String dataIso = OffsetDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        
        String xml = """
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:fac="%s">
               <soapenv:Body>
                  <fac:roteirizarPedidos>
                     <arg0>0</arg0>
                     <arg1>%d</arg1>
                     <arg2>127695</arg2>
                     <arg3>127695</arg3>
                     <arg4>%s</arg4>
                     <arg5>%s</arg5>
                     <arg6>%s</arg6>
                     <arg7>TRUE</arg7>
                     <arg8>%s</arg8>
                     <arg9>%s</arg9>
                     <arg10>%s</arg10>
                  </fac:roteirizarPedidos>
               </soapenv:Body>
            </soapenv:Envelope>
            """.formatted(NS_FACHADA, numCar, dataIso, placa, motorista, config.getCnpj(), config.getSenhaCliente(), config.getSenhaCentral());
        enviarRequest(config.getEndpoints().getAutomatizador(), xml, "roteirizarPedidos");
    }

    /**
     * Executa a requisição HTTP POST para o webservice Concept.
     */
    private void enviarRequest(String url, String xmlBody, String operacao) {
        enviarRequestComRetorno(url, xmlBody, operacao);
    }

    /**
     * Método central de envio com tratamento de timeouts e cabeçalhos SOAP.
     */
    private String enviarRequestComRetorno(String url, String xmlBody, String operacao) {
        long start = System.currentTimeMillis();
        
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);  // 5 seg de conexão
        factory.setReadTimeout(45000);     // 45 seg de leitura (API SOAP pode ser lenta)
        
        RestTemplate rt = new RestTemplate(factory);
        rt.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("text/xml;charset=UTF-8"));
        
        // SOAPAction vazio resolve problemas de despacho em servidores JAX-WS / Glassfish
        headers.set("SOAPAction", "\"\"");
        
        try {
            HttpEntity<String> entity = new HttpEntity<>(xmlBody, headers);
            log.info("[SOAP] Enviando '{}' para {}", operacao, url);
            
            ResponseEntity<String> response = rt.postForEntity(url, entity, String.class);
            
            String body = response.getBody();
            long duration = System.currentTimeMillis() - start;
            
            if (body != null) {
                log.info("[SOAP] Resposta de '{}' recebida em {}ms. Tamanho: {} bytes.", 
                        operacao, duration, body.length());
            }
            
            return body;
        } catch (Exception e) {
            log.error("[SOAP] Falha crítica na operação {}: {}", operacao, e.getMessage());
            throw new RuntimeException("Erro na integração Concept: " + e.getMessage());
        }
    }
}