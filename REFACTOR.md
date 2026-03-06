
 why was removing shiro good

● Shiro had no Grails 7 plugin. The old Grails Shiro plugin stopped at Grails 3, so keeping Shiro
  meant depending on raw Shiro libraries without any framework integration — no realm auto-wiring, no
  session management integration, no filter chain support. It was essentially dead weight that would
  break at runtime even though it compiled.

  Spring Security is the standard for Spring Boot (which Grails 7 is built on), so it gets automatic
  session management, security context propagation across threads, and compatibility with the rest of
  the Spring ecosystem out of the box.

