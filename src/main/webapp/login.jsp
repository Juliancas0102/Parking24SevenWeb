<%@page contentType="text/html" pageEncoding="UTF-8"%>

<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Login - Parking 24 Seven</title>
</head>

<body>

    <h1>Parking 24 Seven</h1>

    <h2>Inicio de sesión</h2>

    <p style="color:red;">
        ${mensaje}
    </p>

    <form method="post"
          action="${pageContext.request.contextPath}/login">

        <label for="usuario">
            Usuario:
        </label>

        <br>

        <input type="text"
               id="usuario"
               name="usuario"
               required>

        <br><br>

        <label for="contrasena">
            Contraseña:
        </label>

        <br>

        <input type="password"
               id="contrasena"
               name="contrasena"
               required>

        <br><br>

        <button type="submit">
            Ingresar
        </button>

    </form>

</body>
</html>