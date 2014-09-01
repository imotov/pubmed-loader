package org.motovs.pubmed;

import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;

/**
 */
public class BulkListener implements BulkProcessor.Listener {
    @Override
    public void beforeBulk(long executionId, BulkRequest request) {
    }

    @Override
    public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
        System.out.println("Processed bulk " + executionId + " success");
    }

    @Override
    public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
        System.out.println("Processed bulk " + executionId + " failure" + failure);
    }
}
