package com.matsurvpark.functions;

import com.microsoft.azure.functions.*;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.*;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class MatsuRvParkTest {

    @Test
    public void testParseEmail() throws Exception {
        // Setup
        @SuppressWarnings("unchecked")
        final HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);

      //  final Map<String, String> queryParams = new HashMap<>();
      //  queryParams.put("name", "Azure");
      //  doReturn(queryParams).when(req).getQueryParameters();

        final Optional<String> queryBody = Optional.of("""
            <table>
            <tbody>
                <tr><th align="left">Number of Adults</th><td>1</td></tr>
                <tr><th align="left">Arrival Date</th><td>2024-06-20</td></tr>
                <tr><th align="left">Departure Date</th><td>2024-06-27</td></tr>
                <tr><th align="left">Type of Resrvation</th><td>RV Reservation</td></tr>
                <tr><th align="left">RV Length in Feet</th><td>45</td></tr>
                <tr><th align="left">Full Name</th><td>jordan</td></tr>
                <tr><th align="left">Contact Email</th><td>reg_jordanjankowski@icloud.com</td></tr>
                <tr><th align="left">Contact Phone Number</th><td>6166349817</td></tr>
                <tr><th align="left">Additional Information</th><td>fuck off</td></tr>
            </tbody>
            </table>
        """);
        doReturn(queryBody).when(req).getBody();

        doAnswer(new Answer<HttpResponseMessage.Builder>() {
            @Override
            public HttpResponseMessage.Builder answer(InvocationOnMock invocation) {
                HttpStatus status = (HttpStatus) invocation.getArguments()[0];
                return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
            }
        }).when(req).createResponseBuilder(any(HttpStatus.class));

        final ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();

        // Invoke
        final HttpResponseMessage ret = new MatsuRvPark().parse(req, context);

        // Verify
        assertEquals(ret.getStatus(), HttpStatus.OK);
    }

}
