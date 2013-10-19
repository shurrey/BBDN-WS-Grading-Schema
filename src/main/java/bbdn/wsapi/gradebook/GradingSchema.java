package bbdn.wsapi.gradebook;

import blackboard.data.course.CourseMembership;
import blackboard.data.course.CourseMembership.Role;
import blackboard.persist.Id;
import blackboard.platform.context.ContextManagerFactory;
import blackboard.platform.security.AccessException;
import blackboard.util.GeneralUtil;
import blackboard.ws.context.ContextWS;
import blackboard.ws.context.ContextWSFactory;
import blackboard.ws.gradebook.*;
import blackboard.ws.util.UtilWS;
import blackboard.ws.util.UtilWSFactory;

import java.util.*;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

public class GradingSchema {

private Id _courseId;
private String _courseIdStr;
private boolean _foundOurColumn;

	public GradingSchema()
	{
		this( null );
	}

	public GradingSchema( Id courseId )
	{
		_courseId = courseId;
  		_courseIdStr = ( _courseId == null ? "" : _courseId.toExternalString() );
  		_foundOurColumn = false;
	}
	
	public String doGradebookSchema()
	  {
	    String result;
	    GradebookWS gb;
	    try
	    {
	      // First try using a gradebook WS suitable for use by the current user
	      gb = GradebookWSFactory.getGradebookWS();
	      result = listGradebookSchema( _courseIdStr, "(as current user)", gb );
	    }
	    catch ( Exception e )
	    {
	      Throwable cause = e.getCause();
	      if ( cause instanceof AccessException )
	      {
	        /*
	         * When this page is accessed as a student, this exception is expected. In practice how you deal with this and
	         * if you even get here depends on logic (i.e. you wouldn't make calls like this in a student-facing page to
	         * begin with typically). For this example we'll just fallback to the tool-based version of the method call to
	         * demonstrate how you can still make privileged calls while the current user is not privileged when required.
	         * NOTE that for this to work you must have declared your intent to call the getGradebookColumns as a tool in
	         * your bb-manifest.xml file (refer to the sample bb-manifest.xml for this project)
	         */
	        try
	        {
	          gb = GradebookWSFactory.getGradebookWSForTool();
	          result = listGradebookSchema( _courseIdStr, "(as tool)", gb );
	        }
	        catch ( Exception e2 )
	        {
	          e2.printStackTrace();
	          result = e2.toString();
	        }
	      }
	      else
	      {
	        e.printStackTrace();
	        result = e.toString();
	      }
	    }
	
	    return result;
	}
	
	private String listGradebookSchema( String courseId, String asWho, GradebookWS gb )
	  {
	    GradingSchemaFilter filter = new GradingSchemaFilter();
	    filter.setFilterType( GradebookWSConstants.GET_GRADING_SCHEMA_BY_COURSE_ID );
	    GradingSchemaVO schemas[] = gb.getGradingSchemas(courseId, filter);
	    StringBuilder info = new StringBuilder( "Gradebook Schemas " + asWho + ": <ul>" );
	    for ( int i = 0; i < schemas.length; i++ )
	    {
	      String schemaInfo = schemas[ i ].getTitle() + " | " + schemas[ i ].getDescription() + " | " +
	    		  schemas[ i ].getScaleType() + " | " + schemas[ i ].toString();
	      

	      info.append( "<li>" + schemaInfo + "</li>" );
	    }
	    info.append( "</ul>" );
	    return info.toString();
	  }
}
