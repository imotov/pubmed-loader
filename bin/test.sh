#!/bin/sh
# curl -s "localhost:9200/_search?search_type=count&pretty" -d '{
#     "query": {
#         "match_all": { }
#     },
#     "aggs": {
#         "years": {
#             "terms": {
#                 "field": "year",
#                 "size": 1000,
#                 "order" : { "_term" : "asc" }
#             },
#             "aggregations": {
#                 "keywords": {
#                     "significant_terms": {
#                         "field": "keyword"
#                     }
#                 }
#             }
#         }
#     }
# }'

curl -s "localhost:9200/_search?search_type=count&pretty" -d '{
    "query": {
        "match_all": { }
    },
    "aggs": {
        "keywords": {
            "terms": {
                "field": "keyword",
                "size": 1000
            },
            "aggregations": {
                "years": {
                    "significant_terms": {
                        "field": "year",
                        "size": 100
                    }
                }
            }
        }
    }
}'
