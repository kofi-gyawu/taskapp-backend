//package com.awsmicroservices.userservice.services;
//
//import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
//import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
//import com.awsmicroservices.userservice.entity.Task;
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.springframework.boot.SpringApplication;
//import org.springframework.boot.autoconfigure.SpringBootApplication;
//import org.springframework.cloud.function.context.MessageRoutingCallback;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Profile;
//import org.springframework.messaging.Message;
//import org.springframework.stereotype.Component;
//import software.amazon.awssdk.regions.Region;
//import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
//import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
//import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
//
//import java.time.LocalDateTime;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.function.Function;
//
//@Component
//public class TaskService {
//
//	@Bean
//	@Profile("TaskService")
//	public MessageRoutingCallback customRouter () {
//		return new MessageRoutingCallback() {
//			@Override
//			public String routingResult(Message<?> message) {
//				if(message.getHeaders().containsKey("aws-api-gateway")) {
//					return routeAPIGatewayRequest(message.getPayload());
//				} else {
//					return "subscribeUser";
//				}
//			}
//
//			public String routeAPIGatewayRequest(Object payload) {
//				try{
//					APIGatewayV2HTTPEvent event = objectMapper.readValue((byte[]) payload,APIGatewayV2HTTPEvent.class);
//					System.out.println("payload " + event);
//					switch (event.getRouteKey()) {
//						case "POST /signup":
//							return "registerUser";
//						case "POST /task":
//							System.out.println("Post /task");
//							return "createTask";
//						//complete task
//						//reopen task
//						//close task
//						//comment task
//
//					}
//					System.out.println("Payload"+event.getBody());
//					return null;
//				} catch (Exception e) {
//					System.out.println(e);
//				}
//				return null;
//			}
//
//		};
//	}
//
//	private static final ObjectMapper objectMapper = new ObjectMapper();
//
//	@Bean
//	public DynamoDbClient dynamoDbClient () {
//		return DynamoDbClient.builder()
//				.region(Region.EU_CENTRAL_1)
//				.build();
//	}
//
//	//only admin
//	@Bean
//	public Function<APIGatewayProxyRequestEvent, Task> createTask () {
//		return (request) -> {
//            try {
//                Task task = objectMapper.readValue(request.getBody(),Task.class);
//				Map<String, AttributeValue> item = new HashMap<>();
//				item.put("name", AttributeValue.builder().s(task.name).build());
//				item.put("description", AttributeValue.builder().s(task.description).build());
//				item.put("status", AttributeValue.builder().s("open").build());
//				item.put("deadline", AttributeValue.builder().s(LocalDateTime.now().plusHours(1).toString()).build());
//				item.put("responsibility", AttributeValue.builder().s(task.responsibility).build());
//				item.put("completed_at", AttributeValue.builder().s(null).build());
//				PutItemRequest putItemRequest = PutItemRequest.builder()
//						.tableName(System.getenv("TASK_TABLE"))
//						.item(item)
//						.build();
//				System.out.println("created Task");
//				dynamoDbClient().putItem(putItemRequest);
//				return new Task();
//            } catch (JsonProcessingException e) {
//                throw new RuntimeException(e);
//            }
//		};
//	}
//
//	@Bean
//	public Function<APIGatewayProxyRequestEvent,Task> completeTask() {
//		return (request) -> {
//			return new Task();
//		};
//	}
//
//	public Task updateTask () {
//		return new Task();
//	}
//
//	@Bean
//	public Function<Task,Task> getTask () {
//		return (request) -> {
//			return new Task();
//		};
//	}
//
//	@Bean
//	public Function<Task,Task> listTasks () {
//		return (request) -> {
//			return new Task();
//		};
//	}
//
//	@Bean
//	public Function<Task,Task> comment() {
//		return (request) -> {
//			return new Task();
//		};
//	}
//
//	@Bean
//	public Function<Task,Task> notifyUsers () {
//		return (request) -> {
//			return new Task();
//		};
//		//based on the task change
//	}
//
//}
