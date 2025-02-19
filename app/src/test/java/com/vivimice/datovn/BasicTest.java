package com.vivimice.datovn;

import org.junit.jupiter.api.Test;

public class BasicTest {

    @Test
    public void emptyTest() {
        new DatovnTester("empty").run().assertSuccess();
    }

}
