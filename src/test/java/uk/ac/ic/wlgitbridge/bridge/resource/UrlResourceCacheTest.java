package uk.ac.ic.wlgitbridge.bridge.resource;

import com.ning.http.client.HttpResponseHeaders;
import org.junit.Test;
import uk.ac.ic.wlgitbridge.bridge.db.noop.NoopDbStore;
import uk.ac.ic.wlgitbridge.bridge.util.CastUtil;
import uk.ac.ic.wlgitbridge.git.exception.SizeLimitExceededException;
import uk.ac.ic.wlgitbridge.io.http.ning.NingHttpClientFacade;
import uk.ac.ic.wlgitbridge.io.http.ning.NingHttpHeaders;
import uk.ac.ic.wlgitbridge.util.FunctionT;

import java.io.IOException;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UrlResourceCacheTest {

    private static String PROJ = "proj";

    private static String URL = "http://localhost/file.jpg";

    private static String NEW_PATH = "file1.jpg";

    private final NingHttpClientFacade http = mock(NingHttpClientFacade.class);

    private final UrlResourceCache cache
            = new UrlResourceCache(new NoopDbStore(), http);

    private static HttpResponseHeaders withContentLength(long cl) {
        return NingHttpHeaders
                .builder()
                .addHeader("Content-Length", String.valueOf(cl))
                .build();
    }

    private void respondWithContentLength(long cl, long actual)
            throws ExecutionException {
        when(http.get(any(), any())).thenAnswer(invoc -> {
            Object[] args = invoc.getArguments();
            //noinspection unchecked
            ((FunctionT<
                    HttpResponseHeaders, Boolean, SizeLimitExceededException
            >) args[1]).apply(withContentLength(cl));
            return new byte[CastUtil.assumeInt(actual)];
        });
    }

    private void respondWithContentLength(long cl) throws ExecutionException {
        respondWithContentLength(cl, cl);
    }

    private void getWithMaxLength(Optional<Long> max)
            throws IOException, SizeLimitExceededException {
        cache.get(
                PROJ, URL, NEW_PATH, new HashMap<>(), new HashMap<>(), max);
    }

    private void getWithMaxLength(long max)
            throws IOException, SizeLimitExceededException {
        getWithMaxLength(Optional.of(max));
    }

    private void getWithoutLimit()
            throws IOException, SizeLimitExceededException {
        getWithMaxLength(Optional.empty());
    }

    @Test
    public void getDoesNotThrowWhenContentLengthLT() throws Exception {
        respondWithContentLength(1);
        getWithMaxLength(2);
    }

    @Test
    public void getDoesNotThrowWhenContentLengthEQ() throws Exception {
        respondWithContentLength(2);
        getWithMaxLength(2);
    }

    @Test (expected = SizeLimitExceededException.class)
    public void getThrowsSizeLimitExceededWhenContentLengthGT()
            throws Exception {
        respondWithContentLength(3);
        getWithMaxLength(2);
    }

    @Test
    public void getWithEmptyContentIsValid() throws Exception {
        respondWithContentLength(0);
        getWithMaxLength(0);
    }

    @Test
    public void getWithoutLimitDoesNotThrow() throws Exception {
        respondWithContentLength(Integer.MAX_VALUE, 0);
        getWithoutLimit();
    }

    @Test (expected = SizeLimitExceededException.class)
    public void getThrowsIfActualContentTooBig() throws Exception {
        respondWithContentLength(0, 10);
        getWithMaxLength(5);
    }

}
