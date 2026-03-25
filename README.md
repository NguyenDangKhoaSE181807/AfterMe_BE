# Reminder Backend (Monolithic + Clean Architecture)

## Tech Stack
- Spring Boot 3
- PostgreSQL
- WebSocket (STOMP)
- Firebase Cloud Messaging

## Architecture (Monolithic + Clean)
- `domain`: Pure business model and enums (no framework annotations)
- `application`: Use cases, commands, and ports (framework-agnostic)
- `infrastructure`: Adapters, JPA entities/repositories, Spring configuration, WebSocket, Firebase
- `presentation`: REST/WebSocket controllers, request/response DTOs, HTTP exception mapping

## Current Project Structure

```text
src/main/java/com/example/reminder
|-- ReminderApplication.java
|-- application
|   |-- dto
|   |-- exception
|   |-- port/out
|   `-- usecase
|-- domain
|   |-- enums
|   `-- model
|-- infrastructure
|   |-- config
|   |-- notification
|   `-- persistence
|       |-- adapter
|       |-- entity
|       `-- repository
`-- presentation
  |-- controller
  |-- dto
  `-- exception
```

## Notification Flow
1. Presentation receives request via REST or WebSocket.
2. `SendNotificationUseCase` creates domain message.
3. `NotificationGateway` dispatches to adapters.
4. `WebSocketNotificationAdapter` publishes realtime message.
5. `FirebaseNotificationAdapter` sends FCM push (if enabled).

## Configuration
`src/main/resources/application.yml`

```yml
app:
  notification:
    websocket:
      endpoint: /ws
      app-prefix: /app
      topic-prefix: /topic
    firebase:
      enabled: false
      service-account-path: ""
```

## WebSocket Usage
- Connect endpoint: `/ws`
- Subscribe topic by user: `/topic/notifications/{userId}`
- Send message mapping: `/app/notifications.send`

Payload:

```json
{
  "userId": 1,
  "title": "Take medicine",
  "body": "It's time to take your 8AM medicine"
}
```

## REST Notification API
- `POST /api/notifications/send`

Payload:

```json
{
  "userId": 1,
  "title": "Stand up",
  "body": "Time to walk 5 minutes"
}
```

## Run
```bash
mvn spring-boot:run
```
