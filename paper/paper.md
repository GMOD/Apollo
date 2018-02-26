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
 - name: Monica Munoz-Torres
   affiliation: Lawrence Berkeley National Labs 
 - name: Deepak Unni
   affiliation: University of Missouri, Columbia
 - name: Colin Diesh
   affiliation: University of Missouri, Columbia
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


## A summary describing the high-level functionality and purpose of the software for a diverse, non-specialist audience

Apollo is a web-based genome annotation editor that allows biologists to manually edit structural and functional
(metadata) genome annotations in a collaborative environment similar to Google Docs.  In order to work within a
bioinformatics workflow, Apollo must be easy to install / use for a single user, but scale to large organizations
collaboratively editing multiple organisms as well as integrate with current bioinformatics software systems.   As such,
we chose the [Grails](https://grails.org/) platform, a Java-based Rapid Application Developement environment that
supports a multiple relational database schemas.  On the front-end we chose a combination of [Google Web
Toolkit](http://www.gwtproject.org/) and the [JBrowse](http://jbrowse.org) genomic viewer which Apollo integrates into
as a plugin.  All of these interact with the backend through a combination of REST web-services and websockets using the
STOMP protocol.  These web-services, in turn are available to the users scripting integration with other systems.


## A clear statement of need that illustrates the purpose of the software

Apollo is currently being used in over one hundred genome annotation projects around the world, ranging from annotation
of a single species to lineage-specific efforts supporting the annotation of dozens of species at a time.

## Mentions (if applicable) of any ongoing research projects using the software
or recent scholarly publications enabled by it

Citations to entries in paper.bib should be in
[rMarkdown](http://rmarkdown.rstudio.com/authoring_bibliographies_and_citations.html)
format.

This is an example citation [@figshare_archive].

Figures can be included like this: ![Fidgit deposited in figshare.](figshare_article.png)

## A list of key references including a link to the software archive

* Apollo 2.0.8 Release [![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.1063658.svg)](https://doi.org/10.5281/zenodo.1063658)
* https://github.com/gmod/apollo/
* http://genomearchitect.org  
* Lee E, Helt GA, Reese JT, Munoz-Torres MC, Childers CP, Buels RM, Stein L, Holmes IH, Elsik CG, Lewis SE. 2013. Apollo: a web-based genomic annotation editing platform. [Genome Biol 14:R93](http://genomebiology.com/2013/14/8/R93/abstract)
* docker compose annotation releases 
* Docker: https://github.com/GMOD/docker-apollo
* Galaxy: https://github.com/GMOD/docker-compose-galaxy-annotation
* License: Berkeley Software Distribution (BSD) License. See https://github.com/GMOD/Apollo/blob/master/LICENSE.md 


