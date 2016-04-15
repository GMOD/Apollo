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
            %{--<input type="username" class="form-control" placeholder="Username (email)" required autofocus>--}%
            %{--<input type="password" class="form-control" placeholder="Password" required>--}%
            %{--<button class="btn btn-lg btn-primary btn-block" type="submit">--}%
            %{--Sign in</button>--}%
            %{--<label class="checkbox pull-left">--}%
            %{--<input type="checkbox" value="${rememberMe}">--}%
            %{--Remember me--}%
            %{--</label>--}%
            %{--<table>--}%
            %{--<tbody>--}%
            %{--<tr>--}%
            %{--<td>Username:</td>--}%
            %{--<td>--}%
            <input name="username" value="${username}" type="username" class="form-control col-md-4"
                   style="margin:10px;"
                   placeholder="Username (email)"
                   required autofocus/>
            %{--</td>--}%
            %{--</tr>--}%
            %{--<tr>--}%
            %{--<td>Password:</td>--}%
            %{--<td><input type="password" name="password" value="" /></td>--}%
            <input type="password" name="password" value="" class="form-control" placeholder="Password" required
                   style="margin:10px;"/>
            %{--</tr>--}%
            %{--<tr>--}%
            %{--<td>Remember me?:</td>--}%
            %{--<td><g:checkBox name="rememberMe" value="${rememberMe}" /></td>--}%
            %{--<div class="col-md-5 row" style="margin-bottom: 10px;">--}%
                <input class="col-md-4 col-md-offset-1 btn btn-lg btn-primary" type="submit"
                       value="Login">

                <label class="checkbox col-md-4" style="margin:10px;">
                    <g:checkBox name="rememberMe" value="${rememberMe}"/>
                    <input class="checkbox" type="checkbox" name="rememberMe" value="${rememberMe}"/>
                    Remember me
                </label>
            %{--</div>--}%

            %{--<div class="col-md-5 row">--}%
            %{--</div>--}%
            %{--</tr>--}%
            %{--<tr>--}%
            %{--<td />--}%
            %{--<input type="submit" value="Sign in"/>--}%
            %{--<button class="btn btn-lg btn-primary btn-block" type="submit" value="Login">--}%
            %{--<td><input type="submit" value="Sign in" /></td>--}%
            %{--</tr>--}%
            %{--</tbody>--}%
            %{--</table>--}%
        </div>
    </g:form>
</body>
</html>
