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
             urlClassLoader = loadClasses(driftClassAbsolutePath);
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
     * @return
     * @throws NoSuchMethodException
     * @throws MalformedURLException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    public ClassLoader loadClasses( File driftClassAbsolutePath ) throws NoSuchMethodException, MalformedURLException, InvocationTargetException, IllegalAccessException //throws NoSuchMethodException, MalformedURLException, InvocationTargetException, IllegalAccessException
     {
         Class<URLClassLoader> urlClass = URLClassLoader.class;
        ClassLoader classLoader = this.getClass().getClassLoader();
        URI outputDirectoryURI = driftClassAbsolutePath.toURI();
        URLClassLoader urlClassLoader = (URLClassLoader) classLoader;
        //add drift class to plugin classpath
        Method addUrl = urlClass.getDeclaredMethod("addURL", new Class[]{URL.class});
        addUrl.setAccessible(true);
        addUrl.invoke(urlClassLoader, new Object[]{outputDirectoryURI.toURL()});
        return urlClassLoader;
    }

}
