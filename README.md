# NAME

Semantics — semantic data server for the [Software Languages Team](http://softlang.wikidot.com/) and [Institute for Web Science](https://west.uni-koblenz.de/lambda-dl) of the [University of Koblenz-Landau](https://www.uni-koblenz-landau.de/en/university-of-koblenz-landau)


# SYNOPSIS

TBD


# INSTALLATION

You'll need the following:

* Scala

* Java JDK 8, shouldn't matter if it's Oracle or OpenJDK

* A Unix-like environment (make, sh)

Then should be able to just run `make` and it'll download the dependencies,
compile the Scala code and run the server for you.

If you want to use curl instead of wget to download the dependencies, use `make
-e WGET=curl` instead.


# LICENSE

[Apache License, Version 2](LICENSE)


# SEE ALSO

* [λ-DL](https://west.uni-koblenz.de/lambda-dl)

* [LambdaDL](https://github.com/hartenfels/LambdaDL)

* [Semantics](https://github.com/hartenfels/Semantics)

* [HermiT](http://www.hermit-reasoner.com/)
