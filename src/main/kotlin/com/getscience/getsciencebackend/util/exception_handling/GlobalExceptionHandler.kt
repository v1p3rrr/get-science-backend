package com.getscience.getsciencebackend.util.exception_handling

import com.getscience.getsciencebackend.util.response_message.ErrorResponse
import io.sentry.Sentry
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
import org.springframework.data.redis.RedisConnectionFailureException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException

@ControllerAdvice
class GlobalExceptionHandler {

    private val logger = KotlinLogging.logger {}

    val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    @ExceptionHandler(IllegalStateException::class, IllegalArgumentException::class)
    fun handleIllegalStateException(ex: RuntimeException, request: HttpServletRequest): ResponseEntity<ErrorResponse> {
        logger.warn { ex.message }
        logger.error { ex.stackTraceToString() }
        Sentry.captureException(ex)
        val timestamp = LocalDateTime.now().format(dateTimeFormatter)
        val message = ex.message ?: "Invalid request"
        val status = if (message.contains("already exists", ignoreCase = true)) {
            HttpStatus.CONFLICT // 409
        } else {
            HttpStatus.BAD_REQUEST
        }

        val errorResponse = ErrorResponse(
            status.value(),
            message,
            request.requestURI,
            timestamp
        )

        return ResponseEntity(errorResponse, status)
    }

    @ExceptionHandler(NoSuchElementException::class)
    fun handleDataNoSuchElementException(
        ex: DataIntegrityViolationException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn { ex.message }
        logger.error { ex.stackTraceToString() }
        Sentry.captureException(ex)
        val timestamp = LocalDateTime.now().format(dateTimeFormatter)
        val errorResponse =
            ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.message, request.requestURI, timestamp = timestamp)
        return ResponseEntity<ErrorResponse>(errorResponse, HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler(BadCredentialsException::class)
    fun handleBadCredentialsException(
        ex: BadCredentialsException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn { "Bad credentials: ${ex.message}" }
        Sentry.captureException(ex)
        val timestamp = LocalDateTime.now().format(dateTimeFormatter)
        val errorResponse = ErrorResponse(
            status = HttpStatus.UNAUTHORIZED.value(),
            error = "Invalid username or password",
            path = request.requestURI,
            timestamp = timestamp
        )
        return ResponseEntity(errorResponse, HttpStatus.UNAUTHORIZED)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
fun handleValidationException(
        ex: MethodArgumentNotValidException,
        request: HttpServletRequest
): ResponseEntity<ErrorResponse> {
    val fieldError = ex.bindingResult.allErrors.firstOrNull() as? FieldError
    val message = fieldError?.defaultMessage ?: "Validation error"

    logger.warn { message }
    Sentry.captureException(ex)

    val timestamp = LocalDateTime.now().format(dateTimeFormatter)
    val errorResponse = ErrorResponse(
        status = HttpStatus.BAD_REQUEST.value(),
        error = message,
        path = request.requestURI,
        timestamp = timestamp
    )

    return ResponseEntity(errorResponse, HttpStatus.BAD_REQUEST)
}

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolationException(
        ex: DataIntegrityViolationException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn { ex.message }
        logger.error { ex.stackTraceToString() }
        Sentry.captureException(ex)
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
        Sentry.captureException(ex)
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
        Sentry.captureException(ex)
        val timestamp = LocalDateTime.now().format(dateTimeFormatter)
        val errorResponse = ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "An unexpected error occurred: " + ex.message,
            request.requestURI,
            timestamp = timestamp
        )
        return ResponseEntity<ErrorResponse>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @ExceptionHandler(RedisConnectionFailureException::class)
    fun handleRedisConnectionFailureException(
        ex: RedisConnectionFailureException,
        request: HttpServletRequest
    ): ResponseEntity<Void> {
        logger.warn { "Redis недоступен: ${ex.message}" }
        return ResponseEntity.ok().build()
    }
}