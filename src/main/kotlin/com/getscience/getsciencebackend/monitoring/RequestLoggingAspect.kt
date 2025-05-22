package com.getscience.getsciencebackend.monitoring

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import io.sentry.Sentry
import mu.KotlinLogging
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import jakarta.servlet.http.HttpServletRequest

/**
 * Аспект для логирования и мониторинга HTTP запросов
 * 
 * Этот аспект перехватывает все HTTP запросы, логирует их информацию
 * и отправляет метрики в Prometheus и Sentry.
 */
@Aspect
@Component
class RequestLoggingAspect(
    private val meterRegistry: MeterRegistry,
    private val httpRequestTimer: Timer
) {
    private val logger = KotlinLogging.logger {}

    /**
     * Перехватывает и логирует HTTP запросы к REST-контроллерам.
     *
     * Регистрирует информацию о запросе, измеряет время выполнения, 
     * увеличивает счетчики метрик и отправляет информацию об ошибках в Sentry.
     * 
     * @param joinPoint точка соединения, представляющая перехваченный метод контроллера
     * @return результат выполнения метода контроллера
     * @throws Exception если произошла ошибка при обработке запроса
     */
    @Around("@within(org.springframework.web.bind.annotation.RestController) || @within(org.springframework.stereotype.Controller)")
    fun logRequest(joinPoint: ProceedingJoinPoint): Any? {
        val request = (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request
        val startTime = System.currentTimeMillis()
        
        try {
            // Логируем информацию о запросе
            logRequestInfo(request, joinPoint)
            
            // Измеряем время выполнения
            return httpRequestTimer.record<Any?> {
                joinPoint.proceed()
            }
        } catch (e: Exception) {
            // Логируем ошибку
            logError(e, request, joinPoint)
            throw e
        } finally {
            // Увеличиваем счетчик запросов
            meterRegistry.counter("http.server.requests.total", "type", "all").increment()
        }
    }

    /**
     * Логирует детальную информацию о входящем HTTP запросе.
     *
     * @param request HTTP запрос
     * @param joinPoint точка соединения метода контроллера
     */
    private fun logRequestInfo(request: HttpServletRequest?, joinPoint: ProceedingJoinPoint) {
        request?.let {
            val logMessage = mapOf(
                "method" to it.method,
                "uri" to it.requestURI,
                "query" to it.queryString,
                "clientIp" to it.remoteAddr,
                "userAgent" to it.getHeader("User-Agent"),
                "endpoint" to joinPoint.signature.toShortString()
            )
            logger.info { "Incoming request: $logMessage" }
        }
    }

    /**
     * Логирует информацию об ошибке при обработке HTTP запроса.
     * Отправляет информацию об ошибке в Sentry для дальнейшего анализа.
     *
     * @param e исключение, возникшее при обработке запроса
     * @param request HTTP запрос
     * @param joinPoint точка соединения метода контроллера
     */
    private fun logError(e: Exception, request: HttpServletRequest?, joinPoint: ProceedingJoinPoint) {
        val errorContext = mapOf(
            "error" to e.message,
            "method" to request?.method,
            "uri" to request?.requestURI,
            "endpoint" to joinPoint.signature.toShortString()
        )
        logger.error(e) { "Request failed: $errorContext" }
        Sentry.captureException(e)
    }
} 