LEIN ?= lein
JAVA ?= java

SOURCE_FILES = $(wildcard src/semserv/*.clj)
BUILD_FILES  = Makefile project.clj


run: semserv.jar
	SEMSERV_DIR=share $(JAVA) -jar semserv.jar

dev:
	SEMSERV_DIR=share $(LEIN) run

semserv.jar: $(SOURCE_FILES) $(BUILD_FILES)
	$(LEIN) uberjar
	cp target/uberjar/*-standalone.jar $@


clean:
	$(LEIN) clean

realclean: clean
	rm -f semserv.jar


.PHONY: run dev clean realclean
