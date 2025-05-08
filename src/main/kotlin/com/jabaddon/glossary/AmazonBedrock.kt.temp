package com.jabaddon.glossary

import com.fasterxml.jackson.databind.ObjectMapper
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse

class AmazonBedrock {
}

fun main() {
    // Get credentials from environment
    val accessKey = System.getenv("ACCESS_KEY")
    val secretKey = System.getenv("SECRET_ACCESS_KEY")

    if (accessKey.isNullOrEmpty() || secretKey.isNullOrEmpty()) {
        println("ERROR: AWS credentials not found in environment variables")
        return
    }

    // Create AWS credentials
    val awsCredentials = AwsBasicCredentials.create(accessKey, secretKey)
    val credentialsProvider = StaticCredentialsProvider.create(awsCredentials)

    // Create BedrockRuntime client
    val bedrockClient = BedrockRuntimeClient.builder()
        .credentialsProvider(credentialsProvider)
        .region(Region.US_EAST_1)
        .build()

    // Sample text to embed
    val textToEmbed = "This is a sample text for embedding with Amazon Bedrock Titan model"

    // Prepare request payload for Titan Embedding model
    val requestPayload = """
        {
            "inputText": "$textToEmbed"
        }
    """.trimIndent()

    // Create the request
    val request = InvokeModelRequest.builder()
        .modelId("amazon.titan-embed-text-v2:0")
//        .modelId("amazon.titan-embed-text-v1")
        .contentType("application/json")
        .accept("application/json")
        .body(SdkBytes.fromUtf8String(requestPayload))
        .build()

    try {
        // Call the model
        val response: InvokeModelResponse = bedrockClient.invokeModel(request)

        // Parse the response
        val responseBody = response.body().asUtf8String()
        val mapper = ObjectMapper()
        val jsonNode = mapper.readTree(responseBody)

        // Extract embedding vector
        val embedding = jsonNode.get("embedding")

        // Display results
        println("Text: $textToEmbed")
        println("Vector dimensions: ${embedding.size()}")
        println("First 5 values: ${(0 until 5).map { embedding.get(it).asDouble() }}")
    } catch (e: Exception) {
        println("Error calling Amazon Bedrock: ${e.message}")
        e.printStackTrace()
    } finally {
        bedrockClient.close()
    }
}