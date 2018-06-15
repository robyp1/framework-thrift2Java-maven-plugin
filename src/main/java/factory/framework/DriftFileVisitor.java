package factory.framework;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.annotation.AnnotationDescription;
import org.apache.maven.plugin.logging.Log;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * For Walking a file tree
 * consider .class files
 */
public class DriftFileVisitor implements FileVisitor<Path> {

    private static final String CLASS_EXT = ".class";
    public static final String DOLLAR_INNER_CLASS = "\\$";
    private final Path _outputDirectory;
    private final String _regexFilter;
    private final Log _log;
    private final Set<String> _res;
    private final Set<String> _outputClasses;

    /**
     *
     * @param outputDirectory  directory where is possible to find classes- required
     * @param regexFilter filtering class name against a patter - optionally
     * @param log
     * @param interfaceClasss Set of Drift Service classes ending with $Iface
     * @param outputClasses Set of classes correspond ti outClassesUrl
     */
    public DriftFileVisitor(Path outputDirectory, String regexFilter, Log log, Set<String> interfaceClasss, Set<String> outputClasses) {
        _outputDirectory = outputDirectory;
        _regexFilter = regexFilter;
        _log =log;
        _res = interfaceClasss;
        _outputClasses = outputClasses;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        return FileVisitResult.CONTINUE;
    }

    /**
     * Resolves tha java name (package.className) for each .class present in the directory
     * against a matcher pattern (optionally)
     * @param file
     * @param attrs
     * @return
     * @throws IOException
     */
    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        if (file != null && file.toString().endsWith(".class")) {
            String outputclasses = file.toFile().getAbsolutePath();
            _log.debug("take classes dir [" + outputclasses + "] and remove target class [" + _outputDirectory + "]");
            String className = outputclasses.replace(_outputDirectory.toFile().getAbsolutePath(), ""); // remove ../target/classes prefix
            className = className.replace("\\", "."); //substitute . insteaf of /
            className = className
                    .substring(1, className.length() - CLASS_EXT.length()) // remove leading "\" and ending ".class"
                    ;
            _outputClasses.add( "drift." + className); //append prefix 'drift' at begining
            _log.debug(String.format("check class name: %s", className));
            //bug!? il doppio dollaro manda in blocco il check!! allora escludo dal check stringhe con piu di un dollaro
            if (className.indexOf(DOLLAR_INNER_CLASS) == className.lastIndexOf(DOLLAR_INNER_CLASS)) {
                _log.debug("**" + className + " match - > " + String.valueOf(className.matches(_regexFilter)));
                if (className.matches(_regexFilter) || _regexFilter == null || _regexFilter.trim().equals("")) {
                    _res.add("drift." + className); //add drift because is the package root
                    _log.debug(String.format("found class name service %s", className));
                }

            }
        }
        return FileVisitResult.CONTINUE;
    }



    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        return FileVisitResult.CONTINUE;
    }



}
