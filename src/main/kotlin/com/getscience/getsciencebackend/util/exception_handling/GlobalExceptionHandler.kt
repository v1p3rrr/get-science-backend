package com.getscience.getsciencebackend.util.exception_handling

import com.getscience.getsciencebackend.util.response_message.ErrorResponse
import io.sentry.SentryClient
import jakarta.servlet.http.HttpServletRequest
import mu.KotlinLogging
import org.hibernate.exception.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@ControllerAdvice
class GlobalExceptionHandler(val sentry: SentryClient) {

    private val logger = KotlinLogging.logger {}

    val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    @ExceptionHandler(IllegalStateException::class, IllegalArgumentException::class)
    fun handleIllegalStateException(ex: RuntimeException, request: HttpServletRequest): ResponseEntity<ErrorResponse> {
        logger.warn { ex.message }
        logger.error { ex.stackTraceToString() }
        sentry.sendException(ex)
        val timestamp = LocalDateTime.now().format(dateTimeFormatter)
        val errorResponse =
            ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.message, request.requestURI, timestamp = timestamp)
        return ResponseEntity<ErrorResponse>(errorResponse, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(NoSuchElementException::class)
    fun handleDataNoSuchElementException(
        ex: DataIntegrityViolationException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn { ex.message }
        logger.error { ex.stackTraceToString() }
        sentry.sendException(ex)
        val timestamp = LocalDateTime.now().format(dateTimeFormatter)
        val errorResponse =
            ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.message, request.requestURI, timestamp = timestamp)
        return ResponseEntity<ErrorResponse>(errorResponse, HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolationException(
        ex: DataIntegrityViolationException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn { ex.message }
        logger.error { ex.stackTraceToString() }
        sentry.sendException(ex)
        val timestamp = LocalDateTime.now().format(dateTimeFormatter)
        val errorResponse =
            ErrorResponse(
                HttpStatus.CONFLICT.value(),
                "Data integrity violation: " + ex.message,
                request.requestURI,
                timestamp = timestamp
            )
        return ResponseEntity<ErrorResponse>(errorResponse, HttpStatus.CONFLICT)
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolationException(
        ex: ConstraintViolationException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn { ex.message }
        logger.error { ex.stackTraceToString() }
        sentry.sendException(ex)
        val timestamp = LocalDateTime.now().format(dateTimeFormatter)
        val errorResponse =
            ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Constraint violation: " + ex.message,
                request.requestURI,
                timestamp = timestamp
            )
        return ResponseEntity<ErrorResponse>(errorResponse, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception, request: HttpServletRequest): ResponseEntity<ErrorResponse> {
        logger.warn { ex.message }
        logger.error { ex.stackTraceToString() }
        sentry.sendException(ex)
        val timestamp = LocalDateTime.now().format(dateTimeFormatter)
        val errorResponse = ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "An unexpected error occurred: " + ex.message,
            request.requestURI,
            timestamp = timestamp
        )
        return ResponseEntity<ErrorResponse>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR)
    }
}