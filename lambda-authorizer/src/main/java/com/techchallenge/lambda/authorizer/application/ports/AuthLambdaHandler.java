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

import java.util.Map;

public class AuthLambdaHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final Logger logger = LoggerFactory.getLogger(AuthLambdaHandler.class);

    private final ObjectMapper objectMapper;
    private final AutenticarUsuarioInput autenticarUsuarioInput;

    public AuthLambdaHandler() {
        this.objectMapper = InjectionFactory.getObjectMapper();
        this.autenticarUsuarioInput = InjectionFactory.getAutenticarUsuarioInput();
    }

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

            AuthResponseDto responseDto = callInput(authRequest);
            return buildResponse(200, headers, objectMapper.writeValueAsString(responseDto));

        } catch (Exception e){
            context.getLogger().log("Erro durante o processamento: " + e.getMessage());
            return buildResponse(400, headers, "{\"error\": \"Erro ao processar autenticação: " + e.getMessage() + "\"}");
        }
    }

    public AuthResponseDto callInput(AuthRequestDto authRequest){
        AuthResponse authResponse = autenticarUsuarioInput.execute(authRequest.toCommand());
        logger.info("Login bem-sucedido para o usuário: {}", authResponse.getUsername());
        return AuthResponseDto.from(authResponse);
    }


    private APIGatewayProxyResponseEvent buildResponse(int statusCode, Map<String, String> headers, String body) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withHeaders(headers)
                .withBody(body);
    }

}
