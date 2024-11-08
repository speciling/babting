package org.cookieandkakao.babting.common.exception;
import org.cookieandkakao.babting.common.apiresponse.ApiResponseBody.FailureBody;
import org.cookieandkakao.babting.common.apiresponse.ApiResponseGenerator;
import org.cookieandkakao.babting.common.exception.customexception.ApiException;
import org.cookieandkakao.babting.common.exception.customexception.CacheEvictionException;
import org.cookieandkakao.babting.common.exception.customexception.EventCreationException;
import org.cookieandkakao.babting.common.exception.customexception.ExpiredTokenException;
import org.cookieandkakao.babting.common.exception.customexception.InvalidFoodPreferenceTypeException;
import org.cookieandkakao.babting.common.exception.customexception.JsonConversionException;
import org.cookieandkakao.babting.common.exception.customexception.MemberNotFoundException;
import org.cookieandkakao.babting.common.exception.customexception.FoodNotFoundException;
import org.cookieandkakao.babting.common.exception.customexception.PreferenceConflictException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<FailureBody> handleValidationException(MethodArgumentNotValidException ex) {
        return ApiResponseGenerator.fail(HttpStatus.BAD_REQUEST, "유효성 검사 오류");
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<FailureBody> handleApiException(ApiException ex) {
        return ApiResponseGenerator.fail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MemberNotFoundException.class)
    public ResponseEntity<FailureBody> handleMemberNotFoundException(MemberNotFoundException ex) {
        return ApiResponseGenerator.fail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(EventCreationException.class)
    public ResponseEntity<FailureBody> handleEventCreationException(EventCreationException ex) {
        return ApiResponseGenerator.fail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    @ExceptionHandler(JsonConversionException.class)
    public ResponseEntity<FailureBody> handleJsonConversionException(JsonConversionException ex) {
        return ApiResponseGenerator.fail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(FoodNotFoundException.class)
    public ResponseEntity<FailureBody> handleFoodNotFoundException(FoodNotFoundException ex) {
        return ApiResponseGenerator.fail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(PreferenceConflictException.class)
    public ResponseEntity<FailureBody> handlePreferenceConflictException(PreferenceConflictException ex) {
        return ApiResponseGenerator.fail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(InvalidFoodPreferenceTypeException.class)
    public ResponseEntity<FailureBody> handleInvalidFoodPreferenceTypeException(InvalidFoodPreferenceTypeException ex) {
        return ApiResponseGenerator.fail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(CacheEvictionException.class)
    public ResponseEntity<FailureBody> handleCacheEvictionException(CacheEvictionException ex) {
        return ApiResponseGenerator.fail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());

    @ExceptionHandler(ExpiredTokenException.class)
    public ResponseEntity<FailureBody> handleExpiredTokenException(ExpiredTokenException ex) {
        return ApiResponseGenerator.fail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    // 모든 Exception을 처리하는 핸들러
    @ExceptionHandler(Exception.class)
    public ResponseEntity<FailureBody> handleAllExceptions(Exception ex) {
        return ApiResponseGenerator.fail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }
}
