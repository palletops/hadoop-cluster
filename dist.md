# Building a tar dist file

The `dist.sh` script builds a tarfile containing the bin script, the lib
directory of dependencies, and the readme.  This is a prototype distribution
mechanism.

To use the bin script you will need to at least build the `lib` directory using
`mvn dependency:copy-dependencies` (see the `dist.sh` script file).
