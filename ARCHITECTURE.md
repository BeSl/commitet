# Архитектура проекта Commitet JM

## Текущая архитектура

```mermaid
graph TD
    A[CommitetJmApplication] --> B[QuartzConfig]
    A --> C[SecurityConfig]
    A --> D[Services]
    A --> E[Components]
    
    B --> F[Committer Job]
    F --> G[GitWorker]
    
    D --> G
    D --> H[ChatHistoryService]
    D --> I[OneRunner]
    
    E --> J[ShellExecutor]
    
    G --> J
    G --> K[DataManager]
    G --> L[FileStorageLocator]
    G --> I
    
    H --> K
    
    I --> J
    I --> K
    
    subgraph Entities
        M[Project]
        N[Commit]
        O[FileCommit]
        P[User]
        Q[Platform]
        R[OneCStorage]
        S[ChatSession]
        T[ChatMessage]
    end
    
    G --> M
    G --> N
    G --> O
    G --> P
    G --> Q
    R --> M
    N --> O
    N --> P
    S --> P
    T --> S
```

## Основные компоненты

### 1. Основное приложение
- `CommitetJmApplication` - главный класс Spring Boot приложения

### 2. Конфигурация
- `QuartzConfig` - настройка задач планировщика
- Security конфигурация

### 3. Сервисы
- `GitWorker` - основной сервис для работы с Git и файлами
- `ChatHistoryService` - сервис для работы с чатом
- `OneRunner` - сервис для работы с OneC

### 4. Компоненты
- `ShellExecutor` - компонент для выполнения shell команд

### 5. Scheduled Jobs
- `Committer` - задача для автоматического создания коммитов

### 6. Сущности (Entities)
- `Project` - проект
- `Commit` - коммит
- `FileCommit` - файл коммита
- `User` - пользователь
- `Platform` - платформа OneC
- `OneCStorage` - хранилище OneC
- `ChatSession` - сессия чата
- `ChatMessage` - сообщение чата