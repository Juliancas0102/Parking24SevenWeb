package com.parking24seven.web;

import com.parking24seven.UsuarioDAO;
import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(name = "LoginServlet", urlPatterns = {"/login"})
public class LoginServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession sesion =
                request.getSession(false);

        if (sesion != null
                && sesion.getAttribute("usuario") != null) {

            String usuario =
                    String.valueOf(
                            sesion.getAttribute("usuario")
                    );

            redirigirSegunUsuario(
                    request,
                    response,
                    usuario
            );

            return;
        }

        request.getRequestDispatcher(
                "/login.jsp"
        ).forward(
                request,
                response
        );
    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        String usuario =
                request.getParameter("usuario");

        String contrasena =
                request.getParameter("contrasena");

        boolean valido =
                UsuarioDAO.validarUsuario(
                        usuario,
                        contrasena
                );

        if (valido) {

            HttpSession sesion =
                    request.getSession(true);

            sesion.setAttribute(
                    "usuario",
                    usuario
            );

            redirigirSegunUsuario(
                    request,
                    response,
                    usuario
            );

        } else {

            request.setAttribute(
                    "mensaje",
                    "Usuario o contraseña incorrectos."
            );

            request.getRequestDispatcher(
                    "/login.jsp"
            ).forward(
                    request,
                    response
            );
        }
    }

    private void redirigirSegunUsuario(
            HttpServletRequest request,
            HttpServletResponse response,
            String usuario)
            throws IOException {

        if ("admin".equalsIgnoreCase(usuario)) {

            response.sendRedirect(
                    request.getContextPath()
                    + "/usuarios"
            );

        } else {

            response.sendRedirect(
                    request.getContextPath()
                    + "/inicio"
            );
        }
    }
}