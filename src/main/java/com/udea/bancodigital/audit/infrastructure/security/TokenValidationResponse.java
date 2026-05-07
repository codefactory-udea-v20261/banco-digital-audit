package com.udea.bancodigital.audit.infrastructure.security;

import java.util.List;

public class TokenValidationResponse {

    private boolean active;
    private String sub;
    private List<String> authorities = List.of();
    private String clienteId;
    private String uid;

    public static TokenValidationResponse inactive() {
        TokenValidationResponse response = new TokenValidationResponse();
        response.setActive(false);
        return response;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getSub() {
        return sub;
    }

    public void setSub(String sub) {
        this.sub = sub;
    }

    public List<String> getAuthorities() {
        return authorities == null ? List.of() : authorities;
    }

    public void setAuthorities(List<String> authorities) {
        this.authorities = authorities;
    }

    public String getClienteId() {
        return clienteId;
    }

    public void setClienteId(String clienteId) {
        this.clienteId = clienteId;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }
}
