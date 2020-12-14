package uk.gov.hmcts.reform.fpl.utils.captor;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class ResultsCaptor<R> implements Answer<R> {
    private final Queue<R> results;

    public ResultsCaptor() {
        this.results = new ArrayDeque<>();
    }

    /**
     * When using this method do the capturing in the following way:
     * <pre class="code"><code class="java">
     * doAnswer(captor).when(spy).method(...);
     * </code></pre>
     *
     * @param invocation mock/spy to call real method on
     * @return result of calling the method
     * @throws Throwable the throwable to be thrown
     */
    @SuppressWarnings("unchecked")
    @Override
    public R answer(InvocationOnMock invocation) throws Throwable {
        R result = (R) invocation.callRealMethod();
        results.offer(result);
        return result;
    }

    public R getResult() {
        return results.poll();
    }

    public List<R> getResults() {
        List<R> results = new ArrayList<>(this.results);
        this.results.clear();
        return results;
    }
}
