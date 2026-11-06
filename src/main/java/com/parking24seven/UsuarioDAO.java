package com.parking24seven;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Persistencia de usuarios y auditoria. Requiere migracion PAR_ADMI_002.sql. */
public final class UsuarioDAO {
    private UsuarioDAO() {}

    public enum ResultadoOperacion {
        EXITOSO, DUPLICADO, NO_ENCONTRADO, DATOS_INVALIDOS, PROTEGIDO, ERROR
    }

    public record EstadoSesion(int id, String nombre, String rol, boolean activo,
                               boolean activado, long version) {}

    public static void listarUsuarios() {
        for (String[] u : obtenerUsuarios()) {
            System.out.println("ID: " + u[0] + " | Usuario: " + u[1]);
        }
    }

    public static List<String[]> obtenerUsuarios() {
        List<String[]> lista = new ArrayList<>();
        String sql = "SELECT id, nomusu, activo, rol, email, activado FROM usuarios ORDER BY id";
        try (Connection c = ConexionBD.conectar()) {
            if (c == null) return lista;
            try (PreparedStatement p = c.prepareStatement(sql);
                 ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    lista.add(new String[] {
                        String.valueOf(r.getInt("id")), r.getString("nomusu"),
                        r.getBoolean("activo") ? "1" : "0", r.getString("rol"),
                        r.getString("email") == null ? "" : r.getString("email"),
                        r.getBoolean("activado") ? "1" : "0"
                    });
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al consultar usuarios: " + e.getMessage());
        }
        return lista;
    }

    public static EstadoSesion obtenerEstadoSesion(String nombre) {
        if (nombre == null || nombre.isBlank()) return null;
        String sql = "SELECT id, nomusu, rol, activo, activado, version_sesion "
                + "FROM usuarios WHERE nomusu = ?";
        try (Connection c = ConexionBD.conectar()) {
            if (c == null) return null;
            try (PreparedStatement p = c.prepareStatement(sql)) {
                p.setString(1, nombre);
                try (ResultSet r = p.executeQuery()) {
                    if (r.next()) return new EstadoSesion(r.getInt("id"), r.getString("nomusu"),
                            r.getString("rol"), r.getBoolean("activo"),
                            r.getBoolean("activado"), r.getLong("version_sesion"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al consultar sesion: " + e.getMessage());
        }
        return null;
    }

    public static String obtenerRolUsuario(String nombre) {
        EstadoSesion s = obtenerEstadoSesion(nombre);
        return s != null && s.activo() && s.activado() ? s.rol() : null;
    }

    public static boolean validarUsuario(String nombre, String contrasena) {
        if (nombre == null || nombre.isBlank() || contrasena == null || contrasena.isBlank())
            return false;
        String sql = "SELECT passusu FROM usuarios WHERE nomusu = ? AND activo = 1 AND activado = 1";
        try (Connection c = ConexionBD.conectar()) {
            if (c == null) return false;
            try (PreparedStatement p = c.prepareStatement(sql)) {
                p.setString(1, nombre);
                try (ResultSet r = p.executeQuery()) {
                    if (!r.next()) return false;
                    String hash = r.getString("passusu");
                    if (hash == null) return false;
                    if (hash.startsWith("pbkdf2$"))
                        return PasswordUtil.verificarPassword(contrasena, hash);
                    boolean coincide = MessageDigest.isEqual(
                            contrasena.getBytes(StandardCharsets.UTF_8),
                            hash.getBytes(StandardCharsets.UTF_8));
                    return coincide && migrarPasswordAntiguo(nombre, contrasena, hash);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al autenticar: " + e.getMessage());
            return false;
        }
    }

    private static boolean migrarPasswordAntiguo(String nombre, String pass, String hashAntiguo) {
        String sql = "UPDATE usuarios SET passusu = ? WHERE nomusu = ? AND passusu = ?";
        try (Connection c = ConexionBD.conectar()) {
            if (c == null) return false;
            try (PreparedStatement p = c.prepareStatement(sql)) {
                p.setString(1, PasswordUtil.hashPassword(pass));
                p.setString(2, nombre);
                p.setString(3, hashAntiguo);
                return p.executeUpdate() == 1;
            }
        } catch (SQLException e) {
            System.err.println("Error al migrar hash: " + e.getMessage());
            return false;
        }
    }

    private static boolean rolValido(String rol) {
        return "ADMINISTRADOR".equals(rol) || "OPERADOR".equals(rol)
                || "CONTADOR".equals(rol);
    }

    private static boolean nombreValido(String nombre) {
        return nombre != null && !nombre.isBlank() && nombre.trim().length() <= 50;
    }

    private static boolean emailValido(String email) {
        return email == null || email.isBlank() ||
                (email.length() <= 254 && email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"));
    }

    private static void auditar(Connection c, Integer actorId, Integer objetivoId,
                                String accion, String detalle) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(
                "INSERT INTO auditoria_usuarios(actor_id, objetivo_id, accion, detalle) VALUES (?, ?, ?, ?)")) {
            if (actorId == null) p.setNull(1, Types.INTEGER); else p.setInt(1, actorId);
            if (objetivoId == null) p.setNull(2, Types.INTEGER); else p.setInt(2, objetivoId);
            p.setString(3, accion);
            p.setString(4, detalle);
            p.executeUpdate();
        }
    }

    public static void insertarUsuario(String usuario, String contrasena) {
        System.out.println("Resultado al insertar: " + insertarUsuarioConResultado(usuario, contrasena));
    }

    public static ResultadoOperacion insertarUsuarioConResultado(String usuario, String contrasena) {
        return crearUsuario(usuario, contrasena, "OPERADOR", null, null, null);
    }

    public static ResultadoOperacion crearUsuario(String usuario, String contrasena,
            String rol, String email, String tokenHash, Integer actorId) {
        if (!nombreValido(usuario) || contrasena == null || contrasena.isBlank()
                || !rolValido(rol) || !emailValido(email)) return ResultadoOperacion.DATOS_INVALIDOS;
        String correo = email == null || email.isBlank() ? null : email.trim();
        if (correo != null && (tokenHash == null || tokenHash.length() != 64))
            return ResultadoOperacion.DATOS_INVALIDOS;
        String sql = "INSERT INTO usuarios (nomusu, passusu, rol, email, activado, "
                + "token_activacion_hash, token_activacion_expira) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = ConexionBD.conectar()) {
            if (c == null) return ResultadoOperacion.ERROR;
            c.setAutoCommit(false);
            try (PreparedStatement p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                p.setString(1, usuario.trim());
                p.setString(2, PasswordUtil.hashPassword(contrasena));
                p.setString(3, rol);
                p.setString(4, correo);
                p.setBoolean(5, correo == null);
                p.setString(6, correo == null ? null : tokenHash);
                if (correo == null) p.setNull(7, Types.TIMESTAMP);
                else p.setTimestamp(7, new Timestamp(System.currentTimeMillis() + 24L * 3600_000));
                if (p.executeUpdate() != 1) { c.rollback(); return ResultadoOperacion.ERROR; }
                try (ResultSet keys = p.getGeneratedKeys()) {
                    if (!keys.next()) { c.rollback(); return ResultadoOperacion.ERROR; }
                    int id = keys.getInt(1);
                    auditar(c, actorId, id, "CREAR", "Usuario=" + usuario.trim()
                            + "; rol=" + rol + "; activacion=" + (correo == null ? "local" : "correo"));
                }
                c.commit();
                return ResultadoOperacion.EXITOSO;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                if (e instanceof SQLException se && se.getErrorCode() == 1062)
                    return ResultadoOperacion.DUPLICADO;
                System.err.println("Error al crear usuario: " + e.getMessage());
                return ResultadoOperacion.ERROR;
            }
        } catch (SQLException e) {
            System.err.println("Error al crear usuario: " + e.getMessage());
            return ResultadoOperacion.ERROR;
        }
    }

    public static void actualizarUsuario(int id, String nombre, String contrasena) {
        System.out.println("Resultado al actualizar: " + actualizarUsuarioConResultado(id, nombre, contrasena));
    }

    public static ResultadoOperacion actualizarUsuarioConResultado(int id, String nombre, String contrasena) {
        return actualizarUsuarioConRol(id, nombre, contrasena, null, null);
    }

    public static ResultadoOperacion actualizarUsuarioConRol(int id, String nombre,
            String contrasena, String rol, Integer actorId) {
        if (id == 1) return ResultadoOperacion.PROTEGIDO;
        if (id <= 0 || !nombreValido(nombre) || (rol != null && !rolValido(rol)))
            return ResultadoOperacion.DATOS_INVALIDOS;
        boolean cambiarPassword = contrasena != null && !contrasena.isBlank();
        String sql = "UPDATE usuarios SET nomusu = ?, "
                + (cambiarPassword ? "passusu = ?, " : "")
                + (rol != null ? "rol = ?, " : "")
                + "version_sesion = version_sesion + 1 WHERE id = ? AND activo = 1";
        try (Connection c = ConexionBD.conectar()) {
            if (c == null) return ResultadoOperacion.ERROR;
            c.setAutoCommit(false);
            try (PreparedStatement p = c.prepareStatement(sql)) {
                int i = 1;
                p.setString(i++, nombre.trim());
                if (cambiarPassword) p.setString(i++, PasswordUtil.hashPassword(contrasena));
                if (rol != null) p.setString(i++, rol);
                p.setInt(i, id);
                if (p.executeUpdate() != 1) { c.rollback(); return ResultadoOperacion.NO_ENCONTRADO; }
                auditar(c, actorId, id, "ACTUALIZAR", "Usuario=" + nombre.trim()
                        + (rol == null ? "" : "; rol=" + rol)
                        + (cambiarPassword ? "; password_modificada=si" : ""));
                c.commit();
                return ResultadoOperacion.EXITOSO;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                if (e instanceof SQLException se && se.getErrorCode() == 1062)
                    return ResultadoOperacion.DUPLICADO;
                System.err.println("Error al actualizar usuario: " + e.getMessage());
                return ResultadoOperacion.ERROR;
            }
        } catch (SQLException e) {
            System.err.println("Error al actualizar usuario: " + e.getMessage());
            return ResultadoOperacion.ERROR;
        }
    }

    public static void eliminarUsuario(int id) {
        System.out.println("Resultado al desactivar: " + desactivarUsuarioConResultado(id));
    }

    public static ResultadoOperacion desactivarUsuarioConResultado(int id) {
        return desactivarUsuarioConResultado(id, null);
    }

    public static ResultadoOperacion desactivarUsuarioConResultado(int id, Integer actorId) {
        if (id == 1) return ResultadoOperacion.PROTEGIDO;
        if (id <= 0) return ResultadoOperacion.DATOS_INVALIDOS;
        try (Connection c = ConexionBD.conectar()) {
            if (c == null) return ResultadoOperacion.ERROR;
            c.setAutoCommit(false);
            try (PreparedStatement p = c.prepareStatement("UPDATE usuarios SET activo = 0, "
                    + "version_sesion = version_sesion + 1, token_activacion_hash = NULL, "
                    + "token_activacion_expira = NULL WHERE id = ? AND activo = 1")) {
                p.setInt(1, id);
                if (p.executeUpdate() != 1) { c.rollback(); return ResultadoOperacion.NO_ENCONTRADO; }
                auditar(c, actorId, id, "DESACTIVAR", "Desactivacion logica; sesiones revocadas");
                c.commit();
                return ResultadoOperacion.EXITOSO;
            } catch (SQLException e) {
                c.rollback();
                System.err.println("Error al desactivar usuario: " + e.getMessage());
                return ResultadoOperacion.ERROR;
            }
        } catch (SQLException e) {
            System.err.println("Error al desactivar usuario: " + e.getMessage());
            return ResultadoOperacion.ERROR;
        }
    }

    public static ResultadoOperacion renovarTokenActivacion(int id, String hash, Integer actorId) {
        if (id == 1) return ResultadoOperacion.PROTEGIDO;
        if (id <= 0 || hash == null || hash.length() != 64) return ResultadoOperacion.DATOS_INVALIDOS;
        try (Connection c = ConexionBD.conectar()) {
            if (c == null) return ResultadoOperacion.ERROR;
            c.setAutoCommit(false);
            try (PreparedStatement p = c.prepareStatement("UPDATE usuarios SET "
                    + "token_activacion_hash = ?, token_activacion_expira = ?, "
                    + "version_sesion = version_sesion + 1 "
                    + "WHERE id = ? AND activo = 1 AND activado = 0 AND email IS NOT NULL")) {
                p.setString(1, hash);
                p.setTimestamp(2, new Timestamp(System.currentTimeMillis() + 24L * 3600_000));
                p.setInt(3, id);
                if (p.executeUpdate() != 1) { c.rollback(); return ResultadoOperacion.NO_ENCONTRADO; }
                auditar(c, actorId, id, "REENVIAR_ACTIVACION", "Nuevo enlace de activacion generado");
                c.commit();
                return ResultadoOperacion.EXITOSO;
            } catch (SQLException e) {
                c.rollback();
                System.err.println("Error al renovar activacion: " + e.getMessage());
                return ResultadoOperacion.ERROR;
            }
        } catch (SQLException e) {
            System.err.println("Error al renovar activacion: " + e.getMessage());
            return ResultadoOperacion.ERROR;
        }
    }

    public static String obtenerEmailPendiente(int id) {
        String sql = "SELECT email FROM usuarios WHERE id = ? AND activo = 1 AND activado = 0";
        try (Connection c = ConexionBD.conectar()) {
            if (c == null) return null;
            try (PreparedStatement p = c.prepareStatement(sql)) {
                p.setInt(1, id);
                try (ResultSet r = p.executeQuery()) {
                    return r.next() ? r.getString(1) : null;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al consultar correo: " + e.getMessage());
            return null;
        }
    }

    public static ResultadoOperacion activarConToken(String hash) {
        if (hash == null || hash.length() != 64) return ResultadoOperacion.DATOS_INVALIDOS;
        try (Connection c = ConexionBD.conectar()) {
            if (c == null) return ResultadoOperacion.ERROR;
            c.setAutoCommit(false);
            String sql = "SELECT id FROM usuarios WHERE token_activacion_hash = ? "
                    + "AND token_activacion_expira > CURRENT_TIMESTAMP "
                    + "AND activo = 1 AND activado = 0 FOR UPDATE";
            try (PreparedStatement p = c.prepareStatement(sql)) {
                p.setString(1, hash);
                try (ResultSet r = p.executeQuery()) {
                    if (!r.next()) { c.rollback(); return ResultadoOperacion.NO_ENCONTRADO; }
                    int id = r.getInt(1);
                    try (PreparedStatement upd = c.prepareStatement("UPDATE usuarios SET "
                            + "activado = 1, token_activacion_hash = NULL, "
                            + "token_activacion_expira = NULL WHERE id = ?")) {
                        upd.setInt(1, id);
                        if (upd.executeUpdate() != 1) { c.rollback(); return ResultadoOperacion.ERROR; }
                    }
                    auditar(c, id, id, "ACTIVAR", "Cuenta activada mediante enlace de correo");
                    c.commit();
                    return ResultadoOperacion.EXITOSO;
                }
            } catch (SQLException e) {
                c.rollback();
                System.err.println("Error al activar cuenta: " + e.getMessage());
                return ResultadoOperacion.ERROR;
            }
        } catch (SQLException e) {
            System.err.println("Error al activar cuenta: " + e.getMessage());
            return ResultadoOperacion.ERROR;
        }
    }

    public static List<String[]> obtenerAuditoria() {
        List<String[]> lista = new ArrayList<>();
        String sql = "SELECT a.id, a.fecha, a.accion, a.actor_id, a.objetivo_id, a.detalle "
                + "FROM auditoria_usuarios a ORDER BY a.id DESC LIMIT 40";
        try (Connection c = ConexionBD.conectar()) {
            if (c == null) return lista;
            try (PreparedStatement p = c.prepareStatement(sql);
                 ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    lista.add(new String[] {String.valueOf(r.getLong(1)),
                        String.valueOf(r.getTimestamp(2)), r.getString(3),
                        r.getString(4) == null ? "-" : r.getString(4),
                        r.getString(5) == null ? "-" : r.getString(5),
                        r.getString(6) == null ? "" : r.getString(6)});
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al consultar auditoria: " + e.getMessage());
        }
        return lista;
    }
}
