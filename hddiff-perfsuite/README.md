
HighDefinition Diff - Performance Suite
=======================================

Perform the evaluation
----------------------

The evaluation is performed with a set of revision pairs from the English 
Wikipedia. Each revision pair is processed by HDDiff, FcDiff and XyDiff in 
various configurations. The results are written to a report file called 
`[SET]-report.csv`, where `[SET]` is the filename prefix of the set files
`[SET].json` (containing the raw wiki markup extracted from Wikipedia) and
`[SET]-parsed.json` (containing the parsed revisions as WOMv3 XML).

1. Download the sets RandomRevisions100kSet1 to RandomRevisions100kSet4 from
   http://sweble.org/downloads/hddiff-perfsuite/ (you need ~4.3 GB of disk space)
2. Extract the sets (you need ~83 GB of disk space) to a directory of your 
   choice, referred to as `${SETS}` hereafter.
3. Install `XyDiff` (see section "XyDiff - XML Diff Tools" below)
4. Install Maven 3 and a recent JDK (at least 1.7)
4. Clone and build the `hddiff-perfsuite` program:

   ```bash
   git clone git://sweble.org/git/hddiff-project.git
   cd hddiff-project
   git checkout hddiff-project-1.0.0
   mvn install -Pbuild-aggregates
   ```
5. Run the Performance suite (your system should have at least 8GB of RAM)

   ```bash
   export LD_LIBRARY_PATH="[PATH TO XYDIFF LIB DIRECTORY]"
   java -Xmx8g \
           -Dxydiff.bin="[PATH TO XYDIFF BINARY]" \
           -jar hddiff-perfsuite/target/hddiff-perfsuite-${VERSION}-jar-with-dependencies.jar \
           ${SETS}/
           RandomRevisions100kSet1 RandomRevisions100kSet2 ...
   ```

   Hint:
   ```
   e.g. [PATH TO XYDIFF LIB DIRECTORY]: /local/opt/xydiff/lib
   e.g. [PATH TO XYDIFF BINARY]: /local/opt/xydiff/bin/xydiff
   ```
   The real location depends on your arguments to 
   `./configure --prefix=[INSTALL PATH]` when you were installing `XyDiff`.

6. You find the results of the evaluation in the ${SETS}/*-report.csv files.

Diff algorithms used in the comparison
--------------------------------------

### Fuego Core XML Diff and Patch Tool

**Project hosted at:** https://code.google.com/p/fc-xmldiff/

**Sources:** `svn checkout http://fc-xmldiff.googlecode.com/svn/trunk/ fc-xmldiff-read-only`

**Language:** Java

**Described in:**
```
Lindholm, Tancred, Jaakko Kangasharju, and Sasu Tarkoma.
"Fast and simple XML tree differencing by sequence alignment."
Proceedings of the 2006 ACM symposium on Document engineering. ACM, 2006.
```

This software is also available as maven artifact:
```
<dependency>
  <groupId>fc.xml.diff</groupId>
  <artifactId>xmldiff</artifactId>
  <version>0.14</version>
</dependency>
```

in the repository:
```
<distributionManagement>
  <repository>
    <id>3rd-party-releases</id>
    <url>http://mojo.cs.fau.de/nexus/content/repositories/3rd-party-releases</url>
  </repository>
</distributionManagement>
```

**Installation:**
```
ant -f project-setup.xml
ant
```

### XyDiff - XML Diff Tools

**Project hosted at:** https://github.com/fdintino/xydiff/

**Sources:**
- `git clone https://github.com/fdintino/xydiff.git`
- `git clone https://github.com/bprosnitz/xydiff.git` (Bug fixes for latest GCC)

**Language:** C++

**Described in:**
```
Cobena, Gregory, Serge Abiteboul, and Amelie Marian. 
"Detecting changes in XML documents." 
Data Engineering, 2002. Proceedings. 18th International Conference on. IEEE, 2002.
```

**Installation:**
```
./autogen.sh 
./configure --prefix=[INSTALL PATH]
make
make install
```
