package app.nexus.domain;

import java.util.*;
import java.util.concurrent.*;

/** Runs source adapters independently. Only explicitly enabled sources receive a query. */
public final class FederatedSearch {
    public static final class SourceException extends java.io.IOException {
        public SourceException(String message) { super(message); }
    }

    public interface Adapter<T> {
        String name();
        boolean enabled();
        List<T> search(String title) throws Exception;
    }

    public static final class Outcome<T> {
        public final String source;
        public final List<T> hits;
        public final String status;
        Outcome(String source, List<T> hits, String status) {
            this.source = source;
            this.hits = Collections.unmodifiableList(new ArrayList<>(hits));
            this.status = status;
        }
    }

    private static final ExecutorService WORKERS = Executors.newFixedThreadPool(12, r -> {
        Thread thread = new Thread(r, "nexus-search");
        thread.setDaemon(true);
        return thread;
    });

    private FederatedSearch() {}

    public static <T> List<Outcome<T>> search(String query, List<Adapter<T>> sources,
                                               long timeoutMillis) throws InterruptedException {
        List<Callable<Outcome<T>>> jobs = new ArrayList<>();
        List<String> names = new ArrayList<>();
        List<Outcome<T>> results = new ArrayList<>();
        for (Adapter<T> source : sources) {
            if (!source.enabled()) {
                results.add(new Outcome<>(source.name(), Collections.emptyList(), "Pending review"));
                continue;
            }
            names.add(source.name());
            jobs.add(() -> new Outcome<>(source.name(), source.search(query), "Searched"));
        }
        List<Future<Outcome<T>>> futures = WORKERS.invokeAll(jobs, timeoutMillis, TimeUnit.MILLISECONDS);
        for (int i = 0; i < futures.size(); i++) {
            try {
                results.add(futures.get(i).get());
            } catch (ExecutionException | CancellationException e) {
                String status = e instanceof CancellationException ? "Timed out" : "Search failed";
                if (e.getCause() instanceof SourceException) status = e.getCause().getMessage();
                results.add(new Outcome<>(names.get(i), Collections.emptyList(), status));
            }
        }
        return results;
    }
}
