Using Eclipse
-------------
To use eclipse you need to use the m2eclipse plugin (http://m2eclipse.sonatype.org/).

The following steps are recommended:

1. Install the latest version of eclipse
2. Launch eclipse and install the m2eclipse plugin, and make sure it uses your repo configs
3. In eclipse preferences Java->Compiler->Errors/Warnings->Deprecated and restricted set forbidden reference to WARNING
4. In eclipse preferences Java->Code Style, import the cleanup, templates, and formatter configs
5. In eclipse preferences Java->Editor->Save Actions enable "Additional Actions" and deselect all actions except for "Remove trailing whitespace"
6. Use import on the root pom, which will pull in all modules

