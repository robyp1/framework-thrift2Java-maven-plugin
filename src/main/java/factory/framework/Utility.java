package factory.framework;

import org.apache.maven.plugin.MojoExecutionException;

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

    public static void getListClass(File directoryClasses) throws Exception {
    List<Object> urls = loadClasses(directoryClasses);
//        for (Object url : urls) {
            //getLog().info( ((URL)url).toExternalForm());
//        }
    }


    private static List<Object> loadClasses(File directoryClasses) throws NoSuchMethodException, MalformedURLException, InvocationTargetException, IllegalAccessException {
            ClassLoader classLoader = Utility.class.getClass().getClassLoader();
            URI outputDirectoryURI = directoryClasses.toURI();
            URLClassLoader urlClassLoader = (URLClassLoader) classLoader;
            Class<URLClassLoader> urlClass = URLClassLoader.class;
            Method addUrl = urlClass.getDeclaredMethod("addURL", new Class[]{URL.class});
            addUrl.setAccessible(true);
            addUrl.invoke(urlClassLoader, new Object[]{outputDirectoryURI.toURL()});
            Method getUrl = urlClass.getDeclaredMethod("getURLs");
            return Arrays.asList(getUrl.invoke(urlClassLoader));


    }

    /**
     * Resolves tha java name (package.className) for each .class present in the directory.
     *
     * @param directory
     * @return
     */
    private static Set<String> getClassNames(File directory,File directoryClasses) {
        Set<String> res = new HashSet<String>();
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file: files) {
                if (file.isDirectory()) {
                    res.addAll(getClassNames(file,directoryClasses));
                } else {
                    String absolutePath = file.getAbsolutePath();
                    String className = absolutePath.replace(directoryClasses.getAbsolutePath(), ""); // remove ../target/classes prefix
                    className = className
                            .substring(1, className.length() - "class".length()) // remove leading "\" and ending ".class"
                            .replace("\\", ".");
                    res.add(className);
                }
            }
        }
        return res;
    }

}
