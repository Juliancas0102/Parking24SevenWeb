package com.parking24seven.web;

import com.parking24seven.UsuarioDAO;
import java.io.IOException;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(name = "UsuarioServlet", urlPatterns = {"/usuarios"})
public class UsuarioServlet extends HttpServlet {

    // ==========================================
    // COMPROBAR SI EL USUARIO ES ADMIN
    // ==========================================
    private boolean esAdmin(HttpServletRequest request) {

        HttpSession sesion =
                request.getSession(false);

        if (sesion == null) {
            return false;
        }

        Object usuario =
                sesion.getAttribute("usuario");

        return usuario != null
                && "admin".equalsIgnoreCase(
                        usuario.toString()
                );
    }

    // ==========================================
    // MOSTRAR MODULO DE USUARIOS
    // ==========================================
    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession sesion =
                request.getSession(false);

        // SI NO HAY SESION
        if (sesion == null
                || sesion.getAttribute("usuario") == null) {

            response.sendRedirect(
                    request.getContextPath()
                    + "/login"
            );

            return;
        }

        // SI HAY SESION PERO NO ES ADMIN
        if (!esAdmin(request)) {

            response.sendRedirect(
                    request.getContextPath()
                    + "/inicio"
            );

            return;
        }

        // OBTENER USUARIOS DE MYSQL
        List<String[]> usuarios =
                UsuarioDAO.obtenerUsuarios();

        request.setAttribute(
                "usuarios",
                usuarios
        );

        // ======================================
        // COMPROBAR SI SE QUIERE EDITAR
        // ======================================
        String idEditar =
                request.getParameter("editar");

        String usuarioEditar = null;

        if (idEditar != null
                && !"1".equals(idEditar)) {

            for (String[] usuario : usuarios) {

                if (usuario[0].equals(idEditar)) {

                    usuarioEditar =
                            usuario[1];

                    break;
                }
            }
        }

        request.setAttribute(
                "idEditar",
                idEditar
        );

        request.setAttribute(
                "usuarioEditar",
                usuarioEditar
        );

        // ======================================
        // MOSTRAR usuarios.jsp
        // ======================================
        request.getRequestDispatcher(
                "/usuarios.jsp"
        ).forward(
                request,
                response
        );
    }

    // ==========================================
    // CREAR / ACTUALIZAR / ELIMINAR
    // ==========================================
    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession sesion =
                request.getSession(false);

        // SI NO HAY SESION
        if (sesion == null
                || sesion.getAttribute("usuario") == null) {

            response.sendRedirect(
                    request.getContextPath()
                    + "/login"
            );

            return;
        }

        // SI EL USUARIO NO ES ADMIN
        if (!esAdmin(request)) {

            response.sendRedirect(
                    request.getContextPath()
                    + "/inicio"
            );

            return;
        }

        request.setCharacterEncoding("UTF-8");

        String accion =
                request.getParameter("accion");

        // ======================================
        // CREAR USUARIO
        // ======================================
        if ("crear".equals(accion)) {

            String usuario =
                    request.getParameter("usuario");

            String contrasena =
                    request.getParameter("contrasena");

            if (usuario != null
                    && !usuario.isBlank()
                    && contrasena != null
                    && !contrasena.isBlank()) {

                UsuarioDAO.insertarUsuario(
                        usuario.trim(),
                        contrasena
                );
            }

        // ======================================
        // ELIMINAR USUARIO
        // ======================================
        } else if ("eliminar".equals(accion)) {

            try {

                int id =
                        Integer.parseInt(
                                request.getParameter("id")
                        );

                // ADMIN ID 1 ESTA PROTEGIDO
                if (id != 1) {

                    UsuarioDAO.eliminarUsuario(id);
                }

            } catch (NumberFormatException e) {

                System.out.println(
                        "ID de usuario no valido."
                );
            }

        // ======================================
        // ACTUALIZAR USUARIO
        // ======================================
        } else if ("actualizar".equals(accion)) {

            try {

                int id =
                        Integer.parseInt(
                                request.getParameter("id")
                        );

                String usuario =
                        request.getParameter("usuario");

                String contrasena =
                        request.getParameter("contrasena");

                // ADMIN ID 1 ESTA PROTEGIDO
                if (id != 1
                        && usuario != null
                        && !usuario.isBlank()
                        && contrasena != null
                        && !contrasena.isBlank()) {

                    UsuarioDAO.actualizarUsuario(
                            id,
                            usuario.trim(),
                            contrasena
                    );
                }

            } catch (NumberFormatException e) {

                System.out.println(
                        "ID de usuario no valido."
                );
            }
        }

        // VOLVER AL LISTADO
        response.sendRedirect(
                request.getContextPath()
                + "/usuarios"
        );
    }
}