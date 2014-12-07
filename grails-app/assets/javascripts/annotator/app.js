//= require jquery
//= require js/jquery-ui-1.10.3.custom
//= require bootstrap
//= require vendor/angular.min.js
//= require vendor/ui-bootstrap-tpls-0.12.0.min.js
//= require vendor/ui-layout.min.js
//= require_self
//= require_tree controllers
//= require_tree services
var as = angular.module('AnnotatorApplication', ['ui.bootstrap','ui.layout']);

//$(function ()
//{
//    $(".resizable1").resizable(
//        {
//            autoHide: true,
//            handles: 'e',
//            resize: function(e, ui)
//            {
//                var parent = ui.element.parent();
//                var remainingSpace = parent.width() - ui.element.outerWidth(),
//                    divTwo = ui.element.next(),
//                    divTwoWidth = (remainingSpace - (divTwo.outerWidth() - divTwo.width()))/parent.width()*100+"%";
//                divTwo.width(divTwoWidth);
//            },
//            stop: function(e, ui)
//            {
//                var parent = ui.element.parent();
//                ui.element.css(
//                    {
//                        width: ui.element.width()/parent.width()*100+"%",
//                    });
//            }
//        });
//});
