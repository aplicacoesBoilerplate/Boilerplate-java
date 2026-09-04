package com.java.boilerplate.enums;

public enum EAcaoRbac {
    CONSULTAR("consultar"),
    GRAVAR("gravar"),
    EDITAR("editar"),
    REMOVER("remover");

    private final String valor;

    EAcaoRbac(String pValor) {
        this.valor = pValor;
    }

    public String valor() {
        return valor;
    }
}
