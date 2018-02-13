package com.elyria.elyrialink;

import com.google.common.collect.FluentIterable;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Messager;
import javax.lang.model.element.Modifier;
import javax.tools.Diagnostic;

/**
 * @author jungletian (tjsummery@gmail.com)
 */
public class QueryBinding {
  static final String ILLEGAL_KEY_PATTERN = "[^a-zA-Z0-9_]";
  private final String classPackage;
  private final String className;
  private final String targetClass;
  private final boolean isActivity;
  private final List<FieldBinding> fieldBindings = new ArrayList<>();
  private static final ClassName BUNDLE_CLASS_NAME = ClassName.get("android.os", "Bundle");
  private static final ClassName LINK_UTILS =
      ClassName.get("com.elyria.elyrialink.internal", "Utils");

  private static final ClassName TYPE_TOKEN =
      ClassName.get("com.elyria.elyrialink.internal", "TypeToken");

  private static final ClassName QUERY_BINDER =
      ClassName.get("com.elyria.elyrialink.internal", "QueryBinder");

  private static final ClassName EXCEPTION =
      ClassName.get("com.elyria.elyrialink", "IllegalElyriaLinkException");

  QueryBinding(String classPackage, String className, String targetClass, boolean isActivity) {
    this.classPackage = classPackage;
    this.className = className;
    this.targetClass = targetClass;
    this.isActivity = isActivity;
  }

  List<FieldBinding> allFields() {
    return fieldBindings;
  }

  void addFiled(FieldBinding fieldBinding) {
    fieldBindings.add(fieldBinding);
  }

  JavaFile brewJava() {
    TypeSpec.Builder queryBinderBuilder = TypeSpec.classBuilder(className)
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        .addSuperinterface(
            ParameterizedTypeName.get(QUERY_BINDER, ClassName.bestGuess(targetClass)))
        .addMethod(buildBindMethod())
        .addMethod(buildGetBundleMethod());
    return JavaFile.builder(classPackage, queryBinderBuilder.build())
        .addFileComment("Generated code from ElyriaLink. Do not modify!")
        .build();
  }

  private MethodSpec buildBindMethod() {
    boolean hasRequiredFields =
        !FluentIterable.from(fieldBindings).filter(field -> field.isRequired).isEmpty();
    MethodSpec.Builder binder = MethodSpec.methodBuilder("bind")
        .addModifiers(Modifier.PUBLIC)
        .addParameter(ClassName.bestGuess(targetClass), "target")
        .addAnnotation(Override.class)
        .addException(EXCEPTION)
        .addStatement("$T bundle = getBundle($L, $L)", BUNDLE_CLASS_NAME, "target",
            hasRequiredFields);

    binder.beginControlFlow("if (bundle != null)");

    FluentIterable.from(fieldBindings)
        .filter(field -> field.isRequired)
        .forEach(field -> binder.addStatement("$T.checkHasKey($L, $S)", LINK_UTILS, "bundle",
            field.queryName));
    binder.addCode("\n");
    FluentIterable.from(fieldBindings).forEach(filed -> {
      binder.addCode(buildBindCode(filed));
    });

    binder.endControlFlow();
    return binder.build();
  }

  private CodeBlock buildBindCode(FieldBinding fieldBinding) {
    CodeBlock.Builder codeBlock = CodeBlock.builder();
    String key = valueOrDefault(fieldBinding.queryName, fieldBinding.fieldName);
    if (!fieldBinding.isRequired) {
      codeBlock.addStatement("// $S is optional, check if present before get",
          fieldBinding.queryName);
      codeBlock.beginControlFlow("if ($L.containsKey($S))", "bundle", key);
    }

    switch (fieldBinding.filedType) {
      case STRING:
        codeBlock.addStatement("$L.$L = $L.getString($S)", "target", fieldBinding.fieldName,
            "bundle", key);
        break;
      case PRIMITIVE:
        bindPrimitiveType(codeBlock, fieldBinding, key);
        break;
      case JSON:
        String filedName = key.replaceAll(ILLEGAL_KEY_PATTERN, "_");
        String typeName = fieldBinding.typeMirror.toString();
        codeBlock.addStatement("$T<$L> typeToken_$L = new $T<$L>(){}", TYPE_TOKEN, typeName,
            filedName, TYPE_TOKEN, typeName);
        codeBlock.addStatement("$T jsonString_$L = $L.getString($S)", String.class, filedName,
            "bundle", key);
        codeBlock.addStatement("$L.$L = $T.fromJson(jsonString_$L, typeToken_$L.getType(), $S)",
            "target", fieldBinding.fieldName, LINK_UTILS, filedName, filedName, key);
        break;
      default:
        break;
    }

    if (!fieldBinding.isRequired) {
      codeBlock.endControlFlow();
    }
    return codeBlock.build();
  }

  private MethodSpec buildGetBundleMethod() {
    return MethodSpec.methodBuilder("getBundle")
        .addModifiers(Modifier.PRIVATE)
        .addException(EXCEPTION)
        .addParameter(ClassName.bestGuess(targetClass), "target")
        .addParameter(boolean.class, "hasRequiredFields")
        .addStatement("$T $L = $L", BUNDLE_CLASS_NAME, "bundle",
            "target" + (isActivity ? ".getIntent().getExtras()" : ".getArguments()"))
        .beginControlFlow("if (hasRequiredFields && $L == null) ", "bundle")
        .addStatement("throw new $T($S + target.getClass().getSimpleName())", EXCEPTION,
            "Bundle is missing in ")
        .endControlFlow()
        .addStatement("return bundle")
        .returns(BUNDLE_CLASS_NAME)
        .build();
  }

  private void bindPrimitiveType(CodeBlock.Builder codeBlock, FieldBinding fieldBinding,
      String valueKey) {
    String methodName = null;
    switch (fieldBinding.typeMirror.getKind()) {
      case BOOLEAN:
        methodName = "parseBoolean";
        break;
      case INT:
        methodName = "parseInt";
        break;
      case LONG:
        methodName = "parseLong";
        break;
      case FLOAT:
        methodName = "parseFloat";
        break;
      case DOUBLE:
        methodName = "parseDouble";
        break;
      default:
        break;
    }
    if (methodName != null) {
      if ("parseBoolean".equals(methodName)) {
        codeBlock.addStatement("$L.$L = $T.$L($L, $S)", "target", fieldBinding.fieldName,
            LINK_UTILS, methodName, "bundle", valueKey);
      } else {
        codeBlock.addStatement("$L.$L = $T.$L($L, $S, $L.$L)", "target", fieldBinding.fieldName,
            LINK_UTILS, methodName, "bundle", valueKey, "target", fieldBinding.fieldName);
      }
    }
  }

  public static boolean isBlank(CharSequence string) {
    return (string == null || string.toString().trim().length() == 0);
  }

  public static String valueOrDefault(String string, String defaultString) {
    return isBlank(string) ? defaultString : string;
  }
}
