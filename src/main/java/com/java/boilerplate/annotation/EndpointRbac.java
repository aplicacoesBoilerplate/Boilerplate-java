package com.java.boilerplate.annotation;

import com.java.boilerplate.enums.EAcaoRbac;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EndpointRbac {
    String recurso();

    EAcaoRbac acao();
}
