package com.company.commitet_jm.sheduledJob

import com.company.commitet_jm.service.git.GitService
import io.jmix.core.security.SystemAuthenticator
import org.quartz.DisallowConcurrentExecution
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * Quartz-задача, которая периодически забирает следующий коммит со статусом NEW
 * и выполняет его выгрузку в Git-репозиторий.
 *
 * [DisallowConcurrentExecution] гарантирует, что задача не запускается повторно,
 * пока предыдущий запуск не завершён. Без этого при интервале 5 секунд и долгой
 * операции push несколько срабатываний могли бы параллельно обрабатывать один и
 * тот же коммит и конкурировать за рабочий каталог репозитория.
 */
@Component
@DisallowConcurrentExecution
class Committer(
    private val gitService: GitService
) : Job {

    @Autowired
    private lateinit var systemAuthenticator: SystemAuthenticator

    override fun execute(context: JobExecutionContext) {
        systemAuthenticator.runWithSystem {
            gitService.createCommit()
        }
    }
}
