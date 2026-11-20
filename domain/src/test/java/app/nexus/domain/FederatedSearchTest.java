package app.nexus.domain;

import org.junit.Test;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.Assert.*;

public class FederatedSearchTest {
    private FederatedSearch.Adapter<String> source(String name, boolean allowed, Callable<String> task) {
        return new FederatedSearch.Adapter<String>() {
            public String name() { return name; }
            public boolean enabled() { return allowed; }
            public List<String> search(String title) throws Exception { return Collections.singletonList(task.call()); }
        };
    }

    @Test public void searchesOverlapAndFailuresAreIsolated() throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(2);
        Callable<String> hit = () -> { barrier.await(2, TimeUnit.SECONDS); return "Title"; };
        List<FederatedSearch.Outcome<String>> result = FederatedSearch.search("Title", Arrays.asList(
            source("first",true,hit), source("second",true,hit),
            source("broken",true,()->{throw new java.io.IOException();}),
            source("blocked",false,()->{throw new AssertionError();})),3000);
        assertEquals(2,result.stream().filter(r->r.hits.contains("Title")).count());
        assertTrue(result.stream().anyMatch(r->r.status.equals("Search failed")));
        assertTrue(result.stream().anyMatch(r->r.status.equals("Pending review")));
    }

    @Test public void timeoutKeepsCompletedResults() throws Exception {
        List<FederatedSearch.Outcome<String>> result=FederatedSearch.search("Title",Arrays.asList(
            source("fast",true,()->"found"), source("slow",true,()->{Thread.sleep(5000);return "late";})),300);
        assertEquals("found",result.get(0).hits.get(0));
        assertEquals("Timed out",result.get(1).status);
    }
}
