PORT ?= 53115
SBT  ?= sbt
JAVA ?= java


SOURCE_FILES = $(shell find src -type f)
BUILD_FILES  = Makefile build.sbt project/plugins.sbt


run: semserv.jar
	@echo
	@echo -e "\e[32mStarting Semserv\e[0m"
	@echo -e "\e[32m================\e[0m"
	@echo
	@echo -e "\e[33mDisregard JarClassLoader warnings, they\e[0m"
	@echo -e "\e[33mare expected and just can't be silenced.\e[0m"
	@echo
	@$(JAVA) -Done-jar.verbose=false -jar $< || echo


semserv.jar: $(SOURCE_FILES) $(BUILD_FILES)
	$(SBT) one-jar
	cp target/scala-2.11/semserv_2.11-0.1.0-one-jar.jar $@


clean:
	$(SBT) clean

realclean: clean
	rm -f semserv.jar


.PHONY: run clean realclean
