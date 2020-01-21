# Adding OpenID Connect Authentication to Apollo

<!-- toc -->

- [Overview](#overview)
- [Architecture](#architecture)
  * [Securing Tomcat](#securing-tomcat)
  * [Authorization](#authorization)
- [Configuration](#configuration)
  * [Apache 2.4 reverse proxy configuration](#apache-24-reverse-proxy-configuration)
  * [mod_auth_openidc configuration](#mod_auth_openidc-configuration)
  * [Combining OIDC with the proxy](#combining-oidc-with-the-proxy)
  * [Apollo configuration](#apollo-configuration)
- [Maintaining administrator access](#maintaining-administrator-access)

<!-- tocstop -->

## Overview

OpenID Connect (https://openid.net/connect/), or OIDC, is an authentication 
layer that uses the OAuth 2 protocol.

It allows you to devolve authentication to an _Identity Provider_.   It is the 
Identity Provider who registers users, provides the infrastructure (back end 
database, log-in page, "forgotten your password?" functions, etc.), and bears 
Data Protection responsibility -- though of course it means that they ultimately 
have control over who can access your Apollo instance.

It is most likely to be useful if you have some relation to an Identity Provider 
that represents your organization or user community, or if you intend to provide 
public access and only require an arbitrary identifier for an end user (to 
ensure it is the same individual each time they start an Apollo session) without 
any need to know their real-world identity.

## Architecture

The method described here uses the following components:

1. An Apache httpd 2.4 web server (https://httpd.apache.org/)
2. The `mod_auth_openidc` Apache module (https://www.mod-auth-openidc.org/)
3. The `remoteUserAuthenticatorService` class provided by Apollo

The Apache httpd is deployed to provide a reverse proxy as the sole point of 
access to Apollo for end users. Its primary role is to allow the use of 
mod_auth_openidc to add OIDC access control in front of Apollo, but of course it 
is also an efficient way to serve static content (e.g. user guides).  It also 
makes it very easy to place multiple services or sources of content behind the 
same access control layer.  Documentation for setting up a reverse proxy is
available at https://httpd.apache.org/docs/2.4/howto/reverse_proxy.html.

mod_auth_openidc is the module that adds OIDC authentication to Apache.  The usage
described here is only the simplest case, but this module offers a lot of
functionality, including the option of letting end users choose between multiple
Identity Providers. The module will intercept requests for protected resources, and redirect
the end user to the Identity Provider so they can log in; after a successful log 
in, the end user is redirected back to the httpd, which serves the protected
content.  OIDC data are made available in the
Apache environment, so that applications run by the httpd (including the reverse
proxy server) can access values such as the authenticated User Identifier.

The RemoteUserAuthenticatorService class in Apollo is part of the standard 
distribution.  It is used to grant access to end users who present with the 
REMOTE_USER HTTP header.  The Apache reverse proxy can configured to pass the
User Identifier, retrieved during OIDC authentication, as a REMOTE_USER header:
thus users who have successfully authenticated via OIDC will be granted access
to Apollo.

### Securing Tomcat

Because RemoteUserAuthenticatorService gives access to any end user who sends a
REMOTE_USER header (and any reasonably savvy user can add whatever HTTP headers
they wish to any request sent by their browser), the Tomcat that
serves Apollo must not be directly accessible to untrusted end users.  For example,
the Tomcat port could be made accessible only on _localhost_, or on a corporate
private network.

### Authorization

The end result of the process described above is an end user who is
_authenticated_, i.e. you know 
the request comes from someone who was able to log in to an account associated 
with the User identifier you received.  The user is not _authorized_ at this 
stage, i.e. they have no permission to access any non-public resources within 
Apollo.

You can configure Apollo automatically to add all Remote Users to a default user group,
which will authorize them according to the access permissions granted to that group.
If you are willing immediately to grant full access to 
all Remote Users authenticated by your Identity provider, this is all that is
required.

If you want to have finer control over end users' access rights, you can use 
normal admin processes (user interface or API) to grant access -- but be aware 
that end users' accounts are only created in Apollo when they log in for the 
first time.  That means you cannot grant them access until _after_ they first 
log in -- so their first session in Apollo will consist of nothing but a "you 
are not authorized to view any organisms" message. It is probably better to have a 
default group for Remote Users that provides limited (read only?) access, and a 
process for adding additional access.

## Configuration

### Apache 2.4 reverse proxy configuration

Apache can be configured to add the reverse proxy server independently from adding the OIDC
access control (it is probably a good idea to add reverse proxying first as it will make any
configuration problems easier to find).  Reverse proxying on its own should be completely
transparent to end users.

The correct Apache proxy configuration described in the Apollo
documentation at
https://genomearchitect.readthedocs.io/en/latest/Configure.html#apache-proxy

This should result in these four proxying modules being enabled in your httpd
conf file(s), with directives like this:
```
LoadModule proxy_module modules/mod_proxy.so
LoadModule proxy_connect_module modules/mod_proxy_connect.so
LoadModule proxy_http_module modules/mod_proxy_http.so
LoadModule proxy_wstunnel_module modules/mod_proxy_wstunnel.so
```
If you wish, you should be able to edit the Apache configuration manually to enable
these modules, rather than use `a2enmod`.  The directives above should already be
in the distributed `httpd.conf` file, commented out.

It is a good idea to use a _VirtualHost_ directive to control the requests which
are proxied.  For instance to proxy all requests on port 80 to a tomcat running
on the same machine using port 8080:
```
<Proxy *>
   Require all granted
</Proxy>

<VirtualHost *:80>

   ServerAdmin       <your admin email>
   ServerName        <your apollo host>
   ProxyPreserveHost On
   ProxyRequests     Off

   ProxyPass         /stomp/info    http://localhost:8080/stomp/info
   ProxyPassReverse  /stomp/info    http://localhost:8080/stomp/info

   ProxyPass         /stomp         ws://localhost:8080/stomp
   ProxyPassReverse  /stomp         ws://localhost:8080/stomp

   ProxyPass         /              http://localhost:8080/
   ProxyPassReverse  /              http://localhost:8080/

</VirtualHost>
```
(Substitute your admin/support email address and the host name where indicated.)

You can tweak the VirtualHost settings so that requests are proxied according to
criteria such as port, host name etc. (see Apache 2.4 documentation for details).
This can be useful to give you access direct to Apollo, bypassing the OIDC log in,
say for admin access or local user accounts (see below).

#### Dockerfile

This Dockerfile will provide an httpd container (including the install of
mod_auth_openidc, used in the next section).  Put the conf file(s) you
wish the httpd to use in _apache2-config_ (a subdirectory of the docker build
directory).
```
FROM  httpd:2.4

ENV   DEBIAN_FRONTEND noninteractive

RUN   apt-get -qq update && \
      apt-get install --yes ca-certificates libapache2-mod-auth-openidc

COPY  apache2-config/  /usr/local/apache2/conf/
```

If you are using docker, the Apache reverse proxy configuration will need to
refer to the host running the Apollo tomcat server (if you were to use _localhost_
in the proxy configuration, it would refer to the docker container in which the
httpd is running, _not_ to the host machine on which you are running the container).
It is good practice in docker for the tomcat to run in a separate
container from the httpd.  On a docker network, you use container names as host
names; so if the tomcat container was named _apollo-tomcat_, then you would use
`http://apollo-tomcat:8080/` (etc.) in the proxy configuration directives.

### mod_auth_openidc configuration

Before starting this part of the configuration, you will need to register with
an OIDC provider.   If you do not have one already, the developer tools provided
by ORCID (https://orcid.org/developer-tools) allow a quick and easy set up for
personal development use.

OIDC can be enabled with this addition to the httpd configuration:
```
LoadModule auth_openidc_module /usr/lib/apache2/modules/mod_auth_openidc.so

<Location />
   AuthType openid-connect
   Require  valid-user
</Location>
<Location /public>
   AuthType None
   Require  all granted
</Location>

OIDCPassClaimsAs        environment

OIDCProviderMetadataURL <URL provided by your identity provider>
OIDCClientID            <ID issued to you by your identity provider>
OIDCClientSecret        <secret issued to you by your identity provider>

OIDCScope               "openid email profile"
# vanity URL points protected path but NOT to any content
OIDCRedirectURI         http://<your host name>/apollo/annotator/openid
OIDCCryptoPassphrase    <generate your own random string for this>
```

Your Identity Provider will give you the values for `OIDCProviderMetadataURL`
(conventionally at `/.well-known/openid-configuration`), 
`OIDCClientID` and `OIDCClientSecret` when you register your client. Refer to
their documentation to find supported scopes to include in `OIDCScope`; _openid_
is required for authentication, but _email_
and _profile_ (requests for email address and profile information) will probably
be supported as well if you need them.

When you register, you will also need to provide the URL to which the Identity
Provider should redirect end users after authentication.  Add this to your
Apache configuration as `OIDCRedirectURI`.  This URL is used internally by
mod_auth_openidc and it should *not* be a "real" URL that references actual
content -- but must be something covered by the OIDC access control (see below).

`OIDCCryptoPassphrase` is used internally; just create a random string.


### Combining OIDC with the proxy

Once OIDC has been enabled as desribed above, access control is just standard
Apache 2.4 configuration, with `openid-connect` as the AuthType.  This is
commonly done using _Location_ directive(s) to define the path(s) of content
to which access control is applied, but Apache provides many flexible methods;
e.g. for complicated access control rules, regular expression matching 
directives (like _LocationMatch_) are worth a look.

The final bit of Apache configuration required is a _RequestHeader_ directive.
This passes the OIDC User Identifier (which mod_auth_openidc makes available
as an Apache environment variable) downstream in proxied requests as a
REMOTE_USER HTTP header.

The example below extends the reverse proxy example (above), adding two
`Location` directives that place all content served by this
Virtual Host behind the OIDC access control, except content in `/public` which
remains freely accessible; and adding a `RequestHeader` directive to send a
REMOTE_USER header downstream to Apollo:

```
<Proxy *>
   Require all granted
</Proxy>

<VirtualHost *:80>

   ServerAdmin       <your admin email>
   ServerName        <your apollo host>
   ProxyPreserveHost On
   ProxyRequests     Off

   # OIDC log in will be required for everything...
   <Location />
      AuthType openid-connect
      Require  valid-user
   </Location>

   # ...except for public access content here
   <Location /public>
      AuthType None
      Require  all granted
   </Location>

   RequestHeader     set   Remote_User    "expr=%{REMOTE_USER}"

   ProxyPass         /stomp/info    http://localhost:8080/stomp/info
   ProxyPassReverse  /stomp/info    http://localhost:8080/stomp/info

   ProxyPass         /stomp         ws://localhost:8080/stomp
   ProxyPassReverse  /stomp         ws://localhost:8080/stomp

   ProxyPass         /              http://localhost:8080/
   ProxyPassReverse  /              http://localhost:8080/

</VirtualHost>
```


### Apollo configuration

When the Apache is fully configured as described above, requests from all
authenticated users will include the REMOTE_USER header.   Apollo must be
configured to use Remote User authentication, to make it grant access to all
users who present with this header.

Add the following to `apollo-config.groovy`
```
apollo {
   authentications = [
        ["name":"Remote User Authenticator",
         "className":"remoteUserAuthenticatorService",
         "active":true,
         "params":["default_group": "remote_users"],
        ]
        ,
        ["name":"Username Password Authenticator",
         "className":"usernamePasswordAuthenticatorService",
         "active":true,
        ]
   ]
}
```

Note that `params` defines a default group.   This is optional but recommended
(see note above regarding authorization).   The named group must have been created, with the user interface
or API, and appropriate organism access rights defined.   All Remote Users will
be placed in this group when they first log in.


## Maintaining administrator access

Access to the administrator account, or any other local user accounts that 
you want to use, require that OIDC authentication is bypassed -- simply because
that stops you seeing the Apollo log-in dialog.

If you have access direct to Tomcat on (probably) port 8080 of the host machine,
you can use that for direct access to Apollo.  Because you have bypassed the
OIDC authentication, your browser will not send a REMOTE_USER header -- so you
should see the Apollo local user log-in dialog.

If direct access to the Tomcat port is a problem, you can simply add another
Virtual Host to the Apache configuration.  This can provide access via a
different host name or port.  For example, to give access on port 8000:
```
Listen 8000
<VirtualHost *:8000>

   ServerAdmin       <your admin email>
   ServerName        <your apollo host>
   ProxyPreserveHost On
   ProxyRequests     Off

   <Location />
      AuthType None
      Require  all granted
   </Location>

   ProxyPass         /stomp/info    http://localhost:8080/stomp/info
   ProxyPassReverse  /stomp/info    http://localhost:8080/stomp/info

   ProxyPass         /stomp         ws://localhost:8080/stomp
   ProxyPassReverse  /stomp         ws://localhost:8080/stomp

   ProxyPass         /              http://localhost:8080/
   ProxyPassReverse  /              http://localhost:8080/

</VirtualHost>
```
To keep the Apollo secure (REMOTE_USER headers are easily spoofed), if this port
is openly accessible then some access control should be added, e.g. with
`Require ip <your.ip.address.here>`.
