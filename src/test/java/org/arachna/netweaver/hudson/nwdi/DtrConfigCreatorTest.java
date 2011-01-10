/**
 *
 */
package org.arachna.netweaver.hudson.nwdi;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import hudson.FilePath;
import hudson.Util;

import java.io.File;
import java.io.IOException;

import org.arachna.netweaver.dc.types.DevelopmentConfiguration;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Unit tests for {@link DtrConfigCreator}.
 * 
 * @author Dirk Weigenand
 */
public final class DtrConfigCreatorTest {
    /**
     * URL to build server.
     */
    private static final String BUILD_SERVER_URL = "http://di0db.example.com:53000";

    /**
     * object under test.
     */
    private DtrConfigCreator configCreator;

    /**
     * workspace to use for tests.
     */
    private File workspace;

    /**
     * development configuration used during test.
     */
    private DevelopmentConfiguration config;

    /**
     * Set up the fixture used during test.
     * 
     * @throws IOException
     *             when creating the temporary directory used as workspace or
     *             sub folders in it fail
     * @throws InterruptedException
     *             might be thrown from FilePath operations
     */
    @Before
    public void setUp() throws IOException, InterruptedException {
        this.workspace = Util.createTempDir();
        config = new DevelopmentConfiguration("DI0_testTrack_D");
        config.setBuildServer(BUILD_SERVER_URL);

        this.configCreator =
            new DtrConfigCreator(new FilePath(this.workspace), config,
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?><confdef />");
        this.configCreator.execute();
    }

    /**
     * Tear down test fixture.
     * 
     * Remote workspace used for testing.
     * 
     * @throws IOException
     *             when removing the workspace fails.
     */
    @After
    public void tearDown() throws IOException {
        this.config = null;
        this.configCreator = null;

        Util.deleteRecursive(this.workspace);
    }

    /**
     * Assert that the various configuration files are created with correct
     * content.
     * 
     * Test method for
     * {@link org.arachna.netweaver.hudson.nwdi.DtrConfigCreator#execute()}.
     */
    @Test
    public void testServersXml() {
        final FilePath workspace = new FilePath(this.workspace);
        final FilePath dotDtc = workspace.child(DtrConfigCreator.DOT_DTC);

        assertFilePathExists(dotDtc);

        final FilePath dotDtr = workspace.child(DtrConfigCreator.DOT_DTR);
        assertFilePathExists(dotDtr);

        final FilePath serversXml = dotDtr.child(DtrConfigCreator.SERVERS_XML);
        assertFilePathExists(serversXml);
        assertContent(serversXml, String.format("/servers/server[@url = '%s']", BUILD_SERVER_URL));
    }

    /**
     * Assert that the various configuration files are created with correct
     * content.
     * 
     * Test method for
     * {@link org.arachna.netweaver.hudson.nwdi.DtrConfigCreator#execute()}.
     */
    @Test
    public void testClientsXml() {
        final FilePath workspace = new FilePath(this.workspace);
        final FilePath dotDtr = workspace.child(DtrConfigCreator.DOT_DTR);
        assertFilePathExists(dotDtr);

        final FilePath dotDtc = workspace.child(DtrConfigCreator.DOT_DTC);
        assertFilePathExists(dotDtc);

        final FilePath clientsXml = dotDtr.child(DtrConfigCreator.CLIENTS_XML);
        assertFilePathExists(clientsXml);
        assertContent(clientsXml, String.format("/clients/client[@name = '%s']", this.config.getName()));
        assertContent(clientsXml, String.format("/clients/client[@logicalSystem = '%s']", this.config.getName()));
        assertContent(clientsXml,
            String.format("/clients/client[@absoluteLocalRoot = '%s']", this.configCreator.makeAbsolute(dotDtc)));
    }

    /**
     * Assert that the given <code>FilePath</code> contains content that is
     * matched by the given XPath expression.
     * 
     * @param path
     *            the <code>FilePath</code> whose contents is to be tested.
     * @param xPath
     *            the XPath expression to be used for testing.
     */
    private void assertContent(final FilePath path, final String xPath) {
        try {
            final String content = path.readToString();
            final Document document = XMLUnit.buildControlDocument(content);
            final XpathEngine engine = XMLUnit.newXpathEngine();
            assertTrue(String.format("xpath '%s' did not match in document '%s'.", xPath, content), engine
                .getMatchingNodes(xPath, document).getLength() == 1);
        }
        catch (SAXException e) {
            fail(e.getMessage());
        }
        catch (IOException e) {
            fail(e.getMessage());
        }
        catch (XpathException e) {
            fail(e.getMessage() + "'" + xPath + "'");
        }
    }

    /**
     * Assert that the given <code>FilePath</code> exists.
     * 
     * @param path
     *            the <code>FilePath</code> to be tested for existance.
     */
    private void assertFilePathExists(final FilePath path) {
        try {
            if (!path.exists()) {
                fail("Assert failed: Path " + path.getName() + " does not exist.");
            }
        }
        catch (IOException e) {
            fail(e.getMessage());
        }
        catch (InterruptedException e) {
            fail(e.getMessage());
        }

    }
}