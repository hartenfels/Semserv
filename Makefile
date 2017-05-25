GRADLE ?= gradle
JAVA   ?= java


SOURCE_FILES = $(shell find src -type f)
BUILD_FILES  = Makefile build.gradle


run: semserv.jar
	@cd share && $(JAVA) -jar ../$< || echo

semserv.jar: $(SOURCE_FILES) $(BUILD_FILES)
	$(GRADLE) jar


clean:
	$(GRADLE) clean

realclean: clean
	rm -f semserv.jar


.PHONY: run clean realclean
