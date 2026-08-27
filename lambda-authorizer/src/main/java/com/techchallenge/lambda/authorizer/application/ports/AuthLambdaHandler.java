package com.techchallenge.lambda.authorizer.application.ports;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techchallenge.lambda.authorizer.application.configure.InjectionFactory;
import com.techchallenge.lambda.authorizer.application.usecases.ports.input.AutenticarUsuarioInput;
import com.techchallenge.lambda.authorizer.application.usecases.responses.AuthResponse;
import com.techchallenge.lambda.authorizer.web.dto.AuthRequestDto;
import com.techchallenge.lambda.authorizer.web.dto.AuthResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Map;
import java.util.UUID;

public class AuthLambdaHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final Logger logger = LoggerFactory.getLogger(AuthLambdaHandler.class);
    private static final String CORRELATION_ID_HEADER = "x-correlation-id";
    private static final String MDC_KEY = "correlationId";

    private final ObjectMapper objectMapper;
    private final AutenticarUsuarioInput autenticarUsuarioInput;

    public AuthLambdaHandler() {
        this(InjectionFactory.getObjectMapper(), InjectionFactory.getAutenticarUsuarioInput());
    }

    // Construtor para injeção de dependências (útil para testes)
    public AuthLambdaHandler(ObjectMapper objectMapper, AutenticarUsuarioInput autenticarUsuarioInput) {
        this.objectMapper = objectMapper;
        this.autenticarUsuarioInput = autenticarUsuarioInput;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, Context context) {
        // Setup correlation ID
        String correlationId = setupCorrelationId(apiGatewayProxyRequestEvent);
        
        Map<String, String> headers = Map.of(
                "Content-Type", "application/json",
                "Access-Control-Allow-Origin", "*",
                CORRELATION_ID_HEADER, correlationId
        );

        try{
            logger.info("Iniciando processamento da requisição de autenticação - correlationId: {}, method: {}, path: {}", 
                    correlationId, apiGatewayProxyRequestEvent.getHttpMethod(), apiGatewayProxyRequestEvent.getPath());

            String bodyString = apiGatewayProxyRequestEvent.getBody();
            if (bodyString == null || bodyString.isBlank()) {
                logger.warn("Body da requisição está vazio ou nulo - correlationId: {}", correlationId);
                return buildResponse(400, headers, "{\"error\": \"Body da requisição é obrigatório\"}");
            }

            logger.debug("Parseando corpo da requisição - correlationId: {}, bodyLength: {}", correlationId, bodyString.length());
            AuthRequestDto authRequest = objectMapper.readValue(bodyString, AuthRequestDto.class);
            
            logger.info("Requisição de autenticação recebida - correlationId: {}, username: {}", 
                    correlationId, maskUsername(authRequest.getUsername()));

            AuthResponseDto responseDto = callInput(authRequest, correlationId);
            
            logger.info("Autenticação concluída com sucesso - correlationId: {}, username: {}", 
                    correlationId, maskUsername(responseDto.getUsername()));
            return buildResponse(200, headers, objectMapper.writeValueAsString(responseDto));

        } catch (IllegalArgumentException e){
            logger.error("Erro de validação na autenticação - correlationId: {}, erro: {}", correlationId, e.getMessage());
            return buildResponse(401, headers, "{\"error\": \"Credenciais inválidas\"}");
        } catch (Exception e){
            logger.error("Erro inesperado durante o processamento - correlationId: {}, erro: {}, stackTrace: {}", 
                    correlationId, e.getMessage(), getStackTraceAsString(e));
            return buildResponse(500, headers, "{\"error\": \"Erro interno ao processar autenticação\"}");
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    private String setupCorrelationId(APIGatewayProxyRequestEvent request) {
        String correlationId = null;
        if (request.getHeaders() != null) {
            correlationId = request.getHeaders().get(CORRELATION_ID_HEADER);
        }
        
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
            logger.debug("Gerando novo correlationId: {}", correlationId);
        }
        
        MDC.put(MDC_KEY, correlationId);
        return correlationId;
    }

    public AuthResponseDto callInput(AuthRequestDto authRequest, String correlationId){
        logger.debug("Executando caso de uso de autenticação - correlationId: {}", correlationId);
        AuthResponse authResponse = autenticarUsuarioInput.execute(authRequest.toCommand());
        logger.info("Token gerado com sucesso - correlationId: {}, username: {}, tipoToken: {}", 
                correlationId, maskUsername(authResponse.getUsername()), authResponse.getType());
        return AuthResponseDto.from(authResponse);
    }

    private String maskUsername(String username) {
        if (username == null || username.length() <= 4) {
            return "***";
        }
        return username.substring(0, 2) + "***" + username.substring(username.length() - 2);
    }

    private String getStackTraceAsString(Exception e) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    private APIGatewayProxyResponseEvent buildResponse(int statusCode, Map<String, String> headers, String body) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withHeaders(headers)
                .withBody(body);
    }

}
