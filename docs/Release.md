## Release steps for Apollo

- Update version in ```application.properties```.
- Update version in ```docs/conf.py```.
- Search doc for specific cases related to ```X.Y.Z```.
- Confirm ChangeLog.md.
- Commit and push on dev.
- In GH Create / Tag a Release reflecting ChangeLog.md with summary on dev. 
- Git push develop to master (with tag).

- Update servers / images:
    - Update demo, download new release and build.  
    - Update stage (from develop).
    - Confirm docker images are updated automatically on [docker hub](https://hub.docker.com/r/gmod/apollo/tags).  With docker hub, will need to pull and tag and push stable and release number, but hopefully should be automatically built.
  - Do announcement on gmod-ajax, apollo.
    


## Post-release

- In develop bump application.properties version to X.Y.Z+1-SNAPSHOT on develop.

