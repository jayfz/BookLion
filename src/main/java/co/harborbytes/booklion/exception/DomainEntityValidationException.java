package co.harborbytes.booklion.exception;

import org.springframework.validation.BindingResult;

public class DomainEntityValidationException extends RuntimeException{

    private BindingResult bindingResult;
    public DomainEntityValidationException(BindingResult result) {
        this.bindingResult = result;
    }

    public BindingResult getBindingResult() {
        return bindingResult;
    }
}
