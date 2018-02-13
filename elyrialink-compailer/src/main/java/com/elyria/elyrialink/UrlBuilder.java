package com.elyria.elyrialink;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.Modifier;
import org.apache.commons.lang.StringUtils;

import static com.elyria.elyrialink.QueryBinding.ILLEGAL_KEY_PATTERN;

/**
 * @author jungletian (tjsummery@gmail.com)
 */

public class UrlBuilder {
  static final String PACKAGE_NAME = "com.elyria.elyrialink.builders";
  private static final ClassName URI_NAME = ClassName.get("android.net", "Uri");
  private static final ClassName URI_BUILDER_NAME = ClassName.get("android.net", "Uri", "Builder");
  private static final ClassName NULLABLE_NAME =
      ClassName.get("android.support.annotation", "Nullable");
  private static final ClassName NON_NULL_NAME =
      ClassName.get("android.support.annotation", "NonNull");

  private static final ClassName TYPE_TOKEN =
      ClassName.get("com.elyria.elyrialink.internal", "TypeToken");

  private static final ClassName LINK_UTILS =
      ClassName.get("com.elyria.elyrialink.internal", "Utils");
  private static final ClassName ARRAY_MAP = ClassName.get("android.support.v4.util", "ArrayMap");

  private static final String BUILDER_NAME_SUFFIX = "LinkBuilder";
  private final String elyrialink;
  private final List<FieldBinding> fieldList = Lists.newArrayList();
  private static final Set<String> IGNORE_LIST = new LinkedHashSet<>();

  static {
    IGNORE_LIST.add("com.elyria.elyrialink.EXTRA_ELYRIA_LINK");
    IGNORE_LIST.add("com.elyria.elyrialink.EXTRA_ELYRIA_LINK_REFERRER");
    IGNORE_LIST.add("com.elyria.elyrialink.EXTRA_ELYRIA_LINK_REDIRECT");
  }

  UrlBuilder(String elyrialink, List<FieldBinding> fieldList) {
    this.elyrialink = elyrialink;
    for (FieldBinding fieldBinding : fieldList) {
      if (!IGNORE_LIST.contains(fieldBinding.queryName)) {
        this.fieldList.add(fieldBinding);
      }
    }
  }

  JavaFile brewJava() {
    String className = createClassName(elyrialink);
    TypeSpec.Builder classBuilder = TypeSpec.classBuilder(className)
        .addModifiers(Modifier.PUBLIC)
        .addMethod(MethodSpec.constructorBuilder()
            .addParameter(String.class, "baseLink")
            .addStatement("this.$L = $L", "baseLink", "baseLink")
            .build());

    FieldSpec urlField =
        FieldSpec.builder(String.class, "baseLink", Modifier.PRIVATE, Modifier.FINAL).build();

    classBuilder.addField(urlField);
    classBuilder.addField(ParameterizedTypeName.get(Map.class, String.class, String.class),
        "nameAndValues", Modifier.PRIVATE);
    classBuilder.addFields(buildQueryFields());
    classBuilder.addMethod(createPutQueryMethod(ClassName.get(PACKAGE_NAME, className)));
    classBuilder.addMethods(buildQuerySetterMethods(ClassName.get(PACKAGE_NAME, className)));
    classBuilder.addMethod(createBuildMethod());
    classBuilder.addMethod(createBuildUriMethod());

    return JavaFile.builder(PACKAGE_NAME, classBuilder.build())
        .addFileComment("Generated code from ElyriaLink. Do not modify!")
        .build();
  }

  private Iterable<FieldSpec> buildQueryFields() {
    return FluentIterable.from(fieldList).transform(new FieldSpecFunc()).toList();
  }

  private Iterable<MethodSpec> buildQuerySetterMethods(ClassName className) {
    return FluentIterable.from(fieldList).transform(new FieldMethodFunc(className)).toList();
  }

  private MethodSpec createPutQueryMethod(ClassName className) {
    return MethodSpec.methodBuilder("addQueryParameter")
        .addModifiers(Modifier.PUBLIC)
        .addParameter(
            ParameterSpec.builder(String.class, "name").addAnnotation(NON_NULL_NAME).build())
        .addParameter(
            ParameterSpec.builder(String.class, "value").addAnnotation(NON_NULL_NAME).build())
        .beginControlFlow("if (name == null)")
        .addStatement("throw new $T($S)", NullPointerException.class, "name == null")
        .endControlFlow()
        .beginControlFlow("if (value == null)")
        .addStatement("throw new $T($S)", NullPointerException.class, "value == null")
        .endControlFlow()
        .beginControlFlow("if (nameAndValues == null)")
        .addStatement("nameAndValues = new $T<>()", ARRAY_MAP)
        .endControlFlow()
        .addStatement("nameAndValues.put(name, value)")
        .addStatement("return this")
        .returns(className)
        .build();
  }

  private MethodSpec createBuildMethod() {
    MethodSpec.Builder methodBuilder =
        MethodSpec.methodBuilder("build").addModifiers(Modifier.PUBLIC).returns(String.class);
    methodBuilder.addStatement("return toUri().toString()");
    return methodBuilder.build();
  }

  private MethodSpec createBuildUriMethod() {
    MethodSpec.Builder methodBuilder =
        MethodSpec.methodBuilder("toUri").addModifiers(Modifier.PUBLIC).returns(URI_NAME);
    boolean hasRequiredFiled =
        !FluentIterable.from(fieldList).filter(new Predicate<FieldBinding>() {
          @Override public boolean apply(FieldBinding input) {
            return input.isRequired;
          }
        }).isEmpty();
    if (hasRequiredFiled) {
      methodBuilder.addStatement("$T missing = $S", String.class, "");
      for (FieldBinding field : fieldList) {
        if (field.isRequired) {
          methodBuilder.beginControlFlow("if ($L == null)", field.fieldName);
          methodBuilder.addStatement("missing += $S", " " + field.fieldName);
          methodBuilder.endControlFlow();
        }
      }
      methodBuilder.beginControlFlow("if (!missing.isEmpty())");
      methodBuilder.addStatement("throw new $T($S + missing)", IllegalStateException.class,
          "Missing required properties:");
      methodBuilder.endControlFlow();
    }
    methodBuilder.addStatement("$T uriBuilder = Uri.parse(baseLink).buildUpon()", URI_BUILDER_NAME);
    for (FieldBinding field : fieldList) {
      if (!field.isRequired) {
        methodBuilder.beginControlFlow("if ($L != null)", field.fieldName);
      }
      if (field.filedType == FieldBinding.FiledType.STRING) {
        methodBuilder.addStatement("uriBuilder.appendQueryParameter($S, $L)", field.queryName,
            field.fieldName);
      } else if (field.filedType == FieldBinding.FiledType.PRIMITIVE) {
        methodBuilder.addStatement("uriBuilder.appendQueryParameter($S, $L.toString())",
            field.queryName, field.fieldName);
      } else {
        methodBuilder.addStatement("$T<$L> typeToken_$L = new $T<$L>(){}", TYPE_TOKEN,
            field.typeMirror.toString(), field.fieldName, TYPE_TOKEN, field.typeMirror.toString());
        methodBuilder.addStatement("$T json_$L = $T.toJson($L, typeToken_$L.getType())",
            String.class, field.fieldName, LINK_UTILS, field.fieldName, field.fieldName);
        methodBuilder.beginControlFlow("if (json_$L != null)", field.fieldName);
        methodBuilder.addStatement("uriBuilder.appendQueryParameter($S, json_$L)", field.queryName,
            field.fieldName);
        methodBuilder.endControlFlow();
      }
      if (!field.isRequired) {
        methodBuilder.endControlFlow();
      }
    }
    methodBuilder.beginControlFlow("if (nameAndValues != null && !nameAndValues.isEmpty())");
    methodBuilder.beginControlFlow("for($T name: nameAndValues.keySet())", String.class);
    methodBuilder.addStatement("uriBuilder.appendQueryParameter(name, nameAndValues.get(name))");
    methodBuilder.endControlFlow();
    methodBuilder.endControlFlow();
    methodBuilder.addStatement("return uriBuilder.build()");
    return methodBuilder.build();
  }

  static String createClassName(String elyrialink) {
    String url = elyrialink.toLowerCase(Locale.US);
    String pathString = url.replace("elyria://", "");
    List<String> pathList = Splitter.on("/").trimResults().splitToList(pathString);
    return FluentIterable.from(pathList).transform(new Function<String, String>() {
      @Override public String apply(String input) {
        return StringUtils.capitalize(input);
      }
    }).join(Joiner.on("")) + BUILDER_NAME_SUFFIX;
  }

  private static class FieldMethodFunc implements Function<FieldBinding, MethodSpec> {
    private final ClassName className;

    private FieldMethodFunc(ClassName className) {
      this.className = className;
    }

    @Override public MethodSpec apply(FieldBinding input) {
      ParameterSpec.Builder parameterBuilder =
          ParameterSpec.builder(TypeName.get(input.typeMirror), input.fieldName);
      if (input.filedType != FieldBinding.FiledType.PRIMITIVE) {
        if (input.isRequired) {
          parameterBuilder.addAnnotation(NON_NULL_NAME);
        } else {
          parameterBuilder.addAnnotation(NULLABLE_NAME);
        }
      }
      return MethodSpec.methodBuilder(createMethodName(input.queryName))
          .addModifiers(Modifier.PUBLIC)
          .addParameter(parameterBuilder.build())
          .addStatement("this.$L = $L", input.fieldName, input.fieldName)
          .addStatement("return this")
          .returns(className)
          .build();
    }
  }

  private static String createMethodName(String queryName) {
    String newName = queryName.toLowerCase(Locale.US).replaceAll(ILLEGAL_KEY_PATTERN, "_");
    Iterable<String> list = Splitter.on("_").trimResults().split(newName);
    String capitalizedName = FluentIterable.from(list).transform(new Function<String, String>() {
      @Override public String apply(String input) {
        return StringUtils.capitalize(input);
      }
    }).join(Joiner.on(""));
    return "with" + capitalizedName;
  }

  private static class FieldSpecFunc implements Function<FieldBinding, FieldSpec> {

    @Override public FieldSpec apply(FieldBinding input) {
      FieldSpec.Builder builder = FieldSpec.builder(findTypeName(input), createFieldName(input))
          .addModifiers(Modifier.PRIVATE);
      return builder.build();
    }

    private TypeName findTypeName(FieldBinding fieldBinding) {
      FieldBinding.FiledType filedType = fieldBinding.filedType;
      if (filedType == FieldBinding.FiledType.PRIMITIVE) {
        return TypeName.get(fieldBinding.typeMirror).box();
      } else {
        return TypeName.get(fieldBinding.typeMirror);
      }
    }

    private String createFieldName(FieldBinding fieldBinding) {
      return fieldBinding.fieldName;
    }
  }
}
