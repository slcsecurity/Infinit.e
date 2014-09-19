/*! kibana - v3.0.0 - 2014-09-02
 * Copyright (c) 2014 Rashid Khan; Licensed Apache License */

define("panels/terms/module",["angular","app","lodash","jquery","kbn"],function(a,b,c,d,e){var f=a.module("kibana.panels.terms",[]);b.useModule(f),f.controller("terms",["$scope","querySrv","dashboard","filterSrv","fields",function(b,d,e,f,g){b.panelMeta={modals:[{description:"Inspect",icon:"icon-info-sign",partial:"app/partials/inspector.html",show:b.panel.spyable}],editorTabs:[{title:"Queries",src:"app/partials/querySelect.html"}],status:"Stable",description:"Displays the results of an elasticsearch facet as a pie chart, bar chart, or a table"};var h={field:"_type",exclude:[],missing:!0,other:!0,size:10,order:"count",style:{"font-size":"10pt"},donut:!1,tilt:!1,labels:!0,arrangement:"horizontal",chart:"bar",counter_pos:"above",spyable:!0,queries:{mode:"all",ids:[]},tmode:"terms",tstat:"total",valuefield:""};c.defaults(b.panel,h),b.init=function(){b.hits=0,b.$on("refresh",function(){b.get_data()}),b.get_data()},b.get_data=function(){if(0!==e.indices.length){b.panelMeta.loading=!0;var h,i,j,k;b.field=c.contains(g.list,b.panel.field+".raw")?b.panel.field+".raw":b.panel.field,h=b.ejs.Request().indices(e.indices),b.panel.queries.ids=d.idsByMode(b.panel.queries),k=d.getQueryObjs(b.panel.queries.ids),j=b.ejs.BoolQuery(),c.each(k,function(a){j=j.should(d.toEjsObj(a))}),"terms"===b.panel.tmode&&(h=h.facet(b.ejs.TermsFacet("terms").field(b.field).size(b.panel.size).order(b.panel.order).exclude(b.panel.exclude).facetFilter(b.ejs.QueryFilter(b.ejs.FilteredQuery(j,f.getBoolFilter(f.ids))))).size(0)),"terms_stats"===b.panel.tmode&&(h=h.facet(b.ejs.TermStatsFacet("terms").valueField(b.panel.valuefield).keyField(b.field).size(b.panel.size).order(b.panel.order).facetFilter(b.ejs.QueryFilter(b.ejs.FilteredQuery(j,f.getBoolFilter(f.ids))))).size(0)),b.inspector=a.toJson(JSON.parse(h.toString()),!0),i=h.doSearch(),i.then(function(a){b.panelMeta.loading=!1,"terms"===b.panel.tmode&&(b.hits=a.hits.total),b.results=a,b.$emit("render")})}},b.build_search=function(a,d){if(c.isUndefined(a.meta))f.set({type:"terms",field:b.field,value:a.label,mandate:d?"mustNot":"must"});else{if("missing"!==a.meta)return;f.set({type:"exists",field:b.field,mandate:d?"must":"mustNot"})}},b.set_refresh=function(a){b.refresh=a},b.close_edit=function(){b.refresh&&b.get_data(),b.refresh=!1,b.$emit("render")},b.showMeta=function(a){return c.isUndefined(a.meta)?!0:("other"!==a.meta||b.panel.other)&&("missing"!==a.meta||b.panel.missing)?!0:!1}}]),f.directive("termsChart",["querySrv",function(b){return{restrict:"A",link:function(f,g){function h(){var a=0;f.data=[],c.each(f.results.facets.terms.terms,function(b){var c;"terms"===f.panel.tmode&&(c={label:b.term,data:[[a,b.count]],actions:!0}),"terms_stats"===f.panel.tmode&&(c={label:b.term,data:[[a,b[f.panel.tstat]]],actions:!0}),f.data.push(c),a+=1}),f.data.push({label:"Missing field",data:[[a,f.results.facets.terms.missing]],meta:"missing",color:"#aaa",opacity:0}),"terms"===f.panel.tmode&&f.data.push({label:"Other values",data:[[a+1,f.results.facets.terms.other]],meta:"other",color:"#444"})}function i(){var a,e;h(),g.css({height:f.row.height}),e=c.clone(f.data),e=f.panel.missing?e:c.without(e,c.findWhere(e,{meta:"missing"})),e=f.panel.other?e:c.without(e,c.findWhere(e,{meta:"other"})),require(["jquery.flot.pie"],function(){try{if("bar"===f.panel.chart&&(a=d.plot(g,e,{legend:{show:!1},series:{lines:{show:!1},bars:{show:!0,fill:1,barWidth:.8,horizontal:!1},shadowSize:1},yaxis:{show:!0,min:0,color:"#c8c8c8"},xaxis:{show:!1},grid:{borderWidth:0,borderColor:"#c8c8c8",color:"#c8c8c8",hoverable:!0,clickable:!0},colors:b.colors})),"pie"===f.panel.chart){var c=function(a,b){return"<div ng-click=\"build_search(panel.field,'"+a+'\') "style="font-size:8pt;text-align:center;padding:2px;color:white;">'+a+"<br/>"+Math.round(b.percent)+"%</div>"};a=d.plot(g,e,{legend:{show:!1},series:{pie:{innerRadius:f.panel.donut?.4:0,tilt:f.panel.tilt?.45:1,radius:1,show:!0,combine:{color:"#999",label:"The Rest"},stroke:{width:0},label:{show:f.panel.labels,radius:2/3,formatter:c,threshold:.1}}},grid:{hoverable:!0,clickable:!0,color:"#c8c8c8"},colors:b.colors})}g.is(":visible")&&setTimeout(function(){f.legend=a.getData(),f.$$phase||f.$apply()})}catch(h){g.text(h)}})}f.$on("render",function(){i()}),a.element(window).bind("resize",function(){i()}),g.bind("plotclick",function(a,b,c){c&&f.build_search(f.data[c.seriesIndex])});var j=d("<div>");g.bind("plothover",function(a,b,c){if(c){var d="bar"===f.panel.chart?c.datapoint[1]:c.datapoint[1][0][1];j.html(e.query_color_dot(c.series.color,20)+" "+c.series.label+" ("+d.toFixed(0)+")").place_tt(b.pageX,b.pageY)}else j.remove()})}}}])});