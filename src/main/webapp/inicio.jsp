<%@page contentType="text/html" pageEncoding="UTF-8"%>

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
        <strong>${sessionScope.usuario}</strong>
    </p>

    <p>
        Has ingresado correctamente al sistema.
    </p>

    <%
        Object usuarioSesion =
                session.getAttribute("usuario");

        if (usuarioSesion != null
                && "admin".equalsIgnoreCase(
                        usuarioSesion.toString())) {
    %>

        <p>
            <a href="${pageContext.request.contextPath}/usuarios">
                Administrar usuarios
            </a>
        </p>

    <%
        }
    %>

    <p>
        <a href="${pageContext.request.contextPath}/logout">
            Cerrar sesión
        </a>
    </p>

</body>
</html>