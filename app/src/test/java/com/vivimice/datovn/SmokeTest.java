/*
 * Copyright 2025 vivimice@gmail.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vivimice.datovn;

import static com.vivimice.datovn.action.MessageLevel.FATAL;
import static com.vivimice.datovn.action.MessageLevel.INFO;

import java.util.Objects;

import org.junit.jupiter.api.Test;

public class SmokeTest {

    @Test
    public void emptyTest() {
        new DatovnTester("empty").run().assertSuccess();
    }

    @Test
    public void emptyStageTest() {
        new DatovnTester("empty-stage").run().assertSuccess();
    }

    @Test
    public void emptyStageYmlTest() {
        new DatovnTester("empty-stage-yml").run().assertSuccess();
    }

    @Test
    public void singleStageTest() {
        new DatovnTester("single-stage").run()
            .assertSuccess()
            .assertHasMessage(INFO, "Hello, World!");
    }

    @Test
    public void duplicateNamesTest() {
        new DatovnTester("duplicate-names").run()
            .assertFailure()
            .assertWithErrors(1)
            .assertHasMessage(INFO, "Hello, World!")
            .assertHasMessage(INFO, "Hello, World! Again!")
            .assertHasMessage(FATAL, "Spec 'hello-world' already scheduled in the same stage. Duplicate specs with same names in the same stage are not allowed.");
    }

    @Test
    public void basicSkippingTest() {
        new DatovnTester("basic-skipping").run()
           .assertSuccess()
           .assertHasEvent("loadSketches:end", data -> Objects.equals(data.get("upToDate"), false))
           .assertHasEvent("writeSketches:start")
           .assertHasMessage(INFO, "Hello, World!");

        new DatovnTester("basic-skipping")
           .preserveActions()
           .run()
           .assertSuccess()
           .assertHasEvent("loadSketches:end", data -> Objects.equals(data.get("upToDate"), true))
           .assertNoEvent("writeSketches:start")
           .assertHasMessage(INFO, "Hello, World!");
    }

}
