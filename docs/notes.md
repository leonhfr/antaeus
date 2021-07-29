# Notes

## Installations

Install IntelliJ Community Edition:

```shell
$ sudo snap install intellij-idea-community --classic
```

Install Gradle:

```shell
$ wget https://services.gradle.org/distributions/gradle-6.4.1-bin.zip -P /tmp
$ sudo unzip -d /opt/gradle /tmp/gradle-*.zip
```

Then add to `$PATH` in `~/.profile`:

```bash
export GRADLE_HOME=/opt/gradle/gradle-6.4.1
export PATH=${GRADLE_HOME}/bin:${PATH}
```

Test installation: `gradle -v`

## Antaeus Basics

Running the service:

```shell
$ ./docker-clean.sh && ./docker-start.sh
```

Running the tests:

No global command to run all the tests??

Update: `gradle test` does the trick and produces a html

## Challenge notes

- first step, get only pending invoices + add end point and unit tests

## Random Kotlin notes

- Runnable: classes that implement that are intended to be run in a thread
- Array != List; array = fixed size + mutable, lists = dynamic + immutable => most cases will use List
- No prettier or gofmt like tools??
- formatting: Ctrl + Alt + L it's intellij

## Random notes on libraries

- Javelin: order in which paths are declared is the same as the order in which they are evaluated, ie put `pending` before `:id` to avoid `java.lang.NumberFormatException` 

## Resources

### Kotlin lang

- https://kotlinlang.org/docs/basic-types.html
- https://kotlinlang.org/docs/data-classes.html
- https://kotlinlang.org/docs/equality.html

### Others

- https://www.petrikainulainen.net/programming/testing/running-kotlin-tests-with-gradle/
- https://stackoverflow.com/questions/36262305/difference-between-list-and-array-types-in-kotlin