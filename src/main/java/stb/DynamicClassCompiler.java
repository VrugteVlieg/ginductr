package stb;

import javax.tools.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Compiles java code for instrumented directory.
 */
public class DynamicClassCompiler {

    private final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

    private final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

    List<String> optionList = new ArrayList<>();

    StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);

    public DynamicClassCompiler() {
        optionList.add("-cp");
        // optionList.add(System.getProperty("java.class.path") + ";dist/InlineCompiler.jar");
        optionList.add("./antlr-4.8-complete.jar");
        // optionList.add("-d");
        // optionList.add();
    }

    /**
     * Recurses output instrumented directory and returns list of java files.
     *
     * @param directory Input directory.
     * @return ArrayList of java files.
     */
    private List<File> getDirectoryFiles(File directory) {
        List<File> fileList = new ArrayList<>();
        if (directory.isDirectory()) {
            for (File file : directory.listFiles()) {
                fileList.addAll(getDirectoryFiles(file));
            }
        } else {
            if (directory.getName().endsWith(".java")) {
                return Collections.singletonList(directory);
            }
        }
        return fileList;
    }

    /**
     * Compiles java files in output instrumented directory.
     *
     * @param target Instrumented code directory.
     * @return
     */
    public boolean compile(File target) {
        Iterable<? extends JavaFileObject> compilationUnit;
        
        if (target.isDirectory()) {
            compilationUnit = fileManager.getJavaFileObjectsFromFiles(getDirectoryFiles(target));
        } else {
            compilationUnit = fileManager.getJavaFileObjectsFromFiles(Collections.singletonList(target));
        }

        System.err.println("Fetched files");
        JavaCompiler.CompilationTask task = compiler.getTask(
                null,
                fileManager,
                null,
                optionList,
                null,
                compilationUnit);
        boolean result = task.call();
        System.err.println("Result " + result);
        return result;
    }
}
