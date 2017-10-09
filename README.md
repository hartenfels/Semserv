# NAME

Semserv — semantic data server for the [Software Languages Team](http://softlang.wikidot.com/) and [Institute for Web Science](https://west.uni-koblenz.de/lambda-dl) of the [University of Koblenz-Landau](https://www.uni-koblenz-landau.de/en/university-of-koblenz-landau)


# VERSION

Version 1.1.0-dev **EXPERIMENTAL CLOJURE REWRITE**


# SYNOPSIS

This server is intended for use with
[Semantics4J](https://github.com/hartenfels/Semantics4J).

Put your ontology files into [the share directory](share) and just run `make`.
The server will be built and ran automatically and use the ontology from your
program.


# BUILDING

You'll need the following:

* Leiningen

* Java JDK 8, shouldn't matter if it's Oracle or OpenJDK

On a Unix-like system, just run `make` and everything will be compiled for you.
The server will be ran automatically.

Otherwise, you need to execute `lein run` manually. To bundle everything into
a single JAR file, run `lein uberjar` and run `java -jar semserv.jar` as if it
were a regular Java program.


# DESCRIPTION

Semserv is a server that provides access to semantic data queries and
reasoning. It uses [OWL API](http://owlapi.sourceforge.net/) and the [HermiT
Reasoner](http://www.hermit-reasoner.com/). It also provides in-memory and
on-disk caching using [SQLite](https://www.sqlite.org/).

By default, Semserv will listen on port 53115. You can change this by
specifying a different port in the `SEMSERV_PORT` environment variable. Client
programs should also pay attention to this variable to connect to the
appropriate port.

On-disk caching is by default done in the file `cache.db` in the current
working directory. You can specify a different path in the `SEMSERV_CACHE`
environment variable. To use in-memory caching only, set this to `:memory:`.

The communication protocol uses [line-based JSON](http://jsonlines.org/).
Clients send a line of JSON as a request and receive a line of JSON as a
response. Empty lines are ignored.

The [JSON Schema](http://json-schema.org/) for the messages can be found at
[here](src/main/resources/semserv.schema.json). All incoming messages are
validated against this schema.

All responses are either a boolean, a string or an array of IRIs (strings). If
an error occurs, it is returned as an object. Therefore, to check if a call
succeeded, it is enough to check if the returned value is an object.

Each message follows the basic format of `[source, operation, arguments]`,
where `source` is the path to the ontology and the other two being the action
to perform and the arguments to pass to it. For example, the message
`["wine.rdf", "query", ["C" ":Wine"]]` will perform a query for all `:Wine`
concepts in the [wine.rdf](share/wine.rdf) ontology.

## Description Logic

Description logic expressions exist in two broad categories: concepts and
roles. They are required as arguments to several message types. The following
tables describe the expression syntax as encoded in JSON.

Concept syntax:

| Concept Type               | DL      | JSON          |
| -------------------------- |-------- | ------------- |
| Everything (Top Concept)   | `⊤`     | `true`        |
| Nothing (Bottom Concept)   | `⊥`     | `false`       |
| Concept Atom               | `C`     | `["C", C]`    |
| Nominal Concept            | `{a}`   | `["O", a]`    |
| Union                      | `C ⊔ D` | `["U", C, D]` |
| Intersection               | `C ⊓ D` | `["I", C, D]` |
| Negation                   | `¬C`    | `["N", C]`    |
| Existential Quantification | `∃R.C`  | `["E", R, C]` |
| Universal Quantification   | `∀R.C`  | `["A", R, C]` |

Role syntax:

| Role Type           | DL   | JSON       |
| ------------------- |----- | ---------- |
| Full (Top Role)     | `▽`  | `true`     |
| Empty (Bottom Role) | `△`  | `false`    |
| Role Atom           | `R`  | `["r", R]` |
| Inversion           | `R⁻` | `["i", R]` |

## Request Types

The following list describes the format of the request messages and responses
for them. As mentioned previously, in case of an error, a JSON object is
returned, which is never a valid response otherwise.

* `[source, "individual", iri] --> string`

Takes a nominal concept specified by the string `iri` from the `source`
ontology and returns its IRI as a string. This can be used to expand an
abbreviated IRI or something.

* `[source, "satisfiable", concept] --> boolean`

Returns if the query specified by `concept` is satisfiable in the `source`
ontology.

* `[source, "same", [individual1, individual2] --> boolean]`

Returns if the given `individual`s represent the same individual.

* `[source, "query", concept] --> array of strings`

Performs a query with the given `concept` expression and returns an array of
strings representing the IRIs of the matching individuals. If no individuals
match, an empty array is returned, it does not trigger an error.

* `[source, "project", [individual, role]] --> array of strings`

Performs a projection of the given `role` on the `individual`. Returns an array
of IRIs again, like querying does.

* `[source, "subtype", [sub, super]] --> boolean`

Returns if the given `sub` concept expression is a subtype of the `super`
concept expression.

* `[source, "member", [concept, individual]] --> boolean`

Returns if the given `individual` is a member (or “instance”) of the given
`concept` expression.

* `[source, "signature", [type, iri]] --> boolean`

Returns if an element is part of the signature of the `source` ontology. The
`type` parameter specifies the kind of element the string `iri` describes, it
can either be `"concept"` for a concept atom, `"role"` for a role atom or
`"individual"` for a nominal individual.


# BUGS

As [OWL API is not
thread-safe](https://sourceforge.net/p/owlapi/mailman/message/26232558/), calls
accessing it run single-threaded. This makes multiple parallel calls pretty
slow and could be mitigated by using multiple *processes* to run things at the
same time.

Caching is kinda dumb, as it is keyed simply by the incoming JSON. Queries that
are logically the same, but structurally different, will result in different
cache entries. To fix this, the incoming requests should be normalized first.


# LICENSE

Copyright 2017 Carsten Hartenfels.

Licensed under the [Apache License, Version 2](LICENSE).

The [Music Ontology](share/music.rdf) is by Martin Leinberger, Ralf Lämmel and
Steffen Staab. It can be found at <https://west.uni-koblenz.de/lambda-dl>.

The [Wine Ontology](share/wine.rdf) is taken from the [OWL Web Ontology
Language Guide](https://www.w3.org/TR/owl-guide/) and is owned by the [World
Wide Web Consortium](https://www.w3.org/).


# SEE ALSO

* [Semantics4J](https://github.com/hartenfels/Semantics4J)

* [λ-DL](https://west.uni-koblenz.de/lambda-dl)

* [OWL API](http://owlapi.sourceforge.net/)

* [HermiT](http://www.hermit-reasoner.com/)
