package co.harborbytes.booklion.transaction.validation;

import co.harborbytes.booklion.transaction.TransactionLine;
import co.harborbytes.booklion.transaction.TransactionLineDTO;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;

public class TransactionLineBothAmountsSetWithZeroValidator implements ConstraintValidator<TransactionLineBothAmountsSetWithZeroConstraint, Object> {

    @Override
    public void initialize(TransactionLineBothAmountsSetWithZeroConstraint constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(Object object, ConstraintValidatorContext constraintValidatorContext) {

        BigDecimal zero = new BigDecimal("0.00");
        BigDecimal credit = null;
        BigDecimal debit = null;

        if(object instanceof TransactionLine tlo) {
            credit = tlo.getCreditAmount();
            debit = tlo.getDebitAmount();
        }

        if(object instanceof TransactionLineDTO dto) {
            credit = dto.getCreditAmount();
            debit = dto.getDebitAmount();
        }

        if(credit == null || debit == null)
            return false;

//        boolean bothCreditsAndDebitsAreSet = credit.compareTo(zero) > 0 && debit.compareTo(zero) > 0;
        boolean bothCreditsAndDebitsAreZero = credit.equals(zero) && debit.equals(zero);

//        return !bothCreditsAndDebitsAreSet && !bothCreditsAndDebitsAreZero;
        return !bothCreditsAndDebitsAreZero;
    }


}
