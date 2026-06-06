package com.java.boilerplate.repository;

import com.java.boilerplate.model.AppContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IAppContextRepository extends JpaRepository<AppContext, String> {
}
