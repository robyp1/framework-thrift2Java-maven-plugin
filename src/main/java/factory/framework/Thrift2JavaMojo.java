package factory.framework;

/**
 * Plugin thrift2Java
 *
 *
 *

 */

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

/**
 * Generate java source from Thrift files , before compile phase( phase generate-sources in maven build lifecycle)
 * This is executed after begin of validate phase (live < phase >empty in plugin execution)
 * @link https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html
 * Install 'thrift.exe' in C:/Program Files (x86)/Apache/thrift/bin/thrift or
 * set thriftExecutable in configuration plugin tag.
 *
 * The default output java dir is "${project.basedir}/target/generated-sources/drift
 * (dove ci sono i sorgenti java generati dal thrift plugin)
 *
 * Plugin must have this configuaration,
 * this goal(generate resources) and the other (PROCCESS_CLASS) are within compile or clean install pahse
 * @code{
 * 		<plugin>
        <groupId>factory.framework</groupId>
        <artifactId>framework-thrift2Java-maven-plugin</artifactId>
        <version>2.6.0-SNAPSHOT</version>
        <configuration>
        <thriftExecutable>C:/Program Files (x86)/Apache/thrift/bin/thrift</thriftExecutable>
        <thriftSourceRoot>${project.basedir}/src/main/thrift</thriftSourceRoot>
        <!--<outputDirectory>${project.basedir}/src/main/java/drift</outputDirectory>-->
        </configuration>
        <executions>
        <execution>
        <!--<phase>non serve</phase>-->
        <goals>
        <goal>genjava</goal> <!--indicare entrambi le fasi verranno seguite in sequenza in fase di clean install o compile-->
        <goal>Java2Ws</goal>
        </goals>
        </execution>
        </executions>
        </plugin>
 }
 */

@Mojo(name = "genjava", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class Thrift2JavaMojo extends AbstractMojo {


    /*******************************************************************************************
     * ********************** READ-ONLY PARAMS, DO NOT USE IN PLUGIN CONFIGURATION
     * ****************************************************************************************
     */
    /**
     * The project currently being build.
     */
    @Parameter( defaultValue = "${project}", readonly = true )
    private MavenProject mavenProject;

    /**
     * The current Maven session.
     */
    @Parameter( defaultValue = "${session}", readonly = true )
    private MavenSession mavenSession;


    /**
     * where to put generated java files
     */
    @Parameter (defaultValue = "${project.basedir}/target/generated-sources", readonly = true)
    private String outputDirectory;


    /*******************************************************************************************
     * ********************** PLUGIN PARAMS, USE OPTIONALLY IN PLUGIN CONFIGURATION
     * ****************************************************************************************
     */

    /**
     * where put thrift files to convert
     */
    @Parameter(defaultValue = "${project.basedir}/src/main/thrift")
    private String thriftSourceRoot;

    /**
     * where are the executable file path ending with filename (without extensione .exe or .sh)
     */
    @Parameter(defaultValue = "C:/Program Files (x86)/Apache/thrift/bin/thrift")
    private String thriftExecutable;


    /**
     * The Maven BuildPluginManager component.
     */
    @Component
    private BuildPluginManager pluginManager;



    public void execute() throws MojoExecutionException, MojoFailureException {
        Log log = getLog();
        log.info("Executing genjava " + " thriftExecutable: "  + thriftExecutable
        + " reading thrift from dir: " + thriftSourceRoot + " write java output to dir " + outputDirectory);
        thriftPluginExecution();
    }


    /**
     * Esegue questo plugin
     * @code {
         <plugin>
         <groupId>org.apache.thrift</groupId>
         <artifactId>thrift-maven-plugin</artifactId>
         <version>1.0-SNAPSHOT</version>
         <configuration>
         <thriftExecutable>C:/Program Files (x86)/Apache/thrift/bin/thrift</thriftExecutable>
         <thriftSourceRoot>${project.basedir}/src/main/thrift</thriftSourceRoot>
         </configuration>
         <executions>
         <execution>
         <id>thrift-sources</id>
         <phase>generate-sources</phase>
         <goals>
         <goal>compile</goal>
         </goals>
         </execution>
         <execution>
         <id>thrift-test-sources</id>
         <phase>generate-test-sources</phase>
         <goals>
         <goal>testCompile</goal>
         </goals>
         </execution>
         </executions>
         </plugin>
        }
     *
     * @throws MojoExecutionException
     */
    public void thriftPluginExecution () throws MojoExecutionException {
        executeMojo(
                plugin(
                        groupId("org.apache.thrift"),
                        artifactId("thrift-maven-plugin"),
                        version("1.0-SNAPSHOT")
                ),
                goal("compile"),
                configuration(
                        element("thriftExecutable", thriftExecutable),
                        element("thriftSourceRoot", thriftSourceRoot),
                        element("outputDirectory", outputDirectory)
                ),
                executionEnvironment(
                        mavenProject,
                        mavenSession,
                        pluginManager
                )
        );
    }
}

