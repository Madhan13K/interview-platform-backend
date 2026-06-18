package com.interview_platform_backend.interview_platform_backend.codeexecution.entity;

import lombok.Getter;

@Getter
public enum SupportedLanguage {

    JAVA("java", "openjdk:21-slim", "java", "/tmp/code/Main.java",
            new String[]{"sh", "-c", "cd /tmp/code && javac Main.java && java Main"},
            "Main.java"),

    PYTHON("python", "python:3.12-slim", "python3", "/tmp/code/main.py",
            new String[]{"python3", "/tmp/code/main.py"},
            "main.py"),

    JAVASCRIPT("javascript", "node:20-slim", "node", "/tmp/code/main.js",
            new String[]{"node", "/tmp/code/main.js"},
            "main.js"),

    TYPESCRIPT("typescript", "node:20-slim", "npx", "/tmp/code/main.ts",
            new String[]{"sh", "-c", "cd /tmp/code && npx --yes tsx main.ts"},
            "main.ts"),

    CPP("cpp", "gcc:13", "g++", "/tmp/code/main.cpp",
            new String[]{"sh", "-c", "cd /tmp/code && g++ -o main main.cpp && ./main"},
            "main.cpp"),

    C("c", "gcc:13", "gcc", "/tmp/code/main.c",
            new String[]{"sh", "-c", "cd /tmp/code && gcc -o main main.c && ./main"},
            "main.c"),

    GO("go", "golang:1.22-alpine", "go", "/tmp/code/main.go",
            new String[]{"sh", "-c", "cd /tmp/code && go run main.go"},
            "main.go"),

    RUST("rust", "rust:1.77-slim", "rustc", "/tmp/code/main.rs",
            new String[]{"sh", "-c", "cd /tmp/code && rustc -o /tmp/code/main main.rs && /tmp/code/main"},
            "main.rs"),

    RUBY("ruby", "ruby:3.3-slim", "ruby", "/tmp/code/main.rb",
            new String[]{"ruby", "/tmp/code/main.rb"},
            "main.rb"),

    PHP("php", "php:8.3-cli", "php", "/tmp/code/main.php",
            new String[]{"php", "/tmp/code/main.php"},
            "main.php");

    private final String languageId;
    private final String dockerImage;
    private final String compiler;
    private final String filePath;
    private final String[] executeCommand;
    private final String fileName;

    SupportedLanguage(String languageId, String dockerImage, String compiler,
                      String filePath, String[] executeCommand, String fileName) {
        this.languageId = languageId;
        this.dockerImage = dockerImage;
        this.compiler = compiler;
        this.filePath = filePath;
        this.executeCommand = executeCommand;
        this.fileName = fileName;
    }

    public static SupportedLanguage fromId(String languageId) {
        for (SupportedLanguage lang : values()) {
            if (lang.getLanguageId().equalsIgnoreCase(languageId)) {
                return lang;
            }
        }
        throw new IllegalArgumentException("Unsupported language: " + languageId
                + ". Supported: java, python, javascript, typescript, cpp, c, go, rust, ruby, php");
    }
}
