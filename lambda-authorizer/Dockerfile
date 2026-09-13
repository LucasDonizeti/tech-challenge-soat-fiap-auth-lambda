FROM public.ecr.aws/lambda/java:21

# Copia o JAR pré-compilado pelo GitHub Actions
COPY target/*.jar ${LAMBDA_TASK_ROOT}/lib/

CMD [ "com.techchallenge.lambda.authorizer.application.ports.AuthLambdaHandler::handleRequest" ]