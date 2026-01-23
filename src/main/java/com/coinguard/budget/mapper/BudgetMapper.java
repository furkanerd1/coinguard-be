package com.coinguard.budget.mapper;

import com.coinguard.budget.dto.response.BudgetResponse;
import com.coinguard.budget.entity.Budget;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface BudgetMapper {

    @Mapping(target = "userId", source = "budget.user.id")
    @Mapping(target = "remainingAmount", expression = "java(budget.getRemainingAmount())")
    @Mapping(target = "usagePercentage", expression = "java(budget.getUsagePercentage())")
    @Mapping(target = "isExceeded", expression = "java(budget.isOverBudget())")
    BudgetResponse toBudgetResponse(Budget budget);

    List<BudgetResponse> toBudgetResponseList(List<Budget> budgets);
}
