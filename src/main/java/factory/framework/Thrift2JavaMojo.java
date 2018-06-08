package factory.framework;

/**
 * Plugin thrift2Java
 * vedi readme.md
 *
 *

 */

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

/**
 *
 * @goal java
 * 
 * @phase from process-sources to compile
 */

@Mojo(name = "genjava")
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
    @Parameter (defaultValue = "${project.basedir}/src/main/java/drift", readonly = true)
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
        thriftPluginExecution();

//      throw new MojoExecutionException("todo: implementare execute");
    }


    /**
     * Esegue questo plugin
     <plugin>
     <groupId>org.apache.thrift</groupId>
     <artifactId>thrift-maven-plugin</artifactId>
     <version>1.0-SNAPSHOT</version>
     <configuration>
     <thriftExecutable>C:/Program Files (x86)/Apache/thrift/bin/thrift</thriftExecutable>
     <outputDirectory>${project.basedir}/src/main/java/drift</outputDirectory>
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
     *
     * @throws MojoExecutionException
     */
    public void thriftPluginExecution () throws MojoExecutionException {
        executeMojo(
                plugin(
                        groupId("org.apache.thrift.tools"),
                         artifactId("maven-thrift-plugin"),
                        version("0.1.11")
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

