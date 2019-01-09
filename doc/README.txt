Jekings development environment

Requirements
------------

- JDK

Jenkins 2.54 is the first weekly release to require a Java 8 runtime.
PureLoad plugin will require Java 8.

- Maven

Maven is required. Download "apache-maven-3.5.n-bin.tar.gz" from
https://maven.apache.org/download.cgi

Install (I istall in ~/java):

> cd ~/java
> tar xvf .../apache-maven-3.5.2-bin.tar
> ln -s apache-maven-3.5.2 maven

Set up environment (I use tcsh and add the following in .cshrc):

setenv M2_HOME /Users/janne/java/maven
alias maven 'setenv M2 $M2_HOME/bin; set path = ( $M2 $path ) ; mvn --version'

Then I execute (to check that maven is installed):

> maven
> mvn -v


- Jenkins Maven Environment

Edit (or create) ~/.m2/settings.xml and add what described here:
https://wiki.jenkins.io/pages/viewpage.action?pageId=67567923#Plugintutorial-SettingUpEnvironment

Build
-----

Compile sources:
> mvn compile

Create a distribution image
> mvn package

Note: use mnv -o to avoid hitting repositories every time

Test
----

Execute:
mvn -Djetty.port=9090 hpi:run

And access http://localhost:9090/jenkins/ in a browser
