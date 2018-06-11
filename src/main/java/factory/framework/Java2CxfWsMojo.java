package factory.framework;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.*;
import java.util.ArrayList;
import java.util.List;


/**
 * genera i wsdl dei servizi
 * la lista dei servizi e nomi wsdl deve essere messa in META-INF/druft-service.list
 */
@Mojo(name = "Java2CxfWs", defaultPhase = LifecyclePhase.COMPILE)
public class Java2CxfWsMojo extends AbstractMojo {

    public static final String ERROR_MSG_SERVICE_LIST_MISSING = "Error: services list missing! Put service list in ${project.basedir}/src/main/resources/META-INF/drift-services.list";

    /**
     * where is the list of services
     */
    @Parameter(defaultValue = "${project.basedir}/src/main/resources/META-INF/drift-services.list", readonly = true)
    private File serviceListFile;


    public void execute() throws MojoExecutionException, MojoFailureException {
        Log log = getLog();
        try {
            if ( serviceListFile == null || !serviceListFile.isDirectory()   ) {
                List<String> serviceNmaes = getServiceListFromFile(serviceListFile);

            } else {
                throw new IOException(ERROR_MSG_SERVICE_LIST_MISSING);
            }
        }
        catch (IOException e) {
              log.error(e.getMessage());   
        }
    }

    private List<String> getServiceListFromFile(File serviceListFile) throws IOException {
        List<String> services = new ArrayList<String>();
        if (serviceListFile.exists()){
            FileReader fileInputStream = new FileReader(serviceListFile);
            BufferedReader bf = new BufferedReader(fileInputStream);
            try {
                String line = bf.readLine();
                while (line != null){
                    getLog().info(line);

                    services.add(line);
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
