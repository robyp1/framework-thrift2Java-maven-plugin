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

public class Utility {

    private static final String CLASS_EXT = ".class";
    private final Log log;

    public Utility(Log log){
        this.log= log;
    }

    public void getListClass(File driftClassAbsolutePath) throws Exception {
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



    public ClassLoader loadClasses( File driftClassAbsolutePath, Class<URLClassLoader> urlClass ) throws NoSuchMethodException, MalformedURLException, InvocationTargetException, IllegalAccessException //throws NoSuchMethodException, MalformedURLException, InvocationTargetException, IllegalAccessException
     {
        ClassLoader classLoader = this.getClass().getClassLoader();
        URI outputDirectoryURI = driftClassAbsolutePath.toURI();
        URLClassLoader urlClassLoader = (URLClassLoader) classLoader;
        //add drift class to plugin classpath
        Method addUrl = urlClass.getDeclaredMethod("addURL", new Class[]{URL.class});
        addUrl.setAccessible(true);
        addUrl.invoke(urlClassLoader, new Object[]{outputDirectoryURI.toURL()});
        return classLoader;
    }


    /**
     * Resolves tha java name (package.className) for each .class present in the directory.
     *
     * @param directory
     * @return
     */
    public Set<String> getClassNames(File directory, File _outputDirectory) {
        Set<String> res = new HashSet();
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file: files) {
                if (file.isDirectory()) {
                    res.addAll(getClassNames(file, _outputDirectory));
                } else {
                    String absolutePath = file.getAbsolutePath();
                    String className = absolutePath.replace(_outputDirectory.getAbsolutePath(), ""); // remove ../target/classes prefix
                    className = className
                            .substring(1, className.length() - CLASS_EXT.length()) // remove leading "\" and ending ".class"
                            .replace("\\", ".");
                    res.add(className);
                }
            }
        }
        return res;
    }
}
