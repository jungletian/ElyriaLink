package com.elyria.elyrialink;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.lang.model.element.Modifier;
import org.apache.commons.lang.StringUtils;

import static com.elyria.elyrialink.UrlBuilder.PACKAGE_NAME;
import static javax.lang.model.element.Modifier.PRIVATE;

/**
 * @author jungletian (tjsummery@gmail.com)
 */

class UrlBuilderUtil {
  private static final String CLASS_NAME = "ElyriaLinkBuilders";
  private final Set<String> elyriaLinks;

  UrlBuilderUtil(Set<String> urls) {
    elyriaLinks = Collections.unmodifiableSet(urls);
  }

  JavaFile brewJava() {
    TypeSpec.Builder classBuilder = TypeSpec.classBuilder(CLASS_NAME)
        .addModifiers(Modifier.PUBLIC)
        .addMethod(MethodSpec.constructorBuilder().addModifiers(PRIVATE).build());

    for (String link : elyriaLinks) {
      // add static filed
      String linkFiledName = linkStaticName(link);
      FieldSpec urlField =
          FieldSpec.builder(String.class, linkFiledName, Modifier.PUBLIC, Modifier.FINAL,
              Modifier.STATIC).initializer("$S", link).build();
      classBuilder.addField(urlField);
      // add simple factory method
      String builderClassName = UrlBuilder.createClassName(link);
      classBuilder.addMethod(createBuildUriMethod(builderClassName, linkFiledName));
    }

    return JavaFile.builder(PACKAGE_NAME, classBuilder.build())
        .addFileComment("Generated code from ElyriaLink. Do not modify!")
        .build();
  }

  private MethodSpec createBuildUriMethod(String className, String linkFiledName) {
    TypeName typeName = ClassName.get(PACKAGE_NAME, className);
    String methodName = StringUtils.uncapitalize(className);
    MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName)
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .returns(typeName);
    methodBuilder.addStatement("return new $T($L)", typeName, linkFiledName);
    return methodBuilder.build();
  }

  private static String linkStaticName(String elyriaLink) {
    String url = elyriaLink.toLowerCase(Locale.US);
    String pathString = url.replace("elyria://", "");
    List<String> pathList = Splitter.on("/").trimResults().splitToList(pathString);
    return FluentIterable.from(pathList).transform(new Function<String, String>() {
      @Override public String apply(String input) {
        return StringUtils.upperCase(input, Locale.US);
      }
    }).join(Joiner.on("_")) + "_LINK";
  }
}
