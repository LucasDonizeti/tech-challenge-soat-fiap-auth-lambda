package com.techchallenge.lambda.authorizer;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techchallenge.lambda.authorizer.application.ports.AuthLambdaHandler;
import com.techchallenge.lambda.authorizer.web.dto.AuthRequestDto;

public class Application {
	//TESTE RÁPIDO

	/*
	public static void main(String[] args) {
		try {
			AuthLambdaHandler authLambdaHandler = new AuthLambdaHandler();
			
			// Criar requisição de teste similar ao que viria do API Gateway
			// NOTA: Para testar sem banco de dados, use username="admin"
			AuthRequestDto authRequest = new AuthRequestDto();
			authRequest.setUsername("admin");
			authRequest.setPassword("admin123");
			
			ObjectMapper objectMapper = new ObjectMapper();
			String requestBody = objectMapper.writeValueAsString(authRequest);
			
			// Criar evento do API Gateway
			APIGatewayProxyRequestEvent requestEvent = new APIGatewayProxyRequestEvent()
					.withBody(requestBody)
					.withHttpMethod("POST")
					.withPath("/v1/auth/login");
			
			// Criar context mock simples
			Context context = new TestContext();
			
			// Chamar handleRequest diretamente (simula execução Lambda real)
			APIGatewayProxyResponseEvent response = authLambdaHandler.handleRequest(requestEvent, context);
			
			// Processar resposta
			System.out.println("Status Code: " + response.getStatusCode());
			System.out.println("Response Body: " + response.getBody());
			System.out.println("Response Headers: " + response.getHeaders());
			
			if (response.getStatusCode() == 200) {
				System.out.println("✅ Teste bem-sucedido!");
			} else {
				System.out.println("❌ Teste falhou com status: " + response.getStatusCode());
			}
			
		} catch (Exception e) {
			System.err.println("❌ Erro durante o teste: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	// Context mock simples para testes locais
	private static class TestContext implements Context {
		@Override
		public String getAwsRequestId() {
			return "test-request-id";
		}
		
		@Override
		public String getLogGroupName() {
			return "test-log-group";
		}
		
		@Override
		public String getLogStreamName() {
			return "test-log-stream";
		}
		
		@Override
		public String getFunctionName() {
			return "test-function";
		}
		
		@Override
		public String getFunctionVersion() {
			return "$LATEST";
		}
		
		@Override
		public String getInvokedFunctionArn() {
			return "arn:aws:lambda:us-east-1:123456789012:function:test-function";
		}
		
		@Override
		public com.amazonaws.services.lambda.runtime.CognitoIdentity getIdentity() {
			return null;
		}
		
		@Override
		public com.amazonaws.services.lambda.runtime.ClientContext getClientContext() {
			return null;
		}
		
		@Override
		public int getRemainingTimeInMillis() {
			return 30000;
		}
		
		@Override
		public int getMemoryLimitInMB() {
			return 512;
		}
		
		@Override
		public LambdaLogger getLogger() {
			return new LambdaLogger() {
				@Override
				public void log(String message) {
					System.out.println("[LAMBDA LOG] " + message);
				}
				
				@Override
				public void log(byte[] message) {
					System.out.println("[LAMBDA LOG] " + new String(message));
				}
			};
		}
	}

	 */
}
