---
title: 'Apollo: Collaborative and Scalable Genome Annotation Software'
tags:
  - biocuration
  - genome annotation
  - bioinformatics
authors:
 - name: Nathan A Dunn
   orcid: 0000-0002-4862-3181
   affiliation: Lawrence Berkeley National Labs 
 - name: Deepak Unni
   affiliation: University of Missouri, Columbia
 - name: Colin Diesh
   affiliation: University of Michigan
 - name: Monica Munoz-Torres
   affiliation: Phoenix Bioinformatics
 - name: Eric Yao
   affiliation: University of California, Berkeley
 - name: Ian Holmes
   affiliation: University of California, Berkeley
 - name: Christine Elsik
   affiliation: University of Missouri, Columbia
 - name: Suzanna E Lewis
   affiliation: Lawrence Berkeley National Labs 

date: 31 August, 2016
bibliography: 
---

# Summary

[] (Add note about Community developers, users, feedback.)
[] (Add note about using standard tools on the backend that allow to scalability of hundreds of users and genomes with rich permission structure.)
[] (Software adds ability to get up and running and add integrations relatively easily.)



[](JOSS Note: A summary describing the high-level functionality and purpose of the software for a diverse, non-specialist audience)

[](Something about google docs and genome editing for folks who don't know anything about biology)

Genome annotation, the process of describing the function and structure of genomic elements (e.g., genes that create proteins that makeup biological organism) is a key step when describing the DNA that makes up an organism.
As the ability to sequence genomes has become cheaper, genome annotation projects have become more common as well as both more distributed and collaborative. 
While the quality of automated annotation tools (cite: Maker, Augustus, XXX) and workflows (cite: Galaxy, Cyverse?, XXX) has improved greatly, the need for manual annotation remains (XX: anything to cite?).

Apollo is a web-based genome annotation editor that allows biologists to manually edit structural and functional (metadata) genome annotations in a collaborative environment similar to Google Docs.  

Multiple users are able to edit genome annotations and have collaborators see the results in real-time. 

Additionally, in order to work within a bioinformatics workflow, Apollo is easy to install / use for a single user, but scales to integrate within diverse workflows and collaborative projects editing multiple organisms.   


As such, we chose the [Grails](https://grails.org/) platform, a Java-based Rapid Application Developement environment that
supports a multiple relational database schemas.  On the front-end we chose a combination of [Google Web
Toolkit](http://www.gwtproject.org/) and the [JBrowse](http://jbrowse.org) genomic viewer which Apollo integrates into
as a plugin.  All of these interact with the backend through a combination of REST web-services and websockets using the
STOMP protocol.  These web-services, in turn are available to the users scripting integration with other systems.


[](JOSS Note: A clear statement of need that illustrates the purpose of the software)

Apollo fulfills two key needs in this space:

1 - It is an effective manual, graphical, editor.  In lieu of this type of tool, users will do this by hand.

[](Some description of some of the feature of the graphical editor.)

2 - In sharp contrast to every other genome annotation tool, Apollo is web-based and real-time collaborative.   


Features:
- Revertible visual history, including merges and splits
- Changeable feature type
- Application side-panel with administrative reports
- Fine-grained permissions for users, instructors., etc..

These features have also allowed Apollo to thrive as an educational tool as well.


[](JOSS Note: Mentions if applicable of any ongoing research projects using the software or recent scholarly publications enabled by it)

Apollo is currently being used in over one hundred genome annotation projects around the world, ranging from annotation
of a single species to lineage-specific efforts supporting the annotation of dozens of species at a time.


[](Add educational uses her as well)

Educational users:  
- DNA Subway
- On-Ramp
- TAMU-CPT

Genome Projects:
- i5K
- two french projects
- look into grant for other stuff 


Key Galaxy Platform Integrations:
- https://github.com/galaxy-genome-annotation
- ??
- GenSAS

[](Figure of Screenshot shared on figshare)


Citations to entries in paper.bib should be in
[rMarkdown](http://rmarkdown.rstudio.com/authoring_bibliographies_and_citations.html)
format.

This is an example citation [@figshare_archive].

Figures can be included like this: ![Fidgit deposited in figshare.](figshare_article.png)

[](A list of key references including a link to the software archive, maybe this goes in codemetata.json)

* [Software repository](https://github.com/gmod/apollo/)
* [![Software archive](https://zenodo.org/badge/DOI/10.5281/zenodo.1063658.svg)](https://doi.org/10.5281/zenodo.1063658)

### References

* http://genomearchitect.org  
* Lee E, Helt GA, Reese JT, Munoz-Torres MC, Childers CP, Buels RM, Stein L, Holmes IH, Elsik CG, Lewis SE. 2013. Apollo: a web-based genomic annotation editing platform. [Genome Biol 14:R93](http://genomebiology.com/2013/14/8/R93/abstract)
* Docker: https://github.com/GMOD/docker-apollo
* Galaxy: https://github.com/GMOD/docker-compose-galaxy-annotation
* License: Berkeley Software Distribution (BSD) License. See https://github.com/GMOD/Apollo/blob/master/LICENSE.md 


