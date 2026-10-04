# Parking 24 Seven

Aplicación web desarrollada en Java para la gestión de usuarios del sistema Parking 24 Seven.

## Tecnologías utilizadas

- Java
- Jakarta Servlets
- JSP
- Maven
- MySQL
- Apache Tomcat
- Apache NetBeans

## Funcionalidades principales

- Inicio de sesión de usuarios.
- Validación de credenciales.
- Manejo de sesiones.
- Cierre de sesión.
- Contraseñas almacenadas mediante hash.
- Administración de usuarios.
- Creación de usuarios.
- Edición de usuarios.
- Eliminación de usuarios.
- Protección del usuario administrador.
- Restricción del módulo administrativo para usuarios no autorizados.

## Base de datos

El proyecto incluye el archivo:

`parking24seven.sql`

Este script crea la base de datos `parking24seven` y la estructura de la tabla `usuarios`.

El archivo SQL contiene únicamente la estructura de la base de datos y no contiene usuarios ni contraseñas.

## Configuración de MySQL

Por seguridad, el archivo `db.properties` no se publica en GitHub.

Para ejecutar el proyecto se debe crear localmente:

`src/main/resources/db.properties`

Con una configuración similar a:

```properties
db.url=jdbc:mysql://127.0.0.1:3306/parking24seven?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC
db.usuario=root
db.contrasena=TU_CONTRASENA_MYSQL
