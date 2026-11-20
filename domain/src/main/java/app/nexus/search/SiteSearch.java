package app.nexus.search;

import java.util.*;
import java.util.concurrent.*;

public final class SiteSearch {
    public static final class Outcome {
        public final String domain, status;
        public final List<SiteConnector.Hit> hits;

        Outcome(String domain, String status, List<SiteConnector.Hit> hits) {
            this.domain = domain;
            this.status = status;
            this.hits = Collections.unmodifiableList(new ArrayList<>(hits));
        }
    }

    public List<Outcome> search(String query, List<SiteConnector> sources, long timeoutMillis)
            throws InterruptedException {
        String title = query.trim();
        if (title.isEmpty() || sources.isEmpty()) return Collections.emptyList();
        List<Outcome> results = new ArrayList<>();
        List<Callable<Outcome>> jobs = new ArrayList<>();
        List<String> domains = new ArrayList<>();
        for (SiteConnector source : sources) {
            if (!source.automationApproved()) {
                results.add(empty(source.domain(), "Permission pending"));
                continue;
            }
            domains.add(source.domain());
            jobs.add(() -> {
                List<SiteConnector.Hit> hits = source.search(title);
                for (SiteConnector.Hit hit : hits) {
                    if (!source.domain().equalsIgnoreCase(hit.page.getHost()))
                        throw new IllegalArgumentException("Unexpected result host");
                }
                return new Outcome(source.domain(), hits.isEmpty() ? "No match" : "Matches", hits);
            });
        }
        if (jobs.isEmpty()) return results;
        ExecutorService workers = Executors.newFixedThreadPool(Math.min(4, jobs.size()));
        try {
            List<Future<Outcome>> futures = workers.invokeAll(jobs, timeoutMillis, TimeUnit.MILLISECONDS);
            for (int i=0; i<futures.size(); i++) {
                try { results.add(futures.get(i).get()); }
                catch (CancellationException timeout) { results.add(empty(domains.get(i), "Timed out")); }
                catch (ExecutionException error) { results.add(empty(domains.get(i), "Source unavailable")); }
            }
            return results;
        } finally { workers.shutdownNow(); }
    }

    private static Outcome empty(String domain, String status) {
        return new Outcome(domain, status, Collections.emptyList());
    }
}
