/*! kibana - v3.1.2 - 2015-01-30
 * Copyright (c) 2015 Rashid Khan; Licensed Apache License */

!function(a,b){L.MarkerClusterGroup=L.FeatureGroup.extend({options:{maxClusterRadius:80,iconCreateFunction:null,spiderfyOnMaxZoom:!0,showCoverageOnHover:!0,zoomToBoundsOnClick:!0,singleMarkerMode:!1,disableClusteringAtZoom:null,removeOutsideVisibleBounds:!0,animateAddingMarkers:!1,spiderfyDistanceMultiplier:1,polygonOptions:{}},initialize:function(a){L.Util.setOptions(this,a),this.options.iconCreateFunction||(this.options.iconCreateFunction=this._defaultIconCreateFunction),L.FeatureGroup.prototype.initialize.call(this,[]),this._inZoomAnimation=0,this._needsClustering=[],this._needsRemoving=[],this._currentShownBounds=null},addLayer:function(a){if(a instanceof L.LayerGroup){var b=[];for(var c in a._layers)b.push(a._layers[c]);return this.addLayers(b)}if(!this._map)return this._needsClustering.push(a),this;if(this.hasLayer(a))return this;this._unspiderfy&&this._unspiderfy(),this._addLayer(a,this._maxZoom);var d=a,e=this._map.getZoom();if(a.__parent)for(;d.__parent._zoom>=e;)d=d.__parent;return this._currentShownBounds.contains(d.getLatLng())&&(this.options.animateAddingMarkers?this._animationAddLayer(a,d):this._animationAddLayerNonAnimated(a,d)),this},removeLayer:function(a){return this._map?a.__parent?(this._unspiderfy&&(this._unspiderfy(),this._unspiderfyLayer(a)),this._removeLayer(a,!0),(a._icon||a._container)&&(L.FeatureGroup.prototype.removeLayer.call(this,a),a.setOpacity&&a.setOpacity(1)),this):this:(!this._arraySplice(this._needsClustering,a)&&this.hasLayer(a)&&this._needsRemoving.push(a),this)},addLayers:function(a){var b,c,d;if(!this._map)return this._needsClustering=this._needsClustering.concat(a),this;for(b=0,c=a.length;c>b;b++)if(d=a[b],!this.hasLayer(d)&&(this._addLayer(d,this._maxZoom),d.__parent&&2===d.__parent.getChildCount())){var e=d.__parent.getAllChildMarkers(),f=e[0]===d?e[1]:e[0];L.FeatureGroup.prototype.removeLayer.call(this,f)}for(b in this._layers)d=this._layers[b],d instanceof L.MarkerCluster&&d._iconNeedsUpdate&&d._updateIcon();return this._topClusterLevel._recursivelyAddChildrenToMap(null,this._zoom,this._currentShownBounds),this},removeLayers:function(a){var b,c,d;if(!this._map){for(b=0,c=a.length;c>b;b++)this._arraySplice(this._needsClustering,a[b]);return this}for(b=0,c=a.length;c>b;b++)d=a[b],d.__parent&&(this._removeLayer(d,!0,!0),d._icon&&(L.FeatureGroup.prototype.removeLayer.call(this,d),d.setOpacity(1)));this._topClusterLevel._recursivelyAddChildrenToMap(null,this._zoom,this._currentShownBounds);for(b in this._layers)d=this._layers[b],d instanceof L.MarkerCluster&&d._updateIcon();return this},clearLayers:function(){this._map||(this._needsClustering=[],delete this._gridClusters,delete this._gridUnclustered),this._noanimationUnspiderfy&&this._noanimationUnspiderfy();for(var a in this._layers)L.FeatureGroup.prototype.removeLayer.call(this,this._layers[a]);return this.eachLayer(function(a){delete a.__parent}),this._map&&this._generateInitialClusters(),this},getBounds:function(){var a=new L.LatLngBounds;if(this._topClusterLevel)a.extend(this._topClusterLevel._bounds);else for(var b=this._needsClustering.length-1;b>=0;b--)a.extend(this._needsClustering[b].getLatLng());return a},eachLayer:function(a,b){var c,d=this._needsClustering.slice();for(this._topClusterLevel&&this._topClusterLevel.getAllChildMarkers(d),c=d.length-1;c>=0;c--)a.call(b,d[c])},hasLayer:function(a){if(!a||a._noHas)return!1;var b,c=this._needsClustering;for(b=c.length-1;b>=0;b--)if(c[b]===a)return!0;for(c=this._needsRemoving,b=c.length-1;b>=0;b--)if(c[b]===a)return!1;return!(!a.__parent||a.__parent._group!==this)},zoomToShowLayer:function(a,b){var c=function(){if((a._icon||a.__parent._icon)&&!this._inZoomAnimation)if(this._map.off("moveend",c,this),this.off("animationend",c,this),a._icon)b();else if(a.__parent._icon){var d=function(){this.off("spiderfied",d,this),b()};this.on("spiderfied",d,this),a.__parent.spiderfy()}};a._icon?b():a.__parent._zoom<this._map.getZoom()?(this._map.on("moveend",c,this),a._icon||this._map.panTo(a.getLatLng())):(this._map.on("moveend",c,this),this.on("animationend",c,this),this._map.setView(a.getLatLng(),a.__parent._zoom+1),a.__parent.zoomToBounds())},onAdd:function(a){this._map=a;var b,c,d;for(this._gridClusters||this._generateInitialClusters(),b=0,c=this._needsRemoving.length;c>b;b++)d=this._needsRemoving[b],this._removeLayer(d);for(this._needsRemoving=[],b=0,c=this._needsClustering.length;c>b;b++)d=this._needsClustering[b],d.__parent||this._addLayer(d,this._maxZoom);this._needsClustering=[],this._map.on("zoomend",this._zoomEnd,this),this._map.on("moveend",this._moveEnd,this),this._spiderfierOnAdd&&this._spiderfierOnAdd(),this._bindEvents(),this._zoom=this._map.getZoom(),this._currentShownBounds=this._getExpandedVisibleBounds(),this._topClusterLevel._recursivelyAddChildrenToMap(null,this._zoom,this._currentShownBounds)},onRemove:function(a){a.off("zoomend",this._zoomEnd,this),a.off("moveend",this._moveEnd,this),this._unbindEvents(),this._map._mapPane.className=this._map._mapPane.className.replace(" leaflet-cluster-anim",""),this._spiderfierOnRemove&&this._spiderfierOnRemove();for(var b in this._layers)L.FeatureGroup.prototype.removeLayer.call(this,this._layers[b]);this._map=null},_arraySplice:function(a,b){for(var c=a.length-1;c>=0;c--)if(a[c]===b)return a.splice(c,1),!0},_removeLayer:function(a,b,c){var d=this._gridClusters,e=this._gridUnclustered,f=this._map;if(b)for(var g=this._maxZoom;g>=0&&e[g].removeObject(a,f.project(a.getLatLng(),g));g--);var h,i=a.__parent,j=i._markers;for(this._arraySplice(j,a);i&&(i._childCount--,!(i._zoom<0));)b&&i._childCount<=1?(h=i._markers[0]===a?i._markers[1]:i._markers[0],d[i._zoom].removeObject(i,f.project(i._cLatLng,i._zoom)),e[i._zoom].addObject(h,f.project(h.getLatLng(),i._zoom)),this._arraySplice(i.__parent._childClusters,i),i.__parent._markers.push(h),h.__parent=i.__parent,i._icon&&(L.FeatureGroup.prototype.removeLayer.call(this,i),c||(h._noHas=!0,L.FeatureGroup.prototype.addLayer.call(this,h),delete h._noHas))):(i._recalculateBounds(),c&&i._icon||i._updateIcon()),i=i.__parent;delete a.__parent},_propagateEvent:function(a){a.target instanceof L.MarkerCluster&&(a.type="cluster"+a.type),L.FeatureGroup.prototype._propagateEvent.call(this,a)},_defaultIconCreateFunction:function(a){var b=a.getChildCount(),c=" marker-cluster-";return c+=10>b?"small":100>b?"medium":"large",new L.DivIcon({html:"<div><span>"+b+"</span></div>",className:"marker-cluster"+c,iconSize:new L.Point(40,40)})},_bindEvents:function(){var a=null,b=this._map,c=this.options.spiderfyOnMaxZoom,d=this.options.showCoverageOnHover,e=this.options.zoomToBoundsOnClick;(c||e)&&this.on("clusterclick",function(a){b.getMaxZoom()===b.getZoom()?c&&a.layer.spiderfy():e&&a.layer.zoomToBounds()},this),d&&(this.on("clustermouseover",function(c){this._inZoomAnimation||(a&&b.removeLayer(a),c.layer.getChildCount()>2&&c.layer!==this._spiderfied&&(a=new L.Polygon(c.layer.getConvexHull(),this.options.polygonOptions),b.addLayer(a)))},this),this.on("clustermouseout",function(){a&&(b.removeLayer(a),a=null)},this),b.on("zoomend",function(){a&&(b.removeLayer(a),a=null)},this),b.on("layerremove",function(c){a&&c.layer===this&&(b.removeLayer(a),a=null)},this))},_unbindEvents:function(){var a=this.options.spiderfyOnMaxZoom,b=this.options.showCoverageOnHover,c=this.options.zoomToBoundsOnClick,d=this._map;(a||c)&&this.off("clusterclick",null,this),b&&(this.off("clustermouseover",null,this),this.off("clustermouseout",null,this),d.off("zoomend",null,this),d.off("layerremove",null,this))},_zoomEnd:function(){this._map&&(this._mergeSplitClusters(),this._zoom=this._map._zoom,this._currentShownBounds=this._getExpandedVisibleBounds())},_moveEnd:function(){if(!this._inZoomAnimation){var a=this._getExpandedVisibleBounds();this._topClusterLevel._recursivelyRemoveChildrenFromMap(this._currentShownBounds,this._zoom,a),this._topClusterLevel._recursivelyAddChildrenToMap(null,this._zoom,a),this._currentShownBounds=a}},_generateInitialClusters:function(){var a=this._map.getMaxZoom(),b=this.options.maxClusterRadius;this.options.disableClusteringAtZoom&&(a=this.options.disableClusteringAtZoom-1),this._maxZoom=a,this._gridClusters={},this._gridUnclustered={};for(var c=a;c>=0;c--)this._gridClusters[c]=new L.DistanceGrid(b),this._gridUnclustered[c]=new L.DistanceGrid(b);this._topClusterLevel=new L.MarkerCluster(this,-1)},_addLayer:function(a,b){var c,d,e=this._gridClusters,f=this._gridUnclustered;for(this.options.singleMarkerMode&&(a.options.icon=this.options.iconCreateFunction({getChildCount:function(){return 1},getAllChildMarkers:function(){return[a]}}));b>=0;b--){c=this._map.project(a.getLatLng(),b);var g=e[b].getNearObject(c);if(g)return g._addChild(a),void(a.__parent=g);if(g=f[b].getNearObject(c)){var h=g.__parent;h&&this._removeLayer(g,!1);var i=new L.MarkerCluster(this,b,g,a);e[b].addObject(i,this._map.project(i._cLatLng,b)),g.__parent=i,a.__parent=i;var j=i;for(d=b-1;d>h._zoom;d--)j=new L.MarkerCluster(this,d,j),e[d].addObject(j,this._map.project(g.getLatLng(),d));for(h._addChild(j),d=b;d>=0&&f[d].removeObject(g,this._map.project(g.getLatLng(),d));d--);return}f[b].addObject(a,c)}this._topClusterLevel._addChild(a),a.__parent=this._topClusterLevel},_mergeSplitClusters:function(){this._zoom<this._map._zoom?(this._animationStart(),this._topClusterLevel._recursivelyRemoveChildrenFromMap(this._currentShownBounds,this._zoom,this._getExpandedVisibleBounds()),this._animationZoomIn(this._zoom,this._map._zoom)):this._zoom>this._map._zoom?(this._animationStart(),this._animationZoomOut(this._zoom,this._map._zoom)):this._moveEnd()},_getExpandedVisibleBounds:function(){if(!this.options.removeOutsideVisibleBounds)return this.getBounds();var a=this._map,b=a.getBounds(),c=b._southWest,d=b._northEast,e=L.Browser.mobile?0:Math.abs(c.lat-d.lat),f=L.Browser.mobile?0:Math.abs(c.lng-d.lng);return new L.LatLngBounds(new L.LatLng(c.lat-e,c.lng-f,!0),new L.LatLng(d.lat+e,d.lng+f,!0))},_animationAddLayerNonAnimated:function(a,b){if(b===a)a._noHas=!0,L.FeatureGroup.prototype.addLayer.call(this,a),delete a._noHas;else if(2===b._childCount){b._addToMap();var c=b.getAllChildMarkers();L.FeatureGroup.prototype.removeLayer.call(this,c[0]),L.FeatureGroup.prototype.removeLayer.call(this,c[1])}else b._updateIcon()}}),L.MarkerClusterGroup.include(L.DomUtil.TRANSITION?{_animationStart:function(){this._map._mapPane.className+=" leaflet-cluster-anim",this._inZoomAnimation++},_animationEnd:function(){this._map&&(this._map._mapPane.className=this._map._mapPane.className.replace(" leaflet-cluster-anim","")),this._inZoomAnimation--,this.fire("animationend")},_animationZoomIn:function(a,b){var c,d=this,e=this._getExpandedVisibleBounds();this._topClusterLevel._recursively(e,a,0,function(f){var g,h=f._latlng,i=f._markers;for(f._isSingleParent()&&a+1===b?(L.FeatureGroup.prototype.removeLayer.call(d,f),f._recursivelyAddChildrenToMap(null,b,e)):(f.setOpacity(0),f._recursivelyAddChildrenToMap(h,b,e)),c=i.length-1;c>=0;c--)g=i[c],e.contains(g._latlng)||L.FeatureGroup.prototype.removeLayer.call(d,g)}),this._forceLayout();var f,g;d._topClusterLevel._recursivelyBecomeVisible(e,b);for(f in d._layers)g=d._layers[f],g instanceof L.MarkerCluster||!g._icon||g.setOpacity(1);d._topClusterLevel._recursively(e,a,b,function(a){a._recursivelyRestoreChildPositions(b)}),setTimeout(function(){d._topClusterLevel._recursively(e,a,0,function(a){L.FeatureGroup.prototype.removeLayer.call(d,a),a.setOpacity(1)}),d._animationEnd()},200)},_animationZoomOut:function(a,b){this._animationZoomOutSingle(this._topClusterLevel,a-1,b),this._topClusterLevel._recursivelyAddChildrenToMap(null,b,this._getExpandedVisibleBounds()),this._topClusterLevel._recursivelyRemoveChildrenFromMap(this._currentShownBounds,a,this._getExpandedVisibleBounds())},_animationZoomOutSingle:function(a,b,c){var d=this._getExpandedVisibleBounds();a._recursivelyAnimateChildrenInAndAddSelfToMap(d,b+1,c);var e=this;this._forceLayout(),a._recursivelyBecomeVisible(d,c),setTimeout(function(){if(1===a._childCount){var f=a._markers[0];f.setLatLng(f.getLatLng()),f.setOpacity(1)}else a._recursively(d,c,0,function(a){a._recursivelyRemoveChildrenFromMap(d,b+1)});e._animationEnd()},200)},_animationAddLayer:function(a,b){var c=this;a._noHas=!0,L.FeatureGroup.prototype.addLayer.call(this,a),delete a._noHas,b!==a&&(b._childCount>2?(b._updateIcon(),this._forceLayout(),this._animationStart(),a._setPos(this._map.latLngToLayerPoint(b.getLatLng())),a.setOpacity(0),setTimeout(function(){L.FeatureGroup.prototype.removeLayer.call(c,a),a.setOpacity(1),c._animationEnd()},200)):(this._forceLayout(),c._animationStart(),c._animationZoomOutSingle(b,this._map.getMaxZoom(),this._map.getZoom())))},_forceLayout:function(){L.Util.falseFn(b.body.offsetWidth)}}:{_animationStart:function(){},_animationZoomIn:function(a,b){this._topClusterLevel._recursivelyRemoveChildrenFromMap(this._currentShownBounds,a),this._topClusterLevel._recursivelyAddChildrenToMap(null,b,this._getExpandedVisibleBounds())},_animationZoomOut:function(a,b){this._topClusterLevel._recursivelyRemoveChildrenFromMap(this._currentShownBounds,a),this._topClusterLevel._recursivelyAddChildrenToMap(null,b,this._getExpandedVisibleBounds())},_animationAddLayer:function(a,b){this._animationAddLayerNonAnimated(a,b)}}),L.markerClusterGroup=function(a){return new L.MarkerClusterGroup(a)},L.MarkerCluster=L.Marker.extend({initialize:function(a,b,c,d){L.Marker.prototype.initialize.call(this,c?c._cLatLng||c.getLatLng():new L.LatLng(0,0),{icon:this}),this._group=a,this._zoom=b,this._markers=[],this._childClusters=[],this._childCount=0,this._iconNeedsUpdate=!0,this._bounds=new L.LatLngBounds,c&&this._addChild(c),d&&this._addChild(d)},getAllChildMarkers:function(a){a=a||[];for(var b=this._childClusters.length-1;b>=0;b--)this._childClusters[b].getAllChildMarkers(a);for(var c=this._markers.length-1;c>=0;c--)a.push(this._markers[c]);return a},getChildCount:function(){return this._childCount},zoomToBounds:function(){this._group._map.fitBounds(this._bounds)},getBounds:function(){var a=new L.LatLngBounds;return a.extend(this._bounds),a},_updateIcon:function(){this._iconNeedsUpdate=!0,this._icon&&this.setIcon(this)},createIcon:function(){return this._iconNeedsUpdate&&(this._iconObj=this._group.options.iconCreateFunction(this),this._iconNeedsUpdate=!1),this._iconObj.createIcon()},createShadow:function(){return this._iconObj.createShadow()},_addChild:function(a,b){this._iconNeedsUpdate=!0,this._expandBounds(a),a instanceof L.MarkerCluster?(b||(this._childClusters.push(a),a.__parent=this),this._childCount+=a._childCount):(b||this._markers.push(a),this._childCount++),this.__parent&&this.__parent._addChild(a,!0)},_expandBounds:function(a){var b,c=a._wLatLng||a._latlng;a instanceof L.MarkerCluster?(this._bounds.extend(a._bounds),b=a._childCount):(this._bounds.extend(c),b=1),this._cLatLng||(this._cLatLng=a._cLatLng||c);var d=this._childCount+b;this._wLatLng?(this._wLatLng.lat=(c.lat*b+this._wLatLng.lat*this._childCount)/d,this._wLatLng.lng=(c.lng*b+this._wLatLng.lng*this._childCount)/d):this._latlng=this._wLatLng=new L.LatLng(c.lat,c.lng)},_addToMap:function(a){a&&(this._backupLatlng=this._latlng,this.setLatLng(a)),this._noHas=!0,L.FeatureGroup.prototype.addLayer.call(this._group,this),delete this._noHas},_recursivelyAnimateChildrenIn:function(a,b,c){this._recursively(a,0,c-1,function(a){var c,d,e=a._markers;for(c=e.length-1;c>=0;c--)d=e[c],d._icon&&(d._setPos(b),d.setOpacity(0))},function(a){var c,d,e=a._childClusters;for(c=e.length-1;c>=0;c--)d=e[c],d._icon&&(d._setPos(b),d.setOpacity(0))})},_recursivelyAnimateChildrenInAndAddSelfToMap:function(a,b,c){this._recursively(a,c,0,function(d){d._recursivelyAnimateChildrenIn(a,d._group._map.latLngToLayerPoint(d.getLatLng()).round(),b),d._isSingleParent()&&b-1===c?(d.setOpacity(1),d._recursivelyRemoveChildrenFromMap(a,b)):d.setOpacity(0),d._addToMap()})},_recursivelyBecomeVisible:function(a,b){this._recursively(a,0,b,null,function(a){a.setOpacity(1)})},_recursivelyAddChildrenToMap:function(a,b,c){this._recursively(c,-1,b,function(d){if(b!==d._zoom)for(var e=d._markers.length-1;e>=0;e--){var f=d._markers[e];c.contains(f._latlng)&&(a&&(f._backupLatlng=f.getLatLng(),f.setLatLng(a),f.setOpacity&&f.setOpacity(0)),f._noHas=!0,L.FeatureGroup.prototype.addLayer.call(d._group,f),delete f._noHas)}},function(b){b._addToMap(a)})},_recursivelyRestoreChildPositions:function(a){for(var b=this._markers.length-1;b>=0;b--){var c=this._markers[b];c._backupLatlng&&(c.setLatLng(c._backupLatlng),delete c._backupLatlng)}if(a-1===this._zoom)for(var d=this._childClusters.length-1;d>=0;d--)this._childClusters[d]._restorePosition();else for(var e=this._childClusters.length-1;e>=0;e--)this._childClusters[e]._recursivelyRestoreChildPositions(a)},_restorePosition:function(){this._backupLatlng&&(this.setLatLng(this._backupLatlng),delete this._backupLatlng)},_recursivelyRemoveChildrenFromMap:function(a,b,c){var d,e;this._recursively(a,-1,b-1,function(a){for(e=a._markers.length-1;e>=0;e--)d=a._markers[e],c&&c.contains(d._latlng)||(L.FeatureGroup.prototype.removeLayer.call(a._group,d),d.setOpacity&&d.setOpacity(1))},function(a){for(e=a._childClusters.length-1;e>=0;e--)d=a._childClusters[e],c&&c.contains(d._latlng)||((!L.FeatureGroup.prototype.hasLayer||L.FeatureGroup.prototype.hasLayer.call(a._group,d))&&L.FeatureGroup.prototype.removeLayer.call(a._group,d),d.setOpacity&&d.setOpacity(1))})},_recursively:function(a,b,c,d,e){var f,g,h=this._childClusters,i=this._zoom;if(b>i)for(f=h.length-1;f>=0;f--)g=h[f],a.intersects(g._bounds)&&g._recursively(a,b,c,d,e);else if(d&&d(this),e&&this._zoom===c&&e(this),c>i)for(f=h.length-1;f>=0;f--)g=h[f],a.intersects(g._bounds)&&g._recursively(a,b,c,d,e)},_recalculateBounds:function(){var a,b=this._markers,c=this._childClusters;for(this._bounds=new L.LatLngBounds,delete this._wLatLng,a=b.length-1;a>=0;a--)this._expandBounds(b[a]);for(a=c.length-1;a>=0;a--)this._expandBounds(c[a])},_isSingleParent:function(){return this._childClusters.length>0&&this._childClusters[0]._childCount===this._childCount}}),L.DistanceGrid=function(a){this._cellSize=a,this._sqCellSize=a*a,this._grid={},this._objectPoint={}},L.DistanceGrid.prototype={addObject:function(a,b){var c=this._getCoord(b.x),d=this._getCoord(b.y),e=this._grid,f=e[d]=e[d]||{},g=f[c]=f[c]||[],h=L.Util.stamp(a);this._objectPoint[h]=b,g.push(a)},updateObject:function(a,b){this.removeObject(a),this.addObject(a,b)},removeObject:function(a,b){var c,d,e=this._getCoord(b.x),f=this._getCoord(b.y),g=this._grid,h=g[f]=g[f]||{},i=h[e]=h[e]||[];for(delete this._objectPoint[L.Util.stamp(a)],c=0,d=i.length;d>c;c++)if(i[c]===a)return i.splice(c,1),1===d&&delete h[e],!0},eachObject:function(a,b){var c,d,e,f,g,h,i,j=this._grid;for(c in j){g=j[c];for(d in g)for(h=g[d],e=0,f=h.length;f>e;e++)i=a.call(b,h[e]),i&&(e--,f--)}},getNearObject:function(a){var b,c,d,e,f,g,h,i,j=this._getCoord(a.x),k=this._getCoord(a.y),l=this._objectPoint,m=this._sqCellSize,n=null;for(b=k-1;k+1>=b;b++)if(e=this._grid[b])for(c=j-1;j+1>=c;c++)if(f=e[c])for(d=0,g=f.length;g>d;d++)h=f[d],i=this._sqDist(l[L.Util.stamp(h)],a),m>i&&(m=i,n=h);return n},_getCoord:function(a){return Math.floor(a/this._cellSize)},_sqDist:function(a,b){var c=b.x-a.x,d=b.y-a.y;return c*c+d*d}},function(){L.QuickHull={getDistant:function(a,b){var c=b[1].lat-b[0].lat,d=b[0].lng-b[1].lng;return d*(a.lat-b[0].lat)+c*(a.lng-b[0].lng)},findMostDistantPointFromBaseLine:function(a,b){var c,d,e,f=0,g=null,h=[];for(c=b.length-1;c>=0;c--)d=b[c],e=this.getDistant(d,a),e>0&&(h.push(d),e>f&&(f=e,g=d));return{maxPoint:g,newPoints:h}},buildConvexHull:function(a,b){var c=[],d=this.findMostDistantPointFromBaseLine(a,b);return d.maxPoint?(c=c.concat(this.buildConvexHull([a[0],d.maxPoint],d.newPoints)),c=c.concat(this.buildConvexHull([d.maxPoint,a[1]],d.newPoints))):[a]},getConvexHull:function(a){var b,c=!1,d=!1,e=null,f=null;for(b=a.length-1;b>=0;b--){var g=a[b];(c===!1||g.lat>c)&&(e=g,c=g.lat),(d===!1||g.lat<d)&&(f=g,d=g.lat)}var h=[].concat(this.buildConvexHull([f,e],a),this.buildConvexHull([e,f],a));return h}}}(),L.MarkerCluster.include({getConvexHull:function(){var a,b,c,d=this.getAllChildMarkers(),e=[],f=[];for(c=d.length-1;c>=0;c--)b=d[c].getLatLng(),e.push(b);for(a=L.QuickHull.getConvexHull(e),c=a.length-1;c>=0;c--)f.push(a[c][0]);return f}}),L.MarkerCluster.include({_2PI:2*Math.PI,_circleFootSeparation:25,_circleStartAngle:Math.PI/6,_spiralFootSeparation:28,_spiralLengthStart:11,_spiralLengthFactor:5,_circleSpiralSwitchover:9,spiderfy:function(){if(this._group._spiderfied!==this&&!this._group._inZoomAnimation){var a,b=this.getAllChildMarkers(),c=this._group,d=c._map,e=d.latLngToLayerPoint(this._latlng);this._group._unspiderfy(),this._group._spiderfied=this,b.length>=this._circleSpiralSwitchover?a=this._generatePointsSpiral(b.length,e):(e.y+=10,a=this._generatePointsCircle(b.length,e)),this._animationSpiderfy(b,a)}},unspiderfy:function(a){this._group._inZoomAnimation||(this._animationUnspiderfy(a),this._group._spiderfied=null)},_generatePointsCircle:function(a,b){var c,d,e=this._group.options.spiderfyDistanceMultiplier*this._circleFootSeparation*(2+a),f=e/this._2PI,g=this._2PI/a,h=[];for(h.length=a,c=a-1;c>=0;c--)d=this._circleStartAngle+c*g,h[c]=new L.Point(b.x+f*Math.cos(d),b.y+f*Math.sin(d))._round();return h},_generatePointsSpiral:function(a,b){var c,d=this._group.options.spiderfyDistanceMultiplier*this._spiralLengthStart,e=this._group.options.spiderfyDistanceMultiplier*this._spiralFootSeparation,f=this._group.options.spiderfyDistanceMultiplier*this._spiralLengthFactor,g=0,h=[];for(h.length=a,c=a-1;c>=0;c--)g+=e/d+5e-4*c,h[c]=new L.Point(b.x+d*Math.cos(g),b.y+d*Math.sin(g))._round(),d+=this._2PI*f/g;return h},_noanimationUnspiderfy:function(){var a,b,c=this._group,d=c._map,e=this.getAllChildMarkers();for(this.setOpacity(1),b=e.length-1;b>=0;b--)a=e[b],L.FeatureGroup.prototype.removeLayer.call(c,a),a._preSpiderfyLatlng&&(a.setLatLng(a._preSpiderfyLatlng),delete a._preSpiderfyLatlng),a.setZIndexOffset(0),a._spiderLeg&&(d.removeLayer(a._spiderLeg),delete a._spiderLeg)}}),L.MarkerCluster.include(L.DomUtil.TRANSITION?{SVG_ANIMATION:function(){return b.createElementNS("http://www.w3.org/2000/svg","animate").toString().indexOf("SVGAnimate")>-1}(),_animationSpiderfy:function(a,c){var d,e,f,g,h=this,i=this._group,j=i._map,k=j.latLngToLayerPoint(this._latlng);for(d=a.length-1;d>=0;d--)e=a[d],e.setZIndexOffset(1e6),e.setOpacity(0),e._noHas=!0,L.FeatureGroup.prototype.addLayer.call(i,e),delete e._noHas,e._setPos(k);i._forceLayout(),i._animationStart();var l=L.Path.SVG?0:.3,m=L.Path.SVG_NS;for(d=a.length-1;d>=0;d--)if(g=j.layerPointToLatLng(c[d]),e=a[d],e._preSpiderfyLatlng=e._latlng,e.setLatLng(g),e.setOpacity(1),f=new L.Polyline([h._latlng,g],{weight:1.5,color:"#222",opacity:l}),j.addLayer(f),e._spiderLeg=f,L.Path.SVG&&this.SVG_ANIMATION){var n=f._path.getTotalLength();f._path.setAttribute("stroke-dasharray",n+","+n);var o=b.createElementNS(m,"animate");o.setAttribute("attributeName","stroke-dashoffset"),o.setAttribute("begin","indefinite"),o.setAttribute("from",n),o.setAttribute("to",0),o.setAttribute("dur",.25),f._path.appendChild(o),o.beginElement(),o=b.createElementNS(m,"animate"),o.setAttribute("attributeName","stroke-opacity"),o.setAttribute("attributeName","stroke-opacity"),o.setAttribute("begin","indefinite"),o.setAttribute("from",0),o.setAttribute("to",.5),o.setAttribute("dur",.25),f._path.appendChild(o),o.beginElement()}if(h.setOpacity(.3),L.Path.SVG)for(this._group._forceLayout(),d=a.length-1;d>=0;d--)e=a[d]._spiderLeg,e.options.opacity=.5,e._path.setAttribute("stroke-opacity",.5);setTimeout(function(){i._animationEnd(),i.fire("spiderfied")},200)},_animationUnspiderfy:function(a){var b,c,d,e=this._group,f=e._map,g=a?f._latLngToNewLayerPoint(this._latlng,a.zoom,a.center):f.latLngToLayerPoint(this._latlng),h=this.getAllChildMarkers(),i=L.Path.SVG&&this.SVG_ANIMATION;for(e._animationStart(),this.setOpacity(1),c=h.length-1;c>=0;c--)b=h[c],b._preSpiderfyLatlng&&(b.setLatLng(b._preSpiderfyLatlng),delete b._preSpiderfyLatlng,b._setPos(g),b.setOpacity(0),i&&(d=b._spiderLeg._path.childNodes[0],d.setAttribute("to",d.getAttribute("from")),d.setAttribute("from",0),d.beginElement(),d=b._spiderLeg._path.childNodes[1],d.setAttribute("from",.5),d.setAttribute("to",0),d.setAttribute("stroke-opacity",0),d.beginElement(),b._spiderLeg._path.setAttribute("stroke-opacity",0)));setTimeout(function(){var a=0;for(c=h.length-1;c>=0;c--)b=h[c],b._spiderLeg&&a++;for(c=h.length-1;c>=0;c--)b=h[c],b._spiderLeg&&(b.setOpacity(1),b.setZIndexOffset(0),a>1&&L.FeatureGroup.prototype.removeLayer.call(e,b),f.removeLayer(b._spiderLeg),delete b._spiderLeg);e._animationEnd()},200)}}:{_animationSpiderfy:function(a,b){var c,d,e,f,g=this._group,h=g._map;for(c=a.length-1;c>=0;c--)f=h.layerPointToLatLng(b[c]),d=a[c],d._preSpiderfyLatlng=d._latlng,d.setLatLng(f),d.setZIndexOffset(1e6),L.FeatureGroup.prototype.addLayer.call(g,d),e=new L.Polyline([this._latlng,f],{weight:1.5,color:"#222"}),h.addLayer(e),d._spiderLeg=e;this.setOpacity(.3),g.fire("spiderfied")},_animationUnspiderfy:function(){this._noanimationUnspiderfy()}}),L.MarkerClusterGroup.include({_spiderfied:null,_spiderfierOnAdd:function(){this._map.on("click",this._unspiderfyWrapper,this),this._map.options.zoomAnimation?this._map.on("zoomstart",this._unspiderfyZoomStart,this):this._map.on("zoomend",this._unspiderfyWrapper,this),L.Path.SVG&&!L.Browser.touch&&this._map._initPathRoot()},_spiderfierOnRemove:function(){this._map.off("click",this._unspiderfyWrapper,this),this._map.off("zoomstart",this._unspiderfyZoomStart,this),this._map.off("zoomanim",this._unspiderfyZoomAnim,this),this._unspiderfy()},_unspiderfyZoomStart:function(){this._map&&this._map.on("zoomanim",this._unspiderfyZoomAnim,this)},_unspiderfyZoomAnim:function(a){L.DomUtil.hasClass(this._map._mapPane,"leaflet-touching")||(this._map.off("zoomanim",this._unspiderfyZoomAnim,this),this._unspiderfy(a))},_unspiderfyWrapper:function(){this._unspiderfy()},_unspiderfy:function(a){this._spiderfied&&this._spiderfied.unspiderfy(a)},_noanimationUnspiderfy:function(){this._spiderfied&&this._spiderfied._noanimationUnspiderfy()},_unspiderfyLayer:function(a){a._spiderLeg&&(L.FeatureGroup.prototype.removeLayer.call(this,a),a.setOpacity(1),a.setZIndexOffset(0),this._map.removeLayer(a._spiderLeg),delete a._spiderLeg)}})}(window,document),function(){L.labelVersion="0.1.4-dev",L.Label=L.Popup.extend({options:{autoPan:!1,className:"",closePopupOnClick:!1,noHide:!1,offset:new L.Point(12,-15),opacity:1},onAdd:function(a){this._map=a,this._pane=this._source instanceof L.Marker?a._panes.markerPane:a._panes.popupPane,this._container||this._initLayout(),this._updateContent();var b=a.options.fadeAnimation;b&&L.DomUtil.setOpacity(this._container,0),this._pane.appendChild(this._container),a.on("viewreset",this._updatePosition,this),this._animated&&a.on("zoomanim",this._zoomAnimation,this),L.Browser.touch&&!this.options.noHide&&L.DomEvent.on(this._container,"click",this.close,this),this._update(),this.setOpacity(this.options.opacity)},onRemove:function(a){this._pane.removeChild(this._container),L.Util.falseFn(this._container.offsetWidth),a.off({viewreset:this._updatePosition,zoomanim:this._zoomAnimation},this),a.options.fadeAnimation&&L.DomUtil.setOpacity(this._container,0),this._map=null},close:function(){var a=this._map;a&&(L.Browser.touch&&!this.options.noHide&&L.DomEvent.off(this._container,"click",this.close),a._label=null,a.removeLayer(this))},updateZIndex:function(a){this._zIndex=a,this._container&&this._zIndex&&(this._container.style.zIndex=a)},setOpacity:function(a){this.options.opacity=a,this._container&&L.DomUtil.setOpacity(this._container,a)},_initLayout:function(){this._container=L.DomUtil.create("div","leaflet-label "+this.options.className+" leaflet-zoom-animated"),this.updateZIndex(this._zIndex)},_updateContent:function(){this._content&&"string"==typeof this._content&&(this._container.innerHTML=this._content)},_updateLayout:function(){},_updatePosition:function(){var a=this._map.latLngToLayerPoint(this._latlng);this._setPosition(a)},_setPosition:function(a){a=a.add(this.options.offset),L.DomUtil.setPosition(this._container,a)},_zoomAnimation:function(a){var b=this._map._latLngToNewLayerPoint(this._latlng,a.zoom,a.center);this._setPosition(b)}}),L.Icon.Default.mergeOptions({labelAnchor:new L.Point(9,-20)}),L.Marker.mergeOptions({icon:new L.Icon.Default}),L.Marker.include({showLabel:function(){return this._label&&this._map&&(this._label.setLatLng(this._latlng),this._map.showLabel(this._label)),this},hideLabel:function(){return this._label&&this._label.close(),this},setLabelNoHide:function(a){this._labelNoHide!==a&&(this._labelNoHide=a,a?(this._removeLabelRevealHandlers(),this.showLabel()):(this._addLabelRevealHandlers(),this.hideLabel()))},bindLabel:function(a,b){var c=L.point(this.options.icon.options.labelAnchor)||new L.Point(0,0);return c=c.add(L.Label.prototype.options.offset),b&&b.offset&&(c=c.add(b.offset)),b=L.Util.extend({offset:c},b),this._labelNoHide=b.noHide,this._label||(this._labelNoHide||this._addLabelRevealHandlers(),this.on("remove",this.hideLabel,this).on("move",this._moveLabel,this),this._hasLabelHandlers=!0),this._label=new L.Label(b,this).setContent(a),this},unbindLabel:function(){return this._label&&(this.hideLabel(),this._label=null,this._hasLabelHandlers&&(this._labelNoHide||this._removeLabelRevealHandlers(),this.off("remove",this.hideLabel,this).off("move",this._moveLabel,this)),this._hasLabelHandlers=!1),this},updateLabelContent:function(a){this._label&&this._label.setContent(a)},_addLabelRevealHandlers:function(){this.on("mouseover",this.showLabel,this).on("mouseout",this.hideLabel,this),L.Browser.touch&&this.on("click",this.showLabel,this)},_removeLabelRevealHandlers:function(){this.off("mouseover",this.showLabel,this).off("mouseout",this.hideLabel,this).off("remove",this.hideLabel,this).off("move",this._moveLabel,this),L.Browser.touch&&this.off("click",this.showLabel,this)},_moveLabel:function(a){this._label.setLatLng(a.latlng)},_originalUpdateZIndex:L.Marker.prototype._updateZIndex,_updateZIndex:function(a){var b=this._zIndex+a;this._originalUpdateZIndex(a),this._label&&this._label.updateZIndex(b)},_originalSetOpacity:L.Marker.prototype.setOpacity,setOpacity:function(a,b){this.options.labelHasSemiTransparency=b,this._originalSetOpacity(a)},_originalUpdateOpacity:L.Marker.prototype._updateOpacity,_updateOpacity:function(){var a=0===this.options.opacity?0:1;this._originalUpdateOpacity(),this._label&&this._label.setOpacity(this.options.labelHasSemiTransparency?this.options.opacity:a)}}),L.Path.include({bindLabel:function(a,b){return this._label&&this._label.options===b||(this._label=new L.Label(b,this)),this._label.setContent(a),this._showLabelAdded||(this.on("mouseover",this._showLabel,this).on("mousemove",this._moveLabel,this).on("mouseout remove",this._hideLabel,this),L.Browser.touch&&this.on("click",this._showLabel,this),this._showLabelAdded=!0),this},unbindLabel:function(){return this._label&&(this._hideLabel(),this._label=null,this._showLabelAdded=!1,this.off("mouseover",this._showLabel,this).off("mousemove",this._moveLabel,this).off("mouseout remove",this._hideLabel,this)),this},updateLabelContent:function(a){this._label&&this._label.setContent(a)},_showLabel:function(a){this._label.setLatLng(a.latlng),this._map.showLabel(this._label)},_moveLabel:function(a){this._label.setLatLng(a.latlng)},_hideLabel:function(){this._label.close()}}),L.Map.include({showLabel:function(a){return this._label=a,this.addLayer(a)}}),L.FeatureGroup.include({clearLayers:function(){return this.unbindLabel(),this.eachLayer(this.removeLayer,this),this},bindLabel:function(a,b){return this.invoke("bindLabel",a,b)},unbindLabel:function(){return this.invoke("unbindLabel")},updateLabelContent:function(a){this.invoke("updateLabelContent",a)}})}(this,document);