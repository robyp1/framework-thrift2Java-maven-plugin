package factory.framework;


import org.apache.maven.plugin.testing.AbstractMojoTestCase;

import java.io.File;


public class Thrift2JavaTest extends AbstractMojoTestCase {


    private static final String GEN_JAVA_PLUGIN_CONFIG_PATH = "src/test/resources/unit/project-to-test/pom.xml";

    /** {@inheritDoc} */
    protected void setUp()
            throws Exception
    {
        super.setUp();

    }

    /** {@inheritDoc} */
    protected void tearDown()
            throws Exception
    {
        // required
        super.tearDown();

    }

    /**
     * @throws Exception if any
     */
    public void testSomething()
            throws Exception
    {
        File testPom = new File(getBasedir(), GEN_JAVA_PLUGIN_CONFIG_PATH);
        assertNotNull( testPom );
        assertTrue( testPom.exists() );

        Thrift2JavaMojo myMojo = (Thrift2JavaMojo) lookupMojo( "genjava", testPom );
        assertNotNull( myMojo );
        myMojo.execute();

    }
}
