# Reminder Backend (Monolithic + 3-Layer Architecture)

## Tech Stack
- Spring Boot 3
- PostgreSQL
- WebSocket (STOMP)
- Firebase Cloud Messaging

## Architecture (3-Layer Standard)
- `controller`: REST/WebSocket entrypoints
- `service`: service interfaces and business logic implementations (`service.impl`)
- `repository`: Spring Data repositories
- `entity`: JPA entities
- `dto`: request/response and command DTOs
- `exception`: custom business exceptions and global API error handling

## Current Project Structure

```text
src/main/java/com/example/reminder
|-- ReminderApplication.java
|-- controller/
|   `-- UserController.java
|-- service/
|   |-- UserService.java
|   `-- impl/
|       `-- UserServiceImpl.java
|-- repository/
|   `-- UserRepository.java
|-- entity/
|   `-- User.java
|-- dto/
|   `-- UserDTO.java
|-- exception/
|   `-- GlobalExceptionHandler.java
|-- config/
|   `-- AppConfig.java
`-- domain/
  |-- enums/
  `-- model/
```

## Notification Flow
1. Presentation receives request via REST or WebSocket.
2. `NotificationService` creates domain message.
3. `NotificationSender` dispatches to concrete notification adapters.
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

## FCM Setup Checklist
- Create a Firebase project and enable Cloud Messaging.
- Generate a service account JSON key and point `app.notification.firebase.service-account-path` to that file.
- Set `app.notification.firebase.enabled=true` in the target environment.
- Store each logged-in device in `user_devices` with `user_id`, `device_id`, `fcm_token`, `platform`, `is_trusted`, `notification_enabled`, and `last_seen_at`.
- Register or refresh the FCM token from the client whenever the app starts or the token changes.
- Request notification permission on the client before relying on push delivery.
- Keep WebSocket enabled if you still want realtime in-app notification delivery alongside FCM.

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
