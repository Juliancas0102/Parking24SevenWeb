package com.parking24seven;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class ConexionBD {

    public static Connection conectar() {

        Properties propiedades = new Properties();

        try (
            InputStream archivo =
                    ConexionBD.class
                            .getClassLoader()
                            .getResourceAsStream("db.properties")
        ) {

            if (archivo == null) {

                System.out.println(
                        "Error: no se encontró db.properties"
                );

                return null;
            }

            propiedades.load(archivo);

            String url =
                    propiedades.getProperty("db.url");

            String usuario =
                    propiedades.getProperty("db.usuario");

            String contrasena =
                    propiedades.getProperty("db.contrasena");

            Class.forName(
                    "com.mysql.cj.jdbc.Driver"
            );

            Connection conexion =
                    DriverManager.getConnection(
                            url,
                            usuario,
                            contrasena
                    );

            System.out.println(
                    "Conexion a MySQL realizada correctamente."
            );

            return conexion;

        } catch (ClassNotFoundException e) {

            System.out.println(
                    "Error: no se encontró el driver de MySQL: "
                    + e.getMessage()
            );

            return null;

        } catch (SQLException e) {

            System.out.println(
                    "Error de conexion: "
                    + e.getMessage()
            );

            return null;

        } catch (IOException e) {

            System.out.println(
                    "Error al leer db.properties: "
                    + e.getMessage()
            );

            return null;
        }
    }
}