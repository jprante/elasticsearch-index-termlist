Elasticsearch Index Termlist Plugin
===================================

This plugin extends Elasticsearch with a term list capability.
Term lists can be generated from indexes, or even of all of the indexes in the cluster.

Installation
------------

Prerequisites::

  Elasticsearch 0.90+

=============  =========  =================  ===========================================================
ES version     Plugin     Release date       Command
-------------  ---------  -----------------  -----------------------------------------------------------
0.90.5         1.3.0      Oct 16, 2013       ./bin/plugin --install termlist --url http://bit.ly/1bzHfIl
0.90.7         1.4.0      Dec 20, 2013       ./bin/plugin --install termlist --url http://bit.ly/1c70ICf
=============  =========  =================  ===========================================================

Do not forget to restart the node after installing.

Project docs
------------

The Maven project site is available at `Github <http://jprante.github.io/elasticsearch-index-termlist>`_

Binaries
--------

Binaries are available at `Bintray <https://bintray.com/pkg/show/general/jprante/elasticsearch-plugins/elasticsearch-index-termlist>`_

Introduction
------------

Getting the list of all terms indexed is useful for variuos purposes, for example

- building dictionaries
- controlling the overall effects of analyzers on the indexed terms
- automatic query building on indexed terms, e.g. for load tests
- input to linguistic analysis tools
- for other post-processing of the indexed terms outside of Elasticsearch

Optionally, the term list can be narrowed down to a field name. The field name is the Lucene field
name as found in the Lucene index.

Only terms of field names not starting with underscore are listed. Terms of internal fields
like `_uid`, `_all`, or `_type` are always skipped.

Example
=======

Consider the following example index::

	curl -XPUT 'http://localhost:9200/test/'
	curl -XPUT 'http://localhost:9200/test/test/1' -d '{ "test": "Hello World" }'
	curl -XPUT 'http://localhost:9200/test/test/2' -d '{ "test": "Hello Jörg Prante" }'
	curl -XPUT 'http://localhost:9200/test/test/3' -d '{ "message": "elastic search" }'

Get term list of index ``test``::

	curl -XGET 'http://localhost:9200/test/_termlist'
	{"_shards":{"total":5,"successful":5,"failed":0},"terms":[{"name":"search"},{"name":"prante"},{"name":"hello"},{"name":"elastic"},{"name":"world"},{"name":"jörg"}]}

Get term list of index `test` of field `message`::

	curl -XGET 'http://localhost:9200/test/_termlist?field=message'
	{"_shards":{"total":5,"successful":5,"failed":0},"terms":[{"name":"elastic"},{"name":"search"}]}

Get term list of index `test` with total frequencies::

	curl -XGET 'http://localhost:9200/test/_termlist?totalfreqs'
	{"_shards":{"total":5,"successful":5,"failed":0},"terms":[{"name":"hello","totalfreq":2},{"name":"world","totalfreq":1},{"name":"search","totalfreq":1},{"name":"prante","totalfreq":1},{"name":"jörg","totalfreq":1},{"name":"elastic","totalfreq":1}]}


Get term list of index `test` with total frequencies but only the first three terms of the list::

	curl -XGET 'http://localhost:9200/test/_termlist?totalfreqs&size=3'
	{"_shards":{"total":5,"successful":5,"failed":0},"terms":[{"name":"hello","totalfreq":2},{"name":"world","totalfreq":1},{"name":"search","totalfreq":1}]}


Caution
=======

The term list is built internally into an unsorted, compact set of strings which i
s not streamed to the client. You should be aware that if you have lots of unique terms
in the index, this procedure consumes a lot of heap memory and may result in
out of memory situations that can render your Elasticsearch cluster unusable
until it is restarted.


License
=======

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

