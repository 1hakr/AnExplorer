/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.dworks.apps.anexplorer.misc;

import java.nio.charset.Charset;

/**
 * Convenient access to the most important built-in charsets.
 * @since 1.7
 */
public final class StandardCharsets {
  private StandardCharsets() {
  }

  /**
   * The ISO-8859-1 charset.
   */
  public static final Charset ISO_8859_1 = Charset.forName("ISO-8859-1");

  /**
   * The US-ASCII charset.
   */
  public static final Charset US_ASCII = Charset.forName("US-ASCII");

  /**
   * The UTF-8 charset.
   */
  public static final Charset UTF_8 = Charset.forName("UTF-8");

  /**
   * The UTF-16 charset.
   */
  public static final Charset UTF_16 = Charset.forName("UTF-16");

  /**
   * The UTF-16BE (big-endian) charset.
   */
  public static final Charset UTF_16BE = Charset.forName("UTF-16BE");

  /**
   * The UTF-16LE (little-endian) charset.
   */
  public static final Charset UTF_16LE = Charset.forName("UTF-16LE");
}
