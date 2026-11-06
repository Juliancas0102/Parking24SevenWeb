package com.parking24seven.web;

import com.parking24seven.UsuarioDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet(name = "LoginServlet", urlPatterns = {"/login"})
public class LoginServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession sesion = req.getSession(false);
        if (sesion != null && sesion.getAttribute("usuarioId") != null) {
            resp.sendRedirect(req.getContextPath() + "/inicio");
            return;
        }
        req.getRequestDispatcher("/login.jsp").forward(req, resp);
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String nombre = req.getParameter("usuario");
        String password = req.getParameter("contrasena");
        if (UsuarioDAO.validarUsuario(nombre, password)) {
            UsuarioDAO.EstadoSesion estado = UsuarioDAO.obtenerEstadoSesion(nombre);
            if (estado != null && estado.activo() && estado.activado()) {
                HttpSession anterior = req.getSession(false);
                if (anterior != null) anterior.invalidate(); // evita fijacion de sesion
                HttpSession sesion = req.getSession(true);
                sesion.setAttribute("usuario", estado.nombre());
                sesion.setAttribute("usuarioId", estado.id());
                sesion.setAttribute("rol", estado.rol());
                sesion.setAttribute("versionSesion", estado.version());
                RegistroSesiones.registrar(estado.id(), sesion);
                resp.sendRedirect(req.getContextPath() + ("ADMINISTRADOR".equalsIgnoreCase(estado.rol())
                        ? "/usuarios" : "/inicio"));
                return;
            }
        }
        req.setAttribute("mensaje", "Credenciales incorrectas o cuenta pendiente de activacion.");
        req.getRequestDispatcher("/login.jsp").forward(req, resp);
    }
}
