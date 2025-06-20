package com.vifinancenews;

import io.github.classgraph.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class UMLDiagramGenerator {
    public static void main(String[] args) throws IOException {
        String basePackage = "com.vifinancenews";

        try (ScanResult scanResult = new ClassGraph()
                .enableAllInfo()
                .acceptPackages(basePackage)
                .scan()) {

            Set<String> allClasses = scanResult.getAllClasses().stream()
                    .map(ClassInfo::getName)
                    .collect(Collectors.toSet());

            StringBuilder sb = new StringBuilder();
            sb.append("@startuml\n");
            sb.append("skinparam classAttributeIconSize 0\n\n");

            // Generate class and interface definitions
            for (ClassInfo classInfo : scanResult.getAllClasses()) {
                if (classInfo.isInterface()) {
                    sb.append("interface ").append(classInfo.getSimpleName()).append(" {\n");
                } else {
                    sb.append("class ").append(classInfo.getSimpleName()).append(" {\n");
                }

                // Attributes (fields)
                for (FieldInfo field : classInfo.getFieldInfo()) {
                    if (field.isStatic() || field.isSynthetic()) continue;

                    String visibility = getVisibilitySymbol(field.getModifiers());
                    String typeName = simpleName(field.getTypeSignatureOrTypeDescriptor().toString());
                    // UML attribute format: visibility name : type
                    sb.append("  ").append(visibility)
                      .append(" ").append(field.getName())
                      .append(" : ").append(typeName)
                      .append("\n");
                }

                sb.append("--\n");  // Separator between attributes and methods

                // Methods
                for (MethodInfo method : classInfo.getDeclaredMethodInfo()) {
                    if (method.isSynthetic() || method.isBridge()) continue;

                    String visibility = getVisibilitySymbol(method.getModifiers());
                    String returnType = simpleName(method.getTypeSignatureOrTypeDescriptor()
                            .getResultType().toString());

                    // Parameters with their simple types (without parameter names because they are not easily accessible)
                    String params = Arrays.stream(method.getParameterInfo())
                            .map(param -> simpleName(param.getTypeSignatureOrTypeDescriptor().toString()))
                            .collect(Collectors.joining(", "));

                    // UML method format: visibility name(params) : returnType
                    sb.append("  ").append(visibility)
                      .append(" ").append(method.getName())
                      .append("(").append(params).append(")")
                      .append(" : ").append(returnType)
                      .append("\n");
                }

                sb.append("}\n\n");
            }

            // Inheritance (extends)
            for (ClassInfo classInfo : scanResult.getAllClasses()) {
                ClassInfo superClass = classInfo.getSuperclass();
                if (superClass != null && !superClass.getName().equals("java.lang.Object")) {
                    sb.append(superClass.getSimpleName())
                      .append(" <|-- ")
                      .append(classInfo.getSimpleName())
                      .append("\n");
                }
            }

            // Interface implementations
            for (ClassInfo classInfo : scanResult.getAllClasses()) {
                for (ClassInfo iface : classInfo.getInterfaces()) {
                    sb.append(iface.getSimpleName())
                      .append(" <|.. ")
                      .append(classInfo.getSimpleName())
                      .append("\n");
                }
            }

            // Associations (field references to other classes in the project)
            Set<String> associations = new HashSet<>();
            for (ClassInfo classInfo : scanResult.getAllClasses()) {
                for (FieldInfo field : classInfo.getFieldInfo()) {
                    String rawType = field.getTypeSignatureOrTypeDescriptor().toString();
                    String candidate = rawType.contains("<") ? rawType.substring(0, rawType.indexOf('<')) : rawType;
                    String simple = simpleName(candidate);

                    boolean refersToClass = allClasses.stream().anyMatch(name ->
                            name.endsWith("." + simple));

                    String rel = classInfo.getSimpleName() + " --> " + simple;
                    if (refersToClass && !associations.contains(rel)) {
                        sb.append(rel).append("\n");
                        associations.add(rel);
                    }
                }
            }

            sb.append("\n@enduml\n");

            Path outputDir = Paths.get("uml");
            Files.createDirectories(outputDir);
            Path outputFile = outputDir.resolve("diagram.puml");
            Files.writeString(outputFile, sb.toString());

            System.out.println("✅ Generated UML diagram at: " + outputFile.toAbsolutePath());
        }
    }

    private static String getVisibilitySymbol(int modifiers) {
        if (java.lang.reflect.Modifier.isPublic(modifiers)) return "+";
        if (java.lang.reflect.Modifier.isPrivate(modifiers)) return "-";
        if (java.lang.reflect.Modifier.isProtected(modifiers)) return "#";
        return "~"; // package-private
    }

    private static String simpleName(String fqcn) {
        if (fqcn.contains(".")) {
            return fqcn.substring(fqcn.lastIndexOf('.') + 1).replace(";", "");
        }
        return fqcn.replace(";", "");
    }
}
