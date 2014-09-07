package org.motovs.pubmed;

import au.com.bytecode.opencsv.CSVReader;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPInputStream;

import static org.elasticsearch.common.collect.Lists.newArrayList;
import static org.elasticsearch.common.collect.Maps.newHashMap;

/**
 */
public class MedlineLoader {

    private final BulkProcessor bulkProcessor;

    private final TransportClient client;

    private final Map<String, Float> ifMap = newHashMap();

    private final File dataDir;

    private final ExecutorService executor;

    private final AtomicLong current = new AtomicLong();

    public MedlineLoader(File ifFile, File dataDir, Settings settings) throws IOException {
        this.dataDir = dataDir;
        if (ifFile != null) {
            loadIf(ifFile);
        }
        client = new TransportClient(settings);
        client.addTransportAddress(
                new InetSocketTransportAddress(
                        settings.get("pubmed.host", "localhost"),
                        settings.getAsInt("pubmed.port", 9300)));
        bulkProcessor = BulkProcessor.builder(client, new BulkProcessor.Listener() {

            @Override
            public void beforeBulk(long executionId, BulkRequest request) {

            }

            @Override
            public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
                System.out.println("Processed bulk " + executionId + " success");
                current.addAndGet(-request.numberOfActions());
            }

            @Override
            public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
                System.out.println("Processed bulk " + executionId + " failure" + failure);
                current.addAndGet(-request.numberOfActions());
            }
        })
                .setBulkActions(settings.getAsInt("pubmed.bulk.size", 1000))
                .setConcurrentRequests(settings.getAsInt("pubmed.bulk.threads", 4)).build();
        executor = Executors.newFixedThreadPool(settings.getAsInt("pubmed.parsing.threads", 1));
    }


    public void loadIf(File file) throws IOException {
        try (CSVReader reader = new CSVReader(new FileReader(file))) {
            String[] headers = reader.readNext();
            if (headers == null) {
                return;
            }
            String[] line;
            while ((line = reader.readNext()) != null) {
                for(int i=1; i<line.length; i++) {
                    ifMap.put(line[0] + "-" + headers[i], parseFloat(line[i]));
                }
            }
        }
    }

    public float parseFloat(String str) {
        try {
            return Float.parseFloat(str);
        } catch (NumberFormatException ex) {
            return 0.0f;
        }

    }

    public void process(int skip) throws Exception {
        processDirectory(dataDir, skip);
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.DAYS);
        bulkProcessor.flush();
        while (current.get() > 0) {
            Thread.sleep(100);
        }
        bulkProcessor.close();
        bulkProcessor.awaitClose(10, TimeUnit.MINUTES);
        client.close();
    }

    private void processDirectory(File file, final int skip) throws Exception {

        final File[] files = file.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                if (name.endsWith(".xml.gz")) {
                    int len = name.length();
                    String num = name.substring(len - 11, len - 7);
                    return Integer.parseInt(num) > skip;
                }
                return false;
            }
        });

        for (int i = 0; i < files.length; i++) {
            final int cur = i;
            if (files[cur].isFile()) {
                if (executor != null) {
                    executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                System.out.println(files[cur]);
                                processFile(files[cur]);
                            } catch (Exception ex) {
                                System.out.println(files[cur] + " " + ex);
                            }
                        }
                    });
                } else {
                    try {
                        System.out.println(files[cur]);
                        processFile(files[cur]);
                    } catch (Exception ex) {
                        System.out.println(files[cur] + " " + ex);
                    }
                }
            } else if (files[i].isDirectory()) {
                processDirectory(files[i], skip);
            }
        }
    }

    private float impactFactor(String str, int year) {
        Float impactFactor = ifMap.get(str.toUpperCase() + "-" + year + " Impact Factor");
        if (impactFactor == null) {
            return 0.0f;
        } else {
            return impactFactor;
        }
    }

    public void processFile(File file) throws IOException, SAXException, ParserConfigurationException {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        SAXParser saxParser = spf.newSAXParser();

        try (InputStream fileStream = new FileInputStream(file)) {
            InputStream gzipStream = new GZIPInputStream(fileStream);
            saxParser.parse(gzipStream, new DefaultHandler() {
                private LinkedList<String> currentName = new LinkedList<>();
                String id;
                int year;
                String journalTitle;
                String journalAbbrTitle;
                String articleTitle;
                String abstractText;
                ArrayList<String> headingDescriptions;

                @Override
                public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                    if (localName.equals("MedlineCitation")) {
                        id = null;
                        year = 0;
                        journalTitle = null;
                        journalAbbrTitle = null;
                        articleTitle = null;
                        abstractText = null;
                        headingDescriptions = newArrayList();
                    }
                    currentName.push(localName);
                }

                @Override
                public void endElement(String uri, String localName, String qName) throws SAXException {
                    if (localName.equals("MedlineCitation")) {
                        Float factor = null;
                        if (journalAbbrTitle != null) {
                            factor = impactFactor(journalAbbrTitle.toUpperCase(), year);
                        }
                        if (factor == null) {
                            factor = 0f;
                        }
                        IndexRequestBuilder indexRequestBuilder = new IndexRequestBuilder(client)
                                .setId(id)
                                .setSource("id", id, "year", year, "journal", journalTitle, "journal-abbr", journalAbbrTitle,
                                        "title", articleTitle, "abstract", abstractText,
                                        "keyword", headingDescriptions, "if", factor)
                                .setIndex("pubmed")
                                .setType("article");
                        bulkProcessor.add(indexRequestBuilder.request());
                        current.incrementAndGet();
                    }
                    currentName.pop();
                }

                @Override
                public void characters(char[] ch, int start, int length) throws SAXException {
                    String current = currentName.peek();
                    String prev = currentName.get(1);
                    if (current.equals("Title") && journalTitle == null) {
                        journalTitle = new String(ch, start, length);
                    } else if (prev.equals("PubDate") && current.equals("Year") && year == 0) {
                        year = Integer.parseInt(new String(ch, start, length));
                    } else if (current.equals("ISOAbbreviation") && journalAbbrTitle == null) {
                        journalAbbrTitle = new String(ch, start, length);
                    } else if (current.equals("ArticleTitle") && articleTitle == null) {
                        articleTitle = new String(ch, start, length);
                    } else if (current.equals("AbstractText") && abstractText == null) {
                        abstractText = new String(ch, start, length);
                    } else if (current.equals("DescriptorName")) {
                        headingDescriptions.add(new String(ch, start, length));
                    } else if (current.equals("PMID") && id == null) {
                        id = new String(ch, start, length);
                    }
                }
            });
        }

    }
}
