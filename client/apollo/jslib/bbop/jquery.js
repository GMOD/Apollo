/* 
 * Package: jquery.js

 * 
 * Namespace: bbop.golr.manager.jquery
 * 
 * jQuery BBOP manager for dealing with actual ajax calls. Remember,
 * this is actually a "subclass" of <bbop.golr.manager>.
 * 
 * This should still be able to limp along (no ajax and no error
 * parsing) even outside of a jQuery environment.
 */


// Setup the internal requirements.
bbop.core.require('bbop', 'core');
bbop.core.require('bbop', 'registry');
bbop.core.require('bbop', 'golr', 'conf');
bbop.core.require('bbop', 'golr', 'response');
bbop.core.require('bbop', 'golr', 'manager');
bbop.core.namespace('bbop', 'golr', 'manager', 'jquery');

/*
 * Constructor: jquery
 * 
 * Contructor for the GOlr query manager
 * 
 * Arguments:
 *  golr_loc - string url to GOlr server;
 *  golr_conf_obj - a <bbop.golr.conf> object
 * 
 * Returns:
 *  golr manager object
 * 
 * See also:
 *  <bbop.golr.manager>
 */
bbop.golr.manager.jquery = function (golr_loc, golr_conf_obj){
    bbop.golr.manager.call(this, golr_loc, golr_conf_obj);
    this._is_a = 'bbop.golr.manager.jquery';
    
    // Get a good self-reference point.
    var anchor = this;

    // Per-manager logger helper.
    function ll(str){ anchor._logger.kvetch(str); }

    // Before anything else, if we cannot find a viable jQuery library
    // for use, we're going to create a fake one so we can still test
    // and work in a non-browser/networked environment.
    anchor.JQ = new bbop.golr.faux_ajax();
    try{ // some interpreters might not like this kind of probing
    	if( typeof(jQuery) !== 'undefined' ){
    	    //JQ = jQuery;
//    	    anchor.JQ = jQuery.noConflict();
    		anchor.JQ = jQuery;
    	}
    }catch (x){
    }finally{
    	var got = bbop.core.what_is(anchor.JQ);
    	if( got && got == 'bbop.golr.faux_ajax'){
    	}else{
    	    got = 'jQuery';
    	}
    	ll('Using ' + got + ' for Ajax calls.');
    }

    // The base jQuery Ajax args we need with the setup we have.
    anchor.jq_vars = {
	//url: qurl,
	type: "GET",
	dataType: 'jsonp',
	jsonp: 'json.wrf'
    };

    // We'll override the original with something that actually speaks
    // jQuery. This is the function that runs where there is an AJAX
    // error during an update. First it has to run some template code,
    // then it does all of the callbacks.
    this._run_error_callbacks = function(result, status, error) {

    	ll('Failed server request: '+ result +', '+ status +', '+ error);
    	//ll('Failed (a): '+ bbop.core.what_is(status));
    	//ll('Failed (b): '+ bbop.core.dump(status));
		
    	var clean_error = "unknown error";

    	// Get the error out (clean it) if possible.
    	var jreq = result.responseText;
    	var req = anchor.JQ.parseJSON(jreq); // TODO/BUG: this must be removed
    	if( req && req['errors'] && req['errors'].length > 0 ){
    	    var in_error = req['errors'][0];
    	    ll('ERROR:' + in_error);
    	    // Split on newline if possible to get
    	    // at the nice part before the perl
    	    // error.
    	    var reg = new RegExp("\n+", "g");
    	    var clean_error_split =
    		in_error.split(reg);
    	    clean_error = clean_error_split[0];
    	}else if( bbop.core.what_is(error) == 'string' &&
    		  error.length > 0){
    	    clean_error = error;
    	}else if( bbop.core.what_is(status) == 'string' &&
    		  status.length > 0){
    	    clean_error = status;
    	}
	
    	// Run all against registered functions.
    	ll('run error callbacks...');
    	anchor.apply_callbacks('error', [clean_error, anchor]);
    };

    // Try and decide between a reset callback and a search callback.
    // This is useful since jQuery doesn't have a natural way to do
    // that within the callbacks.
    this._callback_type_decider = function(json_data){
    	ll('in callback type decider...');

	var response = new bbop.golr.response(json_data);

    	// 
    	if( ! response.success() ){
    	    throw new Error("Unsuccessful response from golr server!");
    	}else{
    	    var cb_type = response.callback_type();
    	    ll('okay response from server, will probe type...: ' + cb_type);
    	    if( cb_type == 'reset' ){
    		anchor._run_reset_callbacks(json_data);
    	    }else if( cb_type == 'search' ){
    		anchor._run_search_callbacks(json_data);
    	    }else{
    		throw new Error("Unknown callback type!");
    	    }
    	}
    };

    /*
     * Function: safety
     *
     * Getter/setter for the trigger safety.
     * 
     * If the safety is on, ajax events controlled by the manager will
     * not occur. The default if off (false).
     * 
     * Parameters: 
     *  safety_on_p - boolean
     *
     * Returns:
     *  boolean
     */
    this.safety = function(safety_on_p){
	if( bbop.core.is_defined(safety_on_p) ){
	    anchor._safety = safety_on_p;
	}
	return anchor._safety;
    };
};
bbop.core.extend(bbop.golr.manager.jquery, bbop.golr.manager);

/*
 * Function: update
 *
 *  See the documentation in <golr_manager.js> on update to get more
 *  of the story. This override function adds functionality for
 *  jQuery.
 * 
 * You can prevent the triggering of ajax with the <safety>
 * method.
 *
 * Parameters: 
 *  callback_type - callback type string
 *  rows - *[serially optional]* integer; the number of rows to return
 *  start - *[serially optional]* integer; the offset of the returned rows
 *
 * Returns:
 *  the query url (with the jQuery callback specific parameters)
 * 
 * Also see:
 *  <get_query_url>
 */
bbop.golr.manager.jquery.prototype.update = function(callback_type,
						     rows, start){
    
    // Get "parents" url first.
    var parent_update = bbop.golr.manager.prototype.update;
    var qurl = parent_update.call(this, callback_type, rows, start);
    
    // Only actually trigger if the safety is off (default).
    if( ! this.safety() ){
	
	//ll('try: ' + qurl);
	//widgets.start_wait('Updating...');
	
	// Setup JSONP for Solr and jQuery ajax-specific parameters.
	this.jq_vars['success'] = this._callback_type_decider; // decide & run
	this.jq_vars['error'] = this._run_error_callbacks; // run error cbs
	//done: _callback_type_decider, // decide & run search or reset
	//fail: _run_error_callbacks, // run error callbacks
	//always: function(){} // do I need this?
	this.JQ.ajax(qurl, this.jq_vars);
    }
    
    return qurl;
};

/*
 * Function: run_batch
 *
 * A distant cousin of <update>.
 * Designed to "serially" get data from a server for
 * certain types of data crunching routines.
 * 
 * Why would you want this? Lets say there are ten distinct things
 * that you want from the server. Coordinating and collating them all
 * without annoying the server or going insane is hard in an
 * asynchronous environment.
 *
 * Parameters: 
 *  accumulator_func - the function that collects
 *  final_func - the function to run on completion
 *
 * Returns:
 *  the number of batch items run
 */
bbop.golr.manager.jquery.prototype.run_batch = function(accumulator_func,
							final_func){

    var anchor = this;

    // Set the various callbacks internally so we can get back at them
    // when we lose our stack during the ajax.
    if( accumulator_func ){ this._batch_accumulator_func = accumulator_func; }
    if( final_func ){ this._batch_final_func = final_func; }

    // Look at how many states are left.
    var qurl = anchor.next_batch_url();
    if( qurl ){
	    
	// Generate a custom callback function that will start
	// this process (next_generator) again--continue the cycle.
	var next_cycle = function(json_data){
	    var response = new bbop.golr.response(json_data);
	    anchor._batch_accumulator_func.apply(anchor, [response, anchor]);
	    anchor.run_batch();
	};
	
	// Put this custom callback on success.
	anchor.jq_vars['success'] = next_cycle;
	anchor.jq_vars['error'] = anchor._run_error_callbacks;
	anchor.JQ.ajax(qurl, anchor.jq_vars);
    }else{
	anchor._batch_final_func.apply(anchor);
    }
};

/*
 * Function: fetch
 *
 * A cousin of <update>, but is made to avoid all of the usual
 * callback functions (except error) and just run the single function
 * from the argument.
 * 
 * Why would you want this? Sometimes you need just a little data
 * without updating the whole interface or whatever.
 *
 * Parameters: 
 *  run_func - the function to run on completion
 *
 * Returns:
 *  n/a
 */
bbop.golr.manager.jquery.prototype.fetch = function(run_func){

    // ...
    var anchor = this;
    var qurl = anchor.get_query_url();
    anchor._run_func = run_func;
    anchor.jq_vars['success'] =
	function(json_data){
	    var response = new bbop.golr.response(json_data);
	    anchor._run_func(response);   
	};
    anchor.jq_vars['error'] = anchor._run_error_callbacks;
    anchor.JQ.ajax(qurl, anchor.jq_vars);
};

/*
 * Namespace: bbop.golr.faux_ajax
 *
 * Constructor: faux_ajax
 * 
 * Contructor for a fake and inactive Ajax. Used by bbop.golr.manager.jquery
 * in (testing) environments where jQuery is not available.
 * 
 * Returns:
 *  faux_ajax object
 */
bbop.golr.faux_ajax = function (){
    this._is_a = 'bbop.golr.faux_ajax';

    /*
     * Function: ajax
     *
     * Fake call to jQuery's ajax.
     *
     * Parameters: 
     *  args - whatever
     *
     * Returns:
     *  null
     */
    this.ajax = function(args){
	return null;
    };
    /*
     * Function: parseJSON
     *
     * Fake call to jQuery's parseJSON.
     *
     * Parameters: 
     *  args - whatever--they are ignored
     *
     * Returns:
     *  ""
     */
    this.parseJSON = function(args){
	return "";
    };
};

