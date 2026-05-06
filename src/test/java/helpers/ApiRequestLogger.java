package helpers;

import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;

/**
 * REST Assured filter that logs every API request and response to stdout.
 * Surefire captures stdout into its XML reports, so this data appears
 * automatically in target/surefire-reports/.
 */
public class ApiRequestLogger implements Filter {

    private static final String SEPARATOR = "═".repeat(80);
    private static final String THIN_SEP = "─".repeat(80);

    @Override
    public Response filter(FilterableRequestSpecification requestSpec,
                           FilterableResponseSpecification responseSpec,
                           FilterContext ctx) {

        long startTime = System.currentTimeMillis();
        Response response = ctx.next(requestSpec, responseSpec);
        long duration = System.currentTimeMillis() - startTime;

        StringBuilder sb = new StringBuilder();
        sb.append('\n').append(SEPARATOR).append('\n');

        // -- REQUEST --
        sb.append("► REQUEST: ").append(requestSpec.getMethod())
          .append(' ').append(requestSpec.getURI()).append('\n');
        sb.append(THIN_SEP).append('\n');

        sb.append("  Headers:\n");
        requestSpec.getHeaders().forEach(h ->
                sb.append("    ").append(h.getName()).append(": ").append(h.getValue()).append('\n'));

        String reqBody = requestBodyToString(requestSpec);
        if (reqBody != null && !reqBody.isBlank()) {
            sb.append("  Body:\n");
            sb.append("    ").append(reqBody).append('\n');
        }

        // -- RESPONSE --
        sb.append(THIN_SEP).append('\n');
        sb.append("◄ RESPONSE: ").append(response.getStatusCode())
          .append(' ').append(response.getStatusLine()).append('\n');
        sb.append("  Duration: ").append(duration).append(" ms\n");

        sb.append("  Headers:\n");
        response.getHeaders().forEach(h ->
                sb.append("    ").append(h.getName()).append(": ").append(h.getValue()).append('\n'));

        String resBody = safeResponseBody(response);
        if (resBody != null && !resBody.isBlank()) {
            sb.append("  Body:\n");
            sb.append("    ").append(resBody).append('\n');
        }

        sb.append(SEPARATOR);
        System.out.println(sb);

        return response;
    }

    private static String requestBodyToString(FilterableRequestSpecification spec) {
        try {
            Object body = spec.getBody();
            return body != null ? body.toString() : null;
        } catch (Exception e) {
            return "<unable to read request body>";
        }
    }

    private static String safeResponseBody(Response response) {
        try {
            return response.getBody().asString();
        } catch (Exception e) {
            return "<unable to read response body>";
        }
    }
}
