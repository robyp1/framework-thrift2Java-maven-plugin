package factory.framework;

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

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;


/**
 *
 * Generate wsdl descriptor from service class after maven compile phase (process classes pahse)
 * @link https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html
 * This is done after compile phase
 * services belongs to package: drift.<progetto>.<modulo>.api.<servizio>.nomeService.class
 * ex :
 * in generated-sources/drift there are the java sources and then:
 *                 drift.drift.thrift.api.shared.SharedService.java -> its the service with $Iface (inner interface Iface)
 *
 * so the command line is (-cp is not required when you are in the classpath):
 *    "C:\Program Files (x86)\Apache\cxf\apache-cxf-3.2.2\bin\java2ws" \
 *    -cp "./target/drift-thrift-api-1.0.1-SNAPSHOT.jar;C:\Progetti\.m2\repository\org\apache\thrift\libthrift\0.11.0\libthrift-0.11.0.jar" \
 *    -verbose \
 *    -wsdl \
 *    -o Shared.wsdl \
 *    drift.drift.thrift.api.shared.SharedService$Iface
 *
 * The services list with wsdl names must be in META-INF/druft-service.list
 *
 * Class with inner interface Iface
 * public class SharedService {
 *
 *   public interface Iface {

 */
@Mojo(name = "Java2Ws", defaultPhase = LifecyclePhase.PROCESS_CLASSES)
public class Java2CxfWsMojo extends AbstractMojo {

    public static final String ERROR_MSG_SERVICE_LIST_MISSING = "Error: services list missing! Put service list in ${project.basedir}/src/main/resources/META-INF/drift-services.list";
    public static final String $_IFACE = "$Iface";
    //senza Service obbligatorio
    public static final String CLASSNAME_SERVICE_OPTIONALLY_$_IFACE = "(\\w+\\.{1})*(\\w)+(Service)*(\\$Iface){1}";
    //con Service obbligatorio
    public static final String CLASSNAME_SERVICE_$_IFACE = "(\\w+\\.{1})*(\\w+Service\\$Iface)+";
    public static final Pattern SERVICE_PATTERN_CLASS = Pattern.compile(CLASSNAME_SERVICE_$_IFACE);

    /**
     * where is the list of services , is not set by plugin configuration
     */
    @Parameter(defaultValue = "${project.basedir}/src/main/resources/META-INF/drift-services.list", readonly = true)
    private File serviceListFile;

    @Parameter(defaultValue = "/drift", readonly = true)
    private String driftClass;

    @Parameter(defaultValue = "${project.build.directory}/classes/drift", readonly = true)
    private File directoryClassAbsolute;

    @Parameter( defaultValue = "${project}", readonly = true )
    private MavenProject mavenProject;

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
        try {
            if ( serviceListFile == null || !serviceListFile.isDirectory()   ) {
                List<String> serviceNames = getServiceListFromFile(serviceListFile);
                try {
                    getLog().info("Search drift class in ... " + directoryClassAbsolute.getAbsolutePath());
                    Utility utility = new Utility(getLog());
                    //carico le classi drift insieme a quelle del plugin
                    utility.loadClasses(directoryClassAbsolute, URLClassLoader.class);
                    File outputDirectoryDrift = new File(directoryClassAbsolute, "drift"); // avoid to look into possible META-INF and other no-java-class resources
                    //controllo che esistono classi con sintassi nomepackage1.nomepackage2...nomepackagen.nomeclasseService$Iface
                    Set<String> classNames = utility.getClassNames(outputDirectoryDrift, directoryClassAbsolute, SERVICE_PATTERN_CLASS.pattern());
                    for (String className: classNames) {
                        log.info("Found Service class " + className);
                    }
                    generateWsdl(classNames,serviceNames);
                } catch (Exception e) {
//                    e.printStackTrace();
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
     * chiamo plugin apache cxf
     * <plugin>
     <groupId>org.apache.cxf</groupId>
     <artifactId>cxf-java2ws-plugin</artifactId>
     <version>3.2.2</version>
     <dependencies>
     <dependency>
     <groupId>org.apache.cxf</groupId>
     <artifactId>cxf-rt-frontend-jaxws</artifactId>
     <version>3.2.2</version>
     </dependency>
     <dependency>
     <groupId>org.apache.cxf</groupId>
     <artifactId>cxf-rt-frontend-simple</artifactId>
     <version>3.2.2</version>
     </dependency>
     </dependencies>
     </plugin>
     * @param classNames
     * @param serviceNames
     */
    private void generateWsdl(Set<String> classNames, List<String> serviceNames) {
       //TODO: chiamare plugin java2ws di apache cxf versione 3.2.2 (come fatto nell'altro goal Thrift2Java per chiamre il plugin thrift)
//        executeMojo(
//                plugin(
//                        groupId(""),
//                        artifactId(""),
//                        version("")
//                ),
//                goal("compile"),
//                configuration(
//                        element("thriftExecutable", ),
//                        element("thriftSourceRoot", ),
//                        element("outputDirectory", )
//                ),
//                executionEnvironment(
//                        mavenProject,
//                        mavenSession,
//                        pluginManager
//                )
//        );

    }

    private List<String> getServiceListFromFile(File serviceListFile) throws IOException {
        List<String> services = new ArrayList<String>();
        if (serviceListFile.exists()){
            FileReader fileInputStream = new FileReader(serviceListFile);
            BufferedReader bf = new BufferedReader(fileInputStream);
            try {
                String line = bf.readLine();
                while (line != null){
                    String serviceName = line.trim();
                    services.add(serviceName);
                    getLog().info(serviceName);
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


}