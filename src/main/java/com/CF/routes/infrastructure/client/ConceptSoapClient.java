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

@Slf4j
@Service
@RequiredArgsConstructor
public class ConceptSoapClient {

    private final ConceptConfig config;
    private final PedidoMapper mapper;

    private static final String NS_FACHADA = "http://fachada.concept/";

    /**
     * Dashboard: Lista o itinerário detalhado de um carregamento.
     * Obs: Redirecionado para o endpoint do importador conforme testes funcionais realizados.
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
        
        // URL específica identificada em testes onde o método responde corretamente
        String urlCorreta = "http://52.6.27.50:8181/importadorPedidos";
        
        return enviarRequestComRetorno(urlCorreta, xml, "listarItinerariosCarregamento");
    }

    /**
     * Consulta a lista de viagens ativas para o painel de monitoramento.
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
     * Sincronização: Cadastra Zona/Praça antes da importação.
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
     * Sincronização: Cadastra Loja com coordenadas de geofence.
     */
    public void cadastrarLoja(String codigo, String nome) {
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
     * Sincronização: Cadastra o motorista garantindo a máscara de CPF.
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
     * Importação: Envio do lote massivo de pedidos concatenados.
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
     * Roteirização: Disparo final do algoritmo da Concept.
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

    private void enviarRequest(String url, String xmlBody, String operacao) {
        enviarRequestComRetorno(url, xmlBody, operacao);
    }

    private String enviarRequestComRetorno(String url, String xmlBody, String operacao) {
        long start = System.currentTimeMillis();
        
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000); 
        factory.setReadTimeout(45000); 
        
        RestTemplate rt = new RestTemplate(factory);
        rt.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("text/xml;charset=UTF-8"));
        headers.set("SOAPAction", "\"\"");
        
        try {
            HttpEntity<String> entity = new HttpEntity<>(xmlBody, headers);
            log.info("[SOAP] Enviando '{}' para {}", operacao, url);
            
            ResponseEntity<String> response = rt.postForEntity(url, entity, String.class);
            String body = response.getBody();
            long duration = System.currentTimeMillis() - start;
            
            log.info("[SOAP] Resposta de '{}' recebida em {}ms.", operacao, duration);
            return body;
        } catch (Exception e) {
            log.error("[SOAP] Falha na operação {}: {}", operacao, e.getMessage());
            throw new RuntimeException("Erro Concept API: " + e.getMessage());
        }
    }
}