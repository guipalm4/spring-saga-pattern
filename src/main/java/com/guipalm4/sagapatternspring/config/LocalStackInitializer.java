package com.guipalm4.sagapatternspring.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;

import java.util.Arrays;
import java.util.List;

@Component
@Profile("local")
@Slf4j
@RequiredArgsConstructor
public class LocalStackInitializer implements ApplicationRunner {

    private final SqsAsyncClient sqsClient;
    private final S3Client s3Client;
    private final DynamoDbClient dynamoDbClient;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        initializeSqsQueues();
        initializeS3Buckets();
        initializeDynamoDbTables();
    }

    private void initializeSqsQueues() {
        List<String> queues = Arrays.asList(
                "payment-queue",
                "payment-response-queue",
                "inventory-queue",
                "inventory-response-queue",
                "shipping-queue",
                "shipping-response-queue",
                "payment-compensation-queue",
                "inventory-compensation-queue",
                "shipping-compensation-queue"
        );

        queues.forEach(queueName -> {
            try {
                sqsClient.createQueue(CreateQueueRequest.builder()
                        .queueName(queueName)
                        .build()).get();
                log.info("Fila SQS criada: {}", queueName);
            } catch (Exception e) {
                log.warn("Fila já existe ou erro ao criar: {}", queueName);
            }
        });
    }

    private void initializeS3Buckets() {
        try {
            s3Client.createBucket(CreateBucketRequest.builder()
                    .bucket("saga-audit-logs")
                    .build());
            log.info("Bucket S3 criado: saga-audit-logs");
        } catch (Exception e) {
            log.warn("Bucket já existe ou erro ao criar: saga-audit-logs");
        }
    }

    private void initializeDynamoDbTables() {
        try {
            dynamoDbClient.createTable(CreateTableRequest.builder()
                    .tableName("SagaAuditLog")
                    .keySchema(KeySchemaElement.builder()
                            .attributeName("sagaId")
                            .keyType(KeyType.HASH)
                            .build())
                    .attributeDefinitions(AttributeDefinition.builder()
                            .attributeName("sagaId")
                            .attributeType(ScalarAttributeType.S)
                            .build())
                    .billingMode(BillingMode.PAY_PER_REQUEST)
                    .build());
            log.info("Tabela DynamoDB criada: SagaAuditLog");
        } catch (Exception e) {
            log.warn("Tabela já existe ou erro ao criar: SagaAuditLog");
        }
    }
}
