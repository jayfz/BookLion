package co.harborbytes.booklion.exception;

public class DomainEntityNotFoundException extends RuntimeException{
    public DomainEntityNotFoundException(String className, String field, String value) {
        super(String.format("%s with %s = %s was not found", className, field, value));
    }
}
