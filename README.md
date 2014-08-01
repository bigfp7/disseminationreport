In order to get all statistics, please call CreateReport with 4 parameters: (your mailing list email adress) (your mailing list password) (slideshare api-key) (slideshare shared secret).

Feel free to extend CreateReport by adding additional metrics.

* Known Bugs
Sometimes slideshare reporting crashes (maybe because of the library used or slideshare itself) but it should work after a rerun.

May later be converted to maven, if the dependencies grow too much.

    <dependency>
      <groupId>org.twitter4j</groupId>
      <artifactId>twitter4j-core</artifactId>
      <version>[4.0,)</version>
    </dependency>

    <repository>
        <id>parancoe-org-repository</id>
        <name>Parancoe Repository for Maven</name>
        <url>http://maven2.parancoe.org/repo</url>
        <layout>default</layout>
    </repository>

    <dependency>
        <groupId>com.benfante</groupId>
        <artifactId>JSlideShare</artifactId>
        <version>0.7</version>
        <scope>compile</scope>
    </dependency>