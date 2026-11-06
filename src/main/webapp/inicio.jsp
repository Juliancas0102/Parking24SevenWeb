
<%@page contentType="text/html" pageEncoding="UTF-8" session="false"%>
<%@page import="jakarta.servlet.DispatcherType"%>
<%@page import="jakarta.servlet.http.HttpSession"%>

<%!
    private String escaparHtml(String texto) {

        if (texto == null) {
            return "";
        }

        return texto
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
%>

<%
    // COMPROBAR SI EXISTE UNA SESION AUTENTICADA
    HttpSession sesionActual =
            request.getSession(false);

    if (sesionActual == null
            || sesionActual.getAttribute("usuario") == null) {

        response.sendRedirect(
                request.getContextPath() + "/login"
        );

        return;
    }

    // EVITAR EL ACCESO DIRECTO AL ARCHIVO JSP
    if (request.getDispatcherType()
            != DispatcherType.FORWARD) {

        response.sendRedirect(
                request.getContextPath() + "/inicio"
        );

        return;
    }

    Object usuarioSesion =
            sesionActual.getAttribute("usuario");

    String nombreUsuario =
            escaparHtml(usuarioSesion.toString());

    boolean esAdmin =
            "ADMINISTRADOR".equalsIgnoreCase(
                    String.valueOf(sesionActual.getAttribute("rol"))
            );
%>

<!DOCTYPE html>
<html>

<head>
    <meta charset="UTF-8">
    <title>Inicio - Parking 24 Seven</title>
</head>

<body>

    <h1>Parking 24 Seven</h1>

    <h2>Bienvenido</h2>

    <p>
        Sesión iniciada como:
        <strong><%= nombreUsuario %></strong>
    </p>

    <p>
        Has ingresado correctamente al sistema.
    </p>

    <% if (esAdmin) { %>

        <p>
            <a href="${pageContext.request.contextPath}/usuarios">
                Administrar usuarios
            </a>
        </p>

    <% } %>

    <p>
        <a href="${pageContext.request.contextPath}/logout">
            Cerrar sesión
        </a>
    </p>

</body>
</html>
