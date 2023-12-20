package co.harborbytes.booklion.transaction.validation;



import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = TransactionValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface TransactionConstraint {
    String message() default "Transaction must be balanced and not contain duplicated accounts";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
