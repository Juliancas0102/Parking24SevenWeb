package com.parking24seven;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * SMTP STARTTLS con configuracion externa al proyecto y al WAR.
 * Ruta predeterminada: <user.home>/.parking24seven/smtp.properties.
 * Puede personalizarse con -Dp24.smtp.config=... o P24_SMTP_CONFIG.
 */
public final class CorreoActivacion {
    private CorreoActivacion() {}

    private static Path rutaConfiguracion() {
        String ruta = System.getProperty("p24.smtp.config");
        if (ruta == null || ruta.isBlank()) ruta = System.getenv("P24_SMTP_CONFIG");
        if (ruta != null && !ruta.isBlank()) return Path.of(ruta);
        return Path.of(System.getProperty("user.home"), ".parking24seven", "smtp.properties");
    }

    private static Properties configuracion() {
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(rutaConfiguracion())) {
            p.load(in);
        } catch (IOException | SecurityException | IllegalArgumentException e) {
            // No registrar la ruta ni los secretos; la UI mostrara SMTP no configurado.
        }
        // Se mantienen las variables de entorno para instalaciones avanzadas.
        String[][] vars = {
            {"P24_SMTP_HOST", "smtp.host"}, {"P24_SMTP_PORT", "smtp.port"},
            {"P24_SMTP_USER", "smtp.usuario"}, {"P24_SMTP_PASSWORD", "smtp.contrasena"},
            {"P24_SMTP_FROM", "smtp.remitente"}, {"P24_BASE_URL", "app.url"}
        };
        for (String[] v : vars) {
            String valor = System.getenv(v[0]);
            if (valor != null && !valor.isBlank()) p.setProperty(v[1], valor);
        }
        return p;
    }

    public static boolean estaConfigurado() {
        Properties p = configuracion();
        for (String k : new String[]{"smtp.host", "smtp.port", "smtp.usuario",
                "smtp.contrasena", "smtp.remitente", "app.url"}) {
            if (p.getProperty(k) == null || p.getProperty(k).isBlank()) return false;
        }
        try {
            int puerto = Integer.parseInt(p.getProperty("smtp.port"));
            return puerto == 587 || puerto == 465;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean enviar(String destino, String token) {
        if (!estaConfigurado() || destino == null || token == null) return false;
        Properties cfg = configuracion();
        String usuario = cfg.getProperty("smtp.usuario");
        String password = cfg.getProperty("smtp.contrasena");
        String puerto = cfg.getProperty("smtp.port");
        Properties mail = new Properties();
        mail.setProperty("mail.smtp.host", cfg.getProperty("smtp.host"));
        mail.setProperty("mail.smtp.port", puerto);
        mail.setProperty("mail.smtp.auth", "true");
        mail.setProperty("mail.smtp.ssl.checkserveridentity", "true");
        mail.setProperty("mail.smtp.connectiontimeout", "10000");
        mail.setProperty("mail.smtp.timeout", "10000");
        mail.setProperty("mail.smtp.writetimeout", "10000");
        if ("465".equals(puerto)) mail.setProperty("mail.smtp.ssl.enable", "true");
        else {
            mail.setProperty("mail.smtp.starttls.enable", "true");
            mail.setProperty("mail.smtp.starttls.required", "true");
        }
        Session sesion = Session.getInstance(mail, new Authenticator() {
            @Override protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(usuario, password);
            }
        });
        try {
            String base = cfg.getProperty("app.url").replaceAll("/+$", "");
            if (!base.startsWith("http://") && !base.startsWith("https://")) return false;
            String enlace = base + "/activar?token=" + token;
            MimeMessage mensaje = new MimeMessage(sesion);
            mensaje.setFrom(new InternetAddress(cfg.getProperty("smtp.remitente")));
            mensaje.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destino, true));
            mensaje.setSubject("Activacion de cuenta - Parking 24 Seven", StandardCharsets.UTF_8.name());
            mensaje.setText("Se ha creado una cuenta para Parking 24 Seven.\n\n"
                    + "Para activarla, abre este enlace (vigencia: 24 horas):\n"
                    + enlace + "\n\nSi no esperabas este mensaje, ignoralo.\n",
                    StandardCharsets.UTF_8.name());
            Transport.send(mensaje);
            return true;
        } catch (MessagingException e) {
            // Nunca registrar token ni contrasena SMTP.
            System.err.println("No fue posible enviar correo de activacion: " + e.getClass().getSimpleName());
            return false;
        }
    }
}
