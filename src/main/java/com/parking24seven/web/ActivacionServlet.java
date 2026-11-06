package com.parking24seven.web;

import com.parking24seven.ActivacionUtil;
import com.parking24seven.UsuarioDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(name = "ActivacionServlet", urlPatterns = {"/activar"})
public final class ActivacionServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setHeader("Cache-Control", "no-store");
        resp.setHeader("Referrer-Policy", "no-referrer");
        String token = req.getParameter("token");
        req.setAttribute("token", ActivacionUtil.hash(token) == null ? "" : token);
        req.getRequestDispatcher("/activar.jsp").forward(req, resp);
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setHeader("Cache-Control", "no-store");
        String hash = ActivacionUtil.hash(req.getParameter("token"));
        UsuarioDAO.ResultadoOperacion resultado = UsuarioDAO.activarConToken(hash);
        req.setAttribute("mensaje", resultado == UsuarioDAO.ResultadoOperacion.EXITOSO
                ? "Cuenta activada. Ya puedes iniciar sesion."
                : "El enlace es invalido, ya se utilizo o ha caducado. Solicita otro al administrador.");
        req.getRequestDispatcher("/activar.jsp").forward(req, resp);
    }
}
