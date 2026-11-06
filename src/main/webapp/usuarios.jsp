<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="java.util.List"%>
<%@page import="jakarta.servlet.DispatcherType"%>
<%
    if (request.getDispatcherType() != DispatcherType.FORWARD
            || request.getAttribute("usuarios") == null) {
        response.sendRedirect(request.getContextPath() + "/usuarios");
        return;
    }
%>
<%!
    private String h(String t) {
        if (t == null) return "";
        return t.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
%>
<%
    List<String[]> usuarios = (List<String[]>) request.getAttribute("usuarios");
    List<String[]> auditoria = (List<String[]>) request.getAttribute("auditoria");
    String[] editar = (String[]) request.getAttribute("usuarioEditar");
    String csrf = (String) request.getAttribute("csrf");
    String mensaje = (String) request.getAttribute("mensajeOperacion");
    boolean exito = Boolean.TRUE.equals(request.getAttribute("exito"));
    boolean smtp = Boolean.TRUE.equals(request.getAttribute("smtpConfigurado"));
%>
<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Usuarios - Parking 24 Seven</title>
  <style>
    body{font:16px system-ui,sans-serif;max-width:1100px;margin:24px auto;padding:0 16px;color:#202b3b}
    input,select,button{font:inherit;padding:8px;margin:5px 0} label{display:block;margin-top:12px}
    table{border-collapse:collapse;width:100%;margin:14px 0}td,th{border:1px solid #d2d7df;padding:9px;text-align:left}
    th{background:#f0f3f8} .mensaje{padding:12px;border-left:4px solid #b63838;background:#fff1f1}
    .ok{border-color:#2c7b42;background:#eefaf0} .sec{margin-top:28px;border-top:1px solid #ddd;padding-top:16px}
    .acciones form{display:inline-block} small{color:#555} .inactivo{color:#777}
  </style>
</head>
<body>
<h1>Parking 24 Seven — Administración de usuarios</h1>
<p>Sesión: <strong><%= h(String.valueOf(session.getAttribute("usuario"))) %></strong>
 | <a href="${pageContext.request.contextPath}/inicio">Inicio</a>
 | <a href="${pageContext.request.contextPath}/logout">Cerrar sesión</a></p>
<% if (mensaje != null) { %><p class="mensaje <%= exito ? "ok" : "" %>" role="status"><%= h(mensaje) %></p><% } %>
<section class="sec">
<h2><%= editar != null ? "Editar usuario" : "Crear usuario" %></h2>
<form method="post" action="${pageContext.request.contextPath}/usuarios">
  <input type="hidden" name="csrf" value="<%= h(csrf) %>">
  <input type="hidden" name="accion" value="<%= editar != null ? "actualizar" : "crear" %>">
  <% if (editar != null) { %><input type="hidden" name="id" value="<%= h(editar[0]) %>"><% } %>
  <label for="usuario">Nombre de usuario</label>
  <input id="usuario" name="usuario" maxlength="50" required value="<%= editar == null ? "" : h(editar[1]) %>">
  <label for="contrasena"><%= editar == null ? "Contraseña inicial" : "Nueva contraseña (dejar vacía para conservarla)" %></label>
  <input type="password" id="contrasena" name="contrasena" <%= editar == null ? "required" : "" %> autocomplete="new-password">
  <label for="rol">Rol</label>
  <select id="rol" name="rol" required>
    <% for (String r : new String[]{"OPERADOR", "CONTADOR", "ADMINISTRADOR"}) { %>
      <option value="<%= r %>" <%= editar != null && r.equals(editar[3]) ? "selected" : "" %>><%= r %></option>
    <% } %>
  </select>
  <% if (editar == null) { %>
    <label for="email">Correo electrónico para activación (opcional)</label>
    <input type="email" id="email" name="email" maxlength="254" placeholder="persona@empresa.com">
    <p><small>Sin correo: usuario local activo. Con correo: queda pendiente hasta activar el enlace enviado por SMTP.</small></p>
    <% if (!smtp) { %><p><small><strong>SMTP no configurado:</strong> por ahora solo se pueden crear usuarios locales sin correo.</small></p><% } %>
  <% } %>
  <p><button type="submit"><%= editar != null ? "Guardar cambios" : "Crear usuario" %></button>
  <% if (editar != null) { %><a href="${pageContext.request.contextPath}/usuarios">Cancelar</a><% } %></p>
</form>
</section>
<section class="sec">
<h2>Usuarios registrados</h2>
<table><thead><tr><th>ID</th><th>Usuario</th><th>Rol</th><th>Estado</th><th>Activación</th><th>Correo</th><th>Acciones</th></tr></thead><tbody>
<% for (String[] u : usuarios) { %>
<tr class="<%= "1".equals(u[2]) ? "" : "inactivo" %>">
  <td><%= h(u[0]) %></td><td><%= h(u[1]) %></td><td><%= h(u[3]) %></td>
  <td><%= "1".equals(u[2]) ? "Activo" : "Inactivo" %></td>
  <td><%= "1".equals(u[5]) ? "Activado" : "Pendiente" %></td><td><%= h(u[4]) %></td>
  <td class="acciones">
    <% if ("1".equals(u[0])) { %>Protegido
    <% } else if ("1".equals(u[2])) { %>
      <a href="${pageContext.request.contextPath}/usuarios?editar=<%= h(u[0]) %>">Editar</a>
      <form method="post" action="${pageContext.request.contextPath}/usuarios">
        <input type="hidden" name="csrf" value="<%= h(csrf) %>">
        <input type="hidden" name="accion" value="eliminar">
        <input type="hidden" name="id" value="<%= h(u[0]) %>">
        <button type="submit" onclick="return confirm('¿Desactivar este usuario y cerrar sus sesiones?')">Desactivar</button>
      </form>
      <% if ("0".equals(u[5]) && !u[4].isBlank()) { %>
      <form method="post" action="${pageContext.request.contextPath}/usuarios">
        <input type="hidden" name="csrf" value="<%= h(csrf) %>">
        <input type="hidden" name="accion" value="reenviar">
        <input type="hidden" name="id" value="<%= h(u[0]) %>">
        <button type="submit">Reenviar activación</button>
      </form>
      <% } %>
    <% } else { %>Sin acción<% } %>
  </td>
</tr>
<% } %>
</tbody></table>
<p>Total de usuarios: <%= usuarios.size() %></p>
</section>
<section class="sec">
<h2>Auditoría de usuarios (40 movimientos recientes)</h2>
<table><thead><tr><th>ID</th><th>Fecha</th><th>Acción</th><th>Actor ID</th><th>Usuario ID</th><th>Detalle</th></tr></thead><tbody>
<% if (auditoria != null) for (String[] a : auditoria) { %>
<tr><td><%= h(a[0]) %></td><td><%= h(a[1]) %></td><td><%= h(a[2]) %></td>
<td><%= h(a[3]) %></td><td><%= h(a[4]) %></td><td><%= h(a[5]) %></td></tr>
<% } %>
</tbody></table>
</section>
</body></html>
