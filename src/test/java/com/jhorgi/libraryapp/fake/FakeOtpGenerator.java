package com.jhorgi.libraryapp.fake;

import com.jhorgi.libraryapp.domain.port.out.OtpGeneratorPort;


public class FakeOtpGenerator implements OtpGeneratorPort {

    public static final String CODE = "123456";

    @Override
    public String generate() {
        return CODE;
    }
}
