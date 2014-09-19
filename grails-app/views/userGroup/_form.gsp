<%@ page import="org.bbop.apollo.UserGroup" %>



<div class="fieldcontain ${hasErrors(bean: userGroupInstance, field: 'users', 'error')} ">
	<label for="users">
		<g:message code="userGroup.users.label" default="Users" />
		
	</label>
	<g:select name="users" from="${org.bbop.apollo.User.list()}" multiple="multiple" optionKey="id" size="5" value="${userGroupInstance?.users*.id}" class="many-to-many"/>

</div>

