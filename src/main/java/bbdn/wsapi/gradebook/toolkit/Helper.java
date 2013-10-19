package bbdn.wsapi.gradebook.toolkit;

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

/**
 * This class has a variety of methods demonstrating different ways to use the webservice methods directly from within a
 * building block, bypassing the SOAP layer typically associated with these webservices.
 */
public class Helper
{
  private static final String SAMPLE_B2_COLUMN_NAME = "Sample B2forWS Column Name";
  private Id _courseId;
  private String _courseIdStr;
  private boolean _foundOurColumn;

  public Helper()
  {
    this( null );
  }

  public Helper( Id courseId )
  {
    _courseId = courseId;
    _courseIdStr = ( _courseId == null ? "" : _courseId.toExternalString() );
    _foundOurColumn = false;
  }

  public String doGradebook()
  {
    String result;
    GradebookWS gb;
    try
    {
      // First try using a gradebook WS suitable for use by the current user
      gb = GradebookWSFactory.getGradebookWS();
      result = listGradebookColumns( _courseIdStr, "(as current user)", gb );
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
          result = listGradebookColumns( _courseIdStr, "(as tool)", gb );
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
    // Result now contains the list of columns.  Let's create one ourselves.
    result = result + createColumnIfNeeded();
    CourseMembership cm = ContextManagerFactory.getInstance().getContext().getCourseMembership();
    if ( cm != null && cm.getRole().equals( Role.STUDENT ) )
    {
      result = result + updateAttempt( cm );
    }
    return result;
  }

  private String updateAttempt( CourseMembership cm )
  {
    GradebookWS gb = GradebookWSFactory.getGradebookWSForTool(); // Again, using the tool access otherwise the calls would fail as the student
    // (Assumes automatic grading... for manual grading based on user input you would use getGradebookWS() and only function
    // if the current user has permission to grade.
    ColumnFilter cfilter = new ColumnFilter();
    cfilter.setFilterType( GradebookWSConstants.GET_COLUMN_BY_COURSE_ID_AND_COLUMN_NAME );
    String[] names = { SAMPLE_B2_COLUMN_NAME };
    cfilter.setNames( names );
    ColumnVO columns[] = gb.getGradebookColumns( _courseIdStr, cfilter );
    String gbColumnId = columns[ 0 ].getId();

    ScoreFilter filter = new ScoreFilter();
    filter.setColumnId( gbColumnId );
    String[] userIds = new String[ 1 ];
    userIds[ 0 ] = cm.getUserId().toExternalString();
    filter.setUserIds( userIds );
    filter.setFilterType( 2 );
    ScoreVO[] grades = gb.getGrades( _courseIdStr, filter );
    String gradeId;
    AttemptVO[] attempts = null;
    if ( grades == null || grades.length == 0 || grades[ 0 ] == null )
    {
      grades = new ScoreVO[ 1 ];
      grades[ 0 ] = new ScoreVO();
      grades[ 0 ].setCourseId( _courseIdStr );
      //grades[ 0 ].setManualGrade( ( ProxyUtil.getInstance().getRandomFloat() * 100 ) + "" );
      grades[ 0 ].setUserId( cm.getUserId().toExternalString() );
      grades[ 0 ].setColumnId( gbColumnId );
      String[] gradeIds = gb.saveGrades( _courseIdStr, grades, false );
      gradeId = gradeIds[ 0 ];
      AttemptFilter attemptFilter = new AttemptFilter();
      attemptFilter.setGradeId( gradeId );
      attemptFilter.setFilterType( 1 );
      attempts = gb.getAttempts( _courseIdStr, attemptFilter );
      if ( attempts == null || attempts.length == 0 || attempts[ 0 ] == null )
      {
        attempts = new AttemptVO[ 1 ];
        attempts[ 0 ] = new AttemptVO();
      }
    }
    else
    {
      gradeId = grades[ 0 ].getId();
      attempts = new AttemptVO[ 1 ];
      attempts[ 0 ] = new AttemptVO();
    }
    attempts[ 0 ].setGradeId( gradeId );
    attempts[ 0 ].setScore( new Random().nextInt( 10 ) );
    // To set the attempt as needsGrading: attempts[0].setGrade("-");
    gb.saveAttempts( _courseIdStr, attempts );

    return "Updated attempt score";
  }

  private String createColumnIfNeeded()
  {
    if ( _foundOurColumn )
    {
      return "<br>Already have column created";
    }
    // Use the tool-based access for this demo - so a student access will also create the column if required.
    GradebookWS gb = GradebookWSFactory.getGradebookWSForTool();
    ColumnVO[] columns = new ColumnVO[ 1 ];
    ColumnVO column = new ColumnVO();
    column.setColumnName( SAMPLE_B2_COLUMN_NAME );
    column.setCourseId( _courseId.toExternalString() );
    column.setPossible( 10 );
    column.setVisible( true );
    column.setVisibleInBook( true );
    String[] expansionData = { "scoreProviderHandle=resource/x-plugin-b2-gb-ws-sample" }; // Value must match that declared in bb-manifest.xml
    column.setExpansionData( expansionData );

    columns[ 0 ] = column;

    String[] ids = gb.saveColumns( _courseId.toExternalString(), columns );
    return "Created column " + ids[ 0 ];
  }

  private String listGradebookColumns( String courseId, String asWho, GradebookWS gb )
  {
    ColumnFilter filter = new ColumnFilter();
    filter.setFilterType( GradebookWSConstants.GET_COLUMN_BY_COURSE_ID );
    ColumnVO columns[] = gb.getGradebookColumns( courseId, filter );
    StringBuilder info = new StringBuilder( "Gradebook columns " + asWho + ": <ul>" );
    for ( int i = 0; i < columns.length; i++ )
    {
      String columnName = columns[ i ].getColumnName();
      if ( SAMPLE_B2_COLUMN_NAME.equals( columnName ) )
      {
        _foundOurColumn = true;
      }
      info.append( "<li>" + columnName + "</li>" );
    }
    info.append( "</ul>" );
    return info.toString();
  }

  public String doGradingPage( HttpServletRequest request )
  {
    // This would typically start the workflow for the instructor to review the student's attempt 
    // (From data local to your tool) and result in updating of their attempt grade.

    // Dump out all the parameters - for debugging only.
    StringBuilder result = new StringBuilder();
    @SuppressWarnings( "unchecked" )
    Map<String, String[]> parms = request.getParameterMap();
    result.append( "<ul>" );
    for ( Entry<String, String[]> entry : parms.entrySet() )
    {
      result.append( "<li>" );
      result.append( entry.getKey() + "=" + arrayAsString( entry.getValue() ) );
      result.append( "</li>" );
    }
    result.append( "</ul>" );

    // Load the user's current attempt - doing it as the current user, not the tool, because
    // this use case is typically executed as the instructor user via a click on the gradeattempt 
    // from within the gradebook.  We get to this code because of the score provider's Grade-Action
    // (From bb-manifest.xml)    
    GradebookWS gb = GradebookWSFactory.getGradebookWS();
    AttemptFilter attemptFilter = new AttemptFilter();
    attemptFilter.setFilterType( GradebookWSConstants.GET_ATTEMPT_BY_IDS );
    String[] ids = { request.getParameter( "attempt_id" ) };
    attemptFilter.setIds( ids );
    AttemptVO[] attempts = gb.getAttempts( _courseIdStr, attemptFilter );
    result.append( "<br>Current Attempt Grade:" + attempts[ 0 ].getScore() );
    return result.toString();
  }

  private String arrayAsString( String[] value )
  {
    StringBuilder res = new StringBuilder();
    boolean first = true;
    for ( String val : value )
    {
      if ( !first )
      {
        res.append( "," );
      }
      first = false;
      res.append( val );
    }
    return res.toString();
  }

  public String doAdminSettingsPage()
  {
    try
    {
      ContextWS c = ContextWSFactory.getContextWS();
      c.initialize();
      UtilWS u = UtilWSFactory.getUtilWS();
      u.initializeUtilWS( true );
      boolean ent = u.checkEntitlement( null, "system.portaloptions.managemodules.VIEW" );
      String sysIdDirect = GeneralUtil.getSystemInstallationId();
      String sysIdWs = c.getSystemInstallationId();

      return "Entitlement Check Result:" + ent + "<br>System Identifier from direct call:" + sysIdDirect
             + " and from webservice call:" + sysIdWs + "";
    }
    catch ( Exception e )
    {
      e.printStackTrace();
      return e.toString();
    }
  }

}
