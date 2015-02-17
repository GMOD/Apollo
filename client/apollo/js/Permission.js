define(['dojo/_base/declare',
        'dojo/request/xhr'
        ],
       function(declare,
           xhr) {

var Permission = declare(null, {

    constructor: function () {
        this.username=null;
        this.permission=null;
        this.context_path="..";
    },


    getPermission: function( trackName ) {
        var thisObj = this;
        return xhr.post(this.context_path + "/AnnotationEditorService", {
            data: JSON.stringify({ "track": trackName, "operation": "get_user_permission" }),
            handleAs: "json",
            timeout: 5 * 1000 // Time in milliseconds
        }).then(function(response) {
            // The LOAD function will be called on a successful response.
            var permission = response.permission;
            thisObj.permission = permission;
            var username = response.username;
            thisObj.username = username;
        });
    }
});
Permission.NONE= 0x0;
Permission.READ= 0x1;
Permission.WRITE= 0x2;
Permission.ADMIN= 0x8;
return Permission;

});
