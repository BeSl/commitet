# Commitet REST API Documentation

## Обзор

REST API для создания и управления коммитами в системе Commitet.

**Base URL:** `http://localhost:8090`

---

## Аутентификация

API использует базовую аутентификацию Spring Security. Для доступа к API необходимо передавать учетные данные пользователя.

---

## Эндпоинты

### 1. Создание коммита

Создает новый коммит в системе.

**URL:** `POST /api/commits`

**Content-Type:** `application/json`

#### Логика определения автора:

1. Если `userId` указан и пользователь найден - он становится автором
2. Если `userId` указан, но пользователь НЕ найден - назначается пользователь по умолчанию
3. Если `userId` не указан - назначается пользователь по умолчанию

Пользователь по умолчанию настраивается через параметр `commit.default.user` в `application.properties` (по умолчанию: `admin`).

#### Request Body:

```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "projectId": "123e4567-e89b-12d3-a456-426614174000",
  "taskNum": "TASK-123",
  "description": "Добавлена новая обработка для выгрузки данных",
  "fixCommit": false,
  "files": [
    {
      "name": "ВыгрузкаДанных.epf",
      "data": "UEsDBBQAAAAIAGlxXVkAAAAA...",
      "type": "DATAPROCESSOR"
    }
  ]
}
```

#### Параметры запроса:

| Поле | Тип | Обязательный | Описание |
|------|-----|--------------|----------|
| `userId` | UUID | Нет | UUID пользователя (автора). Если не указан или не найден - используется пользователь по умолчанию |
| `projectId` | UUID | **Да** | UUID проекта |
| `taskNum` | string | **Да** | Номер задачи (используется для создания ветки) |
| `description` | string | **Да** | Описание коммита |
| `fixCommit` | boolean | Нет | Флаг исправления (по умолчанию: `false`) |
| `files` | array | Нет | Массив файлов для коммита |

#### Параметры файла:

| Поле | Тип | Обязательный | Описание |
|------|-----|--------------|----------|
| `name` | string | **Да** | Имя файла |
| `data` | string | **Да** | Содержимое файла в формате **Base64** |
| `type` | string | **Да** | Тип файла (см. ниже) |

#### Типы файлов:

| Тип | Описание | Путь в репозитории |
|-----|----------|-------------------|
| `REPORT` | Отчеты | `DataProcessorsExt/Отчет/` |
| `DATAPROCESSOR` | Обработки | `DataProcessorsExt/Обработка/` |
| `SCHEDULEDJOBS` | Регламентные задания | `CodeExt/` |
| `EXTERNAL_CODE` | Внешний код | `CodeExt/` |
| `EXCHANGE_RULES` | Правила обмена | `EXCHANGE_RULES/` |

#### Успешный ответ (201 Created):

```json
{
  "success": true,
  "commitId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "message": "Коммит создан успешно",
  "authorId": "550e8400-e29b-41d4-a716-446655440000",
  "authorUsername": "developer"
}
```

#### Ответ с fallback на пользователя по умолчанию:

```json
{
  "success": true,
  "commitId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "message": "Коммит создан успешно. Указанный пользователь не найден, назначен пользователь по умолчанию.",
  "authorId": "11111111-1111-1111-1111-111111111111",
  "authorUsername": "admin"
}
```

#### Ошибка валидации (400 Bad Request):

```json
{
  "success": false,
  "error": "VALIDATION_ERROR",
  "message": "Ошибка валидации данных",
  "details": {
    "errors": [
      "taskNum не может быть пустым",
      "description не может быть пустым"
    ]
  }
}
```

#### Проект не найден (404 Not Found):

```json
{
  "success": false,
  "error": "CREATE_FAILED",
  "message": "Проект с ID 123e4567-e89b-12d3-a456-426614174000 не найден"
}
```

#### Внутренняя ошибка (500 Internal Server Error):

```json
{
  "success": false,
  "error": "INTERNAL_ERROR",
  "message": "Внутренняя ошибка сервера",
  "details": {
    "exception": "Error message"
  }
}
```

---

### 2. Получение статуса коммита

Возвращает текущий статус обработки коммита.

**URL:** `GET /api/commits/{commitId}/status`

#### Параметры пути:

| Параметр | Тип | Описание |
|----------|-----|----------|
| `commitId` | UUID | Идентификатор коммита |

#### Успешный ответ (200 OK):

```json
{
  "commitId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "status": "COMPLETE",
  "hashCommit": "a1b2c3d4e5f6...",
  "urlBranch": "https://github.com/user/repo/tree/feature/TASK-123",
  "errorInfo": null,
  "dateCreated": "2024-01-15T10:30:00"
}
```

#### Статусы коммита:

| Статус | Описание |
|--------|----------|
| `NEW` | Коммит создан, ожидает обработки |
| `PROCESSED` | Коммит обрабатывается |
| `COMPLETE` | Коммит успешно создан в Git |
| `ERROR` | Ошибка при создании коммита |

#### Коммит не найден (404 Not Found):

```json
{
  "error": "NOT_FOUND",
  "message": "Коммит не найден"
}
```

---

## Legacy Endpoint

Для обратной совместимости доступен устаревший эндпоинт:

**URL:** `POST /newcmt`

Рекомендуется использовать новый API: `POST /api/commits`

---

## Примеры использования

### cURL

#### Создание коммита:

```bash
curl -X POST http://localhost:8090/api/commits \
  -H "Content-Type: application/json" \
  -u admin:admin \
  -d '{
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "projectId": "123e4567-e89b-12d3-a456-426614174000",
    "taskNum": "TASK-123",
    "description": "Добавлена новая обработка",
    "files": [
      {
        "name": "test.epf",
        "data": "SGVsbG8gV29ybGQ=",
        "type": "DATAPROCESSOR"
      }
    ]
  }'
```

#### Получение статуса коммита:

```bash
curl -X GET http://localhost:8090/api/commits/7c9e6679-7425-40de-944b-e07fc1f90ae7/status \
  -u admin:admin
```

### JavaScript (fetch)

```javascript
const createCommit = async () => {
  // Кодирование файла в Base64
  const fileContent = await file.arrayBuffer();
  const base64Data = btoa(String.fromCharCode(...new Uint8Array(fileContent)));

  const response = await fetch('http://localhost:8090/api/commits', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': 'Basic ' + btoa('admin:admin')
    },
    body: JSON.stringify({
      userId: '550e8400-e29b-41d4-a716-446655440000',
      projectId: '123e4567-e89b-12d3-a456-426614174000',
      taskNum: 'TASK-123',
      description: 'Описание коммита',
      files: [{
        name: 'file.epf',
        data: base64Data,
        type: 'DATAPROCESSOR'
      }]
    })
  });

  return await response.json();
};
```

### Python (requests)

```python
import requests
import base64

# Кодирование файла в Base64
with open('file.epf', 'rb') as f:
    file_data = base64.b64encode(f.read()).decode('utf-8')

response = requests.post(
    'http://localhost:8090/api/commits',
    auth=('admin', 'admin'),
    json={
        'userId': '550e8400-e29b-41d4-a716-446655440000',
        'projectId': '123e4567-e89b-12d3-a456-426614174000',
        'taskNum': 'TASK-123',
        'description': 'Описание коммита',
        'files': [{
            'name': 'file.epf',
            'data': file_data,
            'type': 'DATAPROCESSOR'
        }]
    }
)

print(response.json())
```

---

## Конфигурация

### application.properties

```properties
# Username пользователя по умолчанию
commit.default.user=admin

# OpenAPI/Swagger
springdoc.api-docs.path=/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.enabled=true
```

---

## Процесс обработки коммита

1. **Создание** - Коммит создается со статусом `NEW`
2. **Обработка** - Quartz job (каждые 5 сек) находит новые коммиты и обрабатывает их
3. **Git операции**:
   - Клонирование/обновление репозитория
   - Создание ветки `feature/{taskNum}`
   - Сохранение файлов
   - Git add, commit, push
4. **Завершение** - Статус меняется на `COMPLETE` или `ERROR`
