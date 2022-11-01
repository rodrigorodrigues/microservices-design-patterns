package com.springboot.edgeserver.config;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.provider.OAuth2Request;

public class OAuth2RequestDeserializer extends JsonDeserializer<OAuth2Request> {
    @Override
    public OAuth2Request deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        ObjectMapper mapper = (ObjectMapper) parser.getCodec();
        JsonNode principal = mapper.readTree(parser);
        Map<String, String> requestParameters = JsonNodeUtils.findValue(principal, "requestParameters", JsonNodeUtils.STRING_OBJECT_STRING_MAP, mapper);
        String clientId = principal.get("clientId").textValue();
        boolean approved = principal.get("approved").asBoolean();
        Collection<GrantedAuthority> authorities = principal.findValues("authorities")
                .stream().map(j -> j.findValuesAsText("role"))
                .flatMap(List::stream)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
        Set<String> scope = JsonNodeUtils.findValue(principal, "scope", JsonNodeUtils.STRING_SET, mapper);
        Set<String> resourceIds = principal.findValues("resourceIds")
                .stream().filter(JsonNode::isArray)
                .flatMap(j -> StreamSupport.stream(Spliterators.spliteratorUnknownSize(j.elements(), Spliterator.ORDERED), false))
                .filter(JsonNode::isTextual)
                .map(JsonNode::textValue)
                .collect(Collectors.toSet());
        String redirectUri = principal.get("redirectUri").textValue();
        Set<String> responseTypes = principal.findValues("responseTypes")
                .stream().filter(JsonNode::isArray)
                .flatMap(j -> StreamSupport.stream(Spliterators.spliteratorUnknownSize(j.elements(), Spliterator.ORDERED), false))
                .filter(JsonNode::isTextual)
                .map(JsonNode::textValue)
                .collect(Collectors.toSet());
        Map<String, Serializable> extensionProperties = JsonNodeUtils.findValue(principal, "extensionProperties", JsonNodeUtils.STRING_OBJECT_MAP, mapper);
        return new OAuth2Request(requestParameters, clientId, authorities, approved, scope, resourceIds, redirectUri, responseTypes, extensionProperties);
    }

    abstract class JsonNodeUtils {

        static final TypeReference<Set<String>> STRING_SET = new TypeReference<>() {
        };

        static final TypeReference<Map<String, Serializable>> STRING_OBJECT_MAP = new TypeReference<>() {
        };

        static final TypeReference<Map<String, String>> STRING_OBJECT_STRING_MAP = new TypeReference<>() {
        };

        static String findStringValue(JsonNode jsonNode, String fieldName) {
            if (jsonNode == null) {
                return null;
            }
            JsonNode value = jsonNode.findValue(fieldName);
            return (value != null && value.isTextual()) ? value.asText() : null;
        }

        static <T> T findValue(JsonNode jsonNode, String fieldName, TypeReference<T> valueTypeReference,
                ObjectMapper mapper) {
            if (jsonNode == null) {
                return null;
            }
            JsonNode value = jsonNode.findValue(fieldName);
            return (value != null && value.isContainerNode()) ? mapper.convertValue(value, valueTypeReference) : null;
        }

        static JsonNode findObjectNode(JsonNode jsonNode, String fieldName) {
            if (jsonNode == null) {
                return null;
            }
            JsonNode value = jsonNode.findValue(fieldName);
            return (value != null && value.isObject()) ? value : null;
        }

    }
    /*

    abstract class JsonNodeUtils {

        static final TypeReference<Set<String>> STRING_SET = new TypeReference<Set<String>>() {
        };

        static final TypeReference<Map<String, String>> STRING_OBJECT_MAP = new TypeReference<Map<String, String>>() {
        };

        static final TypeReference<Map<String, Serializable>> STRING_OBJECT_MAP_SERIALIZABLE = new TypeReference<Map<String, Serializable>>() {
        };

        static final TypeReference<HashSet<? extends GrantedAuthority >> STRING_COLLECTION_AUTHORITY = new TypeReference<HashSet<? extends GrantedAuthority >>() {
        };

        static String findStringValue(JsonNode jsonNode, String fieldName) {
            if (jsonNode == null) {
                return null;
            }
            JsonNode value = jsonNode.findValue(fieldName);
            return (value != null && value.isTextual()) ? value.asText() : null;
        }

        static <T> T findValue(JsonNode jsonNode, String fieldName, TypeReference<T> valueTypeReference,
                ObjectMapper mapper) {
            if (jsonNode == null) {
                return null;
            }
            JsonNode value = jsonNode.findValue(fieldName);
            return (value != null && value.isContainerNode()) ? mapper.convertValue(value, valueTypeReference) : null;
        }

        static OAuth2Request generateOauth2Request(ObjectMapper mapper, JsonNode root) {
            JsonNode storedRequest = findObjectNode(root, "storedRequest");

            Map<String, String> requestParameters = mapper.convertValue(findObjectNode(storedRequest, "requestParameters"), STRING_OBJECT_MAP);
            String clientId = findObjectNode(storedRequest, "clientId").textValue();
            Collection<? extends GrantedAuthority > authorities = mapper.convertValue(findObjectNode(storedRequest, "authorities"), STRING_COLLECTION_AUTHORITY);
            boolean approved = findObjectNode(storedRequest, "approved").asBoolean();
            Set<String> scope = mapper.convertValue(findObjectNode(storedRequest, "scope"), STRING_SET);
            Set<String> resourceIds = mapper.convertValue(findObjectNode(storedRequest, "resourceIds"), STRING_SET);
            String redirectUri = findObjectNode(storedRequest, "redirectUri").textValue();
            Set<String> responseTypes = mapper.convertValue(findObjectNode(storedRequest, "responseTypes"), STRING_SET);
            Map<String, Serializable > extensionProperties = mapper.convertValue(findObjectNode(storedRequest, "extensionProperties"), STRING_OBJECT_MAP_SERIALIZABLE);

            return new OAuth2Request(requestParameters, clientId, authorities, approved, scope, resourceIds, redirectUri, responseTypes, extensionProperties);
        }

        static Authentication generateOAuth2Authentication(ObjectMapper mapper, JsonNode root) {
            JsonNode userAuthentication = findObjectNode(root, "userAuthentication");
            return new Authentication() {
                @Override
                public Collection<? extends GrantedAuthority> getAuthorities() {
                    return mapper.convertValue(findObjectNode(userAuthentication, "authorities"), STRING_COLLECTION_AUTHORITY);
                }

                @Override
                public Object getCredentials() {
                    return null;
                }

                @Override
                public Object getDetails() {
                    return null;
                }

                @Override
                public Object getPrincipal() {
                    return null;
                }

                @Override
                public boolean isAuthenticated() {
                    return findObjectNode(userAuthentication, "authenticated").asBoolean();
                }

                @Override
                public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
                }

                @Override
                public String getName() {
                    return findObjectNode(userAuthentication, "name").textValue();
                }
            };
        }

        static JsonNode findObjectNode(JsonNode jsonNode, String fieldName) {
            if (jsonNode == null) {
                return null;
            }
            return jsonNode.findValue(fieldName);
        }

    }*/
}
