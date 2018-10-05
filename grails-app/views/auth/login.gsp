<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="login"/>
    <title>Login</title>
</head>

<div>
        <div class="content" style="margin: 30px; float:left; font-size:12px">
          <br><p></p>
          <p style="text-align:center"><strong>Upon Login You Agree to the Following Information</strong></p>
          <ul>
            <li>You are accessing a U.S. Government information system, which includes (1) this computer, (2) this computer network, (3) all computers connected to this network, and (4) all devices and storage media attached to this network or to a computer on this network. This information system is provided for U.S. Government authorized use only.</li>
            <li>Unauthorized or improper use of this system may result in disciplinary action, as well as civil and criminal penalties.</li>
            <li>By using this information system, you understand and consent to the following:
          <ol>
            <li>You have no reasonable expectation of privacy regarding any communications or data transmittal or stored on this information system. At any time, the government may for any lawful government purpose monitor, intercept, search and seize and communication or data transmission or storage on this information system.</li>
            <li>Any communications or data transmitting or storing on this information system may be disclosed or used for any lawful government purpose.</li>
            <li>Your consent is final and irrevocable. You may not rely on any statements or informal policies purporting to provide you with any expectation of privacy regarding communications on this system, where oral or written, by your supervisor or any other official, except USDA's Chief Information Officer.</li>
         </ol>
         </li>
          </ul>
     </div>

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
