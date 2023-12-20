package co.harborbytes.booklion.advise;

import co.harborbytes.booklion.apiresponsewrapper.ApiResponseFail;
import co.harborbytes.booklion.exception.DomainEntityNotFoundException;

import co.harborbytes.booklion.exception.DomainEntityValidationException;
import co.harborbytes.booklion.exception.TransactionValidationException;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.Random;

@RestControllerAdvice
public class ApplicationExceptionHandler {
    public ApplicationExceptionHandler() {
        System.out.println("---ApplicationExceptionHandler constructed---");
    }

    @ExceptionHandler( value =  {DomainEntityNotFoundException.class, NoResourceFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponseFail notFoundExceptionHandler(Exception ex) {
        ApiResponseFail error = new ApiResponseFail<>();
        error.addMessage(ex.getMessage());
        error.setTimestamp(Instant.now());
        error.setDebug(ex.getClass().getName());
        return error;
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ApiResponseFail methodNotSupportedHandler(HttpRequestMethodNotSupportedException ex) {
        ApiResponseFail error = new ApiResponseFail<>();
        error.addMessage(ex.getMessage());
        error.setTimestamp(Instant.now());
        error.setDebug(ex.getClass().getName());
        return error;
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.CONFLICT)  //  attempting to violate unique-constrainst in tables
    public ApiResponseFail dataIntegrityViolationHandler(DataIntegrityViolationException ex) {
        ApiResponseFail error = new ApiResponseFail<>();

        error.setTimestamp(Instant.now());
        error.setDebug(ex.getClass().getName());

        if (ex.getCause() instanceof ConstraintViolationException cve) {
            error.addMessage(String.format("provided data does not meet constraints - please check, among other things, that fields or ids that should be unique indeed are" ));
            //for logging: cve.getConstraintName()
        }

        if (error.getErrors().isEmpty()) {
            error.addMessage(ex.getMessage());
        }

        return error;
    }



    @ExceptionHandler
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public ApiResponseFail unsupportedMediaType(HttpMediaTypeNotSupportedException ex){
        ApiResponseFail error = new ApiResponseFail<>();

        error.setTimestamp(Instant.now());
        error.setDebug(ex.getClass().getName());
        error.addMessage(String.format("Validation error: unsupported media type %s, please use %s", ex.getContentType().toString(), MediaType.APPLICATION_JSON));
        return error;
    }


    @ExceptionHandler(
            value = {
                    RuntimeException.class,
                    MissingServletRequestParameterException.class, // adding null to ?accountNumber request parameter
                    HandlerMethodValidationException.class, //automatic simple parameter validation without @valid
                    MethodArgumentNotValidException.class, // automatic jakarta.api.validation failures via @Valid
                    MethodArgumentTypeMismatchException.class, //providing a string for a Long id in a rest api
                    TransactionSystemException.class// unable to commit to jpa due to jakarta.api.validation constraints
            }
    )
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponseFail badRequestHandler(Exception ex) {
        ApiResponseFail error = new ApiResponseFail<>();

        error.setTimestamp(Instant.now());
        error.setDebug(ex.getClass().getName());

        if(ex instanceof MissingServletRequestParameterException msrpe){
            error.addMessage(msrpe.getMessage());
        }

        if (ex instanceof MethodArgumentNotValidException manve) {
            manve.getBindingResult().getFieldErrors().forEach(fieldError -> {
                String message = String.format("Validation error: %s %s %n", fieldError.getField(), fieldError.getDefaultMessage());
                error.addMessage(message);
            });
        }

        if (ex instanceof DomainEntityValidationException deve) {
            deve.getBindingResult().getFieldErrors().forEach(fieldError -> {
                String message = String.format("validation error: %s %s %n", fieldError.getField(), fieldError.getDefaultMessage());
                error.addMessage(message);
            });
        }

        if (ex instanceof TransactionValidationException tve) {

                String message = String.format("validation error: %s%n", tve.getMessage());
                error.addMessage(message);
        }

        if (ex instanceof MethodArgumentTypeMismatchException matm) {
            error.addMessage(String.format("Validation error: \"%s\" is not a valid %s parameter", matm.getValue(), matm.getName()));
        }
        //tecnically this one shouldn't happen anymore... Hmm actually, unique constrainst might trigger this, hmm actually those are catched by DataIntegrityViolationException
        if (ex instanceof TransactionSystemException tse) {

            Throwable throwable = tse.getMostSpecificCause();
            if (throwable instanceof jakarta.validation.ConstraintViolationException cve) {
                var violations = cve.getConstraintViolations();

                for (var violation : violations) {
                    error.addMessage(String.format("Validation error: data constraints were not met - \"%s\" %s%n", violation.getPropertyPath().toString(), violation.getMessage()));
                }
            }

            if (error.getErrors().isEmpty()) {
                error.addMessage(tse.getMessage());
            }
        }

        if (ex instanceof  HandlerMethodValidationException hmve){
            hmve.getAllValidationResults().forEach(r ->{
              error.addMessage(String.format("Validation error:  \"%s\"  is not a valid %s parameter", r.getArgument(), r.getMethodParameter().getParameterName()));
            });
        }

        if (error.getErrors().isEmpty()) {
            error.addMessage(
                    "There was a problem understanding either the contents of the request body" +
                            " or one or more header fields. Please make sure the content-type header is" +
                            " set properly for JSON and that the contents of the request body, if required, " +
                            " are also JSON-valid and presented accordingly to the endpoint's requirements");
        }

        return error;
    }

    @ExceptionHandler(value = Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponseFail catchAllHandler(Exception ex) {
        ApiResponseFail error = new ApiResponseFail<>();

        error.addMessage(String.format("Server couldn't handle the request. Generated error code: %s", new Random().nextLong(1L, 10_000_000_000L)));
        error.setTimestamp(Instant.now());
        error.setDebug(ex.getClass().getName());
        return error;
    }
}
