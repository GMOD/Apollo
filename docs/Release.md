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


- Create an Apollo Public AMI:
  - Launch prior version (X.Y.Z-1) AMI as an t2.medium or larger with port 22, 80, and 8080  open.
  - Download X.Y.Z release and decompress it and go into the X.Y.Z directory.
  - Copy `apollo-config.groovy` from (X.Y.Z-1) to (X.Y.Z).
  - Stop tomcat: `sudo service tomcat8 stop`
  - `./apollo deploy`
  - `sudo rm -rf /var/lib/tomcat8/webapps/apollo*`
  - `sudo cp target/apollo-X.Y.Z.war /var/lib/tomcat8/webapps/apollo.war`
  - Restart tomcat: `sudo service tomcat8 restart`
  - Verify running via logs: `tail -f /var/log/tomcat8/catalina.out`
  - Verify running by seeing register screen at http://<host-ip>:8080/apollo  (DO NOT REGISTER)
  - Create an AMI from the running instance.
  - Once created, make the AMI public


- Do announcement on gmod-ajax <gmod-ajax@lists.sourceforge.net>, apollo lists (apollo@lbl.gov).
  

## Post-release

- In develop bump application.properties version to X.Y.Z+1-SNAPSHOT on develop.

