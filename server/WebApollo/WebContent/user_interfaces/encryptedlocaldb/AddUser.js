function addUser() {
	var $content = $('<div title="Add User"></div>');
	var $userDiv = $('<div class="user_login"><span class="fieldname">User name</span><input class="input_field" type="text" id="username" /></div>');
	var $passDiv = $('<div class="user_login"><span class="fieldname">Password</span><input class="input_field" type="password" id="password" /></div>');
	var $addDiv = $('<div class="button_add_user"><button id="add_user_button" onclick="add_user()">Add user</button><button id="clear_button" onclick="clear_fields()">Clear</button></div>');
	
}

<div class="user_login"><span class="fieldname">User name</span><input class="input_field" type="text" id="username" /></div>
<div class="user_login"><span class="fieldname">Password</span><input class="input_field" type="password" id="password" /></div>
<div class="button_add_user"><button id="add_user_button" onclick="add_user()">Add user</button><button id="clear_button" onclick="clear_fields()">Clear</button></div>
