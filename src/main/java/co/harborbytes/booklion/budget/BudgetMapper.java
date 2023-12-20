package co.harborbytes.booklion.budget;

import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface BudgetMapper {

    @Mapping( target = "account", ignore = true)
    @Mapping( target = "user", ignore = true)
    Budget dtoToBudget (BudgetDTO dto);
    BudgetDTO budgetToDto(Budget budget);
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping( target = "id", ignore = true)
    @Mapping( target = "account", ignore = true)
    @Mapping( target = "user", ignore = true)
    void updateBudgetFromDto(BudgetDTO dto, @MappingTarget Budget budget);
}
