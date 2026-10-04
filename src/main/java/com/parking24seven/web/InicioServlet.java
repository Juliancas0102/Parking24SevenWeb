package com.parking24seven.web;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(name = "InicioServlet", urlPatterns = {"/inicio"})
public class InicioServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession sesion =
                request.getSession(false);

        // SI NO HAY SESION, VOLVER AL LOGIN
        if (sesion == null
                || sesion.getAttribute("usuario") == null) {

            response.sendRedirect(
                    request.getContextPath()
                    + "/login"
            );

            return;
        }

        // MOSTRAR LA PAGINA JSP
        request.getRequestDispatcher(
                "/inicio.jsp"
        ).forward(
                request,
                response
        );
    }
}