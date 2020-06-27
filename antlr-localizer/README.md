## antlr-localizer
This is the antlr implementation of [Spectrum-based Fault Localization for Context-free Grammars](https://dl.acm.org/doi/10.1145/3357766.3359538). It has since changed after the paper and does not yet contain evaluation shown in the paper.

## Getting started 

This tool requires and has been tested on Java 1.8, maven 3.6, python3 and numpy.
```
git clone https://github.com/miraselimo/antlr-localizer.git
cd antlr-localizer/
git submodule update --init --recursive
cd st4-maven-plugin
mvn clean install

```
