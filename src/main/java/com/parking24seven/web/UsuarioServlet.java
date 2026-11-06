package com.parking24seven.web;

import com.parking24seven.ActivacionUtil;
import com.parking24seven.CorreoActivacion;
import com.parking24seven.UsuarioDAO;
import com.parking24seven.UsuarioDAO.ResultadoOperacion;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

@WebServlet(name = "UsuarioServlet", urlPatterns = {"/usuarios"})
public class UsuarioServlet extends HttpServlet {
    private static final SecureRandom RNG = new SecureRandom();

    private boolean verificarAcceso(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession s = req.getSession(false);
        if (s == null || s.getAttribute("usuarioId") == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return false;
        }
        if (!"ADMINISTRADOR".equalsIgnoreCase(String.valueOf(s.getAttribute("rol")))) {
            resp.sendRedirect(req.getContextPath() + "/inicio");
            return false;
        }
        return true;
    }

    private static String csrf(HttpSession s) {
        Object actual = s.getAttribute("csrf");
        if (actual instanceof String valor) return valor;
        byte[] bytes = new byte[32];
        RNG.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        s.setAttribute("csrf", token);
        return token;
    }

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!verificarAcceso(req, resp)) return;
        HttpSession s = req.getSession(false);
        req.setAttribute("csrf", csrf(s));
        List<String[]> usuarios = UsuarioDAO.obtenerUsuarios();
        req.setAttribute("usuarios", usuarios);
        req.setAttribute("auditoria", UsuarioDAO.obtenerAuditoria());
        req.setAttribute("smtpConfigurado", CorreoActivacion.estaConfigurado());
        String editar = req.getParameter("editar");
        if (editar != null && !"1".equals(editar)) {
            for (String[] u : usuarios) {
                if (u[0].equals(editar) && "1".equals(u[2])) {
                    req.setAttribute("usuarioEditar", u);
                    break;
                }
            }
        }
        String estado = req.getParameter("estado");
        String mensaje = switch (estado == null ? "" : estado) {
            case "creado" -> "Usuario creado correctamente.";
            case "correo_enviado" -> "Usuario creado. Se envio un enlace de activacion al correo.";
            case "correo_pendiente" -> "La cuenta sigue pendiente de activacion porque fallo el envio. Usa Reenviar activacion.";
            case "reenviado" -> "Se envio un nuevo enlace de activacion.";
            case "actualizado" -> "Usuario actualizado. Sus sesiones anteriores fueron revocadas.";
            case "desactivado" -> "Usuario desactivado y sesiones revocadas.";
            case "duplicado" -> "Nombre de usuario duplicado.";
            case "invalidos" -> "Datos invalidos. Verifica usuario (max. 50), rol, correo y contrasena.";
            case "smtp" -> "Para crear cuentas por correo configura SMTP. Puedes crear una cuenta local sin correo.";
            case "csrf" -> "Formulario caducado. Actualiza la pagina y vuelve a intentarlo.";
            case "protegido" -> "La cuenta principal de administrador esta protegida.";
            case "no_encontrado" -> "No existe un usuario activo que cumpla la operacion.";
            case "error" -> "Error en la operacion. Revisa la configuracion y el registro de Tomcat.";
            default -> null;
        };
        req.setAttribute("mensajeOperacion", mensaje);
        req.setAttribute("exito", "creado".equals(estado) || "correo_enviado".equals(estado)
                || "reenviado".equals(estado) || "actualizado".equals(estado)
                || "desactivado".equals(estado));
        resp.setHeader("Cache-Control", "no-store");
        req.getRequestDispatcher("/usuarios.jsp").forward(req, resp);
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!verificarAcceso(req, resp)) return;
        req.setCharacterEncoding("UTF-8");
        HttpSession s = req.getSession(false);
        String esperado = csrf(s);
        String recibido = req.getParameter("csrf");
        if (recibido == null || !MessageDigest.isEqual(esperado.getBytes(StandardCharsets.UTF_8),
                recibido.getBytes(StandardCharsets.UTF_8))) {
            redirigir(req, resp, "csrf");
            return;
        }
        Integer actorId = (Integer) s.getAttribute("usuarioId");
        String accion = req.getParameter("accion");
        String estado = "invalidos";
        if ("crear".equals(accion)) {
            String email = req.getParameter("email");
            boolean conCorreo = email != null && !email.isBlank();
            if (conCorreo && !CorreoActivacion.estaConfigurado()) estado = "smtp";
            else {
                String token = conCorreo ? ActivacionUtil.nuevoToken() : null;
                ResultadoOperacion resultado = UsuarioDAO.crearUsuario(
                        req.getParameter("usuario"), req.getParameter("contrasena"),
                        req.getParameter("rol"), email,
                        conCorreo ? ActivacionUtil.hash(token) : null, actorId);
                estado = estadoDeResultado(resultado, "creado");
                if (resultado == ResultadoOperacion.EXITOSO && conCorreo)
                    estado = CorreoActivacion.enviar(email.trim(), token)
                            ? "correo_enviado" : "correo_pendiente";
            }
        } else if ("actualizar".equals(accion)) {
            int id = parseId(req.getParameter("id"));
            ResultadoOperacion resultado = UsuarioDAO.actualizarUsuarioConRol(id,
                    req.getParameter("usuario"), req.getParameter("contrasena"),
                    req.getParameter("rol"), actorId);
            estado = estadoDeResultado(resultado, "actualizado");
            if (resultado == ResultadoOperacion.EXITOSO) RegistroSesiones.revocar(id);
        } else if ("eliminar".equals(accion)) {
            int id = parseId(req.getParameter("id"));
            ResultadoOperacion resultado = UsuarioDAO.desactivarUsuarioConResultado(id, actorId);
            estado = estadoDeResultado(resultado, "desactivado");
            if (resultado == ResultadoOperacion.EXITOSO) RegistroSesiones.revocar(id);
        } else if ("reenviar".equals(accion)) {
            int id = parseId(req.getParameter("id"));
            String email = UsuarioDAO.obtenerEmailPendiente(id);
            if (!CorreoActivacion.estaConfigurado()) estado = "smtp";
            else if (email == null) estado = "no_encontrado";
            else {
                String token = ActivacionUtil.nuevoToken();
                ResultadoOperacion resultado = UsuarioDAO.renovarTokenActivacion(
                        id, ActivacionUtil.hash(token), actorId);
                estado = estadoDeResultado(resultado, "reenviado");
                if (resultado == ResultadoOperacion.EXITOSO) {
                    RegistroSesiones.revocar(id);
                    estado = CorreoActivacion.enviar(email, token) ? "reenviado" : "correo_pendiente";
                }
            }
        }
        redirigir(req, resp, estado);
    }

    private static int parseId(String valor) {
        try { return Integer.parseInt(valor); }
        catch (NumberFormatException e) { return -1; }
    }

    private static void redirigir(HttpServletRequest req, HttpServletResponse resp, String estado)
            throws IOException {
        resp.sendRedirect(req.getContextPath() + "/usuarios?estado=" + estado);
    }

    private static String estadoDeResultado(ResultadoOperacion resultado, String exito) {
        return switch (resultado) {
            case EXITOSO -> exito;
            case DUPLICADO -> "duplicado";
            case DATOS_INVALIDOS -> "invalidos";
            case PROTEGIDO -> "protegido";
            case NO_ENCONTRADO -> "no_encontrado";
            case ERROR -> "error";
        };
    }
}
