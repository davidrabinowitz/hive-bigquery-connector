/*
 * Copyright 2022 Google Inc. All Rights Reserved.
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
package com.google.cloud.hive.bigquery.connector.utils.bq;

import com.google.cloud.bigquery.*;
import com.google.cloud.hive.bigquery.connector.Constants;
import java.util.ArrayList;
import java.util.List;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.metastore.api.StorageDescriptor;
import org.apache.hadoop.hive.serde2.objectinspector.*;
import org.apache.hadoop.hive.serde2.typeinfo.*;
import repackaged.by.hivebqconnector.com.google.common.base.Preconditions;
import repackaged.by.hivebqconnector.com.google.common.collect.ImmutableMap;

/** Converts Hive Schema to BigQuery schema. */
public class BigQuerySchemaConverter {

  private static final ImmutableMap<PrimitiveObjectInspector.PrimitiveCategory, StandardSQLTypeName>
      hiveToBigQueryTypes =
          new ImmutableMap.Builder<
                  PrimitiveObjectInspector.PrimitiveCategory, StandardSQLTypeName>()
              .put(PrimitiveObjectInspector.PrimitiveCategory.CHAR, StandardSQLTypeName.STRING)
              .put(PrimitiveObjectInspector.PrimitiveCategory.VARCHAR, StandardSQLTypeName.STRING)
              .put(PrimitiveObjectInspector.PrimitiveCategory.STRING, StandardSQLTypeName.STRING)
              .put(
                  PrimitiveObjectInspector.PrimitiveCategory.BYTE,
                  StandardSQLTypeName.INT64) // Tiny Int
              .put(
                  PrimitiveObjectInspector.PrimitiveCategory.SHORT,
                  StandardSQLTypeName.INT64) // Small Int
              .put(
                  PrimitiveObjectInspector.PrimitiveCategory.INT,
                  StandardSQLTypeName.INT64) // Regular Int
              .put(
                  PrimitiveObjectInspector.PrimitiveCategory.LONG,
                  StandardSQLTypeName.INT64) // Big Int
              .put(PrimitiveObjectInspector.PrimitiveCategory.FLOAT, StandardSQLTypeName.FLOAT64)
              .put(PrimitiveObjectInspector.PrimitiveCategory.DOUBLE, StandardSQLTypeName.FLOAT64)
              .put(PrimitiveObjectInspector.PrimitiveCategory.DECIMAL, StandardSQLTypeName.NUMERIC)
              .put(PrimitiveObjectInspector.PrimitiveCategory.BOOLEAN, StandardSQLTypeName.BOOL)
              .put(PrimitiveObjectInspector.PrimitiveCategory.DATE, StandardSQLTypeName.DATE)
              .put(
                  PrimitiveObjectInspector.PrimitiveCategory.TIMESTAMP,
                  StandardSQLTypeName.TIMESTAMP)
              .put(PrimitiveObjectInspector.PrimitiveCategory.BINARY, StandardSQLTypeName.BYTES)
              .build();

  public static Schema toBigQuerySchema(StorageDescriptor sd) {
    List<Field> bigQueryFields = new ArrayList<>();
    for (FieldSchema hiveField : sd.getCols()) {
      TypeInfo typeInfo = TypeInfoUtils.getTypeInfoFromTypeString(hiveField.getType());
      bigQueryFields.add(buildBigQueryField(hiveField.getName(), typeInfo));
    }
    return Schema.of(bigQueryFields);
  }

  private static Field buildBigQueryField(String fieldName, TypeInfo typeInfo) {
    Field.Builder bigQueryFieldBuilder;

    Field.Mode mode = null;
    if (typeInfo instanceof ListTypeInfo) {
      typeInfo = ((ListTypeInfo) typeInfo).getListElementTypeInfo();
      mode = Field.Mode.REPEATED;
    }

    if (typeInfo instanceof StructTypeInfo) {
      List<TypeInfo> subFieldTypeInfos = ((StructTypeInfo) typeInfo).getAllStructFieldTypeInfos();
      List<String> subFieldNames = ((StructTypeInfo) typeInfo).getAllStructFieldNames();
      List<Field> bigQuerySubFields = new ArrayList<>();
      for (int i = 0; i < subFieldNames.size(); i++) {
        Field bigQuerySubField = buildBigQueryField(subFieldNames.get(i), subFieldTypeInfos.get(i));
        bigQuerySubFields.add(bigQuerySubField);
      }
      bigQueryFieldBuilder =
          Field.newBuilder(fieldName, LegacySQLTypeName.RECORD, FieldList.of(bigQuerySubFields));
    } else {
      StandardSQLTypeName fieldType = toBigQueryFieldType(typeInfo);
      bigQueryFieldBuilder = Field.newBuilder(fieldName, fieldType);
    }

    bigQueryFieldBuilder.setMode(mode);
    return bigQueryFieldBuilder.build();
  }

  private static StandardSQLTypeName toBigQueryFieldType(TypeInfo typeInfo) {
    if (typeInfo.getCategory() == ObjectInspector.Category.MAP) {
      throw new IllegalArgumentException(Constants.MAPTYPE_ERROR_MESSAGE);
    } else if (typeInfo instanceof PrimitiveTypeInfo) {
      PrimitiveObjectInspector.PrimitiveCategory category =
          ((PrimitiveTypeInfo) typeInfo).getPrimitiveCategory();
      return Preconditions.checkNotNull(
          hiveToBigQueryTypes.get(category),
          new IllegalStateException("Unexpected type: " + category.name()));
    } else {
      throw new IllegalStateException("Unexpected type: " + typeInfo.getCategory().name());
    }
  }
}
