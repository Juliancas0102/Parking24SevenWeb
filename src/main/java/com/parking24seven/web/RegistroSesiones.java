package com.parking24seven.web;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import jakarta.servlet.annotation.WebListener;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Revocacion inmediata en esta instancia de Tomcat; version_sesion cubre otras instancias. */
@WebListener
public final class RegistroSesiones implements HttpSessionListener {
    private static final ConcurrentHashMap<Integer, Set<HttpSession>> SESIONES = new ConcurrentHashMap<>();

    public static void registrar(int usuarioId, HttpSession sesion) {
        SESIONES.computeIfAbsent(usuarioId, k -> ConcurrentHashMap.newKeySet()).add(sesion);
    }

    public static void revocar(int usuarioId) {
        Set<HttpSession> lista = SESIONES.remove(usuarioId);
        if (lista != null) for (HttpSession s : lista) {
            try { s.invalidate(); } catch (IllegalStateException ignored) { }
        }
    }

    @Override public void sessionDestroyed(HttpSessionEvent evento) {
        HttpSession sesion = evento.getSession();
        Object id;
        try { id = sesion.getAttribute("usuarioId"); }
        catch (IllegalStateException e) { return; }
        if (id instanceof Integer usuarioId) {
            Set<HttpSession> lista = SESIONES.get(usuarioId);
            if (lista != null) {
                lista.remove(sesion);
                if (lista.isEmpty()) SESIONES.remove(usuarioId, lista);
            }
        }
    }
}
