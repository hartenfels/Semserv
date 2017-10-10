GRADLE ?= gradle
JAVA   ?= java


SOURCE_FILES = $(shell find src -type f)
BUILD_FILES  = Makefile build.gradle


run: semserv.jar
	@SEMSERV_DIR=share $(JAVA) -jar $< || echo

dev:
	SEMSERV_DIR=share $(GRADLE) run

semserv.jar: $(SOURCE_FILES) $(BUILD_FILES)
	$(GRADLE) jar
	touch $@


clean:
	$(GRADLE) clean

realclean: clean
	rm -f semserv.jar


.PHONY: run dev clean realclean
