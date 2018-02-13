package com.elyria.elyrialink;

import com.google.auto.common.SuperficialValidation;
import com.google.auto.service.AutoService;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;

import static javax.lang.model.element.ElementKind.CLASS;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.Diagnostic.Kind.NOTE;

/**
 * @author jungletian (tjsummery@gmail.com)
 */
@AutoService(Processor.class) public class ElyriaLinkProcessor extends AbstractProcessor {
  private static final String BINDING_CLASS_SUFFIX = "QueryBinder";
  private static final String STRING_TYPE = "java.lang.String";
  private static final String ACTIVITY_TYPE = "android.app.Activity";
  private static final String FRAGMENT_TYPE = "android.app.Fragment";
  private static final String FRAGMENT_TYPE_V4 = "android.support.v4.app.Fragment";
  private static final Pattern REGEX = Pattern.compile("/$");

  private static final ClassName ELYRIA_LINK_ENTRY_NAME =
      ClassName.get("com.elyria.elyrialink", "ElyriaLinkData");
  private static final ClassName ELYRIA_LINK_PARSER_NAME =
      ClassName.get("com.elyria.elyrialink", "ElyriaLinkParser");

  private static final Set<TypeKind> SUPPORTED_PRIMITIVE_KINDS =
      Sets.newHashSet(TypeKind.INT, TypeKind.LONG, TypeKind.FLOAT, TypeKind.BOOLEAN,
          TypeKind.DOUBLE);
  // Element: Represents a program element such as a package, class, or method.
  // TypeElement: Represents a class or interface program element.
  // Utility methods for operating on program elements.
  private Elements elementUtils;
  // This interface supports the creation of new files by an annotation processor.
  private Filer filer;
  // A Messager provides the way for an annotation processor to report error messages, warnings, and other notices.
  private Messager messager;

  // ProcessingEnvironment 提供了上下文环境
  @Override public synchronized void init(ProcessingEnvironment processingEnvironment) {
    super.init(processingEnvironment);
    elementUtils = processingEnvironment.getElementUtils();
    filer = processingEnvironment.getFiler();
    messager = processingEnvironment.getMessager();
  }

  /**
   * @param roundEnvironment An annotation processing tool framework will provide an annotation
   * processor with an object implementing this interface so that the processor can query for
   * information about a round of annotation processing.
   */
  @Override public boolean process(Set<? extends TypeElement> set,
      RoundEnvironment roundEnvironment) {
    try {
      // 解析 ElyriaLinkQuery 注解
      Map<TypeElement, QueryBinding> queryBindings = parseElyriaLinkQuery(roundEnvironment);
      if (!queryBindings.isEmpty()) {
        for (TypeElement typeElement : queryBindings.keySet()) {
          queryBindings.get(typeElement).brewJava().writeTo(filer);
        }
      }

      // 解析 ElyriaLink 注解
      Map<String, ElyriaLinkAnnotation> linksMap = parseLinks(roundEnvironment);
      generateBinder(linksMap);
      if (!queryBindings.isEmpty() && !linksMap.isEmpty()) {
        Map<String, UrlBuilder> builderMap = new LinkedHashMap<>();
        for (String url : linksMap.keySet()) {
          String clazz = linksMap.get(url).clazz;
          List<FieldBinding> fields = findAllFields(clazz, queryBindings);
          builderMap.put(url, new UrlBuilder(url, fields));
        }
        for (String url : builderMap.keySet()) {
          builderMap.get(url).brewJava().writeTo(filer);
        }
        UrlBuilderUtil urlBuilderUtil = new UrlBuilderUtil(builderMap.keySet());
        urlBuilderUtil.brewJava().writeTo(filer);
      }
    } catch (IOException e) {
      error("Error createing file");
    } catch (Exception e) {
      StringWriter stackTrace = new StringWriter();
      e.printStackTrace(new PrintWriter(stackTrace));
      error("Internal error during annotation processing");
    }
    return true;
  }

  private List<FieldBinding> findAllFields(String clazz,
      Map<TypeElement, QueryBinding> queryBindingMap) {
    for (TypeElement element : queryBindingMap.keySet()) {
      if (element.toString().equals(clazz)) {
        return queryBindingMap.get(element).allFields();
      }
    }
    return Lists.newArrayList();
  }

  private void generateBinder(Map<String, ElyriaLinkAnnotation> elements) throws IOException {
    if (elements == null || elements.isEmpty()) return;
    // field
    ClassName list = ClassName.get("java.util", "Map");
    ClassName arrayMap = ClassName.get("android.support.v4.util", "ArrayMap");
    TypeName listOfLinkEntry =
        ParameterizedTypeName.get(list, TypeName.get(String.class), ELYRIA_LINK_ENTRY_NAME);
    //TypeVariableName.get("Class<?>"));
    FieldSpec.Builder binder = FieldSpec.builder(listOfLinkEntry, "BINDERS")
        .addModifiers(Modifier.PRIVATE, Modifier.FINAL, Modifier.STATIC);

    CodeBlock.Builder codeBlock =
        CodeBlock.builder().addStatement("BINDERS = new $T<>()", arrayMap);

    for (String key : elements.keySet()) {
      ClassName activity = ClassName.bestGuess(elements.get(key).clazz);
      //codeBlock.addStatement("BINDERS.put($S, $T.class)", key, activity);
      codeBlock.addStatement("BINDERS.put($S, new $T($T.class, $L))", key, ELYRIA_LINK_ENTRY_NAME,
          activity, elements.get(key).userRequired);
    }

    MethodSpec finder = MethodSpec.methodBuilder("parse")
        .addAnnotation(Override.class)
        .addParameter(String.class, "urlString")
        .addModifiers(Modifier.PUBLIC)
        .addStatement("return BINDERS.get(urlString)")
        .returns(ELYRIA_LINK_ENTRY_NAME)
        .build();

    ClassName set = ClassName.get("java.util", "Set");
    TypeName urlSet = ParameterizedTypeName.get(set, TypeName.get(String.class));

    MethodSpec urls = MethodSpec.methodBuilder("urls")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .addStatement("return $L.unmodifiableSet($L.keySet())", ClassName.get(Collections.class),
            "BINDERS")
        .returns(urlSet)
        .build();

    TypeSpec linkBinder = TypeSpec.classBuilder("SimpleElyriaLinkParser")
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        .addSuperinterface(ELYRIA_LINK_PARSER_NAME)
        .addField(binder.build())
        .addStaticBlock(codeBlock.build())
        .addMethod(finder)
        .addMethod(urls)
        .build();

    JavaFile.builder("com.elyria.elyrialink", linkBinder)
        .addFileComment("Generated code from ElyriaLink. Do not modify!")
        .build()
        .writeTo(filer);
  }

  private Map<String, ElyriaLinkAnnotation> parseLinks(RoundEnvironment roundEnvironment) {
    Map<String, ElyriaLinkAnnotation> deepLinkElements = new HashMap<>();
    for (Element element : roundEnvironment.getElementsAnnotatedWith(ElyriaLink.class)) {
      if (!SuperficialValidation.validateElement(element)) continue;
      if (element.getKind() != ElementKind.CLASS) {
        error("Only class can be annotated with @%s", ElyriaLink.class.getSimpleName());
      }

      // We can cast it, because we know that it of ElementKind.CLASS
      TypeElement typeElement = (TypeElement) element;
      if (!isSubtypeOfType(typeElement.getSuperclass(), ACTIVITY_TYPE)) {
        error("%s is not a type of %s. @%s can only annotated with %s or it's subclass",
            typeElement.getSimpleName(), ACTIVITY_TYPE, ElyriaLink.class.getSimpleName(),
            ACTIVITY_TYPE);
      }
      String[] links = element.getAnnotation(ElyriaLink.class).value();
      boolean userRequired = element.getAnnotation(ElyriaLink.class).userRequired();
      for (String link : links) {
        try {
          if (REGEX.matcher(link).find()) {
            error("%s MUST NOT have a trailing slash, it should be: %s", link,
                link.replaceAll("/$", ""));
          }
          String uri = parserUri(link);
          if (uri == null) {
            throw new MalformedURLException("wrong url: " + link);
          }
          if (deepLinkElements.get(uri) != null) {
            error("%s and %s have same elyria link path: %s", deepLinkElements.get(uri),
                element.toString(), uri);
          } else {
            deepLinkElements.put(uri, new ElyriaLinkAnnotation(element.toString(), userRequired));
          }
        } catch (MalformedURLException e) {
          messager.printMessage(ERROR, "Malformed Elyria Link URL " + link);
        }
      }
    }
    return deepLinkElements;
  }

  /**
   * 解析 {@link ElyriaLinkQuery} 注解
   *
   * @param roundEnvironment 上下文
   */
  private Map<TypeElement, QueryBinding> parseElyriaLinkQuery(RoundEnvironment roundEnvironment) {
    Map<TypeElement, QueryBinding> targetMap = new LinkedHashMap<>();
    // element: 变量名
    for (Element element : roundEnvironment.getElementsAnnotatedWith(ElyriaLinkQuery.class)) {
      if (!SuperficialValidation.validateElement(element)) continue;
      if (element.getKind() != ElementKind.FIELD) {
        error("@%s can only annotated with filed: %s@%s", ElyriaLinkQuery.class.getSimpleName(),
            ElyriaLinkQuery.class.getSimpleName(), element.toString());
      }
      /**
       * 获取封闭元素
       * Returns the innermost element within which this element is, loosely speaking, enclosed.
       * If this element is one whose declaration is lexically enclosed immediately within the declaration of another element, that other element is returned.
       * If this is a top-level type, its package is returned.
       * If this is a package, null is returned.
       * If this is a type parameter, the generic element of the type parameter is returned.
       */
      TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();
      String queryName = element.getAnnotation(ElyriaLinkQuery.class).value();
      boolean hasError = false;
      // 校验生成代码是否可以访问
      hasError |= isInaccessibleViaGeneratedCode(ElyriaLinkQuery.class, "fields", element);
      FieldBinding.FiledType filedType = parseFieldType(enclosingElement, element, queryName);
      hasError |= filedType == null;
      TypeMirror typeMirror = element.asType();
      boolean isActivity = false;
      if (isSubtypeOfType(enclosingElement.asType(), ACTIVITY_TYPE)) {
        isActivity = true;
      } else if (isSubtypeOfType(enclosingElement.asType(), FRAGMENT_TYPE) || isSubtypeOfType(
          enclosingElement.asType(), FRAGMENT_TYPE_V4)) {
        isActivity = false;
      } else {
        hasError = true;
        error("@%s can only work with Activity or Fragment, current: %s",
            ElyriaLinkQuery.class.getSimpleName(), enclosingElement.toString());
      }

      boolean isRequired = !element.getAnnotation(ElyriaLinkQuery.class).optional();
      if (!hasError) {
        QueryBinding binding = getOrCreateTargetClass(targetMap, enclosingElement, isActivity);
        String name = element.getSimpleName().toString();
        info(
            "FieldBinding = { name = %s, queryName = %s, typeMirror = %s, isRequired = %s, filedType = %s }",
            name, queryName, typeMirror, isRequired, filedType);
        binding.addFiled(new FieldBinding(name, queryName, typeMirror, isRequired, filedType));
      }
    }
    return targetMap;
  }

  private QueryBinding getOrCreateTargetClass(Map<TypeElement, QueryBinding> targetClassMap,
      TypeElement enclosingElement, boolean isActivity) {
    QueryBinding bindingClass = targetClassMap.get(enclosingElement);
    if (bindingClass == null) {
      String targetType = enclosingElement.getQualifiedName().toString();
      String classPackage = getPackageName(enclosingElement);
      String className = getClassName(enclosingElement, classPackage) + BINDING_CLASS_SUFFIX;
      bindingClass = new QueryBinding(classPackage, className, targetType, isActivity);
      info("QueryBinding = { classPackage = %s, className = %s, targetType = %s, isActivity = %s }",
          classPackage, className, targetType, isActivity);
      targetClassMap.put(enclosingElement, bindingClass);
    }
    return bindingClass;
  }

  private String getPackageName(TypeElement type) {
    return elementUtils.getPackageOf(type).getQualifiedName().toString();
  }

  private static String getClassName(TypeElement type, String packageName) {
    int packageLen = packageName.length() + 1;
    return type.getQualifiedName().toString().substring(packageLen).replace('.', '$');
  }

  private boolean isSubtypeOfType(TypeMirror typeMirror, String otherType) {
    if (otherType.equals(typeMirror.toString())) {
      return true;
    }
    if (typeMirror.getKind() != TypeKind.DECLARED) {
      return false;
    }
    DeclaredType declaredType = (DeclaredType) typeMirror;
    List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
    if (typeArguments.size() > 0) {
      StringBuilder typeString = new StringBuilder(declaredType.asElement().toString());
      typeString.append('<');
      for (int i = 0; i < typeArguments.size(); i++) {
        if (i > 0) {
          typeString.append(',');
        }
        typeString.append('?');
      }
      typeString.append('>');
      if (typeString.toString().equals(otherType)) {
        return true;
      }
    }
    Element element = declaredType.asElement();
    if (!(element instanceof TypeElement)) {
      return false;
    }
    TypeElement typeElement = (TypeElement) element;
    TypeMirror superType = typeElement.getSuperclass();
    if (isSubtypeOfType(superType, otherType)) {
      return true;
    }
    for (TypeMirror interfaceType : typeElement.getInterfaces()) {
      if (isSubtypeOfType(interfaceType, otherType)) {
        return true;
      }
    }
    return false;
  }

  private FieldBinding.FiledType parseFieldType(TypeElement enclosingElement, Element element,
      String queryName) {
    TypeKind kind = element.asType().getKind();
    if (STRING_TYPE.equalsIgnoreCase(element.asType().toString())) {
      return FieldBinding.FiledType.STRING;
    } else if (SUPPORTED_PRIMITIVE_KINDS.contains(kind)) {
      return FieldBinding.FiledType.PRIMITIVE;
    } else if (isAnnotatedWithJson(element)) {
      return FieldBinding.FiledType.JSON;
    }
    if (kind.isPrimitive()) {
      error("\n%s(\"%s\") is NOT supported. Supported types: [int, long, float, boolean, double],"
              + " now is: %s", ElyriaLinkQuery.class.getSimpleName(), queryName,
          element.asType().toString());
    } else {
      error("\n%s.%s is not supported, Did you forget add @Json?", enclosingElement.toString(),
          queryName);
    }
    return null;
  }

  private boolean isAnnotatedWithJson(Element element) {
    for (AnnotationMirror annotationMirror : element.getAnnotationMirrors()) {
      if (annotationMirror.getAnnotationType().asElement().toString().endsWith(".Json")) {
        return true;
      }
    }
    return false;
  }

  @Override public Set<String> getSupportedAnnotationTypes() {
    Set<String> types = new LinkedHashSet<>();
    types.add(ElyriaLink.class.getCanonicalName());
    types.add(ElyriaLinkQuery.class.getCanonicalName());
    return types;
  }

  @Override public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  private String parserUri(String s) {
    s = s.toLowerCase();
    if (s.startsWith("elyria")) {
      return s.split("\\?")[0];
    }
    return null;
  }

  private void error(String message, Object... args) {
    if (args.length > 0) {
      message = String.format(message, args);
      messager.printMessage(ERROR, message);
    }
  }

  private void info(String message, Object... args) {
    if (args.length > 0) {
      message = String.format(message, args);
      messager.printMessage(NOTE, message);
    }
  }

  // steal from ButterKnife
  private boolean isInaccessibleViaGeneratedCode(Class<? extends Annotation> annotationClass,
      String targetThing, Element element) {
    boolean hasError = false;
    TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();

    // Verify method modifiers.
    Set<Modifier> modifiers = element.getModifiers();
    if (modifiers.contains(PRIVATE) || modifiers.contains(STATIC)) {
      error("@%s %s must not be private or static. (%s.%s)", annotationClass.getSimpleName(),
          targetThing, enclosingElement.getQualifiedName(), element.getSimpleName());
      hasError = true;
    }

    // Verify containing type.
    if (enclosingElement.getKind() != CLASS) {
      error("@%s %s may only be contained in classes. (%s.%s)", annotationClass.getSimpleName(),
          targetThing, enclosingElement.getQualifiedName(), element.getSimpleName());
      hasError = true;
    }

    // Verify containing class visibility is not private.
    if (enclosingElement.getModifiers().contains(PRIVATE)) {
      error("@%s %s may not be contained in private classes. (%s.%s)",
          annotationClass.getSimpleName(), targetThing, enclosingElement.getQualifiedName(),
          element.getSimpleName());
      hasError = true;
    }

    return hasError;
  }
}
