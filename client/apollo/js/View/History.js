define( [
            'dojo/_base/declare',
            'dijit/Menu',
            'dijit/MenuItem', 
            'dijit/PopupMenuItem',
            'dijit/form/Button',
            'WebApollo/JSONUtils',
            'JBrowse/Util', 
            'JBrowse/View/GranularRectLayout',
            'dojo/request/xhr'
        ],
        function( declare, 
          dijitMenu,
          dijitMenuItem,
          dijitPopupMenuItem,
          dijitButton,
          JSONUtils, 
          Util,
          Layout,
          xhr)
        {

var context_path='..';
return declare([],{

    getHistory: function()  {
        var selected = this.selectionManager.getSelection();
        this.selectionManager.clearSelection();
        this.getHistoryForSelectedFeatures(selected);
    }, 

    getHistoryForSelectedFeatures: function(selected) {
        var track = this;
        var content = dojo.create("div");
        var historyDiv = dojo.create("div", { className: "history_div" }, content);
        var historyTable = dojo.create("div", { className: "history_table" }, historyDiv);
        var historyHeader = dojo.create("div", { className: "history_header", innerHTML: "<span class='history_header_column history_column_operation history_column'>Operation</span><span class='history_header_column history_column'>Editor</span><span class='history_header_column history_column'>Date</span>" }, historyTable);
        var historyRows = dojo.create("div", { className: "history_rows" }, historyTable);
        var historyPreviewDiv = dojo.create("div", { className: "history_preview" }, historyDiv);
        var history;
        var selectedIndex = 0;
        var minFmin = undefined;
        var maxFmax = undefined;
        var current;
        var historyMenu;
        var canEdit = this.canEdit(selected[0].feature);

        function revert() {
            if (selectedIndex == current) {
                return;
            }
            if (selectedIndex < current) {
                track.undoFeaturesByUniqueName([ history[0].features[0].uniquename ], current - selectedIndex);
            }
            else if (selectedIndex > current) {
                track.redoFeaturesByUniqueName([ history[0].features[0].uniquename ], selectedIndex - current);
            }
            history[selectedIndex].current = true;
            history[current].current = false;
            dojo.attr(historyRows.childNodes.item(selectedIndex), "class", history[selectedIndex].current ? "history_row history_row_current" : "history_row");
            dojo.attr(historyRows.childNodes.item(current), "class", "history_row");
            current = selectedIndex;
        };
        
        function initMenu() {
            historyMenu = new dijitMenu({ });
            historyMenu.addChild(new dijitMenuItem({
                label: "Set as current",
                onClick: function() {
                    revert();
                }
            }));
            historyMenu.startup();
        }
        
        var cleanupDiv = function(div) {
            if (div.style.top) {
                div.style.top = null;
            }
            if (div.style.visibility)  { div.style.visibility = null; }
            // annot_context_menu.unBindDomNode(div);
            $(div).unbind();
            for (var i = 0; i < div.childNodes.length; ++i) {
                cleanupDiv(div.childNodes[i]);
            }
        };

        var displayPreview = function(index) {
            var historyItem = history[index];
            var afeature = historyItem.features[0];
            var jfeature = JSONUtils.createJBrowseFeature(afeature);
            var fmin = afeature.location.fmin;
            var fmax = afeature.location.fmax;
            var maxLength = maxFmax - minFmin;
            // track.featureStore._add_getters(track.attrs.accessors().get, jfeature);
            historyPreviewDiv.featureLayout = new Layout(fmin, fmax);
            historyPreviewDiv.featureNodes = new Array();
            historyPreviewDiv.startBase = minFmin - (maxLength * 0.1);
            historyPreviewDiv.endBase = maxFmax + (maxLength * 0.1);
            var coords = dojo.position(historyPreviewDiv);
            // setting labelScale and descriptionScale parameter to 100 px/bp,
            // so neither should get triggered
            var featDiv = track.renderFeature(jfeature, jfeature.uid, historyPreviewDiv, coords.w / (maxLength), 100, 100, minFmin, maxFmax, true);
            cleanupDiv(featDiv);
            
            historyMenu.bindDomNode(featDiv);
            
            while (historyPreviewDiv.hasChildNodes()) {
                historyPreviewDiv.removeChild(historyPreviewDiv.lastChild);
            }
            historyPreviewDiv.appendChild(featDiv);
            dojo.attr(historyRows.childNodes.item(selectedIndex), "class", history[selectedIndex].current ? "history_row history_row_current" : "history_row");
            dojo.attr(historyRows.childNodes.item(index), "class", "history_row history_row_selected");
            selectedIndex = index;
        };

        var displayHistory = function() {
            for (var i = 0; i < history.length; ++i) {
                var historyItem = history[i];
                var rowCssClass = "history_row";
                var row = dojo.create("div", { className: rowCssClass }, historyRows);
                var columnCssClass = "history_column";
                dojo.create("span", { className: columnCssClass + " history_column_operation ", innerHTML: historyItem.operation }, row);
                dojo.create("span", { className: columnCssClass, innerHTML: historyItem.editor }, row);
                dojo.create("span", { className: columnCssClass + " history_column_date", innerHTML: historyItem.date }, row);
                var revertButton = new dijitButton( {
                    label: "Revert",
                    showLabel: false,
                    iconClass: "dijitIconUndo",
                    'class': "revert_button",
                    onClick: function(index) {
                        return function() {
                            selectedIndex = index;
                            revert();
                        }
                    }(i)
                });
                if (!canEdit) {
                    revertButton.set("disabled", true);
                }
                dojo.place(revertButton.domNode, row);
                var afeature = historyItem.features[0];
                var fmin = afeature.location.fmin;
                var fmax = afeature.location.fmax;
                if (minFmin == undefined || fmin < minFmin) {
                    minFmin = fmin;
                }
                if (maxFmax == undefined || fmax > maxFmax) {
                    maxFmax = fmax;
                }
                
                if (historyItem.current) {
                    current = i;
                }

                dojo.connect(row, "onclick", row, function(index) {
                    return function() {
                        displayPreview(index);
                    };
                }(i));

                dojo.connect(row, "oncontextmenu", row, function(index) {
                    return function() {
                        displayPreview(index);
                    };
                }(i));

                historyMenu.bindDomNode(row);

            }
            displayPreview(current);
            var coords = dojo.position(row);
            historyRows.scrollTop = selectedIndex * coords.h;
        };
    
        var fetchHistory = function() {
            var features = [];
            for (var i in selected)  {
                var record = selected[i];
                var annot = track.getTopLevelAnnotation(record.feature);
                var uniqueName = annot.id();
                // just checking to ensure that all features in selection are
                // from this track
                if (record.track === track)  {
                    var trackdiv = track.div;
                    var trackName = track.getUniqueTrackName();

                    features.push({ "uniquename": uniqueName });
                }
            }
            var operation = "get_history_for_features";
            var trackName = track.getUniqueTrackName();
            dojo.xhrPost( {
                postData: JSON.stringify( { "track": trackName, "features": features, "operation": operation }),
                url: context_path + "/AnnotationEditorService",
                handleAs: "json",
                timeout: 5000 * 1000, // Time in milliseconds
                load: function(response, ioArgs) {
                    var features = response.features;
                    history = features[i].history;
                    displayHistory();
                },
                // The ERROR function will be called in an error case.
                error: function(response, ioArgs) { // 
                    track.handleError(response);
                    return response; // 
                }

            });
        };

        initMenu();
        fetchHistory();
        this.openDialog("History", content);
        this.popupDialog.resize();
        this.popupDialog._position();
    }


});
});
