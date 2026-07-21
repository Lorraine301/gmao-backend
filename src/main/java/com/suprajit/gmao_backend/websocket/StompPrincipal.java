package com.suprajit.gmao_backend.websocket;

import java.security.Principal;

public class StompPrincipal implements Principal {

    private final String name; // ici : l'id utilisateur en String

    public StompPrincipal(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}