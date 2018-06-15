package factory.framework;

import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * Tools for reflection and others utilities..
 */
public class Utility {

    private static final String CLASS_EXT = ".class";
    private final Log log;

    public Utility(Log log){
        this.log= log;
    }


    /**
     * call loadClasses methods and list all classes in this classloader
     * @param driftClassAbsolutePath
     * @throws Exception
     */
    public void getAllListClass(File driftClassAbsolutePath) throws Exception {
        Class<URLClassLoader> urlClass = URLClassLoader.class;
        ClassLoader urlClassLoader = null;
        try {
             urlClassLoader = loadClasses(driftClassAbsolutePath, urlClass);
        }catch (Exception ex){
            ex.printStackTrace();;
        }
        if (urlClassLoader != null) {
            Method getUrl = urlClass.getDeclaredMethod("getURLs");
            getUrl.setAccessible(true);
            Object[] arrayUrl = (Object[]) getUrl.invoke(urlClassLoader);
            for (int i = 0 ; i < arrayUrl.length; i++) {
                log.info(((URL) arrayUrl[i]).toExternalForm());
            }
        }
    }


    /**
     * load target drift (thrift) classes in this classloader
     * @param driftClassAbsolutePath
     * @param urlClass
     * @return
     * @throws NoSuchMethodException
     * @throws MalformedURLException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    public ClassLoader loadClasses( File driftClassAbsolutePath, Class<URLClassLoader> urlClass ) throws NoSuchMethodException, MalformedURLException, InvocationTargetException, IllegalAccessException //throws NoSuchMethodException, MalformedURLException, InvocationTargetException, IllegalAccessException
     {
        ClassLoader classLoader = this.getClass().getClassLoader();
        URI outputDirectoryURI = driftClassAbsolutePath.toURI();
        URLClassLoader urlClassLoader = (URLClassLoader) classLoader;
        //add drift class to plugin classpath
        Method addUrl = urlClass.getDeclaredMethod("addURL", new Class[]{URL.class});
        addUrl.setAccessible(true);
        addUrl.invoke(urlClassLoader, new Object[]{outputDirectoryURI.toURL()});
        return urlClassLoader;
    }


    /**
     * use:
     * @see factory.framework.DriftFileVisitor
     * Resolves tha java name (package.className) for each .class present in the directory
     * against a matcher pattern (optionally)
     * recursive method
     * @param directory  directory where is possible to find classes- required
     * @param _outputDirectory target classes directory (root of searching) where start to find  classes- required
     * @param regexFilter filtering class name against a patter - optionally
     * @param  externalPackageName the parent package in root project classes, 'drift' wich containts subpackages drift.x.y...className.class
     * @return
     */
    @Deprecated
    public Set<String> getClassNames(File directory, File _outputDirectory, String regexFilter, String externalPackageName) {
        Set<String> res = new HashSet();
        File[] files = directory.listFiles();
//        log.info(directory.getPath());
        if (files != null) {
            for (File file: files) {
                if (file.isDirectory()) {
                    res.addAll(getClassNames(file, _outputDirectory, regexFilter, externalPackageName));
                    log.info(String.format("looking into dir: %s", file.getAbsolutePath()));
                } else {
                    String absolutePath = file.getAbsolutePath();
                    String className = absolutePath.replace(_outputDirectory.getAbsolutePath(), ""); // remove ../target/classes prefix
                    className = className
                            .substring(1, className.length() - CLASS_EXT.length()) // remove leading "\" and ending ".class"
                            .replace("\\", ".");
                    log.debug(String.format("found class name: %s", className));
                    //bug!? il doppio dollaro manda in blocco il check!! allora escludo dal check stringhe con piu di un dollaro
                    if (className.indexOf("\\$") == className.lastIndexOf("\\$")) {
                        log.debug("**" + className + " match - > " + String.valueOf(className.matches(regexFilter)));
                        if (className.matches(regexFilter) || regexFilter == null || regexFilter.trim().equals("")) {
                            res.add("drift." + className);
                            log.debug(String.format("found class name service %s", className));
                        }
                    }
                }
            }
        }
        return res;
    }
}
