## Rough steps for Release of X.Y.Z

- Update version in ```application.properties```.
- Update version in ```docs/conf.py```.
- Search doc for specific cases related to ```X.Y.Z```.
- Confirm ChangeLog.md.
- Commit and push.
- In GH Create / Tag a Release reflecting ChangeLog.md with summary.

- Update servers / images:
    - Update icebox (demo -> retire, download new release and build, staging -> master)
    - Update docker images (https://github.com/GMOD/docker-apollo)
    - Update AWS images and resubmit.
    
- Update genomearchitect to reflect zenodo and version.
- Generate and update link on master to reflect zenodo link if appropriate.
- Do announcement on gmod-ajax, apollo, and other lists.
