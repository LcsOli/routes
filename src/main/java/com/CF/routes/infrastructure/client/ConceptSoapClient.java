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

    public void cadastrarZona(String codigo, String nome) {
        String xml = """
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:fac="http://fachada.concept/">
               <soapenv:Body>
                  <fac:cadastrarZona>
                     <arg0><id>0</id><codigoZona>%s</codigoZona><nome>%s</nome></arg0>
                     <arg1>%s</arg1><arg2>%s</arg2><arg3>%s</arg3>
                  </fac:cadastrarZona>
               </soapenv:Body>
            </soapenv:Envelope>
            """.formatted(codigo, mapper.escapeXml(nome), config.getCnpj(), config.getSenhaCliente(), config.getSenhaCentral());
        enviarRequest(config.getEndpoints().getAutomatizador(), xml, "cadastrarZona");
    }

    public void cadastrarLoja(String codigo, String nome) {
        String lat = "-20.797732132339135";
        String lng = "-49.32830021380005";

        String xml = """
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:fac="http://fachada.concept/">
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
            """.formatted(codigo, mapper.escapeXml(nome), lat, lng, config.getCnpj(), config.getSenhaCliente(), config.getSenhaCentral());
        enviarRequest(config.getEndpoints().getAutomatizador(), xml, "cadastrarLoja");
    }

    public void cadastrarMotorista(String matricula, String nome, String cpf) {
        String xml = """
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:fac="http://fachada.concept/">
               <soapenv:Body>
                  <fac:cadastrarMotorista>
                     <arg0><id>0</id><matricula>%s</matricula><nome>%s</nome><cpf>%s</cpf></arg0>
                     <arg1>%s</arg1><arg2>%s</arg2><arg3>%s</arg3>
                  </fac:cadastrarMotorista>
               </soapenv:Body>
            </soapenv:Envelope>
            """.formatted(matricula, mapper.escapeXml(nome), mapper.formatarCpf(cpf), config.getCnpj(), config.getSenhaCliente(), config.getSenhaCentral());
        enviarRequest(config.getEndpoints().getAutomatizador(), xml, "cadastrarMotorista");
    }

    public void importarPedidos(String blocosArg0Xml) {
        String xml = """
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:fac="http://fachada.concept/">
               <soapenv:Header/>
               <soapenv:Body>
                  <fac:importarPedidos>
                     %s
                     <arg1>%s</arg1>
                     <arg2>%s</arg2>
                     <arg3>%s</arg3>
                  </fac:importarPedidos>
               </soapenv:Body>
            </soapenv:Envelope>
            """.formatted(blocosArg0Xml, config.getCnpj(), config.getSenhaCliente(), config.getSenhaCentral());
        
        enviarRequest(config.getEndpoints().getImportador(), xml, "importarPedidos");
    }

    public void roteirizarPedidos(Long numCar, String placa, String motorista) {
        String dataIso = OffsetDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        
        String xml = """
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:fac="http://fachada.concept/">
               <soapenv:Header/>
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
            """.formatted(numCar, dataIso, placa, motorista, config.getCnpj(), config.getSenhaCliente(), config.getSenhaCentral());
            
        enviarRequest(config.getEndpoints().getAutomatizador(), xml, "roteirizarPedidos");
    }

    private void enviarRequest(String url, String xmlBody, String operacao) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(30000);

        RestTemplate rt = new RestTemplate(factory);
        rt.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("text/xml;charset=UTF-8"));
        
        log.info("------------------------------------------------------------");
        log.info("ENVIANDO REQUEST SOAP - OPERAÇÃO: {}", operacao);
        log.info("XML ENVIADO:\n{}", xmlBody);

        try {
            HttpEntity<String> entity = new HttpEntity<>(xmlBody, headers);
            ResponseEntity<String> response = rt.postForEntity(url, entity, String.class);
            log.info("RESPOSTA DA API ({}) - STATUS: {}", operacao, response.getStatusCode());
            log.info("BODY DA RESPOSTA:\n{}", response.getBody());
        } catch (Exception e) {
            log.error("ERRO NA OPERAÇÃO {}: {}", operacao, e.getMessage());
            throw new RuntimeException("Falha na comunicação com a Concept: " + e.getMessage());
        }
    }
}