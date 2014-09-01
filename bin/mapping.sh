#!/bin/sh
HOST=localhost
curl -XDELETE "http://$HOST:9200/pubmed"
curl -XPUT "http://$HOST:9200/pubmed" -d '{
    "settings": {
        "index.number_of_shards": 1,
        "index.number_of_replicas": 0
    },
    "mappings": {
        "article": {
            "properties": {
                "abstract": {
                    "type": "string"
                },
                "date": {
                    "type": "date",
                    "format": "dateOptionalTime"
                },
                "id": {
                    "type": "long"
                },
                "journal": {
                    "type": "string",
                     "index": "not_analyzed"
                },
                "keyword": {
                    "type": "string",
                    "index": "not_analyzed"
                },
                "title": {
                    "type": "string"
                },
                "if" : {
                    "type" : "float"
                }
            }
        }
    }
}'
