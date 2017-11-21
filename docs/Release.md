## Rough steps for Release of X.Y.Z

- Update version in ```application.properties```.
- Update version in ```docs/conf.py```.
- Search doc for specific cases related to ```X.Y.Z```.
- Confirm ChangeLog.md.
- Commit and push.
- In GH Create / Tag a Release reflecting ChangeLog.md with summary.

- Update servers / images:
    - Update demo, download new release and build
    - Update docker images (https://github.com/GMOD/docker-apollo), retag for docker hub, and create a release
    - Update AWS images and resubmit.
    - Update AWS  training image.
    
- Update genomearchitect to reflect zenodo and version.
- Generate and update link on master to reflect zenodo link if appropriate.
- Do announcement on gmod-ajax, apollo, and other lists.
