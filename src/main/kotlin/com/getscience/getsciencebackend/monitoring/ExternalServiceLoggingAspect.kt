package com.getscience.getsciencebackend.monitoring

import io.micrometer.core.instrument.MeterRegistry
import mu.KotlinLogging
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.stereotype.Component

/**
 * Аспект для логирования и сбора метрик вызовов внешних сервисов.
 * 
 * Перехватывает вызовы методов сервисов, логирует информацию о вызове,
 * измеряет время выполнения и регистрирует ошибки при вызове внешних сервисов.
 */
@Aspect
@Component
class ExternalServiceLoggingAspect(
    private val meterRegistry: MeterRegistry
) {
    private val logger = KotlinLogging.logger {}

    /**
     * Логирует информацию о вызове внешнего сервиса и собирает метрики.
     * Перехватывает все вызовы методов в классах пакета service.
     *
     * @param joinPoint точка соединения, представляющая перехваченный метод
     * @return результат выполнения перехваченного метода
     * @throws Exception если произошла ошибка при вызове внешнего сервиса
     */
    @Around("execution(* com.getscience.getsciencebackend..service.*.*(..))")
    fun logExternalServiceCall(joinPoint: ProceedingJoinPoint): Any? {
        val serviceName = joinPoint.target.javaClass.simpleName
        val methodName = joinPoint.signature.name
        
        val timer = meterRegistry.timer("app.external.service.time", 
            "service", serviceName,
            "method", methodName)
        
        logger.info { "Calling external service: $serviceName.$methodName" }
        
        return try {
            timer.record<Any?> {
                joinPoint.proceed()
            }
        } catch (e: Exception) {
            meterRegistry.counter("app.external.service.errors",
                "service", serviceName,
                "method", methodName).increment()
            logger.error(e) { "External service call failed: $serviceName.$methodName" }
            throw e
        } finally {
            logger.info { "Completed external service call: $serviceName.$methodName" }
        }
    }
} 