# NAME

Semantics — semantic data server for the [Software Languages Team](http://softlang.wikidot.com/) and [Institute for Web Science](https://west.uni-koblenz.de/lambda-dl) of the [University of Koblenz-Landau](https://www.uni-koblenz-landau.de/en/university-of-koblenz-landau)


# SYNOPSIS

TBD


# INSTALLATION

You'll need the following:

* Scala and SBT

* Java JDK 8, shouldn't matter if it's Oracle or OpenJDK

On a Unix-like system, just run `make` and everything will be compiled for you.
The server will be ran automatically.

Otherwise, you need to execute `sbt run` manually. To bundle everything into a
single JAR file, run `sbt one-jar` and run
`target/scala-2.11/semserv_2.11-0.1.0-one-jar.jar` as if it were a regular Java
program.


# LICENSE

[Apache License, Version 2](LICENSE)


# SEE ALSO

* [λ-DL](https://west.uni-koblenz.de/lambda-dl)

* [LambdaDL](https://github.com/hartenfels/LambdaDL)

* [Semantics](https://github.com/hartenfels/Semantics)

* [HermiT](http://www.hermit-reasoner.com/)
