package co.harborbytes.booklion.transaction.validation;


import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = TransactionLineBothAmountsSetWithZeroValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface TransactionLineBothAmountsSetWithZeroConstraint {
    String message() default "Transaction line must only have one field, either credits or debits, set to zero and not both";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
