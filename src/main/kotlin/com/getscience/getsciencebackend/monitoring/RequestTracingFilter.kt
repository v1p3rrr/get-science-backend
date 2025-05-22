package com.getscience.getsciencebackend.monitoring

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

/**
 * Фильтр для трассировки HTTP-запросов через уникальные идентификаторы.
 * 
 * Добавляет уникальный идентификатор трассировки (trace ID) в каждый запрос,
 * что позволяет отслеживать запрос через все слои приложения и в логах.
 * Если заголовок X-Trace-ID уже присутствует в запросе, использует его значение.
 * В противном случае генерирует новый идентификатор.
 */
@Component
class RequestTracingFilter : OncePerRequestFilter() {
    
    /**
     * Обрабатывает HTTP-запрос, добавляя идентификатор трассировки.
     * 
     * Метод извлекает trace ID из заголовка запроса или генерирует новый,
     * добавляет его в контекст MDC для логгирования и в заголовок ответа.
     * 
     * @param request HTTP-запрос
     * @param response HTTP-ответ
     * @param chain цепочка фильтров для дальнейшей обработки запроса
     */
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        val traceId = request.getHeader("X-Trace-ID") ?: UUID.randomUUID().toString()
        MDC.put("traceId", traceId)
        response.setHeader("X-Trace-ID", traceId)
        try {
            chain.doFilter(request, response)
        } finally {
            MDC.clear()
        }
    }
} 