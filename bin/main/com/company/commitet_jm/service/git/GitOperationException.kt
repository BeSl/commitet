package com.company.commitet_jm.service.git

/**
 * Типы ошибок Git операций
 */
enum class GitErrorType(val userMessage: String) {
    REPO_NOT_FOUND("Репозиторий не найден или недоступен"),
    INVALID_URL("Некорректный URL репозитория"),
    AUTH_FAILED("Ошибка аутентификации. Проверьте учётные данные"),
    BRANCH_NOT_FOUND("Указанная ветка не найдена"),
    CLONE_FAILED("Не удалось клонировать репозиторий"),
    FETCH_FAILED("Не удалось получить обновления из удалённого репозитория"),
    CHECKOUT_FAILED("Не удалось переключиться на ветку"),
    COMMIT_FAILED("Не удалось создать коммит"),
    PUSH_FAILED("Не удалось отправить изменения в удалённый репозиторий"),
    MERGE_CONFLICT("Обнаружены конфликты слияния"),
    WORKING_DIR_NOT_FOUND("Рабочий каталог не найден"),
    NOT_A_REPOSITORY("Указанный каталог не является git репозиторием"),
    NO_CHANGES("Нет изменений для коммита"),
    MISSING_CONFIG("Отсутствуют необходимые настройки"),
    UNKNOWN("Неизвестная ошибка Git операции")
}

/**
 * Исключение для операций с Git
 */
class GitOperationException(
    val errorType: GitErrorType,
    val technicalMessage: String,
    cause: Throwable? = null
) : RuntimeException(errorType.userMessage, cause) {

    /**
     * Сообщение для отображения пользователю
     */
    val userFriendlyMessage: String
        get() = "${errorType.userMessage}. $technicalMessage"

    /**
     * Полное сообщение для логов
     */
    val logMessage: String
        get() = "[${errorType.name}] $technicalMessage"

    companion object {
        fun repoNotFound(path: String) = GitOperationException(
            GitErrorType.REPO_NOT_FOUND,
            "Путь: $path"
        )

        fun invalidUrl(url: String) = GitOperationException(
            GitErrorType.INVALID_URL,
            "URL: $url"
        )

        fun authFailed(url: String, details: String) = GitOperationException(
            GitErrorType.AUTH_FAILED,
            "URL: $url. Детали: $details"
        )

        fun branchNotFound(branch: String) = GitOperationException(
            GitErrorType.BRANCH_NOT_FOUND,
            "Ветка: $branch"
        )

        fun cloneFailed(url: String, details: String) = GitOperationException(
            GitErrorType.CLONE_FAILED,
            "URL: $url. Детали: $details"
        )

        fun fetchFailed(branch: String, details: String) = GitOperationException(
            GitErrorType.FETCH_FAILED,
            "Ветка: $branch. Детали: $details"
        )

        fun checkoutFailed(branch: String, details: String) = GitOperationException(
            GitErrorType.CHECKOUT_FAILED,
            "Ветка: $branch. Детали: $details"
        )

        fun commitFailed(details: String) = GitOperationException(
            GitErrorType.COMMIT_FAILED,
            "Детали: $details"
        )

        fun pushFailed(branch: String, details: String) = GitOperationException(
            GitErrorType.PUSH_FAILED,
            "Ветка: $branch. Детали: $details"
        )

        fun mergeConflict(sourceBranch: String, targetBranch: String) = GitOperationException(
            GitErrorType.MERGE_CONFLICT,
            "Конфликт между $sourceBranch и $targetBranch"
        )

        fun notARepository(path: String) = GitOperationException(
            GitErrorType.NOT_A_REPOSITORY,
            "Путь: $path"
        )

        fun missingConfig(what: String) = GitOperationException(
            GitErrorType.MISSING_CONFIG,
            "Отсутствует: $what"
        )

        fun unknown(details: String, cause: Throwable? = null) = GitOperationException(
            GitErrorType.UNKNOWN,
            details,
            cause
        )
    }
}
