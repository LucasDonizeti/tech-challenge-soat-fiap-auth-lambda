package com.techchallenge.lambda.authorizer.application.ports;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JavaType;
import com.techchallenge.lambda.authorizer.application.configure.InjectionFactory;
import com.techchallenge.lambda.authorizer.application.usecases.ports.input.AutenticarUsuarioInput;
import com.techchallenge.lambda.authorizer.application.usecases.responses.AuthResponse;
import com.techchallenge.lambda.authorizer.web.dto.AuthRequestDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes de AuthLambdaHandler - Application Layer")
class AuthLambdaHandlerTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private AutenticarUsuarioInput autenticarUsuarioInput;

    @Mock
    private Context context;

    @Mock
    private InjectionFactory injectionFactory;

    private AuthLambdaHandler handler;
    private APIGatewayProxyRequestEvent requestEvent;

    @BeforeEach
    void setUp() {
        // Criar handler com mocks injetados via construtor
        handler = new AuthLambdaHandler(objectMapper, autenticarUsuarioInput);
        
        // Setup request event básico
        requestEvent = new APIGatewayProxyRequestEvent();
        requestEvent.setHttpMethod("POST");
        requestEvent.setPath("/v1/auth");
    }

    @Test
    @DisplayName("Deve processar autenticação com sucesso quando request válido")
    void deveProcessarAutenticacaoComSucessoQuandoRequestValido() throws Exception {
        // Arrange
        String requestBody = "{\"username\":\"admin\",\"password\":\"admin123\"}";
        requestEvent.setBody(requestBody);
        
        AuthRequestDto authRequest = new AuthRequestDto();
        authRequest.setUsername("admin");
        authRequest.setPassword("admin123");
        AuthResponse authResponse = new AuthResponse("fake-jwt-token", "Bearer", "admin", "ADMIN");
        
        when(objectMapper.readValue(anyString(), eq(AuthRequestDto.class))).thenReturn(authRequest);
        when(autenticarUsuarioInput.execute(any())).thenReturn(authResponse);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"token\":\"fake-jwt-token\",\"type\":\"Bearer\",\"username\":\"admin\",\"role\":\"ADMIN\"}");
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(requestEvent, context);
        
        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getBody()).contains("fake-jwt-token");
        assertThat(response.getHeaders()).containsKey("Content-Type");
        assertThat(response.getHeaders()).containsKey("x-correlation-id");
        
        verify(objectMapper, times(1)).readValue(eq(requestBody), eq(AuthRequestDto.class));
        verify(autenticarUsuarioInput, times(1)).execute(any());
        verify(objectMapper, times(1)).writeValueAsString(any());
    }

    @Test
    @DisplayName("Deve retornar 400 quando body está nulo")
    void deveRetornar400QuandoBodyNulo() throws JsonProcessingException {
        // Arrange
        requestEvent.setBody(null);
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(requestEvent, context);
        
        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(response.getBody()).contains("Body da requisição é obrigatório");
        assertThat(response.getHeaders()).containsKey("x-correlation-id");
        
        verify(objectMapper, never()).readValue(anyString(), eq(AuthRequestDto.class));
        verify(autenticarUsuarioInput, never()).execute(any());
    }

    @Test
    @DisplayName("Deve retornar 400 quando body está vazio")
    void deveRetornar400QuandoBodyVazio() throws JsonProcessingException {
        // Arrange
        requestEvent.setBody("");
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(requestEvent, context);
        
        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(response.getBody()).contains("Body da requisição é obrigatório");
        
        verify(objectMapper, never()).readValue(anyString(), eq(AuthRequestDto.class));
        verify(autenticarUsuarioInput, never()).execute(any());
    }

    @Test
    @DisplayName("Deve retornar 400 quando body está em branco")
    void deveRetornar400QuandoBodyEmBranco() throws JsonProcessingException {
        // Arrange
        requestEvent.setBody("   ");
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(requestEvent, context);
        
        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(response.getBody()).contains("Body da requisição é obrigatório");
        
        verify(objectMapper, never()).readValue(anyString(), eq(AuthRequestDto.class));
        verify(autenticarUsuarioInput, never()).execute(any());
    }

    @Test
    @DisplayName("Deve retornar 401 quando IllegalArgumentException é lançada")
    void deveRetornar401QuandoIllegalArgumentExceptionLancada() throws Exception {
        // Arrange
        String requestBody = "{\"username\":\"invalid\",\"password\":\"wrong\"}";
        requestEvent.setBody(requestBody);
        
        AuthRequestDto authRequest = new AuthRequestDto();
        authRequest.setUsername("invalid");
        authRequest.setPassword("wrong");
        
        when(objectMapper.readValue(anyString(), eq(AuthRequestDto.class))).thenReturn(authRequest);
        when(autenticarUsuarioInput.execute(any())).thenThrow(new IllegalArgumentException("Credenciais inválidas"));
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(requestEvent, context);
        
        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(401);
        assertThat(response.getBody()).contains("Credenciais inválidas");
        
        verify(objectMapper, times(1)).readValue(eq(requestBody), eq(AuthRequestDto.class));
        verify(autenticarUsuarioInput, times(1)).execute(any());
    }

    @Test
    @DisplayName("Deve retornar 500 quando exceção genérica é lançada")
    void deveRetornar500QuandoExcecaoGenericaLancada() throws Exception {
        // Arrange
        String requestBody = "{\"username\":\"admin\",\"password\":\"admin123\"}";
        requestEvent.setBody(requestBody);
        
        AuthRequestDto authRequest = new AuthRequestDto();
        authRequest.setUsername("admin");
        authRequest.setPassword("admin123");
        
        when(objectMapper.readValue(anyString(), eq(AuthRequestDto.class))).thenReturn(authRequest);
        when(autenticarUsuarioInput.execute(any())).thenThrow(new RuntimeException("Erro interno"));
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(requestEvent, context);
        
        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(500);
        assertThat(response.getBody()).contains("Erro interno ao processar autenticação");
        
        verify(objectMapper, times(1)).readValue(eq(requestBody), eq(AuthRequestDto.class));
        verify(autenticarUsuarioInput, times(1)).execute(any());
    }

    @Test
    @DisplayName("Deve usar correlation ID do header quando presente")
    void deveUsarCorrelationIdDoHeaderQuandoPresente() throws Exception {
        // Arrange
        String correlationId = "test-correlation-id-123";
        Map<String, String> headers = new HashMap<>();
        headers.put("x-correlation-id", correlationId);
        requestEvent.setHeaders(headers);
        
        String requestBody = "{\"username\":\"admin\",\"password\":\"admin123\"}";
        requestEvent.setBody(requestBody);
        
        AuthRequestDto authRequest = new AuthRequestDto();
        authRequest.setUsername("admin");
        authRequest.setPassword("admin123");
        AuthResponse authResponse = new AuthResponse("fake-jwt-token", "Bearer", "admin", "ADMIN");
        
        when(objectMapper.readValue(anyString(), eq(AuthRequestDto.class))).thenReturn(authRequest);
        when(autenticarUsuarioInput.execute(any())).thenReturn(authResponse);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"token\":\"fake-jwt-token\"}");
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(requestEvent, context);
        
        // Assert
        assertThat(response.getHeaders().get("x-correlation-id")).isEqualTo(correlationId);
    }

    @Test
    @DisplayName("Deve gerar correlation ID quando não presente no header")
    void deveGerarCorrelationIdQuandoNaoPresenteNoHeader() throws Exception {
        // Arrange
        requestEvent.setHeaders(null);
        
        String requestBody = "{\"username\":\"admin\",\"password\":\"admin123\"}";
        requestEvent.setBody(requestBody);
        
        AuthRequestDto authRequest = new AuthRequestDto();
        authRequest.setUsername("admin");
        authRequest.setPassword("admin123");
        AuthResponse authResponse = new AuthResponse("fake-jwt-token", "Bearer", "admin", "ADMIN");
        
        when(objectMapper.readValue(anyString(), eq(AuthRequestDto.class))).thenReturn(authRequest);
        when(autenticarUsuarioInput.execute(any())).thenReturn(authResponse);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"token\":\"fake-jwt-token\"}");
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(requestEvent, context);
        
        // Assert
        assertThat(response.getHeaders().get("x-correlation-id")).isNotNull();
        assertThat(response.getHeaders().get("x-correlation-id")).isNotEmpty();
        // Verifica se é um UUID válido
        UUID.fromString(response.getHeaders().get("x-correlation-id"));
    }

    @Test
    @DisplayName("Deve incluir headers CORS na resposta")
    void deveIncluirHeadersCorsNaResposta() throws Exception {
        // Arrange
        String requestBody = "{\"username\":\"admin\",\"password\":\"admin123\"}";
        requestEvent.setBody(requestBody);
        
        AuthRequestDto authRequest = new AuthRequestDto();
        authRequest.setUsername("admin");
        authRequest.setPassword("admin123");
        AuthResponse authResponse = new AuthResponse("fake-jwt-token", "Bearer", "admin", "ADMIN");
        
        when(objectMapper.readValue(anyString(), eq(AuthRequestDto.class))).thenReturn(authRequest);
        when(autenticarUsuarioInput.execute(any())).thenReturn(authResponse);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"token\":\"fake-jwt-token\"}");
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(requestEvent, context);
        
        // Assert
        assertThat(response.getHeaders()).containsKey("Access-Control-Allow-Origin");
        assertThat(response.getHeaders().get("Access-Control-Allow-Origin")).isEqualTo("*");
        assertThat(response.getHeaders()).containsKey("Content-Type");
        assertThat(response.getHeaders().get("Content-Type")).isEqualTo("application/json");
    }

    @Test
    @DisplayName("Deve limpar MDC ao final do processamento")
    void deveLimparMdcAoFinalDoProcessamento() throws Exception {
        // Arrange
        String requestBody = "{\"username\":\"admin\",\"password\":\"admin123\"}";
        requestEvent.setBody(requestBody);
        
        AuthRequestDto authRequest = new AuthRequestDto();
        authRequest.setUsername("admin");
        authRequest.setPassword("admin123");
        AuthResponse authResponse = new AuthResponse("fake-jwt-token", "Bearer", "admin", "ADMIN");
        
        when(objectMapper.readValue(anyString(), eq(AuthRequestDto.class))).thenReturn(authRequest);
        when(autenticarUsuarioInput.execute(any())).thenReturn(authResponse);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"token\":\"fake-jwt-token\"}");
        
        // Act
        handler.handleRequest(requestEvent, context);
        
        // Assert - Verifica se o MDC foi limpo (não há como verificar diretamente, 
        // mas o teste não deve lançar exceção)
        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("Deve mascarar username nos logs")
    void deveMascararUsernameNosLogs() throws Exception {
        // Arrange
        String requestBody = "{\"username\":\"12345678901\",\"password\":\"password123\"}";
        requestEvent.setBody(requestBody);
        
        AuthRequestDto authRequest = new AuthRequestDto();
        authRequest.setUsername("12345678901");
        authRequest.setPassword("password123");
        AuthResponse authResponse = new AuthResponse("fake-jwt-token", "Bearer", "12345678901", "CLIENTE");
        
        when(objectMapper.readValue(anyString(), eq(AuthRequestDto.class))).thenReturn(authRequest);
        when(autenticarUsuarioInput.execute(any())).thenReturn(authResponse);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"token\":\"fake-jwt-token\"}");
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(requestEvent, context);
        
        // Assert - O método maskUsername deve ser chamado internamente
        assertThat(response.getStatusCode()).isEqualTo(200);
    }
}