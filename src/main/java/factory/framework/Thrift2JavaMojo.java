package factory.framework;

/**
 * Plugin thrift2Java
 * vedi readme.md
 */

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;



/**
 * Goal which touches a timestamp file.
 *
 * @goal java
 * 
 * @phase from process-sources to compile
 */

@Mojo(name = "genjava")
public class Thrift2JavaMojo extends AbstractMojo {

    public void execute() throws MojoExecutionException, MojoFailureException {

            throw new MojoExecutionException("todo: implementare execute");
    }
}

