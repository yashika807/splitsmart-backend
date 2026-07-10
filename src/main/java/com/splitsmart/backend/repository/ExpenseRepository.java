package com.splitsmart.backend.repository;

import com.splitsmart.backend.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
}
