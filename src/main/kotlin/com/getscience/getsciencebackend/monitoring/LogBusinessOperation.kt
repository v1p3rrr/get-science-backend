package com.getscience.getsciencebackend.monitoring

/**
 * Аннотация для маркировки бизнес-операций, требующих логирования и сбора метрик.
 * 
 * Используется совместно с BusinessLogicLoggingAspect для автоматического
 * логирования начала и окончания выполнения бизнес-операций, а также
 * для сбора метрик о времени выполнения и ошибках.
 *
 * @param operationType тип операции для группировки в логах и метриках
 * @param description опциональное описание назначения операции
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class LogBusinessOperation(
    val operationType: String,
    val description: String = ""
) 