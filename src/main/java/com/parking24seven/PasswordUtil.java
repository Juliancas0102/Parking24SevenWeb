package com.parking24seven;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class PasswordUtil {

    private static final int ITERACIONES = 210000;
    private static final int TAMANO_SALT = 16;
    private static final int TAMANO_HASH = 256;

    private PasswordUtil() {
    }

    public static String hashPassword(String password) {

        try {

            SecureRandom random = new SecureRandom();

            byte[] salt = new byte[TAMANO_SALT];
            random.nextBytes(salt);

            PBEKeySpec spec = new PBEKeySpec(
                    password.toCharArray(),
                    salt,
                    ITERACIONES,
                    TAMANO_HASH
            );

            SecretKeyFactory factory =
                    SecretKeyFactory.getInstance(
                            "PBKDF2WithHmacSHA256"
                    );

            byte[] hash =
                    factory.generateSecret(spec).getEncoded();

            spec.clearPassword();

            return "pbkdf2$"
                    + ITERACIONES
                    + "$"
                    + Base64.getEncoder().encodeToString(salt)
                    + "$"
                    + Base64.getEncoder().encodeToString(hash);

        } catch (Exception e) {

            throw new RuntimeException(
                    "Error al proteger la contraseña.",
                    e
            );
        }
    }

    public static boolean verificarPassword(
            String password,
            String valorGuardado) {

        try {

            if (valorGuardado == null
                    || !valorGuardado.startsWith("pbkdf2$")) {
                return false;
            }

            String[] partes = valorGuardado.split("\\$");

            if (partes.length != 4) {
                return false;
            }

            int iteraciones =
                    Integer.parseInt(partes[1]);

            byte[] salt =
                    Base64.getDecoder().decode(partes[2]);

            byte[] hashGuardado =
                    Base64.getDecoder().decode(partes[3]);

            PBEKeySpec spec = new PBEKeySpec(
                    password.toCharArray(),
                    salt,
                    iteraciones,
                    hashGuardado.length * 8
            );

            SecretKeyFactory factory =
                    SecretKeyFactory.getInstance(
                            "PBKDF2WithHmacSHA256"
                    );

            byte[] hashCalculado =
                    factory.generateSecret(spec).getEncoded();

            spec.clearPassword();

            return MessageDigest.isEqual(
                    hashGuardado,
                    hashCalculado
            );

        } catch (InvalidKeySpecException
                 | java.security.NoSuchAlgorithmException
                 | IllegalArgumentException e) {

            return false;
        }
    }
}