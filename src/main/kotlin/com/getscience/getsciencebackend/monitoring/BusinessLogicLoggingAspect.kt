package com.getscience.getsciencebackend.monitoring

import io.micrometer.core.instrument.MeterRegistry
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Аспект для логирования и сбора метрик бизнес-операций.
 * 
 * Перехватывает методы, помеченные аннотацией @LogBusinessOperation,
 * логирует их выполнение и собирает метрики о времени выполнения и ошибках.
 */
@Aspect
@Component
class BusinessLogicLoggingAspect(
    private val meterRegistry: MeterRegistry
) {
    private val logger = LoggerFactory.getLogger(BusinessLogicLoggingAspect::class.java)

    /**
     * Логирует выполнение бизнес-операции и собирает метрики.
     * 
     * @param joinPoint точка соединения, представляющая перехваченный метод
     * @param logBusinessOperation аннотация с информацией о бизнес-операции
     * @return результат выполнения перехваченного метода
     * @throws Exception если произошла ошибка при выполнении метода
     */
    @Around("@annotation(logBusinessOperation)")
    fun logBusinessOperation(joinPoint: ProceedingJoinPoint, logBusinessOperation: LogBusinessOperation): Any? {
        val operationName = joinPoint.signature.name
        val timer = meterRegistry.timer("app.business.operation.time", 
            "operation", operationName,
            "type", logBusinessOperation.operationType)
        
        logger.debug("Starting business operation: ${logBusinessOperation.operationType} - ${logBusinessOperation.description}")
        
        return try {
            timer.record<Any?> {
                joinPoint.proceed()
            }
        } catch (e: Exception) {
            meterRegistry.counter("app.business.operation.errors", 
                "operation", operationName,
                "type", logBusinessOperation.operationType).increment()
            logger.error("Business operation failed: ${logBusinessOperation.operationType}", e)
            throw e
        } finally {
            logger.debug("Completed business operation: ${logBusinessOperation.operationType}")
        }
    }
} 