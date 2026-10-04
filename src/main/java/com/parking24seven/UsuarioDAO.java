package com.parking24seven;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDAO {

    // ==========================================
    // LISTAR USUARIOS EN CONSOLA
    // ==========================================
    public static void listarUsuarios() {

        String sql =
                "SELECT id, nomusu FROM usuarios";

        try (
            Connection conexion = ConexionBD.conectar();
            Statement sentencia = conexion.createStatement();
            ResultSet resultado = sentencia.executeQuery(sql)
        ) {

            System.out.println(
                    "=== USUARIOS REGISTRADOS ==="
            );

            int cantidad = 0;

            while (resultado.next()) {

                cantidad++;

                System.out.println(
                        "ID: "
                        + resultado.getInt("id")
                        + " | Usuario: "
                        + resultado.getString("nomusu")
                );
            }

            System.out.println(
                    "Total de usuarios encontrados: "
                    + cantidad
            );

        } catch (SQLException e) {

            System.out.println(
                    "Error al consultar usuarios: "
                    + e.getMessage()
            );
        }
    }

    // ==========================================
    // INSERTAR USUARIO
    // ==========================================
    public static void insertarUsuario(
            String usuario,
            String contrasena) {

        String sql =
                "INSERT INTO usuarios "
                + "(nomusu, passusu) "
                + "VALUES (?, ?)";

        try (
            Connection conexion = ConexionBD.conectar();
            PreparedStatement sentencia =
                    conexion.prepareStatement(sql)
        ) {

            String contrasenaProtegida =
                    PasswordUtil.hashPassword(
                            contrasena
                    );

            sentencia.setString(
                    1,
                    usuario
            );

            sentencia.setString(
                    2,
                    contrasenaProtegida
            );

            int filas =
                    sentencia.executeUpdate();

            if (filas > 0) {

                System.out.println(
                        "Usuario insertado correctamente."
                );
            }

        } catch (SQLException e) {

            System.out.println(
                    "Error al insertar usuario: "
                    + e.getMessage()
            );
        }
    }

    // ==========================================
    // ACTUALIZAR USUARIO
    // ==========================================
    public static void actualizarUsuario(
            int id,
            String nuevoUsuario,
            String nuevaContrasena) {

        String sql =
                "UPDATE usuarios "
                + "SET nomusu = ?, passusu = ? "
                + "WHERE id = ?";

        try (
            Connection conexion = ConexionBD.conectar();
            PreparedStatement sentencia =
                    conexion.prepareStatement(sql)
        ) {

            String contrasenaProtegida =
                    PasswordUtil.hashPassword(
                            nuevaContrasena
                    );

            sentencia.setString(
                    1,
                    nuevoUsuario
            );

            sentencia.setString(
                    2,
                    contrasenaProtegida
            );

            sentencia.setInt(
                    3,
                    id
            );

            int filas =
                    sentencia.executeUpdate();

            if (filas > 0) {

                System.out.println(
                        "Usuario actualizado correctamente."
                );

            } else {

                System.out.println(
                        "No se encontro el usuario."
                );
            }

        } catch (SQLException e) {

            System.out.println(
                    "Error al actualizar usuario: "
                    + e.getMessage()
            );
        }
    }

    // ==========================================
    // ELIMINAR USUARIO
    // ==========================================
    public static void eliminarUsuario(int id) {

        String sql =
                "DELETE FROM usuarios "
                + "WHERE id = ?";

        try (
            Connection conexion = ConexionBD.conectar();
            PreparedStatement sentencia =
                    conexion.prepareStatement(sql)
        ) {

            sentencia.setInt(
                    1,
                    id
            );

            int filas =
                    sentencia.executeUpdate();

            if (filas > 0) {

                System.out.println(
                        "Usuario eliminado correctamente."
                );

            } else {

                System.out.println(
                        "No se encontro el usuario."
                );
            }

        } catch (SQLException e) {

            System.out.println(
                    "Error al eliminar usuario: "
                    + e.getMessage()
            );
        }
    }

    // ==========================================
    // OBTENER USUARIOS PARA LA PAGINA WEB
    // ==========================================
    public static List<String[]> obtenerUsuarios() {

        List<String[]> usuarios =
                new ArrayList<>();

        String sql =
                "SELECT id, nomusu "
                + "FROM usuarios";

        try (
            Connection conexion = ConexionBD.conectar();
            Statement sentencia =
                    conexion.createStatement();
            ResultSet resultado =
                    sentencia.executeQuery(sql)
        ) {

            while (resultado.next()) {

                String id =
                        String.valueOf(
                                resultado.getInt("id")
                        );

                String nombre =
                        resultado.getString("nomusu");

                usuarios.add(
                        new String[]{
                            id,
                            nombre
                        }
                );
            }

        } catch (SQLException e) {

            System.out.println(
                    "Error al obtener usuarios: "
                    + e.getMessage()
            );
        }

        return usuarios;
    }

    // ==========================================
    // VALIDAR INICIO DE SESION
    // ==========================================
    public static boolean validarUsuario(
            String usuario,
            String contrasena) {

        String sql =
                "SELECT passusu "
                + "FROM usuarios "
                + "WHERE nomusu = ?";

        String passwordGuardado = null;

        try (
            Connection conexion = ConexionBD.conectar();
            PreparedStatement sentencia =
                    conexion.prepareStatement(sql)
        ) {

            sentencia.setString(
                    1,
                    usuario
            );

            try (
                ResultSet resultado =
                        sentencia.executeQuery()
            ) {

                if (resultado.next()) {

                    passwordGuardado =
                            resultado.getString(
                                    "passusu"
                            );
                }
            }

        } catch (SQLException e) {

            System.out.println(
                    "Error al validar usuario: "
                    + e.getMessage()
            );

            return false;
        }

        // EL USUARIO NO EXISTE
        if (passwordGuardado == null) {

            return false;
        }

        // ======================================
        // CONTRASEÑA YA PROTEGIDA CON PBKDF2
        // ======================================
        if (passwordGuardado.startsWith(
                "pbkdf2$"
        )) {

            return PasswordUtil.verificarPassword(
                    contrasena,
                    passwordGuardado
            );
        }

        // ======================================
        // CONTRASEÑA ANTIGUA EN TEXTO PLANO
        // ======================================
        boolean coincide =
                MessageDigest.isEqual(
                        contrasena.getBytes(
                                StandardCharsets.UTF_8
                        ),
                        passwordGuardado.getBytes(
                                StandardCharsets.UTF_8
                        )
                );

        if (!coincide) {

            return false;
        }

        // SI LA CONTRASEÑA ANTIGUA ES CORRECTA,
        // LA MIGRAMOS AUTOMATICAMENTE A PBKDF2
        return migrarPasswordAntiguo(
                usuario,
                contrasena,
                passwordGuardado
        );
    }

    // ==========================================
    // MIGRAR CONTRASEÑA ANTIGUA A PBKDF2
    // ==========================================
    private static boolean migrarPasswordAntiguo(
            String usuario,
            String contrasena,
            String passwordAntiguo) {

        String nuevoHash =
                PasswordUtil.hashPassword(
                        contrasena
                );

        String sql =
                "UPDATE usuarios "
                + "SET passusu = ? "
                + "WHERE nomusu = ? "
                + "AND passusu = ?";

        try (
            Connection conexion = ConexionBD.conectar();
            PreparedStatement sentencia =
                    conexion.prepareStatement(sql)
        ) {

            sentencia.setString(
                    1,
                    nuevoHash
            );

            sentencia.setString(
                    2,
                    usuario
            );

            sentencia.setString(
                    3,
                    passwordAntiguo
            );

            int filas =
                    sentencia.executeUpdate();

            if (filas > 0) {

                System.out.println(
                        "Contraseña antigua migrada "
                        + "correctamente a PBKDF2."
                );

                return true;
            }

        } catch (SQLException e) {

            System.out.println(
                    "Error al migrar contraseña: "
                    + e.getMessage()
            );
        }

        return false;
    }
}