# Рекомендации по улучшению архитектуры проекта

## 1. Улучшения структуры проекта

### 1.1. Переход к многоуровневой архитектуре

Текущая структура проекта не имеет четкого разделения на слои. Рекомендуется перейти к многоуровневой архитектуре:

```
src/main/kotlin/com/company/commitet_jm/
├── application/          # Слой приложения (use cases)
├── domain/               # Доменный слой (entities, repositories, services)
├── infrastructure/       # Инфраструктурный слой (конфигурации, внешние сервисы)
├── presentation/         # Слой представления (view, контроллеры)
└── shared/               # Общие компоненты
```

### 1.2. Улучшение структуры пакетов

#### Текущая структура:
- service/ - содержит все сервисы
- component/ - содержит вспомогательные компоненты
- entity/ - содержит все сущности

#### Рекомендуемая структура:
```
domain/
├── project/              # Все, что связано с проектами
│   ├── entity/
│   ├── repository/
│   └── service/
├── commit/               # Все, что связано с коммитами
│   ├── entity/
│   ├── repository/
│   └── service/
├── user/                 # Все, что связано с пользователями
│   ├── entity/
│   ├── repository/
│   └── service/
├── chat/                 # Все, что связано с чатом
│   ├── entity/
│   ├── repository/
│   └── service/
└── ones/                 # Все, что связано с OneC
    ├── entity/
    ├── repository/
    └── service/

infrastructure/
├── git/                  # Инфраструктурные компоненты для работы с Git
├── shell/                # Компоненты для работы с shell командами
├── quartz/               # Конфигурация Quartz
└── persistence/          # Конфигурация базы данных

presentation/
├── view/                 # Vaadin views
└── rest/                 # REST API (если планируется)

shared/
├── exception/            # Общие исключения
├── util/                 # Утилитарные классы
└── config/               # Общая конфигурация
```

## 2. Улучшения читаемости и поддерживаемости кода

### 2.1. Разделение ответственностей

#### Проблема:
Класс `GitWorker` имеет слишком много ответственностей:
- Работа с Git репозиториями
- Работа с файлами
- Работа с OneC
- Обработка ошибок
- Управление ветками

#### Решение:
Разделить на несколько специализированных сервисов:

```kotlin
// Сервис для работы с Git
interface GitService {
    fun cloneRepo(url: String, directoryPath: String, branch: String): Result<Unit>
    fun createBranch(repoPath: String, branchName: String): Result<Unit>
    fun checkoutBranch(repoPath: String, branchName: String): Result<Unit>
    fun commitChanges(repoPath: String, message: String, author: GitAuthor): Result<String>
    fun pushChanges(repoPath: String, branchName: String): Result<Unit>
}

// Сервис для работы с файлами
interface FileService {
    fun saveFile(baseDir: String, fileName: String, content: ByteArray, type: FileType): Result<Unit>
    fun createTempFile(content: String): Result<File>
    fun deleteTempFile(file: File): Result<Unit>
}

// Сервис для работы с OneC
interface OneCService {
    fun uploadExternalFiles(inputFile: File, outDir: String, platformPath: String, version: String): Result<Unit>
    fun unpackExternalFiles(inputFile: File, outDir: String): Result<Unit>
}
```

### 2.2. Улучшение обработки ошибок

#### Проблема:
Текущая обработка ошибок использует исключения без четкой структуры.

#### Решение:
Ввести типизированную обработку ошибок с использованием sealed классов:

```kotlin
sealed class GitError {
    object RepositoryNotFound : GitError()
    object InvalidUrl : GitError()
    object BranchExists : GitError()
    data class CommandFailed(val message: String) : GitError()
}

sealed class FileError {
    object DirectoryNotEmpty : FileError()
    data class IoError(val message: String) : FileError()
}

sealed class OneCError {
    data class PlatformNotFound(val path: String) : OneCError()
    data class UnpackFailed(val message: String) : OneCError()
}

typealias GitResult<T> = Result<T, GitError>
typealias FileResult<T> = Result<T, FileError>
typealias OneCResult<T> = Result<T, OneCError>
```

### 2.3. Улучшение конфигурации

#### Проблема:
Жестко заданные пути в коде (`DataProcessorsExt\\erf`, `DataProcessorsExt\\epf`).

#### Решение:
Вынести конфигурацию в application.properties/application.yml:

```yaml
app:
  paths:
    data-processors:
      erf: DataProcessorsExt/erf
      epf: DataProcessorsExt/epf
    code-ext: CodeExt
    exchange-rules: EXCHANGE_RULES
```

И создать конфигурационный класс:

```kotlin
@ConfigurationProperties(prefix = "app.paths")
data class AppPathsConfig(
    val dataProcessors: DataProcessorsPaths,
    val codeExt: String,
    val exchangeRules: String
) {
    data class DataProcessorsPaths(
        val erf: String,
        val epf: String
    )
}
```

### 2.4. Улучшение логирования

#### Проблема:
В `ShellExecutor` используется логгер от `GitWorker`.

#### Решение:
Каждый класс должен использовать свой собственный логгер:

```kotlin
@Component
class ShellExecutor(var workingDir: File = File("."), var timeout: Long = 1) {
    companion object {
        private val log = LoggerFactory.getLogger(ShellExecutor::class.java)
    }
    // ...
}
```

## 3. Улучшения архитектуры

### 3.1. Внедрение зависимостей

#### Проблема:
В `Committer` создается новый экземпляр `GitWorker` напрямую.

#### Решение:
Использовать внедрение зависимостей Spring:

```kotlin
@Component
class Committer(
    private val gitWorker: GitWorker
): Job {
    // ...
}
```

### 3.2. Улучшение работы с файлами

#### Проблема:
Работа с путями файлов через жестко заданные строки.

#### Решение:
Использовать Path API для работы с путями:

```kotlin
private fun correctPath(baseDir: Path, type: TypesFiles): Path {
    return when (type) {
        REPORT -> baseDir.resolve("DataProcessorsExt").resolve("Отчет")
        DATAPROCESSOR -> baseDir.resolve("DataProcessorsExt").resolve("Обработка")
        SCHEDULEDJOBS -> baseDir.resolve("CodeExt")
        EXTERNAL_CODE -> baseDir.resolve("CodeExt")
        EXCHANGE_RULES -> baseDir.resolve("EXCHANGE_RULES")
    }
}
```

### 3.3. Улучшение работы с временными файлами

#### Проблема:
Создание и удаление временных файлов вручную.

#### Решение:
Использовать try-with-resources аналог для автоматического управления временными файлами:

```kotlin
fun <T> withTempFile(content: String, action: (File) -> T): T {
    val tempFile = File.createTempFile("temp", ".txt")
    return try {
        tempFile.writeText(content, Charsets.UTF_8)
        action(tempFile)
    } finally {
        tempFile.delete()
    }
}
```

## 4. Тестирование

### 4.1. Введение unit-тестов

Для каждого сервиса необходимо создать unit-тесты, используя mocking фреймворк (MockK):

```kotlin
class GitWorkerTest {
    private val dataManager = mockk<DataManager>()
    private val fileStorageLocator = mockk<FileStorageLocator>()
    private val shellExecutor = mockk<ShellExecutor>()
    private val oneRunner = mockk<OneRunner>()
    
    private lateinit var gitWorker: GitWorker
    
    @BeforeEach
    fun setUp() {
        gitWorker = GitWorker(dataManager, fileStorageLocator, shellExecutor, oneRunner)
    }
    
    @Test
    fun `should clone repository successfully`() {
        // Тестовая реализация
    }
}
```

### 4.2. Введение интеграционных тестов

Для проверки взаимодействия компонентов необходимо создать интеграционные тесты:

```kotlin
@SpringBootTest
@Testcontainers
class GitWorkerIntegrationTest {
    // Интеграционная тестовая реализация
}
```

## 5. Документация

### 5.1. Введение документации по API

Создать документацию по основным сервисам и их методам.

### 5.2. Введение документации по архитектуре

Расширить файл ARCHITECTURE.md с описанием новой архитектуры.

## 6. CI/CD улучшения

### 6.1. Добавление тестирования в GitHub Actions

Расширить существующий workflow для запуска тестов:

```yaml
- name: Run tests
  run: ./gradlew test
  
- name: Publish test results
  uses: EnricoMi/publish-unit-test-result-action@v2
  if: always()
  with:
    files: build/test-results/**/*.xml
```

### 6.2. Добавление статического анализа кода

Включить проверки качества кода:

```yaml
- name: Run detekt
  run: ./gradlew detekt
  
- name: Run ktlint
  run: ./gradlew ktlintCheck