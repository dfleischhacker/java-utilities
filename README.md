Java utilities
==============

This repository contains some small to mid-size tools for working with OWL ontologies or Java in general.
They are mostly results from tiny hacks during experiments but maybe they are also useful for other people.

Binaries
========
Binaries (JARs) for most of these libraries and tools should be available from the LSKI Maven repository:

https://breda.informatik.uni-mannheim.de/nexus/index.html#nexus-search;quick~de.krkm
    
to directly use them as Maven dependencies, include

    <repositories>
        <repository>
            <id>lski</id>
            <url>https://breda.informatik.uni-mannheim.de/nexus/content/groups/public/</url>
        </repository>
    </repositories>
    
into the pom.xml of your Maven project.

License
=======
Probably, most of these tools have to be under AGPL3 at least those which are using Pellet as a library. But this is
something I have to look into in more detail later. Actually, Apache License Version 2 is the preferred one, if
possible...

Details
=======
collection-to-string-wrapper
----------------------------
Small class for giving Java Collections useful toString() methods when used in slf4j

ontology-minimizer
------------------
Tool for determining the core of an ontology, i.e., an ontology having the same semantic closure containing
no redundant axioms.

owl-annotated-axiom-extractor
-----------------------------
Tool for extracting annotated axioms from an ontology and sort them by the value in a specific annotation.

owl-complexity-reducer
----------------------
Tool to remove axioms from ontologies which are unsupported by the Pellet reasoner.

owl-confidence-histogram
------------------------
Generates lists of all confidence values found throughout an ontology, useful for creating histograms of confidence values.

owl-random-partitioner
----------------------
Splits an ontology randomly into a given number of partitions.

owl-subproperty-cycle-remover
-----------------------------
Removes (simple) cycles caused by OWL axioms.