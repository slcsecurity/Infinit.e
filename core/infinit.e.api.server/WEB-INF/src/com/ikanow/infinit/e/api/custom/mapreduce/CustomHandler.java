/*******************************************************************************
 * Copyright 2012, The Infinit.e Open Source Project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.ikanow.infinit.e.api.custom.mapreduce;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.bson.types.ObjectId;

import com.ikanow.infinit.e.api.social.sharing.ShareHandler;
import com.ikanow.infinit.e.api.utils.RESTTools;
import com.ikanow.infinit.e.api.utils.SocialUtils;
import com.ikanow.infinit.e.data_model.api.ResponsePojo;
import com.ikanow.infinit.e.data_model.api.ResponsePojo.ResponseObject;
import com.ikanow.infinit.e.data_model.api.custom.mapreduce.CustomMapReduceJobPojoApiMap;
import com.ikanow.infinit.e.data_model.index.ElasticSearchManager;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.MongoDbManager;
import com.ikanow.infinit.e.data_model.store.custom.mapreduce.CustomMapReduceJobPojo;
import com.ikanow.infinit.e.data_model.store.custom.mapreduce.CustomMapReduceJobPojo.SCHEDULE_FREQUENCY;
import com.ikanow.infinit.e.data_model.store.custom.mapreduce.CustomMapReduceJobPojo.INPUT_COLLECTIONS;
import com.ikanow.infinit.e.data_model.store.social.community.CommunityPojo;
import com.ikanow.infinit.e.data_model.store.social.community.CommunityPojo.CommunityType;
import com.ikanow.infinit.e.data_model.store.social.person.PersonCommunityPojo;
import com.ikanow.infinit.e.data_model.store.social.person.PersonPojo;
import com.ikanow.infinit.e.data_model.store.social.sharing.SharePojo;
import com.ikanow.infinit.e.processing.custom.CustomProcessingController;
import com.ikanow.infinit.e.processing.custom.output.CustomOutputIndexingEngine;
import com.ikanow.infinit.e.processing.custom.utils.CustomApiUtils;
import com.ikanow.infinit.e.processing.custom.utils.HadoopUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

public class CustomHandler 
{
	
	private static final Logger logger = Logger.getLogger(CustomHandler.class);
	
	public CustomHandler() {
		this(null);
	}
	public CustomHandler(Integer debugLimit) {
		_debugLimit = debugLimit;
	}
	private Integer _debugLimit = null;
	
	/**
	 * Returns the results of a map reduce job if it has completed
	 * 
	 * @param userid
	 * @param jobid
	 * @return
	 */
	
	public ResponsePojo getJobResults(String userid, String jobid, int limit, String fields, String findStr, String sortStr, boolean bCsv ) 
	{
		ResponsePojo rp = new ResponsePojo();		
		
		List<Object> searchTerms = new ArrayList<Object>();
		try
		{
			ObjectId jid = new ObjectId(jobid);
			searchTerms.add(new BasicDBObject(CustomMapReduceJobPojo._id_,jid));
		}
		catch (Exception ex)
		{
			//oid failed, will only add title
		}
		searchTerms.add(new BasicDBObject(CustomMapReduceJobPojo.jobtitle_,jobid));
				
		try 
		{
			//find admin entry);
			DBObject dbo = DbManager.getCustom().getLookup().findOne(new BasicDBObject(DbManager.or_,searchTerms.toArray()));			
			if ( dbo != null )
			{				
				CustomMapReduceJobPojo cmr = CustomMapReduceJobPojo.fromDb(dbo, CustomMapReduceJobPojo.class);
				//make sure user is allowed to see results
				if ( RESTTools.adminLookup(userid) || isInAllCommunities(cmr.communityIds, userid) )
				{										
					//get results collection if done and return
					if ( ( cmr.lastCompletionTime != null ) || (cmr.mapper.equals("none") && cmr.exportToHdfs))
					{
						CustomApiUtils.getJobResults(rp, cmr, limit, fields, findStr, sortStr, bCsv);
					}
					else
					{
						rp.setResponse(new ResponseObject("Custom Map Reduce Job Results",false,"Map reduce job has not completed yet"));
					}
				}
				else
				{
					rp.setResponse(new ResponseObject("Custom Map Reduce Job Results",false,"User is not a member of communities with read permissions"));
				}
			}
			else
			{
				rp.setResponse(new ResponseObject("Custom Map Reduce Job Results",false,"Job does not exist"));
			}
		} 
		catch (Exception e)
		{
			// If an exception occurs log the error
			logger.error("Exception Message: " + e.getMessage(), e);
			rp.setResponse(new ResponseObject("Custom Map Reduce Job Results",false,"error retrieving job info"));
		}
		return rp;
	}
	
	/**
	 * Checks if a user is part of all communities, or an admin
	 * 
	 * @param communityIds
	 * @param userid
	 * @return
	 */
	private boolean isInAllCommunities(List<ObjectId> communityIds, String userid)
	{		
		//test each community for membership
		try
		{
			BasicDBObject inQuery = new BasicDBObject(DbManager.in_, communityIds.toArray());
			BasicDBObject query = new BasicDBObject(CustomMapReduceJobPojo._id_,inQuery);
			DBCursor dbc = DbManager.getSocial().getCommunity().find(query);
			while (dbc.hasNext())
			{
				DBObject dbo = dbc.next();				
				CommunityPojo cp = CommunityPojo.fromDb(dbo, CommunityPojo.class);
				if (CommunityType.user == cp.getType()) { // Insta fail if adding a user group
					return false;
				}//TODO (INF-2866): TOTEST
				
				//if this is not your self community AND you are not a member THEN you are not in all these communities
				if ( !((cp.getId().toString()).equals(userid)) && !(cp.isMember(new ObjectId(userid))) )
					return false;
			}
		}
		catch(Exception ex)
		{
			//error looking up communities
			return false;
		}
		return true;
	}
	
	/**
	 * Schedules a map reduce job to be ran, whether it be repeated or not
	 * 
	 * @param userid
	 * @return
	 */	
	
	public ResponsePojo scheduleJob(String userid, String title, String desc, String communityIds, String jarURL, String nextRunTime, String schedFreq, String mapperClass, String reducerClass, String combinerClass, String query, String inputColl, String outputKey, String outputValue, String appendResults, String ageOutInDays, Boolean incrementalMode, String jobsToDependOn, String json, Boolean exportToHdfs, boolean bQuickRun, Boolean selfMerge)
	{
		ResponsePojo rp = new ResponsePojo();
		List<ObjectId> commids = new ArrayList<ObjectId>(); 
		for ( String s : communityIds.split(","))
			commids.add(new ObjectId(s));
		boolean bAdmin = RESTTools.adminLookup(userid);
		//first make sure user is allowed to submit on behalf of the commids given
		if ( bAdmin || isInAllCommunities(commids, userid) )
		{
			CustomMapReduceJobPojo cmr = new CustomMapReduceJobPojo();
			//make sure user can use the input collection
			String inputCollection = getStandardInputCollection(inputColl);			
			if ( inputCollection != null )
			{
				cmr.isCustomTable = false;
			}
			else
			{
				inputCollection = getCustomInputCollection(inputColl, commids);
				cmr.isCustomTable = true;
			}
			if ( inputCollection != null)
			{			
				try 
				{					
					cmr.communityIds = commids;
					cmr._id = new ObjectId();
					cmr.jobtitle = title;
					cmr.jobdesc = desc;
					cmr.inputCollection = inputCollection;
					if ((null == jarURL) || jarURL.equals("null")) {
						cmr.jarURL = null;
						// Force the types:
						outputKey = "org.apache.hadoop.io.Text";
						outputValue = "com.mongodb.hadoop.io.BSONWritable";
					}
					else {
						cmr.jarURL = jarURL;
					}
					cmr.outputCollection = cmr._id.toString() + "_1";
					cmr.outputCollectionTemp = cmr._id.toString() + "_2";
					cmr.exportToHdfs = exportToHdfs;
					
					cmr.setOutputDatabase(CustomApiUtils.getJobDatabase(cmr));
					
					cmr.submitterID = new ObjectId(userid);
					long nextRun = Long.parseLong(nextRunTime);
					cmr.firstSchedule = new Date(nextRun);					
					cmr.nextRunTime = nextRun;
					//if this job is set up to run before now, just set the next run time to now
					//so we can schedule jobs appropriately
					long nNow = new Date().getTime();
					if ( cmr.nextRunTime < nNow )
						cmr.nextRunTime = nNow - 1;
					//TESTED
					
					cmr.scheduleFreq = SCHEDULE_FREQUENCY.valueOf(schedFreq);
					if ( (null != mapperClass) && !mapperClass.equals("null"))
						cmr.mapper = mapperClass;
					else
						cmr.mapper = "";
					if ( (null != reducerClass) && !reducerClass.equals("null"))
						cmr.reducer = reducerClass;
					else
						cmr.reducer = "";
					if ( (null != combinerClass) &&  !combinerClass.equals("null"))
						cmr.combiner = combinerClass;
					else
						cmr.combiner = "";
					if ( (null != outputKey) && !outputKey.equals("null"))
						cmr.outputKey = outputKey;
					else
						cmr.outputKey = "com.mongodb.hadoop.io.BSONWritable";
					if ( (null != outputValue) && !outputValue.equals("null"))
						cmr.outputValue = outputValue;
					else
						cmr.outputValue = "com.mongodb.hadoop.io.BSONWritable";
					if ( (null != query) && !query.equals("null") && !query.isEmpty())
						cmr.query = query;
					else
						cmr.query = "{}";
					
					boolean append = false;
					double ageOut = 0.0;
					try
					{
						append = Boolean.parseBoolean(appendResults);
						ageOut = Double.parseDouble(ageOutInDays);
					}
					catch (Exception ex)
					{
						append = false;
						ageOut = 0.0;
					}
					cmr.appendResults = append;
					cmr.appendAgeOutInDays = ageOut;
					cmr.incrementalMode = incrementalMode;
					cmr.selfMerge = selfMerge;
					
					if ( json != null && !json.equals("null") )
						cmr.arguments = json;
					else
						cmr.arguments = null;
					
					if ((null == cmr.jarURL) && (null != cmr.arguments) && !cmr.arguments.isEmpty()) {
						// In saved query, if arguments is valid BSON then copy over query
						try {
							Object tmpQuery = com.mongodb.util.JSON.parse(cmr.arguments);
							if (tmpQuery instanceof BasicDBObject) {
								cmr.query = cmr.arguments;
							}
						}
						catch (Exception e) {} // fine just carry on
					}
					else if ((null == cmr.jarURL)) { // ie args == null, copy from query
						cmr.arguments = cmr.query;
					}
					
					//try to work out dependencies, error out if they fail
					if ( (null != jobsToDependOn) && !jobsToDependOn.equals("null"))
					{
						try
						{
							cmr.jobDependencies = getJobDependencies(jobsToDependOn);
							cmr.waitingOn = cmr.jobDependencies;
						}
						catch (Exception ex)
						{
							rp.setResponse(new ResponseObject("Schedule MapReduce Job",false,"Error parsing the job dependencies, did a title or id get set incorrectly or did a job not exist?"));
							return rp;
						}
					}
					
					//make sure title hasn't been used before
					DBObject dbo = DbManager.getCustom().getLookup().findOne(new BasicDBObject("jobtitle",title));
					if ( dbo == null )
					{
						Date nextRunDate = new Date(nextRun);
						Date now = new Date();
						String nextRunString = nextRunDate.toString();
						boolean bRunNowIfPossible = false;
						if ( nextRunDate.getTime() < now.getTime() ) {
							nextRunString = "next available timeslot";
							bRunNowIfPossible = true;
						}
						rp.setResponse(new ResponseObject("Schedule MapReduce Job",true,"Job scheduled successfully, will run on: " + nextRunString));
						rp.setData(cmr._id.toString(), null);
												
						if (bRunNowIfPossible) {
							CustomApiUtils.runJobAndWaitForCompletion(cmr, bQuickRun, false, _debugLimit);
						}//TESTED
						else {
							DbManager.getCustom().getLookup().save(cmr.toDb());							
						}
					}
					else
					{					
						rp.setResponse(new ResponseObject("Schedule MapReduce Job",false,"A job already matches that title, please choose another title"));
					}
				} 
				catch (IllegalArgumentException e)
				{
					logger.error("Exception Message: " + e.getMessage(), e);
					rp.setResponse(new ResponseObject("Schedule MapReduce Job",false,"No enum matching scheduled frequency, try NONE, DAILY, WEEKLY, MONTHLY"));
				}
				catch (Exception e)
				{
					// If an exception occurs log the error
					logger.error("Exception Message: " + e.getMessage(), e);
					rp.setResponse(new ResponseObject("Schedule MapReduce Job",false,"error scheduling job"));
				}					
			}
			else
			{
				rp.setResponse(new ResponseObject("Schedule MapReduce Job",false,"You do not have permission to use the given input collection."));
			}
		}
		else
		{
			rp.setResponse(new ResponseObject("Schedule MapReduce Job",false,"You do not have permissions for all the communities given."));
		}
		return rp;
	}
	
	/**
	 * Tests a map reduce job to be run, discarding the output
	 * 
	 * @param userid
	 * @return
	 */	
	public ResponsePojo testJob(String userId, String communityIds, CustomMapReduceJobPojo job, int testLimit)
	{
		//TODO (INF-2865): add support to be able to call this from the RESTful interface
		
		// Simple security checks:
		List<ObjectId> commids = null;
		if (null != communityIds) {
			commids = new ArrayList<ObjectId>(); 
			for ( String s : communityIds.split(","))
				commids.add(new ObjectId(s));
		}
		else {
			commids = job.communityIds;
		}
		boolean bAdmin = RESTTools.adminLookup(userId);
		
		//first make sure user is allowed to submit on behalf of the commids given
		if ( bAdmin || isInAllCommunities(commids, userId) )
		{			
			ObjectId testCollection = new ObjectId();
	
			job.setOutputDatabase(CustomApiUtils.getJobDatabase(job));
			job.outputCollection = testCollection.toString();
			job.outputCollectionTemp = job.outputCollection;
			
			try {
				CustomApiUtils.runJobAndWaitForCompletion(job, true, true, _debugLimit);
				ResponsePojo rp = new ResponsePojo();
				CustomApiUtils.getJobResults(rp, job, 100, null, null, null);
				rp.getResponse().setAction("Test MapReduce Job");
				return rp;
			}
			finally {
				DbManager.getCollection(job.getOutputDatabase(), job.outputCollection).drop();
			}
		}
		else {
			ResponsePojo rp = new ResponsePojo();
			rp.setResponse(new ResponseObject("Test MapReduce Job",false,"You do not have permissions for all the communities given."));
			return rp;
		}
	}
	
	public ResponsePojo updateJob(String userid, String jobidortitle, String title, String desc, String communityIds, String jarURL, String nextRunTime, String schedFreq, String mapperClass, String reducerClass, String combinerClass, String query, String inputColl, String outputKey, String outputValue, String appendResults, String ageOutInDays, Boolean incrementalMode, String jobsToDependOn, String json, Boolean exportToHdfs, boolean bQuickRun, Boolean selfMerge)
	{
		String esVersionWarning = "";
		ResponsePojo rp = new ResponsePojo();
		//first make sure job exists, and user is allowed to edit
		List<Object> searchTerms = new ArrayList<Object>();
		try
		{
			ObjectId jid = new ObjectId(jobidortitle);
			searchTerms.add(new BasicDBObject(CustomMapReduceJobPojo._id_,jid));
		}
		catch (Exception ex)
		{
			//oid failed, will only add title
		}
		searchTerms.add(new BasicDBObject(CustomMapReduceJobPojo.jobtitle_,jobidortitle));
		DBObject dbo = DbManager.getCustom().getLookup().findOne(new BasicDBObject(DbManager.or_,searchTerms.toArray()));
		
		if ( dbo != null )
		{
			CustomMapReduceJobPojo cmr = CustomMapReduceJobPojo.fromDb(dbo, CustomMapReduceJobPojo.class);
			//verify user can update this job
			if ( RESTTools.adminLookup(userid) || cmr.submitterID.toString().equals(userid) )
			{
				//check if job is already running
				if ( ( cmr.jobidS != null ) && !cmr.jobidS.equals( "CHECKING_COMPLETION" ) &&  !cmr.jobidS.equals( "" ) ) // (< robustness, sometimes server gets stuck here...)
				{
					// If it is running and we're trying to turn it off .. .then kill the job:
					com.ikanow.infinit.e.processing.custom.utils.PropertiesManager customProps = new com.ikanow.infinit.e.processing.custom.utils.PropertiesManager();
					boolean bLocalMode = customProps.getHadoopLocalMode();
					
					boolean tryToKillJob = false;
					if (!bLocalMode) { // else not possible
						
						// This line means: either we're NONE already (and it hasn't changed), or we've been changed to NONE
						if ((((null == schedFreq) || (schedFreq.equalsIgnoreCase("null"))) 
											&& (CustomMapReduceJobPojo.SCHEDULE_FREQUENCY.NONE == cmr.scheduleFreq))
								||
							(null != schedFreq) && (schedFreq.equalsIgnoreCase("none")))
						{
							long candidateNextRuntime = 0L;
							try {
								candidateNextRuntime = Long.parseLong(nextRunTime);
							}
							catch (Exception e) {}
							if (candidateNextRuntime >= CustomApiUtils.DONT_RUN_TIME) {
								tryToKillJob = true;
							}
						}
					}//TESTED - (don't run/daily/once-only) - covers all the cases, except the "theoretical" null cases
					
					if (tryToKillJob) {
						// (ie is running and updating it to mean don't run anymore .. that 4e12 number is 2099 in ms, anything bigger than that is assumed to mean "don't run)
						CustomProcessingController pxController = new CustomProcessingController();
						if (pxController.killRunningJob(cmr)) {						
							rp.setResponse(new ResponseObject("Update MapReduce Job",true,"Killed job, may take a few moments for the status to update."));
						}
						else {
							rp.setResponse(new ResponseObject("Update MapReduce Job",false,"Failed to kill the job - it may not have started yet, try again in a few moments."));
						}							
						return rp;
					}//TODO (INF-2395): TOTEST
					else {
						rp.setResponse(new ResponseObject("Update MapReduce Job",false,"Job is currently running (or not yet marked as completed).  Please wait until the job completes to update it."));
						return rp;
					}
				}
				if (cmr.jobidS != null) { // (must be checking completion, ie in bug state, so reset...)
					cmr.jobidS = null;
					cmr.jobidN = 0;
				}
				//check each variable to see if its needs/can be updated
				if ( (null != communityIds) && !communityIds.equals("null") )
				{
					List<ObjectId> commids = new ArrayList<ObjectId>(); 
					for ( String s : communityIds.split(","))
						commids.add(new ObjectId(s));
					boolean bAdmin = RESTTools.adminLookup(userid);
					//make sure user is allowed to submit on behalf of the commids given
					if ( bAdmin || isInAllCommunities(commids, userid) )
					{
						ElasticSearchManager customIndex = null;
						try {
							customIndex = CustomOutputIndexingEngine.getExistingIndex(cmr);
							if (null != customIndex) {
								CustomOutputIndexingEngine.swapAliases(customIndex, commids, true);  
							}//TESTED (by hand - removal and deletion)						
						}
						catch (Throwable t) { // 0.19 - just delete the index if we're trying to change its security settings 
							if (null != customIndex) {
								customIndex.deleteMe();
								esVersionWarning = " (Error changing index security settings (probably ES version related) - deleted the index of the results, to rebuild it please rerun the job)";
							}							
						}						
						cmr.communityIds = commids;
					}
					else
					{
						rp.setResponse(new ResponseObject("Update MapReduce Job",false,"You do have permissions for all the communities given."));
						return rp;
					}
				}
				if ( (null != inputColl) && !inputColl.equals("null"))
				{
					//make sure user can use the input collection
					String inputCollection = getStandardInputCollection(inputColl);			
					
					if ( inputCollection != null )
					{
						cmr.isCustomTable = false;
					}
					else
					{
						inputCollection = getCustomInputCollection(inputColl, cmr.communityIds);
						cmr.isCustomTable = true;
					}
					if ( inputCollection != null)
					{
						cmr.inputCollection = inputCollection;
					}
					else
					{
						rp.setResponse(new ResponseObject("Update MapReduce Job",false,"You do not have permission to use the given input collection."));
						return rp;
					}
				}
				try 
				{
					if ( (null != title) && !title.equals("null"))
					{
						// If this is indexed then can't change the title
						try {
							if (null != CustomOutputIndexingEngine.getExistingIndex(cmr)) {
								rp.setResponse(new ResponseObject("Update MapReduce Job",false,"You cannot change the title of a non-empty indexed job - you can turn indexing off and then change the title"));
								return rp;							
							}//TESTED (by hand)
						}
						catch (Throwable t) { } // (es 0.19 case, just carry on silently)
						
						cmr.jobtitle = title;
						//make sure the new title hasn't been used before
						DBObject dbo1 = DbManager.getCustom().getLookup().findOne(new BasicDBObject("jobtitle",title));
						if ( dbo1 != null )
						{
							rp.setResponse(new ResponseObject("Update MapReduce Job",false,"A job already matches that title, please choose another title"));
							return rp;
						}
					}
					if ( (null != desc) && !desc.equals("null"))
					{
						cmr.jobdesc = desc;
					}
					if ( (null != jarURL) && !jarURL.equals("null"))
					{
						cmr.jarURL = jarURL;
					}
					if ( (null != nextRunTime) && !nextRunTime.equals("null"))
					{
						cmr.nextRunTime = Long.parseLong(nextRunTime);
						long nNow = new Date().getTime();
						cmr.firstSchedule = new Date(cmr.nextRunTime);
						if (cmr.nextRunTime < nNow) { // ie leave firstSchedule alone since that affects when we next run, but just set this to now...
							cmr.nextRunTime = nNow - 1;
						}//TESTED
						cmr.timesRan = 0;
						cmr.timesFailed = 0;
					}
					if ( (null != schedFreq) && !schedFreq.equals("null"))
					{
						cmr.scheduleFreq = SCHEDULE_FREQUENCY.valueOf(schedFreq);
					}
					if ( (null != mapperClass) && !mapperClass.equals("null"))
					{
						cmr.mapper = mapperClass;
					}
					if ( (null != reducerClass) && !reducerClass.equals("null"))
					{
						cmr.reducer = reducerClass;
					}
					if ( (null != combinerClass) && !combinerClass.equals("null"))
					{
						cmr.combiner = combinerClass;
					}
					if ( (null != query) && !query.equals("null"))
					{
						boolean wasIndexed = false;
						try {
							wasIndexed = CustomOutputIndexingEngine.isIndexed(cmr);
						}
						catch (Throwable t) { } // (es 0.19 case, just carry on silently)
						
						if ( !query.isEmpty() )
							cmr.query = query;
						else
							cmr.query = "{}";
						
						// If we're in indexing mode, check if the index has been turned off, in which case delete the index
						try {
							if (wasIndexed && !CustomOutputIndexingEngine.isIndexed(cmr)) {
								CustomOutputIndexingEngine.deleteOutput(cmr);  
							}//TESTED (by hand)
						}
						catch (Throwable t) { } // (es 0.19 case, just carry on silently)							
					}
					if (null == cmr.jarURL) { // (if in savedQuery mode, force types to be Text/BSONWritable)
						// Force the types:
						outputKey = "org.apache.hadoop.io.Text";
						outputValue = "com.mongodb.hadoop.io.BSONWritable";						
					}
					if ( (null != outputKey) && !outputKey.equals("null"))
					{
						cmr.outputKey = outputKey;
					}
					if ( (null != outputValue) && !outputValue.equals("null"))
					{
						cmr.outputValue = outputValue;
					}
					if ( (null != appendResults) && !appendResults.equals("null"))
					{
						try
						{
							cmr.appendResults = Boolean.parseBoolean(appendResults);
						}
						catch (Exception ex)
						{
							cmr.appendResults = false;
						}
					}
					if ( (null != ageOutInDays) && !ageOutInDays.equals("null"))
					{
						try
						{
							cmr.appendAgeOutInDays = Double.parseDouble(ageOutInDays);
						}
						catch (Exception ex)
						{
							cmr.appendAgeOutInDays = 0.0;
						}
					}
					if (null != incrementalMode) 
					{
						cmr.incrementalMode = incrementalMode;
					}
					if (null != selfMerge) 
					{
						cmr.selfMerge = selfMerge;
					}
					
					if (null != exportToHdfs) {
						cmr.exportToHdfs = exportToHdfs;
					}
					
					//try to work out dependencies, error out if they fail
					if ( (null != jobsToDependOn) && !jobsToDependOn.equals("null"))
					{
						try
						{
							cmr.jobDependencies = getJobDependencies(jobsToDependOn);
							cmr.waitingOn = cmr.jobDependencies;
						}
						catch (Exception ex)
						{
							rp.setResponse(new ResponseObject("Update MapReduce Job",false,"Error parsing the job dependencies, did a title or id get set incorrectly or did a job not exist?"));
							return rp;
						}
					}
					if ( json != null && !json.equals("null"))
					{
						cmr.arguments = json;
					}
					else
					{
						cmr.arguments = null;
					}
					if ((null == cmr.jarURL) && (null != cmr.arguments) && !cmr.arguments.isEmpty()) {
						// In saved query, if arguments is valid BSON then copy over query
						try {
							Object tmpQuery = com.mongodb.util.JSON.parse(cmr.arguments);
							if (tmpQuery instanceof BasicDBObject) {
								cmr.query = cmr.arguments;
							}
						}
						catch (Exception e) {} // fine just carry on
					}
					else if ((null == cmr.jarURL)) { // ie args == null, copy from query
						cmr.arguments = cmr.query;
					}
					
				} 
				catch (IllegalArgumentException e)
				{
					// If an exception occurs log the error
					logger.error("Exception Message: " + e.getMessage(), e);
					rp.setResponse(new ResponseObject("Update MapReduce Job",false,"Illegal arg (enum needs to be DAILY/WEEKLY/MONTHLY/NONE?): " + e.getMessage()));
					return rp;
				}
				catch (Exception e)
				{
					// If an exception occurs log the error
					logger.error("Exception Message: " + e.getMessage(), e);
					rp.setResponse(new ResponseObject("Update MapReduce Job",false,"error scheduling job: " + e.getMessage()));
					return rp;
				}

				// Setup post-processing 
				
				String nextRunString = new Date(cmr.nextRunTime).toString();
				boolean bRunNowIfPossible = false;
				if ( cmr.nextRunTime < new Date().getTime() ) {
					nextRunString = "next available timeslot";
					bRunNowIfPossible = true;
				}
				
				rp.setResponse(new ResponseObject("Update MapReduce Job",true,"Job updated successfully, will run on: " + nextRunString + esVersionWarning));
				rp.setData(cmr._id.toString(), null);

				if (bRunNowIfPossible) {
					CustomApiUtils.runJobAndWaitForCompletion(cmr, bQuickRun, false, _debugLimit);
				}//TESTED
				else {
					DbManager.getCustom().getLookup().save(cmr.toDb());					
				}
			}
			else
			{
				rp.setResponse(new ResponseObject("Update MapReduce Job", false, "You do not have permission to submit this job"));
			}
		}
		else
		{
			rp.setResponse(new ResponseObject("Update MapReduce Job", false, "No jobs with this ID exist"));
		}
		return rp;
	}
	
	private String getStandardInputCollection(String inputColl)
	{
		try
		{
			INPUT_COLLECTIONS input = INPUT_COLLECTIONS.valueOf(inputColl);
			if ( input == INPUT_COLLECTIONS.DOC_METADATA )
				return "doc_metadata.metadata";
			if ( input == INPUT_COLLECTIONS.DOC_CONTENT )
				return "doc_content.gzip_content";
			if ( input == INPUT_COLLECTIONS.FEATURE_ASSOCS )
				return "feature.association";
			if ( input == INPUT_COLLECTIONS.FEATURE_ENTITIES )
				return "feature.entity";			
			if ( input == INPUT_COLLECTIONS.FEATURE_TEMPORAL )
				return "feature.temporal";			
			if ( input == INPUT_COLLECTIONS.FILESYSTEM )
				return "filesystem";			
			if ( input == INPUT_COLLECTIONS.SHARE_ZIP )
				return "file.binary_shares";
			if ( input == INPUT_COLLECTIONS.RECORDS )
				return "records";			
		}
		catch (Exception ex)
		{
			//was not one of the standard collections
		}
		return null;
	}

	private String getCustomInputCollection(String inputColl, List<ObjectId> communityIds) 
	{
		String output = null;		
		//if not one of the standard collections, see if its in custommr.customlookup
		List<Object> searchTerms = new ArrayList<Object>();
		try
		{
			ObjectId jid = new ObjectId(inputColl);
			searchTerms.add(new BasicDBObject(CustomMapReduceJobPojo._id_,jid));
		}
		catch (Exception e)
		{
			//oid failed, will only add title
		}
		searchTerms.add(new BasicDBObject("jobtitle",inputColl));
				
		try 
		{
			//find admin entry
			BasicDBObject query = new BasicDBObject(CustomMapReduceJobPojo.communityIds_,
														new BasicDBObject(DbManager.in_,communityIds.toArray()));
			query.append(DbManager.or_,searchTerms.toArray());
			DBObject dbo = DbManager.getCustom().getLookup().findOne(query);
			if ( dbo != null )
			{
				CustomMapReduceJobPojo cmr = CustomMapReduceJobPojo.fromDb(dbo, CustomMapReduceJobPojo.class);
				output = cmr._id.toString();
			}
		}
		catch (Exception exc)
		{
			//no tables were found leave output null
		}
				
		return output;
	}

	/**
	 * Returns all jobs that you have access to by checking for matching communities
	 * 
	 * @param userid
	 * @return
	 */
	public ResponsePojo getJobOrJobs(String userid, String jobIdOrTitle, String commIds) 
	{
		ResponsePojo rp = new ResponsePojo();		
		try 
		{
			DBObject dbo = DbManager.getSocial().getPerson().findOne(new BasicDBObject(CustomMapReduceJobPojo._id_,
																		new ObjectId(userid)));
			if (dbo != null )
			{
				PersonPojo pp = PersonPojo.fromDb(dbo, PersonPojo.class);
				HashSet<ObjectId> communities = new HashSet<ObjectId>();
				HashSet<ObjectId> savedCommunities = communities; // (change it if we do additional filtering) 
				for ( PersonCommunityPojo pcp : pp.getCommunities()) {
					communities.add(pcp.get_id());
				}
				if (null != commIds) { // Filter out communities based on command line
					String[] commIdStrs = SocialUtils.getCommunityIds(pp.get_id().toString(), commIds);
					HashSet<ObjectId> filterCommunities = new HashSet<ObjectId>();
					for (String commId: commIdStrs) {
						filterCommunities.add(new ObjectId(commId));
					}
					savedCommunities = new HashSet<ObjectId>(communities);
					communities.retainAll(filterCommunities);
				}//TESTED (by hand)
				
				BasicDBObject commquery = new BasicDBObject(
						CustomMapReduceJobPojo.communityIds_, new BasicDBObject(MongoDbManager.in_,communities));
				if (null != jobIdOrTitle) {
					try {
						if (jobIdOrTitle.contains(",")) {
							String[] jobidstrs = jobIdOrTitle.split("\\s*,\\s*");
							ObjectId[] jobids = new ObjectId[jobidstrs.length];
							for (int i = 0; i < jobidstrs.length; ++i) {
								jobids[i] = new ObjectId(jobidstrs[i]);
							}
							commquery.put(CustomMapReduceJobPojo._id_, new BasicDBObject(MongoDbManager.in_, jobids));
						}
						else {
							ObjectId jobid = new ObjectId(jobIdOrTitle);
							commquery.put(CustomMapReduceJobPojo._id_, jobid);
						}
					}
					catch (Exception e) { // Must be a jobtitle
						if (jobIdOrTitle.contains(",")) {
							String[] jobtitles = jobIdOrTitle.split("\\s*,\\s*");
							commquery.put(CustomMapReduceJobPojo.jobtitle_, new BasicDBObject(MongoDbManager.in_, jobtitles));
						}
						else {
							commquery.put(CustomMapReduceJobPojo.jobtitle_, jobIdOrTitle);
						}
					}
				}
				DBCursor dbc = DbManager.getCustom().getLookup().find(commquery);		
				if ((0 == dbc.count()) && (null != jobIdOrTitle)) {
					rp.setResponse(new ResponseObject("Custom Map Reduce Get Jobs",false,"No jobs to find"));					
				}
				else {
					rp.setResponse(new ResponseObject("Custom Map Reduce Get Jobs",true,"succesfully returned jobs"));
					List<CustomMapReduceJobPojo> jobs = CustomMapReduceJobPojo.listFromDb(dbc, CustomMapReduceJobPojo.listType());
					// Extra bit of code, need to eliminate partial community matches, eg if job belongs to A,B and we only belong to A
					// then obviously we can't see the job since it contains data from B
					Iterator<CustomMapReduceJobPojo> jobsIt = jobs.iterator();
					while (jobsIt.hasNext()) {
						CustomMapReduceJobPojo job = jobsIt.next();
						if (!savedCommunities.containsAll(job.communityIds)) {
							jobsIt.remove();
						}
					}//TESTED
					rp.setData(jobs, new CustomMapReduceJobPojoApiMap(savedCommunities));
				}
			}
			else
			{
				rp.setResponse(new ResponseObject("Custom Map Reduce Get Jobs",false,"error retrieving users communities"));
			}			
		} 
		catch (Exception e)
		{
			// If an exception occurs log the error
			logger.error("Exception Message: " + e.getMessage(), e);
			rp.setResponse(new ResponseObject("Custom Map Reduce Get Jobs",false,"error retrieving jobs"));
		}
		return rp;
	}
	
	//////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////
	
	// UTILITIES		
	
	/**
	 * Helper function to work out the dependencies for a job.  Will try to find a job
	 * for each item given.  Throws errors if the job cannot be found.
	 * 
	 * @param jobDependencyString A comma deliminated list of jobids or job titles or any combination
	 * @return
	 * @throws Exception
	 */
	private Set<ObjectId> getJobDependencies(String jobDependencyString) throws Exception
	{
		Set<ObjectId> dependencies = new HashSet<ObjectId>();
		//try to parse our all the names, throw any errors up if we fail
		String[] dependencyStrings = jobDependencyString.split(",");
		for (String dependency : dependencyStrings)
		{
			//try to get the job via id or title
			List<Object> searchTerms = new ArrayList<Object>();
			try
			{
				ObjectId jid = new ObjectId(dependency);
				searchTerms.add(new BasicDBObject(CustomMapReduceJobPojo._id_,jid));
			}
			catch (Exception ex)
			{
				//oid failed, will only add title
			}
			searchTerms.add(new BasicDBObject(CustomMapReduceJobPojo.jobtitle_,dependency));
			DBObject dbo = DbManager.getCustom().getLookup().findOne(new BasicDBObject(DbManager.or_,searchTerms.toArray()));			
			if ( dbo != null )
			{	
				//job existed, add its id
				CustomMapReduceJobPojo cmr = CustomMapReduceJobPojo.fromDb(dbo, CustomMapReduceJobPojo.class);
				dependencies.add(cmr._id);
			}
			else
			{
				//throw an exception, job did not exist
				throw new Exception();
			}
		}		
		return dependencies;
	}
	
	/**
	 * Attempts to remove the map reduce job as well as results and
	 * jar file.
	 * 
	 * @param userid
	 * @param jobidortitle
	 * @param forced 
	 * @return
	 */
	public static ResponsePojo removeJob(String userid, String jobidortitle, Boolean removeJar, boolean forced) 
	{
		ResponsePojo rp = new ResponsePojo();		
		
		List<Object> searchTerms = new ArrayList<Object>();
		try
		{
			ObjectId jid = new ObjectId(jobidortitle);
			searchTerms.add(new BasicDBObject(CustomMapReduceJobPojo._id_,jid));
		}
		catch (Exception ex)
		{
			//oid failed, will only add title
		}
		searchTerms.add(new BasicDBObject(CustomMapReduceJobPojo.jobtitle_,jobidortitle));
				
		try 
		{
			//find admin entry);
			DBObject dbo = DbManager.getCustom().getLookup().findOne(new BasicDBObject(DbManager.or_,searchTerms.toArray()));			
			if ( dbo != null )
			{				
				CustomMapReduceJobPojo cmr = CustomMapReduceJobPojo.fromDb(dbo, CustomMapReduceJobPojo.class);
				//make sure user is allowed to see results
				
				if (null == removeJar) {
					removeJar = doesJobOwnJar(cmr);
				}				
				if ( forced || RESTTools.adminLookup(userid) || cmr.submitterID.toString().equals(userid) )
				{
					//make sure job is not running
					if ( ( cmr.jobidS == null ) || cmr.jobidS.equals( "CHECKING_COMPLETION" ) || cmr.jobidS.equals( "" ) ) // (< robustness, sometimes server gets stuck here...)
					{
						// Remove index
						try {
							if (CustomOutputIndexingEngine.isIndexed(cmr)) {
								CustomOutputIndexingEngine.deleteOutput(cmr);  						
							}//TESTED (by hand)
						}
						catch (Throwable t) { } // (es 0.19 case, just carry on silently)
						
						//remove results and job
						DbManager.getCustom().getLookup().remove(new BasicDBObject(CustomMapReduceJobPojo._id_, cmr._id));
						DbManager.getCollection(cmr.getOutputDatabase(), cmr.outputCollection).drop();
						DbManager.getCollection(cmr.getOutputDatabase(), cmr.outputCollectionTemp).drop();

						// Delete HDFS directory, if one is used
						if ((null != cmr.exportToHdfs) && cmr.exportToHdfs) {
							HadoopUtils.deleteHadoopDir(cmr);
						}
						
						//remove jar file if unused elsewhere?
						if ( removeJar )
						{
							ResponsePojo sharerp = removeJarFile(cmr.jarURL, cmr.submitterID.toString());
							if ( sharerp.getResponse().isSuccess() )
							{
								rp.setResponse(new ResponseObject("Remove Custom Map Reduce Job",true,"Job, results, and jar removed successfully."));
							}
							else
							{
								rp.setResponse(new ResponseObject("Remove Custom Map Reduce Job",true,"Job and results removed successfully.  Removing the jar had an error: " + sharerp.getResponse().getMessage()));
							}
						}
						else
						{
							rp.setResponse(new ResponseObject("Remove Custom Map Reduce Job",true,"Job and results removed successfully.  Manually remove the jar if you are done with it."));
						}
					}
					else
					{
						rp.setResponse(new ResponseObject("Remove Custom Map Reduce Job",false,"Job is currently running (or not yet marked as completed).  Please wait until the job completes to update it."));
					}
				}
				else
				{
					rp.setResponse(new ResponseObject("Remove Custom Map Reduce Job",false,"User must have submitter or admin permissions to remove jobs"));
				}
			}
			else
			{
				rp.setResponse(new ResponseObject("Remove Custom Map Reduce Job",false,"Job does not exist"));
			}
		} 
		catch (Exception e)
		{
			// If an exception occurs log the error
			logger.error("Exception Message: " + e.getMessage(), e);
			rp.setResponse(new ResponseObject("Remove Custom Map Reduce Job",false,"error retrieving job info"));
		}
		return rp;
	}
	
	public static boolean doesJobOwnJar(CustomMapReduceJobPojo cmr) {
		if (null != cmr.jarURL) {
			try {
				String jaridstr = cmr.jarURL.substring( cmr.jarURL.lastIndexOf("/") + 1 );
				ObjectId jarid = new ObjectId(jaridstr);
				BasicDBObject query = new BasicDBObject(SharePojo._id_, jarid);
				BasicDBObject fields = new BasicDBObject();
				SharePojo jarShare = SharePojo.fromDb(DbManager.getSocial().getShare().findOne(query, fields), SharePojo.class);
				if (null != jarShare) {
					return cmr.jobtitle.equals(jarShare.getTitle());
				}
			}
			catch (Exception e) {} // it's fine just don't delete
	
		}
		return false; // (if we're not sure then leave it alone)
	}//TODO (INF-2866)
	
	/**
	 * Tries to remove given community from any jobs it may be connected to.
	 * If jobs have no community now, deletes them completely.
	 * 
	 * @return returns a list of jobs that were running or had other errors so they could not be deleted
	 * 
	 * @param communityId
	 */
	public static List<CustomMapReduceJobPojo> removeCommunityJobs(ObjectId communityId)
	{
		//Step 1, remove commid from anywhere it may exist
		BasicDBObject comm_query = new BasicDBObject(CustomMapReduceJobPojo.communityIds_, communityId);
		BasicDBObject update_query = new BasicDBObject(MongoDbManager.pull_, new BasicDBObject(CustomMapReduceJobPojo.communityIds_, communityId));
		DbManager.getCustom().getLookup().update(comm_query, update_query, false, true);
		
		//Step 2, get any jobs that have no commid now
		BasicDBObject empty_comm_query = new BasicDBObject(CustomMapReduceJobPojo.communityIds_, new BasicDBObject(MongoDbManager.size_,0));
		DBCursor dbc = DbManager.getCustom().getLookup().find(empty_comm_query);
		List<CustomMapReduceJobPojo> jobs = CustomMapReduceJobPojo.listFromDb(dbc, CustomMapReduceJobPojo.listType());
		
		//Step 3, remove the jobs via removeJob, used forced
		List<CustomMapReduceJobPojo> failedToRemove = new ArrayList<CustomMapReduceJobPojo>();
		for ( CustomMapReduceJobPojo cmr : jobs )
		{			
			ResponsePojo rp = removeJob(null, cmr.jobtitle, null, true);
			if ( !rp.getResponse().isSuccess() )
			{
				failedToRemove.add(cmr);
			}
		}
		
		//Step 4, return the failed to remove jobs
		return failedToRemove;
	}
	
	/**
	 * Checks if any other custom jobs use the same jar and attempts to remove using the share handler.
	 * 
	 * @param url
	 * @param ownerid
	 * @return
	 */
	private static ResponsePojo removeJarFile(String url, String ownerid)
	{
		//is a jar on our system
		if ( url.startsWith("$infinite") )
		{
			//check if the jar is used for any other jobs, remove it if not
			if ( DbManager.getCustom().getLookup().find(new BasicDBObject(CustomMapReduceJobPojo.jarURL_, url)).count() == 0 )
			{
				//grab the id
				String jarid = url.substring( url.lastIndexOf("/") + 1 );
				//remove the jar
				return new ShareHandler().removeShare(ownerid, jarid);
			}
			else
			{
				return new ResponsePojo(new ResponseObject("removejar", false, "More than 1 job use this jar, could not remove."));
			}
		}
		return new ResponsePojo(new ResponseObject("removejar", false, "Jar URL is not an infinite share, could not remove."));
	}
	
	//////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////

	// REMOVED THIS BECAUSE THE DATABASES ARE ALL SHARDED
	/*
	public ResponsePojo runAggregation(String userid, String key, String cond, String initial, String reduce, String collection) 
	{
		//FINALize is in the new java driver it looks like, not our current version
		//String finalize = null; 
		ResponsePojo rp = new ResponsePojo();	
		try
		{
			CollectionManager cm = new CollectionManager();
			//get user communities
			DBObject dboperson = cm.getPerson().findOne(new BasicDBObject("_id", new ObjectId(userid)));
			if ( dboperson != null )
			{
				GsonBuilder gb = GsonTypeAdapter.getGsonBuilder(GsonTypeAdapter.GsonAdapterType.DESERIALIZER);
				Gson gson = gb.create();
				PersonPojo pp = gson.fromJson(dboperson.toString(), PersonPojo.class);
				List<ObjectId> communityIds = new ArrayList<ObjectId>();
				for ( PersonCommunityPojo pcp : pp.getCommunities() )
					communityIds.add( pcp.get_id() );
				//parse statements
				DBObject dbKey = (DBObject) com.mongodb.util.JSON.parse(key);
				DBObject dbCond = (DBObject) com.mongodb.util.JSON.parse(cond);
				DBObject dbInitial = (DBObject) com.mongodb.util.JSON.parse(initial);
				//add in communities to condition statement
				dbCond.put("communityId", new BasicDBObject("$in",communityIds.toArray()));			
				//run command
				String inputCol = getInputCollection(collection);
				DBObject dbo = cm.getCustomCollection(inputCol).group(dbKey, dbCond, dbInitial, reduce);
				if ( dbo != null )
				{
					rp.setResponse(new ResponseObject("Custom Map Reduce Run Aggregation",true,"Aggregation ran successfully"));
					rp.setData(dbo.toString());
				}
				else //need to test, dont know if results will ever be empty if it ran
				{
					rp.setResponse(new ResponseObject("Custom Map Reduce Run Aggregation",false,"aggregation returned no results"));
				}
			}
			else
			{
				rp.setResponse(new ResponseObject("Custom Map Reduce Run Aggregation",false,"user did not exist"));
			}			
		}
		catch (Exception ex)
		{
			rp.setResponse(new ResponseObject("Custom Map Reduce Run Aggregation",false,"error running aggregation, could not parse db objects maybe?"));
		}
		
		return rp;
	}
	*/
}

