# Elasticsearch Index Termlist Plugin

This plugin extends Elasticsearch with a term list capability. It presents a list of terms in a field of an index
and can also list each terms frequency. Term lists can be generated from one index or even of all of the
indexes.

## Versions

| Elasticsearch  | Plugin       | Release date |
| -------------- | ------------ | ------------ |
| 2.2.0          | 2.2.0.2      | May 22, 2016 |     
| 1.5.2          | 1.5.2.0      | Jun  5, 2015 |
| 1.5.0          | 1.5.0.0      | Apr  9, 2015 |
| 1.4.4          | 1.4.4.0      | Mar 15, 2015 |
| 1.4.0          | 1.4.0.2      | Feb 19, 2015 |
| 1.4.0          | 1.4.0.1      | Jan 14, 2015 |
| 1.4.0          | 1.4.0.0      | Nov 18, 2014 |
| 1.3.2          | 1.3.0.0      | Aug 21, 2014 |
| 1.2.1          | 1.2.1.0      | Jul  3, 2014 |

## Installation

    ./bin/plugin -install index-termlist -url http://xbib.org/repository/org/xbib/elasticsearch/plugin/elasticsearch-index-termlist/1.5.2.0/elasticsearch-index-termlist-1.5.2.0-plugin.zip

Do not forget to restart the node after installing.

## Project docs

The Maven project site is available at [Github](http://jprante.github.io/elasticsearch-index-termlist)

## Issues

All feedback is welcome! If you find issues, please post them at [Github](https://github.com/jprante/elasticsearch-index-termlist/issues)

# Introduction

Getting the list of all terms indexed is useful for various purposes, for example

- term statistics
- building dictionaries
- controlling the overall effects of analyzers on the indexed terms
- automatic query building on indexed terms, e.g. for load tests
- input to linguistic analysis tools
- for other post-processing of the indexed terms outside of Elasticsearch

Optionally, the term list can be narrowed down to a field name. The field name is the Lucene field
name as found in the Lucene index.

Only terms of field names not starting with underscore are listed. Terms of internal fields
like `_uid`, `_all`, or `_type` are always skipped.

# Response

For each term, statistics are computed.

    {
       "_shards": {
          "total": 3,
          "successful": 3,
          "failed": 0
       },
       "took": 384,
       "numdocs": 51279,
       "numterms": 100,
       "terms": [
		  {
			 "term": "aacr2",
			 "totalfreq": 34699,
			 "docfreq": 34697,
			 "min": 1,
			 "max": 2,
			 "mean": 1.0000505458956723,
			 "geomean": 1.0000399550985877,
			 "sumofsquares": 34703,
			 "sumoflogs": 1.3862943611198906,
			 "sigma": 0.008475454987021664,
			 "variance": 0.00007183333723703039
		  }, ...
           
           
`took` - milliseconds required for executing

`numdocs` - the number of documents examined
           
`numterms` - the number of terms returned
           
`terms` - the array of term infos
           
`term` - the name of the term
           
`totalfreq` - the total number of occurrences of this term
           
`docfreq` - the document count where this term appears in
            
`min` - the minimum number of occurrences of this term in a document
            
`max` - the maximum number of occurrences of this term in a document 

`mean` - the mean of the term occurences 

`geomean` - the gemotric mean of the term occurrences

`sumofsquares` - sum of the squares of the term occurrences

`sumoflogs` - sum of the logarithms of the term occurences

`variance` - the variance of the term occurences

`sigma` - the standard deviation, equal to sqrt(variance)

# Example

Consider the following example 

	curl -XDELETE 'http://localhost:9200/test/'
	curl -XPUT 'http://localhost:9200/test/'
	curl -XPUT 'http://localhost:9200/test/test/1' -d '{ "test": "Hello World" }'
	curl -XPUT 'http://localhost:9200/test/test/2' -d '{ "test": "Hello Jörg Prante" }'
	curl -XPUT 'http://localhost:9200/test/test/3' -d '{ "message": "elastic search" }'

Get term list of index ``test``

	curl -XGET 'http://localhost:9200/test/_termlist'

Get term list of index `test` of field `message`

	curl -XGET 'http://localhost:9200/test/_termlist?field=message'

Get term list of index `test` with total frequencies but only the first three of the list

	curl -XGET 'http://localhost:9200/test/_termlist?size=3'

Get term list of terms starting with `hello` in index `test` field `test`

	curl -XGET 'http://localhost:9200/test/_termlist?field=test&term=hello'

A page of 100 terms of a sorted list of terms in your index beginning with `a`

    curl -XGET 'http://localhost:9200/books/_termlist?term=a&sortbyterms&pretty&from=0&size=100' 

# Caution

The term list is built internally into an unsorted, compact set of strings which i
s not streamed to the client. You should be aware that if you have lots of unique terms
in the index, this procedure consumes a lot of heap memory and may result in
out of memory situations that can render your Elasticsearch cluster unusable
until it is restarted.

# License

Elasticsearch Term List Plugin

Copyright (C) 2011-2015 Jörg Prante

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

