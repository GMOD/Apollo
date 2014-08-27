/*
 * Package: search_box.js
 * 
 * Namespace: bbop.widget.search_box
 * 
 * BBOP object to draw various UI elements that have to do with
 * autocompletion.
 * 
 * This is a completely self-contained UI and manager.
 */


bbop.core.require('bbop', 'core');
bbop.core.require('bbop', 'logger');
bbop.core.require('bbop', 'template');
bbop.core.require('bbop', 'golr', 'manager', 'jquery');
bbop.core.namespace('bbop', 'widget', 'search_box');

/*
 * Constructor: search_box
 * 
 * Contructor for the bbop.widget.search_box object.
 * 
 * This is a specialized (and widgetized) subclass of
 * <bbop.golr.manager.jquery>.
 * 
 * The function for the callback argument should either accept a
 * JSONized solr document representing the selected item or null
 * (nothing found).
 * 
 * While everything in the argument hash is technically optional,
 * there are probably some fields that you'll want to fill out to make
 * things work decently. The options for the argument hash are:
 * 
 *  label_template - string template for dropdown, can use any document field
 *  value_template - string template for selected, can use any document field
 *  minimum_length - wait for this many characters to start (default 3)
 *  list_select_callback - function takes a json solr doc on dropdown selection
 * 
 * To get a better idea on how to use the templates, see the demo page
 * at http://cdn.berkeleybop.org/jsapi/bbop-js/demo/index.html and
 * read the documentation for <bbop.template>.
 * 
 * Arguments:
 *  golr_loc - string url to GOlr server;
 *  golr_conf_obj - a <bbop.golr.conf> object
 *  interface_id - string id of the element to build on
 *  in_argument_hash - *[optional]* optional hash of optional arguments
 * 
 * Returns:
 *  this object
 */
bbop.widget.search_box = function(golr_loc,
				  golr_conf_obj,
				  node,
				  in_argument_hash){
    bbop.golr.manager.jquery.call(this, golr_loc, golr_conf_obj);
    this._is_a = 'bbop.widget.search_box';

    // Aliases.
    var anchor = this;
    var loop = bbop.core.each;
    
    // Per-UI logger.
    var logger = new bbop.logger();
    logger.DEBUG = true;
    function ll(str){ logger.kvetch('W (auto): ' + str); }

    // Our argument default hash.
    var default_hash =
	{
	    'label_template': '{{id}}',
	    'value_template': '{{id}}',
	    'minimum_length': 3, // wait for three characters or more
	    'list_select_callback': function(){}
	};
    var folding_hash = in_argument_hash || {};
    var arg_hash = bbop.core.fold(default_hash, folding_hash);

    // There should be a string interface_id argument.
//    this._interface_id = interface_id;
    this._$node = jQuery(node);
    this._list_select_callback = arg_hash['list_select_callback'];
    var label_tt = new bbop.template(arg_hash['label_template']);
    var value_tt = new bbop.template(arg_hash['value_template']);
    var minlen = arg_hash['minimum_length'];

    // The all-important argument hash. See:
    // http://jqueryui.com/demos/autocomplete/#method-widget
    var auto_args = {
	minLength: minlen,
	// Function for a successful data hit.
	// The data getter, which is making it all more complicated
	// than it needs to be...we need to close around those
	// callback hooks so we have to do it inplace here.
	source: function(request_data, response_hook) {
	    anchor.jq_vars['success'] = function(json_data){
		var retlist = [];
		var resp = new bbop.golr.response(json_data);
		if( resp.success() ){
		    loop(resp.documents(),
			 function(doc){

			     // First, try and pull what we can out of our
			     var lbl = label_tt.fill(doc);

			     // Now the same thing for the return/value.
			     var val = value_tt.fill(doc);

			     // Add the discovered items to the return
			     // save.
			     var item = {
				 'label': lbl,
				 'value': val,
				 'document': doc
			     };
			     retlist.push(item);
			 });
		}
		response_hook(retlist);
	    };

	    // Get the selected term into the manager and fire.
	    //anchor.set_query(request_data.term);
	    anchor.set_comfy_query(request_data.term);
	    anchor.JQ.ajax(anchor.get_query_url(), anchor.jq_vars);
	},
	// What to do when an element is selected.
	select: function(event, ui){
	    var doc_to_apply = null;
	    if( ui.item ){
		doc_to_apply = ui.item.document;
	    }

	    // Only do the callback if it is defined.
	    if( bbop.core.is_defined(anchor._list_select_callback) ){
		anchor._list_select_callback(doc_to_apply);
	    }
	}
    };

    // Set the ball rolling (attach jQuery autocomplete to doc).
//    jQuery('.' + anchor._interface_id).autocomplete(auto_args);
    this._$node.autocomplete(auto_args);

    /*
     * Function: destroy
     * 
     * Remove the autocomplete and functionality from the DOM.
     * 
     * Arguments:
     *  n/a
     * 
     * Returns:
     *  n/a
     */
    this.destroy = function(){
    	this._$node.autocomplete('destroy');
    };

    /*
     * Function: content
     * 
     * Get the current text contents of the search box.
     * 
     * Arguments:
     *  n/a
     * 
     * Returns:
     *  string
     */
    this.content = function(){
	return this._$node.val();
    };

};
bbop.core.extend(bbop.widget.search_box, bbop.golr.manager.jquery);

