/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
<@pp.dropOutputFile />
<@pp.changeOutputFile name="/org/apache/arrow/vector/complex/impl/UnionViewWriter.java" />

package org.apache.arrow.vector.complex.impl;

<#include "/@includes/vv_imports.ftl" />
import org.apache.arrow.vector.complex.writer.BaseWriter;
import org.apache.arrow.vector.types.Types.MinorType;
import org.apache.arrow.util.Preconditions;
import org.apache.arrow.vector.complex.impl.NullableStructWriterFactory;
import org.apache.arrow.vector.types.Types;

<#function is_timestamp_tz type>
  <#return type?starts_with("TimeStamp") && type?ends_with("TZ")>
</#function>

/*
 * This class is generated using freemarker and the ${.template_name} template.
 */
@SuppressWarnings("unused")
public class UnionViewWriter extends UnionWriter {

  public UnionViewWriter(UnionVector vector) {
    this(vector, NullableStructWriterFactory.getNullableStructWriterFactoryInstance());
  }

  public UnionViewWriter(UnionVector vector, NullableStructWriterFactory nullableStructWriterFactory) {
    super(vector, nullableStructWriterFactory);
  }

  @Override
  public StructWriter struct() {
    data.setType(idx(), MinorType.LISTVIEW);
    getListWriter().setPosition(idx());
    return getListWriter().struct();
  }

  <#list vv.types as type>
    <#list type.minor as minor>
      <#assign name = minor.class?cap_first />
      <#assign fields = minor.fields!type.fields />
      <#assign uncappedName = name?uncap_first/>
      <#assign friendlyType = (minor.friendlyType!minor.boxedType!type.boxedType) />
      <#if !minor.typeParams?? || minor.class?starts_with("Decimal") || is_timestamp_tz(minor.class) || minor.class == "Duration" || minor.class == "FixedSizeBinary">

  private ${name}Writer ${name?uncap_first}Writer;

  <#if minor.class?starts_with("Decimal") || is_timestamp_tz(minor.class) || minor.class == "Duration" || minor.class == "FixedSizeBinary">
  private ${name}Writer get${name}Writer(ArrowType arrowType) {
    if (${uncappedName}Writer == null) {
      ${uncappedName}Writer = new ${name}WriterImpl(data.get${name}Vector(arrowType));
      ${uncappedName}Writer.setPosition(idx());
      writers.add(${uncappedName}Writer);
    }
    return ${uncappedName}Writer;
  }

  public ${name}Writer as${name}(ArrowType arrowType) {
    data.setType(idx(), MinorType.${name?upper_case});
    return get${name}Writer(arrowType);
  }
  <#else>
  private ${name}Writer get${name}Writer() {
    if (${uncappedName}Writer == null) {
      ${uncappedName}Writer = new ${name}WriterImpl(data.get${name}Vector());
      ${uncappedName}Writer.setPosition(idx());
      writers.add(${uncappedName}Writer);
    }
    return ${uncappedName}Writer;
  }

  public ${name}Writer as${name}() {
    data.setType(idx(), MinorType.${name?upper_case});
    return get${name}Writer();
  }
  </#if>

  @Override
  public void write(${name}Holder holder) {
    data.setType(idx(), MinorType.${name?upper_case});
    <#if minor.class?starts_with("Decimal")>
        ArrowType arrowType = new ArrowType.Decimal(holder.precision, holder.scale, ${name}Holder.WIDTH * 8);
    get${name}Writer(arrowType).setPosition(idx());
    get${name}Writer(arrowType).write${name}(<#list fields as field>holder.${field.name}<#if field_has_next>, </#if></#list>, arrowType);
    <#elseif is_timestamp_tz(minor.class)>
        ArrowType.Timestamp arrowTypeWithoutTz = (ArrowType.Timestamp) MinorType.${name?upper_case?remove_ending("TZ")}.getType();
    ArrowType arrowType = new ArrowType.Timestamp(arrowTypeWithoutTz.getUnit(), holder.timezone);
    get${name}Writer(arrowType).setPosition(idx());
    get${name}Writer(arrowType).write(holder);
    <#elseif minor.class == "Duration">
        ArrowType arrowType = new ArrowType.Duration(holder.unit);
    get${name}Writer(arrowType).setPosition(idx());
    get${name}Writer(arrowType).write(holder);
    <#elseif minor.class == "FixedSizeBinary">
        ArrowType arrowType = new ArrowType.FixedSizeBinary(holder.byteWidth);
    get${name}Writer(arrowType).setPosition(idx());
    get${name}Writer(arrowType).write(holder);
    <#else>
    get${name}Writer().setPosition(idx());
    get${name}Writer().write${name}(<#list fields as field>holder.${field.name}<#if field_has_next>, </#if></#list>);
    </#if>
  }

  public void write${minor.class}(<#list fields as field>${field.type} ${field.name}<#if field_has_next>, </#if></#list><#if minor.class?starts_with("Decimal")>, ArrowType arrowType</#if>) {
    data.setType(idx(), MinorType.${name?upper_case});
    <#if minor.class?starts_with("Decimal")>
        get${name}Writer(arrowType).setPosition(idx());
    get${name}Writer(arrowType).write${name}(<#list fields as field>${field.name}<#if field_has_next>, </#if></#list>, arrowType);
    <#elseif is_timestamp_tz(minor.class)>
        ArrowType.Timestamp arrowTypeWithoutTz = (ArrowType.Timestamp) MinorType.${name?upper_case?remove_ending("TZ")}.getType();
    ArrowType arrowType = new ArrowType.Timestamp(arrowTypeWithoutTz.getUnit(), "UTC");
    get${name}Writer(arrowType).setPosition(idx());
    get${name}Writer(arrowType).write${name}(<#list fields as field>${field.name}<#if field_has_next>, </#if></#list>);
    <#elseif minor.class == "Duration" || minor.class == "FixedSizeBinary">
        // This is expected to throw. There's nothing more that we can do here since we can't infer any
        // sort of default unit for the Duration or a default width for the FixedSizeBinary types.
        ArrowType arrowType = MinorType.${name?upper_case}.getType();
    get${name}Writer(arrowType).setPosition(idx());
    get${name}Writer(arrowType).write${name}(<#list fields as field>${field.name}<#if field_has_next>, </#if></#list>);
    <#else>
    get${name}Writer().setPosition(idx());
    get${name}Writer().write${name}(<#list fields as field>${field.name}<#if field_has_next>, </#if></#list>);
    </#if>
  }
  <#if minor.class?starts_with("Decimal")>
  public void write${name}(${friendlyType} value) {
    data.setType(idx(), MinorType.${name?upper_case});
    ArrowType arrowType = new ArrowType.Decimal(value.precision(), value.scale(), ${name}Vector.TYPE_WIDTH * 8);
    get${name}Writer(arrowType).setPosition(idx());
    get${name}Writer(arrowType).write${name}(value);
  }

  public void writeBigEndianBytesTo${name}(byte[] value, ArrowType arrowType) {
    data.setType(idx(), MinorType.${name?upper_case});
    get${name}Writer(arrowType).setPosition(idx());
    get${name}Writer(arrowType).writeBigEndianBytesTo${name}(value, arrowType);
  }
  <#elseif minor.class?ends_with("VarBinary")>
  @Override
  public void write${minor.class}(byte[] value) {
    get${name}Writer().setPosition(idx());
    get${name}Writer().write${minor.class}(value);
  }

  @Override
  public void write${minor.class}(byte[] value, int offset, int length) {
    get${name}Writer().setPosition(idx());
    get${name}Writer().write${minor.class}(value, offset, length);
  }

  @Override
  public void write${minor.class}(ByteBuffer value) {
    get${name}Writer().setPosition(idx());
    get${name}Writer().write${minor.class}(value);
  }

  @Override
  public void write${minor.class}(ByteBuffer value, int offset, int length) {
    get${name}Writer().setPosition(idx());
    get${name}Writer().write${minor.class}(value, offset, length);
  }
  <#elseif minor.class?ends_with("VarChar")>
  @Override
  public void write${minor.class}(${friendlyType} value) {
    get${name}Writer().setPosition(idx());
    get${name}Writer().write${minor.class}(value);
  }

  @Override
  public void write${minor.class}(String value) {
    get${name}Writer().setPosition(idx());
    get${name}Writer().write${minor.class}(value);
  }
  </#if>
      </#if>
    </#list>
  </#list>

  <#list vv.types as type><#list type.minor as minor>
  <#assign lowerName = minor.class?uncap_first />
  <#if lowerName == "int" ><#assign lowerName = "integer" /></#if>
  <#assign upperName = minor.class?upper_case />
  <#assign capName = minor.class?cap_first />
  <#if !minor.typeParams?? || minor.class?starts_with("Decimal") || is_timestamp_tz(minor.class) || minor.class == "Duration" || minor.class == "FixedSizeBinary">

  @Override
  public ${capName}Writer ${lowerName}() {
    data.setType(idx(), MinorType.LISTVIEW);
    getListViewWriter().setPosition(idx());
    return getListViewWriter().${lowerName}();
  }
  </#if>
  </#list></#list>
}
