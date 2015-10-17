# Permissions guide

### Global

* **admin**: access to everything
* **user**: only guarantees a login with permissions configured on organism basis


### Organism

Can only view things related to that organism.

* **read**: view / search only, no annotation

``` 
    Annotations: lock detail / coding
    RefSeq: hide export
    Organism: hide
    User: hide 
    Group: hide 
    Preferences: hide 
    JBrowse: disable UcA track 
```
* **export**: same as read, but can use the export screen

``` 
    RefSeq: show export 
```

* **write**: same as above, but can add / edit annotations

``` 
    Annotations: allow editing
    JBrowse: enable UcA track 
```

* **admin**: access to everything for that organism

``` 
    Organism: show
    User: show 
    Group: show
    Preferences: (still hide)
```


Table of permissions:

``` 
| Permission | Annotator          | Users/groups  | Annotations         | Organism                  |
|------------|--------------------|---------------|---------------------|---------------------------|
| READ       | visible / locked   | hide          | visible / no export | visible                   |
| EXPORT     | visible / locked   | hide          | visible / export    | visible                   |
| WRITE      | visible + editable | hide          | visible / export    | visible                   |
| ADMIN      | visible + editable | visible       | visible /export     | visible + admin functions |
| NONE       | not available      | not available | not available       | not visible               |
```

The Preference panel is available only for GLOBAL admin.
