package app.nexus.discovery;


import app.nexus.domain.FederatedSearch;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;

/** Metadata cards on the public listing routes. No search, API or player requests. */
public enum AnimeLokListing implements FederatedSearch.Adapter<List<String>> {
    MOVIES("/movies","MOVIE"), SERIES("/tv-series","TV");
    private final String path, type;
    private static final Pattern CARD=Pattern.compile("href=\"(/anime/[0-9]+)\"[^>]*>[^<]*(?:<div[^>]*>\\s*)?<img\\s+([^>]+)>",Pattern.CASE_INSENSITIVE);
    private static final Pattern ALT=Pattern.compile("\\balt=\"([^\"]+)\"");
    private static final Pattern SRC=Pattern.compile("\\bsrc=\"(https://s4\\.anilist\\.co/[^\"]+)\"");
    private List<List<String>> cache=Collections.emptyList();
    private long refreshed, attempted;
    AnimeLokListing(String path,String type){this.path=path;this.type=type;}
    public boolean enabled(){return true;}
    public List<List<String>> search(String query) throws IOException {
        List<List<String>> hits=new ArrayList<>();
        if(query==null || query.trim().isEmpty() || query.length()>200) return hits;
        app.nexus.domain.TitleMatcher matcher=new app.nexus.domain.TitleMatcher(query);
        for(List<String> row:listing()) if(matcher.matches(row.get(0))) hits.add(row);
        return hits;
    }
    private synchronized List<List<String>> listing() throws IOException {
        long now=java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
        if(!cache.isEmpty() && now-refreshed<900000) return cache;
        if(attempted!=0 && now-attempted<60000) throw new IOException("Retry later");
        attempted=now;
        HttpURLConnection c=(HttpURLConnection)new URL("https://animelok.cc"+path).openConnection();
        try {
            c.setInstanceFollowRedirects(false);
            c.setRequestProperty("User-Agent","NexusCatalogue/1.0");
            c.setConnectTimeout(4000); c.setReadTimeout(5000);
            if(c.getResponseCode()!=200) throw new IOException("Catalogue unavailable");
            try(InputStream in=c.getInputStream();ByteArrayOutputStream bytes=new ByteArrayOutputStream()){
                byte[] buf=new byte[8192];int count;
                while((count=in.read(buf))!=-1){
                    if(bytes.size()+count>1000000) throw new IOException("Listing too large");
                    bytes.write(buf,0,count);
                }
                List<List<String>> rows=new ArrayList<>();
                Matcher cards=CARD.matcher(bytes.toString("UTF-8"));
                while(cards.find()){
                    Matcher title=ALT.matcher(cards.group(2)), image=SRC.matcher(cards.group(2));
                    if(title.find()) rows.add(Arrays.asList(title.group(1).replace("&amp;","&").replace("&#39;","'").replace("&quot;","\""),"",type,
                        "https://animelok.cc"+cards.group(1),"","",image.find()?image.group(1):""));
                }
                if(rows.isEmpty()) throw new IOException("No recognizable catalogue cards");
                cache=rows;refreshed=now;return rows;
            }
        } finally {c.disconnect();}
    }
}



