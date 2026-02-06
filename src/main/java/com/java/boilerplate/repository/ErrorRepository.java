package com.java.boilerplate.repository;

import com.java.boilerplate.model.LogErrors;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ErrorRepository extends JpaRepository<LogErrors, Long> {}
