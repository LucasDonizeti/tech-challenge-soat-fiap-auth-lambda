package com.techchallenge.lambda.authorizer.application.ports;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techchallenge.lambda.authorizer.application.usecases.ports.input.AutenticarUsuarioInput;
import com.techchallenge.lambda.authorizer.infrastructure.security.SecurityConfig;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

@RequiredArgsConstructor
public class AuthLambdaHandler implements RequestStreamHandler {

    private final ObjectMapper objectMapper;

    private final AutenticarUsuarioInput autenticarUsuarioInput;

    private static final Logger logger = LoggerFactory.getLogger(AuthLambdaHandler.class);

    private static final SpringLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;

    static {
        try {
            handler = SpringLambdaContainerHandler.getAwsProxyHandler(SecurityConfig.class);
        } catch (ContainerInitializationException e) {
            throw new RuntimeException("Falha ao inicializar Spring", e);
        }
    }


    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        handler.proxyStream(inputStream, outputStream, context);
    }
    /*

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, Context context) {
        Map<String, String> headers = Map.of(
                "Content-Type", "application/json",
                "Access-Control-Allow-Origin", "*"
        );

        try{
            logger.info("Iniciando processamento da requisição de login...");

            String bodyString = apiGatewayProxyRequestEvent.getBody();
            if (bodyString == null || bodyString.isBlank()) {
                return buildResponse(400, headers, "{\"error\": \"Body da requisição é obrigatório\"}");
            }

            AuthRequestDto authRequest = objectMapper.readValue(bodyString, AuthRequestDto.class);

            AuthResponse authResponse = autenticarUsuarioInput.execute(authRequest.toCommand());
            AuthResponseDto responseDto = AuthResponseDto.from(authResponse);
            logger.info("Login bem-sucedido para o usuário: {}", authResponse.getUsername());

            return buildResponse(200, headers, objectMapper.writeValueAsString(responseDto));

        } catch (Exception e){
            context.getLogger().log("Erro durante o processamento: " + e.getMessage());
            return buildResponse(400, headers, "{\"error\": \"Erro ao processar autenticação: " + e.getMessage() + "\"}");
        }
    }


     */
    private APIGatewayProxyResponseEvent buildResponse(int statusCode, Map<String, String> headers, String body) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withHeaders(headers)
                .withBody(body);
    }
}
