<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="java.util.List"%>

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
    List<String[]> usuarios =
            (List<String[]>) request.getAttribute("usuarios");

    String idEditar =
            (String) request.getAttribute("idEditar");

    String usuarioEditar =
            (String) request.getAttribute("usuarioEditar");
%>

<!DOCTYPE html>
<html>

<head>
    <meta charset="UTF-8">
    <title>Usuarios - Parking 24 Seven</title>
</head>

<body>

    <h1>Parking 24 Seven</h1>

    <p>
        Sesión iniciada como:
        <strong>${sessionScope.usuario}</strong>
    </p>

    <p>
        <a href="${pageContext.request.contextPath}/inicio">
            Inicio
        </a>

        |

        <a href="${pageContext.request.contextPath}/logout">
            Cerrar sesión
        </a>
    </p>

    <hr>

    <% if (idEditar != null
            && usuarioEditar != null
            && !"1".equals(idEditar)) { %>

        <h2>Editar usuario</h2>

        <form method="post"
              action="${pageContext.request.contextPath}/usuarios">

            <input type="hidden"
                   name="accion"
                   value="actualizar">

            <input type="hidden"
                   name="id"
                   value="<%= escaparHtml(idEditar) %>">

            <label for="usuario">
                Usuario:
            </label>

            <br>

            <input type="text"
                   id="usuario"
                   name="usuario"
                   value="<%= escaparHtml(usuarioEditar) %>"
                   required>

            <br><br>

            <label for="contrasena">
                Nueva contraseña:
            </label>

            <br>

            <input type="password"
                   id="contrasena"
                   name="contrasena"
                   required>

            <br><br>

            <button type="submit">
                Actualizar usuario
            </button>

            <a href="${pageContext.request.contextPath}/usuarios">
                Cancelar
            </a>

        </form>

    <% } else { %>

        <h2>Agregar nuevo usuario</h2>

        <form method="post"
              action="${pageContext.request.contextPath}/usuarios">

            <input type="hidden"
                   name="accion"
                   value="crear">

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
                Guardar usuario
            </button>

        </form>

    <% } %>

    <hr>

    <h2>Usuarios registrados</h2>

    <table border="1" cellpadding="8">

        <tr>
            <th>ID</th>
            <th>Usuario</th>
            <th>Acciones</th>
        </tr>

        <%
            if (usuarios != null) {

                for (String[] usuario : usuarios) {
        %>

        <tr>

            <td>
                <%= escaparHtml(usuario[0]) %>
            </td>

            <td>
                <%= escaparHtml(usuario[1]) %>
            </td>

            <td>

                <% if ("1".equals(usuario[0])) { %>

                    Protegido

                <% } else { %>

                    <a href="${pageContext.request.contextPath}/usuarios?editar=<%= escaparHtml(usuario[0]) %>">
                        Editar
                    </a>

                    <form method="post"
                          action="${pageContext.request.contextPath}/usuarios"
                          style="display:inline;">

                        <input type="hidden"
                               name="accion"
                               value="eliminar">

                        <input type="hidden"
                               name="id"
                               value="<%= escaparHtml(usuario[0]) %>">

                        <button type="submit">
                            Eliminar
                        </button>

                    </form>

                <% } %>

            </td>

        </tr>

        <%
                }
            }
        %>

    </table>

    <p>
        Total de usuarios:
        <%= usuarios != null ? usuarios.size() : 0 %>
    </p>

</body>

</html>