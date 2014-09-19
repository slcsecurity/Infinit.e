<!--
Copyright 2012 The Infinit.e Open Source Project

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"%>
<%@ include file="inc/sharedFunctions.jsp" %>

<%!
	// 
	String action = "";
	String logoutAction = "";

	//
	String templateShareId = "";
	String sourceUpdateTemplateButton = "style=\"display: none\";";

	String shareid = "";
	String sourceJson = "";
	String communityId = "";
	String shareTitle = "";
	String shareTags = "";
	String shareDescription = "";
	String shareType = "";
	
	// !----------  ----------!
	String sourceTemplateSelect = "";
	String selectedSourceSample = "";
	String selectedSourceTemplate = "";
	String communityIdSelect = "";	
%>

<%
	messageToDisplay = "";
	
	// 
	if (isLoggedIn) 
	{
		// Determine which action to perform on postback/request
		action = "";
		if (request.getParameter("action") != null) action = request.getParameter("action").toLowerCase();
		if (request.getParameter("dispatchAction") != null) action = request.getParameter("dispatchAction").toLowerCase();
		if (request.getParameter("logoutButton") != null) action = request.getParameter("logoutButton").toLowerCase();
		if (request.getParameter("selectTemplate") != null) action = request.getParameter("selectTemplate");
		if (request.getParameter("saveSource") != null) action = "saveSource";
		if (request.getParameter("updateTemplate") != null) action = "updateTemplate";
		
		try
		{

			// Read in values from the edit form
			templateShareId = (request.getParameter("templateShareId") != null) ? request.getParameter("templateShareId") : "";
			if (templateShareId.length() > 0) {
				sourceUpdateTemplateButton = "";
			}
			else {
				sourceUpdateTemplateButton = "style=\"display: none\";";
			}
			
			shareid = (request.getParameter("shareid") != null) ? request.getParameter("shareid") : "";
			communityId = (request.getParameter("Community_ID") != null) ? request.getParameter("Community_ID") : "";
			shareTitle = "Title";
			shareDescription = "Description";
			shareTitle = (request.getParameter("shareTitle") != null) ? request.getParameter("shareTitle") : "";
			shareTitle = org.apache.commons.lang.StringEscapeUtils.unescapeHtml(shareTitle);
			shareTags = (request.getParameter("shareTags") != null) ? request.getParameter("shareTags") : "";
			shareTags = org.apache.commons.lang.StringEscapeUtils.unescapeHtml(shareTags);
			shareDescription = (request.getParameter("shareDescription") != null) ? request.getParameter("shareDescription") : "";
			sourceJson = (request.getParameter("Source_JSON") != null) ? request.getParameter("Source_JSON") : "";
			selectedSourceTemplate = (request.getParameter("sourceTemplateSelect") != null) ? request.getParameter("sourceTemplateSelect") : "";
			selectedSourceSample = (request.getParameter("sourceTemplateSample") != null) ? request.getParameter("sourceTemplateSample") : "";
			
			if (action.equals("selectTemplate")) 
			{
				templateShareId = "";
				sourceUpdateTemplateButton = "style=\"display: none\";";

				if (!selectedSourceSample.isEmpty()) {
					JSONObject sourceJsonObj = new JSONObject(selectedSourceSample);
					sourceJson= sourceJsonObj.toString(4);
					updateFieldsFromSample(sourceJsonObj);
				}
				else {
					sourceJson = getSourceJSONObjectFromShare(selectedSourceTemplate, request, response).toString(4);
					sourceUpdateTemplateButton = "";
					templateShareId = selectedSourceTemplate;
				}
			}
			else if (action.equals("updateTemplate")) {
				updateTemplate(request, response);
			}
			else if (action.equals("saveSource")) 
			{
				String newId = saveShare(request, response);
				if ( newId.length() > 0 )
				{	
					out.println("<meta http-equiv=\"refresh\" content=\"0;url=sources.jsp?action=edit&shareid="+ newId + "\">");
				}
			}
			else if (action.equals("logout")) 
			{
				logOut(request, response);
				out.println("<meta http-equiv=\"refresh\" content=\"0;url=index.jsp\">");
			}
			
			createCommunityIdSelect(request, response);
			createSourceTemplateSelect(request, response);
		}
		catch (Exception e)
		{
			System.out.println(e.getMessage());
		}
	}
	
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
	
	<link rel="stylesheet" type="text/css" href="inc/manager.css" />
	
	<script type="text/javascript" src="inc/utilities.js"></script>
	<link rel="shortcut icon" href="image/favicon.ico" />
	
   <script src="lib/jquery.js"></script>
   <script src="lib/jquery.cookie.js"></script>
   
    <script src="lib/splitter.js"></script>
    
   	<script type="text/javascript" src="lib/codemirror.js"></script>
   	<script type="text/javascript" src="lib/languages/javascript.js"></script>
	<link rel="stylesheet" type="text/css" href="lib/codemirror.css" />
    <script src="lib/codemirror_extra/dialog/dialog.js"></script>
    <link rel="stylesheet" href="lib/codemirror_extra/dialog/dialog.css"/>
    <script src="lib/codemirror_extra/search/searchcursor.js"></script>
    <script src="lib/codemirror_extra/search/search.js"></script>
    <script src="lib/codemirror_extra/edit/matchbrackets.js"></script>
    <script src="lib/codemirror_extra/fold/foldcode.js"></script>
    <script src="lib/codemirror_extra/fold/brace-fold.js"></script>
    
    <script src="lib/jshint.js"></script>
    
    <script src="inc/sampleSources.js"></script>
	
<style media="screen" type="text/css">
	
#lrSplitter {
	width: 100%;
	height: 750px;
}
#tbSplitter {
	height: 700px;
}
#lrSplitter .Pane {
	overflow: auto;
}
#Right {
	overflow: hidden;
}
.vsplitbar {
	width: 3px;
	background: #999999 no-repeat center;
	/* No margin, border, or padding allowed */
}
.vsplitbar.active, .vsplitbar:hover {
	background: #e88 no-repeat center;
}
.hsplitbar {
	height: 3px;
	background: #999999 no-repeat center;
	/* No margin, border, or padding allowed */
}
.hsplitbar.active, .hsplitbar:hover {
	background: #e88 no-repeat center;
}
.CodeMirror { border-width:1px; border-style: solid; border-color:#DBDFE6; }
.CodeMirror-foldmarker {
        color: blue;
        text-shadow: #b9f 1px 1px 2px, #b9f -1px -1px 2px, #b9f 1px -1px 2px, #b9f -1px 1px 2px;
        font-family: arial;
        line-height: .3;
        cursor: pointer;
      }
</style>
	
<script type="text/javascript">
$().ready(function() {
	$("#lrSplitter").splitter({
		type: "v",
		sizeLeft: 400, maxLeft: 400,
		outline: true,
		cookie: "lrSplitterNew"
	});
});
$().ready(function() {
	$("#tbSplitter").splitter({
		type: "h",
		sizeTop: 170, minTop: 33, maxTop: 170,
		outline: true,
		cookie: "tbSplitterNew"
	});
});
</script>
<script language=javascript>

var currWidth = 0;
var currHeight = 0;

var int=self.setInterval(function(){clock()},50);
function clock()
  {
	var newHeight = $('#Bottom').height() - 20;
	var newWidth = $('#Right').width() - 25;
	
	if ((currWidth != newWidth) || (currHeight != newHeight)) {
		currWidth = newWidth;
		currHeight = newHeight;
		$("#tbSplitter").css("width", ($('#Right').width() - 20)+"px").trigger("resize");
		$("#Top").css("width", ($('#Right').width() - 20)+"px");
		sourceJsonEditor.setSize(newWidth, newHeight);
	}
  }
  $(window).resize(function(){
  	var leftWidth = $('#Left').width();
  	var winWidth = $(window).width();
  	$("#Right").css("width", (winWidth - leftWidth - 20)+"px");
  });
</script>
<script language=javascript>
	function checkFormat(alertOnSuccess)
	{
		var success = JSHINT(sourceJsonEditor.getValue());
		var output = "<fmt:message key='source.result.check_format.success'/>";
		if (!success) {
			output = "<fmt:message key='source.result.check_format.error'/>\n\n";
			for (var i in JSHINT.errors) {
				var err = JSHINT.errors[i];
				if (null != err) {
					output += err.line + '[' + err.character + ']: ' + err.reason + '\n';
				}
				else {
					output += "<fmt:message key='source.result.check_format.unknown_error'/>\n";
				}
			}
		}
		if (alertOnSuccess || !success) {
			if (output == "") {
				output = "<fmt:message key='source.result.check_format.success'/>\n";
			}
			alert(output);
		}
		return success;
	}
	function fill_in_source_template()
	{
		var el = document.getElementById("sourceTemplateSelect");
		if (SAMPLE_SOURCES[el.value]) {
			document.getElementById('sourceTemplateSample').value = JSON.stringify(SAMPLE_SOURCES[el.value]);
		}
	}
</script>	
	<title><fmt:message key='newsource.title'/></title>
</head>
<body>

<%
	// !-- Create JavaScript Popup --
	if (messageToDisplay.length() > 0) { 
%>
	<script language="javascript" type="text/javascript">
		alert('<%=messageToDisplay.replace("'", "\\'") %>');
	</script>
<% } %>

	<form method="post" onsubmit="fill_in_source_template()">
	
<%@ include file="inc/header.jsp.inc" %>

<% if (!isLoggedIn) { %>
		<%@ include file="inc/login_form.jsp" %>
<% } else { %>
	
	 <div id="lrSplitter">
		 <div id="Left" class="Pane">
		
			<table class="standardTable" cellpadding="5" cellspacing="1" width="100%">
			<tr>
				<td class="headerLink"><fmt:message key='newsource.templates.title'/></td>
				<td align="right"></td>
			</tr>
			<tr>
				<td colspan="2" bgcolor="white">
					<table class="listTable" cellpadding="3" cellspacing="1" width="100%">
						<tr>
							<td bgcolor="white"><%=sourceTemplateSelect %><button name="selectTemplate" value="selectTemplate"><fmt:message key='newsource.templates.action.select'/></button></td>
						</tr>
						<tr>
							<td bgcolor="white">&nbsp;</td>
						</tr>
					</table>
				</td>
			</tr>
			</table>

		</div><!-- Left -->
		<div id="Right">
		
			<table class="standardTable" cellpadding="5" cellspacing="1" width="100%">
			<tr>
				<td class="headerLink"><fmt:message key='newsource.editor.title'/></td>
				<td align="right">
					<button <%=  sourceUpdateTemplateButton %> onclick="return checkFormat(false) && confirm('<fmt:message key='newsource.editor.action.update_template'/>')" name="updateTemplate" value="updateTemplate"><fmt:message key='newsource.editor.action.update_template'/></button>				
					<button onclick="return checkFormat(false)" name="saveSource" value="saveSource"><fmt:message key='newsource.editor.action.save_source'/></button>
				</td>
			</tr>
			<tr>
				<td colspan="2" bgcolor="white">
					<div id="tbSplitter">
					<div id="Top" class="Pane">
					<table class="standardSubTable" cellpadding="3" cellspacing="1" width="100%" >
						<tr>
							<td bgcolor="white" width="30%"><fmt:message key='newsource.editor.title.title'/></td>
							<td bgcolor="white" width="70%">
								<input type="text" id="shareTitle" name="shareTitle" value="<%=org.apache.commons.lang.StringEscapeUtils.escapeHtml(shareTitle)%>" size="60" />
							</td>		
						</tr>
						<tr valign="top">
							<td bgcolor="white" width="30%"><fmt:message key='newsource.editor.description.title'/></td>
							<td bgcolor="white" width="70%">
								<textarea cols="45" rows="3" id="shareDescription" name="shareDescription"><%=shareDescription%></textarea>
							</td>		
						</tr>
						<tr>
							<td bgcolor="white" width="30%"><fmt:message key='newsource.editor.tags.title'/></td>
							<td bgcolor="white" width="70%">
								<input type="text" id="shareTags" name="shareTags" value="<%=org.apache.commons.lang.StringEscapeUtils.escapeHtml(shareTags)%>" size="60" />
							</td>		
						</tr>
						<tr>
							<td bgcolor="white" width="30%"><fmt:message key='newsource.editor.community.title'/></td>
							<td bgcolor="white" width="70%"><%=communityIdSelect%></td>		
						</tr>
					</table>
					</div>
					<div id="Bottom" class="Pane">					
						<textarea cols="90" rows="25" id="Source_JSON" name="Source_JSON"><%=sourceJson%></textarea>
					</div>
					</div>
					
					
				</td>
			</tr>
			</table>
		
		</div><!--  Right -->
	</div><!-- lrSplitter -->
	<input type="hidden" name="templateShareId" id="templateShareId" value="<%=templateShareId%>"/>	
	<input type="hidden" name="sourceTemplateSample" id="sourceTemplateSample" value="" />			
	</form>
	
<!---------- CodeMirror JavaScripts ---------->
<script>
var foldFunc = CodeMirror.newFoldFunction(CodeMirror.braceRangeFinder);
	var sourceJsonEditor = CodeMirror.fromTextArea(document.getElementById("Source_JSON"), {
		mode: "application/json",
		lineNumbers: true,
		matchBrackets: true,
		extraKeys: { "Tab": "indentAuto", "Ctrl-Q": function(cm){foldFunc(cm, cm.getCursor().line);}}
	});
	sourceJsonEditor.setSize("100%", "100%");
	sourceJsonEditor.on("gutterClick", foldFunc);
</script>
	
	
	
<% } %>

<%@ include file="inc/footer.jsp" %>
</body>
</html>


<%!

private void updateFieldsFromSample(JSONObject sample)
{
	try {
		shareTitle = sample.getString("title");
	}
	catch (Exception e) {} // carry on

	try {
		shareDescription = sample.getString("description");
	}
	catch (Exception e) {} // carry on

	try {
		if (sample.has("tags")) {
			StringBuilder stags = new StringBuilder();
			JSONArray arrTags = sample.getJSONArray("tags");
			for (int i = 0; i < arrTags.length(); ++i) {
				stags.append(arrTags.get(i)).append(' ');
			}
			shareTags = stags.toString();
		}
	}
	catch (Exception e) {} // carry on
}

//saveShare - 
private String saveShare(HttpServletRequest request, HttpServletResponse response) 
{
	String returnid = "";
	if (validateFormFields() && validateSourceJson())
	{
		try 
		{
			String urlShareTitle = URLEncoder.encode(shareTitle.trim(), "UTF-8");
			String urlShareDescription = URLEncoder.encode(shareDescription.trim(), "UTF-8");
			String apiAddress = "social/share/add/json/source/" + urlShareTitle + "/" + urlShareDescription;
			
			//
			JSONObject source = new JSONObject(sourceJson);
			source.remove("title");
			source.put("title", shareTitle.trim());
			source.remove("description");
			source.put("description", shareDescription.trim());
			source.remove("tags");
			String trimmedShareTags = shareTags.trim();
			if (!trimmedShareTags.isEmpty())
				source.put("tags", new JSONArray(trimmedShareTags.split("(?:\\s*,\\s*|\\s+)")));
			
			// CommunityID Array - Delete and replace with id from community id dropdown list
			if (communityId.length() > 0)
			{
				source.remove("communityIds");
				JSONArray communityIds = new JSONArray();
				communityIds.put(communityId);
				source.put("communityIds", communityIds);
				sourceJson = source.toString(4);
			} //TESTED
	
			// Post the update to our rest API and check the results of the post
			JSONObject json_response =  new JSONObject(postToRestfulApi(apiAddress, sourceJson, request, response));
			JSONObject responseObject = json_response.getJSONObject("response");
			JSONObject shareObject = json_response.getJSONObject("data");
			
			if (responseObject.getString("success").equalsIgnoreCase("true")) 
			{
				returnid = shareObject.getString("_id");
				messageToDisplay = "Success: " + responseObject.getString("message");
			}
			else
			{
				messageToDisplay = "Error: " + responseObject.getString("message");
			}
		} 
		catch (Exception e) 
		{
			messageToDisplay = "Error: " + e.getMessage() + " " + e.getStackTrace().toString();
		}
	}
	return returnid;
} // TESTED


// validateFormFields
private boolean validateFormFields()
{
	boolean isValid = true;
	ArrayList<String> al = new ArrayList<String>();
	if (shareTitle.length() < 1) al.add("Title");
	if (shareDescription.length() < 1) al.add("Description");
	if (sourceJson.length() < 1) al.add("Source JSON");
	
	if (al.size() > 0)
	{
		isValid = false;
		messageToDisplay = "Error, the following required fields are missing: " + al.toString();
	}
	return isValid;
}  // TESTED


// validateSourceJson
private boolean validateSourceJson()
{
	boolean isValid = true;
	try
	{
		JSONObject test = new JSONObject(sourceJson);
	}
	catch (Exception e)
	{
		isValid = false;
		messageToDisplay = "Error, the JSON in the source field is not valid. (" + e.getMessage() + ")";
	}
	return isValid;
}


// createCommunityIdSelect -
// Create select control with list of communityids available to user
private void createCommunityIdSelect(HttpServletRequest request, HttpServletResponse response) 
{
	try 
	{
		StringBuffer html = new StringBuffer();
		html.append("<select name=\"Community_ID\" id=\"Community_ID\">");
		
		JSONArray communities = getPersonCommunities(request, response);
		if (communities != null)
		{
			for (int i = 0; i < communities.length(); i++) 
			{
				JSONObject source = communities.getJSONObject(i);
				String name = source.getString("name");
				String id = source.getString("_id");
				String selectedString = (id.equalsIgnoreCase(communityId)) ? " SELECTED" : "";
				html.append("<option value=\"" + id + "\"" + selectedString + ">" + name + "</option>");
			}				
		}
		html.append("</select>");
		communityIdSelect = html.toString();
	} 
	catch (Exception e) 
	{
	}
} // TESTED



// createSourceTemplateSelect -
// Create select control with list of source templates available to user
private void createSourceTemplateSelect(HttpServletRequest request, HttpServletResponse response) 
{
	StringBuffer sources = new StringBuffer();
	sources.append("<select name=\"sourceTemplateSelect\" id=\"sourceTemplateSelect\">");
	sources.append("<option value=\"\">------------------Select an example source or template:</option>");
	sources.append("<option value=\"empty\">Empty Source Template</option>");
	sources.append("<option value=\"rss\">RSS Source Example</option>");
	sources.append("<option value=\"web\">Web Page Source Example</option>");
	sources.append("<option value=\"json_api_simple\">Simple JSON API Example</option>");
	sources.append("<option value=\"json_api_links\">Complex JSON API Example #1 (link following)</option>");
	sources.append("<option value=\"json_api_complex\">Complex JSON API Example #2 (document splitting)</option>");
	sources.append("<option value=\"local_file\">Local File Example (any file type)</option>");
	sources.append("<option value=\"remote_file\">Fileshare Example (any file type)</option>");
	sources.append("<option value=\"remote_file_logs\">Fileshare Example (log file type)</option>");
	sources.append("<option value=\"amazon_file\">Amazon S3 Source Example (any file type)</option>");
	sources.append("<option value=\"infinite_share_upload\">Infinit.e ZIP Archives/JSON Share Example</option>");
	sources.append("<option value=\"infinite_custom_ingest\">Infinit.e Custom Analytics Example</option>");
	sources.append("<option value=\"database\">Basic SQL Database Example</option>");
	sources.append("<option value=\"logstash\">Logstash Template</option>");
	sources.append("<option value=\"\">------------------User/Shared templates:</option>");
	
	String apiAddress = "social/share/search/?searchby=type&type=source_template";
	try 
	{
		// Call the api and get the result as a string
		String result = callRestfulApi(apiAddress, request, response);
		JSONObject json = new JSONObject(result);
		JSONObject json_response = json.getJSONObject("response");
		if (json_response.getString("success").equalsIgnoreCase("true") && json.has("data")) 
		{
			JSONArray data = json.getJSONArray("data");
			for (int i = 0; i < data.length(); i++) 
			{
				JSONObject source = data.getJSONObject(i);
				String title = source.getString("title");
				String id = source.getString("_id");
				sources.append("<option value=\"" + id + "\">" + title
						+ "</option>");
			}
		}
	} 
	catch (Exception e) 
	{
	}
	
	sources.append("</select>");
	sourceTemplateSelect = sources.toString();
} // TESTED


// getSourceJSONObject
private JSONObject getSourceJSONObjectFromShare(String shareId, HttpServletRequest request, HttpServletResponse response)
{
	try
	{
		// Call the api and get the result as a string
		String result = getShare(shareId, request, response);
		
		// Convert string to JSONObjects
		JSONObject json_response = new JSONObject(result);
		JSONObject data = json_response.getJSONObject("data");
		
		JSONObject source = new JSONObject(data.getString("share"));
		source.remove("_id");
		source.remove("communityIds");
		source.remove("created");
		source.remove("harvest");
		source.remove("harvestBadSource");
		source.remove("isApproved");
		source.remove("key");
		source.remove("modified");
		source.remove("ownerId");
		source.remove("shah256Hash");
		
		try {
			shareTitle = source.getString("title");
		}
		catch (Exception e) {} // title/desc/tags not present
		try {
			shareDescription = source.getString("description");
		}
		catch (Exception e) {} // title/desc/tags not present
		try {
			if (source.has("tags")) {
				StringBuilder stags = new StringBuilder();
				JSONArray arrTags = source.getJSONArray("tags");
				for (int i = 0; i < arrTags.length(); ++i) {
					stags.append(arrTags.get(i)).append(' ');
				}
				shareTags = stags.toString();
			}
		}
		catch (Exception e) {} // title/desc/tags not present
		
		return source;
	}
	catch (Exception e)
	{
		return null;
	}
}

//saveSourceAsTemplate - 
private void updateTemplate(HttpServletRequest request, HttpServletResponse response) 
{
	try 
	{
		if (validateFormFields() && validateSourceJson())
		{
			JSONObject source = new JSONObject(sourceJson);
			source.remove("title");
			source.put("title", shareTitle.trim());
			source.remove("description");
			source.put("description", shareDescription.trim());
	
			// Remove any non-functional things:
			source.remove("_id");
			source.remove("communityIds");
			source.remove("created");
			source.remove("harvest");
			source.remove("harvestBadSource");
			source.remove("isApproved");
			source.remove("key");
			source.remove("modified");
			source.remove("ownerId");
			source.remove("shah256Hash");
			
			sourceJson = source.toString(4);
			
			String urlShareTitle = URLEncoder.encode(shareTitle + " - Template", "UTF-8");
			String urlShareDescription = URLEncoder.encode(shareDescription, "UTF-8");
			String apiAddress = "social/share/update/json/"+templateShareId+"/source_template/" + urlShareTitle + "/" + urlShareDescription;
			JSONObject JSONresponse = new JSONObject(postToRestfulApi(apiAddress, sourceJson, request, response)).getJSONObject("response");
			if (JSONresponse.getString("success").equalsIgnoreCase("true")) 
			{
				messageToDisplay = "Success: " + JSONresponse.getString("message");
			}
			else
			{
				messageToDisplay = "Error: " + JSONresponse.getString("message");
			}
		}
		
	} 
	catch (Exception e) 
	{
		messageToDisplay = "Error: " + e.getMessage() + " " + e.getStackTrace().toString();
	}
} //


%>
