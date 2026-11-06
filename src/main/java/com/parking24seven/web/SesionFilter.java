package com.parking24seven.web;

import com.parking24seven.UsuarioDAO;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

/** Revalida el usuario, su rol y la version de sesion en cada peticion protegida. */
@WebFilter(urlPatterns = {"/usuarios", "/inicio", "/usuarios.jsp", "/inicio.jsp"})
public final class SesionFilter implements Filter {
    @Override public void doFilter(ServletRequest req, ServletResponse res,
            FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        HttpSession s = request.getSession(false);
        if (s == null || !(s.getAttribute("usuarioId") instanceof Integer id)
                || !(s.getAttribute("usuario") instanceof String nombre)
                || !(s.getAttribute("versionSesion") instanceof Long version)) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        UsuarioDAO.EstadoSesion estado = UsuarioDAO.obtenerEstadoSesion(nombre);
        if (estado == null || estado.id() != id || !estado.activo()
                || !estado.activado() || estado.version() != version) {
            s.invalidate();
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        s.setAttribute("rol", estado.rol());
        String path = request.getServletPath();
        if (("/usuarios".equals(path) || "/usuarios.jsp".equals(path))
                && !"ADMINISTRADOR".equalsIgnoreCase(estado.rol())) {
            response.sendRedirect(request.getContextPath() + "/inicio");
            return;
        }
        chain.doFilter(req, res);
    }
}
