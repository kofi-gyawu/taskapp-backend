package com.awsmicroservices.userservice;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.CognitoEvent;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPostConfirmationEvent;
import com.awsmicroservices.userservice.entity.Comment;
import com.awsmicroservices.userservice.entity.Task;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.awsmicroservices.userservice.entity.User;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.function.context.MessageRoutingCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.Message;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InvalidParameterException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InvalidPasswordException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UsernameExistsException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sfn.model.StartExecutionRequest;
import org.json.simple.parser.JSONParser;
import org.json.simple.JSONObject;
import software.amazon.awssdk.services.sfn.model.StartExecutionResponse;


import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@SpringBootApplication
@ComponentScan()
public class UserserviceApplication {

	private static final Logger logger = LoggerFactory.getLogger(UserserviceApplication.class);
	private static final ObjectMapper objectMapper = new ObjectMapper();
	public static void main(String[] args) {
		SpringApplication.run(UserserviceApplication.class, args);
	}

	@Bean
	public MessageRoutingCallback customRouter () {
		return new MessageRoutingCallback() {
			@Override
			public String routingResult(Message<?> message) {
				if(message.getHeaders().containsKey("aws-api-gateway")) {
					return routeAPIGatewayRequest(message.getPayload());
				} else {
					return "subscribeUser";
				}
			}

			public String routeAPIGatewayRequest(Object payload) {
				try{
					System.out.println("################################# Conversion");
					APIGatewayV2HTTPEvent event = objectMapper.readValue((byte[]) payload,APIGatewayV2HTTPEvent.class);
					System.out.println("payload " + event);
					switch (event.getRouteKey()) {
						case "POST /signup":
							return "registerUser";
						case "POST /task":
							System.out.println("Post /task");
							return "createTask";
						case "POST /task/comment":
							return "comment";
						case "GET /task/comment":
							return "getComments";
						case "GET /task":
							return "listTasks";
						case "PATCH /task/complete":
							return "completeTask";
						case "PATCH /task/reopen":
							return "reopenTask";
						case "PATCH /task/reassign":
							return "reassignTask";
						//close task
					}
					return null;
				} catch (Exception e) {
					System.out.println(e);
				}
				return null;
			}

		};
	}

	@Bean
	public CognitoIdentityProviderClient cognitoClient() {
		return CognitoIdentityProviderClient.builder()
				.region(Region.of(System.getenv("AWS_REGION")))
				.build();
	}

	@Bean
	public SfnClient sfnClient() {
		return SfnClient.builder()
				.region(Region.of(System.getenv("AWS_REGION")))
				.build();
	}

	@Bean
	public DynamoDbClient dynamoDbClient () {
		return DynamoDbClient.builder()
				.region(Region.EU_CENTRAL_1)
				.build();
	}

	@Bean
	public Function<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> registerUser (CognitoIdentityProviderClient cognitoClient) {
		return (request) -> {
			APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
			response.setHeaders(Map.of("Content-Type", "application/json"));
            try {
                User user = objectMapper.readValue(request.getBody(),User.class);
				AttributeType emailAttribute = AttributeType.builder()
						.name("email")
						.value(user.email)
						.build();
				AdminCreateUserRequest adminCreateUserRequest = AdminCreateUserRequest.builder()
						.userPoolId(System.getenv("COGNITO_USER_POOL_ID"))
						.userAttributes(emailAttribute)
						.username(user.email)
						.build();
				AdminCreateUserResponse cognitoResponse = cognitoClient.adminCreateUser(adminCreateUserRequest);
				Map<String, Object> responseBody = new HashMap<>();
				responseBody.put("message", "User registration successful. Please check your email for verification code.");
				response.setStatusCode(200);
				response.setBody(objectMapper.writeValueAsString(responseBody));
            } catch (JsonProcessingException e) {
				throw new RuntimeException(e);
			} catch (UsernameExistsException e) {
				response.setStatusCode(400);
				response.setBody(createErrorResponse("Email address already exists"));
			} catch (InvalidPasswordException e) {
				response.setStatusCode(400);
				response.setBody(createErrorResponse("Invalid Password"));
			} catch (InvalidParameterException e) {
				response.setStatusCode(400);
				response.setBody(createErrorResponse("Invalid parameters provided"));
			} catch (Exception e) {
				response.setStatusCode(500);
				response.setBody(createErrorResponse("Internal server error"));
			}
			return response;
		};
	}

	private String createErrorResponse(String message) {
		try {
			Map<String, String> errorResponse = Map.of("error", message);
			return objectMapper.writeValueAsString(errorResponse);
		} catch (Exception e) {
			return "{\"error\":\"Error creating error response\"}";
		}
	}

	@Bean
	public Function<CognitoUserPoolPostConfirmationEvent,StartExecutionResponse> subscribeUser () {
		return (event) ->  {
			logger.info("Processing subscription request");
			String email = event.getRequest().getUserAttributes().get("email");
			Map<String, String> stepFunctionInput = new HashMap<>();
			stepFunctionInput.put("userEmail", email);
			stepFunctionInput.put("topic1", System.getenv("TaskAssignmentNotificationTopic"));
			stepFunctionInput.put("topic2", System.getenv("TaskDeadlineNotificationTopic"));
			stepFunctionInput.put("topic3", System.getenv("ClosedTaskNotificationTopic"));
			stepFunctionInput.put("topic4", System.getenv("ReopenedTasksNotificationTopic"));
            StartExecutionRequest request = null;
            try {
                request = StartExecutionRequest.builder()
                        .stateMachineArn(System.getenv("STATE_MACHINE_ARN"))
                        .input(objectMapper.writeValueAsString(stepFunctionInput))
                        .build();
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            StartExecutionResponse response = sfnClient().startExecution(request);
			logger.info("subscribing this email " + email);
			logger.info("operation status: " + response.sdkHttpResponse().statusCode());
			return response;
		};
	}

	private static String getJSONString(String path) {
		try {
			JSONParser parser = new JSONParser();
			JSONObject data = (JSONObject) parser.parse(new FileReader(path));// path to the JSON file.
			String json = data.toJSONString();
			return json;

		} catch (IOException | org.json.simple.parser.ParseException e) {
			e.printStackTrace();
		}
		return "";
	}

	@Bean
	public Function<APIGatewayProxyRequestEvent, Task> createTask () {
		return (request) -> {
			try {
				Task task = objectMapper.readValue(request.getBody(),Task.class);
				task.setId(String.valueOf(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)));
				task.setDeadline(LocalDateTime.now().plusHours(1));
				Map<String, AttributeValue> item = new HashMap<>();
				item.put("id", AttributeValue.builder().n(task.getId()).build());
				item.put("name", AttributeValue.builder().s(task.name).build());
				item.put("description", AttributeValue.builder().s(task.description).build());
				item.put("status", AttributeValue.builder().s("open").build());
				item.put("deadline", AttributeValue.builder().s(task.getDeadline().toString()).build());
				item.put("responsibility", AttributeValue.builder().s(task.responsibility).build());
				item.put("completed_at", AttributeValue.builder().s("").build());
				PutItemRequest putItemRequest = PutItemRequest.builder()
						.tableName(System.getenv("TASK_TABLE"))
						.item(item)
						.build();
				dynamoDbClient().putItem(putItemRequest);
				return task;
			} catch (JsonProcessingException e) {
				throw new RuntimeException(e);
			}
		};
	}

	@Bean
	public Function<APIGatewayProxyRequestEvent,Task> completeTask() {
		return (request) -> {
            try {
                Task task = objectMapper.readValue(request.getBody(),Task.class);
				task.setStatus("complete");
				task.setCompleted_at(LocalDateTime.now());
				task = updateTask(task);
				//sqs queue
				return task;
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
		};
	}

	@Bean
	public Function<APIGatewayProxyRequestEvent,Task> reassignTask() {
		return (request) -> {
			try {
				Task task = objectMapper.readValue(request.getBody(),Task.class);
				task = updateTask(task);
				//sqs queue
				return task;
			} catch (JsonProcessingException e) {
				throw new RuntimeException(e);
			}
		};
	}

	@Bean
	public Function<APIGatewayProxyRequestEvent,Task> reopenTask() {
		return (request) -> {
			try {
				System.out.println("#############"+request);
				Task task = objectMapper.readValue(request.getBody(),Task.class);
				System.out.println("#############"+task.id);
				task.setStatus("open");
				task = updateTask(task);
				//sqs queue
				return task;
			} catch (JsonProcessingException e) {
				throw new RuntimeException(e);
			}
		};
	}

	public Task updateTask (Task task) {
		Map<String, AttributeValue> key = new HashMap<>();
		Map<String, AttributeValueUpdate> item = new HashMap<>();
		key.put("id", AttributeValue.builder().n(task.getId()).build());
		if(task.getStatus() != null) {
			item.put("status",AttributeValueUpdate.builder()
					.value(AttributeValue.builder()
							.s(task.status)
							.build())
					.build()
			);
		}
		if(task.getDeadline() != null) {
			item.put("deadline",AttributeValueUpdate.builder()
					.value(AttributeValue.builder()
							.s(task.deadline.toString())
							.build())
					.build()
			);
		}
		if(task.getResponsibility() != null){
			item.put("responsibility",AttributeValueUpdate.builder()
					.value(AttributeValue.builder()
							.s(task.responsibility)
							.build())
					.build()
			);
		}
		if(task.getCompleted_at() != null) {
			item.put("completed_at",AttributeValueUpdate.builder()
					.value(AttributeValue.builder()
							.s(task.getCompleted_at().toString())
							.build())
					.build()
			);
		}
		UpdateItemRequest updateItemRequest = UpdateItemRequest.builder()
				.tableName(System.getenv("TASK_TABLE"))
				.key(key)
				.attributeUpdates(item)
				.build();
		dynamoDbClient().updateItem(updateItemRequest);
		return task;
	}

	@Bean
	public Function<APIGatewayProxyRequestEvent,Object> listTasks () {
		return (request) -> {
            try {
                String email = objectMapper.writeValueAsString(request.getBody());
				ScanRequest scanRequest = ScanRequest.builder()
						.tableName(System.getenv("TASK_TABLE"))
						.filterExpression("email = :emailValue")
						.expressionAttributeValues(Map.of(
								":emailValue", AttributeValue.builder().s(email).build()
						))
						.build();
				ScanResponse response = dynamoDbClient().scan(scanRequest);
				return response.items();
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

		};
	}

	@Bean
	public Function<APIGatewayProxyRequestEvent,Comment> comment() {
		return (request) -> {
            try {
                Comment comment = objectMapper.readValue(request.getBody(),Comment.class);
				Map<String, AttributeValue> item = new HashMap<>();
				item.put("id", AttributeValue.builder().s(comment.getSender()).build());
				item.put("sender", AttributeValue.builder().s(comment.getSender()).build());
				item.put("body", AttributeValue.builder().s(comment.getBody()).build());
				item.put("timeStamp", AttributeValue.builder().s(comment.getTimeStamp().toString()).build());
				PutItemRequest putItemRequest = PutItemRequest.builder()
						.tableName(System.getenv("COMMENT_TABLE"))
						.item(item)
						.build();
				dynamoDbClient().putItem(putItemRequest);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            return null;
		};
	}

	@Bean
	public Function<Task,Task> getComments () {
		return (request) -> {
			return new Task();
		};
	}

	@Bean
	public Function<Task,Task> notifyUsers () {
		return (request) -> {
			return new Task();
		};
		//based on the task change
	}

}
