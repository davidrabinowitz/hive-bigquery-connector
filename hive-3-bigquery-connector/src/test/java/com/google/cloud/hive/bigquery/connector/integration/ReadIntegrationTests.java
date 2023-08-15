/*
 * Copyright 2023 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.cloud.hive.bigquery.connector.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class ReadIntegrationTests extends ReadIntegrationTestsBase {

  @ParameterizedTest
  @MethodSource(READ_FORMAT)
  public void testReadAllTypesHive(String readDataFormat) throws IOException {
    // (Pacific/Honolulu, -10:00)
    String additionalCol = "cast(\"2000-01-01T00:23:45.123456-10\" as timestamp)";
    Object[] row = readAllTypes(readDataFormat, additionalCol);
    assertEquals(
        "2000-01-01T10:23:45.123456Z", // 'Z' == UTC
        Instant.from(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS VV")
                    .parse(row[18].toString()))
            .toString());
  }
}
