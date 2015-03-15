# Elasticsearch Index Termlist Plugin

This plugin extends Elasticsearch with a term list capability. It presents a list of terms in a field of an index
and can also list each terms frequency. Term lists can be generated from one index or even of all of the
indexes.

## Versions

| Elasticsearch  | Plugin       | Release date |
| -------------- | ------------ | ------------ |
| 1.4.4          | 1.4.4.0      | Mar 15, 2015 |
| 1.4.0          | 1.4.0.2      | Feb 19, 2015 |
| 1.4.0          | 1.4.0.1      | Jan 14, 2015 |
| 1.4.0          | 1.4.0.0      | Nov 18, 2014 |
| 1.3.2          | 1.3.0.0      | Aug 21, 2014 |
| 1.2.1          | 1.2.1.0      | Jul  3, 2014 |

## Installation

    ./bin/plugin -install index-termlist -url http://xbib.org/repository/org/xbib/elasticsearch/plugin/elasticsearch-index-termlist/1.4.4.0/elasticsearch-index-termlist-1.4.4.0-plugin.zip

Do not forget to restart the node after installing.

## Project docs

The Maven project site is available at [Github](http://jprante.github.io/elasticsearch-index-termlist)

## Issues

All feedback is welcome! If you find issues, please post them at [Github](https://github.com/jprante/elasticsearch-index-termlist/issues)

# Introduction

Getting the list of all terms indexed is useful for various purposes, for example

- building dictionaries
- controlling the overall effects of analyzers on the indexed terms
- automatic query building on indexed terms, e.g. for load tests
- input to linguistic analysis tools
- for other post-processing of the indexed terms outside of Elasticsearch

Optionally, the term list can be narrowed down to a field name. The field name is the Lucene field
name as found in the Lucene index.

Only terms of field names not starting with underscore are listed. Terms of internal fields
like `_uid`, `_all`, or `_type` are always skipped.

# Example

Consider the following example index

	curl -XDELETE 'http://localhost:9200/test/'
	curl -XPUT 'http://localhost:9200/test/'
	curl -XPUT 'http://localhost:9200/test/test/1' -d '{ "test": "Hello World" }'
	curl -XPUT 'http://localhost:9200/test/test/2' -d '{ "test": "Hello Jörg Prante" }'
	curl -XPUT 'http://localhost:9200/test/test/3' -d '{ "message": "elastic search" }'

Get term list of index ``test``

	curl -XGET 'http://localhost:9200/test/_termlist'
	{"_shards":{"total":5,"successful":5,"failed":0},"total":6,"terms":[{"name":"search"},{"name":"prante"},{"name":"hello"},{"name":"world"},{"name":"jörg"},{"name":"elastic"}]}

Get term list of index `test` of field `message`

	curl -XGET 'http://localhost:9200/test/_termlist?field=message'
	{"_shards":{"total":5,"successful":5,"failed":0},"total":2,"terms":[{"name":"elastic"},{"name":"search"}]}

Get term list of index `test` with doc count

	curl -XGET 'http://localhost:9200/test/_termlist?totalfreqs'
	{"_shards":{"total":5,"successful":5,"failed":0},"total":6,"terms":[{"name":"search","totalfreq":1},{"name":"prante","totalfreq":1},{"name":"hello","totalfreq":2},{"name":"world","totalfreq":1},{"name":"jörg","totalfreq":1},{"name":"elastic","totalfreq":1}]}

Get term list of index `test` with total frequencies

	curl -XGET 'http://localhost:9200/test/_termlist?totalfreqs'
	{"_shards":{"total":5,"successful":5,"failed":0},"total":6,"terms":[{"name":"search","totalfreq":1},{"name":"prante","totalfreq":1},{"name":"hello","totalfreq":2},{"name":"world","totalfreq":1},{"name":"jörg","totalfreq":1},{"name":"elastic","totalfreq":1}]}

Get term list of index `test` with total frequencies but only the first three terms of the list

	curl -XGET 'http://localhost:9200/test/_termlist?totalfreqs&size=3'
	{"_shards":{"total":5,"successful":5,"failed":0},"total":6,"terms":[{"name":"prante","totalfreq":1},{"name":"hello","totalfreq":2},{"name":"world","totalfreq":1}]}

Get term list of terms starting with `hello` in index `test` field `test`, with total frequencies. This can be useful to estimate hits.

	curl -XGET 'http://localhost:9200/test/_termlist?field=test&term=hello&totalfreqs'
	{"_shards":{"total":5,"successful":5,"failed":0},"total":1,"terms":[{"name":"hello","totalfreq":2}]}

A complete sorted list of terms in your index beginning with `a`, pageable, complete with frequencies

    curl -XGET 'http://localhost:9200/books/_termlist?term=a&totalfreqs&sortbyterms&pretty&from=0&size=100' 

# Caution

The term list is built internally into an unsorted, compact set of strings which i
s not streamed to the client. You should be aware that if you have lots of unique terms
in the index, this procedure consumes a lot of heap memory and may result in
out of memory situations that can render your Elasticsearch cluster unusable
until it is restarted.

# License

Elasticsearch Term List Plugin

Copyright (C) 2011 Jörg Prante

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

