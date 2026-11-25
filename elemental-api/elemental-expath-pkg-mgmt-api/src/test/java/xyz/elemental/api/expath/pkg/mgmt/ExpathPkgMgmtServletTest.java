package xyz.elemental.api.expath.pkg.mgmt;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.fluent.Request;
import org.exist.test.ExistWebServer;
import org.junit.ClassRule;
import org.junit.Test;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;

import javax.xml.transform.Source;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ExpathPkgMgmtServletTest {

    @ClassRule
    public static final ExistWebServer existWebServer = new ExistWebServer(true, false, true, true);

    private static String getPackageUri() {
        return "http://localhost:" + existWebServer.getPort() + "/expath/package/";
    }

    @Test
    public void packageQuery() throws IOException {
        final HttpResponse response = Request.Get(getPackageUri())
                .execute()
                .returnResponse();
        final int resultStatusCode = response.getStatusLine().getStatusCode();
        assertEquals("Server returned response code: " + resultStatusCode, HttpStatus.SC_OK, resultStatusCode);
        final String responseBody = readResponse(response.getEntity());

        final Source expectedSource = Input.from("<package-list xmlns=\"http://expath.org/ns/pkg-api\" spec=\"1.0.0\"/>").build();
        final Source actualSource = Input.from(responseBody).build();
        final Diff diff = DiffBuilder.compare(expectedSource)
                .withTest(actualSource)
                .checkForSimilar()
                .build();

        assertFalse(diff.toString(), diff.hasDifferences());
    }

    private String readResponse(final HttpEntity response) throws IOException {
        try (final InputStream is = response.getContent();
             final BufferedReader reader = new BufferedReader(new InputStreamReader(is, UTF_8))) {
            String line;
            final StringBuilder out = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                out.append(line);
                out.append("\r\n");
            }
            return out.toString();
        }
    }
}