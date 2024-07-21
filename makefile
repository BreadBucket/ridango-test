

.PHONY: all
all: $(wildcard src/*.java)
	javac --source-path 'src/' $^


# .PHONY: run
# run: all
# 	java -cp 'src/' Main '10' '3' 'relative'


.PHONY: clean
clean:
	rm src/*.class
