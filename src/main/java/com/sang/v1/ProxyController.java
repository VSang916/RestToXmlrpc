package com.sang.v1;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.MediaType;


@RestController
@RequestMapping("/api")
public class ProxyController {
    private final String TARGET_SERVER = "http://localhost:1237";
    private final RestTemplate restTemplate;
    private final XmlRpcConverter xmlRpcConverter;

    public ProxyController(RestTemplate restTemplate, XmlRpcConverter xmlRpcConverter) {
        this.restTemplate = restTemplate;
        this.xmlRpcConverter = xmlRpcConverter;
    }

    @GetMapping(value = "/rest/hello", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> proxyHelloRest(@RequestParam(required = false) String name) {
        String url = TARGET_SERVER + "/1?name=" + (name != null ? name : "");
        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
        return xmlRpcConverter.convertToMap(response.getBody());
    }

    @GetMapping(value = "/xmlrpc/hello", produces = MediaType.TEXT_XML_VALUE)
    public String proxyHelloXmlRpc(@RequestParam(required = false) String name) {
        String url = TARGET_SERVER + "/1?name=" + (name != null ? name : "");
        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
        Map<String, String> resultMap = xmlRpcConverter.convertToMap(response.getBody());
        return xmlRpcConverter.convertToXmlRpc(new HashMap<>(resultMap));
    }

    @PostMapping(value = "/rest/input", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> proxyInputRest(@RequestBody String input) {
        String url = TARGET_SERVER + "/cc";
        ResponseEntity<Map> response = restTemplate.postForEntity(url, input, Map.class);
        return xmlRpcConverter.convertToMap(response.getBody());
    }

    @PostMapping(value = "/xmlrpc/input", produces = MediaType.TEXT_XML_VALUE)
    public String proxyInputXmlRpc(@RequestBody String input) {
        String url = TARGET_SERVER + "/cc";
        ResponseEntity<Map> response = restTemplate.postForEntity(url, input, Map.class);
        Map<String, String> resultMap = xmlRpcConverter.convertToMap(response.getBody());
        return xmlRpcConverter.convertToXmlRpc(new HashMap<>(resultMap));
    }

    @GetMapping(value = "/rest/sua", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> proxySuaRest(@RequestParam(required = false) String name) {
        String url = TARGET_SERVER + "/sua?name=" + (name != null ? name : "minh");
        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
        return xmlRpcConverter.convertToMap(response.getBody());
    }

    @GetMapping(value = "/xmlrpc/sua", produces = MediaType.TEXT_XML_VALUE)
    public String proxySuaXmlRpc(@RequestParam(required = false) String name) {
        String url = TARGET_SERVER + "/sua?name=" + (name != null ? name : "minh");
        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
        Map<String, String> resultMap = xmlRpcConverter.convertToMap(response.getBody());
        return xmlRpcConverter.convertToXmlRpc(new HashMap<>(resultMap));
    }
    
    @GetMapping(value = "/xmlrpc/int", produces = MediaType.TEXT_XML_VALUE)
    public String proxyIntXmlRpc(@RequestParam(required = false) String nam) {
        String url = TARGET_SERVER + "/int?nam=" + (nam != null ? nam : "minh");
        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
        return xmlRpcConverter.convertToXmlRpc(response.getBody());
    }
    

    @PostMapping(value = "/xmlrpc", produces = MediaType.TEXT_XML_VALUE)
    public ResponseEntity<String> handleXmlRpcRequest(@RequestBody String xmlRpcRequest) {
        try {
            Map<String, Object> params = xmlRpcConverter.parseXmlRpcRequest(xmlRpcRequest);
            String method = (String) params.get("method");
            Map<String, String> result;
            
            switch (method) {
                case "hello":
                    result = proxyHelloRest((String) params.get("name"));
                    break;
                case "input":
                    result = proxyInputRest((String) params.get("input"));
                    break;
                case "sua":
                    result = proxySuaRest((String) params.get("name"));
                    break;
                default:
                    throw new IllegalArgumentException("Unknown method: " + method);
            }
            
            String xmlResponse = xmlRpcConverter.convertToXmlRpc(new HashMap<>(result));
            return ResponseEntity.ok()
                .contentType(MediaType.TEXT_XML)
                .body(xmlResponse);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to process XML-RPC request: " + e.getMessage());
            String xmlErrorResponse = xmlRpcConverter.convertToXmlRpc(new HashMap<>(errorResponse));
            return ResponseEntity.badRequest()
                .contentType(MediaType.TEXT_XML)
                .body(xmlErrorResponse);
        }
    }
}