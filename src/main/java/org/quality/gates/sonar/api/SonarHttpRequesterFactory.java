package org.quality.gates.sonar.api;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.quality.gates.jenkins.plugin.GlobalConfigDataForSonarInstance;
import org.quality.gates.sonar.api5x.SonarHttpRequester5x;
import org.quality.gates.sonar.api60.SonarHttpRequester60;
import org.quality.gates.sonar.api61.SonarHttpRequester61;
import org.quality.gates.sonar.api80.SonarHttpRequester80;
import org.quality.gates.sonar.api88.SonarHttpRequester88;

import java.io.IOException;

/**
 * @author arkanjoms
 * @since 1.0.1
 */
class SonarHttpRequesterFactory {

    private static final String SONAR_API_SERVER_VERSION = "/api/server/version";

    static SonarHttpRequester getSonarHttpRequester(GlobalConfigDataForSonarInstance globalConfigDataForSonarInstance) {

        try {
            HttpGet request = new HttpGet(getSonarApiServerVersion(globalConfigDataForSonarInstance));

            HttpClientContext context = HttpClientContext.create();
            CloseableHttpClient client = HttpClientBuilder.create().build();
            CloseableHttpResponse response = client.execute(request, context);
            String sonarVersion = EntityUtils.toString(response.getEntity());

            int sonarVersionMajor = majorSonarVersion(sonarVersion);
            int sonarVersionMinor = minorSonarVersion(sonarVersion);
            SonarHttpRequester requester;

            if (sonarVersionMajor <= 5) {
                requester = new SonarHttpRequester5x();
            } else {
                if (sonarVersionMajor == 6 && sonarVersionMinor == 0) {
                    requester = new SonarHttpRequester60();
                } else if ((sonarVersionMajor == 6 && sonarVersionMinor >= 1) ||
                        sonarVersionMajor == 7 ) {
                    requester = new SonarHttpRequester61();
                } else if (sonarVersionMajor == 8 && sonarVersionMinor < 8) {
                    requester = new SonarHttpRequester80();
                } else if (sonarVersionMajor == 8) {
                    requester = new SonarHttpRequester88();
                } else {
                    throw new UnsuportedVersionException("Plugin doesn't suport this version of sonar api! Please contact the developer.");
                }
            }

            return requester;
        } catch (IOException e) {
            throw new ApiConnectionException(e.getLocalizedMessage());
        }
    }

    private static String getSonarApiServerVersion(GlobalConfigDataForSonarInstance globalConfigDataForSonarInstance) {

        return globalConfigDataForSonarInstance.getSonarUrl() + SONAR_API_SERVER_VERSION;
    }

    private static int majorSonarVersion(String sonarVersion) {

        return Integer.parseInt(sonarVersion.split("\\.")[0]);
    }

    private static int minorSonarVersion(String sonarVersion) {

        return Integer.parseInt(sonarVersion.split("\\.")[1]);
    }
}
