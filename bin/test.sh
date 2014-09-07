#!/bin/sh
curl -s "localhost:9200/pubmed/_search?search_type=count&pretty" -d '{
    "query": {
        "range": {
            "year": {
                "gte": 2000
            }
        }
    },
    "aggs": {
        "all_docs_per_year": {
            "terms": {
                "field": "year",
                "size": 1000,
                "order" : { "_term" : "asc" }
            }
        },
        "non_zero_if_only": {
            "filter": {
                "range" : {
                    "if" : { "gt" : 0.0 }
                }
            },
            "aggs": {
                "docs_by_year" : {
                    "terms": {
                        "field": "year",
                        "size": 1000,
                        "order" : { "_term" : "asc" }
                    },
                    "aggs": {
                        "if_stats": {
                            "stats": {
                                "field": "if"
                            }
                        }
                    }
                },
                "docs_by_journal" : {
                    "terms": {
                        "field": "journal",
                        "size": 1000,
                        "order" : { "_term" : "asc" }
                    }
                },
                "significant_keywords": {
                    "significant_terms": {
                        "field": "keyword",
                        "size": 100
                    },
                    "aggs": {
                        "docs_per_year": {
                            "terms": {
                                "field": "year",
                                "size": 1000,
                                "order" : { "_term" : "asc" }
                            },
                            "aggs": {
                                "if_stats": {
                                    "stats": {
                                        "field": "if"
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}'


#curl -s "localhost:9200/_search?search_type=count&pretty" -d '{
#     "query": {
#         "match_all": {
#
#          }
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
#}'
#
#

#curl -s "localhost:9200/_search?search_type=count&pretty" -d '{
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
#}'

#curl -s "localhost:9200/_search?search_type=count&pretty" -d '{
#    "query": {
#        "match_all": { }
#    },
#    "aggs": {
#        "keywords": {
#            "terms": {
#                "field": "keyword",
#                "size": 1000
#            },
#            "aggregations": {
#                "years": {
#                    "significant_terms": {
#                        "field": "year",
#                        "size": 100
#                    }
#                }
#            }
#        }
#    }
#}'
