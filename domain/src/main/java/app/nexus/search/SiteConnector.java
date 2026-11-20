package app.nexus.search;

import java.net.URI;
import java.util.List;

/** A site adapter implements the site's reviewed search method and extracts title-level evidence. */
public interface SiteConnector {
    String domain();
    boolean automationApproved();
    List<Hit> search(String title) throws Exception;

    final class Hit {
        public final String title;
        public final URI page;
        public final URI thumbnail;
        public final URI download;

        public Hit(String title, URI page, URI thumbnail, URI authorizedDirectFile) {
            this.title = title.trim();
            if (this.title.isEmpty()) throw new IllegalArgumentException("Missing result title");
            this.page = https(page);
            this.thumbnail = thumbnail == null ? null : https(thumbnail);
            this.download = authorizedDirectFile == null ? null : https(authorizedDirectFile);
        }

        private static URI https(URI uri) {
            if (uri == null || !"https".equalsIgnoreCase(uri.getScheme()) ||
                    uri.getHost() == null || uri.getRawUserInfo() != null)
                throw new IllegalArgumentException("Invalid source URL");
            return uri;
        }
    }
}
