package com.sang.v1;

import java.io.StringWriter;
import java.io.StringReader;
import java.util.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.springframework.stereotype.Component;

@Component
public class XmlRpcConverter {
    
    public String convertToXmlRpc(Map<String, Object> data) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();
            
            Element methodResponse = doc.createElement("methodResponse");
            doc.appendChild(methodResponse);
            
            Element params = doc.createElement("params");
            methodResponse.appendChild(params);
            
            Element param = doc.createElement("param");
            params.appendChild(param);
            
            Element value = doc.createElement("value");
            param.appendChild(value);
            
            addValue(doc, value, data);
            
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            
            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert to XML-RPC", e);
        }
    }

    private void addValue(Document doc, Element parent, Object value) {
        if (value == null) {
            Element nil = doc.createElement("nil");
            parent.appendChild(nil);
            return;
        }

        if (value instanceof Map) {
            Element struct = doc.createElement("struct");
            parent.appendChild(struct);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) value;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                Element member = doc.createElement("member");
                struct.appendChild(member);
                
                Element name = doc.createElement("name");
                name.setTextContent(entry.getKey());
                member.appendChild(name);
                
                Element memberValue = doc.createElement("value");
                member.appendChild(memberValue);
                
                addValue(doc, memberValue, entry.getValue());
            }
        } else if (value instanceof List || value.getClass().isArray()) {
            Element array = doc.createElement("array");
            parent.appendChild(array);
            
            Element data = doc.createElement("data");
            array.appendChild(data);
            
            List<?> list;
            if (value.getClass().isArray()) {
                list = Arrays.asList((Object[]) value);
            } else {
                list = (List<?>) value;
            }
            
            for (Object item : list) {
                Element itemValue = doc.createElement("value");
                data.appendChild(itemValue);
                addValue(doc, itemValue, item);
            }
        } else if (value instanceof Integer || value instanceof Long) {
            Element i4 = doc.createElement("i4");
            i4.setTextContent(String.valueOf(value));
            parent.appendChild(i4);
        } else if (value instanceof Double || value instanceof Float) {
            Element double_ = doc.createElement("double");
            double_.setTextContent(String.valueOf(value));
            parent.appendChild(double_);
        } else if (value instanceof Boolean) {
            Element boolean_ = doc.createElement("boolean");
            boolean_.setTextContent(((Boolean) value) ? "1" : "0");
            parent.appendChild(boolean_);
        } else {
            Element string = doc.createElement("string");
            string.setTextContent(String.valueOf(value));
            parent.appendChild(string);
        }
    }

    public Map<String, Object> parseXmlRpcRequest(String xmlRpcContent) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlRpcContent)));
            
            Map<String, Object> result = new HashMap<>();
            
            // Parse methodName
            NodeList methodNameNodes = doc.getElementsByTagName("methodName");
            if (methodNameNodes.getLength() > 0) {
                result.put("method", methodNameNodes.item(0).getTextContent());
            }
            
            // Parse parameters
            NodeList valueNodes = doc.getElementsByTagName("value");
            for (int i = 0; i < valueNodes.getLength(); i++) {
                Element valueElement = (Element) valueNodes.item(i);
                if (valueElement.getParentNode().getNodeName().equals("member")) {
                    Element memberElement = (Element) valueElement.getParentNode();
                    String name = memberElement.getElementsByTagName("name").item(0).getTextContent();
                    Object value = parseValue(valueElement);
                    result.put(name, value);
                }
            }
            
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse XML-RPC request", e);
        }
    }

    private Object parseValue(Element valueElement) {
        NodeList children = valueElement.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                String nodeName = child.getNodeName();
                String textContent = child.getTextContent();
                
                switch (nodeName) {
                    case "string":
                        return textContent;
                    case "i4":
                    case "int":
                        return Integer.parseInt(textContent);
                    case "double":
                        return Double.parseDouble(textContent);
                    case "boolean":
                        return "1".equals(textContent) || "true".equalsIgnoreCase(textContent);
                    case "nil":
                        return null;
                    case "array":
                        return parseArray((Element) child);
                    case "struct":
                        return parseStruct((Element) child);
                    default:
                        return textContent; // For any unrecognized type, treat as string
                }
            }
        }
        // If no type tag is present, treat the direct text content as a string
        return valueElement.getTextContent().trim();
    }

    private List<Object> parseArray(Element arrayElement) {
        List<Object> result = new ArrayList<>();
        Element dataElement = (Element) arrayElement.getElementsByTagName("data").item(0);
        NodeList valueElements = dataElement.getElementsByTagName("value");
        
        for (int i = 0; i < valueElements.getLength(); i++) {
            Element valueElement = (Element) valueElements.item(i);
            result.add(parseValue(valueElement));
        }
        
        return result;
    }

    private Map<String, Object> parseStruct(Element structElement) {
        Map<String, Object> result = new HashMap<>();
        NodeList memberElements = structElement.getElementsByTagName("member");
        
        for (int i = 0; i < memberElements.getLength(); i++) {
            Element memberElement = (Element) memberElements.item(i);
            String name = memberElement.getElementsByTagName("name").item(0).getTextContent();
            Element valueElement = (Element) memberElement.getElementsByTagName("value").item(0);
            result.put(name, parseValue(valueElement));
        }
        
        return result;
    }

    public Map<String, String> convertToMap(Map<String, Object> input) {
        if (input == null) {
            return new HashMap<>();
        }
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : input.entrySet()) {
            result.put(entry.getKey(), String.valueOf(entry.getValue()));
        }
        return result;
    }
}