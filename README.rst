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
0.90.5         **1.3.0**  Oct 16, 2013       ./bin/plugin --url http://bit.ly/1bzHfIl --install termlist
=============  =========  =================  ===========================================================

Introduction
------------

Getting the list of all terms indexed is useful for variuos purposes, for example

- building dictionaries
- controlling the overall effects of analyzers on the indexed terms
- automatic query building on indexed terms, e.g. for load tests
- input to linguistic analysis tools
- for other post-processing of the indexed terms outside of Elasticsearch

Example of getting term lists

Consider the following example index::

	curl -XPUT 'http://localhost:9200/test/'
	curl -XPUT 'http://localhost:9200/test/test/1' -d '{ "test": "Hello World" }'
	curl -XPUT 'http://localhost:9200/test/test/2' -d '{ "test": "Hello Jörg Prante" }'
	curl -XPUT 'http://localhost:9200/test/test/3' -d '{ "message": "elastic search" }'

Get term list of index ``test``::

	curl -XGET 'http://localhost:9200/test/_termlist'
	{"ok":true,"_shards":{"total":5,"successful":5,"failed":0},"terms":["hello","prant","world","elastic","search","jorg"]}

Get term list of index `test` of field `message`::

	curl -XGET 'http://localhost:9200/test/_termlist/message'
	{"ok":true,"_shards":{"total":5,"successful":5,"failed":0},"terms":["elastic","search"]}

Optionally, the term list can be narrowed down to a field name. The field name is the Lucene field name as found in the Lucene index.

Only terms of field names not starting with underscore are listed. Terms of internal fields like `_uid`, `_all`, or `_type` are always skipped.

If you want a sorted term list, you have to sort the obtained list at client side.

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

