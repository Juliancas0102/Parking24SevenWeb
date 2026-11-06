<%@page contentType="text/html" pageEncoding="UTF-8" session="false"%>
<%@page import="jakarta.servlet.DispatcherType"%>
<% if (request.getDispatcherType() != DispatcherType.FORWARD) {
    response.sendRedirect(request.getContextPath() + "/login"); return;
} %>
<%!
  private String h(String t) {
    if (t == null) return "";
    return t.replace("&", "&amp;").replace("<", "&lt;")
            .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
  }
%>
<% String token = (String) request.getAttribute("token");
   String mensaje = (String) request.getAttribute("mensaje"); %>
<!DOCTYPE html><html lang="es"><head><meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Activar cuenta - Parking 24 Seven</title></head><body>
<h1>Activación de cuenta</h1>
<% if (mensaje != null) { %><p role="status"><%= h(mensaje) %></p><% }
else if (token != null && !token.isBlank()) { %>
<p>Confirma la activación de tu cuenta. El enlace vence 24 horas después de su emisión.</p>
<form method="post" action="<%= request.getContextPath() %>/activar">
<input type="hidden" name="token" value="<%= h(token) %>">
<button type="submit">Activar cuenta</button></form>
<% } else { %><p>El enlace no es válido.</p><% } %>
<p><a href="<%= request.getContextPath() %>/login">Volver al inicio de sesión</a></p>
</body></html>
