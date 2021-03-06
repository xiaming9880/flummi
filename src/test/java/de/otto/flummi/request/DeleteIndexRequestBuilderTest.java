package de.otto.flummi.request;

import de.otto.flummi.CompletedFuture;
import de.otto.flummi.InvalidElasticsearchResponseException;
import de.otto.flummi.MockResponse;
import de.otto.flummi.response.HttpServerErrorException;
import de.otto.flummi.util.HttpClientWrapper;
import org.asynchttpclient.BoundRequestBuilder;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

public class DeleteIndexRequestBuilderTest {

    private HttpClientWrapper httpClient;
    private DeleteIndexRequestBuilder testee;

    @BeforeMethod
    private void setup() {
        httpClient = mock(HttpClientWrapper.class);
    }

    @Test
    public void shouldDeleteIndex() {
        testee = new DeleteIndexRequestBuilder(httpClient, Stream.of("someIndexName"));
        BoundRequestBuilder boundRequestBuilderMock = mock(BoundRequestBuilder.class);

        when(httpClient.prepareDelete("/someIndexName")).thenReturn(boundRequestBuilderMock);
        when(boundRequestBuilderMock.execute()).thenReturn(new CompletedFuture(new MockResponse(200, "ok", "")));
        when(boundRequestBuilderMock.addHeader(anyString(),anyString())).thenReturn(boundRequestBuilderMock);
        testee.execute();
        verify(httpClient).prepareDelete("/someIndexName");
    }

    @Test
    public void shouldDeleteMultipleIndices() throws Exception {
        testee = new DeleteIndexRequestBuilder(httpClient, Stream.of("someIndexName", "someOtherIndex"));
        BoundRequestBuilder boundRequestBuilderMock = mock(BoundRequestBuilder.class);

        when(httpClient.prepareDelete("/someIndexName,someOtherIndex")).thenReturn(boundRequestBuilderMock);
        when(boundRequestBuilderMock.execute()).thenReturn(new CompletedFuture(new MockResponse(200, "ok", "")));
        when(boundRequestBuilderMock.addHeader(anyString(),anyString())).thenReturn(boundRequestBuilderMock);
        testee.execute();
        verify(httpClient).prepareDelete("/someIndexName,someOtherIndex");
    }

    @Test(expectedExceptions = HttpServerErrorException.class)
    public void shouldThrowExceptionIfStatusCodeNotOk() {
        testee = new DeleteIndexRequestBuilder(httpClient, Stream.of("someIndexName"));
        BoundRequestBuilder boundRequestBuilderMock = mock(BoundRequestBuilder.class);
        when(httpClient.prepareDelete("/someIndexName")).thenReturn(boundRequestBuilderMock);
        when(boundRequestBuilderMock.execute()).thenReturn(new CompletedFuture(new MockResponse(400, "not ok", "")));
        when(boundRequestBuilderMock.addHeader(anyString(),anyString())).thenReturn(boundRequestBuilderMock);
        try {
            testee.execute();
        } catch (HttpServerErrorException e) {
            assertThat(e.getStatusCode(), is(400));
            assertThat(e.getResponseBody(), is(""));
            throw e;
        }
    }

    @Test
    public void shouldNotDeleteIndexForAcknowledgedFalse() throws ExecutionException, InterruptedException, IOException {
        BoundRequestBuilder boundRequestBuilderMock = mock(BoundRequestBuilder.class);
        when(httpClient.prepareDelete("/someIndexName")).thenReturn(boundRequestBuilderMock);
        when(boundRequestBuilderMock.execute()).thenReturn(new CompletedFuture(new MockResponse(200, "OK", "{\"acknowledged\":\"false\"}")));
        when(boundRequestBuilderMock.addHeader(anyString(),anyString())).thenReturn(boundRequestBuilderMock);
        try {
            testee.execute();
        } catch (InvalidElasticsearchResponseException e) {
            assertThat(e.getMessage(), is("{\"acknowledged\":\"false\"}"));
            throw e;
        }
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void shouldNotInvokeHttpClientWhenNoIndicesAreGivenForDeletion() throws Exception {
        testee = new DeleteIndexRequestBuilder(httpClient, Stream.of());
        testee.execute();
        verifyZeroInteractions(httpClient);
    }
}