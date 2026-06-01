# Интеграция с RabbitMQ

## Обзор

Помимо REST API, коммиты внешних обработок 1С можно создавать асинхронно —
через очередь RabbitMQ. Отдельный поток-потребитель читает сообщения из очереди
и создаёт коммиты, переиспользуя ту же бизнес-логику, что и REST
(`CommitRestService`). После создания коммит обрабатывается штатной Quartz-задачей
`Committer` (выгрузка файлов и push в Git).

```
RabbitMQ queue ──> CommitQueueListener (отдельный пул потоков)
                        └─> CommitRestService.createCommit() ──> Commit (status = NEW)
                                                                      └─> Quartz Committer ──> git push
```

## Включение

Интеграция **выключена по умолчанию**, чтобы приложение стартовало без брокера.
Для активации задайте `commit.rabbitmq.enabled=true` и параметры подключения.

### `application.yml`

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    virtual-host: /

commit:
  rabbitmq:
    enabled: true            # включить потребителя очереди
    queue: commitet.commits  # имя очереди
    concurrency: 1           # стартовое число потоков-потребителей
    max-concurrency: 1       # максимальное число потоков-потребителей
```

| Свойство | По умолчанию | Описание |
|----------|--------------|----------|
| `commit.rabbitmq.enabled` | `false` | Включает чтение очереди |
| `commit.rabbitmq.queue` | `commitet.commits` | Имя durable-очереди |
| `commit.rabbitmq.concurrency` | `1` | Стартовое число конкурентных потребителей |
| `commit.rabbitmq.max-concurrency` | `1` | Максимальное число конкурентных потребителей |

Параметры подключения (`host`, `port`, `username`, `password`, `virtual-host`)
берутся из стандартных свойств Spring Boot `spring.rabbitmq.*`.

## Формат сообщения

Тело сообщения — JSON в том же формате, что и тело запроса `POST /api/commits`
(DTO `CommitCreateRequest`). См. [REST_API.md](REST_API.md).

```json
{
  "externalProjectId": "PRJ-001",
  "externalUserId": "USR-042",
  "taskNum": "TASK-123",
  "description": "Доработка обработки выгрузки",
  "fixCommit": false,
  "files": [
    {
      "name": "МояОбработка.epf",
      "data": "<base64>",
      "type": "DATAPROCESSOR",
      "code": "001"
    }
  ]
}
```

> Заголовок сообщения `content_type` должен быть `application/json`.

## Обработка ошибок

* Очередь объявляется как **durable** — сообщения переживают перезапуск брокера.
* «Ядовитые» сообщения не возвращаются в очередь повторно
  (`defaultRequeueRejected = false`), чтобы избежать зацикливания.
* Бизнес-ошибки (например, проект не найден) логируются, сообщение
  подтверждается и удаляется — повторная обработка не помогла бы.
