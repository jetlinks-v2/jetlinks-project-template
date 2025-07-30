package org.jetlinks.pro.auth;

import lombok.SneakyThrows;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

public class RsaGenerator {
    @SneakyThrows
    public static void main(String[] args) {
        KeyPairGenerator keyPairGen;
        keyPairGen = KeyPairGenerator.getInstance("RSA");
        keyPairGen.initialize(512);
        KeyPair keyPair = keyPairGen.generateKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

        System.out.println("------public key-------");
        System.out.println(Base64.getEncoder().encodeToString(publicKey.getEncoded()));
        System.out.println("------private key-------");
        System.out.println(Base64.getEncoder().encodeToString(privateKey.getEncoded()));

    }
}
