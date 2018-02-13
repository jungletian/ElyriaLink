package com.elyria.elyrialink;

import javax.lang.model.type.TypeMirror;

/**
 * @author jungletian (tjsummery@gmail.com)
 */
public class FieldBinding {
  public final String fieldName;
  public final String queryName;
  //ArrayType, DeclaredType, ErrorType, ExecutableType, NoType, NullType, PrimitiveType, ReferenceType, TypeVariable, UnionType, WildcardType
  /**
   * Represents a type in the Java programming language. Types include primitive types, declared types (class and interface types),
   * array types, type variables, and the null type.
   * Also represented are wildcard type arguments, the signature and return types of executables,
   * and pseudo-types corresponding to packages and to the keyword void.
   *
   * Types should be compared using the utility methods in Types.There is no guarantee that any
   * particular type will always be represented by the same object.
   *
   *
   * To implement operations based on the class of an TypeMirror object, either use a visitor or use
   * the result of the getKind() method.
   * Using instanceof is not necessarily a reliable idiom for determining the effective class of an
   * object in this modeling hierarchy since an implementation may choose to have a single object
   * implement multiple TypeMirror subinterfaces.
   */
  public final TypeMirror typeMirror;
  public final boolean isRequired;
  public final FiledType filedType;

  FieldBinding(String fieldName, String queryName, TypeMirror typeMirror, boolean isRequired,
      FiledType filedType) {
    this.fieldName = fieldName;
    this.queryName = queryName;
    this.typeMirror = typeMirror;
    this.isRequired = isRequired;
    this.filedType = filedType;
  }

  enum FiledType {
    STRING,
    JSON,
    PRIMITIVE
  }
}
