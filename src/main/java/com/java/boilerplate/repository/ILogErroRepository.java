package com.java.boilerplate.repository;

import com.java.boilerplate.model.CLogErro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ILogErroRepository extends JpaRepository<CLogErro, Long> {
}
