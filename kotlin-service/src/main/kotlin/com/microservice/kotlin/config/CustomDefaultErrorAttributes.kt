package com.microservice.kotlin.config

import org.apache.commons.lang.exception.ExceptionUtils
import org.slf4j.LoggerFactory
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes
import org.springframework.http.HttpStatus
import org.springframework.security.core.AuthenticationException
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.server.ResponseStatusException
import javax.servlet.http.HttpServletRequest
import javax.validation.ConstraintViolationException

class CustomDefaultErrorAttributes : DefaultErrorAttributes() {
    private val log = LoggerFactory.getLogger(javaClass)

    fun getErrorAttributes(request: HttpServletRequest, throwable: Throwable, includeStackTrace: Boolean): Map<String, Any> {
        val errorAttributes = super.getErrorAttributes(ServletWebRequest(request), includeStackTrace)
        val status = getHttpStatusError(throwable)
        errorAttributes["status"] = status.value()
        errorAttributes["message"] = ExceptionUtils.getMessage(throwable)
        errorAttributes["error"] = status
        log.debug("Default Error Attributes: {}", errorAttributes)
        return errorAttributes
    }

    /**
     * Return [HttpStatus] according to type of Exception.
     * @param ex current exception
     * @return httpStatus
     */
    private fun getHttpStatusError(ex: Throwable): HttpStatus {
        var httpStatus = HttpStatus.SERVICE_UNAVAILABLE
        if (ex is HttpStatusCodeException) {
            httpStatus = ex.statusCode
        } else if (ex is ResponseStatusException) {
            httpStatus = ex.status
        } else if (ex is AuthenticationException) {
            httpStatus = HttpStatus.UNAUTHORIZED
        } else if (ex is ConstraintViolationException) {
            httpStatus = HttpStatus.BAD_REQUEST
        }
        return httpStatus
    }
}
