package com.apac.assessment.exception;

import com.apac.assessment.dto.response.ProblemDto;
import com.apac.assessment.dto.response.ProblemFieldErrorDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<ProblemDto> handleBadRequest(
			BadRequestException exception,
			HttpServletRequest request) {
		return problem(HttpStatus.BAD_REQUEST, exception.getMessage(), request, List.of());
	}

	@ExceptionHandler(PolicyNotFoundException.class)
	public ResponseEntity<ProblemDto> handleNotFound(
			PolicyNotFoundException exception,
			HttpServletRequest request) {
		return problem(HttpStatus.NOT_FOUND, exception.getMessage(), request, List.of());
	}

	@ExceptionHandler({
			MethodArgumentNotValidException.class,
			HandlerMethodValidationException.class,
			ConstraintViolationException.class
	})
	public ResponseEntity<ProblemDto> handleValidation(
			Exception exception,
			HttpServletRequest request) {
		List<ProblemFieldErrorDto> errors = exception instanceof MethodArgumentNotValidException validationException
				? validationException.getBindingResult().getFieldErrors().stream()
						.map(error -> new ProblemFieldErrorDto(error.getField(), error.getDefaultMessage()))
						.toList()
				: List.of();
		return problem(HttpStatus.BAD_REQUEST, "Request validation failed.", request, errors);
	}

	@ExceptionHandler({
			MethodArgumentTypeMismatchException.class,
			HttpMessageNotReadableException.class
	})
	public ResponseEntity<ProblemDto> handleMalformedRequest(
			Exception exception,
			HttpServletRequest request) {
		String detail = exception instanceof MethodArgumentTypeMismatchException mismatch
				? "Invalid value for parameter '" + mismatch.getName() + "'."
				: "The request body is missing or malformed.";
		return problem(HttpStatus.BAD_REQUEST, detail, request, List.of());
	}

	@ExceptionHandler(DataAccessException.class)
	public ResponseEntity<ProblemDto> handleDataAccess(
			DataAccessException exception,
			HttpServletRequest request) {
		LOGGER.error("Database request failed.", exception);
		return problem(
				HttpStatus.SERVICE_UNAVAILABLE,
				"The policy data service is temporarily unavailable.",
				request,
				List.of());
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ProblemDto> handleUnexpected(
			Exception exception,
			HttpServletRequest request) {
		LOGGER.error("Unexpected request failure.", exception);
		return problem(
				HttpStatus.INTERNAL_SERVER_ERROR,
				"An unexpected server error occurred.",
				request,
				List.of());
	}

	private ResponseEntity<ProblemDto> problem(
			HttpStatus status,
			String detail,
			HttpServletRequest request,
			List<ProblemFieldErrorDto> errors) {
		ProblemDto body = new ProblemDto(
				"about:blank",
				status.getReasonPhrase(),
				status.value(),
				detail,
				request.getRequestURI(),
				errors);
		return ResponseEntity.status(status)
				.contentType(MediaType.APPLICATION_PROBLEM_JSON)
				.body(body);
	}
}
