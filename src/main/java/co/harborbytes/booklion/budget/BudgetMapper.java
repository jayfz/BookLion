package co.harborbytes.booklion.budget;

import org.mapstruct.*;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public abstract class BudgetMapper {

//    @Mapping( target = "account", ignore = true)
//    @Mapping( target = "user", ignore = true)
//    public abstract Budget dtoToBudget (BudgetDTO dto);
//    public abstract BudgetDTO budgetToDto(Budget budget);
//    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
//    @Mapping( target = "id", ignore = true)
//    @Mapping( target = "account", ignore = true)
//    @Mapping( target = "user", ignore = true)
//    void updateBudgetFromDto(BudgetDTO dto, @MappingTarget Budget budget);

    public abstract Budget createBudgetDtoToBudget(CreateBudgetDTO dto);
    public ReadBudgetDTO budgetToReadBudgetDto(Budget budget){
        if(budget == null){
            return null;
        }
        ReadBudgetDTO readBudgetDTO = new ReadBudgetDTO();
        readBudgetDTO.setId(budget.getId());
        readBudgetDTO.setDescription(budget.getDescription());
        readBudgetDTO.setAmount(budget.getAmount());
        readBudgetDTO.setAccountNumber(budget.getAccount().getNumber());
        readBudgetDTO.setSpentSoFar(new BigDecimal("0.00"));

        return readBudgetDTO;
    }

    public BudgetWithSpendingOverTimeDTO budgetToBudgetWithSpendingOverTimeDTO(Budget budget){
        if(budget == null){
            return null;
        }

        BudgetWithSpendingOverTimeDTO spendingOverTimeDTO = new BudgetWithSpendingOverTimeDTO();
        spendingOverTimeDTO.setBudgetId(budget.getId());
        spendingOverTimeDTO.setName(budget.getDescription());
        spendingOverTimeDTO.setAmount(budget.getAmount());
        spendingOverTimeDTO.setAccountNumber(budget.getAccount().getNumber());

        return spendingOverTimeDTO;
    }
}
