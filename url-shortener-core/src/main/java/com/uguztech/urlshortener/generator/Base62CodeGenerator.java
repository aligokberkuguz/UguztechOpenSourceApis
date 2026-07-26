package com.uguztech.urlshortener.generator;

public class Base62CodeGenerator implements CodeGenerator {

    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int BASE = ALPHABET.length();
    private static final int MIN_LENGTH = 6;

    @Override
    public String generate(long id) {
        StringBuilder sb = new StringBuilder();

        if (id == 0){
            sb.append(ALPHABET.charAt(0));
        }

        while (id > 0){
            int remainder = (int) (id % BASE);
            sb.append(ALPHABET.charAt(remainder));
            id /= BASE;
        }

        sb.reverse();

        while (sb.length() < MIN_LENGTH){
            sb.insert(0, ALPHABET.charAt(0));
        }

        return sb.toString();
    }
}