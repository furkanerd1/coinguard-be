package com.coinguard.budget.service;

import com.coinguard.budget.dto.request.CreateBudgetRequest;
import com.coinguard.budget.dto.response.BudgetResponse;
import com.coinguard.budget.entity.Budget;
import com.coinguard.budget.mapper.BudgetMapper;
import com.coinguard.budget.repository.BudgetRepository;
import com.coinguard.common.exception.ActiveBudgetAlreadyExistsException;
import com.coinguard.common.exception.BudgetNotFoundException;
import com.coinguard.common.exception.InvalidBudgetPeriodException;
import com.coinguard.common.exception.UserNotFoundException;
import com.coinguard.receipt.enums.ReceiptCategory;
import com.coinguard.user.entity.User;
import com.coinguard.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetServiceImplTest {

    @InjectMocks
    private BudgetServiceImpl budgetService;

    @Mock
    private BudgetRepository budgetRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private BudgetMapper budgetMapper;

    // --- Helper Methods to Create Dummy Data ---

    private User createDummyUser(Long id) {
        return User.builder()
                .id(id)
                .username("testuser")
                .email("test@example.com")
                .build();
    }

    private CreateBudgetRequest createValidRequest() {
        return new CreateBudgetRequest(
                ReceiptCategory.FOOD_BEVERAGE,
                new BigDecimal("5000.00"),
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                80
        );
    }

    private Budget createDummyBudget(User user) {
        return Budget.builder()
                .id(100L)
                .user(user)
                .category(ReceiptCategory.FOOD_BEVERAGE)
                .limitAmount(new BigDecimal("5000.00"))
                .spentAmount(BigDecimal.ZERO)
                .isActive(true)
                .build();
    }


    @Test
    @DisplayName("Should create budget successfully when request is valid")
    void shouldCreateBudget_Success() {
        // GIVEN
        Long userId = 1L;
        CreateBudgetRequest request = createValidRequest();
        User user = createDummyUser(userId);
        Budget budget = createDummyBudget(user);
        BudgetResponse expectedResponse = new BudgetResponse(100L, 1L, ReceiptCategory.FOOD_BEVERAGE, BigDecimal.valueOf(5000), BigDecimal.ZERO, BigDecimal.valueOf(5000), 0.0, LocalDate.now(), LocalDate.now().plusDays(30), true, false);

        // Mock behaviors
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(budgetRepository.findByUserIdAndCategoryAndIsActiveTrue(userId, request.category()))
                .thenReturn(Optional.empty()); // Çakışma yok
        when(budgetRepository.save(any(Budget.class))).thenReturn(budget);
        when(budgetMapper.toBudgetResponse(budget)).thenReturn(expectedResponse);

        // WHEN
        BudgetResponse result = budgetService.createBudget(userId, request);

        // THEN
        assertNotNull(result);
        assertEquals(expectedResponse.id(), result.id());

        verify(userRepository).findById(userId);
        verify(budgetRepository).save(any(Budget.class));
    }

    @Test
    @DisplayName("Should throw InvalidBudgetPeriodException when end date is before start date")
    void shouldThrowException_WhenEndDateIsBeforeStartDate() {
        // GIVEN
        Long userId = 1L;
        CreateBudgetRequest request = new CreateBudgetRequest(
                ReceiptCategory.FOOD_BEVERAGE,
                BigDecimal.TEN,
                LocalDate.now().plusDays(5),
                LocalDate.now(),
                80
        );

        // WHEN & THEN
        assertThrows(InvalidBudgetPeriodException.class, () -> budgetService.createBudget(userId, request));

        // Repository'ye hiç gitmemeli
        verifyNoInteractions(userRepository);
        verifyNoInteractions(budgetRepository);
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when user does not exist")
    void shouldThrowException_WhenUserNotFound() {
        // GIVEN
        Long userId = 99L;
        CreateBudgetRequest request = createValidRequest();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // WHEN & THEN
        assertThrows(UserNotFoundException.class, () -> budgetService.createBudget(userId, request));

        verify(budgetRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ActiveBudgetAlreadyExistsException when user already has active budget in category")
    void shouldThrowException_WhenBudgetAlreadyExists() {
        // GIVEN
        Long userId = 1L;
        CreateBudgetRequest request = createValidRequest();
        User user = createDummyUser(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(budgetRepository.findByUserIdAndCategoryAndIsActiveTrue(userId, request.category()))
                .thenReturn(Optional.of(createDummyBudget(user)));

        // WHEN & THEN
        assertThrows(ActiveBudgetAlreadyExistsException.class, () -> budgetService.createBudget(userId, request));

        verify(budgetRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should return list of budgets for user")
    void shouldGetUserBudgets_Success() {
        // GIVEN
        Long userId = 1L;
        List<Budget> budgets = List.of(createDummyBudget(createDummyUser(userId)));
        List<BudgetResponse> responses = List.of(new BudgetResponse(100L, 1L, ReceiptCategory.FOOD_BEVERAGE, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN, 0.0, LocalDate.now(), LocalDate.now(), true, false));

        when(budgetRepository.findAllByUserId(userId)).thenReturn(budgets);
        when(budgetMapper.toBudgetResponseList(budgets)).thenReturn(responses);

        // WHEN
        List<BudgetResponse> result = budgetService.getUserBudgets(userId);

        // THEN
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(budgetRepository).findAllByUserId(userId);
    }

    @Test
    @DisplayName("Should delete budget successfully when user owns it")
    void shouldDeleteBudget_Success() {
        // GIVEN
        Long userId = 1L;
        Long budgetId = 100L;
        User user = createDummyUser(userId);
        Budget budget = createDummyBudget(user);

        when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(budget));

        // WHEN
        budgetService.deleteBudget(userId, budgetId);

        // THEN
        verify(budgetRepository).delete(budget);
    }

    @Test
    @DisplayName("Should throw BudgetNotFoundException when budget id does not exist")
    void shouldThrowException_WhenBudgetToDeleteNotFound() {
        // GIVEN
        Long userId = 1L;
        Long budgetId = 999L;

        when(budgetRepository.findById(budgetId)).thenReturn(Optional.empty());

        // WHEN & THEN
        assertThrows(BudgetNotFoundException.class, () -> budgetService.deleteBudget(userId, budgetId));

        verify(budgetRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should throw SecurityException when user tries to delete someone else's budget")
    void shouldThrowException_WhenDeletingOthersBudget() {
        // GIVEN
        Long currentUserId = 1L;
        Long budgetOwnerId = 2L;
        Long budgetId = 100L;

        User ownerUser = createDummyUser(budgetOwnerId);
        Budget budget = createDummyBudget(ownerUser);

        when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(budget));
        // WHEN & THEN
        assertThrows(SecurityException.class, () -> budgetService.deleteBudget(currentUserId, budgetId));

        verify(budgetRepository, never()).delete(any());
    }
}