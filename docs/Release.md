## Rough steps for Release of X.Y.Z

- Update version in ```application.properties```.
- Update version in ```docs/conf.py```.
- Search doc for specific cases related to ```X.Y.Z```.
- Confirm ChangeLog.md.
- Commit and push.
- In GH Create / Tag a Release reflecting ChangeLog.md with summary.
- Git push develop to master

- Update servers / images:
    - Update demo, download new release and build.
    - Confirm docker images are updated automatically on [quay.io](https://quay.io/repository/gmod/apollo?tab=builds) and [docker hub](https://hub.docker.com/r/gmod/apollo/tags).  With docker hub, will need to pull and tag and push stable and release number.
    - (1) Update AWS empty images and data image, (2)  create newer versions of the images from the running instances, (3) test, (4) distribute to N. Virginia and N. California.
    
Zenodo:     
  - Update README.md to reflect zenodo.
  - Generate and update link on master to reflect zenodo link if appropriate.
  - Do announcement on gmod-ajax, apollo, and other lists.


## Post-release

- In develop bump application.properties version to X.Y.Z+1-SNAPSHOT

