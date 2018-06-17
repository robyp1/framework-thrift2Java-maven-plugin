package factory.framework;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.matcher.ElementMatchers;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import javax.jws.WebService;
import javax.xml.bind.annotation.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;


/**
 *
 * Generate wsdl descriptor from service class after maven compile phase (phase process classes in maven build lifecycle)
 * @link https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html
 * This is executed after compile phase (live <phase>empty in plugin execution)
 *
 *  services belongs to package: drift.< progetto >.< modulo >.api.< servizio >.nomeService.class
 *
 * example :
 * in target/classes/generated-sources/drift there are the java thrift sources for example:
 *
 *                 drift.drift.thrift.api.shared.SharedService.java -> its the service with $Iface (inner interface Iface)
 *
 * The services list with wsdl names must be in META-INF/druft-service.list
 *
 * NB:
 * Per collagare il path al wsdl presente nel drift-service.list
 * il nome del wsdl deve essere pari al nome della classe che precede $Iface
 * es:
 * META-INF/wsdl/drift-thrift@SharedService.wsdl -> SharedService$Iface
 * (SharedService sarà la class name e SharedService.wsdl il nome del wsdl che va nel file di lista)
 *
 * Class with inner interface Iface
 * public class SharedService {
 *
 *   public interface Iface {
 *   ..
 *   }
 *  ..
 *  }
 *
 *   NOTES:
 *
 *   CXF_VERSION_JDK7_BYTECODE ->3.1.15, that is the last versione of Apache CXF compiled for jdk 1.7 (but it works also with 1.8)
 *
 *   CLASSNAME_SERVICE_$_IFACE is the string packages.<name></>Service$Iface
 *   where <name> is the service name
 *
 *   You have to  map service <name>Service with @<name>Service</name>.wsdl in  ${project.basedir}/sources/META-INF/drift-service.list
 *   Example is
 *   META-INF/wsdl/drift-thrift@SharedService.wsdl
 *   is the wsdl generated with SharedService$Iface class.
 *   Manually, with command:
 *   java2ws -cp C:\Progetti\altri\Thrift\drift-thrift-api\target\classes; \
 *            -o C:\Progetti\altri\Thrift\drift-thrift-api\target/resources/META-INF/wsdl/drift-thrift@SharedService.wsdl \
 *            -wsdl \
 *            -verbose \
 *            drift.drift.thrift.api.shared.SharedService$Iface
 *

 */
@Mojo(name = "Java2Ws", defaultPhase = LifecyclePhase.PROCESS_CLASSES)
public class Java2CxfWsMojo extends AbstractMojo {

    public static final String ERROR_MSG_SERVICE_LIST_MISSING = "Error: services list missing! Put service list in ${project.basedir}/src/main/resources/META-INF/drift-services.list";
    public static final String $_IFACE = "$Iface";
    //senza Service obbligatorio (non usata perchè supponiamo che le classi con l'interfaccia Iface annidata fniscano tutti in Service)
    public static final String CLASSNAME_SERVICE_OPTIONALLY_$_IFACE = "(\\w+\\.{1})*(\\w)+(Service)*(\\$Iface){1}";
    //con Service obbligatorio
    public static final String CLASSNAME_SERVICE_$_IFACE = "(\\w+\\.{1})*(\\w+Service\\$Iface)+";
    public static final Pattern SERVICE_PATTERN_CLASS = Pattern.compile(CLASSNAME_SERVICE_$_IFACE);
    public static final String CXF_VERSION_JDK7_BYTECODE = "3.1.15";//last version compiled with jdk 1.7
    public static final String DRIFT_PARENT_DIR = "drift";
    public static final String LIBTHRIF_VERSION = "0.11.0";

    /**
     * where is the list of services , is not set by plugin configuration
     */
    @Parameter(defaultValue = "${project.basedir}/src/main/resources/META-INF/drift-services.list", readonly = true)
    private File serviceListFile;

    @Parameter(defaultValue = "/drift", readonly = true)
    private String driftClass;

    /**
     * where drift class file compiled
     * this is not in plugin configuration tag
     */
    @Parameter(defaultValue = "${project.build.directory}/classes", readonly = true)
    private File directoryClassAbsolute;

    @Parameter( defaultValue = "${project}", readonly = true )
    private MavenProject mavenProject;


    /**
     * Project dir, this is not for plugin configuration tag
     */
    @Parameter( defaultValue ="${project.build.directory}",readonly = true )
    String projectDir;

    /**
     * The current Maven session.
     */
    @Parameter( defaultValue = "${session}", readonly = true )
    private MavenSession mavenSession;

    /**
     * The Maven BuildPluginManager component.
     */
    @Component
    private BuildPluginManager pluginManager;


    public void execute() throws MojoExecutionException, MojoFailureException {
        Log log = getLog();
        Utility utility = new Utility(log);
        try {
            if ( serviceListFile == null || !serviceListFile.isDirectory()   ) {
                Map<String, String> serviceNames = getServiceListFromFile(serviceListFile);
                try {
                    getLog().info("Search drift class in ... " + directoryClassAbsolute.getAbsolutePath());
//                    utility.getAllListClass(directoryClassAbsolute);
                    utility.loadClasses(directoryClassAbsolute);
                    getLog().debug("After Load drift classes:");
                    utility.getAllListClass(directoryClassAbsolute);
                    File outputDirectoryDrift = new File(directoryClassAbsolute, DRIFT_PARENT_DIR); // avoid to look into other no-java-class resources
                    //controllo che esistono classi con sintassi nomepackage1.nomepackage2...nomepackagen.nomeclasseService$Iface
                    Set<String> interfaceClassNames = new HashSet<>();
                    Path pathToClassesAbs = Paths.get(outputDirectoryDrift.toURI());
                    Set<String> outputclasses = new HashSet<>();
                    DriftFileVisitor driftFileVisitor
                            = new DriftFileVisitor(pathToClassesAbs, SERVICE_PATTERN_CLASS.pattern(), log, interfaceClassNames, outputclasses);//"drift."
                    try {
                        Files.walkFileTree(pathToClassesAbs, driftFileVisitor);
                    } catch (IOException e1)
                    {
                        getLog().error(e1.getMessage());
                        e1.printStackTrace();
                    }
                    try {
                        for (String className : outputclasses){
//                          //le classi prima di essere redefinite da buddy devono essere caricare nel class loader
                            Class clazz = Class.forName(className);
                            addMissingAnnotation(clazz, pathToClassesAbs);
                        }
                        //genero i wsdl
                        generateWsdl(interfaceClassNames,serviceNames);
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }


                } catch (Exception e) {
                    e.printStackTrace();
                    throw new IOException("Drift class not found! " + e.getMessage());
                }
            } else {
                throw new IOException(ERROR_MSG_SERVICE_LIST_MISSING);
            }
        }
        catch (IOException e) {
              log.error(e.getMessage());
              throw new MojoExecutionException(e.getMessage(),e);
        }
    }

    /**
     * Apache CXF java2ws classes rules (all classes, except enums type,  must respect these contracts):
     * if class adding @XmlAccessorType(XlmAccessType.FIELD) for class className and save (overwrite) class to disk
     * if interface  Iface adding @Webservice annotation and  save (overwrite) class to disk
     * For each class with a list, adding to list the annotation @XmlElementWrapper(name=) and @XmlElement(name)
     * @param className name of class loaded in this ClassLoader
     */
    private void addMissingAnnotation(Class className, Path _outputDirectory) {
        //addinbg annotation @XmlAccessorType(XmlAccessorType.FIELD) to class type
        try {
            getLog().info("class " + className.getCanonicalName() + "loaded, redefine it, adding missing annotations");
            List<AnnotationDescription> annotations = new ArrayList<>();
            //gli enum non hanno annotazioni, invece per altri tipi:
            if (!className.isEnum()) {
                //se è una interfaccia Iface deve avere l'annotazione @WebService
                if (className.isInterface() && className.getCanonicalName().endsWith("$Iface")) {
                    annotations.add(AnnotationDescription.Builder.ofType(WebService.class).build());
                }
                //altrimenti se è una classe di input output del servizio
                else {
                    // aggiungo sulla classe l'annotazione @XmlAccessorType(XmlAccessType.FIELD)
                    annotations.add(AnnotationDescription.Builder.ofType(XmlAccessorType.class).define("value", XmlAccessType.FIELD).build());
                }
                if (annotations.size() > 0) {
                    AnnotationDescription annotationsArray[] = new AnnotationDescription[annotations.size()];
                    new ByteBuddy(ClassFileVersion.JAVA_V7)
                            .redefine(className)
                            .annotateType(
                                    annotations.toArray(annotationsArray))
                            .field(ElementMatchers.fieldType(List.class)) //per ogni List nella classe aggiungo @XmlElementWrapper e @XmlElement
                            .annotateField(AnnotationDescription.Builder.ofType(XmlElementWrapper.class).define("name", "elementWrap1").build(),
                                    AnnotationDescription.Builder.ofType(XmlElement.class).define("name", "element1").build()
                            )
                            .make()
                            .saveIn(_outputDirectory.toFile()); //salvo la classe modificata sovrascrivendo quella compilata
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            getLog().error("Error adding annotations for class "  + className + "!");
        }
    }


    /**
     * chiamo plugin apache cxf
     * Per collagare il path al wsdl presente nel drift-service.list
     * il nome del wsdl deve essere pari al nome della classe che precede $Iface
     * es:
     * META-INF/wsdl/drift-thrift@SharedService.wsdl -> SharedService$Iface
     * (SharedService sarà la class name e SharedService.wsdl il nome del wsdl)
     * <plugin>
     <groupId>org.apache.cxf</groupId>
     <artifactId>cxf-java2ws-plugin</artifactId>
     <version>${cxf.version}</version>
     <dependencies>
     <dependency>
     <groupId>org.apache.cxf</groupId>
     <artifactId>cxf-rt-frontend-jaxws</artifactId>
     <version>${cxf.version}</version>
     </dependency>
     <dependency>
     <groupId>org.apache.cxf</groupId>
     <artifactId>cxf-rt-frontend-simple</artifactId>
     <version>${cxf.version}</version>
     </dependency>
     </dependencies>
     <executions>
     <execution>
     <id>process-classes</id>
     <phase>process-classes</phase>
     <configuration>
     <className>org.apache.hello_world.Greeter</className>
     <genWsdl>true</genWsdl>
     <verbose>true</verbose>
     </configuration>
     <goals>
     <goal>java2ws</goal>
     </goals>
     </execution>
     </executions>
     </plugin>
     * @param classNames
     * @param serviceNames
     */
    private void generateWsdl(Set<String> classNames, Map<String,String> serviceNames) throws MojoExecutionException {
        //chiamata plugin java2ws di apache cxf versione 3.1.15 (come fatto nell'altro goal Thrift2Java per chiamare il plugin thrift)
        //la fase non viene passata perchè è la stessa del goal
        for (String className : classNames){
            getLog().info("Found Service class " + className);
            int i = 0;
            Element[] elements = new Element[4];
            elements[i++] = new Element("className", className);
            elements[i++] = new Element("genWsdl",  Boolean.TRUE.toString());
            elements[i++] = new Element("verbose",  Boolean.TRUE.toString());
            String[] splitpackages = className.trim().split("\\.");
            String classNameInner = splitpackages[splitpackages.length-1];
            String classServiceName = classNameInner.substring(0, (classNameInner.indexOf("Iface")-1));
            String wsdlOutputFile = serviceNames.get(classServiceName);
            getLog().info("take class interface "+classServiceName + ", will output to " + wsdlOutputFile );
            elements[i++] = new Element("outputFile",  projectDir + "/resources/" + wsdlOutputFile);
            executeMojo(
                    plugin(
                            groupId("org.apache.cxf"),
                            artifactId("cxf-java2ws-plugin"),
                            version(CXF_VERSION_JDK7_BYTECODE),
                            dependencies(
                                    dependency("org.apache.cxf", "cxf-rt-frontend-jaxws"  , CXF_VERSION_JDK7_BYTECODE),
                                    dependency("org.apache.cxf", "cxf-rt-frontend-simple"  , CXF_VERSION_JDK7_BYTECODE),
                                    dependency("org.apache.thrift","libthrift", LIBTHRIF_VERSION)
                            )
                    ),
                    goal("java2ws"),
                    configuration(
                            elements
                    ),
                    executionEnvironment(
                            mavenProject,
                            mavenSession,
                            pluginManager
                    )
            );
        }


    }

    private Map<String,String> getServiceListFromFile(File serviceListFile) throws IOException {
        Map<String,String> services = new HashMap<>();
        if (serviceListFile.exists()){
            FileReader fileInputStream = new FileReader(serviceListFile);
            BufferedReader bf = new BufferedReader(fileInputStream);
            try {
                String line = bf.readLine();
                while (line != null){
                    String servicePathFileName = line.trim();
                    // META-INF/wsdl/drift-thrift@SharedService.wsdl ->  SharedService
                    String serviceName = servicePathFileName.substring(servicePathFileName.lastIndexOf("@")+1,servicePathFileName.lastIndexOf(".wsdl") );
                    services.put(serviceName, servicePathFileName);
                    getLog().debug(serviceName + " -> " + servicePathFileName);
                    line = bf.readLine();
                }
            } catch (IOException e) {
                throw new IOException(ERROR_MSG_SERVICE_LIST_MISSING);
            } finally {
                if (fileInputStream != null){
                    fileInputStream.close();
                    fileInputStream=null;
                    bf = null;
                }
            }

        }
        else throw  new FileNotFoundException(ERROR_MSG_SERVICE_LIST_MISSING);
        return services;

    }


//    @Parameter(property = "project.build.outputDirectory", required = true)
//    private String classpath;
//
//    @Parameter(property = "project.compileClasspathElements", required = true)
//    private List<?> classpathElements;

//    private String getPathElement(String classpath) {
//        String res = classpath;
//        if (!classpath.startsWith("/")) {
//            res = "/" + res;
//        }
//        return res;
//    }
//
//    public void execute2() throws MojoExecutionException, MojoFailureException {
//        final List<URL> urls = new ArrayList<>();
//        try {
//            urls.add(new URL("file://" + getPathElement(classpath) + "/"));
//            for (Object classPathElement : classpathElements) {
//                urls.add(new URL("file://" + getPathElement(classPathElement.toString())));
//            }
//        } catch (MalformedURLException e) {
//            throw new MojoExecutionException("Failed to add classpath to classloader", e);
//        }
//
//        getLog().debug("Services classpath:");
//        for (URL url : urls) {
//            getLog().debug(url.toString());
//        }
//
//        final List<String> classesNames = new ArrayList<>();
//
//        Path classesPath = FileSystems.getDefault().getPath(classpath);
//        try {
//            Files.walkFileTree(classesPath, new FileVisitor<Path>() {
//                @Override
//                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
//                    return FileVisitResult.CONTINUE;
//                }
//
//                @Override
//                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
//                    try {
//                        if (file != null && file.toString().endsWith(".class")) {
//                            classesNames.add(file.toString().substring(classpath.length() + 1, file.toString().length() - 6).replace('/', '.').replace('\\', '.'));
//                        }
//                    } catch (Exception e) {
//                        getLog().warn("Cannot add " + file + " to the classes: " + e.getMessage());
//                        getLog().debug(e);
//                    }
//
//                    return FileVisitResult.CONTINUE;
//                }
//
//                @Override
//                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
//                    return FileVisitResult.CONTINUE;
//                }
//
//                @Override
//                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
//                    return FileVisitResult.CONTINUE;
//                }
//            });
//        } catch (IOException e) {
//            getLog().warn("Cannot scan the classpath " + classesPath + ": " + e.getMessage());
//            getLog().debug(e);
//        }
//
//        ClassLoader loader = new URLClassLoader(urls.toArray(new URL[urls.size()]));
//
//        getLog().debug("Scanning for services...");
//
//        for (String className : classesNames) {
//            try {
//                getLog().debug("Checking " + className);
//                getLog().info("Checking " + className);
//                Class clazz = loader.loadClass(className);
//                if (clazz != null && clazz.isInterface()) {
//                    getLog().info("Found!!!!!!");
//                    getLog().info(clazz.getName());
//
//                }
//            } catch (ClassNotFoundException e) {
//                throw new MojoExecutionException(String.format("Failed to load class %s", className), e);
//            }
//        }
//    }


}
