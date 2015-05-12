<%@ page import="org.bbop.apollo.CannedComment" %>



<div class="fieldcontain ${hasErrors(bean: cannedCommentInstance, field: 'comment', 'error')} required">
	<label for="comment">
		<g:message code="cannedComment.comment.label" default="Comment" />
		<span class="required-indicator">*</span>
	</label>
	<g:textField name="comment" required="" value="${cannedCommentInstance?.comment}"/>

</div>

<div class="fieldcontain ${hasErrors(bean: cannedCommentInstance, field: 'metadata', 'error')} required">
	<label for="metadata">
		<g:message code="cannedComment.metadata.label" default="Metadata" />
		%{--<span class="required-indicator">*</span>--}%
	</label>
	<g:textField name="metadata" value="${cannedCommentInstance?.metadata}"/>

</div>

