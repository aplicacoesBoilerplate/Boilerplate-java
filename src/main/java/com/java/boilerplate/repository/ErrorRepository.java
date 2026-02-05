package com.java.boilerplate.repository;

import com.java.boilerplate.model.LogErrors;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ErrorRepository extends JpaRepository<LogErrors, Integer> {}
