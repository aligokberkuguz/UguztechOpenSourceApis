package com.uguztech.urlshortener.generator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class Base62CodeGeneratorTest {

    private final CodeGenerator generator = new Base62CodeGenerator();

    @Test
    void generateCodeShouldHaveMinimumLengthSix(){
        String code = generator.generate(1);
        assertEquals(6, code.length());
    }

    @Test
    void sameIdShouldAlwaysProduceSameCode(){
        String code1 = generator.generate(12345);
        String code2 = generator.generate(12345);
        assertEquals(code1, code2);
    }

    @Test
    void differentIdsShouldProduceDifferentCodes(){
        String code1 = generator.generate(12345);
        String code2 = generator.generate(54321);
        assertNotEquals(code1, code2);
    }

    @Test
    void zeroIdShouldNotThrowException(){
        assertDoesNotThrow(()  -> generator.generate(0));
    }

    @Test
    void largeIdShouldProduceLongerCode(){
        // 62^2 civarı bir değer, minimum 6 karakteri aşan bir kod bekliyorum
        String code = generator.generate(62L * 62 * 62 * 62 * 62 * 62);
        assertTrue(code.length() > 6);
    }
}
