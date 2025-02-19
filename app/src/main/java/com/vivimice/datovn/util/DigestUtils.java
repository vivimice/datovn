/**
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
package com.vivimice.datovn.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class DigestUtils {

    public static String sha256Hex(String input) {
        return hexDigest("sha-256", input);
    }

    private static String hexDigest(String algorithm, String input) {
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("sha256");
        } catch (NoSuchAlgorithmException e) {
            // should never happen
            throw new AssertionError(e);
        }
        messageDigest.update(input.getBytes(StandardCharsets.UTF_8));
        byte[] digest = messageDigest.digest();
        return HexFormat.of().formatHex(digest);
    }

    public static void main(String[] args) {
        System.out.println(sha256Hex("Hello, World!"));
    }

}
