PORT       ?= 53115
SCALA      ?= scala
FSC        ?= fsc
JAR        ?= jar
WGET       ?= wget -O -
HERMIT_URL ?= https://www.cs.ox.ac.uk/isg/tools/HermiT/download/current/HermiT.zip
SPRAY_URL  ?= http://central.maven.org/maven2/io/spray/spray-json_2.11/1.3.3/spray-json_2.11-1.3.3.jar

SOURCES = KnowBase interpret Server
JARDEPS = vendor/HermiT.jar vendor/spray-json_2.11-1.3.3.jar

EMPTY  :=
SPACE  := $(EMPTY) $(EMPTY)
JARPATH = $(subst $(SPACE),:,$(JARDEPS))

SCALA_FILES = $(patsubst %,src/semserv/%.scala,$(SOURCES))
CLASS_FILES = $(patsubst src/%.scala,build/%.class,$(SCALA_FILES))


all: semserv.jar
	-$(SCALA) -classpath $<:$(JARPATH) -e 'semserv.Server($(PORT))'


semserv.jar: $(CLASS_FILES)
	cd build && $(JAR) cf ../$@ .


build/%.class: src/%.scala | build $(JARDEPS)
	$(FSC) -d build -classpath build:$(JARPATH) $<

build:
	mkdir $@


vendor/HermiT.jar: | vendor
	$(WGET) '$(HERMIT_URL)' > vendor/HermiT.zip
	cd vendor && unzip HermiT.zip HermiT.jar
	rm vendor/HermiT.zip

vendor/spray-json_2.11-1.3.3.jar: | vendor
	$(WGET) '$(SPRAY_URL)' > $@

vendor:
	mkdir $@


clean:
	rm -rf build
	$(FSC) -shutdown

realclean: clean
	rm -f semserv.jar


.PHONY: all clean realclean
