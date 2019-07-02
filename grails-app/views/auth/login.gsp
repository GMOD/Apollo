<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="login"/>
    <title>Login</title>
</head>

<div>
    <g:if test="${flash.message}">
        <div class="message">${flash.message}</div>
    </g:if>
    <g:form action="signIn">
        <input type="hidden" name="targetUri" value="${targetUri}" class="col-md-4"/>
        <div class="col-md-5 col-lg-offset-1" style="margin-top: 10px;">
            <input name="username" value="${username}" type="username" class="form-control col-md-4"
                   style="margin:10px;"
                   placeholder="Username (email)"
                   required autofocus/>
            <input type="password" name="password" value="" class="form-control" placeholder="Password" required
                   style="margin:10px;"/>
                <input class="col-md-4 col-md-offset-1 btn btn-lg btn-primary" type="submit"
                       value="Login">

                <label class="checkbox col-md-4" style="margin:10px;">
                    <g:checkBox name="rememberMe" value="${rememberMe}"/>
                    <input class="checkbox" type="checkbox" name="rememberMe" value="${rememberMe}"/>
                    Remember me
                </label>
        </div>
    </g:form>
</body>
</html>
